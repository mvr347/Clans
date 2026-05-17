package me.lovelace.clans.listener;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.war.ClanWar;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class CombatListener implements Listener {
    private final ClansPlugin plugin;

    public CombatListener(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = player(event.getDamager());
        Player victim = player(event.getEntity());
        if (attacker == null || victim == null) {
            return;
        }
        Optional<Clan> attackerClan = plugin.getClanManager().getPlayerClan(attacker.getUniqueId());
        Optional<Clan> victimClan = plugin.getClanManager().getPlayerClan(victim.getUniqueId());
        if (attackerClan.isEmpty() || victimClan.isEmpty()) {
            return;
        }
        if (attackerClan.get().id().equals(victimClan.get().id()) && !plugin.getConfig().getBoolean("clans.friendly-fire", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        // Handle banner drop on death
        Optional<Clan> victimClanOpt = plugin.getClanManager().getPlayerClan(victim.getUniqueId());
        if (victimClanOpt.isPresent()) {
            for (ClanWar war : plugin.getWarManager().activeWars()) {
                if (war.capturedBannerBy() != null && war.capturedBannerBy().equals(victim.getUniqueId())) {
                    plugin.getWarManager().resetBannerCapture(war.id());
                    // Remove banner from drops
                    event.getDrops().removeIf(item -> item != null && item.getType().name().endsWith("_BANNER"));
                    plugin.getMessages().send(victim, "war.banner-dropped");
                    break;
                }
            }
        }
        
        if (killer == null) {
            return;
        }
        Optional<Clan> killerClan = plugin.getClanManager().getPlayerClan(killer.getUniqueId());
        Optional<Clan> victimClan = plugin.getClanManager().getPlayerClan(victim.getUniqueId());
        if (killerClan.isEmpty() || victimClan.isEmpty()) {
            return;
        }
        plugin.getWarManager().addKillScore(killerClan.get().id(), victimClan.get().id());
    }

    private Player player(Entity entity) {
        return entity instanceof Player player ? player : null;
    }
}