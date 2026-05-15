package me.lovelace.clans.api.events;

import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanTerritory;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public final class ClanClaimEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Clan clan;
    private final ClanTerritory territory;
    private final UUID actorId;
    private boolean cancelled;

    public ClanClaimEvent(Clan clan, ClanTerritory territory, UUID actorId) {
        this.clan = clan;
        this.territory = territory;
        this.actorId = actorId;
    }

    public Clan clan() {
        return clan;
    }

    public ClanTerritory territory() {
        return territory;
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
