package me.lovelace.clans.command;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanRank;
import me.lovelace.clans.model.DiplomacyRelation;
import me.lovelace.clans.model.TerritoryKey;
import me.lovelace.clans.model.artifact.ArtifactType;
import me.lovelace.clans.model.ritual.RitualType;
import me.lovelace.clans.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ClanCommand implements CommandExecutor, TabCompleter {
    private static final List<String> ROOT = List.of(
            "help", "create", "disband", "invite", "accept", "leave", "kick", "promote", "demote",
            "info", "claim", "unclaim", "menu", "members", "territories", "upgrades", "chest",
            "war", "ally", "enemy", "neutral", "ritual", "vote", "artifact", "reload"
    );

    private final ClansPlugin plugin;

    public ClanCommand(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(Permissions.COMMAND)) {
            plugin.getMessages().send(sender, "general.no-permission");
            return true;
        }
        if (args.length == 0) {
            if (sender instanceof Player player) {
                openMenu(player);
            } else {
                plugin.getMessages().send(sender, "clan.help");
            }
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        try {
            switch (sub) {
                case "help" -> plugin.getMessages().send(sender, "clan.help");
                case "create" -> create(requirePlayer(sender), args);
                case "disband" -> disband(requirePlayer(sender));
                case "invite" -> invite(requirePlayer(sender), args);
                case "accept" -> accept(requirePlayer(sender), args);
                case "leave" -> leave(requirePlayer(sender));
                case "kick" -> kick(requirePlayer(sender), args);
                case "promote" -> rank(requirePlayer(sender), args, ClanRank.ASSISTANT);
                case "demote" -> rank(requirePlayer(sender), args, ClanRank.MEMBER);
                case "info" -> info(sender, args);
                case "claim" -> claim(requirePlayer(sender));
                case "unclaim" -> unclaim(requirePlayer(sender));
                case "menu" -> openMenu(requirePlayer(sender));
                case "members" -> openMembers(requirePlayer(sender));
                case "territories" -> openTerritories(requirePlayer(sender));
                case "upgrades" -> openUpgrades(requirePlayer(sender));
                case "chest" -> openChest(requirePlayer(sender));
                case "war" -> war(requirePlayer(sender), args);
                case "ally" -> diplomacy(requirePlayer(sender), args, DiplomacyRelation.ALLY);
                case "enemy" -> diplomacy(requirePlayer(sender), args, DiplomacyRelation.ENEMY);
                case "neutral" -> diplomacy(requirePlayer(sender), args, DiplomacyRelation.NEUTRAL);
                case "ritual" -> ritual(requirePlayer(sender), args);
                case "vote" -> vote(requirePlayer(sender), args);
                case "artifact" -> artifact(sender, args);
                case "reload" -> reload(sender);
                default -> plugin.getMessages().send(sender, "general.unknown-command");
            }
        } catch (IllegalStateException exception) {
            plugin.sendOperationError(sender, exception);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(ROOT, args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "accept", "war", "ally", "enemy", "neutral", "info" -> filter(plugin.getClanManager().getAllClans().stream().map(Clan::tag).toList(), args[1]);
                case "ritual" -> filter(Arrays.stream(RitualType.values()).map(type -> type.name().toLowerCase(Locale.ROOT)).toList(), args[1]);
                case "artifact" -> filter(Arrays.stream(ArtifactType.values()).map(type -> type.name().toLowerCase(Locale.ROOT)).toList(), args[1]);
                default -> null;
            };
        }
        return List.of();
    }

    private void create(Player player, String[] args) {
        requirePermission(player, Permissions.CREATE);
        if (args.length < 3) {
            plugin.getMessages().send(player, "clan.help");
            return;
        }
        String tag = args[1];
        String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        plugin.getClanManager().createClanAsync(name, tag, player.getUniqueId())
                .thenAccept(clan -> plugin.runSync(() -> plugin.getMessages().send(player, "clan.created", Map.of("tag", clan.tag()))))
                .exceptionally(throwable -> {
                    plugin.runSync(() -> plugin.sendOperationError(player, throwable));
                    return null;
                });
    }

    private void disband(Player player) {
        requirePermission(player, Permissions.DISBAND);
        Optional<Clan> optionalClan = requireClan(player);
        if (optionalClan.isEmpty()) {
            plugin.getMessages().send(player, "clan.not-in-clan");
            return;
        }
        Clan clan = optionalClan.get();
        plugin.getClanManager().disbandClanAsync(clan, player.getUniqueId())
                .thenRun(() -> plugin.runSync(() -> plugin.getMessages().send(player, "clan.disbanded")))
                .exceptionally(throwable -> {
                    plugin.runSync(() -> plugin.sendOperationError(player, throwable));
                    return null;
                });
    }

    private void invite(Player player, String[] args) {
        requirePermission(player, Permissions.INVITE);
        if (args.length < 2) {
            plugin.getMessages().send(player, "clan.help");
            return;
        }
        Optional<Clan> optionalClan = requireClan(player);
        if (optionalClan.isEmpty()) {
            plugin.getMessages().send(player, "clan.not-in-clan");
            return;
        }
        Clan clan = optionalClan.get();
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.getMessages().send(player, "general.player-not-found", Map.of("player", args[1]));
            return;
        }
        plugin.getClanManager().invitePlayerAsync(clan, player.getUniqueId(), target.getUniqueId())
                .thenAccept(invite -> plugin.runSync(() -> {
                    plugin.getMessages().send(player, "clan.invited", Map.of("player", target.getName()));
                    plugin.getMessages().send(target, "clan.invite-received", Map.of("tag", clan.tag()));
                }))
                .exceptionally(throwable -> {
                    plugin.runSync(() -> plugin.sendOperationError(player, throwable));
                    return null;
                });
    }

    private void accept(Player player, String[] args) {
        requirePermission(player, Permissions.ACCEPT);
        if (args.length < 2) {
            plugin.getMessages().send(player, "clan.help");
            return;
        }
        plugin.getClanManager().acceptInviteAsync(player.getUniqueId(), args[1])
                .thenAccept(clan -> plugin.runSync(() -> plugin.getMessages().send(player, "clan.joined", Map.of("tag", clan.tag()))))
                .exceptionally(throwable -> {
                    plugin.runSync(() -> plugin.sendOperationError(player, throwable));
                    return null;
                });
    }

    private void leave(Player player) {
        requirePermission(player, Permissions.LEAVE);
        Optional<Clan> optionalClan = requireClan(player);
        if (optionalClan.isEmpty()) {
            plugin.getMessages().send(player, "clan.not-in-clan");
            return;
        }
        Clan clan = optionalClan.get();
        plugin.getClanManager().removeMemberAsync(clan, player.getUniqueId(), player.getUniqueId(), false)
                .thenRun(() -> plugin.runSync(() -> plugin.getMessages().send(player, "clan.left")))
                .exceptionally(throwable -> {
                    plugin.runSync(() -> plugin.sendOperationError(player, throwable));
                    return null;
                });
    }

    private void kick(Player player, String[] args) {
        requirePermission(player, Permissions.KICK);
        if (args.length < 2) {
            plugin.getMessages().send(player, "clan.help");
            return;
        }
        Optional<Clan> optionalClan = requireClan(player);
        if (optionalClan.isEmpty()) {
            plugin.getMessages().send(player, "clan.not-in-clan");
            return;
        }
        Clan clan = optionalClan.get();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        plugin.getClanManager().removeMemberAsync(clan, player.getUniqueId(), target.getUniqueId(), true)
                .thenRun(() -> plugin.runSync(() -> plugin.getMessages().send(player, "clan.kicked", Map.of("player", displayName(target)))))
                .exceptionally(throwable -> {
                    plugin.runSync(() -> plugin.sendOperationError(player, throwable));
                    return null;
                });
    }

    private void rank(Player player, String[] args, ClanRank rank) {
        requirePermission(player, Permissions.RANK);
        if (args.length < 2) {
            plugin.getMessages().send(player, "clan.help");
            return;
        }
        Optional<Clan> optionalClan = requireClan(player);
        if (optionalClan.isEmpty()) {
            plugin.getMessages().send(player, "clan.not-in-clan");
            return;
        }
        Clan clan = optionalClan.get();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        plugin.getClanManager().setRankAsync(clan, player.getUniqueId(), target.getUniqueId(), rank)
                .thenAccept(updated -> plugin.runSync(() -> plugin.getMessages().send(player, "clan.rank-changed", Map.of("player", displayName(target), "rank", rank.displayName()))))
                .exceptionally(throwable -> {
                    plugin.runSync(() -> plugin.sendOperationError(player, throwable));
                    return null;
                });
    }

    private void info(CommandSender sender, String[] args) {
        Optional<Clan> clan = args.length >= 2 ? plugin.getClanManager().getClanByTag(args[1])
                : sender instanceof Player player ? plugin.getClanManager().getPlayerClan(player.getUniqueId()) : Optional.empty();
        if (clan.isEmpty()) {
            plugin.getMessages().send(sender, "clan.not-in-clan");
            return;
        }
        Clan value = clan.get();
        plugin.getMessages().send(sender, "clan.info", Map.of(
                "tag", value.tag(),
                "name", value.name(),
                "level", String.valueOf(value.level()),
                "members", String.valueOf(value.members().size()),
                "max_members", String.valueOf(plugin.getClanManager().maxMembers(value))
        ));
    }

    private void claim(Player player) {
        requirePermission(player, Permissions.CLAIM);
        Optional<Clan> optionalClan = requireClan(player);
        if (optionalClan.isEmpty()) {
            plugin.getMessages().send(player, "clan.not-in-clan");
            return;
        }
        Clan clan = optionalClan.get();
        plugin.getClanManager().claimTerritoryAsync(clan, player.getChunk(), player)
                .thenAccept(territory -> plugin.runSync(() -> plugin.getMessages().send(player, "territory.claimed")))
                .exceptionally(throwable -> {
                    plugin.runSync(() -> plugin.sendOperationError(player, throwable));
                    return null;
                });
    }

    private void unclaim(Player player) {
        requirePermission(player, Permissions.UNCLAIM);
        Optional<Clan> optionalClan = requireClan(player);
        if (optionalClan.isEmpty()) {
            plugin.getMessages().send(player, "clan.not-in-clan");
            return;
        }
        Clan clan = optionalClan.get();
        plugin.getClanManager().unclaimTerritoryAsync(clan, TerritoryKey.fromLocation(player.getLocation()), player.getUniqueId())
                .thenRun(() -> plugin.runSync(() -> plugin.getMessages().send(player, "territory.unclaimed")))
                .exceptionally(throwable -> {
                    plugin.runSync(() -> plugin.sendOperationError(player, throwable));
                    return null;
                });
    }

    private void openMenu(Player player) {
        requirePermission(player, Permissions.MENU);
        Optional<Clan> optionalClan = requireClan(player);
        if (optionalClan.isEmpty()) {
            plugin.getMessages().send(player, "clan.not-in-clan");
            return;
        }
        plugin.getGuiManager().openMain(player, optionalClan.get());
    }

    private void openMembers(Player player) {
        requirePermission(player, Permissions.MENU);
        Optional<Clan> optionalClan = requireClan(player);
        if (optionalClan.isEmpty()) {
            plugin.getMessages().send(player, "clan.not-in-clan");
            return;
        }
        plugin.getGuiManager().openMembers(player, optionalClan.get());
    }

    private void openTerritories(Player player) {
        requirePermission(player, Permissions.MENU);
        Optional<Clan> optionalClan = requireClan(player);
        if (optionalClan.isEmpty()) {
            plugin.getMessages().send(player, "clan.not-in-clan");
            return;
        }
        plugin.getGuiManager().openTerritories(player, optionalClan.get());
    }

    private void openUpgrades(Player player) {
        requirePermission(player, Permissions.MENU);
        Optional<Clan> optionalClan = requireClan(player);
        if (optionalClan.isEmpty()) {
            plugin.getMessages().send(player, "clan.not-in-clan");
            return;
        }
        plugin.getGuiManager().openUpgrades(player, optionalClan.get());
    }

    private void openChest(Player player) {
        requirePermission(player, Permissions.CHEST);
        Optional<Clan> optionalClan = requireClan(player);
        if (optionalClan.isEmpty()) {
            plugin.getMessages().send(player, "clan.not-in-clan");
            return;
        }
        plugin.getClanChestManager().openChest(player, optionalClan.get());
    }

    private void war(Player player, String[] args) {
        requirePermission(player, Permissions.WAR);
        if (args.length < 2) {
            plugin.getMessages().send(player, "clan.help");
            return;
        }
        Optional<Clan> optionalAttacker = requireClan(player);
        if (optionalAttacker.isEmpty()) {
            plugin.getMessages().send(player, "clan.not-in-clan");
            return;
        }
        Clan attacker = optionalAttacker.get();
        Clan defender = plugin.getClanManager().getClanByTag(args[1]).orElseThrow(() -> new IllegalStateException("war.not-found"));
        plugin.getWarManager().startWarAsync(attacker, defender, TerritoryKey.fromLocation(player.getLocation()))
                .thenAccept(war -> plugin.runSync(() -> plugin.getMessages().send(player, "war.started", Map.of("attacker", attacker.tag(), "defender", defender.tag()))))
                .exceptionally(throwable -> {
                    plugin.runSync(() -> plugin.sendOperationError(player, throwable));
                    return null;
                });
    }

    private void diplomacy(Player player, String[] args, DiplomacyRelation relation) {
        requirePermission(player, Permissions.DIPLOMACY);
        if (args.length < 2) {
            plugin.getMessages().send(player, "clan.help");
            return;
        }
        Optional<Clan> optionalSource = requireClan(player);
        if (optionalSource.isEmpty()) {
            plugin.getMessages().send(player, "clan.not-in-clan");
            return;
        }
        Clan source = optionalSource.get();
        Clan target = plugin.getClanManager().getClanByTag(args[1]).orElseThrow(() -> new IllegalStateException("war.not-found"));
        plugin.getClanManager().setDiplomacyAsync(source, target, relation, player.getUniqueId())
                .thenAccept(clan -> plugin.runSync(() -> plugin.getMessages().send(player, "diplomacy.updated", Map.of("tag", target.tag(), "relation", relation.name()))))
                .exceptionally(throwable -> {
                    plugin.runSync(() -> plugin.sendOperationError(player, throwable));
                    return null;
                });
    }

    private void ritual(Player player, String[] args) {
        requirePermission(player, Permissions.RITUAL);
        if (args.length < 2) {
            plugin.getMessages().send(player, "clan.help");
            return;
        }
        Optional<Clan> optionalClan = requireClan(player);
        if (optionalClan.isEmpty()) {
            plugin.getMessages().send(player, "clan.not-in-clan");
            return;
        }
        Clan clan = optionalClan.get();
        RitualType type;
        try {
            type = RitualType.valueOf(args[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("general.error", exception);
        }
        long remaining = plugin.getRitualManager().cooldownRemaining(clan.id());
        plugin.getRitualManager().startRitualAsync(clan, player.getUniqueId(), type)
                .thenAccept(ritual -> plugin.runSync(() -> plugin.getMessages().send(player, "ritual.started", Map.of("ritual", type.displayName()))))
                .exceptionally(throwable -> {
                    plugin.runSync(() -> {
                        if (remaining > 0L) {
                            plugin.getMessages().send(player, "ritual.cooldown", Map.of("time", formatDuration(remaining)));
                        } else {
                            plugin.sendOperationError(player, throwable);
                        }
                    });
                    return null;
                });
    }

    private void vote(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessages().send(player, "clan.help");
            return;
        }
        Optional<Clan> optionalClan = requireClan(player);
        if (optionalClan.isEmpty()) {
            plugin.getMessages().send(player, "clan.not-in-clan");
            return;
        }
        Clan clan = optionalClan.get();
        OfflinePlayer candidate = Bukkit.getOfflinePlayer(args[1]);
        plugin.getSuccessionManager().castVote(clan, player.getUniqueId(), candidate.getUniqueId());
        plugin.getMessages().send(player, "succession.voted");
    }

    private void artifact(CommandSender sender, String[] args) {
        requirePermission(sender, Permissions.ADMIN);
        if (!(sender instanceof Player player)) {
            plugin.getMessages().send(sender, "general.players-only");
            return;
        }
        if (args.length < 2) {
            plugin.getMessages().send(sender, "clan.help");
            return;
        }
        ArtifactType type;
        try {
            type = ArtifactType.valueOf(args[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("general.error", exception);
        }
        player.getInventory().addItem(plugin.getArtifactManager().createArtifact(type));
    }

    private void reload(CommandSender sender) {
        requirePermission(sender, Permissions.ADMIN);
        plugin.reloadConfig();
        plugin.getMessages().reload();
        plugin.getMessages().send(sender, "general.reloaded");
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        throw new IllegalStateException("general.players-only");
    }

    private Optional<Clan> requireClan(Player player) {
        return plugin.getClanManager().getPlayerClan(player.getUniqueId());
    }

    private void requirePermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            throw new IllegalStateException("general.no-permission");
        }
    }

    private String displayName(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }

    private String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(millis);
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        return hours + "h " + minutes + "m";
    }

    private List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(value);
            }
        }
        return result;
    }
}