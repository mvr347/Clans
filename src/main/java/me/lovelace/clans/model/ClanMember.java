package me.lovelace.clans.model;

import java.util.UUID;

public record ClanMember(UUID playerId, ClanRank rank, long joinedAt, long lastSeen) {

    public ClanMember withRank(ClanRank rank) {
        return new ClanMember(playerId, rank, joinedAt, lastSeen);
    }

    public ClanMember withLastSeen(long lastSeen) {
        return new ClanMember(playerId, rank, joinedAt, lastSeen);
    }
}
