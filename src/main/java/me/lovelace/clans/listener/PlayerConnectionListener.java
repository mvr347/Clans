package me.lovelace.clans.listener;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.war.ClanWar;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class PlayerConnectionListener implements Listener {
    private final ClansPlugin plugin;

    public PlayerConnectionListener(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getClanManager().updateLastSeen(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getClanManager().updateLastSeen(player.getUniqueId(), System.currentTimeMillis());
        
        // Handle banner drop if holding it during war
        Optional<Clan> clanOpt = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clanOpt.isPresent()) {
            for (ClanWar war : plugin.getWarManager().activeWars()) {
                if (war.capturedBannerBy() != null && war.capturedBannerBy().equals(player.getUniqueId())) {
                    plugin.getWarManager().resetBannerCapture(war.id());
                    // Remove banner from inventory
                    for (int i = 0; i < player.getInventory().getSize(); i++) {
                        ItemStack item = player.getInventory().getItem(i);
                        if (item != null && item.getType().name().endsWith("_BANNER")) {
                            player.getInventory().setItem(i, null);
                        }
                    }
                    plugin.getMessages().send(player, "war.banner-dropped");
                    break;
                }
            }
        }
    }
}