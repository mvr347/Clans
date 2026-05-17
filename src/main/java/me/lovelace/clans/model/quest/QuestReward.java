package me.lovelace.clans.model.quest;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import org.bukkit.entity.Player;

public interface QuestReward {
    /**
     * Applies the reward to the given player and/or clan.
     * @param plugin The ClansPlugin instance.
     * @param player The player who is claiming the reward (can be null if reward is purely for clan).
     * @param clan The clan receiving the reward.
     */
    void giveReward(ClansPlugin plugin, Player player, Clan clan);

    /**
     * Gets a display string for the reward.
     * @return The display string.
     */
    String getDisplayString();
}
