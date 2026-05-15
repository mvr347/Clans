package me.lovelace.clans.api;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanRank;
import me.lovelace.clans.model.ClanTerritory;
import me.lovelace.clans.model.DiplomacyRelation;
import me.lovelace.clans.model.TerritoryKey;
import me.lovelace.clans.model.artifact.ArtifactType;
import me.lovelace.clans.model.ritual.ClanRitual;
import me.lovelace.clans.model.ritual.RitualType;
import me.lovelace.clans.model.war.ClanWar;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public integration surface for Clans.
 *
 * <p>The API mirrors the style of singleton Bukkit plugin APIs: cache reads are synchronous,
 * state-changing operations are asynchronous and return {@link CompletableFuture}.</p>
 */
public final class ClansAPI {
    private static ClansAPI instance;

    private final ClansPlugin plugin;

    private ClansAPI(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public static void init(ClansPlugin plugin) {
        instance = new ClansAPI(plugin);
    }

    public static void shutdown() {
        instance = null;
    }

    public static ClansAPI getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ClansAPI is not initialized");
        }
        return instance;
    }

    public ClansPlugin getPlugin() {
        return plugin;
    }

    public Optional<Clan> getClanById(UUID clanId) {
        return plugin.getClanManager().getClanById(clanId);
    }

    public Optional<Clan> getClanByTag(String tag) {
        return plugin.getClanManager().getClanByTag(tag);
    }

    public Optional<Clan> getPlayerClan(UUID playerId) {
        return plugin.getClanManager().getPlayerClan(playerId);
    }

    public Optional<Clan> getPlayerClan(OfflinePlayer player) {
        return getPlayerClan(player.getUniqueId());
    }

    public Optional<Clan> getClanAt(Location location) {
        return plugin.getClanManager().getClanAt(location);
    }

    public Collection<Clan> getAllClans() {
        return plugin.getClanManager().getAllClans();
    }

    public boolean isInClan(UUID playerId) {
        return getPlayerClan(playerId).isPresent();
    }

    public boolean isInClan(OfflinePlayer player) {
        return isInClan(player.getUniqueId());
    }

    public CompletableFuture<Clan> createClanAsync(String name, String tag, Player founder) {
        return createClanAsync(name, tag, founder.getUniqueId());
    }

    public CompletableFuture<Clan> createClanAsync(String name, String tag, UUID founderId) {
        return plugin.getClanManager().createClanAsync(name, tag, founderId);
    }

    public CompletableFuture<Void> disbandClanAsync(Clan clan, UUID actorId) {
        return plugin.getClanManager().disbandClanAsync(clan, actorId);
    }

    public CompletableFuture<Clan> invitePlayerAsync(Clan clan, UUID inviterId, UUID targetId) {
        return plugin.getClanManager().invitePlayerAsync(clan, inviterId, targetId).thenApply(invite -> clan);
    }

    public CompletableFuture<Clan> acceptInviteAsync(UUID playerId, String clanTag) {
        return plugin.getClanManager().acceptInviteAsync(playerId, clanTag);
    }

    public CompletableFuture<Clan> addMemberAsync(Clan clan, UUID playerId, ClanRank rank) {
        return plugin.getClanManager().addMemberAsync(clan, playerId, rank);
    }

    public CompletableFuture<Void> removeMemberAsync(Clan clan, UUID actorId, UUID playerId, boolean kicked) {
        return plugin.getClanManager().removeMemberAsync(clan, actorId, playerId, kicked);
    }

    public CompletableFuture<Clan> setRankAsync(Clan clan, UUID actorId, UUID playerId, ClanRank rank) {
        return plugin.getClanManager().setRankAsync(clan, actorId, playerId, rank);
    }

    public CompletableFuture<ClanTerritory> claimChunkAsync(Clan clan, Chunk chunk, Player actor) {
        return plugin.getClanManager().claimTerritoryAsync(clan, chunk, actor);
    }

    public CompletableFuture<Void> unclaimChunkAsync(Clan clan, TerritoryKey key, UUID actorId) {
        return plugin.getClanManager().unclaimTerritoryAsync(clan, key, actorId);
    }

    public CompletableFuture<Clan> addExperienceAsync(Clan clan, long amount) {
        return plugin.getClanManager().addExperienceAsync(clan, amount);
    }

    public CompletableFuture<ClanWar> startWarAsync(Clan attacker, Clan defender, TerritoryKey contestedTerritory) {
        return plugin.getWarManager().startWarAsync(attacker, defender, contestedTerritory);
    }

    public boolean isAtWar(Clan first, Clan second) {
        return plugin.getWarManager().areAtWar(first.id(), second.id());
    }

    public CompletableFuture<ClanRitual> startRitualAsync(Clan clan, UUID actorId, RitualType type) {
        return plugin.getRitualManager().startRitualAsync(clan, actorId, type);
    }

    public CompletableFuture<Clan> setDiplomacyAsync(Clan source, Clan target, DiplomacyRelation relation, UUID actorId) {
        return plugin.getClanManager().setDiplomacyAsync(source, target, relation, actorId);
    }

    public DiplomacyRelation getRelation(Clan source, Clan target) {
        return source.relationTo(target.id());
    }

    public ItemStack createArtifact(ArtifactType type) {
        return plugin.getArtifactManager().createArtifact(type);
    }
}
