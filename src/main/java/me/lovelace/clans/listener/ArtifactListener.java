package me.lovelace.clans.listener;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.artifact.ArtifactType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Optional;

public final class ArtifactListener implements Listener {
    private final ClansPlugin plugin;

    public ArtifactListener(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Optional<ArtifactType> artifact = plugin.getArtifactManager().readArtifact(event.getItem());
        if (artifact.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        Optional<Clan> clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        switch (artifact.get()) {
            case WAR_HORN -> {
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 1.0F, 0.8F);
                clan.get().members().keySet().stream()
                        .map(org.bukkit.Bukkit::getPlayer)
                        .filter(java.util.Objects::nonNull)
                        .filter(member -> member.getWorld().equals(player.getWorld()) && member.getLocation().distanceSquared(player.getLocation()) <= 40 * 40)
                        .forEach(member -> member.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 20, 0, true, true, true)));
            }
            case AEGIS_BANNER -> {
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 40, 1, 1, 1, 0.05);
                clan.get().members().keySet().stream()
                        .map(org.bukkit.Bukkit::getPlayer)
                        .filter(java.util.Objects::nonNull)
                        .filter(member -> member.getWorld().equals(player.getWorld()) && member.getLocation().distanceSquared(player.getLocation()) <= 25 * 25)
                        .forEach(member -> member.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 15, 1, true, true, true)));
            }
        }
    }
}
