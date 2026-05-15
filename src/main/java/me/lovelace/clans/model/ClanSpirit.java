package me.lovelace.clans.model;

public record ClanSpirit(int level, long energy, long awakenedUntil) {

    public static ClanSpirit fresh() {
        return new ClanSpirit(1, 0L, 0L);
    }

    public boolean awakened(long now) {
        return awakenedUntil > now;
    }

    public ClanSpirit withLevel(int level) {
        return new ClanSpirit(level, energy, awakenedUntil);
    }

    public ClanSpirit addEnergy(long amount) {
        return new ClanSpirit(level, Math.max(0L, energy + amount), awakenedUntil);
    }

    public ClanSpirit awakenUntil(long timestamp) {
        return new ClanSpirit(level, energy, timestamp);
    }
}
