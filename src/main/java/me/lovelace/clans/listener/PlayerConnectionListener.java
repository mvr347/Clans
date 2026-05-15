package me.lovelace.clans.listener;

import me.lovelace.clans.ClansPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
        plugin.getClanManager().updateLastSeen(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }
}
