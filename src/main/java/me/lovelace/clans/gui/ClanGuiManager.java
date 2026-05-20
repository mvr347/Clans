package me.lovelace.clans.gui;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanPermission;
import me.lovelace.clans.model.ClanMember;
import me.lovelace.clans.model.ClanRank;
import me.lovelace.clans.model.ClanTerritory;
import me.lovelace.clans.model.ClanUpgrade;
import me.lovelace.clans.model.DiplomacyRelation;
import me.lovelace.clans.model.TerritoryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClanGuiManager implements Listener {
    private final ClansPlugin plugin;
    private final ClanMembersMenu membersMenu;
    private final ClanTerritoriesMenu territoriesMenu;
    private final ClanUpgradesMenu upgradesMenu;
    private final ClanSettingsMenu settingsMenu;
    private final ClanApplicationsMenu applicationsMenu;
    private final ClanConfirmMenu confirmMenu;
    private final ClanDiplomacyMenu diplomacyMenu;
    private final ClanMemberDetailMenu memberDetailMenu;
    private final ClanColorPickerMenu colorPickerMenu;
    private final NamespacedKey memberKey;

    private final Map<UUID, Runnable> confirmYes = new ConcurrentHashMap<>();
    private final Map<UUID, Runnable> confirmNo = new ConcurrentHashMap<>();

    public ClanGuiManager(ClansPlugin plugin) {
        this.plugin = plugin;
        this.membersMenu = new ClanMembersMenu(plugin);
        this.territoriesMenu = new ClanTerritoriesMenu(plugin);
        this.upgradesMenu = new ClanUpgradesMenu(plugin);
        this.settingsMenu = new ClanSettingsMenu(plugin);
        this.applicationsMenu = new ClanApplicationsMenu(plugin);
        this.confirmMenu = new ClanConfirmMenu(plugin);
        this.diplomacyMenu = new ClanDiplomacyMenu(plugin);
        this.memberDetailMenu = new ClanMemberDetailMenu(plugin);
        this.colorPickerMenu = new ClanColorPickerMenu(plugin);
        this.memberKey = new NamespacedKey(plugin, "gui_member");
    }

    public NamespacedKey memberKey() { return memberKey; }

    public void openMain(Player player, Clan clan) { new ClanMainMenu(plugin, clan, player).open(); }
    public void openMembers(Player player, Clan clan) { membersMenu.open(player, clan); }
    public void openTerritories(Player player, Clan clan) { territoriesMenu.open(player, clan); }
    public void openUpgrades(Player player, Clan clan) { upgradesMenu.open(player, clan); }
    public void openSettings(Player player, Clan clan) { settingsMenu.open(player, clan); }
    public void openApplications(Player player, Clan clan) { applicationsMenu.open(player, clan); }
    public void openClanList(Player player) { new ClanListMenu(plugin, player).open(); }

    public void openDiplomacy(Player player, Clan sourceClan, Clan targetClan) {
        diplomacyMenu.open(player, sourceClan, targetClan);
    }

    public void openDiplomacySelect(Player player, Clan sourceClan) {
        ClanDiplomacySelectMenu menu = new ClanDiplomacySelectMenu(plugin, player, sourceClan);
        if (!menu.hasClans()) {
            plugin.getMessages().send(player, "diplomacy.no-other-clans");
            return;
        }
        menu.open();
    }

    public void openCreateMenu(Player player) { new ClanCreateMenu(plugin, player).open(); }

    public void openMemberDetail(Player player, Clan clan, UUID targetId) {
        memberDetailMenu.open(player, clan, targetId);
    }

    public void openColorPicker(Player player, Clan clan) { colorPickerMenu.open(player, clan); }

    public void openTerritorySettings(Player player, Clan clan, ClanTerritory territory) {
        new TerritorySettingsMenu(plugin, clan, territory).open(player);
    }

    public void openConfirm(Player player, Clan clan, Component title, Component lore, Runnable onYes, Runnable onNo) {
        confirmYes.put(player.getUniqueId(), onYes);
        confirmNo.put(player.getUniqueId(), onNo);
        confirmMenu.open(player, clan, title, lore);
    }
    
    public void openRoleSettings(Player player, Clan clan) {
        new ClanRoleSettingsMenu(plugin, clan).open(player, clan);
    }

    public void openRankPermissions(Player player, Clan clan, ClanRank rank) {
        new ClanRankPermissionsMenu(plugin, clan, rank).open(player, clan);
    }

    public void openSpirit(Player player, Clan clan) {
        new ClanSpiritMenu(plugin, clan).open(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlot() < 0) return;

        // Cancel shift-clicks from bottom inventory into our custom GUIs
        if (event.getView().getTopInventory().getHolder() != null &&
            (event.getView().getTopInventory().getHolder() instanceof ClanMenuHolder ||
             event.getView().getTopInventory().getHolder() instanceof ClanMainMenu ||
             event.getView().getTopInventory().getHolder() instanceof ClanCreateMenu ||
             event.getView().getTopInventory().getHolder() instanceof ClanListMenu ||
             event.getView().getTopInventory().getHolder() instanceof ClanInfoMenu ||
             event.getView().getTopInventory().getHolder() instanceof ClanCapitalManagementMenu ||
             event.getView().getTopInventory().getHolder() instanceof TerritorySettingsMenu ||
             event.getView().getTopInventory().getHolder() instanceof ClanSpiritMenu)) {
             
             if (event.getRawSlot() >= event.getView().getTopInventory().getSize() && event.isShiftClick()) {
                 event.setCancelled(true);
             }
        }

        if (event.getView().getTopInventory().getHolder() instanceof ClanMainMenu mainMenu) {
            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
            event.setCancelled(true);
            mainMenu.handleInventoryClick(player, event.getRawSlot());
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof ClanCreateMenu createMenu) {
            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
            event.setCancelled(true);
            createMenu.handleInventoryClick(event.getRawSlot());
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof ClanListMenu clanListMenu) {
            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
            event.setCancelled(true);
            clanListMenu.handleInventoryClick(event.getRawSlot());
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof ClanDiplomacySelectMenu selectMenu) {
            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
            event.setCancelled(true);
            selectMenu.handleInventoryClick(event.getRawSlot());
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof ClanInfoMenu infoMenu) {
            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
            event.setCancelled(true);
            if (event.getRawSlot() == 22) {
                Clan clan = infoMenu.clan();
                if (clan.isOpen()) {
                    plugin.getClanManager().applyToClanAsync(clan, player.getUniqueId())
                            .thenRun(() -> plugin.runSync(() -> {
                                plugin.getMessages().send(player, "clan.applied", Map.of("tag", clan.tag()));
                                player.closeInventory();
                            }))
                            .exceptionally(t -> {
                                plugin.runSync(() -> plugin.sendOperationError(player, t));
                                return null;
                            });
                }
            }
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof PlayerApplicationsMenu appMenu) {
            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
            event.setCancelled(true);
            appMenu.handleInventoryClick(event.getRawSlot());
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof TerritorySettingsMenu tsMenu) {
            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
            event.setCancelled(true);
            tsMenu.handleInventoryClick(player, event.getRawSlot());
            return;
        }

        if (event.getView().getTopInventory().getHolder() instanceof ClanSpiritMenu spiritMenu) {
            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
            event.setCancelled(true);
            spiritMenu.handleInventoryClick(player, event.getRawSlot());
            return;
        }

        if (!(event.getView().getTopInventory().getHolder() instanceof ClanMenuHolder holder)) return;
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        event.setCancelled(true);

        Optional<Clan> optionalClan = plugin.getClanManager().getClanById(holder.clanId());
        if (optionalClan.isEmpty()) { player.closeInventory(); return; }
        Clan clan = optionalClan.get();

        switch (holder.type()) {
            case MEMBERS -> handleMembersClick(event.getRawSlot(), player, clan);
            case TERRITORIES -> territoriesMenu.handleTerritoryClick(player, clan, event.getRawSlot(), event.isRightClick());
            case UPGRADES -> handleUpgradesClick(event.getRawSlot(), player, clan);
            case SETTINGS -> handleSettingsClick(event.getRawSlot(), player, clan);
            case APPLICATIONS -> applicationsMenu.handleInventoryClick(event, player, clan);
            case CONFIRM -> handleConfirmClick(event.getRawSlot(), player);
            case DIPLOMACY -> handleDiplomacyClick(event.getRawSlot(), player, clan);
            case MEMBER_DETAIL -> handleMemberDetailClick(event, player, clan);
            case COLOR_PICKER -> handleColorPickerClick(event.getRawSlot(), player, clan);
            case ROLE_SETTINGS -> handleRoleSettingsClick(event.getRawSlot(), player, clan);
            case RANK_PERMISSIONS -> handleRankPermissionsClick(event, player, clan);
            default -> {}
        }
    }

    private void handleRoleSettingsClick(int slot, Player player, Clan clan) {
        if (slot == 22) { openSettings(player, clan); return; }
        ItemStack item = player.getOpenInventory().getTopInventory().getItem(slot);
        if (item == null || !item.hasItemMeta()) return;
        String rankName = item.getItemMeta().getPersistentDataContainer()
                .get(memberKey, PersistentDataType.STRING);
        if (rankName == null) return;
        try {
            ClanRank rank = ClanRank.valueOf(rankName);
            openRankPermissions(player, clan, rank);
        } catch (IllegalArgumentException ignored) {}
    }
    
    private void handleRankPermissionsClick(InventoryClickEvent event, Player player, Clan clan) {
        int slot = event.getRawSlot();
        if (slot == 49) { openRoleSettings(player, clan); return; }
        
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        
        String permName = item.getItemMeta().getPersistentDataContainer()
                .get(memberKey, PersistentDataType.STRING);
        if (permName == null) return;
        
        try {
            ClanPermission permission = ClanPermission.valueOf(permName);
            if (event.getInventory().getHolder() instanceof ClanRankPermissionsMenu menu) {
                ClanRank rank = menu.getRank();
                boolean current = clan.getPermission(rank, permission);
                clan.setPermission(rank, permission, !current);
                plugin.getStorage().saveClanAsync(clan).thenRun(() -> plugin.runSync(() -> {
                    openRankPermissions(player, clan, rank);
                }));
            }
        } catch (IllegalArgumentException ignored) {}
    }

    private void handleMembersClick(int slot, Player player, Clan clan) {
        int inventorySize = player.getOpenInventory().getTopInventory().getSize();
        int backButtonSlot = inventorySize - 5;
        int inviteButtonSlot = inventorySize - 4;

        if (slot == backButtonSlot) {
            openMain(player, clan);
            return;
        }
        
        if (slot == inviteButtonSlot) {
            boolean canInvite = clan.hasPermission(player.getUniqueId(), ClanPermission.INVITE);
            if (canInvite) {
                if (plugin.getClanManager().isClanFull(clan)) {
                    plugin.getMessages().send(player, "clan.member-limit-reached");
                    return;
                }
                player.closeInventory();
                plugin.getMessages().send(player, "gui.members.invite.prompt");
                plugin.expectChatInput(player.getUniqueId(), (inputName, isCancelled) -> {
                    if (isCancelled) {
                        plugin.runSync(() -> openMembers(player, clan));
                        return;
                    }
                    plugin.getServer().dispatchCommand(player, "clan invite " + inputName);
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                       if (plugin.getClanManager().getPlayerClan(player.getUniqueId()).isPresent()) {
                           openMembers(player, plugin.getClanManager().getPlayerClan(player.getUniqueId()).get());
                       }
                    }, 10L);
                });
            } else {
                plugin.getMessages().send(player, "general.no-permission");
            }
            return;
        }
        
        ItemStack item = player.getOpenInventory().getTopInventory().getItem(slot);
        if (item == null || !item.hasItemMeta()) return;
        String playerId = item.getItemMeta().getPersistentDataContainer()
                .get(memberKey, PersistentDataType.STRING);
        if (playerId == null) return;
        try {
            UUID targetId = UUID.fromString(playerId);
            if (targetId.equals(player.getUniqueId())) return;
            openMemberDetail(player, clan, targetId);
        } catch (IllegalArgumentException ignored) {}
    }

    private void handleUpgradesClick(int slot, Player player, Clan clan) {
        if (slot == 49) { openMain(player, clan); return; }
        
        boolean canUpgrade = clan.hasPermission(player.getUniqueId(), ClanPermission.UPGRADE);
        if (!canUpgrade) {
            plugin.getMessages().send(player, "general.no-permission"); return;
        }
        
        ClanUpgrade upgrade = switch (slot) {
            case 20 -> ClanUpgrade.MEMBERS;
            case 21 -> ClanUpgrade.TERRITORIES;
            case 22 -> ClanUpgrade.LOOTING;
            case 23 -> ClanUpgrade.SPIRIT;
            case 24 -> ClanUpgrade.WARFARE;
            default -> null;
        };
        
        if (upgrade != null) {
            if (clan.upgradePoints() > 0) {
                clan.removeUpgradePoints(1);
                clan.setUpgradeLevel(upgrade, clan.upgradeLevel(upgrade) + 1);
                plugin.getStorage().saveClanAsync(clan).thenRun(() -> plugin.runSync(() -> {
                    plugin.getMessages().send(player, "gui.upgrades.success", Map.of("upgrade", upgrade.displayName()));
                    openUpgrades(player, clan);
                }));
            } else {
                plugin.getMessages().send(player, "gui.upgrades.not-enough-points");
            }
        }
    }

    private void handleSettingsClick(int slot, Player player, Clan clan) {
        if (slot == 13) { openMain(player, clan); return; }
        if (!clan.hasPermission(player.getUniqueId(), ClanPermission.SETTINGS)) {
            plugin.getMessages().send(player, "general.no-permission"); return;
        }
        switch (slot) {
            case 1 -> {
                player.closeInventory();
                plugin.getMessages().send(player, "gui.settings.rename.prompt");
                plugin.expectChatInput(player.getUniqueId(), (newName, isCancelled) -> {
                    if (isCancelled) {
                        plugin.runSync(() -> openSettings(player, clan));
                        return;
                    }
                    int min = plugin.getConfig().getInt("clans.name.min-length", 4);
                    int max = plugin.getConfig().getInt("clans.name.max-length", 10);
                    if (newName.trim().length() < min || newName.trim().length() > max) {
                        plugin.getMessages().send(player, "clan.invalid-name",
                                Map.of("min", String.valueOf(min), "max", String.valueOf(max)));
                        plugin.runSync(() -> openSettings(player, clan));
                        return;
                    }
                    openConfirm(player, clan, plugin.getMessages().component("gui.confirm.rename.title", player), Component.empty(),
                        () -> plugin.getClanManager().renameClanAsync(clan, player.getUniqueId(), newName.trim())
                            .thenAccept(updated -> plugin.runSync(() -> {
                                plugin.getMessages().send(player, "gui.settings.rename.success", Map.of("name", updated.name()));
                                openSettings(player, updated);
                            }))
                            .exceptionally(t -> { plugin.runSync(() -> plugin.sendOperationError(player, t)); return null; }),
                        () -> plugin.runSync(() -> openSettings(player, clan))
                    );
                });
            }
            case 2 -> {
                player.closeInventory();
                plugin.getMessages().send(player, "gui.settings.change-tag.prompt");
                plugin.expectChatInput(player.getUniqueId(), (newTag, isCancelled) -> {
                    if (isCancelled) {
                        plugin.runSync(() -> openSettings(player, clan));
                        return;
                    }
                    int min = plugin.getConfig().getInt("clans.tag.min-length", 3);
                    int max = plugin.getConfig().getInt("clans.tag.max-length", 6);
                    String pattern = plugin.getConfig().getString("clans.tag.pattern", "^[A-Za-z0-9_]+$");
                    if (newTag.trim().length() < min || newTag.trim().length() > max || !newTag.trim().matches(pattern)) {
                        plugin.getMessages().send(player, "clan.invalid-tag",
                                Map.of("min", String.valueOf(min), "max", String.valueOf(max)));
                        plugin.runSync(() -> openSettings(player, clan));
                        return;
                    }
                    openConfirm(player, clan, plugin.getMessages().component("gui.confirm.change-tag.title", player), Component.empty(),
                        () -> plugin.getClanManager().changeClanTagAsync(clan, player.getUniqueId(), newTag.trim())
                            .thenAccept(updated -> plugin.runSync(() -> {
                                plugin.getMessages().send(player, "gui.settings.change-tag.success", Map.of("tag", updated.tag()));
                                openSettings(player, updated);
                            }))
                            .exceptionally(t -> { plugin.runSync(() -> plugin.sendOperationError(player, t)); return null; }),
                        () -> plugin.runSync(() -> openSettings(player, clan))
                    );
                });
            }
            case 3 -> {
                ItemStack inHand = player.getInventory().getItemInMainHand();
                if (!inHand.getType().name().endsWith("_BANNER")) {
                    plugin.getMessages().send(player, "gui.settings.change-banner.no-banner-in-hand"); return;
                }
                org.bukkit.Material bannerMat = inHand.getType();
                plugin.getClanManager().changeClanEmblemAsync(clan, player.getUniqueId(), bannerMat)
                    .thenAccept(updated -> plugin.runSync(() -> {
                        plugin.getMessages().send(player, "gui.settings.change-banner.success");
                        openSettings(player, updated);
                    }))
                    .exceptionally(t -> { plugin.runSync(() -> plugin.sendOperationError(player, t)); return null; });
            }
            case 4 -> openColorPicker(player, clan);
            case 5 -> { // Toggle clan open/closed status
                openConfirm(player, clan, clan.isOpen() ? plugin.getMessages().component("gui.confirm.close-clan.title", player) : plugin.getMessages().component("gui.confirm.open-clan.title", player), Component.empty(),
                    () -> plugin.getClanManager().setClanOpenStatusAsync(clan, player.getUniqueId(), !clan.isOpen())
                        .thenAccept(updated -> plugin.runSync(() -> {
                            plugin.getMessages().send(player, updated.isOpen() ? "gui.settings.status.open.success" : "gui.settings.status.closed.success");
                            openSettings(player, updated);
                        }))
                        .exceptionally(t -> { plugin.runSync(() -> plugin.sendOperationError(player, t)); return null; }),
                    () -> plugin.runSync(() -> openSettings(player, clan))
                );
            }
            case 6 -> openRoleSettings(player, clan);
            case 7 -> { // Disband button
                if (!isGuildmaster(clan, player.getUniqueId())) {
                    plugin.getMessages().send(player, "general.no-permission"); return;
                }
                openConfirm(player, clan, plugin.getMessages().component("gui.confirm.disband.title", player), Component.empty(),
                    () -> {
                        player.closeInventory();
                        plugin.getClanManager().disbandClanAsync(clan, player.getUniqueId())
                            .thenRun(() -> plugin.runSync(() -> plugin.getMessages().send(player, "clan.disbanded")))
                            .exceptionally(t -> { plugin.runSync(() -> plugin.sendOperationError(player, t)); return null; });
                    },
                    () -> plugin.runSync(() -> openSettings(player, clan))
                );
            }
            default -> {}
        }
    }

    private void handleConfirmClick(int slot, Player player) {
        UUID id = player.getUniqueId();
        if (slot == 11) {
            Runnable yes = confirmYes.remove(id);
            confirmNo.remove(id);
            player.closeInventory();
            if (yes != null) yes.run();
        } else if (slot == 15) {
            Runnable no = confirmNo.remove(id);
            confirmYes.remove(id);
            player.closeInventory();
            if (no != null) no.run();
        }
    }

    private void handleDiplomacyClick(int slot, Player player, Clan targetClan) {
        Optional<Clan> sourceClanOpt = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (sourceClanOpt.isEmpty()) { player.closeInventory(); return; }
        Clan sourceClan = sourceClanOpt.get();

        if (slot == 22) { openDiplomacySelect(player, sourceClan); return; }

        DiplomacyRelation relation = switch (slot) {
            case 10 -> DiplomacyRelation.ALLY;
            case 13 -> DiplomacyRelation.NEUTRAL;
            case 16 -> DiplomacyRelation.ENEMY;
            default -> null;
        };
        if (relation == null) return;

        if (relation == DiplomacyRelation.ALLY) {
            if (sourceClan.relationTo(targetClan.id()) == DiplomacyRelation.ALLY) {
                plugin.getClanManager().setDiplomacyAsync(sourceClan, targetClan, DiplomacyRelation.NEUTRAL, player.getUniqueId())
                    .thenAccept(updated -> plugin.runSync(() -> {
                        plugin.getMessages().send(player, "diplomacy.updated", Map.of("tag", targetClan.tag(), "relation", "NEUTRAL"));
                        openDiplomacy(player, updated, targetClan);
                    }))
                    .exceptionally(t -> { plugin.runSync(() -> plugin.sendOperationError(player, t)); return null; });
                return;
            }
            if (plugin.getClanManager().hasPendingAllianceFrom(targetClan.id(), sourceClan.id())) {
                plugin.getClanManager().acceptAllianceAsync(sourceClan, targetClan, player.getUniqueId())
                    .thenRun(() -> plugin.runSync(() -> {
                        plugin.getMessages().send(player, "diplomacy.alliance-accepted", Map.of("tag", targetClan.tag()));
                        plugin.getClanManager().getOnlineLeader(targetClan).ifPresent(leader ->
                                plugin.getMessages().send(leader, "diplomacy.alliance-accepted-by", Map.of("tag", sourceClan.tag())));
                        openDiplomacy(player, sourceClan, targetClan);
                    }))
                    .exceptionally(t -> { plugin.runSync(() -> plugin.sendOperationError(player, t)); return null; });
                return;
            }
            plugin.getClanManager().addAllianceRequest(sourceClan.id(), targetClan.id());
            plugin.getMessages().send(player, "diplomacy.alliance-sent", Map.of("tag", targetClan.tag()));
            plugin.getClanManager().getOnlineLeader(targetClan).ifPresent(leader ->
                    plugin.getMessages().sendClickableAlliance(leader, sourceClan.tag()));
            openDiplomacy(player, sourceClan, targetClan);
            return;
        }

        plugin.getClanManager().setDiplomacyAsync(sourceClan, targetClan, relation, player.getUniqueId())
            .thenAccept(updated -> plugin.runSync(() -> {
                plugin.getMessages().send(player, "diplomacy.updated", Map.of("tag", targetClan.tag(), "relation", relation.name()));
                openDiplomacy(player, updated, targetClan);
            }))
            .exceptionally(t -> { plugin.runSync(() -> plugin.sendOperationError(player, t)); return null; });
    }

    private void handleMemberDetailClick(InventoryClickEvent event, Player player, Clan clan) {
        int slot = event.getRawSlot();
        if (slot == 22) { openMembers(player, clan); return; }

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String rawId = item.getItemMeta().getPersistentDataContainer().get(memberKey, PersistentDataType.STRING);
        if (rawId == null) return;

        UUID targetId;
        try { targetId = UUID.fromString(rawId); } catch (IllegalArgumentException ignored) { return; }

        Optional<ClanMember> targetMemberOpt = clan.member(targetId);
        if (targetMemberOpt.isEmpty()) return;
        ClanRank targetRank = targetMemberOpt.get().rank();

        switch (slot) {
            case 14 -> {
                ClanRank newRank = targetRank.nextRank();
                if (newRank != null) {
                    plugin.getClanManager().setRankAsync(clan, player.getUniqueId(), targetId, newRank)
                        .thenAccept(updated -> plugin.runSync(() -> openMemberDetail(player, updated, targetId)))
                        .exceptionally(t -> { plugin.runSync(() -> plugin.sendOperationError(player, t)); return null; });
                }
            }
            case 15 -> {
                ClanRank newRank = targetRank.previousRank();
                if (newRank != null) {
                    plugin.getClanManager().setRankAsync(clan, player.getUniqueId(), targetId, newRank)
                        .thenAccept(updated -> plugin.runSync(() -> openMemberDetail(player, updated, targetId)))
                        .exceptionally(t -> { plugin.runSync(() -> plugin.sendOperationError(player, t)); return null; });
                }
            }
            case 16 -> {
                plugin.getClanManager().removeMemberAsync(clan, player.getUniqueId(), targetId, true)
                    .thenRun(() -> plugin.runSync(() -> {
                        plugin.getMessages().send(player, "clan.kicked", Map.of("player", nameOf(targetId)));
                        openMembers(player, clan);
                    }))
                    .exceptionally(t -> { plugin.runSync(() -> plugin.sendOperationError(player, t)); return null; });
            }
            case 12 -> {
                openConfirm(player, clan, plugin.getMessages().component("gui.confirm.transfer.title", player), Component.empty(),
                    () -> plugin.getClanManager().transferLeadershipAsync(clan, player.getUniqueId(), targetId)
                        .thenRun(() -> plugin.runSync(() -> openMembers(player, clan)))
                        .exceptionally(t -> { plugin.runSync(() -> plugin.sendOperationError(player, t)); return null; }),
                    () -> plugin.runSync(() -> openMemberDetail(player, clan, targetId))
                );
            }
            default -> {}
        }
    }

    private void handleColorPickerClick(int slot, Player player, Clan clan) {
        if (slot == 22) { openSettings(player, clan); return; }
        ItemStack item = player.getOpenInventory().getTopInventory().getItem(slot);
        if (item == null || !item.hasItemMeta()) return;
        String colorTag = item.getItemMeta().getPersistentDataContainer().get(memberKey, PersistentDataType.STRING);
        if (colorTag == null) return;
        plugin.getClanManager().changeTagColorAsync(clan, player.getUniqueId(), colorTag)
            .thenAccept(updated -> plugin.runSync(() -> {
                plugin.getMessages().send(player, "gui.settings.change-color.success");
                openColorPicker(player, updated);
            }))
            .exceptionally(t -> { plugin.runSync(() -> plugin.sendOperationError(player, t)); return null; });
    }

    private boolean isGuildmaster(Clan clan, UUID playerId) {
        return clan.member(playerId).map(m -> m.rank() == ClanRank.LEADER).orElse(false);
    }

    private String nameOf(UUID id) {
        String name = org.bukkit.Bukkit.getOfflinePlayer(id).getName();
        return name != null ? name : id.toString();
    }
}
