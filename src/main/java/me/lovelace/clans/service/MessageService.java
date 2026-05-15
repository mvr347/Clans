package me.lovelace.clans.service;

import me.lovelace.clans.ClansPlugin;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;

public final class MessageService {
    private final ClansPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration messages;
    private Method papiSetPlaceholders;

    public MessageService(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
        resolvePlaceholderApi();
    }

    public Component component(String key) {
        return component(key, Map.of(), null);
    }

    public Component component(String key, Map<String, String> placeholders) {
        return component(key, placeholders, null);
    }

    public Component component(String key, Map<String, String> placeholders, Player player) {
        String raw = messages.getString(key, key);
        raw = raw.replace("<prefix>", messages.getString("prefix", ""));
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("<" + entry.getKey() + ">", miniMessage.escapeTags(entry.getValue()));
        }
        raw = applyPlaceholders(player, raw);
        return miniMessage.deserialize(raw);
    }

    public String raw(String key) {
        String raw = messages.getString(key, key);
        return raw.replace("<prefix>", messages.getString("prefix", ""));
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        Player player = sender instanceof Player playerSender ? playerSender : null;
        Audience audience = sender;
        audience.sendMessage(component(key, placeholders, player));
    }

    private String applyPlaceholders(Player player, String raw) {
        if (player == null || papiSetPlaceholders == null || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return raw;
        }
        try {
            Object value = papiSetPlaceholders.invoke(null, player, raw);
            return value instanceof String string ? string : raw;
        } catch (ReflectiveOperationException exception) {
            return raw;
        }
    }

    private void resolvePlaceholderApi() {
        papiSetPlaceholders = null;
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        try {
            Class<?> placeholderApi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            papiSetPlaceholders = placeholderApi.getMethod("setPlaceholders", Player.class, String.class);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("PlaceholderAPI found, but setPlaceholders(Player, String) is unavailable.");
        }
    }
}
