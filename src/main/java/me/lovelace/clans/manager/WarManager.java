package me.lovelace.clans.manager;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.api.events.ClanWarEndEvent;
import me.lovelace.clans.api.events.ClanWarStartEvent;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.TerritoryKey;
import me.lovelace.clans.model.war.ClanWar;
import me.lovelace.clans.model.war.WarResult;
import me.lovelace.clans.model.war.WarState;
import org.bukkit.Bukkit;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class WarManager {
    private static final Duration DEFAULT_DURATION = Duration.ofMinutes(30);

    private final ClansPlugin plugin;
    private final Map<UUID, ClanWar> activeWars = new ConcurrentHashMap<>();

    public WarManager(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<ClanWar> startWarAsync(Clan attacker, Clan defender, TerritoryKey territory) {
        return plugin.supplySync(() -> {
            if (areAtWar(attacker.id(), defender.id())) {
                throw new IllegalStateException("war.already-at-war");
            }
            long now = System.currentTimeMillis();
            ClanWar war = new ClanWar(UUID.randomUUID(), attacker.id(), defender.id(), territory, now,
                    now + DEFAULT_DURATION.toMillis(), WarState.ACTIVE, 0, 0);
            ClanWarStartEvent event = new ClanWarStartEvent(war);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                throw new IllegalStateException("general.error");
            }
            activeWars.put(war.id(), war);
            return war;
        });
    }

    public CompletableFuture<Void> endWarAsync(UUID warId, WarResult result) {
        return plugin.supplySync(() -> {
            ClanWar war = activeWars.remove(warId);
            if (war != null) {
                Bukkit.getPluginManager().callEvent(new ClanWarEndEvent(war.withState(WarState.FINISHED), result));
                long reward = plugin.getConfig().getLong("leveling.war-win-exp", 1200L);
                if (result == WarResult.ATTACKER_WIN) {
                    plugin.getClanManager().getClanById(war.attackerClanId()).ifPresent(clan -> plugin.getClanManager().addExperienceAsync(clan, reward));
                } else if (result == WarResult.DEFENDER_WIN) {
                    plugin.getClanManager().getClanById(war.defenderClanId()).ifPresent(clan -> plugin.getClanManager().addExperienceAsync(clan, reward));
                }
            }
            return null;
        });
    }

    public boolean areAtWar(UUID firstClanId, UUID secondClanId) {
        return activeWars.values().stream().anyMatch(war -> war.state() == WarState.ACTIVE && war.between(firstClanId, secondClanId));
    }

    public Optional<ClanWar> activeWar(UUID firstClanId, UUID secondClanId) {
        return activeWars.values().stream()
                .filter(war -> war.state() == WarState.ACTIVE && war.between(firstClanId, secondClanId))
                .findFirst();
    }

    public Collection<ClanWar> activeWars() {
        return java.util.List.copyOf(activeWars.values());
    }

    public void addKillScore(UUID killerClanId, UUID victimClanId) {
        activeWar(killerClanId, victimClanId).ifPresent(war -> {
            ClanWar updated = killerClanId.equals(war.attackerClanId()) ? war.addAttackerScore(1) : war.addDefenderScore(1);
            activeWars.put(war.id(), updated);
        });
    }

    public void tick() {
        long now = System.currentTimeMillis();
        for (ClanWar war : activeWars.values()) {
            if (war.endsAt() <= now) {
                WarResult result = war.attackerScore() > war.defenderScore()
                        ? WarResult.ATTACKER_WIN
                        : war.defenderScore() > war.attackerScore() ? WarResult.DEFENDER_WIN : WarResult.DRAW;
                endWarAsync(war.id(), result);
            }
        }
    }
}
