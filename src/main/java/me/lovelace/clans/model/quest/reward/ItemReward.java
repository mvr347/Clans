package me.lovelace.clans.model.quest.reward;

import me.lovelace.clans.ClansPlugin;
import me.lovelace.clans.model.Clan;
import me.lovelace.clans.model.quest.QuestReward; // Added import
import me.lovelace.clans.util.ItemBuilder;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public record ItemReward(Material material, int amount, String displayName) implements QuestReward {

    @Override
    public void giveReward(ClansPlugin plugin, Player player, Clan clan) {
        if (player != null) {
            ItemStack item = new ItemStack(material, amount);
            if (displayName != null && !displayName.isEmpty()) {
                item = ItemBuilder.of(item).name(plugin.getMessages().component(displayName, player)).build();
            }
            player.getInventory().addItem(item);
            plugin.getMessages().send(player, "quest.reward.item",
                    Map.of("amount", String.valueOf(amount), "item", MiniMessage.miniMessage().serialize(item.displayName())));
        }
    }

    @Override
    public String getDisplayString() {
        return amount + "x " + (displayName != null && !displayName.isEmpty() ? displayName : material.name());
    }
}
