package me.lovelace.clans.model.quest;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public interface QuestObjective {
    /**
     * Returns a unique identifier for this objective type.
     * @return The objective type ID.
     */
    String getType();

    /**
     * Gets the display name of the objective, potentially including progress.
     * @param player The player for whom to get the display name (for PAPI, etc.).
     * @param currentProgress The current progress for this objective.
     * @return The display name component.
     */
    Component getDisplayName(Player player, int currentProgress);

    /**
     * Gets the target amount required for this objective.
     * @return The target amount.
     */
    int getTargetAmount();

    /**
     * Checks if the objective is completed given the current progress.
     * @param currentProgress The current progress.
     * @return True if completed, false otherwise.
     */
    boolean isCompleted(int currentProgress);

    /**
     * Updates the progress for this objective based on a player's action.
     * This method should be called by event listeners.
     * @param clanId The ID of the clan whose progress is being updated.
     * @param playerId The ID of the player who performed the action.
     * @param currentProgress The current progress of the objective for the clan.
     * @param eventData A map containing relevant data from the event (e.g., mob type, item type, amount).
     * @return The amount by which the progress should increase, or 0 if no increase.
     */
    int updateProgress(UUID clanId, UUID playerId, int currentProgress, Map<String, Object> eventData);
}
