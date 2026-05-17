package me.lovelace.clans.model.quest.objective;

import me.lovelace.clans.model.quest.QuestObjective;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public record CraftItemObjective(Material itemType, int targetAmount, String displayNameFormat) implements QuestObjective {

    @Override
    public String getType() {
        return "CRAFT_ITEM";
    }

    @Override
    public Component getDisplayName(Player player, int currentProgress) {
        String formatted = displayNameFormat
                .replace("{item_type}", itemType.name())
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
        if (eventData.containsKey("crafted_item_type") && eventData.get("crafted_item_type") instanceof Material craftedType &&
            eventData.containsKey("amount") && eventData.get("amount") instanceof Integer amountCrafted) {
            if (craftedType == itemType) {
                return amountCrafted;
            }
        }
        return 0;
    }
}
