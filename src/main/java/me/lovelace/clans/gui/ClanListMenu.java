package me.lovelace.clans.gui;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.DiplomacyRelation;
import me.lovelace.clans.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Date;

public final class ClanListMenu implements InventoryHolder {
    // Slots 9-44 = 36 content slots
    private static final int[] CONTENT_SLOTS = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };

    private final ClansPlugin plugin;
    private final Player player;
    private final List<Clan> allClans;
    private int currentPage;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");


    public ClanListMenu(ClansPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.allClans = new ArrayList<>(plugin.getClanManager().getAllClans().stream()
                .sorted(Comparator.comparingInt(Clan::level).reversed())
                .toList());
        this.currentPage = 0;
    }

    @Override
    public Inventory getInventory() {
        int maxPage = Math.max(0, (allClans.size() - 1) / CONTENT_SLOTS.length);
        currentPage = Math.max(0, Math.min(currentPage, maxPage));

        Inventory inventory = Bukkit.createInventory(this, 54,
                plugin.getMessages().component("gui.clan-list.title", player));
        fillGlass(inventory);

        // Back button in slot 0
        inventory.setItem(0, ItemBuilder.head(ItemBuilder.HEAD_BACK)
                .name(plugin.getMessages().component("gui.back", player)).build());

        int start = currentPage * CONTENT_SLOTS.length;
        int end = Math.min(start + CONTENT_SLOTS.length, allClans.size());
        for (int index = start; index < end; index++) {
            inventory.setItem(CONTENT_SLOTS[index - start], createClanItem(allClans.get(index)));
        }

        if (allClans.isEmpty()) {
            inventory.setItem(31, ItemBuilder.of(Material.PAPER)
                    .name(plugin.getMessages().component("gui.clan-list.empty.name", player))
                    .lore(plugin.getMessages().component("gui.clan-list.empty.lore", player))
                    .build());
        }

        if (currentPage > 0) inventory.setItem(45, ItemBuilder.head(ItemBuilder.HEAD_BACK) // Changed to HEAD_PREVIOUS
                .name(plugin.getMessages().component("gui.previous-page", player)).build());
        
        // Slot 53: Next page OR My Applications if not in clan
        boolean notInClan = plugin.getClanManager().getPlayerClan(player.getUniqueId()).isEmpty();
        if (currentPage < maxPage) {
            inventory.setItem(53, ItemBuilder.head("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTliZjMyOTJlMTI2YTEwNWI1NGViYTcxM2FhMWIxNTJkNTQxYTFkODkzODgyOWM1NjM2NGQxNzhlZDIyYmYifX19") // Changed to HEAD_NEXT
                    .name(plugin.getMessages().component("gui.next-page", player)).build());
        } else if (notInClan) {
            int appCount = countPlayerApplicationsAndInvites();
            inventory.setItem(53, ItemBuilder.head(ItemBuilder.HEAD_MSG)
                    .name(plugin.getMessages().component("gui.clan-list.my-applications.name", player))
                    .lore(plugin.getMessages().component("gui.clan-list.my-applications.lore",
                            Map.of("count", String.valueOf(appCount)), player))
                    .build());
        }

        // Close button
        inventory.setItem(49, ItemBuilder.head(ItemBuilder.HEAD_BARRIER)
                .name(plugin.getMessages().component("gui.close", player)).build());

        return inventory;
    }

    private int contentSlotIndex(int slot) {
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    private ItemStack createClanItem(Clan clan) {
        boolean full = plugin.getClanManager().isClanFull(clan);
        boolean closed = !clan.isOpen();
        boolean isMember = plugin.getClanManager().getPlayerClan(player.getUniqueId())
                .map(c -> c.id().equals(clan.id())).orElse(false);

        Material emblemMaterial = clan.emblem().name().endsWith("_BANNER") ? clan.emblem() : Material.WHITE_BANNER;

        ItemBuilder builder = ItemBuilder.of(emblemMaterial)
                .name(plugin.getMessages().component("gui.clan-list.clan-item.name",
                        Map.of("tag", clan.coloredTag(), "name", clan.name()), player));
        
        // Leader information
        clan.leaderId().ifPresent(leaderId -> {
            OfflinePlayer leader = Bukkit.getOfflinePlayer(leaderId);
            String leaderName = leader.getName() != null ? leader.getName() : leaderId.toString().substring(0, 8);
            builder.lore(plugin.getMessages().component("gui.clan-list.clan-item.leader",
                    Map.of("player", leaderName), player));
            
            if (leader.isOnline()) {
                builder.lore(plugin.getMessages().component("gui.clan-list.clan-item.leader-status.online", player));
            } else {
                builder.lore(plugin.getMessages().component("gui.clan-list.clan-item.leader-status.offline", player));
                if (leader.getLastPlayed() > 0) {
                    builder.lore(plugin.getMessages().component("gui.clan-list.clan-item.leader-last-seen",
                            Map.of("date", dateTimeFormat.format(new Date(leader.getLastPlayed()))), player));
                }
            }
        });

        builder.lore(plugin.getMessages().component("gui.clan-list.clan-item.level",
                        Map.of("level", String.valueOf(clan.level())), player))
                .lore(plugin.getMessages().component("gui.clan-list.clan-item.members",
                        Map.of("current", String.valueOf(clan.members().size()),
                                "max", String.valueOf(plugin.getClanManager().maxMembers(clan))), player))
                .lore(plugin.getMessages().component("gui.clan-list.clan-item.created-at",
                        Map.of("date", dateFormat.format(clan.createdAt())), player));

        // Apply/Status button logic
        if (isMember) {
            builder.lore(plugin.getMessages().component("gui.clan-list.clan-item.already-member", player));
        } else if (closed) {
            builder.lore(plugin.getMessages().component("gui.clan-list.clan-item.apply-closed", player));
        } else { // Open clan
            builder.lore(plugin.getMessages().component("gui.clan-list.clan-item.apply-open", player));
        }
        
        builder.lore(plugin.getMessages().component("gui.clan-list.clan-item.click-info", player));
                
        return builder.build();
    }

    private int countPlayerApplicationsAndInvites() {
        int apps = (int) plugin.getClanManager().getAllClans().stream()
                .flatMap(c -> plugin.getClanManager().getClanApplications(c.id()).stream())
                .filter(a -> a.applicantId().equals(player.getUniqueId()))
                .count();
        int invites = plugin.getClanManager().getPlayerInvites(player.getUniqueId()).size();
        return apps + invites;
    }

    public void open() { player.openInventory(getInventory()); }

    public void handleInventoryClick(int slot) {
        if (slot == 0) { // Back button
            player.closeInventory(); // Assuming back means close for now, or open previous menu if applicable
            return;
        }
        if (slot == 45 && currentPage > 0) { currentPage--; open(); return; }
        
        int maxPage = Math.max(0, (allClans.size() - 1) / CONTENT_SLOTS.length);
        if (slot == 53) {
            if (currentPage < maxPage) {
                currentPage++; open();
            } else if (plugin.getClanManager().getPlayerClan(player.getUniqueId()).isEmpty()) {
                new PlayerApplicationsMenu(plugin, player).open();
            }
            return;
        }

        int relativeIndex = contentSlotIndex(slot);
        if (relativeIndex < 0) return;
        int clanIndex = currentPage * CONTENT_SLOTS.length + relativeIndex;
        if (clanIndex < 0 || clanIndex >= allClans.size()) return;

        Clan clickedClan = allClans.get(clanIndex);
        Optional<Clan> playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        // Handle apply button click (if it was the apply button, which is now part of the clan item lore)
        // The click on the clan item itself will open ClanInfoMenu.
        // If the player clicks on a clan item and is not a member, and the clan is open, they can apply.
        if (playerClan.isEmpty() && clickedClan.isOpen()) {
            plugin.getClanManager().applyToClanAsync(clickedClan, player.getUniqueId())
                .thenRun(() -> plugin.runSync(() -> {
                    plugin.getMessages().send(player, "clan.applied", Map.of("tag", clickedClan.tag()));
                    player.closeInventory();
                }))
                .exceptionally(t -> { plugin.runSync(() -> plugin.sendOperationError(player, t)); return null; });
            return;
        }


        // Click → open clan info
        new ClanInfoMenu(plugin, player, clickedClan).open();
    }

    private void fillGlass(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            // Don't fill slot 0 (back button)
            if (slot == 0) continue;
            inventory.setItem(slot, ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build());
        }
    }
}