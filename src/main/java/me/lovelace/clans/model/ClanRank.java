package me.lovelace.clans.model;

public enum ClanRank {
    MEMBER(0, "Member"),
    ASSISTANT(1, "Assistant"),
    GUILDMASTER(2, "Guildmaster");

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
        return this == GUILDMASTER || (this == ASSISTANT && target == MEMBER);
    }
}
