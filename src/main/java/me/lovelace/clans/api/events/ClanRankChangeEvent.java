package me.lovelace.clans.api.events;

import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanRank;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public final class ClanRankChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Clan clan;
    private final UUID playerId;
    private final ClanRank oldRank;
    private final ClanRank newRank;

    public ClanRankChangeEvent(Clan clan, UUID playerId, ClanRank oldRank, ClanRank newRank) {
        this.clan = clan;
        this.playerId = playerId;
        this.oldRank = oldRank;
        this.newRank = newRank;
    }

    public Clan clan() {
        return clan;
    }

    public UUID playerId() {
        return playerId;
    }

    public ClanRank oldRank() {
        return oldRank;
    }

    public ClanRank newRank() {
        return newRank;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
