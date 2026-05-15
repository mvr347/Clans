package me.lovelace.clans.integration;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanMember;
import me.lovelace.clans.model.ClanRank;
import me.lovelace.clans.model.ClanTerritory;
import me.lovelace.clans.model.TerritoryKey;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class AdvancedClaimsHook {
    private final ClansPlugin plugin;
    private Object api;
    private Class<?> trustLevelClass;
    private boolean enabled;

    public AdvancedClaimsHook(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        enabled = plugin.getConfig().getBoolean("integration.advanced-claims.enabled", true)
                && Bukkit.getPluginManager().isPluginEnabled("AdvancedClaims");
        if (!enabled) {
            plugin.getLogger().info("AdvancedClaims integration is disabled or plugin is not installed.");
            return;
        }

        try {
            String apiClassName = plugin.getConfig().getString("integration.advanced-claims.api-class", "me.lovelace.advancedclaims.api.AdvancedClaimsAPI");
            String trustLevelClassName = plugin.getConfig().getString("integration.advanced-claims.trust-level-class", "me.lovelace.advancedclaims.model.TrustLevel");
            Class<?> apiClass = Class.forName(apiClassName);
            Method getInstance = apiClass.getMethod(plugin.getConfig().getString("integration.advanced-claims.methods.get-instance", "getInstance"));
            api = getInstance.invoke(null);
            trustLevelClass = Class.forName(trustLevelClassName);
            plugin.getLogger().info("AdvancedClaimsAPI integration enabled.");
        } catch (ReflectiveOperationException exception) {
            enabled = false;
            plugin.getLogger().warning("AdvancedClaims found, but API reflection failed: " + exception.getMessage());
        }
    }

    public boolean enabled() {
        return enabled && api != null;
    }

    public Optional<UUID> createOrAttachClaim(Clan clan, ClanTerritory territory) {
        if (!enabled() || !plugin.getConfig().getBoolean("integration.advanced-claims.auto-claim-chunk", true)) {
            return Optional.empty();
        }
        TerritoryKey key = territory.key();
        World world = Bukkit.getWorld(key.world());
        if (world == null) {
            return Optional.empty();
        }
        int minX = key.chunkX() << 4;
        int minZ = key.chunkZ() << 4;
        BoundingBox box = new BoundingBox(minX, world.getMinHeight(), minZ, minX + 15, world.getMaxHeight(), minZ + 15);
        Object claim = invokeBest(
                methodName("create-claim", "createClaim"),
                new Object[]{world, box, clan.guildmasterId().orElse(territory.claimedBy()), world.getHighestBlockAt(minX + 8, minZ + 8).getLocation()},
                new Object[]{world, box, clan.guildmasterId().orElse(territory.claimedBy())},
                new Object[]{clan.guildmasterId().orElse(territory.claimedBy()), world.getChunkAt(key.chunkX(), key.chunkZ())}
        ).orElse(null);

        Optional<UUID> claimId = extractClaimId(claim);
        claimId.ifPresent(id -> syncClanTrust(clan, territory.withAdvancedClaimId(id)));
        return claimId;
    }

    public void deleteClaim(UUID claimId) {
        if (!enabled() || claimId == null) {
            return;
        }
        invokeBest(methodName("delete-claim", "deleteClaim"), new Object[]{claimId}, new Object[]{claimId.toString()});
    }

    public void syncClanTrust(Clan clan, ClanTerritory territory) {
        if (!enabled() || territory.advancedClaimId() == null) {
            return;
        }
        Object claim = findClaim(territory.advancedClaimId()).orElse(territory.advancedClaimId());
        for (ClanMember member : clan.members().values()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(member.playerId());
            syncPlayerTrust(claim, player, member.rank());
        }
    }

    public void syncPlayerTrust(Clan clan, OfflinePlayer player, ClanRank rank) {
        if (!enabled()) {
            return;
        }
        for (ClanTerritory territory : clan.territories()) {
            if (territory.advancedClaimId() == null) {
                continue;
            }
            Object claim = findClaim(territory.advancedClaimId()).orElse(territory.advancedClaimId());
            syncPlayerTrust(claim, player, rank);
        }
    }

    public void removePlayerTrust(Clan clan, OfflinePlayer player) {
        if (!enabled()) {
            return;
        }
        for (ClanTerritory territory : clan.territories()) {
            if (territory.advancedClaimId() == null) {
                continue;
            }
            Object claim = findClaim(territory.advancedClaimId()).orElse(territory.advancedClaimId());
            invokeBest(methodName("remove-player", "removePlayerFromClaim"),
                    new Object[]{claim, player},
                    new Object[]{territory.advancedClaimId(), player.getUniqueId()},
                    new Object[]{territory.advancedClaimId(), player});
        }
    }

    private void syncPlayerTrust(Object claim, OfflinePlayer player, ClanRank rank) {
        Object trustLevel = trustLevel(rank);
        invokeBest(methodName("add-player", "addPlayerToClaim"),
                new Object[]{claim, player, trustLevel},
                new Object[]{claim, player.getUniqueId(), trustLevel},
                new Object[]{claim, player, String.valueOf(trustLevel)},
                new Object[]{claim, player.getUniqueId(), String.valueOf(trustLevel)});
    }

    private Optional<Object> findClaim(UUID claimId) {
        Object value = invokeBest(methodName("get-claim-by-id", "getClaimById"), new Object[]{claimId}, new Object[]{claimId.toString()}).orElse(null);
        if (value instanceof Optional<?> optional) {
            return optional.map(object -> object);
        }
        return Optional.ofNullable(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object trustLevel(ClanRank rank) {
        String configured = plugin.getConfig().getString("integration.advanced-claims.trust-mapping." + rank.name(), "INTERACT");
        if (trustLevelClass != null && trustLevelClass.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) trustLevelClass.asSubclass(Enum.class), configured.toUpperCase(Locale.ROOT));
        }
        return configured;
    }

    private Optional<UUID> extractClaimId(Object claim) {
        if (claim == null) {
            return Optional.empty();
        }
        if (claim instanceof Optional<?> optional) {
            return optional.flatMap(this::extractClaimId);
        }
        if (claim instanceof UUID uuid) {
            return Optional.of(uuid);
        }
        if (claim instanceof String string) {
            try {
                return Optional.of(UUID.fromString(string));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }
        for (String method : new String[]{"getId", "id", "getUniqueId", "uniqueId"}) {
            try {
                Object value = claim.getClass().getMethod(method).invoke(claim);
                Optional<UUID> uuid = extractClaimId(value);
                if (uuid.isPresent()) {
                    return uuid;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return Optional.empty();
    }

    private Optional<Object> invokeBest(String methodName, Object[]... candidates) {
        if (!enabled()) {
            return Optional.empty();
        }
        for (Object[] args : candidates) {
            Optional<Method> method = findCompatibleMethod(methodName, args);
            if (method.isEmpty()) {
                continue;
            }
            try {
                return Optional.ofNullable(method.get().invoke(api, args));
            } catch (IllegalAccessException | InvocationTargetException exception) {
                plugin.getLogger().fine("AdvancedClaims call failed for " + methodName + ": " + exception.getMessage());
            }
        }
        return Optional.empty();
    }

    private Optional<Method> findCompatibleMethod(String methodName, Object[] args) {
        for (Method method : api.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean compatible = true;
            for (int index = 0; index < parameterTypes.length; index++) {
                if (args[index] != null && !wrap(parameterTypes[index]).isAssignableFrom(args[index].getClass())) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                method.setAccessible(true);
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    private Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }

    private String methodName(String pathName, String fallback) {
        return plugin.getConfig().getString("integration.advanced-claims.methods." + pathName, fallback);
    }
}
