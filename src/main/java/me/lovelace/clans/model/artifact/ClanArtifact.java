package me.lovelace.clans.model.artifact;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public sealed interface ClanArtifact permits WarHornArtifact, AegisBannerArtifact {
    ArtifactType type();

    Material material();

    Component displayName();
}
