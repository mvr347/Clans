package me.lovelace.clans.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ItemBuilder {
    private final ItemStack itemStack;
    private final List<Component> lore = new ArrayList<>();

    private ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    public ItemBuilder name(Component name) {
        mutate(meta -> meta.displayName(name));
        return this;
    }

    public ItemBuilder lore(Component line) {
        lore.add(line);
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        mutate(meta -> meta.addItemFlags(flags));
        return this;
    }

    public ItemBuilder amount(int amount) {
        itemStack.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    public ItemBuilder mutate(Consumer<ItemMeta> consumer) {
        ItemMeta meta = itemStack.getItemMeta();
        consumer.accept(meta);
        itemStack.setItemMeta(meta);
        return this;
    }

    public ItemStack build() {
        if (!lore.isEmpty()) {
            mutate(meta -> meta.lore(lore));
        }
        return itemStack;
    }
}
