package me.lovelace.clans.manager;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.ClanMember;
import me.lovelace.clans.model.ClanRank;
import me.lovelace.clans.model.succession.SuccessionVote;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SuccessionManager {
    private final ClansPlugin plugin;
    private final Map<UUID, SuccessionVote> votes = new ConcurrentHashMap<>();
    private BukkitTask task;

    public SuccessionManager(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 60L, 20L * 60L * 30L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    public Optional<SuccessionVote> activeVote(UUID clanId) {
        return Optional.ofNullable(votes.get(clanId));
    }

    public void castVote(Clan clan, UUID voterId, UUID candidateId) {
        SuccessionVote vote = votes.get(clan.id());
        if (vote == null || !clan.hasMember(voterId) || !clan.hasMember(candidateId)) {
            throw new IllegalStateException("general.error");
        }
        vote.vote(voterId, candidateId);
        maybeFinishVote(clan, vote);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        for (Clan clan : plugin.getClanManager().getAllClans()) {
            if (votes.containsKey(clan.id())) {
                SuccessionVote vote = votes.get(clan.id());
                if (vote.expired(now)) {
                    finishVote(clan, vote);
                }
                continue;
            }
            clan.leaderId().ifPresent(leader -> {
                if (leaderAbsent(clan, leader, now)) {
                    startVote(clan, leader, now);
                }
            });
        }
    }

    private void startVote(Clan clan, UUID leaderId, long now) {
        long durationMillis = plugin.getConfig().getLong("mechanics.succession.vote-duration-hours", 48L) * 60L * 60L * 1000L;
        votes.put(clan.id(), new SuccessionVote(clan.id(), leaderId, now, now + durationMillis));
        clan.members().keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(java.util.Objects::nonNull)
                .forEach(player -> plugin.getMessages().send(player, "succession.vote-started"));
    }

    private boolean leaderAbsent(Clan clan, UUID leaderId, long now) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(leaderId);
        long lastSeen = offlinePlayer.getLastPlayed();
        ClanMember member = clan.member(leaderId).orElse(null);
        if (member != null) {
            lastSeen = Math.max(lastSeen, member.lastSeen());
        }
        long trigger = plugin.getConfig().getLong("mechanics.succession.offline-days-trigger", 14L) * 24L * 60L * 60L * 1000L;
        return lastSeen > 0L && now - lastSeen >= trigger;
    }

    private void maybeFinishVote(Clan clan, SuccessionVote vote) {
        int eligible = Math.max(1, clan.members().size() - 1);
        int quorumPercent = plugin.getConfig().getInt("mechanics.succession.quorum-percent", 50);
        int required = Math.max(1, (int) Math.ceil(eligible * quorumPercent / 100.0D));
        if (vote.votes().size() >= required) {
            finishVote(clan, vote);
        }
    }

    private void finishVote(Clan clan, SuccessionVote vote) {
        votes.remove(clan.id());
        Optional<UUID> winner = vote.winner();
        if (winner.isEmpty() || !clan.hasMember(winner.get())) {
            return;
        }
        clan.leaderId().ifPresent(old -> clan.setRank(old, ClanRank.GUARDIAN));
        clan.setRank(winner.get(), ClanRank.LEADER);
        plugin.getStorage().saveClanAsync(clan);
        OfflinePlayer promoted = Bukkit.getOfflinePlayer(winner.get());
        clan.members().keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(java.util.Objects::nonNull)
                .forEach(player -> plugin.getMessages().send(player, "succession.promoted", Map.of("player", promoted.getName() == null ? winner.get().toString() : promoted.getName())));
    }
}
