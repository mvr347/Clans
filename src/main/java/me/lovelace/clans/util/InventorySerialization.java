package me.lovelace.clans.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class InventorySerialization {
    private InventorySerialization() {
    }

    public static byte[] serialize(Inventory inventory) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream output = new BukkitObjectOutputStream(bytes)) {
            ItemStack[] contents = inventory.getContents();
            output.writeInt(contents.length);
            for (ItemStack item : contents) {
                output.writeObject(item);
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to serialize inventory", exception);
        }
    }

    public static ItemStack[] deserialize(byte[] data, int expectedSize) {
        if (data == null || data.length == 0) {
            return new ItemStack[expectedSize];
        }
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(data);
             BukkitObjectInputStream input = new BukkitObjectInputStream(bytes)) {
            int size = input.readInt();
            ItemStack[] contents = new ItemStack[expectedSize];
            for (int index = 0; index < size; index++) {
                ItemStack item = (ItemStack) input.readObject();
                if (index < expectedSize) {
                    contents[index] = item;
                }
            }
            return contents;
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("Unable to deserialize inventory", exception);
        }
    }
}
