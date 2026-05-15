package me.lovelace.clans.api.events;

import me.lovelace.clans.model.Clan;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public final class ClanMemberLeaveEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Clan clan;
    private final UUID playerId;
    private final boolean kicked;

    public ClanMemberLeaveEvent(Clan clan, UUID playerId, boolean kicked) {
        this.clan = clan;
        this.playerId = playerId;
        this.kicked = kicked;
    }

    public Clan clan() {
        return clan;
    }

    public UUID playerId() {
        return playerId;
    }

    public boolean kicked() {
        return kicked;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
