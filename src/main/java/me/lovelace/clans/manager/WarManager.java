package me.lovelace.clans.manager;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.api.events.ClanWarEndEvent;
import me.lovelace.clans.api.events.ClanWarStartEvent;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanMember;
import me.lovelace.clans.model.ClanRank;
import me.lovelace.clans.model.ClanTerritory;
import me.lovelace.clans.model.TerritoryKey;
import me.lovelace.clans.model.war.ClanWar;
import me.lovelace.clans.model.war.WarResult;
import me.lovelace.clans.model.war.WarState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class WarManager {
    private static final Duration DEFAULT_DURATION = Duration.ofMinutes(30);
    private static final Duration BANNER_CAPTURE_DURATION = Duration.ofMinutes(3);
    // Cooldown for declaring war on the same clan (e.g., 24 hours)
    private static final Duration WAR_COOLDOWN_DURATION = Duration.ofHours(24);

    private final ClansPlugin plugin;
    private final Map<UUID, ClanWar> activeWars = new ConcurrentHashMap<>();
    // Map to store the last time a war was declared between two clans
    private final Map<AbstractMap.SimpleImmutableEntry<UUID, UUID>, Long> warCooldowns = new ConcurrentHashMap<>();

    public WarManager(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    // Helper method to create a consistent key for a pair of clan UUIDs
    private AbstractMap.SimpleImmutableEntry<UUID, UUID> getWarPairKey(UUID clan1, UUID clan2) {
        // Ensure consistent order for the key regardless of which UUID is passed first
        return clan1.compareTo(clan2) < 0
                ? new AbstractMap.SimpleImmutableEntry<>(clan1, clan2)
                : new AbstractMap.SimpleImmutableEntry<>(clan2, clan1);
    }

    public CompletableFuture<ClanWar> startWarAsync(Clan attacker, Clan defender, TerritoryKey territory) {
        return plugin.supplySync(() -> {
            if (activeWars.size() >= 3) {
                throw new IllegalStateException("war.max-wars-reached");
            }
            if (areAtWar(attacker.id(), defender.id())) {
                throw new IllegalStateException("war.already-at-war");
            }

            // Check for war cooldown
            AbstractMap.SimpleImmutableEntry<UUID, UUID> cooldownKey = getWarPairKey(attacker.id(), defender.id());
            Long lastWarTime = warCooldowns.get(cooldownKey);
            long now = System.currentTimeMillis();

            if (lastWarTime != null && (now - lastWarTime < WAR_COOLDOWN_DURATION.toMillis())) {
                long remainingSeconds = (WAR_COOLDOWN_DURATION.toMillis() - (now - lastWarTime)) / 1000;
                throw new IllegalStateException("war.cooldown-active:" + remainingSeconds); // Custom message key with remaining time
            }

            int attackerOnline = 0;
            for (UUID memberId : attacker.members().keySet()) {
                if (Bukkit.getPlayer(memberId) != null) attackerOnline++;
            }

            int defenderOnline = 0;
            for (UUID memberId : defender.members().keySet()) {
                if (Bukkit.getPlayer(memberId) != null) defenderOnline++;
            }

            if (attackerOnline < 3 || defenderOnline < 3) {
                throw new IllegalStateException("war.not-enough-members");
            }

            // Check if defender has any online leader or guardian
            boolean defenderHasOnlineLeaderOrGuardian = defender.members().values().stream()
                    .filter(member -> member.rank() == ClanRank.LEADER || member.rank() == ClanRank.GUARDIAN)
                    .anyMatch(member -> Bukkit.getPlayer(member.playerId()) != null);

            if (!defenderHasOnlineLeaderOrGuardian) {
                throw new IllegalStateException("war.defender-no-online-leader-or-guardian");
            }

            ClanWar war = new ClanWar(UUID.randomUUID(), attacker.id(), defender.id(), territory, now,
                    now + DEFAULT_DURATION.toMillis(), WarState.ACTIVE, 0, 0);
            ClanWarStartEvent event = new ClanWarStartEvent(war);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                throw new IllegalStateException("general.error");
            }
            activeWars.put(war.id(), war);
            // Set cooldown after successful war declaration
            warCooldowns.put(cooldownKey, now);

            // Distribute war compasses
            distributeWarCompasses(war);

            return war;
        });
    }

    public CompletableFuture<Void> endWarAsync(UUID warId, WarResult result) {
        return plugin.supplySync(() -> {
            ClanWar war = activeWars.remove(warId);
            if (war != null) {
                Bukkit.getPluginManager().callEvent(new ClanWarEndEvent(war.withState(WarState.FINISHED), result));
                long reward = plugin.getConfig().getLong("leveling.war-win-exp", 1200L);
                if (result == WarResult.ATTACKER_WIN) {
                    plugin.getClanManager().getClanById(war.attackerClanId()).ifPresent(clan -> plugin.getClanManager().addExperienceAsync(clan, reward));
                    if (war.capturedBannerBy() != null) {
                        plugin.getClanManager().getClanById(war.defenderClanId()).ifPresent(defender -> {
                            plugin.getClanManager().disbandClanAsync(defender, war.capturedBannerBy());
                        });
                    }
                } else if (result == WarResult.DEFENDER_WIN) {
                    plugin.getClanManager().getClanById(war.defenderClanId()).ifPresent(clan -> plugin.getClanManager().addExperienceAsync(clan, reward));
                }
                // Remove compasses at the end of the war
                removeWarCompasses(war);
            }
            return null;
        });
    }

    public CompletableFuture<Void> peaceAsync(Clan sourceClan, Clan targetClan) {
        return plugin.supplySync(() -> {
            Optional<ClanWar> warOpt = activeWar(sourceClan.id(), targetClan.id());
            if (warOpt.isEmpty()) {
                throw new IllegalStateException("war.not-at-war");
            }

            ClanWar war = warOpt.get();
            activeWars.remove(war.id());
            Bukkit.getPluginManager().callEvent(new ClanWarEndEvent(war.withState(WarState.FINISHED), WarResult.DRAW));

            // Broadcast peace message
            for (UUID memberId : sourceClan.members().keySet()) {
                org.bukkit.entity.Player p = Bukkit.getPlayer(memberId);
                if (p != null) plugin.getMessages().send(p, "war.peace", Map.of("tag", targetClan.tag()));
            }
            for (UUID memberId : targetClan.members().keySet()) {
                org.bukkit.entity.Player p = Bukkit.getPlayer(memberId);
                if (p != null) plugin.getMessages().send(p, "war.peace", Map.of("tag", sourceClan.tag()));
            }
            // Remove compasses at the end of the war
            removeWarCompasses(war);

            return null;
        });
    }

    public boolean areAtWar(UUID firstClanId, UUID secondClanId) {
        return activeWars.values().stream().anyMatch(war -> war.state() == WarState.ACTIVE && war.between(firstClanId, secondClanId));
    }

    public Optional<ClanWar> activeWar(UUID firstClanId, UUID secondClanId) {
        return activeWars.values().stream()
                .filter(war -> war.state() == WarState.ACTIVE && war.between(firstClanId, secondClanId))
                .findFirst();
    }

    public Collection<ClanWar> activeWars() {
        return java.util.List.copyOf(activeWars.values());
    }

    public void addKillScore(UUID killerClanId, UUID victimClanId) {
        activeWar(killerClanId, victimClanId).ifPresent(war -> {
            ClanWar updated = killerClanId.equals(war.attackerClanId()) ? war.addAttackerScore(1) : war.addDefenderScore(1);
            activeWars.put(war.id(), updated);
        });
    }

    public void setBannerCapture(UUID warId, UUID playerId) {
        ClanWar war = activeWars.get(warId);
        if (war != null) {
            activeWars.put(warId, war.withBannerCapture(playerId, System.currentTimeMillis()));
        }
    }

    public void resetBannerCapture(UUID warId) {
        ClanWar war = activeWars.get(warId);
        if (war != null) {
            activeWars.put(warId, war.withBannerCapture(null, 0));
        }
    }

    public void tick() {
        long now = System.currentTimeMillis();
        // Clean up expired cooldowns (optional, but good practice)
        warCooldowns.entrySet().removeIf(entry -> now - entry.getValue() >= WAR_COOLDOWN_DURATION.toMillis());

        for (ClanWar war : activeWars.values()) {
            if (war.capturedBannerBy() != null && (now - war.bannerCapturedAt() >= BANNER_CAPTURE_DURATION.toMillis())) {
                endWarAsync(war.id(), WarResult.ATTACKER_WIN);
                continue;
            }

            if (war.endsAt() <= now) {
                WarResult result = war.attackerScore() > war.defenderScore()
                        ? WarResult.ATTACKER_WIN
                        : war.defenderScore() > war.attackerScore() ? WarResult.DEFENDER_WIN : WarResult.DRAW;
                endWarAsync(war.id(), result);
            }
        }
    }

    private void distributeWarCompasses(ClanWar war) {
        Optional<Clan> attackerClanOpt = plugin.getClanManager().getClanById(war.attackerClanId());
        Optional<Clan> defenderClanOpt = plugin.getClanManager().getClanById(war.defenderClanId());

        if (attackerClanOpt.isEmpty() || defenderClanOpt.isEmpty()) {
            plugin.getLogger().warning("Could not find one of the clans for war " + war.id());
            return;
        }

        Clan attackerClan = attackerClanOpt.get();
        Clan defenderClan = defenderClanOpt.get();

        Optional<ClanTerritory> defenderTerritoryOpt = defenderClan.territory(war.contestedTerritory());
        if (defenderTerritoryOpt.isEmpty()) {
            plugin.getLogger().warning("Defender clan " + defenderClan.name() + " does not have the claimed territory " + war.contestedTerritory());
            return;
        }

        ClanTerritory defenderTerritory = defenderTerritoryOpt.get();
        if (defenderTerritory.bannerX() == null || defenderTerritory.bannerY() == null || defenderTerritory.bannerZ() == null) {
            plugin.getLogger().warning("Defender clan " + defenderClan.name() + " territory " + war.contestedTerritory() + " has no banner coordinates set.");
            return;
        }

        // Assuming all banners are in the same world for simplicity, or we need to store world in ClanTerritory
        // For now, let's assume the main world or the world of the first online player.
        // A more robust solution would store the world in ClanTerritory.
        Location bannerLocation = new Location(
                Bukkit.getWorlds().get(0), // Placeholder: Should be the world of the territory
                defenderTerritory.bannerX(),
                defenderTerritory.bannerY(),
                defenderTerritory.bannerZ()
        );

        // Give compass to attacker's leader/guardians
        attackerClan.members().values().stream()
                .filter(member -> (member.rank() == ClanRank.LEADER || member.rank() == ClanRank.GUARDIAN))
                .map(ClanMember::playerId)
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(player -> giveTrackingCompass(player, bannerLocation, defenderClan.tag()));

        // Give compass to defender's leader/guardians
        defenderClan.members().values().stream()
                .filter(member -> (member.rank() == ClanRank.LEADER || member.rank() == ClanRank.GUARDIAN))
                .map(ClanMember::playerId)
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(player -> giveTrackingCompass(player, bannerLocation, attackerClan.tag()));
    }

    private void giveTrackingCompass(Player player, Location targetLocation, String enemyClanTag) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("War Compass - Tracking " + enemyClanTag + " Banner").color(NamedTextColor.GOLD));
            meta.lore(List.of(Component.text("Points to the enemy clan's banner during war.").color(NamedTextColor.GRAY)));
            // To make it "undroppable" or persistent, NBT data would be needed,
            // or a listener to cancel drop events for this specific item.
            // For now, it's just a regular compass with a target.
            compass.setItemMeta(meta);
        }

        player.setCompassTarget(targetLocation);

        // Give to player, if no free slot, replace first slot
        if (player.getInventory().firstEmpty() == -1) {
            player.getInventory().setItem(0, compass); // Replace first slot
        } else {
            player.getInventory().addItem(compass);
        }
        plugin.getMessages().send(player, "war.compass-given", Map.of("tag", enemyClanTag));
    }

    private void removeWarCompasses(ClanWar war) {
        Optional<Clan> attackerClanOpt = plugin.getClanManager().getClanById(war.attackerClanId());
        Optional<Clan> defenderClanOpt = plugin.getClanManager().getClanById(war.defenderClanId());

        attackerClanOpt.ifPresent(clan -> clan.members().values().stream()
                .filter(member -> (member.rank() == ClanRank.LEADER || member.rank() == ClanRank.GUARDIAN))
                .map(ClanMember::playerId)
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(this::clearWarCompass));

        defenderClanOpt.ifPresent(clan -> clan.members().values().stream()
                .filter(member -> (member.rank() == ClanRank.LEADER || member.rank() == ClanRank.GUARDIAN))
                .map(ClanMember::playerId)
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(this::clearWarCompass));
    }

    private void clearWarCompass(Player player) {
        // Reset compass target to spawn
        player.setCompassTarget(player.getWorld().getSpawnLocation());
        // Remove any "War Compass" from inventory
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.COMPASS) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    // Check if the display name contains the specific war compass text
                    if (meta.displayName().equals(Component.text("War Compass - Tracking " + player.getName() + " Banner").color(NamedTextColor.GOLD))) { // Placeholder for actual check
                        player.getInventory().setItem(i, null);
                        plugin.getMessages().send(player, "war.compass-removed");
                        break; // Assuming only one war compass per player
                    }
                }
            }
        }
    }
}