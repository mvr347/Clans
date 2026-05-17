package me.lovelace.clans.model.quest.objective;

import me.lovelace.clans.model.quest.QuestObjective;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public record PvPKillObjective(int targetAmount, String displayNameFormat) implements QuestObjective {

    @Override
    public String getType() {
        return "PVP_KILL";
    }

    @Override
    public Component getDisplayName(Player player, int currentProgress) {
        String formatted = displayNameFormat
                .replace("{current_progress}", String.valueOf(currentProgress))
                .replace("{target_amount}", String.valueOf(targetAmount));
        return MiniMessage.miniMessage().deserialize(formatted);
    }

    @Override
    public int getTargetAmount() {
        return targetAmount;
    }

    @Override
    public boolean isCompleted(int currentProgress) {
        return currentProgress >= targetAmount;
    }

    @Override
    public int updateProgress(UUID clanId, UUID playerId, int currentProgress, Map<String, Object> eventData) {
        // Assuming eventData contains "killer_id" (UUID) and "victim_is_player" (Boolean)
        if (eventData.containsKey("killer_id") && eventData.get("killer_id") instanceof UUID killerId &&
            eventData.containsKey("victim_is_player") && eventData.get("victim_is_player") instanceof Boolean victimIsPlayer) {
            if (victimIsPlayer && killerId.equals(playerId)) { // Ensure the killer is the player from the clan
                return 1; // Increment by 1 for each PvP kill
            }
        }
        return 0;
    }
}
