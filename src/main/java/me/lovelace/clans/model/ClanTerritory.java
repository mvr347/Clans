package me.lovelace.clans.model;

import java.util.UUID;

public record ClanTerritory(
        UUID clanId,
        TerritoryKey key,
        UUID advancedClaimId,
        UUID claimedBy,
        long claimedAt
) {

    public ClanTerritory withAdvancedClaimId(UUID advancedClaimId) {
        return new ClanTerritory(clanId, key, advancedClaimId, claimedBy, claimedAt);
    }
}
