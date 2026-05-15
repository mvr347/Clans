package me.lovelace.clans.gui;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanRank;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ClanGuiManager implements Listener {
    private final ClansPlugin plugin;
    private final ClanMainMenu mainMenu;
    private final ClanMembersMenu membersMenu;
    private final ClanTerritoriesMenu territoriesMenu;
    private final ClanUpgradesMenu upgradesMenu;
    private final NamespacedKey memberKey;

    public ClanGuiManager(ClansPlugin plugin) {
        this.plugin = plugin;
        this.mainMenu = new ClanMainMenu(plugin);
        this.membersMenu = new ClanMembersMenu(plugin);
        this.territoriesMenu = new ClanTerritoriesMenu(plugin);
        this.upgradesMenu = new ClanUpgradesMenu(plugin);
        this.memberKey = new NamespacedKey(plugin, "gui_member");
    }

    public NamespacedKey memberKey() {
        return memberKey;
    }

    public void openMain(Player player, Clan clan) {
        mainMenu.open(player, clan);
    }

    public void openMembers(Player player, Clan clan) {
        membersMenu.open(player, clan);
    }

    public void openTerritories(Player player, Clan clan) {
        territoriesMenu.open(player, clan);
    }

    public void openUpgrades(Player player, Clan clan) {
        upgradesMenu.open(player, clan);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getInventory().getHolder() instanceof ClanChestHolder) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof ClanMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        Optional<Clan> clan = plugin.getClanManager().getClanById(holder.clanId());
        if (clan.isEmpty()) {
            player.closeInventory();
            return;
        }
        if (holder.type() == ClanMenuType.MAIN) {
            handleMainClick(event.getRawSlot(), player, clan.get());
        } else if (holder.type() == ClanMenuType.MEMBERS && event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
            handleMemberClick(event, player, clan.get());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        plugin.getClanChestManager().saveIfClanChest(event.getInventory());
    }

    private void handleMainClick(int slot, Player player, Clan clan) {
        switch (slot) {
            case 10 -> openMembers(player, clan);
            case 12 -> openTerritories(player, clan);
            case 14 -> openUpgrades(player, clan);
            case 16 -> plugin.getClanChestManager().openChest(player, clan);
            default -> {
            }
        }
    }

    private void handleMemberClick(InventoryClickEvent event, Player player, Clan clan) {
        String playerId = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(memberKey, PersistentDataType.STRING);
        if (playerId == null || !event.isShiftClick()) {
            return;
        }
        UUID target = UUID.fromString(playerId);
        clan.member(target).ifPresent(member -> {
            ClanRank newRank = event.isLeftClick() ? promote(member.rank()) : demote(member.rank());
            plugin.getClanManager().setRankAsync(clan, player.getUniqueId(), target, newRank)
                    .thenAccept(updated -> plugin.runSync(() -> {
                        plugin.getMessages().send(player, "clan.rank-changed", Map.of("player", target.toString(), "rank", newRank.displayName()));
                        openMembers(player, updated);
                    }))
                    .exceptionally(throwable -> {
                        plugin.runSync(() -> plugin.sendOperationError(player, throwable));
                        return null;
                    });
        });
    }

    private ClanRank promote(ClanRank rank) {
        return rank == ClanRank.MEMBER ? ClanRank.ASSISTANT : rank;
    }

    private ClanRank demote(ClanRank rank) {
        return rank == ClanRank.ASSISTANT ? ClanRank.MEMBER : rank;
    }
}
