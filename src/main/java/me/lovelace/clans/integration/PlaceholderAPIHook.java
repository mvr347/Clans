package me.lovelace.clans.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanMember;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

public final class PlaceholderAPIHook extends PlaceholderExpansion {
    private final ClansPlugin plugin;

    public PlaceholderAPIHook(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "clans";
    }

    @Override
    public @NotNull String getAuthor() {
        return "lovelace";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        Optional<Clan> clanOptional = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        String key = params.toLowerCase(Locale.ROOT);
        if (key.equals("in_clan")) {
            return String.valueOf(clanOptional.isPresent());
        }
        if (clanOptional.isEmpty()) {
            return "";
        }
        Clan clan = clanOptional.get();
        return switch (key) {
            case "id" -> clan.id().toString();
            case "name" -> clan.name();
            case "tag" -> clan.tag();
            case "colored_tag" -> clan.coloredTag();
            case "description" -> clan.description();
            case "level" -> String.valueOf(clan.level());
            case "experience", "xp" -> String.valueOf(clan.experience());
            case "members" -> String.valueOf(clan.members().size());
            case "territories" -> String.valueOf(clan.territories().size());
            case "spirit_level" -> String.valueOf(clan.spirit().level());
            case "rank" -> clan.member(player.getUniqueId()).map(ClanMember::rank).map(Enum::name).orElse("");
            case "rank_display" -> clan.member(player.getUniqueId()).map(ClanMember::rank).map(rank -> rank.displayName()).orElse("");
            default -> "";
        };
    }
}
