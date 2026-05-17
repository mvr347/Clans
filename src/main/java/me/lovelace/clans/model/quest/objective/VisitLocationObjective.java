package me.lovelace.clans.model.quest.objective;

import me.lovelace.clans.model.quest.QuestObjective;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public record VisitLocationObjective(String worldName, double x, double y, double z, double radius, String displayNameFormat) implements QuestObjective {

    @Override
    public String getType() {
        return "VISIT_LOCATION";
    }

    @Override
    public Component getDisplayName(Player player, int currentProgress) {
        String formatted = displayNameFormat
                .replace("{world}", worldName)
                .replace("{x}", String.valueOf((int) x))
                .replace("{y}", String.valueOf((int) y))
                .replace("{z}", String.valueOf((int) z))
                .replace("{radius}", String.valueOf((int) radius))
                .replace("{current_progress}", String.valueOf(currentProgress))
                .replace("{target_amount}", String.valueOf(getTargetAmount()));
        return MiniMessage.miniMessage().deserialize(formatted);
    }

    @Override
    public int getTargetAmount() {
        return 1; // Usually, you visit a location once
    }

    @Override
    public boolean isCompleted(int currentProgress) {
        return currentProgress >= getTargetAmount();
    }

    @Override
    public int updateProgress(UUID clanId, UUID playerId, int currentProgress, Map<String, Object> eventData) {
        if (currentProgress >= getTargetAmount()) {
            return 0; // Already completed
        }

        if (eventData.containsKey("player_location") && eventData.get("player_location") instanceof Location playerLocation) {
            if (playerLocation.getWorld() != null && playerLocation.getWorld().getName().equals(worldName)) {
                Location targetLocation = new Location(playerLocation.getWorld(), x, y, z);
                if (playerLocation.distance(targetLocation) <= radius) {
                    return 1; // Player is within the target radius
                }
            }
        }
        return 0;
    }
}
