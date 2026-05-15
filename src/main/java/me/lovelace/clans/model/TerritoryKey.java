package me.lovelace.clans.model;

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.Locale;
import java.util.Objects;

public record TerritoryKey(String world, int chunkX, int chunkZ) {

    public TerritoryKey {
        Objects.requireNonNull(world, "world");
        world = world.toLowerCase(Locale.ROOT);
    }

    public static TerritoryKey fromChunk(Chunk chunk) {
        return new TerritoryKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public static TerritoryKey fromLocation(Location location) {
        return new TerritoryKey(location.getWorld().getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public String serialize() {
        return world + ":" + chunkX + ":" + chunkZ;
    }

    public static TerritoryKey deserialize(String input) {
        String[] parts = input.split(":", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid territory key: " + input);
        }
        return new TerritoryKey(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}
