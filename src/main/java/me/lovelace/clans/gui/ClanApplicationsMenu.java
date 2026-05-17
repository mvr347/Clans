package me.lovelace.clans.gui;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanApplication;
import me.lovelace.clans.model.ClanInvite;
import me.lovelace.clans.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClanApplicationsMenu {
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final String INVITE_TAG = "invite:";

    private final ClansPlugin plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private final Map<UUID, Integer> pageByPlayer = new ConcurrentHashMap<>();

    public ClanApplicationsMenu(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        open(player, clan, pageByPlayer.getOrDefault(player.getUniqueId(), 0));
    }

    private void open(Player player, Clan clan, int requestedPage) {
        List<ClanApplication> applications = plugin.getClanManager().getClanApplications(clan.id()).stream()
                .sorted(Comparator.comparingLong(ClanApplication::appliedAt).reversed())
                .toList();
        List<ClanInvite> invites = plugin.getClanManager().getClanInvites(clan.id());

        // Total items = applications + invites
        int totalItems = applications.size() + invites.size();
        int maxPage = Math.max(0, (totalItems - 1) / CONTENT_SLOTS.length);
        int page = Math.max(0, Math.min(requestedPage, maxPage));
        pageByPlayer.put(player.getUniqueId(), page);

        Inventory inventory = Bukkit.createInventory(new ClanMenuHolder(ClanMenuType.APPLICATIONS, clan.id()), 54,
                plugin.getMessages().component("gui.applications.title", Map.of("clan", clan.name()), player));

        fillGlass(inventory);

        // Build combined list: applications first, then invites
        List<Object> combined = new ArrayList<>();
        combined.addAll(applications);
        combined.addAll(invites);

        int start = page * CONTENT_SLOTS.length;
        int end = Math.min(start + CONTENT_SLOTS.length, combined.size());
        for (int index = start; index < end; index++) {
            Object entry = combined.get(index);
            if (entry instanceof ClanApplication application) {
                OfflinePlayer applicant = Bukkit.getOfflinePlayer(application.applicantId());
                String applicantName = applicant.getName() == null ? applicant.getUniqueId().toString() : applicant.getName();

                inventory.setItem(CONTENT_SLOTS[index - start], ItemBuilder.of(Material.PLAYER_HEAD)
                        .name(plugin.getMessages().component("gui.applications.applicant-item.name",
                                Map.of("player", applicantName), player))
                        .lore(plugin.getMessages().component("gui.applications.applicant-item.applied-at",
                                Map.of("date", dateFormat.format(new Date(application.appliedAt()))), player))
                        .lore(plugin.getMessages().component("gui.applications.applicant-item.left-click-accept", player))
                        .lore(plugin.getMessages().component("gui.applications.applicant-item.right-click-reject", player))
                        .mutate(meta -> {
                            if (meta instanceof SkullMeta skullMeta) {
                                skullMeta.setOwningPlayer(applicant);
                            }
                            meta.getPersistentDataContainer().set(
                                    plugin.getGuiManager().memberKey(),
                                    PersistentDataType.STRING,
                                    application.applicantId().toString()
                            );
                        })
                        .build());
            } else if (entry instanceof ClanInvite invite) {
                OfflinePlayer invited = Bukkit.getOfflinePlayer(invite.invitedPlayer());
                String invitedName = invited.getName() == null ? invite.invitedPlayer().toString() : invited.getName();

                inventory.setItem(CONTENT_SLOTS[index - start], ItemBuilder.of(Material.PAPER)
                        .name(plugin.getMessages().component("gui.applications.invite-item.name",
                                Map.of("player", invitedName), player))
                        .lore(plugin.getMessages().component("gui.applications.invite-item.status", player))
                        .mutate(meta -> {
                            if (meta instanceof SkullMeta skullMeta) {
                                skullMeta.setOwningPlayer(invited);
                            }
                            // Mark as invite so click is ignored
                            meta.getPersistentDataContainer().set(
                                    plugin.getGuiManager().memberKey(),
                                    PersistentDataType.STRING,
                                    INVITE_TAG + invite.invitedPlayer()
                            );
                        })
                        .build());
            }
        }

        if (combined.isEmpty()) {
            inventory.setItem(22, ItemBuilder.of(Material.PAPER)
                    .name(plugin.getMessages().component("gui.applications.empty.name", player))
                    .lore(plugin.getMessages().component("gui.applications.empty.lore", player))
                    .build());
        }

        if (page > 0) {
            inventory.setItem(45, ItemBuilder.head(ItemBuilder.HEAD_BACK)
                    .name(plugin.getMessages().component("gui.previous-page", player))
                    .build());
        }
        if (page < maxPage) {
            inventory.setItem(53, ItemBuilder.of(Material.ARROW)
                    .name(plugin.getMessages().component("gui.next-page", player))
                    .build());
        }

        inventory.setItem(49, ItemBuilder.head(ItemBuilder.HEAD_BACK)
                .name(plugin.getMessages().component("gui.back", player))
                .build());

        player.openInventory(inventory);
    }

    public void handleInventoryClick(InventoryClickEvent event, Player player, Clan clan) {
        int slot = event.getRawSlot();
        if (slot == 49) {
            pageByPlayer.remove(player.getUniqueId());
            plugin.getGuiManager().openMain(player, clan);
            return;
        }
        if (slot == 45) {
            open(player, clan, pageByPlayer.getOrDefault(player.getUniqueId(), 0) - 1);
            return;
        }
        if (slot == 53) {
            open(player, clan, pageByPlayer.getOrDefault(player.getUniqueId(), 0) + 1);
            return;
        }

        if (!isContentSlot(slot) || event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) {
            return;
        }

        String rawId = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(
                plugin.getGuiManager().memberKey(), PersistentDataType.STRING);
        if (rawId == null || rawId.startsWith(INVITE_TAG)) {
            return; // invites are display-only
        }

        UUID applicantId;
        try {
            applicantId = UUID.fromString(rawId);
        } catch (IllegalArgumentException ignored) {
            return;
        }

        if (event.isLeftClick()) {
            plugin.getClanManager().acceptApplicationAsync(clan, player.getUniqueId(), applicantId)
                    .thenAccept(updatedClan -> plugin.runSync(() -> {
                        OfflinePlayer applicant = Bukkit.getOfflinePlayer(applicantId);
                        String name = applicant.getName() == null ? applicant.getUniqueId().toString() : applicant.getName();
                        plugin.getMessages().send(player, "gui.applications.accepted", Map.of("player", name));
                        Player target = Bukkit.getPlayer(applicantId);
                        if (target != null) {
                            plugin.getMessages().send(target, "clan.joined", Map.of("tag", updatedClan.tag()));
                        }
                        open(player, updatedClan);
                    }))
                    .exceptionally(throwable -> {
                        plugin.runSync(() -> plugin.sendOperationError(player, throwable));
                        return null;
                    });
            return;
        }

        if (event.isRightClick()) {
            plugin.getClanManager().rejectApplicationAsync(clan, player.getUniqueId(), applicantId)
                    .thenRun(() -> plugin.runSync(() -> {
                        OfflinePlayer applicant = Bukkit.getOfflinePlayer(applicantId);
                        String name = applicant.getName() == null ? applicant.getUniqueId().toString() : applicant.getName();
                        plugin.getMessages().send(player, "gui.applications.rejected", Map.of("player", name));
                        open(player, clan);
                    }))
                    .exceptionally(throwable -> {
                        plugin.runSync(() -> plugin.sendOperationError(player, throwable));
                        return null;
                    });
        }
    }

    private boolean isContentSlot(int slot) {
        for (int contentSlot : CONTENT_SLOTS) {
            if (contentSlot == slot) return true;
        }
        return false;
    }

    private void fillGlass(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                    .name(Component.empty())
                    .build());
        }
    }
}
