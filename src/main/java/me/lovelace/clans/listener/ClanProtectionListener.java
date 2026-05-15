package me.lovelace.clans.listener;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Map;
import java.util.Optional;

public final class ClanProtectionListener implements Listener {
    private final ClansPlugin plugin;

    public ClanProtectionListener(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!canInteract(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!canInteract(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        if (!canInteract(event.getPlayer(), event.getClickedBlock().getLocation())) {
            event.setCancelled(true);
        }
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
        plugin.getMessages().send(player, "territory.protected", Map.of("tag", territoryClan.get().tag()));
        return false;
    }
}
