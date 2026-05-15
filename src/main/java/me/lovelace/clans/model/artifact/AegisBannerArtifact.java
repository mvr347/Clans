package me.lovelace.clans.model.artifact;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;

public record AegisBannerArtifact() implements ClanArtifact {
    @Override
    public ArtifactType type() {
        return ArtifactType.AEGIS_BANNER;
    }

    @Override
    public Material material() {
        return Material.SHIELD;
    }

    @Override
    public Component displayName() {
        return MiniMessage.miniMessage().deserialize("<aqua>Артефакт войны: Знамя Эгиды");
    }
}
