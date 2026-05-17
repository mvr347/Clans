package me.lovelace.clans.gui;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanRank;
import me.lovelace.clans.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public final class ClanMainMenu {
    private final ClansPlugin plugin;

    public ClanMainMenu(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        Inventory inventory = Bukkit.createInventory(new ClanMenuHolder(ClanMenuType.MAIN, clan.id()), 54,
                plugin.getMessages().component("gui.main-title", Map.of("tag", clan.tag()), player));

        fillGlass(inventory);

        // Clan Info (Emblem) - Top center
        inventory.setItem(4, ItemBuilder.of(clan.emblem())
                .name(plugin.getMessages().component("gui.main.info.name", Map.of("tag", clan.tag()), player))
                .lore(plugin.getMessages().component("gui.main.info.name-lore", Map.of("name", clan.name()), player))
                .lore(plugin.getMessages().component("gui.main.info.members-lore", Map.of(
                        "members", String.valueOf(clan.members().size()),
                        "max_members", String.valueOf(plugin.getClanManager().maxMembers(clan))
                ), player))
                .lore(plugin.getMessages().component("gui.main.info.level-lore", Map.of("level", String.valueOf(clan.level())), player))
                .build());

        // Main buttons - Row 2
        inventory.setItem(20, ItemBuilder.head(ItemBuilder.HEAD_MEMBERS) // Members
                .name(plugin.getMessages().component("gui.main.members.name", player))
                .lore(plugin.getMessages().component("gui.main.members.lore", player))
                .build());

        inventory.setItem(22, ItemBuilder.head(ItemBuilder.HEAD_EXP) // Territories
                .name(plugin.getMessages().component("gui.main.territories.name", player))
                .lore(plugin.getMessages().component("gui.main.territories.lore", player))
                .build());

        inventory.setItem(24, ItemBuilder.head(ItemBuilder.HEAD_EXPAND) // Upgrades
                .name(plugin.getMessages().component("gui.main.upgrades.name", player))
                .lore(plugin.getMessages().component("gui.main.upgrades.lore", player))
                .glow(clan.upgradePoints() > 0) // Добавлено свечение, если есть очки улучшений
                .build());

        // Main buttons - Row 3
        inventory.setItem(30, ItemBuilder.head(ItemBuilder.HEAD_MAP) // Diplomacy
                .name(plugin.getMessages().component("gui.main.diplomacy.name", player))
                .lore(plugin.getMessages().component("gui.main.diplomacy.lore", player))
                .build());

        boolean guildmaster = clan.member(player.getUniqueId())
                .map(member -> member.rank() == ClanRank.LEADER)
                .orElse(false);

        if (guildmaster) {
            inventory.setItem(32, ItemBuilder.head(ItemBuilder.HEAD_MSG) // Applications
                    .name(plugin.getMessages().component("gui.main.applications.name", player))
                    .lore(plugin.getMessages().component("gui.main.applications.lore", player))
                    .build());
        } else {
            inventory.setItem(32, ItemBuilder.head(ItemBuilder.HEAD_BARRIER)
                    .name(plugin.getMessages().component("gui.main.applications.name", player))
                    .lore(plugin.getMessages().component("gui.main.applications.no-permission-lore", player))
                    .build());
        }

        // Row 4
        inventory.setItem(39, ItemBuilder.head(ItemBuilder.HEAD_DAILY_QUESTS)
                .name(plugin.getMessages().component("gui.main.quests.name", player))
                .lore(plugin.getMessages().component("gui.main.quests.lore", player))
                .build());

        inventory.setItem(41, ItemBuilder.head(ItemBuilder.HEAD_SETTINGS)
                .name(plugin.getMessages().component("gui.main.settings.name", player))
                .lore(plugin.getMessages().component("gui.main.settings.lore", player))
                .build());

        // Leave Clan button - moved to slot 53 (bottom right corner)
        // Disband is removed from here for the leader
        if (!guildmaster) {
            inventory.setItem(53, ItemBuilder.head(ItemBuilder.HEAD_DELETE_YES)
                    .name(plugin.getMessages().component("gui.main.leave.name", player))
                    .lore(plugin.getMessages().component("gui.main.leave.lore", player))
                    .build());
        }

        // Close button - Row 5, now at slot 49
        inventory.setItem(49, ItemBuilder.head(ItemBuilder.HEAD_BARRIER)
                .name(plugin.getMessages().component("gui.close", player))
                .build());

        player.openInventory(inventory);
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
