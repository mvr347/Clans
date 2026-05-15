package me.lovelace.clans.model.ritual;

import java.util.UUID;

public record WarDrumRitual(UUID clanId, long startedAt, long endsAt) implements ClanRitual {
    @Override
    public RitualType type() {
        return RitualType.WAR_DRUM;
    }
}
