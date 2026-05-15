package me.lovelace.clans.model.ritual;

public enum RitualType {
    HARVEST("Harvest", 250),
    WAR_DRUM("War Drum", 350),
    GUARDIAN("Guardian", 300);

    private final String displayName;
    private final long experienceReward;

    RitualType(String displayName, long experienceReward) {
        this.displayName = displayName;
        this.experienceReward = experienceReward;
    }

    public String displayName() {
        return displayName;
    }

    public long experienceReward() {
        return experienceReward;
    }
}
