package me.lovelace.clans.model;

public enum ClanRank {
    RECRUIT(0, "Новичок"),
    MEMBER(1, "Со-клановец"),
    GUARDIAN(2, "Хранитель"),
    LEADER(3, "Глава");

    private final int weight;
    private final String displayName;

    ClanRank(int weight, String displayName) {
        this.weight = weight;
        this.displayName = displayName;
    }

    public int weight() {
        return weight;
    }

    public String displayName() {
        return displayName;
    }

    public boolean atLeast(ClanRank rank) {
        return weight >= rank.weight;
    }

    public boolean canManage(ClanRank target) {
        return this == LEADER || (this.weight > target.weight);
    }

    public ClanRank nextRank() {
        ClanRank[] ranks = ClanRank.values();
        int ordinal = this.ordinal();
        if (ordinal < ranks.length - 1) {
            return ranks[ordinal + 1];
        }
        return null; // Already the highest rank
    }

    public ClanRank previousRank() {
        ClanRank[] ranks = ClanRank.values();
        int ordinal = this.ordinal();
        if (ordinal > 0) {
            return ranks[ordinal - 1];
        }
        return null; // Already the lowest rank
    }
}