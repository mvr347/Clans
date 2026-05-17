package me.lovelace.clans.model;

import java.util.UUID;

public record ClanTerritory(
        UUID clanId,
        TerritoryKey key,
        UUID advancedClaimId,
        UUID claimedBy,
        long claimedAt,
        Integer bannerX,
        Integer bannerY,
        Integer bannerZ,
        String name,
        boolean pvp
) {
    public ClanTerritory(UUID clanId, TerritoryKey key, UUID advancedClaimId, UUID claimedBy, long claimedAt) {
        this(clanId, key, advancedClaimId, claimedBy, claimedAt, null, null, null, null, false);
    }

    public ClanTerritory withAdvancedClaimId(UUID advancedClaimId) {
        return new ClanTerritory(clanId, key, advancedClaimId, claimedBy, claimedAt, bannerX, bannerY, bannerZ, name, pvp);
    }

    public ClanTerritory withBannerCoords(Integer x, Integer y, Integer z) {
        return new ClanTerritory(clanId, key, advancedClaimId, claimedBy, claimedAt, x, y, z, name, pvp);
    }
    
    public ClanTerritory withName(String name) {
        return new ClanTerritory(clanId, key, advancedClaimId, claimedBy, claimedAt, bannerX, bannerY, bannerZ, name, pvp);
    }
    
    public ClanTerritory withPvp(boolean pvp) {
        return new ClanTerritory(clanId, key, advancedClaimId, claimedBy, claimedAt, bannerX, bannerY, bannerZ, name, pvp);
    }
}