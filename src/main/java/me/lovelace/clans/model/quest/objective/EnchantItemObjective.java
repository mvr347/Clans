package me.lovelace.clans.model.quest.objective;

import me.lovelace.clans.model.quest.QuestObjective;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public record EnchantItemObjective(Enchantment enchantmentType, int targetAmount, String displayNameFormat) implements QuestObjective {

    @Override
    public String getType() {
        return "ENCHANT_ITEM";
    }

    @Override
    public Component getDisplayName(Player player, int currentProgress) {
        String formatted = displayNameFormat
                .replace("{enchantment_type}", enchantmentType.getKey().getKey()) // Use getKey().getKey() for enchantment name
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
        if (eventData.containsKey("enchantment_type") && eventData.get("enchantment_type") instanceof Enchantment enchantedType) {
            if (enchantedType.equals(enchantmentType)) { // Check if the enchanted type matches
                return 1; // Increment by 1 for each item enchanted with the specific enchantment
            }
        }
        return 0;
    }
}
