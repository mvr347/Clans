package me.lovelace.clans.gui;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.util.ItemBuilder;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public final class ClanMainMenu {
    private final ClansPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ClanMainMenu(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        Inventory inventory = Bukkit.createInventory(new ClanMenuHolder(ClanMenuType.MAIN, clan.id()), 27,
                plugin.getMessages().component("gui.main-title", Map.of("tag", clan.tag()), player));
        inventory.setItem(10, ItemBuilder.of(Material.PLAYER_HEAD)
                .name(miniMessage.deserialize("<gold>Участники"))
                .lore(miniMessage.deserialize("<gray>" + clan.members().size() + "/" + plugin.getClanManager().maxMembers(clan)))
                .build());
        inventory.setItem(12, ItemBuilder.of(Material.GRASS_BLOCK)
                .name(miniMessage.deserialize("<green>Территории"))
                .lore(miniMessage.deserialize("<gray>" + clan.territories().size() + "/" + plugin.getClanManager().maxTerritories(clan)))
                .build());
        inventory.setItem(14, ItemBuilder.of(Material.ANVIL)
                .name(miniMessage.deserialize("<aqua>Улучшения"))
                .lore(miniMessage.deserialize("<gray>Уровень " + clan.level()))
                .build());
        inventory.setItem(16, ItemBuilder.of(Material.CHEST)
                .name(miniMessage.deserialize("<yellow>Клан-сундук"))
                .lore(miniMessage.deserialize("<gray>" + clan.chestRows() + " рядов"))
                .build());
        player.openInventory(inventory);
    }
}
