package me.lovelace.clans.api.events;

import me.lovelace.clans.model.war.ClanWar;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class ClanWarStartEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ClanWar war;
    private boolean cancelled;

    public ClanWarStartEvent(ClanWar war) {
        this.war = war;
    }

    public ClanWar war() {
        return war;
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
