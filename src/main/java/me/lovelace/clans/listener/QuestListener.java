package me.lovelace.clans.listener;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.manager.ClanManager;
import me.lovelace.clans.manager.QuestManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuestListener implements Listener {
    private final ClansPlugin plugin;
    private final QuestManager questManager;
    private final ClanManager clanManager;

    public QuestListener(ClansPlugin plugin, QuestManager questManager, ClanManager clanManager) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.clanManager = clanManager;
    }

    private void updateQuestProgress(Player player, Map<String, Object> eventData) {
        clanManager.getPlayerClan(player.getUniqueId()).ifPresent(clan -> {
            questManager.updateQuestProgress(clan.id(), player.getUniqueId(), eventData)
                    .exceptionally(ex -> {
                        plugin.getLogger().severe("Error updating quest progress for clan " + clan.id() + ": " + ex.getMessage());
                        return null;
                    });
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() instanceof Player killer) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("entity_type", event.getEntity().getType());
            eventData.put("killer_id", killer.getUniqueId());
            eventData.put("victim_is_player", event.getEntity() instanceof Player);
            updateQuestProgress(killer, eventData);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("block_type", event.getBlock().getType());
        updateQuestProgress(player, eventData);

        // For GatherItemObjective, if the item drops directly from the block
        // This might need more sophisticated handling if items are picked up later
        // For now, we assume breaking a block counts as gathering the block itself.
        // A separate PlayerPickupItemEvent handler might be needed for more general item gathering.
        eventData.put("item_type", event.getBlock().getType()); // Assuming block type is item type
        eventData.put("amount", 1); // Assuming 1 item per block break
        updateQuestProgress(player, eventData);
    }

    private final Map<UUID, Location> lastPlayerLocation = new HashMap<>();
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // Only check every few blocks or seconds to prevent spam
        if (to.distanceSquared(from) < 1.0 && to.getBlockX() == from.getBlockX() && to.getBlockY() == from.getBlockY() && to.getBlockZ() == from.getBlockZ()) {
            return; // Player didn't move a significant distance
        }

        // Optional: Add a cooldown or distance check to avoid excessive updates
        Location lastLoc = lastPlayerLocation.get(player.getUniqueId());
        if (lastLoc != null && lastLoc.distanceSquared(to) < 9.0) { // Check every 3 blocks
            return;
        }
        lastPlayerLocation.put(player.getUniqueId(), to);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("player_location", to);
        updateQuestProgress(player, eventData);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            ItemStack result = event.getRecipe().getResult();
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("crafted_item_type", result.getType());
            eventData.put("amount", result.getAmount());
            updateQuestProgress(player, eventData);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        // Assuming we only care about the first enchantment applied
        Enchantment enchantment = event.getEnchantsToAdd().keySet().stream().findFirst().orElse(null);
        if (enchantment != null) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("enchantment_type", enchantment);
            updateQuestProgress(player, eventData);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            if (event.getCaught() instanceof org.bukkit.entity.Item caughtItem) {
                Player player = event.getPlayer();
                ItemStack item = caughtItem.getItemStack();
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("fished_item_type", item.getType());
                eventData.put("amount", item.getAmount());
                updateQuestProgress(player, eventData);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player player) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("bred_animal_type", event.getEntity().getType());
            updateQuestProgress(player, eventData);
        } else if (event.getFather() instanceof Player player) { // Fallback for cases where breeder might be null
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("bred_animal_type", event.getEntity().getType());
            updateQuestProgress(player, eventData);
        } else if (event.getMother() instanceof Player player) { // Fallback for cases where breeder might be null
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("bred_animal_type", event.getEntity().getType());
            updateQuestProgress(player, eventData);
        }
    }
}
