package me.lovelace.clans.gui;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanRank;
import me.lovelace.clans.model.ClanTerritory;
import me.lovelace.clans.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.Optional;

public class ClanCapitalManagementMenu implements InventoryHolder {
    private final ClansPlugin plugin;
    private final Clan clan;
    private final Player player;
    private Inventory inventory;

    public ClanCapitalManagementMenu(ClansPlugin plugin, Clan clan, Player player) {
        this.plugin = plugin;
        this.clan = clan;
        this.player = player;
    }

    public void open() {
        this.inventory = Bukkit.createInventory(this, 27, plugin.getMessages().component("gui.capital.title", Map.of("clan", clan.name()), player));

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build());
        }

        boolean isAtWar = plugin.getWarManager().isAtWar(clan.id());
        boolean isLeader = clan.member(player.getUniqueId()).map(m -> m.rank() == ClanRank.LEADER).orElse(false);
        boolean isAssistant = clan.member(player.getUniqueId()).map(m -> m.rank() == ClanRank.GUARDIAN).orElse(false);
        boolean canManage = isLeader || isAssistant;

        Optional<ClanTerritory> capitalTerritoryOpt = clan.territories().stream().filter(ClanTerritory::isCapital).findFirst();
        if (capitalTerritoryOpt.isEmpty()) {
            // Should not happen if this menu is opened, but for safety
            player.closeInventory();
            plugin.getMessages().send(player, "territory.capital.not-found");
            return;
        }
        ClanTerritory capital = capitalTerritoryOpt.get();

        // Slot 10: Move Home Point
        inventory.setItem(10, ItemBuilder.of(Material.COMPASS)
                .name(plugin.getMessages().component("gui.capital.move-home.name", player))
                .lore(plugin.getMessages().component("gui.capital.move-home.lore", player))
                .build());

        // Slot 12: Transfer Capital Base
        ItemStack transferItem;
        if (isAtWar) {
            transferItem = ItemBuilder.of(Material.RED_CONCRETE)
                    .name(plugin.getMessages().component("gui.capital.transfer.name", player))
                    .lore(plugin.getMessages().component("gui.capital.war-blocked", player))
                    .build();
        } else {
            transferItem = ItemBuilder.of(Material.ENDER_CHEST)
                    .name(plugin.getMessages().component("gui.capital.transfer.name", player))
                    .lore(plugin.getMessages().component("gui.capital.transfer.lore", player))
                    .build();
        }
        inventory.setItem(12, transferItem);

        // Slot 14: Disband Capital Base
        ItemStack disbandItem;
        if (isAtWar) {
            disbandItem = ItemBuilder.of(Material.RED_CONCRETE)
                    .name(plugin.getMessages().component("gui.capital.disband.name", player))
                    .lore(plugin.getMessages().component("gui.capital.war-blocked", player))
                    .build();
        } else {
            disbandItem = ItemBuilder.of(Material.LAVA_BUCKET)
                    .name(plugin.getMessages().component("gui.capital.disband.name", player))
                    .lore(plugin.getMessages().component("gui.capital.disband.lore", player))
                    .build();
        }
        inventory.setItem(14, disbandItem);

        // Back button (bottom center)
        inventory.setItem(22, ItemBuilder.head(ItemBuilder.HEAD_BACK)
                .name(plugin.getMessages().component("gui.back", player))
                .build());

        player.openInventory(inventory);
    }

    public void handleInventoryClick(Player clicker, int slot) {
        boolean isAtWar = plugin.getWarManager().isAtWar(clan.id());
        boolean isLeader = clan.member(player.getUniqueId()).map(m -> m.rank() == ClanRank.LEADER).orElse(false);
        boolean isAssistant = clan.member(player.getUniqueId()).map(m -> m.rank() == ClanRank.GUARDIAN).orElse(false);
        boolean canManage = isLeader || isAssistant;

        switch (slot) {
            case 22: // Back button
                plugin.getGuiManager().openClanTerritoriesMenu(clicker, clan);
                break;
            case 10: // Move Home Point
                if (!canManage) {
                    plugin.getMessages().send(clicker, "gui.capital.no-permission");
                    return;
                }
                plugin.getMessages().send(clicker, "gui.capital.move-home.action");
                clicker.closeInventory();
                break;
            case 12: // Transfer Capital Base
                if (!canManage) {
                    plugin.getMessages().send(clicker, "gui.capital.no-permission");
                    return;
                }
                if (isAtWar) {
                    plugin.getMessages().send(clicker, "gui.capital.war-blocked");
                    return;
                }
                plugin.getMessages().send(clicker, "gui.capital.transfer.action");
                clicker.closeInventory();
                break;
            case 14: // Disband Capital Base
                if (!canManage) {
                    plugin.getMessages().send(clicker, "gui.capital.no-permission");
                    return;
                }
                if (isAtWar) {
                    plugin.getMessages().send(clicker, "gui.capital.war-blocked");
                    return;
                }
                plugin.getMessages().send(clicker, "gui.capital.disband.action");
                clicker.closeInventory();
                break;
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}