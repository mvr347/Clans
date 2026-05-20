package me.lovelace.clans.gui;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public final class ClanSettingsMenu {
    private final ClansPlugin plugin;

    public ClanSettingsMenu(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        Inventory inventory = Bukkit.createInventory(
                new ClanMenuHolder(ClanMenuType.SETTINGS, clan.id()), 18,
                plugin.getMessages().component("gui.settings.title", Map.of("clan", clan.name(), "color", clan.tagColor()), player));

        fillGlass(inventory);

        inventory.setItem(1, ItemBuilder.head("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTY3ZDgxM2FlN2ZmZTViZTk1MWE0ZjQxZjJhYTYxOWE1ZTM4OTRlODVlYTVkNDk4NmY4NDk0OWM2M2Q3NjcyZSJ9fX0=")
                .name(plugin.getMessages().component("gui.settings.rename.name", player))
                .lore(plugin.getMessages().component("gui.settings.rename.lore", player))
                .build());

        inventory.setItem(2, ItemBuilder.head("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzFiYzJiY2ZiMmJkMzc1OWU2YjFlODZmYzdhNzk1ODVlMTEyN2RkMzU3ZmMyMDI4OTNmOWRlMjQxYmM5ZTUzMCJ9fX0=")
                .name(plugin.getMessages().component("gui.settings.change-tag.name", player))
                .lore(plugin.getMessages().component("gui.settings.change-tag.lore", player))
                .build());

        Material emblemMaterial = clan.emblem().name().endsWith("_BANNER") ? clan.emblem() : Material.WHITE_BANNER;
        inventory.setItem(3, ItemBuilder.of(emblemMaterial)
                .name(plugin.getMessages().component("gui.settings.change-banner.name", player))
                .lore(plugin.getMessages().component("gui.settings.change-banner.lore", player))
                .build());

        inventory.setItem(4, ItemBuilder.head("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGIyNDllODhhZmEzMGZjODM3YjgyMTczYTMwNDgzNDU4ZDRlOWEzM2M3ZWMyNWU1NTEzODdlOGU1NGEwMThhZSJ9fX0=")
                .name(plugin.getMessages().component("gui.settings.change-color.name", player))
                .lore(plugin.getMessages().component("gui.settings.change-color.lore",
                        Map.of("preview", clan.coloredTag()), player))
                .build());

        // New item for open/closed status
        if (clan.isOpen()) {
            inventory.setItem(5, ItemBuilder.head("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTQ4YmI0ZTQ0MzVjMmMyMWQ3ZjYxODNiMzhhMmI3MzcyNjUzZjM1NDBiZTAyMjU5ZGQ0N2JmNTI0OTJkZTY2OSJ9fX0=")
                    .name(plugin.getMessages().component("gui.settings.status.open.name", player))
                    .lore(plugin.getMessages().component("gui.settings.status.open.lore", player))
                    .build());
        } else {
            inventory.setItem(5, ItemBuilder.head("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmJmNDZiZjM5ZGZjNzE4ZTdlYTMxZGI0MzQ3N2ZjNmI3ZGNhNTg4ZmUwYTc4OTFkNDgxYzVkZGE5ZTE2ZjUyMCJ9fX0=")
                    .name(plugin.getMessages().component("gui.settings.status.closed.name", player))
                    .lore(plugin.getMessages().component("gui.settings.status.closed.lore", player))
                    .build());
        }

        inventory.setItem(6, ItemBuilder.head("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDVmODcyZTMxYTQzZWU4YTY1Y2FjY2Y3M2I5NDJjOTdmMmNmODJjYzdjYmRhN2M5NzUyODc0MDliYzhlMjQxNCJ9fX0=")
                .name(plugin.getMessages().component("gui.settings.roles.name", player))
                .lore(plugin.getMessages().component("gui.settings.roles.lore", player))
                .build());

        inventory.setItem(7, ItemBuilder.head(ItemBuilder.HEAD_BARRIER)
                .name(plugin.getMessages().component("gui.settings.disband.name", player))
                .lore(plugin.getMessages().component("gui.settings.disband.lore", player))
                .build());

        inventory.setItem(13, ItemBuilder.head(ItemBuilder.HEAD_BACK)
                .name(plugin.getMessages().component("gui.back", player))
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