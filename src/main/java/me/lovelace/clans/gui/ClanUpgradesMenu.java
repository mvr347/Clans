package me.lovelace.clans.gui;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanUpgrade;
import me.lovelace.clans.util.ItemBuilder;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public final class ClanUpgradesMenu {
    private final ClansPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ClanUpgradesMenu(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        Inventory inventory = Bukkit.createInventory(new ClanMenuHolder(ClanMenuType.UPGRADES, clan.id()), 27,
                plugin.getMessages().component("gui.upgrades-title", Map.of("tag", clan.tag()), player));
        int slot = 10;
        for (ClanUpgrade upgrade : ClanUpgrade.values()) {
            if (slot == 17) {
                break;
            }
            inventory.setItem(slot++, ItemBuilder.of(material(upgrade))
                    .name(miniMessage.deserialize("<aqua>" + upgrade.displayName()))
                    .lore(miniMessage.deserialize("<gray>Уровень: <white>" + clan.upgradeLevel(upgrade)))
                    .lore(miniMessage.deserialize("<gray>Развитие идёт через уровень клана и события."))
                    .build());
        }
        player.openInventory(inventory);
    }

    private Material material(ClanUpgrade upgrade) {
        return switch (upgrade) {
            case MEMBERS -> Material.PLAYER_HEAD;
            case TERRITORIES -> Material.GRASS_BLOCK;
            case LOOTING -> Material.DIAMOND_PICKAXE;
            case CHEST -> Material.CHEST;
            case SPIRIT -> Material.AMETHYST_SHARD;
            case WARFARE -> Material.IRON_SWORD;
        };
    }
}
