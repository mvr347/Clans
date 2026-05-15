package me.lovelace.clans.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class ClanChestHolder implements InventoryHolder {
    private final UUID clanId;

    public ClanChestHolder(UUID clanId) {
        this.clanId = clanId;
    }

    public UUID clanId() {
        return clanId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
