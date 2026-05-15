package me.lovelace.clans.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class ClanMenuHolder implements InventoryHolder {
    private final ClanMenuType type;
    private final UUID clanId;

    public ClanMenuHolder(ClanMenuType type, UUID clanId) {
        this.type = type;
        this.clanId = clanId;
    }

    public ClanMenuType type() {
        return type;
    }

    public UUID clanId() {
        return clanId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
