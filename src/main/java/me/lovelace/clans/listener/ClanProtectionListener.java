package me.lovelace.clans.listener;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.gui.ClanConfirmMenu; // Added import
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanTerritory;
import me.lovelace.clans.model.TerritoryKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent; // Added import
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBurnEvent; // Added import
import org.bukkit.event.entity.EntityExplodeEvent; // Added import
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClanProtectionListener implements Listener {
    private final ClansPlugin plugin;
    private final Map<UUID, TerritoryKey> pendingClaims = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> particleTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> messageCooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> glowingPlayers = new HashSet<>(); // Track players currently glowing

    public ClanProtectionListener(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();

        // Handle breaking claim banners during war
        if (block.getType().name().endsWith("_BANNER")) {
            Optional<Clan> territoryClanOpt = plugin.getClanManager().getClanAt(location);
            if (territoryClanOpt.isPresent()) {
                Clan territoryClan = territoryClanOpt.get();
                TerritoryKey key = TerritoryKey.fromLocation(location);
                Optional<ClanTerritory> territoryOpt = territoryClan.territory(key);
                
                if (territoryOpt.isPresent()) {
                    ClanTerritory territory = territoryOpt.get();
                    if (territory.bannerX() != null && territory.bannerX() == location.getBlockX() &&
                        territory.bannerY() != null && territory.bannerY() == location.getBlockY() &&
                        territory.bannerZ() != null && territory.bannerZ() == location.getBlockZ()) {
                        
                        Optional<Clan> playerClanOpt = plugin.getClanManager().getPlayerClan(player.getUniqueId());
                        if (playerClanOpt.isPresent() && plugin.getWarManager().areAtWar(playerClanOpt.get().id(), territoryClan.id())) {
                            // Give player the war banner and start capture sequence
                            plugin.getWarManager().activeWar(playerClanOpt.get().id(), territoryClan.id()).ifPresent(war -> {
                                plugin.getWarManager().setBannerCapture(war.id(), player.getUniqueId());
                                ItemStack warBanner = new ItemStack(block.getType());
                                player.getInventory().addItem(warBanner);
                                plugin.getMessages().send(player, "war.banner-captured", Map.of("tag", territoryClan.tag()));
                            });
                            return; // Allow breaking
                        } else if (playerClanOpt.isPresent() && playerClanOpt.get().id().equals(territoryClan.id())) {
                            // Clan members cannot break their own banner directly, show confirmation
                            event.setCancelled(true);
                            plugin.getGuiManager().openConfirm(player,
                                    territoryClan, // Pass the clan object
                                    plugin.getMessages().component("gui.confirm.unclaim-banner.name", player),
                                    plugin.getMessages().component("gui.confirm.unclaim-banner.lore", player),
                                    () -> {
                                        // On confirm, unclaim the territory
                                        plugin.getClanManager().unclaimTerritoryAsync(territoryClan, key, player.getUniqueId())
                                                .thenAccept(v -> plugin.runSync(() -> {
                                                    plugin.getMessages().send(player, "territory.unclaimed");
                                                    // Break the block after confirmation
                                                    block.breakNaturally();
                                                }))
                                                .exceptionally(t -> {
                                                    plugin.runSync(() -> plugin.sendOperationError(player, t));
                                                    return null;
                                                });
                                    },
                                    () -> {
                                        // On cancel, do nothing
                                        player.closeInventory();
                                    });
                            return;
                        } else {
                             sendProtectedMessage(player, territoryClan, "territory.protected");
                             event.setCancelled(true);
                             return;
                        }
                    }
                }
            }
        }

        if (!canInteract(player, location)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!canInteract(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractClaimBanner(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || !event.getItem().getType().name().endsWith("_BANNER")) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!item.hasItemMeta()) return;

        String rawClanId = item.getItemMeta().getPersistentDataContainer()
                .get(plugin.getGuiManager().memberKey(), PersistentDataType.STRING);
        if (rawClanId == null) return;

        event.setCancelled(true);

        try {
            UUID clanId = UUID.fromString(rawClanId);
            Optional<Clan> clanOpt = plugin.getClanManager().getClanById(clanId);
            if (clanOpt.isEmpty()) return;
            Clan clan = clanOpt.get();

            if (!plugin.getClanManager().getPlayerClan(player.getUniqueId()).map(c -> c.id().equals(clanId)).orElse(false)) {
                sendProtectedMessage(player, null, "general.no-permission");
                return;
            }

            TerritoryKey key = TerritoryKey.fromLocation(event.getClickedBlock().getLocation());
            Optional<Clan> existingClan = plugin.getClanManager().getClanAt(key);
            
            if (existingClan.isPresent()) {
                sendProtectedMessage(player, existingClan.get(), "territory.already-claimed");
                return;
            }

            if (pendingClaims.containsKey(player.getUniqueId()) && pendingClaims.get(player.getUniqueId()).equals(key)) {
                // Confirm claim
                if (plugin.getAdvancedClaimsHook().enabled()) {
                    plugin.getAdvancedClaimsHook().hideClaimBorder(player);
                } else if (particleTasks.containsKey(player.getUniqueId())) {
                    particleTasks.get(player.getUniqueId()).cancel();
                    particleTasks.remove(player.getUniqueId());
                }
                
                Location placeLoc = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation();
                plugin.getClanManager().claimTerritoryAsync(clan, event.getClickedBlock().getChunk(), player)
                        .thenAccept(territory -> {
                            ClanTerritory updated = territory.withBannerCoords(placeLoc.getBlockX(), placeLoc.getBlockY(), placeLoc.getBlockZ());
                            clan.addTerritory(updated);
                            plugin.runSync(() -> {
                                plugin.getMessages().send(player, "territory.claimed");
                                pendingClaims.remove(player.getUniqueId());
                                // Place the block
                                placeLoc.getBlock().setType(item.getType());
                                // Consume the banner item
                                item.setAmount(item.getAmount() - 1);
                            });
                        })
                        .exceptionally(t -> {
                            plugin.runSync(() -> plugin.sendOperationError(player, t));
                            return null;
                        });
            } else {
                // Mark for claim
                pendingClaims.put(player.getUniqueId(), key);
                plugin.getMessages().send(player, "territory.claim-marked");
                
                // Start particle task
                if (plugin.getAdvancedClaimsHook().enabled()) {
                    World world = player.getWorld();
                    int minX = key.chunkX() << 4;
                    int minZ = key.chunkZ() << 4;
                    BoundingBox box = new BoundingBox(minX, world.getMinHeight(), minZ, minX + 15, world.getMaxHeight(), minZ + 15);
                    plugin.getAdvancedClaimsHook().showClaimBorder(player, box, 20 * 60); // Show for 1 minute
                } else if (particleTasks.containsKey(player.getUniqueId())) {
                    particleTasks.get(player.getUniqueId()).cancel();
                }
                
                if (!plugin.getAdvancedClaimsHook().enabled()) {
                    BukkitRunnable task = new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!player.isOnline() || !pendingClaims.containsKey(player.getUniqueId()) || !pendingClaims.get(player.getUniqueId()).equals(key)) {
                                this.cancel();
                                particleTasks.remove(player.getUniqueId());
                                return;
                            }
                            visualizeChunk(player, key);
                        }
                    };
                    task.runTaskTimer(plugin, 0L, 20L);
                    particleTasks.put(player.getUniqueId(), task);
                }
            }
        } catch (IllegalArgumentException ignored) {}
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        
        // Skip check if it's the claim banner interaction which is handled above
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getItem() != null && event.getItem().getType().name().endsWith("_BANNER")) {
            if (event.getItem().hasItemMeta() && event.getItem().getItemMeta().getPersistentDataContainer().has(plugin.getGuiManager().memberKey(), PersistentDataType.STRING)) {
                return;
            }
        }

        // Middle-click visualization with a stick
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getItem() != null && event.getItem().getType() == Material.STICK) {
            Player player = event.getPlayer();
            Location clickedLocation = event.getClickedBlock().getLocation();
            TerritoryKey key = TerritoryKey.fromLocation(clickedLocation);

            Optional<Clan> territoryClanOpt = plugin.getClanManager().getClanAt(clickedLocation);
            if (territoryClanOpt.isPresent()) {
                Clan territoryClan = territoryClanOpt.get();
                // Show claim border for 5 seconds (100 ticks)
                World world = player.getWorld();
                int minX = key.chunkX() << 4;
                int minZ = key.chunkZ() << 4;
                BoundingBox box = new BoundingBox(minX, world.getMinHeight(), minZ, minX + 15, world.getMaxHeight(), minZ + 15);
                plugin.getAdvancedClaimsHook().showClaimBorder(player, box, 100);
                plugin.getMessages().send(player, "territory.visualization-shown", Map.of("tag", territoryClan.tag()));
            } else {
                plugin.getMessages().send(player, "territory.not-claimed");
            }
            event.setCancelled(true); // Cancel the event to prevent default stick action
            return;
        }
        
        if (!canInteract(event.getPlayer(), event.getClickedBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Block blockAbove = block.getRelative(0, 1, 0);
            if (isBlockUnderBanner(block, blockAbove)) {
                return !isBannerBreakableInWar(blockAbove.getLocation());
            }
            return false;
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        Block toBlock = event.getToBlock();
        Block blockAbove = toBlock.getRelative(0, 1, 0);
        if (isBlockUnderBanner(toBlock, blockAbove)) {
            if (!isBannerBreakableInWar(blockAbove.getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        Block blockAbove = block.getRelative(0, 1, 0);
        if (isBlockUnderBanner(block, blockAbove)) {
            if (!isBannerBreakableInWar(blockAbove.getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isBlockUnderBanner(Block block, Block blockAbove) {
        return blockAbove.getType().name().endsWith("_BANNER");
    }

    private boolean isBannerBreakableInWar(Location bannerLocation) {
        Optional<Clan> territoryClanOpt = plugin.getClanManager().getClanAt(bannerLocation);
        if (territoryClanOpt.isPresent()) {
            Clan territoryClan = territoryClanOpt.get();
            TerritoryKey key = TerritoryKey.fromLocation(bannerLocation);
            Optional<ClanTerritory> territoryOpt = territoryClan.territory(key);

            if (territoryOpt.isPresent()) {
                ClanTerritory territory = territoryOpt.get();
                if (territory.bannerX() != null && territory.bannerX() == bannerLocation.getBlockX() &&
                    territory.bannerY() != null && territory.bannerY() == bannerLocation.getBlockY() &&
                    territory.bannerZ() != null && territory.bannerZ() == bannerLocation.getBlockZ()) {
                    
                    // Check if there's an active war where this banner can be broken
                    // This logic needs to be refined based on how your war system determines breakability.
                    // For now, it returns false (protected) unless a specific war condition is met.
                    // Example: return plugin.getWarManager().isBannerTargetInActiveWar(territoryClan.id(), key);
                    return false; // Protected by default
                }
            }
        }
        return false; // Not a banner or not a protected banner
    }

    private boolean canInteract(Player player, Location location) {
        Optional<Clan> territoryClan = plugin.getClanManager().getClanAt(location);
        if (territoryClan.isEmpty()) {
            return true;
        }
        Optional<Clan> playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (playerClan.isPresent() && playerClan.get().id().equals(territoryClan.get().id())) {
            return true;
        }
        if (playerClan.isPresent() && plugin.getWarManager().areAtWar(playerClan.get().id(), territoryClan.get().id())) {
            return true;
        }
        
        // Check for PvP territory setting
        TerritoryKey key = TerritoryKey.fromLocation(location);
        Optional<ClanTerritory> territoryOpt = territoryClan.get().territory(key); // Corrected method call
        if (territoryOpt.isPresent() && territoryOpt.get().pvp()) {
            // Might need additional checks depending on interaction type, but allow by default if PvP is on
            return true;
        }

        sendProtectedMessage(player, territoryClan.get(), "territory.protected");
        return false;
    }

    private void sendProtectedMessage(Player player, Clan clan, String messageKey) {
        long now = System.currentTimeMillis();
        long last = messageCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 2000) return; // 2 second cooldown

        messageCooldowns.put(player.getUniqueId(), now);
        if (clan != null) {
            plugin.getMessages().send(player, messageKey, Map.of("tag", clan.tag()));
        } else {
            plugin.getMessages().send(player, messageKey);
        }
    }
    
    private void visualizeChunk(Player player, TerritoryKey key) {
        World world = player.getWorld();
        int minX = key.chunkX() << 4;
        int minZ = key.chunkZ() << 4;
        int maxX = minX + 16;
        int maxZ = minZ + 16;

        for (int x = minX; x <= maxX; x++) {
            drawVerticalLine(player, world, x, minZ, x == minX || x == maxX);
            drawVerticalLine(player, world, x, maxZ, x == minX || x == maxX);
        }
        for (int z = minZ + 1; z < maxZ; z++) {
            drawVerticalLine(player, world, minX, z, false);
            drawVerticalLine(player, world, maxX, z, false);
        }
    }

    private void drawVerticalLine(Player player, World world, int x, int z, boolean isCorner) {
        int y = world.getHighestBlockYAt(x, z);
        double height = 4.0; // Fixed height for visualization
        for (double offY = 0; offY < height; offY += 0.5) {
            player.spawnParticle(Particle.HAPPY_VILLAGER, x + 0.5, y + offY + 1.1, z + 0.5, 1, 0, 0, 0, 0);
        }
    }

    public void updateGlowingPlayers() {
        Set<UUID> playersToStopGlowing = new HashSet<>(glowingPlayers); // Players that were glowing, might need to stop

        for (Player player : Bukkit.getOnlinePlayers()) {
            Optional<Clan> playerClanOpt = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (playerClanOpt.isEmpty()) {
                // Player is not in a clan, ensure they are not glowing
                if (glowingPlayers.contains(player.getUniqueId())) {
                    player.setGlowing(false);
                    glowingPlayers.remove(player.getUniqueId());
                }
                continue;
            }

            Clan playerClan = playerClanOpt.get();
            Location playerLocation = player.getLocation();
            Optional<Clan> territoryClanOpt = plugin.getClanManager().getClanAt(playerLocation);

            if (territoryClanOpt.isPresent()) {
                Clan territoryClan = territoryClanOpt.get();

                // Check if player's clan and territory's clan are at war
                if (plugin.getWarManager().areAtWar(playerClan.id(), territoryClan.id())) {
                    // If the player is an enemy in the territory (not their own clan's territory)
                    if (!playerClan.id().equals(territoryClan.id())) {
                        if (!glowingPlayers.contains(player.getUniqueId())) {
                            player.setGlowing(true);
                            glowingPlayers.add(player.getUniqueId());
                        }
                        playersToStopGlowing.remove(player.getUniqueId()); // This player should continue glowing
                    } else {
                        // Player is in their own clan's territory during war, should not glow
                        if (glowingPlayers.contains(player.getUniqueId())) {
                            player.setGlowing(false);
                            glowingPlayers.remove(player.getUniqueId());
                        }
                    }
                } else {
                    // Not at war, ensure player is not glowing
                    if (glowingPlayers.contains(player.getUniqueId())) {
                        player.setGlowing(false);
                        glowingPlayers.remove(player.getUniqueId());
                    }
                }
            } else {
                // Player is in unclaimed territory, ensure they are not glowing
                if (glowingPlayers.contains(player.getUniqueId())) {
                    player.setGlowing(false);
                    glowingPlayers.remove(player.getUniqueId());
                }
            }
        }

        // Ensure any players who were glowing but are no longer online or no longer meet criteria stop glowing
        for (UUID playerId : playersToStopGlowing) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isGlowing()) {
                player.setGlowing(false);
            }
            glowingPlayers.remove(playerId);
        }
    }
}