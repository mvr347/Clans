package me.lovelace.clans.model;

public enum ClanUpgrade {
    MEMBERS("Больше участников"),
    TERRITORIES("Больше территорий"),
    LOOTING("Бонус добычи"),
    SPIRIT("Дух клана"),
    WARFARE("Военные перки");

    private final String displayName;

    ClanUpgrade(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
