package me.lovelace.clans.api.events;

import me.lovelace.clans.model.DiplomacyRelation;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public final class ClanDiplomacyChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID sourceClanId;
    private final UUID targetClanId;
    private final DiplomacyRelation relation;

    public ClanDiplomacyChangeEvent(UUID sourceClanId, UUID targetClanId, DiplomacyRelation relation) {
        this.sourceClanId = sourceClanId;
        this.targetClanId = targetClanId;
        this.relation = relation;
    }

    public UUID sourceClanId() {
        return sourceClanId;
    }

    public UUID targetClanId() {
        return targetClanId;
    }

    public DiplomacyRelation relation() {
        return relation;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
