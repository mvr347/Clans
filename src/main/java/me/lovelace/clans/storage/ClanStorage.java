package me.lovelace.clans.storage;

import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanApplication;
import me.lovelace.clans.model.ClanMember;
import me.lovelace.clans.model.ClanTerritory;
import me.lovelace.clans.model.ClanUpgrade;
import me.lovelace.clans.model.DiplomacyRelation;
import me.lovelace.clans.model.TerritoryKey;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ClanStorage {
    CompletableFuture<Collection<Clan>> loadAllClansAsync();

    CompletableFuture<Void> saveClanAsync(Clan clan);

    CompletableFuture<Void> deleteClanAsync(UUID clanId);

    CompletableFuture<Void> saveMemberAsync(UUID clanId, ClanMember member);

    CompletableFuture<Void> deleteMemberAsync(UUID clanId, UUID playerId);

    CompletableFuture<Void> saveTerritoryAsync(ClanTerritory territory);

    CompletableFuture<Void> deleteTerritoryAsync(TerritoryKey key);

    CompletableFuture<Void> saveDiplomacyAsync(UUID sourceClanId, UUID targetClanId, DiplomacyRelation relation);

    CompletableFuture<Void> saveUpgradeAsync(UUID clanId, ClanUpgrade upgrade, int level);

    // Clan Applications
    CompletableFuture<Collection<ClanApplication>> loadAllApplicationsAsync();

    CompletableFuture<Void> saveApplicationAsync(ClanApplication application);

    CompletableFuture<Void> deleteApplicationAsync(UUID clanId, UUID applicantId);

    CompletableFuture<Void> deleteAllApplicationsForClanAsync(UUID clanId);
}