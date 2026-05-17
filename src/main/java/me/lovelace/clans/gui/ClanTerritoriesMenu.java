package me.lovelace.clans.gui;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanRank;
import me.lovelace.clans.model.ClanTerritory;
import me.lovelace.clans.model.TerritoryKey;
import me.lovelace.clans.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;

public final class ClanTerritoriesMenu {
    private final ClansPlugin plugin;

    public ClanTerritoriesMenu(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        List<ClanTerritory> territories = clan.territories().stream().toList();

        int numTerritories = territories.size();
        // Calculate required rows for territories, each row holds 7 territories (leaving 1 slot on each side for padding)
        // Plus 2 rows for top/bottom control rows.
        int contentRows = (int) Math.ceil(numTerritories / 7.0);
        if (contentRows == 0) contentRows = 1; // At least one content row for the "empty" message
        int inventorySize = Math.max(27, Math.min(54, (contentRows + 2) * 9)); // Min 3 rows, max 6 rows

        Inventory inventory = Bukkit.createInventory(new ClanMenuHolder(ClanMenuType.TERRITORIES, clan.id()), inventorySize,
                plugin.getMessages().component("gui.territories-title", Map.of("tag", clan.tag()), player));

        fillGlass(inventory);

        if (territories.isEmpty()) {
            inventory.setItem(inventorySize / 2, ItemBuilder.of(Material.BARRIER) // Centered dynamically
                    .name(plugin.getMessages().component("gui.territories.empty.name", player))
                    .lore(plugin.getMessages().component("gui.territories.empty.lore", player))
                    .build());
        } else {
            // Start placing from slot 10 (second row, second slot)
            for (int i = 0; i < numTerritories; i++) {
                ClanTerritory territory = territories.get(i);
                int centerX = territory.key().chunkX() * 16 + 8;
                int centerZ = territory.key().chunkZ() * 16 + 8;

                // Calculate slot dynamically, skipping first and last slot of each row for padding
                int row = i / 7;
                int col = i % 7;
                int slot = 9 * (row + 1) + 1 + col; // (row+1) for content rows, +1 for left padding

                inventory.setItem(slot, ItemBuilder.head(ItemBuilder.HEAD_MAP)
                        .name(plugin.getMessages().component("gui.territories.item.name",
                                Map.of("world", territory.key().world()), player))
                        .lore(plugin.getMessages().component("gui.territories.item.coords",
                                Map.of(
                                        "x", String.valueOf(centerX),
                                        "z", String.valueOf(centerZ),
                                        "cx", String.valueOf(territory.key().chunkX()),
                                        "cz", String.valueOf(territory.key().chunkZ())
                                ), player))
                        .lore(plugin.getMessages().component("gui.territories.item.click-teleport", player))
                        .mutate(meta -> meta.getPersistentDataContainer().set(
                                plugin.getGuiManager().memberKey(),
                                PersistentDataType.STRING,
                                territory.key().world() + ";" + territory.key().chunkX() + ";" + territory.key().chunkZ()
                        ))
                        .build());
            }
        }

        boolean canManage = clan.member(player.getUniqueId())
                .map(m -> m.rank().atLeast(ClanRank.GUARDIAN))
                .orElse(false);

        boolean alreadyHasBanner = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            if (item.getItemMeta().getPersistentDataContainer().has(plugin.getGuiManager().memberKey(), PersistentDataType.STRING)) {
                alreadyHasBanner = true;
                break;
            }
        }

        boolean hasInstalledBanner = clan.territories().stream().anyMatch(t -> t.bannerX() != null);

        // Place control buttons in the last row
        inventory.setItem(inventorySize - 5, ItemBuilder.head(ItemBuilder.HEAD_BACK) // Back button
                .name(plugin.getMessages().component("gui.back", player))
                .build());
                
        if (canManage && !alreadyHasBanner && !hasInstalledBanner) {
            inventory.setItem(inventorySize - 4, ItemBuilder.of(Material.WHITE_BANNER) // Banner button
                    .name(plugin.getMessages().component("gui.territories.banner.name", player))
                    .lore(plugin.getMessages().component("gui.territories.banner.lore", player))
                    .build());
        }

        player.openInventory(inventory);
    }

    public void handleTerritoryClick(Player player, Clan clan, int slot, boolean isRightClick) {
        int inventorySize = player.getOpenInventory().getTopInventory().getSize();
        if (slot == inventorySize - 5) { // Back button
            plugin.getGuiManager().openMain(player, clan);
            return;
        }

        if (slot == inventorySize - 4) { // Banner button
            boolean canManage = clan.member(player.getUniqueId())
                    .map(m -> m.rank().atLeast(ClanRank.GUARDIAN))
                    .orElse(false);
            if (canManage) {
                // Give banner logic here
                ItemStack banner = ItemBuilder.of(Material.WHITE_BANNER)
                        .name(plugin.getMessages().component("item.claim-banner.name", player))
                        .lore(plugin.getMessages().component("item.claim-banner.lore", player))
                        .mutate(meta -> meta.getPersistentDataContainer().set(
                                plugin.getGuiManager().memberKey(),
                                PersistentDataType.STRING,
                                clan.id().toString()
                        ))
                        .build();
                player.getInventory().addItem(banner);
                player.closeInventory();
                plugin.getMessages().send(player, "territory.banner-given");
            }
            return;
        }

        // Check if it's a territory item slot
        // Content slots are from row 1 (slot 9) to row (contentRows + 1) (slot inventorySize - 9 - 1)
        // And within each row, from slot +1 to +7 (excluding first and last for padding)
        boolean isContentSlot = slot >= 9 && slot < inventorySize - 9;
        if (!isContentSlot) return;

        // Further check if it's within the 7-slot content area of a row
        if ((slot % 9 == 0) || (slot % 9 == 8)) return; // Exclude first and last column (padding)

        org.bukkit.inventory.ItemStack item = player.getOpenInventory().getTopInventory().getItem(slot);
        if (item == null || !item.hasItemMeta()) return;

        String data = item.getItemMeta().getPersistentDataContainer()
                .get(plugin.getGuiManager().memberKey(), PersistentDataType.STRING);
        if (data == null) return;

        String[] parts = data.split(";");
        if (parts.length != 3) return;

        try {
            String worldName = parts[0];
            int chunkX = Integer.parseInt(parts[1]);
            int chunkZ = Integer.parseInt(parts[2]);
            
            if (isRightClick) {
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getMessages().send(player, "territory.world-not-found");
                    return;
                }
                int centerX = chunkX * 16 + 8;
                int centerZ = chunkZ * 16 + 8;
                int y = world.getHighestBlockYAt(centerX, centerZ) + 1;
                player.closeInventory();
                player.teleport(new Location(world, centerX + 0.5, y, centerZ + 0.5));
                plugin.getMessages().send(player, "territory.teleported");
            } else {
                // Open territory settings
                TerritoryKey key = new TerritoryKey(worldName, chunkX, chunkZ);
                clan.territory(key).ifPresent(territory -> {
                    plugin.getGuiManager().openTerritorySettings(player, clan, territory);
                });
            }
        } catch (NumberFormatException ignored) {}
    }

    private void fillGlass(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            // Only fill empty slots, do not overwrite existing items
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                        .name(Component.empty())
                        .build());
            }
        }
    }
}