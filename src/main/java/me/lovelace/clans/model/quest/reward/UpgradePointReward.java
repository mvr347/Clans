package me.lovelace.clans.model.quest.reward;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.quest.QuestReward; // Added import
import org.bukkit.entity.Player;

import java.util.Map;

public record UpgradePointReward(int amount) implements QuestReward {

    @Override
    public void giveReward(ClansPlugin plugin, Player player, Clan clan) {
        clan.addUpgradePoints(amount); // Предполагается, что у Clan есть метод addUpgradePoints
        plugin.getClanManager().updateClanAsync(clan); // Сохраняем обновленный клан
        if (player != null) {
            plugin.getMessages().send(player, "quest.reward.upgrade_points", Map.of("amount", String.valueOf(amount)));
        }
    }

    @Override
    public String getDisplayString() {
        return amount + " очков улучшения";
    }
}
