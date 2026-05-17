package me.lovelace.clans.gui;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanUpgrade;
import me.lovelace.clans.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public final class ClanUpgradesMenu {
    private final ClansPlugin plugin;

    public ClanUpgradesMenu(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        Inventory inventory = Bukkit.createInventory(new ClanMenuHolder(ClanMenuType.UPGRADES, clan.id()), 54,
                plugin.getMessages().component("gui.upgrades-title", Map.of("tag", clan.tag()), player));

        fillGlass(inventory);

        // Level Info in the middle of second row
        long currentExp = clan.experience();
        long expForCurrent = plugin.getClanManager().experienceForLevel(clan.level());
        long expForNext = plugin.getClanManager().experienceForLevel(clan.level() + 1);
        long required = expForNext - expForCurrent;
        long progress = currentExp - expForCurrent;
        double percent = required > 0 ? (double) progress / required * 100 : 100.0;

        inventory.setItem(13, ItemBuilder.head(ItemBuilder.HEAD_EXP)
                .name(plugin.getMessages().component("gui.upgrades.level-info.name", player))
                .lore(plugin.getMessages().components("gui.upgrades.level-info.lore", Map.of(
                        "level", String.valueOf(clan.level()),
                        "current_exp", String.valueOf(currentExp),
                        "next_exp", String.valueOf(expForNext),
                        "percent", String.format("%.1f", percent)
                ), player))
                .lore(plugin.getMessages().component("gui.upgrades.points-info.lore", Map.of("points", String.valueOf(clan.upgradePoints())), player))
                .build());

        int[] slots = {20, 21, 22, 23, 24};
        ClanUpgrade[] upgrades = ClanUpgrade.values();
        for (int i = 0; i < Math.min(slots.length, upgrades.length); i++) {
            ClanUpgrade upgrade = upgrades[i];
            
            boolean hasPoints = clan.upgradePoints() > 0;
            
            inventory.setItem(slots[i], ItemBuilder.head(hasPoints ? ItemBuilder.HEAD_EXPAND : ItemBuilder.HEAD_BARRIER)
                    .name(plugin.getMessages().component("gui.upgrades.item.name", Map.of("name", upgrade.displayName()), player))
                    .lore(plugin.getMessages().component("gui.upgrades.item.level", Map.of(
                            "level", String.valueOf(clan.upgradeLevel(upgrade))
                    ), player))
                    .lore(plugin.getMessages().component("gui.upgrades.item.click-to-upgrade", player))
                    .build());
        }

        inventory.setItem(49, ItemBuilder.head(ItemBuilder.HEAD_BACK)
                .name(plugin.getMessages().component("gui.back", player))
                .build());

        player.openInventory(inventory);
    }

    private void fillGlass(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                    .name(Component.empty())
                    .build());
        }
    }
}
