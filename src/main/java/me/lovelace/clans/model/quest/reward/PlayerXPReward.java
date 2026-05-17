package me.lovelace.clans.model.quest.reward;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.quest.QuestReward; // Added import
import org.bukkit.entity.Player;

import java.util.Map;

public record PlayerXPReward(int amount) implements QuestReward {

    @Override
    public void giveReward(ClansPlugin plugin, Player player, Clan clan) {
        if (player != null) {
            player.giveExp(amount);
            plugin.getMessages().send(player, "quest.reward.player_xp", Map.of("amount", String.valueOf(amount)));
        }
    }

    @Override
    public String getDisplayString() {
        return amount + " опыта игрока";
    }
}
