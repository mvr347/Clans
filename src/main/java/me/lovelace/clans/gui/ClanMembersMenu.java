package me.lovelace.clans.gui;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanMember;
import me.lovelace.clans.util.ItemBuilder;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Comparator;
import java.util.Map;

public final class ClanMembersMenu {
    private final ClansPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ClanMembersMenu(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        Inventory inventory = Bukkit.createInventory(new ClanMenuHolder(ClanMenuType.MEMBERS, clan.id()), 54,
                plugin.getMessages().component("gui.members-title", Map.of("tag", clan.tag()), player));
        int slot = 0;
        for (ClanMember member : clan.members().values().stream().sorted(Comparator.comparing(member -> member.rank().weight(), Comparator.reverseOrder())).toList()) {
            if (slot >= 54) {
                break;
            }
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(member.playerId());
            String name = offlinePlayer.getName() == null ? member.playerId().toString().substring(0, 8) : offlinePlayer.getName();
            inventory.setItem(slot++, ItemBuilder.of(Material.PLAYER_HEAD)
                    .name(miniMessage.deserialize("<white>" + name))
                    .lore(miniMessage.deserialize("<gray>Ранг: <yellow>" + member.rank().displayName()))
                    .lore(miniMessage.deserialize("<gray>Shift+ЛКМ: повысить, Shift+ПКМ: понизить"))
                    .mutate(meta -> meta.getPersistentDataContainer().set(plugin.getGuiManager().memberKey(), org.bukkit.persistence.PersistentDataType.STRING, member.playerId().toString()))
                    .build());
        }
        player.openInventory(inventory);
    }
}
