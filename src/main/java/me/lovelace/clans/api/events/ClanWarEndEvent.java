package me.lovelace.clans.api.events;

import me.lovelace.clans.model.war.ClanWar;
import me.lovelace.clans.model.war.WarResult;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class ClanWarEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ClanWar war;
    private final WarResult result;

    public ClanWarEndEvent(ClanWar war, WarResult result) {
        this.war = war;
        this.result = result;
    }

    public ClanWar war() {
        return war;
    }

    public WarResult result() {
        return result;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
