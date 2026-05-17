package me.lovelace.clans.model.quest.objective;

import me.lovelace.clans.model.quest.QuestObjective;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public record KillMobObjective(EntityType mobType, int targetAmount, String displayNameFormat) implements QuestObjective {

    @Override
    public String getType() {
        return "KILL_MOB";
    }

    @Override
    public Component getDisplayName(Player player, int currentProgress) {
        String formatted = displayNameFormat
                .replace("{mob_type}", mobType.name())
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
        if (eventData.containsKey("entity_type") && eventData.get("entity_type") instanceof EntityType killedType) {
            if (killedType == mobType) {
                return 1; // Increment by 1 for each mob killed
            }
        }
        return 0;
    }
}
