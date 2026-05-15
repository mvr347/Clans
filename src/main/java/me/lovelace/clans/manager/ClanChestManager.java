package me.lovelace.clans.manager;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.gui.ClanChestHolder;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.util.InventorySerialization;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClanChestManager {
    private final ClansPlugin plugin;
    private final Map<UUID, Inventory> openInventories = new ConcurrentHashMap<>();

    public ClanChestManager(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void openChest(org.bukkit.entity.Player player, Clan clan) {
        Inventory cached = openInventories.get(clan.id());
        if (cached != null) {
            player.openInventory(cached);
            return;
        }
        plugin.getStorage().loadClanChestAsync(clan.id()).thenAccept(optional -> plugin.runSync(() -> {
            int rows = Math.max(1, Math.min(6, clan.chestRows()));
            int size = rows * 9;
            Component title = plugin.getMessages().component("gui.chest-title", Map.of("tag", clan.tag()), player);
            Inventory inventory = Bukkit.createInventory(new ClanChestHolder(clan.id()), size, title);
            optional.ifPresent(bytes -> inventory.setContents(InventorySerialization.deserialize(bytes, size)));
            openInventories.put(clan.id(), inventory);
            player.openInventory(inventory);
        }));
    }

    public void saveIfClanChest(Inventory inventory) {
        if (!(inventory.getHolder() instanceof ClanChestHolder holder)) {
            return;
        }
        if (inventory.getViewers().size() > 1) {
            return;
        }
        byte[] bytes = InventorySerialization.serialize(inventory);
        plugin.getStorage().saveClanChestAsync(holder.clanId(), bytes);
        openInventories.remove(holder.clanId());
    }
}
