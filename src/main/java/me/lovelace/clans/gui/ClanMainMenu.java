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
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;

public final class ClanMainMenu implements InventoryHolder {
    private final ClansPlugin plugin;
    private final Clan clan;
    private final Player player;
    private Inventory inventory;

    public ClanMainMenu(ClansPlugin plugin, Clan clan, Player player) {
        this.plugin = plugin;
        this.clan = clan;
        this.player = player;
    }

    public void open() {
        this.inventory = Bukkit.createInventory(this, 54,
                plugin.getMessages().component("gui.main.title", Map.of("clan", clan.name(), "color", clan.tagColor()), player));

        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build());
        }

        // Row 0 — clan info (center)
        inventory.setItem(4, ItemBuilder.of(clan.emblem())
                .name(plugin.getMessages().component("gui.main.info.name", Map.of("clan", clan.name()), player))
                .lore(plugin.getMessages().component("gui.main.info.tag",
                        Map.of("tag", clan.tag()), player))
                .lore(plugin.getMessages().component("gui.main.info.level",
                        Map.of("level", String.valueOf(clan.level())), player))
                .lore(plugin.getMessages().component("gui.main.info.members",
                        Map.of("current", String.valueOf(clan.members().size()),
                               "max", String.valueOf(plugin.getClanManager().maxMembers(clan))), player))
                .build());

        // Row 2 — main nav buttons
        inventory.setItem(19, ItemBuilder.head(ItemBuilder.HEAD_MEMBERS)
                .name(plugin.getMessages().component("gui.main.members.name", player))
                .lore(plugin.getMessages().component("gui.main.members.lore", player))
                .build());

        inventory.setItem(21, ItemBuilder.of(Material.WRITABLE_BOOK)
                .name(plugin.getMessages().component("gui.main.diplomacy.name", player))
                .lore(plugin.getMessages().component("gui.main.diplomacy.lore", player))
                .build());

        inventory.setItem(23, ItemBuilder.of(Material.GRASS_BLOCK)
                .name(plugin.getMessages().component("gui.main.territories.name", player))
                .lore(plugin.getMessages().component("gui.main.territories.lore", player))
                .build());

        inventory.setItem(25, ItemBuilder.of(Material.ANVIL)
                .name(plugin.getMessages().component("gui.main.upgrades.name", player))
                .lore(plugin.getMessages().component("gui.main.upgrades.lore", player))
                .build());

        // Row 4 — secondary buttons
        inventory.setItem(38, ItemBuilder.of(Material.NETHER_STAR)
                .name(plugin.getMessages().component("gui.main.spirit.name", player))
                .lore(plugin.getMessages().component("gui.main.spirit.lore", player))
                .build());

        inventory.setItem(40, ItemBuilder.head(ItemBuilder.HEAD_SETTINGS)
                .name(plugin.getMessages().component("gui.main.settings.name", player))
                .lore(plugin.getMessages().component("gui.main.settings.lore", player))
                .build());

        int applicationsCount = plugin.getClanManager().getClanApplications(clan.id()).size();
        boolean isLeaderOrGuardian = clan.member(player.getUniqueId())
                .map(m -> m.rank() == ClanRank.LEADER || m.rank() == ClanRank.GUARDIAN)
                .orElse(false);
        inventory.setItem(42, ItemBuilder.of(Material.PAPER)
                .name(plugin.getMessages().component("gui.main.applications.name", player))
                .lore(isLeaderOrGuardian
                        ? plugin.getMessages().component("gui.main.applications.lore",
                                Map.of("count", String.valueOf(applicationsCount)), player)
                        : plugin.getMessages().component("gui.main.applications.no-permission-lore", player))
                .build());

        // Row 5 — Close + Leave (only for non-leaders)
        inventory.setItem(49, ItemBuilder.of(Material.BARRIER)
                .name(plugin.getMessages().component("gui.close", player))
                .build());

        boolean isLeader = clan.member(player.getUniqueId())
                .map(m -> m.rank() == ClanRank.LEADER)
                .orElse(false);
        if (!isLeader) {
            inventory.setItem(52, ItemBuilder.of(Material.RED_BED)
                    .name(plugin.getMessages().component("gui.main.leave.name", player))
                    .lore(plugin.getMessages().component("gui.main.leave.lore", player))
                    .build());
        }

        player.openInventory(inventory);
    }

    public void handleInventoryClick(Player clicker, int slot) {
        switch (slot) {
            case 19 -> plugin.getGuiManager().openMembers(clicker, clan);
            case 21 -> plugin.getGuiManager().openDiplomacySelect(clicker, clan);
            case 23 -> plugin.getGuiManager().openClanTerritoriesMenu(clicker, clan);
            case 25 -> plugin.getGuiManager().openUpgrades(clicker, clan);
            case 38 -> plugin.getGuiManager().openSpirit(clicker, clan);
            case 40 -> plugin.getGuiManager().openSettings(clicker, clan);
            case 42 -> {
                boolean canViewApps = clan.member(clicker.getUniqueId())
                        .map(m -> m.rank() == ClanRank.LEADER || m.rank() == ClanRank.GUARDIAN)
                        .orElse(false);
                if (canViewApps) {
                    plugin.getGuiManager().openApplications(clicker, clan);
                } else {
                    plugin.getMessages().send(clicker, "general.no-permission");
                }
            }
            case 49 -> clicker.closeInventory();
            case 52 -> {
                boolean isLeader = clan.member(clicker.getUniqueId())
                        .map(m -> m.rank() == ClanRank.LEADER)
                        .orElse(false);
                if (isLeader) return;

                plugin.getGuiManager().openConfirm(clicker, clan, 
                        plugin.getMessages().component("gui.confirm.leave.title", clicker), 
                        Component.empty(),
                        () -> plugin.getClanManager().removeMemberAsync(clan, clicker.getUniqueId(), clicker.getUniqueId(), false)
                                .thenRun(() -> plugin.runSync(() -> {
                                    plugin.getMessages().send(clicker, "clan.left");
                                    clicker.closeInventory();
                                }))
                                .exceptionally(t -> { plugin.runSync(() -> plugin.sendOperationError(clicker, t)); return null; }),
                        () -> plugin.runSync(this::open)
                );
            }
            default -> {}
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
