package me.lovelace.clans.manager;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.gui.ClanApplicationsMenu;
import me.lovelace.clans.gui.ClanCapitalManagementMenu;
import me.lovelace.clans.gui.ClanDiplomacyMenu;
import me.lovelace.clans.gui.ClanDiplomacySelectMenu;
import me.lovelace.clans.gui.ClanListMenu;
import me.lovelace.clans.gui.ClanMainMenu;
import me.lovelace.clans.gui.ClanMembersMenu;
import me.lovelace.clans.gui.ClanMenuHolder; // Добавлено
import me.lovelace.clans.gui.ClanMenuType; // Добавлено
import me.lovelace.clans.gui.ClanOtherTerritoriesMenu;
import me.lovelace.clans.gui.ClanSettingsMenu;
import me.lovelace.clans.gui.ClanSpiritMenu;
import me.lovelace.clans.gui.ClanTerritoriesMenu;
import me.lovelace.clans.gui.ClanTerritoriesSelectionGui;
import me.lovelace.clans.gui.ClanUpgradesMenu;
import me.lovelace.clans.gui.TerritorySettingsMenu;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanTerritory;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder; // Добавлено

public class GuiManager implements Listener {

    private final ClansPlugin plugin;
    private final NamespacedKey memberKey;
    private final ClanDiplomacyMenu diplomacyMenu;
    private final ClanTerritoriesMenu territoriesMenu;
    private final ClanMembersMenu membersMenu;
    private final ClanUpgradesMenu upgradesMenu;
    private final ClanSettingsMenu settingsMenu;
    private final ClanApplicationsMenu applicationsMenu;

    public GuiManager(ClansPlugin plugin) {
        this.plugin = plugin;
        this.memberKey = new NamespacedKey(plugin, "gui_member");
        this.diplomacyMenu = new ClanDiplomacyMenu(plugin);
        this.territoriesMenu = new ClanTerritoriesMenu(plugin);
        this.membersMenu = new ClanMembersMenu(plugin);
        this.upgradesMenu = new ClanUpgradesMenu(plugin);
        this.settingsMenu = new ClanSettingsMenu(plugin);
        this.applicationsMenu = new ClanApplicationsMenu(plugin);
    }

    public NamespacedKey memberKey() {
        return memberKey;
    }

    // ── Main menu ──────────────────────────────────────────────────────────────

    public void openMain(Player player, Clan clan) {
        new ClanMainMenu(plugin, clan, player).open();
    }

    public void openMembers(Player player, Clan clan) {
        membersMenu.open(player, clan);
    }

    public void openUpgrades(Player player, Clan clan) {
        upgradesMenu.open(player, clan);
    }

    public void openSettings(Player player, Clan clan) {
        settingsMenu.open(player, clan);
    }

    public void openApplications(Player player, Clan clan) {
        applicationsMenu.open(player, clan);
    }

    // ── Territory menus ────────────────────────────────────────────────────────

    public void openSpiritMenu(Player player, Clan clan) {
        new ClanSpiritMenu(plugin, clan).open(player);
    }

    public void openClanTerritoriesMenu(Player player, Clan clan) {
        new ClanTerritoriesSelectionGui(plugin, clan, player).open();
    }

    public void openClanCapitalManagementMenu(Player player, Clan clan) {
        new ClanCapitalManagementMenu(plugin, clan, player).open();
    }

    public void openClanOtherTerritoriesMenu(Player player, Clan clan) {
        new ClanOtherTerritoriesMenu(plugin, clan, player).open();
    }

    public void openTerritories(Player player, Clan clan) {
        territoriesMenu.open(player, clan);
    }

    public void openTerritorySettings(Player player, Clan clan, ClanTerritory territory) {
        new TerritorySettingsMenu(plugin, clan, territory).open(player);
    }

    // ── Diplomacy ──────────────────────────────────────────────────────────────

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

    // ── Misc ───────────────────────────────────────────────────────────────────

    public void openClanList(Player player) {
        new ClanListMenu(plugin, player).open();
    }

    public void clearPlayerCache(java.util.UUID playerId) {
        applicationsMenu.clearPlayer(playerId);
    }

    // ── Click routing for new InventoryHolder menus ────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlot() < 0) return;

        InventoryHolder holder = event.getView().getTopInventory().getHolder();

        if (holder instanceof ClanMainMenu mainMenu) {
            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
            event.setCancelled(true);
            mainMenu.handleInventoryClick(player, event.getRawSlot());
            return;
        }

        if (holder instanceof ClanCapitalManagementMenu capitalMenu) {
            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
            event.setCancelled(true);
            capitalMenu.handleInventoryClick(player, event.getRawSlot());
            return;
        }

        if (holder instanceof ClanOtherTerritoriesMenu otherMenu) {
            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
            event.setCancelled(true);
            otherMenu.handleInventoryClick(player, event.getRawSlot());
            return;
        }

        // Обработка ClanMenuHolder
        if (holder instanceof ClanMenuHolder clanMenuHolder) {
            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
            event.setCancelled(true);

            switch (clanMenuHolder.type()) {
                case UPGRADES -> upgradesMenu.handleInventoryClick(player, event.getRawSlot());
                // Добавьте другие типы меню, если они используют ClanMenuHolder
                // case MEMBERS -> membersMenu.handleInventoryClick(player, event.getRawSlot());
                // case TERRITORIES -> territoriesMenu.handleInventoryClick(player, event.getRawSlot());
                // case APPLICATIONS -> applicationsMenu.handleInventoryClick(player, event.getRawSlot());
                // case DIPLOMACY -> diplomacyMenu.handleInventoryClick(player, event.getRawSlot());
                default -> {
                    // Возможно, логирование или другая обработка неизвестного типа
                }
            }
        }
    }
}
