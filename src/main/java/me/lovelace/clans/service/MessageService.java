package me.lovelace.clans.service;

import me.lovelace.clans.ClansPlugin;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MessageService {
    private final ClansPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration lang;
    private Method papiSetPlaceholders;

    public MessageService(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "lang.yml");
        if (!file.exists()) {
            plugin.saveResource("lang.yml", false);
        }
        lang = YamlConfiguration.loadConfiguration(file);
        try (InputStream stream = plugin.getResource("lang.yml")) {
            if (stream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
                lang.setDefaults(defaults);
                lang.options().copyDefaults(true);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Unable to load default lang.yml from jar: " + exception.getMessage());
        }
        resolvePlaceholderApi();
    }

    public Component component(String key) {
        return component(key, Map.of(), null);
    }

    public Component component(String key, Map<String, String> placeholders) {
        return component(key, placeholders, null);
    }

    public Component component(String key, Player player) {
        return component(key, Map.of(), player);
    }

    public Component component(String key, Map<String, String> placeholders, Player player) {
        String raw = lang.getString(key);
        if (raw == null) {
            raw = key;
        }
        return parse(raw, placeholders, player);
    }

    public List<Component> components(String key, Player player) {
        return components(key, Map.of(), player);
    }

    public List<Component> components(String key, Map<String, String> placeholders, Player player) {
        if (lang.isList(key)) {
            List<String> list = lang.getStringList(key);
            List<Component> components = new ArrayList<>();
            for (String raw : list) {
                components.add(parse(raw, placeholders, player));
            }
            return components;
        } else {
            String raw = lang.getString(key);
            if (raw == null) raw = key;
            return List.of(parse(raw, placeholders, player));
        }
    }

    private Component parse(String raw, Map<String, String> placeholders, Player player) {
        String prefix = lang.getString("prefix", "");
        raw = raw.replace("<prefix>", prefix);

        List<TagResolver> resolvers = new ArrayList<>();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String val = entry.getValue();
            if (val.contains("<") && val.contains(">")) {
                resolvers.add(Placeholder.parsed(entry.getKey(), val));
            } else {
                resolvers.add(Placeholder.component(entry.getKey(), Component.text(val)));
            }
        }

        raw = applyPlaceholders(player, raw);
        return miniMessage.deserialize(raw, TagResolver.resolver(resolvers));
    }

    public String raw(String key) {
        String raw = lang.getString(key);
        if (raw == null) {
            raw = key;
        }
        return raw.replace("<prefix>", lang.getString("prefix", ""));
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        Player player = sender instanceof Player playerSender ? playerSender : null;
        Audience audience = sender;
        audience.sendMessage(component(key, placeholders, player));
    }

    public void sendClickableApplication(Player leader, String applicantName, String clanTag) {
        Component msg = component("clan.application-received",
                Map.of("player", applicantName, "tag", clanTag), leader)
                .append(Component.text(" "))
                .append(component("gui.accept", leader)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(
                                "/clan applications accept " + applicantName)))
                .append(Component.text(" "))
                .append(component("gui.reject", leader)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(
                                "/clan applications reject " + applicantName)));
        leader.sendMessage(msg);
    }

    public void sendClickableInvite(Player target, String clanTag) {
        Component msg = component("clan.invite-received",
                Map.of("tag", clanTag), target)
                .append(Component.text(" "))
                .append(component("gui.accept", target)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/clan accept " + clanTag)))
                .append(Component.text(" "))
                .append(component("gui.reject", target)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/clan decline " + clanTag)));
        target.sendMessage(msg);
    }

    public void sendClickableAlliance(Player guildmaster, String sourceClanTag) {
        Component msg = component("diplomacy.alliance-request",
                Map.of("tag", sourceClanTag), guildmaster)
                .append(Component.text(" "))
                .append(component("gui.accept", guildmaster)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(
                                "/clan ally accept " + sourceClanTag)))
                .append(Component.text(" "))
                .append(component("gui.reject", guildmaster)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(
                                "/clan ally decline " + sourceClanTag)));
        guildmaster.sendMessage(msg);
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