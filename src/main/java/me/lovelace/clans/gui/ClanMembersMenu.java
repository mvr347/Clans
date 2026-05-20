package me.lovelace.clans.gui;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanMember;
import me.lovelace.clans.model.ClanRank;
import me.lovelace.clans.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ClanMembersMenu {
    private final ClansPlugin plugin;

    public ClanMembersMenu(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        List<ClanMember> sortedMembers = clan.members().values().stream()
                .sorted(Comparator.comparingInt((ClanMember member) -> member.rank().weight()).reversed())
                .toList();

        int numMembers = sortedMembers.size();
        // Calculate required rows for members, each row holds 9 members. Add 2 for top/bottom control rows.
        int requiredRows = (int) Math.ceil(numMembers / 9.0) + 2;
        // Ensure minimum 3 rows (27 slots) and maximum 6 rows (54 slots)
        int inventorySize = Math.max(27, Math.min(54, requiredRows * 9));

        Inventory inventory = Bukkit.createInventory(new ClanMenuHolder(ClanMenuType.MEMBERS, clan.id()), inventorySize,
                plugin.getMessages().component("gui.members-title", Map.of("tag", clan.tag(), "color", clan.tagColor()), player));

        fillGlass(inventory);

        boolean canInvite = clan.member(player.getUniqueId())
                .map(m -> m.rank().atLeast(ClanRank.GUARDIAN))
                .orElse(false);
                
        boolean isGuildmaster = clan.member(player.getUniqueId())
                .map(m -> m.rank() == ClanRank.LEADER)
                .orElse(false);

        // Place member heads starting from the second row (slot 9)
        for (int i = 0; i < numMembers; i++) {
            ClanMember member = sortedMembers.get(i);
            OfflinePlayer offline = Bukkit.getOfflinePlayer(member.playerId());
            String name = offline.getName() != null ? offline.getName() : member.playerId().toString().substring(0, 8);
            String status = offline.isOnline() ? "<green>В сети</green>" : "<red>Оффлайн</red>";

            ItemBuilder builder = ItemBuilder.of(Material.PLAYER_HEAD)
                    .name(plugin.getMessages().component("gui.members.item.name", Map.of("player", name), player))
                    .lore(plugin.getMessages().component("gui.members.item.rank",
                            Map.of("rank", member.rank().displayName()), player))
                    .lore(plugin.getMessages().component("gui.members.item.status",
                            Map.of("status", status), player))
                    .lore(plugin.getMessages().component("gui.members.item.contribution",
                            Map.of("amount", String.valueOf(member.contribution())), player));

            // Show click hint for guildmaster (not for themselves or other GMs)
            if (isGuildmaster && member.rank() != ClanRank.LEADER && !member.playerId().equals(player.getUniqueId())) {
                builder.lore(plugin.getMessages().component("gui.members.item.hint", player));
            }

            builder.mutate(meta -> {
                if (meta instanceof SkullMeta skullMeta) skullMeta.setOwningPlayer(offline);
                meta.getPersistentDataContainer().set(
                        plugin.getGuiManager().memberKey(), PersistentDataType.STRING,
                        member.playerId().toString());
            });

            inventory.setItem(9 + i, builder.build()); // Start placing from slot 9
        }

        // Place control buttons in the last row
        inventory.setItem(inventorySize - 5, ItemBuilder.head(ItemBuilder.HEAD_BACK) // Back button
                .name(plugin.getMessages().component("gui.back", player))
                .build());
                
        if (canInvite) {
            boolean isFull = plugin.getClanManager().isClanFull(clan);
            ItemBuilder inviteBuilder = isFull ? ItemBuilder.head(ItemBuilder.HEAD_BARRIER) : ItemBuilder.head(ItemBuilder.HEAD_INVITE);
            inviteBuilder.name(plugin.getMessages().component("gui.members.invite.name", player))
                    .lore(plugin.getMessages().component(isFull ? "gui.members.invite.lore-full" : "gui.members.invite.lore", player));
            inventory.setItem(inventorySize - 4, inviteBuilder.build()); // Invite button
        }

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