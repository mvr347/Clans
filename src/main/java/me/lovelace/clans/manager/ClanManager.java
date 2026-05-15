package me.lovelace.clans.manager;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.api.events.ClanClaimEvent;
import me.lovelace.clans.api.events.ClanCreateEvent;
import me.lovelace.clans.api.events.ClanDiplomacyChangeEvent;
import me.lovelace.clans.api.events.ClanDisbandEvent;
import me.lovelace.clans.api.events.ClanLevelUpEvent;
import me.lovelace.clans.api.events.ClanMemberJoinEvent;
import me.lovelace.clans.api.events.ClanMemberLeaveEvent;
import me.lovelace.clans.api.events.ClanRankChangeEvent;
import me.lovelace.clans.api.events.ClanUnclaimEvent;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanInvite;
import me.lovelace.clans.model.ClanMember;
import me.lovelace.clans.model.ClanRank;
import me.lovelace.clans.model.ClanTerritory;
import me.lovelace.clans.model.ClanUpgrade;
import me.lovelace.clans.model.DiplomacyRelation;
import me.lovelace.clans.model.TerritoryKey;
import me.lovelace.clans.storage.ClanStorage;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class ClanManager {
    private final ClansPlugin plugin;
    private final ClanStorage storage;
    private final Map<UUID, Clan> clansById = new ConcurrentHashMap<>();
    private final Map<String, UUID> clanByTag = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> clanByPlayer = new ConcurrentHashMap<>();
    private final Map<TerritoryKey, UUID> clanByTerritory = new ConcurrentHashMap<>();
    private final Map<UUID, List<ClanInvite>> invitesByPlayer = new ConcurrentHashMap<>();

    public ClanManager(ClansPlugin plugin, ClanStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public CompletableFuture<Void> loadAsync() {
        return storage.loadAllClansAsync().thenAccept(clans -> {
            clansById.clear();
            clanByTag.clear();
            clanByPlayer.clear();
            clanByTerritory.clear();
            for (Clan clan : clans) {
                indexClan(clan);
            }
            plugin.getLogger().info("Loaded " + clans.size() + " clans.");
        });
    }

    public Optional<Clan> getClanById(UUID clanId) {
        return Optional.ofNullable(clansById.get(clanId));
    }

    public Optional<Clan> getClanByTag(String tag) {
        if (tag == null) {
            return Optional.empty();
        }
        UUID id = clanByTag.get(normalizeTag(tag));
        return id == null ? Optional.empty() : getClanById(id);
    }

    public Optional<Clan> getPlayerClan(UUID playerId) {
        UUID clanId = clanByPlayer.get(playerId);
        return clanId == null ? Optional.empty() : getClanById(clanId);
    }

    public Optional<Clan> getClanAt(Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        return getClanAt(TerritoryKey.fromLocation(location));
    }

    public Optional<Clan> getClanAt(TerritoryKey key) {
        UUID clanId = clanByTerritory.get(key);
        return clanId == null ? Optional.empty() : getClanById(clanId);
    }

    public Collection<Clan> getAllClans() {
        return List.copyOf(clansById.values());
    }

    public CompletableFuture<Clan> createClanAsync(String name, String tag, UUID founderId) {
        return plugin.supplySync(() -> {
            validateName(name);
            validateTag(tag);
            if (getPlayerClan(founderId).isPresent()) {
                throw new IllegalStateException("clan.already-in-clan");
            }
            if (getClanByTag(tag).isPresent()) {
                throw new IllegalStateException("clan.tag-exists");
            }
            Material emblem = Material.matchMaterial(plugin.getConfig().getString("clans.default-emblem", "WHITE_BANNER"));
            if (emblem == null) {
                emblem = Material.WHITE_BANNER;
            }
            Clan clan = Clan.create(UUID.randomUUID(), name, tag, plugin.getConfig().getString("clans.default-tag-color", "<gold>"), emblem,
                    founderId, plugin.getConfig().getInt("limits.base-chest-rows", 3));
            ClanCreateEvent event = new ClanCreateEvent(clan, founderId);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                throw new IllegalStateException("general.error");
            }
            indexClan(clan);
            return clan;
        }).thenCompose(clan -> storage.saveClanAsync(clan).thenApply(ignored -> clan));
    }

    public CompletableFuture<Void> disbandClanAsync(Clan clan, UUID actorId) {
        return plugin.supplySync(() -> {
            requireRank(clan, actorId, ClanRank.GUILDMASTER);
            ClanDisbandEvent event = new ClanDisbandEvent(clan, actorId);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                throw new IllegalStateException("general.error");
            }
            for (ClanTerritory territory : clan.territories()) {
                plugin.getAdvancedClaimsHook().deleteClaim(territory.advancedClaimId());
                clanByTerritory.remove(territory.key());
            }
            unindexClan(clan);
            return null;
        }).thenCompose(ignored -> storage.deleteClanAsync(clan.id()));
    }

    public CompletableFuture<ClanInvite> invitePlayerAsync(Clan clan, UUID inviterId, UUID invitedPlayerId) {
        return plugin.supplySync(() -> {
            requireRank(clan, inviterId, ClanRank.ASSISTANT);
            if (clan.hasMember(invitedPlayerId) || getPlayerClan(invitedPlayerId).isPresent()) {
                throw new IllegalStateException("clan.already-in-clan");
            }
            if (clan.members().size() >= maxMembers(clan)) {
                throw new IllegalStateException("clan.member-limit-reached");
            }
            long expiresAt = System.currentTimeMillis() + plugin.getConfig().getLong("clans.invite-expire-seconds", 120L) * 1000L;
            ClanInvite invite = new ClanInvite(clan.id(), invitedPlayerId, inviterId, expiresAt);
            invitesByPlayer.computeIfAbsent(invitedPlayerId, ignored -> new ArrayList<>()).removeIf(old -> old.clanId().equals(clan.id()) || old.expired(System.currentTimeMillis()));
            invitesByPlayer.computeIfAbsent(invitedPlayerId, ignored -> new ArrayList<>()).add(invite);
            return invite;
        });
    }

    public CompletableFuture<Clan> acceptInviteAsync(UUID playerId, String tag) {
        return plugin.supplySync(() -> {
            Clan clan = getClanByTag(tag).orElseThrow(() -> new IllegalStateException("clan.invite-missing"));
            List<ClanInvite> invites = invitesByPlayer.getOrDefault(playerId, List.of());
            long now = System.currentTimeMillis();
            ClanInvite invite = invites.stream()
                    .filter(candidate -> candidate.clanId().equals(clan.id()) && !candidate.expired(now))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("clan.invite-missing"));
            invites.remove(invite);
            return clan;
        }).thenCompose(clan -> addMemberAsync(clan, playerId, ClanRank.MEMBER));
    }

    public CompletableFuture<Clan> addMemberAsync(Clan clan, UUID playerId, ClanRank rank) {
        return plugin.supplySync(() -> {
            if (getPlayerClan(playerId).isPresent()) {
                throw new IllegalStateException("clan.already-in-clan");
            }
            if (clan.members().size() >= maxMembers(clan)) {
                throw new IllegalStateException("clan.member-limit-reached");
            }
            clan.addMember(playerId, rank);
            ClanMember member = clan.member(playerId).orElseThrow();
            clanByPlayer.put(playerId, clan.id());
            Bukkit.getPluginManager().callEvent(new ClanMemberJoinEvent(clan, member));
            plugin.getAdvancedClaimsHook().syncPlayerTrust(clan, Bukkit.getOfflinePlayer(playerId), rank);
            return clan;
        }).thenCompose(saved -> storage.saveMemberAsync(saved.id(), saved.member(playerId).orElseThrow()).thenApply(ignored -> saved));
    }

    public CompletableFuture<Void> removeMemberAsync(Clan clan, UUID actorId, UUID playerId, boolean kicked) {
        return plugin.supplySync(() -> {
            ClanMember target = clan.member(playerId).orElseThrow(() -> new IllegalStateException("clan.not-in-clan"));
            if (kicked) {
                ClanMember actor = clan.member(actorId).orElseThrow(() -> new IllegalStateException("clan.not-in-clan"));
                if (!actor.rank().canManage(target.rank())) {
                    throw new IllegalStateException("clan.rank-too-low");
                }
            }
            if (target.rank() == ClanRank.GUILDMASTER && clan.members().size() > 1) {
                throw new IllegalStateException("clan.not-leader");
            }
            clan.removeMember(playerId);
            clanByPlayer.remove(playerId);
            plugin.getAdvancedClaimsHook().removePlayerTrust(clan, Bukkit.getOfflinePlayer(playerId));
            Bukkit.getPluginManager().callEvent(new ClanMemberLeaveEvent(clan, playerId, kicked));
            if (clan.members().isEmpty()) {
                for (ClanTerritory territory : clan.territories()) {
                    plugin.getAdvancedClaimsHook().deleteClaim(territory.advancedClaimId());
                }
                unindexClan(clan);
            }
            return clan.members().isEmpty();
        }).thenCompose(empty -> empty ? storage.deleteClanAsync(clan.id()) : storage.deleteMemberAsync(clan.id(), playerId));
    }

    public CompletableFuture<Clan> setRankAsync(Clan clan, UUID actorId, UUID playerId, ClanRank rank) {
        return plugin.supplySync(() -> {
            requireRank(clan, actorId, ClanRank.GUILDMASTER);
            ClanMember target = clan.member(playerId).orElseThrow(() -> new IllegalStateException("clan.not-in-clan"));
            if (target.rank() == ClanRank.GUILDMASTER || rank == ClanRank.GUILDMASTER) {
                throw new IllegalStateException("clan.not-leader");
            }
            ClanRank oldRank = target.rank();
            clan.setRank(playerId, rank);
            Bukkit.getPluginManager().callEvent(new ClanRankChangeEvent(clan, playerId, oldRank, rank));
            plugin.getAdvancedClaimsHook().syncPlayerTrust(clan, Bukkit.getOfflinePlayer(playerId), rank);
            return clan;
        }).thenCompose(saved -> storage.saveMemberAsync(saved.id(), saved.member(playerId).orElseThrow()).thenApply(ignored -> saved));
    }

    public CompletableFuture<ClanTerritory> claimTerritoryAsync(Clan clan, Chunk chunk, Player actor) {
        return plugin.supplySync(() -> {
            requireRank(clan, actor.getUniqueId(), ClanRank.ASSISTANT);
            if (clan.territories().size() >= maxTerritories(clan)) {
                throw new IllegalStateException("territory.limit-reached");
            }
            TerritoryKey key = TerritoryKey.fromChunk(chunk);
            Optional<Clan> existing = getClanAt(key);
            if (existing.isPresent()) {
                throw new IllegalStateException("territory.already-claimed");
            }
            ClanTerritory territory = new ClanTerritory(clan.id(), key, null, actor.getUniqueId(), System.currentTimeMillis());
            ClanClaimEvent event = new ClanClaimEvent(clan, territory, actor.getUniqueId());
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                throw new IllegalStateException("general.error");
            }
            UUID claimId = plugin.getAdvancedClaimsHook().createOrAttachClaim(clan, territory).orElse(null);
            ClanTerritory saved = territory.withAdvancedClaimId(claimId);
            clan.addTerritory(saved);
            clanByTerritory.put(key, clan.id());
            return saved;
        }).thenCompose(territory -> storage.saveTerritoryAsync(territory)
                .thenCompose(ignored -> addExperienceAsync(clan, plugin.getConfig().getLong("leveling.territory-claim-exp", 150L)))
                .thenApply(ignored -> territory));
    }

    public CompletableFuture<Void> unclaimTerritoryAsync(Clan clan, TerritoryKey key, UUID actorId) {
        return plugin.supplySync(() -> {
            requireRank(clan, actorId, ClanRank.ASSISTANT);
            ClanTerritory territory = clan.territory(key).orElseThrow(() -> new IllegalStateException("territory.not-claimed"));
            plugin.getAdvancedClaimsHook().deleteClaim(territory.advancedClaimId());
            clan.removeTerritory(key);
            clanByTerritory.remove(key);
            Bukkit.getPluginManager().callEvent(new ClanUnclaimEvent(clan, territory, actorId));
            return territory;
        }).thenCompose(territory -> storage.deleteTerritoryAsync(territory.key()));
    }

    public CompletableFuture<Clan> setDiplomacyAsync(Clan source, Clan target, DiplomacyRelation relation, UUID actorId) {
        return plugin.supplySync(() -> {
            requireRank(source, actorId, ClanRank.ASSISTANT);
            if (source.id().equals(target.id())) {
                throw new IllegalStateException("general.error");
            }
            source.setDiplomacy(target.id(), relation);
            Bukkit.getPluginManager().callEvent(new ClanDiplomacyChangeEvent(source.id(), target.id(), relation));
            return source;
        }).thenCompose(clan -> storage.saveDiplomacyAsync(clan.id(), target.id(), relation).thenApply(ignored -> clan));
    }

    public CompletableFuture<Clan> addExperienceAsync(Clan clan, long amount) {
        return plugin.supplySync(() -> {
            int maxLevel = plugin.getConfig().getInt("limits.max-level", 20);
            int oldLevel = clan.level();
            clan.addExperience(amount);
            while (clan.level() < maxLevel && clan.experience() >= experienceForLevel(clan.level() + 1)) {
                clan.levelUp();
                Bukkit.getPluginManager().callEvent(new ClanLevelUpEvent(clan, clan.level() - 1, clan.level()));
            }
            if (oldLevel != clan.level()) {
                clan.setUpgradeLevel(ClanUpgrade.SPIRIT, Math.max(clan.upgradeLevel(ClanUpgrade.SPIRIT), clan.level() / 4));
            }
            return clan;
        }).thenCompose(storage::saveClanAsync).thenApply(ignored -> clan);
    }

    public void updateLastSeen(UUID playerId, long timestamp) {
        getPlayerClan(playerId).ifPresent(clan -> {
            clan.markSeen(playerId, timestamp);
            clan.member(playerId).ifPresent(member -> storage.saveMemberAsync(clan.id(), member));
        });
    }

    public int maxMembers(Clan clan) {
        return plugin.getConfig().getInt("limits.base-members", 10)
                + clan.upgradeLevel(ClanUpgrade.MEMBERS) * plugin.getConfig().getInt("limits.members-per-upgrade", 3);
    }

    public int maxTerritories(Clan clan) {
        return plugin.getConfig().getInt("limits.base-territories", 4) + clan.level() * plugin.getConfig().getInt("limits.territories-per-level", 1);
    }

    public long experienceForLevel(int level) {
        if (level <= 1) {
            return 0L;
        }
        double base = plugin.getConfig().getDouble("leveling.base-exp", 1000D);
        double multiplier = plugin.getConfig().getDouble("leveling.multiplier", 1.35D);
        return Math.round(base * Math.pow(multiplier, level - 2));
    }

    public List<Clan> topClansByLevel(int limit) {
        return clansById.values().stream()
                .sorted(Comparator.comparingInt(Clan::level).thenComparingLong(Clan::experience).reversed())
                .limit(limit)
                .toList();
    }

    private void indexClan(Clan clan) {
        clansById.put(clan.id(), clan);
        clanByTag.put(normalizeTag(clan.tag()), clan.id());
        for (UUID playerId : clan.members().keySet()) {
            clanByPlayer.put(playerId, clan.id());
        }
        for (ClanTerritory territory : clan.territories()) {
            clanByTerritory.put(territory.key(), clan.id());
        }
    }

    private void unindexClan(Clan clan) {
        clansById.remove(clan.id());
        clanByTag.remove(normalizeTag(clan.tag()));
        for (UUID playerId : clan.members().keySet()) {
            clanByPlayer.remove(playerId);
        }
        for (ClanTerritory territory : clan.territories()) {
            clanByTerritory.remove(territory.key());
        }
    }

    private void requireRank(Clan clan, UUID playerId, ClanRank minimum) {
        ClanMember member = clan.member(playerId).orElseThrow(() -> new IllegalStateException("clan.not-in-clan"));
        if (!member.rank().atLeast(minimum)) {
            throw new IllegalStateException("clan.rank-too-low");
        }
    }

    private void validateTag(String tag) {
        int min = plugin.getConfig().getInt("clans.tag.min-length", 2);
        int max = plugin.getConfig().getInt("clans.tag.max-length", 6);
        String pattern = plugin.getConfig().getString("clans.tag.pattern", "^[A-Za-z0-9_]+$");
        if (tag == null || tag.length() < min || tag.length() > max || !Pattern.compile(pattern).matcher(tag).matches()) {
            throw new IllegalStateException("clan.invalid-tag");
        }
    }

    private void validateName(String name) {
        int min = plugin.getConfig().getInt("clans.name.min-length", 3);
        int max = plugin.getConfig().getInt("clans.name.max-length", 32);
        if (name == null || name.length() < min || name.length() > max) {
            throw new IllegalStateException("clan.invalid-name");
        }
    }

    private String normalizeTag(String tag) {
        return tag.toLowerCase(Locale.ROOT);
    }
}
