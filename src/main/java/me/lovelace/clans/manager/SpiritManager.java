package me.lovelace.clans.manager;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class SpiritManager {
    private final ClansPlugin plugin;
    private BukkitTask task;

    public SpiritManager(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("mechanics.spirit.enabled", true)) {
            return;
        }
        long period = Math.max(20L, plugin.getConfig().getLong("mechanics.spirit.tick-seconds", 15L) * 20L);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, period, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    public void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Optional<Clan> playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (playerClan.isEmpty()) {
                continue;
            }
            Optional<Clan> territoryClan = plugin.getClanManager().getClanAt(player.getLocation());
            if (territoryClan.isEmpty() || !territoryClan.get().id().equals(playerClan.get().id())) {
                continue;
            }
            applyBuffs(player, playerClan.get());
        }
    }

    private void applyBuffs(Player player, Clan clan) {
        int levelKey = clan.level() >= 20 ? 20 : clan.level() >= 15 ? 15 : clan.level() >= 10 ? 10 : clan.level() >= 5 ? 5 : 1;
        List<String> buffs = plugin.getConfig().getStringList("mechanics.spirit.buffs." + levelKey);
        int duration = (int) Math.max(60L, plugin.getConfig().getLong("mechanics.spirit.tick-seconds", 15L) * 40L);
        for (String buff : buffs) {
            String[] parts = buff.split(":", 2);
            PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase(Locale.ROOT));
            if (type == null) {
                continue;
            }
            int amplifier = parts.length == 2 ? Integer.parseInt(parts[1]) : 0;
            player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, true));
        }
    }
}
