package me.lovelace.clans.listener;

import me.lovelace.clans.ClansPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ChatInputListener implements Listener {
    private final ClansPlugin plugin;

    public ChatInputListener(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        plugin.getChatInputListener(player.getUniqueId()).ifPresent(callback -> {
            event.setCancelled(true);
            String message = event.getMessage();
            if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("отмена")) {
                plugin.runSync(() -> {
                    callback.accept(null, true);
                    plugin.getMessages().send(player, "general.chat-input-cancelled");
                });
            } else {
                plugin.runSync(() -> callback.accept(message, false));
            }
        });
    }
}