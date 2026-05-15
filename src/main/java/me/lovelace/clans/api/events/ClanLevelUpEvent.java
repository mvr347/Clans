package me.lovelace.clans.api.events;

import me.lovelace.clans.model.Clan;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class ClanLevelUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Clan clan;
    private final int oldLevel;
    private final int newLevel;

    public ClanLevelUpEvent(Clan clan, int oldLevel, int newLevel) {
        this.clan = clan;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public Clan clan() {
        return clan;
    }

    public int oldLevel() {
        return oldLevel;
    }

    public int newLevel() {
        return newLevel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
