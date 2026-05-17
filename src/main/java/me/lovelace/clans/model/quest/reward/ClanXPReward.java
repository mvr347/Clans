package me.lovelace.clans.model.quest.reward;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.quest.QuestReward; // Added import
import org.bukkit.entity.Player;

import java.util.Map;

public record ClanXPReward(long amount) implements QuestReward { // Изменено на long

    @Override
    public void giveReward(ClansPlugin plugin, Player player, Clan clan) {
        plugin.getClanManager().addExperienceAsync(clan, amount); // Используем метод addExperienceAsync
        if (player != null) {
            plugin.getMessages().send(player, "quest.reward.clan_xp", Map.of("amount", String.valueOf(amount)));
        }
    }

    @Override
    public String getDisplayString() {
        return amount + " опыта клана";
    }
}
