package me.lovelace.clans.model.quest.objective;

import me.lovelace.clans.model.quest.QuestObjective;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public record MineBlockObjective(Material blockType, int targetAmount, String displayNameFormat) implements QuestObjective {

    @Override
    public String getType() {
        return "MINE_BLOCK";
    }

    @Override
    public Component getDisplayName(Player player, int currentProgress) {
        String formatted = displayNameFormat
                .replace("{block_type}", blockType.name())
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
        if (eventData.containsKey("block_type") && eventData.get("block_type") instanceof Material minedType) {
            if (minedType == blockType) {
                return 1; // Increment by 1 for each block mined
            }
        }
        return 0;
    }
}
