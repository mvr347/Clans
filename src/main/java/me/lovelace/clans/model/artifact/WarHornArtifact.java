package me.lovelace.clans.model.artifact;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;

public record WarHornArtifact() implements ClanArtifact {
    @Override
    public ArtifactType type() {
        return ArtifactType.WAR_HORN;
    }

    @Override
    public Material material() {
        return Material.GOAT_HORN;
    }

    @Override
    public Component displayName() {
        return MiniMessage.miniMessage().deserialize("<red>Артефакт войны: Боевой рог");
    }
}
