package me.lovelace.clans.gui;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanTerritory;
import me.lovelace.clans.util.ItemBuilder;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public final class ClanTerritoriesMenu {
    private final ClansPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ClanTerritoriesMenu(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        Inventory inventory = Bukkit.createInventory(new ClanMenuHolder(ClanMenuType.TERRITORIES, clan.id()), 54,
                plugin.getMessages().component("gui.territories-title", Map.of("tag", clan.tag()), player));
        int slot = 0;
        for (ClanTerritory territory : clan.territories()) {
            if (slot >= 54) {
                break;
            }
            inventory.setItem(slot++, ItemBuilder.of(Material.MAP)
                    .name(miniMessage.deserialize("<green>" + territory.key().world()))
                    .lore(miniMessage.deserialize("<gray>Chunk: <white>" + territory.key().chunkX() + ", " + territory.key().chunkZ()))
                    .lore(miniMessage.deserialize("<gray>AdvancedClaims: <white>" + (territory.advancedClaimId() == null ? "нет" : territory.advancedClaimId())))
                    .build());
        }
        player.openInventory(inventory);
    }
}
