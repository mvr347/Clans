package me.lovelace.clans.model;

import java.util.UUID;

public record ClanMember(UUID playerId, ClanRank rank, long joinedAt, long lastSeen, int contribution) {
    public ClanMember(UUID playerId, ClanRank rank, long joinedAt, long lastSeen) {
        this(playerId, rank, joinedAt, lastSeen, 0);
    }

    public ClanMember withRank(ClanRank rank) {
        return new ClanMember(playerId, rank, joinedAt, lastSeen, contribution);
    }

    public ClanMember withLastSeen(long lastSeen) {
        return new ClanMember(playerId, rank, joinedAt, lastSeen, contribution);
    }
    
    public ClanMember withContribution(int contribution) {
        return new ClanMember(playerId, rank, joinedAt, lastSeen, contribution);
    }
}