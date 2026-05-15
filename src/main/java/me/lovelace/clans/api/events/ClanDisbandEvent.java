package me.lovelace.clans.api.events;

import me.lovelace.clans.model.Clan;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public final class ClanDisbandEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Clan clan;
    private final UUID actorId;
    private boolean cancelled;

    public ClanDisbandEvent(Clan clan, UUID actorId) {
        this.clan = clan;
        this.actorId = actorId;
    }

    public Clan clan() {
        return clan;
    }

    public UUID actorId() {
        return actorId;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
