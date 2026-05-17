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
import org.jetbrains.annotations.NotNull; // Импортируем аннотацию

import java.util.Map;

public class TerritorySettingsMenu implements InventoryHolder {
    private final ClansPlugin plugin;
    private final ClanTerritory territory;
    private final Clan clan;
    private Inventory inventory;

    public TerritorySettingsMenu(ClansPlugin plugin, Clan clan, ClanTerritory territory) {
        this.plugin = plugin;
        this.clan = clan;
        this.territory = territory;
    }

    public void open(Player player) {
        String titleName = territory.name() != null ? territory.name() : territory.key().chunkX() + "," + territory.key().chunkZ();
        this.inventory = Bukkit.createInventory(this, 27, plugin.getMessages().component("gui.territory-settings.title", Map.of("chunk", titleName), player));

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build());
        }

        // Rename Private
        inventory.setItem(11, ItemBuilder.head("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTY3ZDgxM2FlN2ZmZTViZTk1MWE0ZjQxZjJhYTYxOWE1ZTM4OTRlODVlYTVkNDk4NmY4NDk0OWM2M2Q3NjcyZSJ9fX0=")
                .name(plugin.getMessages().component("gui.territory-settings.rename-private.name", player))
                .lore(plugin.getMessages().component("gui.territory-settings.rename-private.lore", player))
                .build());

        // Toggle PvP
        boolean pvpEnabled = territory.pvp();
        inventory.setItem(15, ItemBuilder.head(ItemBuilder.HEAD_DISBAND)
                .name(plugin.getMessages().component("gui.territory-settings.pvp.name", player))
                .lore(plugin.getMessages().component("gui.territory-settings.pvp.lore", Map.of("status", pvpEnabled ? "Включено" : "Выключено"), player))
                .build());

        // Disband Private button
        inventory.setItem(13, ItemBuilder.head(ItemBuilder.HEAD_BARRIER)
                .name(plugin.getMessages().component("gui.territory-settings.disband-private.name", player))
                .lore(plugin.getMessages().component("gui.territory-settings.disband-private.lore", player))
                .build());

        // Back button
        inventory.setItem(22, ItemBuilder.head(ItemBuilder.HEAD_BACK)
                .name(plugin.getMessages().component("gui.back", player))
                .build());

        player.openInventory(inventory);
    }

    public void handleInventoryClick(Player player, int slot) {
        if (slot == 22) {
            plugin.getGuiManager().openTerritories(player, clan);
            return;
        }

        boolean canManage = clan.member(player.getUniqueId()).map(m -> m.rank().atLeast(ClanRank.GUARDIAN)).orElse(false);
        if (!canManage) {
            plugin.getMessages().send(player, "general.no-permission");
            return;
        }

        if (slot == 11) { // Rename Private
            player.closeInventory();
            plugin.getMessages().send(player, "gui.territory-settings.rename.prompt");
            plugin.expectChatInput(player.getUniqueId(), (newName, cancelled) -> {
                if (cancelled) {
                    plugin.runSync(() -> new TerritorySettingsMenu(plugin, clan, territory).open(player));
                    return;
                }
                ClanTerritory updated = territory.withName(newName);
                plugin.getClanManager().updateTerritoryAsync(clan, updated).thenRun(() -> plugin.runSync(() -> {
                    plugin.getMessages().send(player, "gui.territory-settings.rename.success", Map.of("name", newName));
                    new TerritorySettingsMenu(plugin, clan, updated).open(player);
                }));
            });
        } else if (slot == 13) { // Disband Private
            player.closeInventory();
            plugin.getClanManager().unclaimTerritoryAsync(clan, territory.key(), player.getUniqueId())
                    .thenAccept(v -> plugin.runSync(() -> {
                        plugin.getMessages().send(player, "territory.unclaimed");
                        plugin.getGuiManager().openTerritories(player, clan);
                    }))
                    .exceptionally(t -> {
                        plugin.runSync(() -> plugin.sendOperationError(player, t));
                        return null;
                    });
        } else if (slot == 15) { // Toggle PvP
            ClanTerritory updated = territory.withPvp(!territory.pvp());
            plugin.getClanManager().updateTerritoryAsync(clan, updated).thenRun(() -> plugin.runSync(() -> {
                plugin.getMessages().send(player, "gui.territory-settings.pvp.success");
                new TerritorySettingsMenu(plugin, clan, updated).open(player);
            }));
        }
    }

    @Override
    @NotNull // Добавлена аннотация
    public Inventory getInventory() {
        return inventory;
    }
}
