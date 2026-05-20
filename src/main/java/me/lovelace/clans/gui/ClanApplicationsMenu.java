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

        List<Object> combined = new ArrayList<>();
        combined.addAll(applications);
        combined.addAll(invites);

        int totalItems = combined.size();
        int invSize = computeSize(totalItems);
        int[] contentSlots = computeContentSlots(invSize);
        int perPage = contentSlots.length;
        int maxPage = perPage == 0 ? 0 : Math.max(0, (totalItems - 1) / perPage);
        int page = Math.max(0, Math.min(requestedPage, maxPage));
        pageByPlayer.put(player.getUniqueId(), page);

        Inventory inventory = Bukkit.createInventory(
                new ClanMenuHolder(ClanMenuType.APPLICATIONS, clan.id()),
                invSize,
                plugin.getMessages().component("gui.applications.title",
                        Map.of("clan", clan.name(), "color", clan.tagColor()), player));

        fillGlass(inventory);

        int start = page * perPage;
        int end = Math.min(start + perPage, totalItems);
        for (int index = start; index < end; index++) {
            Object entry = combined.get(index);
            int targetSlot = contentSlots[index - start];
            if (entry instanceof ClanApplication application) {
                OfflinePlayer applicant = Bukkit.getOfflinePlayer(application.applicantId());
                String applicantName = applicant.getName() == null ? applicant.getUniqueId().toString() : applicant.getName();

                inventory.setItem(targetSlot, ItemBuilder.of(Material.PLAYER_HEAD)
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

                inventory.setItem(targetSlot, ItemBuilder.of(Material.PAPER)
                        .name(plugin.getMessages().component("gui.applications.invite-item.name",
                                Map.of("player", invitedName), player))
                        .lore(plugin.getMessages().component("gui.applications.invite-item.status", player))
                        .mutate(meta -> meta.getPersistentDataContainer().set(
                                plugin.getGuiManager().memberKey(),
                                PersistentDataType.STRING,
                                INVITE_TAG + invite.invitedPlayer()))
                        .build());
            }
        }

        if (combined.isEmpty()) {
            int emptySlot = invSize / 2 - (invSize >= 54 ? 4 : 4);
            inventory.setItem(Math.max(0, emptySlot), ItemBuilder.of(Material.PAPER)
                    .name(plugin.getMessages().component("gui.applications.empty.name", player))
                    .lore(plugin.getMessages().component("gui.applications.empty.lore", player))
                    .build());
        }

        int lastRowStart = invSize - 9;
        if (page > 0) {
            inventory.setItem(lastRowStart, ItemBuilder.head(ItemBuilder.HEAD_PREVIOUS)
                    .name(plugin.getMessages().component("gui.previous-page", player))
                    .build());
        }
        if (page < maxPage) {
            inventory.setItem(lastRowStart + 8, ItemBuilder.head(ItemBuilder.HEAD_NEXT)
                    .name(plugin.getMessages().component("gui.next-page", player))
                    .build());
        }

        inventory.setItem(lastRowStart + 4, ItemBuilder.head(ItemBuilder.HEAD_BACK)
                .name(plugin.getMessages().component("gui.back", player))
                .build());

        player.openInventory(inventory);
    }

    public void handleInventoryClick(InventoryClickEvent event, Player player, Clan clan) {
        int slot = event.getRawSlot();
        int invSize = event.getView().getTopInventory().getSize();
        int lastRowStart = invSize - 9;
        int backSlot = lastRowStart + 4;
        int prevSlot = lastRowStart;
        int nextSlot = lastRowStart + 8;

        if (slot == backSlot) {
            pageByPlayer.remove(player.getUniqueId());
            plugin.getGuiManager().openMain(player, clan);
            return;
        }
        if (slot == prevSlot) {
            open(player, clan, pageByPlayer.getOrDefault(player.getUniqueId(), 0) - 1);
            return;
        }
        if (slot == nextSlot) {
            open(player, clan, pageByPlayer.getOrDefault(player.getUniqueId(), 0) + 1);
            return;
        }

        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

        String rawId = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(
                plugin.getGuiManager().memberKey(), PersistentDataType.STRING);
        if (rawId == null || rawId.startsWith(INVITE_TAG)) return;

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

    public void clearPlayer(UUID playerId) {
        pageByPlayer.remove(playerId);
    }

    private static int computeSize(int totalItems) {
        if (totalItems <= 7) return 27;
        if (totalItems <= 14) return 36;
        if (totalItems <= 21) return 45;
        return 54;
    }

    private static int[] computeContentSlots(int invSize) {
        List<Integer> slots = new ArrayList<>();
        int contentRowCount = (invSize / 9) - 2;
        for (int row = 0; row < contentRowCount; row++) {
            int rowStart = 9 + row * 9;
            for (int col = 1; col <= 7; col++) {
                slots.add(rowStart + col);
            }
        }
        return slots.stream().mapToInt(i -> i).toArray();
    }

    private void fillGlass(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                    .name(Component.empty())
                    .build());
        }
    }
}
