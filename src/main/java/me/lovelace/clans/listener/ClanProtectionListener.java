package me.lovelace.clans.listener;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.manager.ClanManager;
import me.lovelace.clans.manager.WarManager;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanRank;
import me.lovelace.clans.model.ClanTerritory;
import me.lovelace.clans.model.TerritoryKey;
import me.lovelace.clans.util.ClanItemFactory;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent; // New import
import org.bukkit.event.player.PlayerQuitEvent; // New import
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.UUID;

public class ClanProtectionListener implements Listener {

    private final ClansPlugin plugin;
    private final ClanManager clanManager;
    private final WarManager warManager;

    public ClanProtectionListener(ClansPlugin plugin, ClanManager clanManager, WarManager warManager) {
        this.plugin = plugin;
        this.clanManager = clanManager;
        this.warManager = warManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItemInHand();
        Block placedBlock = event.getBlockPlaced();

        // Check if the player is trying to place a clan banner
        if (!itemInHand.hasItemMeta() || !itemInHand.getType().toString().endsWith("_BANNER")) {
            return; // Not a banner or no meta
        }

        ItemMeta meta = itemInHand.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String bannerType = pdc.get(ClanItemFactory.BANNER_TYPE_KEY, PersistentDataType.STRING);
        String clanIdString = pdc.get(ClanItemFactory.CLAN_ID_KEY, PersistentDataType.STRING);

        if (bannerType == null || clanIdString == null) {
            return; // Not a clan banner
        }

        UUID clanId = UUID.fromString(clanIdString);
        Optional<Clan> clanOpt = clanManager.getClanById(clanId);

        if (clanOpt.isEmpty()) {
            plugin.getMessages().send(player, "territory.banner.invalid-clan");
            event.setCancelled(true);
            return;
        }

        Clan clan = clanOpt.get();

        // **ЗАЩИТА ОТ ДУРАЧКОВ**: Проверяем, что игрок является членом клана, которому принадлежит баннер
        if (!clan.hasMember(player.getUniqueId())) {
            plugin.getMessages().send(player, "territory.banner.not-your-clan");
            event.setCancelled(true);
            return;
        }

        // Check if player is confirming an existing pending claim
        if (clanManager.hasPendingClaim(player.getUniqueId())) {
            event.setCancelled(true); // Always cancel the event, confirmation logic will handle actual placement
            clanManager.confirmPendingClaim(player, placedBlock.getLocation())
                    .thenAccept(territory -> plugin.runSync(() -> {
                        // Success message is sent by ClanManager
                        // The block is actually placed by the player, so we don't need to do anything here
                    }))
                    .exceptionally(throwable -> {
                        plugin.runSync(() -> plugin.sendOperationError(player, throwable));
                        return null;
                    });
        } else {
            // This is an initiation of a new claim
            event.setCancelled(true); // Cancel the event, we'll handle placement after confirmation

            boolean initiated = clanManager.initiateClaimConfirmation(player, clan, placedBlock.getLocation(), bannerType);
            if (!initiated) {
                // If initiation failed, we don't need to do anything since the event is already cancelled
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (clanManager.hasPendingClaim(player.getUniqueId())) {
            clanManager.cancelPendingClaim(player.getUniqueId()).ifPresent(pendingClaim -> {
                // Give the banner back to the player
                ItemStack banner = plugin.getClanManager().getClanItemFactory().createBannerByType(
                        pendingClaim.bannerType(),
                        pendingClaim.clan().id(),
                        pendingClaim.clan().name()
                );
                giveItemBack(player, banner);
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (clanManager.hasPendingClaim(player.getUniqueId())) {
            clanManager.cancelPendingClaim(player.getUniqueId()).ifPresent(pendingClaim -> {
                // Give the banner back to the player
                ItemStack banner = plugin.getClanManager().getClanItemFactory().createBannerByType(
                        pendingClaim.bannerType(),
                        pendingClaim.clan().id(),
                        pendingClaim.clan().name()
                );
                giveItemBack(player, banner);
                plugin.getLogger().info("Cancelled pending claim for " + player.getName() + " due to logout.");
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getClickedBlock() == null || !event.getAction().isRightClick()) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (!clickedBlock.getType().toString().endsWith("_BANNER")) {
            return;
        }

        Optional<PersistentDataContainer> blockPdcOpt = getPDCFromBlock(clickedBlock);

        if (blockPdcOpt.isEmpty()) {
            return; // Not a clan banner block
        }
        PersistentDataContainer pdc = blockPdcOpt.get();

        String bannerType = pdc.get(ClanItemFactory.BANNER_TYPE_KEY, PersistentDataType.STRING);
        String clanIdString = pdc.get(ClanItemFactory.CLAN_ID_KEY, PersistentDataType.STRING);

        if (bannerType == null || clanIdString == null) {
            return;
        }

        UUID clanId = UUID.fromString(clanIdString);
        Optional<Clan> clanOpt = clanManager.getClanById(clanId);

        if (clanOpt.isEmpty()) {
            return;
        }

        Clan clan = clanOpt.get();
        Player player = event.getPlayer();

        if ("TERRITORY".equals(bannerType)) {
            if (clan.member(player.getUniqueId()).map(m -> m.rank() == ClanRank.LEADER).orElse(false)) {
                event.setCancelled(true);
                clan.territories().stream()
                        .filter(t -> !t.isCapital()
                                && t.bannerX() != null
                                && t.bannerX() == clickedBlock.getX()
                                && t.bannerY() != null
                                && t.bannerY() == clickedBlock.getY()
                                && t.bannerZ() != null
                                && t.bannerZ() == clickedBlock.getZ())
                        .findFirst()
                        .ifPresent(territory -> plugin.getGuiManager().openTerritorySettings(player, clan, territory));
            }
            return;
        }

        if (!"CAPITAL".equals(bannerType)) {
            return;
        }

        // Check if player is Guildmaster of this clan
        if (clan.member(player.getUniqueId()).map(m -> m.rank() == ClanRank.LEADER).orElse(false)) {
            event.setCancelled(true);
            plugin.getGuiManager().openClanCapitalManagementMenu(player, clan);
        } else {
            plugin.getMessages().send(player, "gui.territories.capital.no-permission"); // "Управление базой доступно только лидеру."
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block brokenBlock = event.getBlock();
        Player player = event.getPlayer();

        // First, check for protection of the bearing block
        Block blockAbove = brokenBlock.getRelative(BlockFace.UP);
        if (blockAbove.getType().toString().endsWith("_BANNER")) {
            Optional<PersistentDataContainer> blockAbovePdcOpt = getPDCFromBlock(blockAbove);
            if (blockAbovePdcOpt.isPresent()) {
                PersistentDataContainer pdcAbove = blockAbovePdcOpt.get();

                String bannerTypeAbove = pdcAbove.get(ClanItemFactory.BANNER_TYPE_KEY, PersistentDataType.STRING);
                String clanIdStringAbove = pdcAbove.get(ClanItemFactory.CLAN_ID_KEY, PersistentDataType.STRING);

                if ("CAPITAL".equals(bannerTypeAbove) && clanIdStringAbove != null) {
                    UUID clanId = UUID.fromString(clanIdStringAbove);
                    // Check if the clan is currently at war
                    if (warManager.isAtWar(clanId)) {
                        // During war, bearing block can be broken
                        return;
                    } else {
                        plugin.getMessages().send(player, "territory.capital.cannot-break-bearing-block"); // "<red>Нельзя сломать блок под Баннером Столицы!"
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        // Now, check if the broken block itself is a Capital Banner
        if (!brokenBlock.getType().toString().endsWith("_BANNER")) {
            return; // Not a banner
        }

        Optional<PersistentDataContainer> blockPdcOpt = getPDCFromBlock(brokenBlock);
        if (blockPdcOpt.isEmpty()) {
            return; // Not a clan banner block
        }
        PersistentDataContainer pdc = blockPdcOpt.get();

        String bannerType = pdc.get(ClanItemFactory.BANNER_TYPE_KEY, PersistentDataType.STRING);
        String clanIdString = pdc.get(ClanItemFactory.CLAN_ID_KEY, PersistentDataType.STRING);

        if (!"CAPITAL".equals(bannerType) || clanIdString == null) {
            return; // Not a Capital Banner
        }

        UUID clanId = UUID.fromString(clanIdString);

        if (warManager.isAtWar(clanId)) {
            // During war, Capital Banner can be broken
            // TODO: Add logic for war consequences (e.g., clan loses capital, etc.)
        } else {
            plugin.getMessages().send(player, "territory.capital.cannot-break-peace"); // "<red>Нельзя разрушить Баннер Столицы в мирное время!"
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        // Check if victim is in a clan
        Optional<Clan> victimClanOpt = clanManager.getPlayerClan(victim.getUniqueId());
        if (victimClanOpt.isEmpty()) {
            return;
        }
        Clan victimClan = victimClanOpt.get();

        // Check if victim is in their Capital Territory
        Optional<ClanTerritory> capitalTerritoryOpt = victimClan.territories().stream().filter(ClanTerritory::isCapital).findFirst();
        if (capitalTerritoryOpt.isEmpty()) {
            return;
        }
        ClanTerritory capital = capitalTerritoryOpt.get();

        // Check if victim's location is within the capital territory's claim
        if (plugin.getAdvancedClaimsHook().isClaimed(victim.getLocation())) {
            // If the victim's clan is at war, force PvP
            if (warManager.isAtWar(victimClan.id())) {
                event.setCancelled(false); // Ensure PvP is active
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();
        ItemStack itemStack = item.getItemStack();

        if (isClanBanner(itemStack)) {
            event.setCancelled(true);
            plugin.getMessages().send(event.getPlayer(), "territory.banner.cannot-drop");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        for (ItemStack item : event.getDrops()) {
            if (isClanBanner(item)) {
                event.getItemsToKeep().add(item);
            }
        }
        event.getDrops().removeIf(this::isClanBanner);
    }

    private boolean isClanBanner(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(ClanItemFactory.BANNER_TYPE_KEY, PersistentDataType.STRING) &&
               pdc.has(ClanItemFactory.CLAN_ID_KEY, PersistentDataType.STRING);
    }

    /**
     * Helper method to get PersistentDataContainer from a Block (assuming it's a BlockEntity like a banner).
     * This is a more robust implementation using BlockState.
     *
     * @param block The block to check.
     * @return Optional containing the PDC if found and applicable, otherwise empty.
     */
    private Optional<PersistentDataContainer> getPDCFromBlock(Block block) {
        BlockState blockState = block.getState();
        if (blockState instanceof org.bukkit.block.Banner bannerState) {
            return Optional.of(bannerState.getPersistentDataContainer());
        }
        return Optional.empty();
    }

    /**
     * Helper method to give an item back to the player's inventory or drop it if full.
     * @param player The player to give the item to.
     * @param item The item to give back.
     */
    private void giveItemBack(Player player, ItemStack item) {
        if (player.getInventory().addItem(item).size() > 0) {
            // Inventory was full, drop the item
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }
}
