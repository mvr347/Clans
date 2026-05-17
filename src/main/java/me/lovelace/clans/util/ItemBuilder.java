package me.lovelace.clans.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class ItemBuilder {
    // Custom Head Textures
    public static final String HEAD_BACK = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODY1MmUyYjkzNmNhODAyNmJkMjg2NTFkN2M5ZjI4MTlkMmU5MjM2OTc3MzRkMThkZmRiMTM1NTBmOGZkYWQ1ZiJ9fX0=";
    public static final String HEAD_BARRIER = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkMWFiYTczZjYzOWY0YmM0MmJkNDgxOTZjNzE1MTk3YmUyNzEyYzNiOTYyYzk3ZWJmOWU5ZWQ4ZWZhMDI1In19fQ==";
    public static final String HEAD_SETTINGS = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGViMmQ3NTRhYmJjNWQwMDRiODBmZTA4YjUzMTg2ZjY5ZGM4ZTllZjZmOGY3ZmQwNzIxNGE4Yzg0OTZlYjA4NSJ9fX0=";
    public static final String HEAD_MEMBERS = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjFhZTgwMzg0YTAwYjZmOWY2NGRkODMwN2E5MDY3NjU0NGM5N2E3OTI5NzE2NWVhNzEzMjYyYzdkODgzMzg0NyJ9fX0=";
    public static final String HEAD_INFO = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjgwZDMyOTVkM2Q5YWJkNjI3NzZhYmNiOGRhNzU2ZjI5OGE1NDVmZWU5NDk4YzRmNjlhMWMyYzc4NTI0YzgyNCJ9fX0=";
    public static final String HEAD_DESC = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmRkMmViMGM2ZjhhOTU0M2VmNWZkNzI1MjVjYzJmYWIzNTY2M2NkNzA5MTM1ZTQzYjhlMjU3ZGMwYjc1ODk0OCJ9fX0=";
    public static final String HEAD_EXPAND = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzA0MGZlODM2YTZjMmZiZDJjN2E5YzhlYzZiZTUxNzRmZGRmMWFjMjBmNTVlMzY2MTU2ZmE1ZjcxMmUxMCJ9fX0=";
    public static final String HEAD_MSG = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDc1N2U5NzJjNzYzMTQ5NDc2YmM1NTZjZmJhZDM2MTRjMWQ0OTk1NGYxM2Y4ODkzZWI5ZDEwNTg0NTc1M2YyZSJ9fX0=";
    public static final String HEAD_QUEST = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTZkN2I3MjA0YmVhMWZmMGRmNzkzMGJkNDU1OWEzMGI1ZWE1NTllNTYyNDI5NDY5NGU4NjdiZjdiNWFlMDM2MSJ9fX0=";
    public static final String HEAD_INVITE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkZDIwYmU5MzUyMDk0OWU2Y2U3ODlkYzRmNDNlZmFlYjI4YzcxN2VlNmJmY2JiZTAyNzgwMTQyZjcxNiJ9fX0=";
    public static final String HEAD_DELETE_YES = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTMwZjQ1MzdkMjE0ZDM4NjY2ZTYzMDRlOWM4NTFjZDZmN2U0MWEwZWI3YzI1MDQ5YzlkMjJjOGM1ZjY1NDVkZiJ9fX0=";
    public static final String HEAD_DELETE_NO = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWE2Nzg3YmEzMjU2NGU3YzJmM2EwY2U2NDQ5OGVjYmIyM2I4OTg0NWU1YTY2YjVjZWM3NzM2ZjcyOWVkMzcifX19";
    public static final String HEAD_DISBAND = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGEzNWU3NjQ5MmQ0MmQ3MmNmNTZkZmM0YjdkNTM0N2Q0OGNjODg5NDBkZmQ2YTlkNjIzNWJjNzE4ZjRjNmJhYiJ9fX0=";
    public static final String HEAD_TERRITORIES = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTUwMTU0NzBlMjg2ZTRlZDc3YTAzODc2Y2JiZmQ3YjNkMzU4YTYwNjA2YjQ0NmQyYzRiYzhkOGU5YzM3M2VlOSJ9fX0=";
    public static final String HEAD_PRIVATES = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjNkMDJjZGMwNzViYjFjYzVmNmZlM2M3NzExYWU0OTc3ZTM4YjkxMGQ1MGVkNjAyM2RmNzM5MTNlNWU3ZmNmZiJ9fX0=";
    public static final String HEAD_EXPERIENCE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDhiODFkODkzNWZjMTAxZTM1ZjNlNTRmNTM3YjYxMjgyMmYxOGZhZjc1MmE5MDAzN2NjNDIxNWE1ZGM4ODFlZiJ9fX0=";
    public static final String HEAD_DAILY_QUESTS = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2MxYjJmNTkyY2ZjOGQzNzJkY2Y1ZmQ0NGVlZDY5ZGRkYzY0NjAxZDc4NDZkNzI2MTlmNzA1MTFkODA0M2E4OSJ9fX0=";
    public static final String HEAD_WEEKLY_QUESTS = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjJjZGEwNWVhNWQ5Y2NhNzAyMDdhOGIyOWZkZTFjOTY5NmM3NTI3YzYyZTU5OWUzZGEyZDI3NWYyMGUwYmE5YiJ9fX0=";
    public static final String HEAD_MONTHLY_QUESTS = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmY1NjhkZDgwODIwNjU3NTU0ZjliNDVkYjAzMWQyMmNkOWI3ZDE2MWMxOTgyNzczYjFiMzU4NTRjOTkzOTgyNCJ9fX0=";
    public static final String HEAD_COMPLETED_QUESTS = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjdkYTA3ZmMwZTRhZTEwMWNkM2Q2YjYwODBiMDRhYTc1MzgxZWY4MDc0ZTAyZWQ1Mzk3NTk0ZjQ5YjcxY2IwYyJ9fX0=";
    public static final String HEAD_EXP = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTUwMTU0NzBlMjg2ZTRlZDc3YTAzODc2Y2JiZmQ3YjNkMzU4YTYwNjA2YjQ0NmQyYzRiYzhkOGU5YzM3M2VlOSJ9fX0=";
    public static final String HEAD_MAP = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzk5MjRiY2ZkN2VjNmVhNDM3NDM4Yjc1YjcyZWU0MWEyYjU3MTA0ZGJhMjVlY2FmYTYxMThlNjhlMWI4MWM5ZCJ9fX0=";

    private final ItemStack itemStack;
    private final List<Component> lore = new ArrayList<>();

    private ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }
    
    public static ItemBuilder of(ItemStack itemStack) {
        ItemBuilder builder = new ItemBuilder(itemStack.getType());
        builder.itemStack.setItemMeta(itemStack.getItemMeta());
        builder.itemStack.setAmount(itemStack.getAmount());
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasLore()) {
            builder.lore.addAll(itemStack.getItemMeta().lore());
        }
        return builder;
    }

    public static ItemBuilder head(String base64) {
        ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD);
        builder.mutate(meta -> {
            if (meta instanceof SkullMeta skullMeta) {
                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                profile.setProperty(new ProfileProperty("textures", base64));
                skullMeta.setPlayerProfile(profile);
            }
        });
        return builder;
    }

    public ItemBuilder name(Component name) {
        mutate(meta -> meta.displayName(name));
        return this;
    }

    public ItemBuilder lore(Component line) {
        lore.add(line);
        return this;
    }

    public ItemBuilder lore(Collection<Component> lines) {
        lore.addAll(lines);
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
    
    public ItemBuilder glow(boolean glow) {
        if (glow) {
            mutate(meta -> {
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            });
        }
        return this;
    }

    public ItemBuilder mutate(Consumer<ItemMeta> consumer) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            consumer.accept(meta);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemStack build() {
        if (!lore.isEmpty()) {
            mutate(meta -> meta.lore(lore));
        }
        return itemStack;
    }
}
