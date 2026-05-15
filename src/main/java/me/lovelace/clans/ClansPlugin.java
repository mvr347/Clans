package me.lovelace.clans;

import me.lovelace.clans.api.ClansAPI;
import me.lovelace.clans.command.ClanCommand;
import me.lovelace.clans.gui.ClanGuiManager;
import me.lovelace.clans.integration.AdvancedClaimsHook;
import me.lovelace.clans.integration.PlaceholderAPIHook;
import me.lovelace.clans.listener.ArtifactListener;
import me.lovelace.clans.listener.ClanProtectionListener;
import me.lovelace.clans.listener.CombatListener;
import me.lovelace.clans.listener.PlayerConnectionListener;
import me.lovelace.clans.manager.ArtifactManager;
import me.lovelace.clans.manager.ClanChestManager;
import me.lovelace.clans.manager.ClanManager;
import me.lovelace.clans.manager.RitualManager;
import me.lovelace.clans.manager.SpiritManager;
import me.lovelace.clans.manager.SuccessionManager;
import me.lovelace.clans.manager.WarManager;
import me.lovelace.clans.service.MessageService;
import me.lovelace.clans.storage.ClanStorage;
import me.lovelace.clans.storage.DatabaseManager;
import me.lovelace.clans.storage.SqlClanStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

public final class ClansPlugin extends JavaPlugin {
    private DatabaseManager databaseManager;
    private ClanStorage storage;
    private MessageService messages;
    private ClanManager clanManager;
    private WarManager warManager;
    private RitualManager ritualManager;
    private SuccessionManager successionManager;
    private SpiritManager spiritManager;
    private ClanChestManager clanChestManager;
    private ArtifactManager artifactManager;
    private ClanGuiManager guiManager;
    private AdvancedClaimsHook advancedClaimsHook;
    private BukkitTask heartbeatTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messages = new MessageService(this);
        messages.reload();

        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        storage = new SqlClanStorage(databaseManager);

        clanManager = new ClanManager(this, storage);
        warManager = new WarManager(this);
        ritualManager = new RitualManager(this);
        successionManager = new SuccessionManager(this);
        spiritManager = new SpiritManager(this);
        clanChestManager = new ClanChestManager(this);
        artifactManager = new ArtifactManager(this);
        guiManager = new ClanGuiManager(this);
        advancedClaimsHook = new AdvancedClaimsHook(this);
        advancedClaimsHook.initialize();

        clanManager.loadAsync().join();
        ClansAPI.init(this);

        registerCommands();
        registerListeners();
        registerIntegrations();

        spiritManager.start();
        successionManager.start();
        heartbeatTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            warManager.tick();
            ritualManager.tick();
        }, 20L * 60L, 20L * 60L);
    }

    @Override
    public void onDisable() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
        }
        if (spiritManager != null) {
            spiritManager.stop();
        }
        if (successionManager != null) {
            successionManager.stop();
        }
        ClansAPI.shutdown();
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    public MessageService getMessages() {
        return messages;
    }

    public ClanStorage getStorage() {
        return storage;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public WarManager getWarManager() {
        return warManager;
    }

    public RitualManager getRitualManager() {
        return ritualManager;
    }

    public SuccessionManager getSuccessionManager() {
        return successionManager;
    }

    public ClanChestManager getClanChestManager() {
        return clanChestManager;
    }

    public ArtifactManager getArtifactManager() {
        return artifactManager;
    }

    public ClanGuiManager getGuiManager() {
        return guiManager;
    }

    public AdvancedClaimsHook getAdvancedClaimsHook() {
        return advancedClaimsHook;
    }

    public CompletableFuture<Void> runSync(Runnable runnable) {
        return supplySync(() -> {
            runnable.run();
            return null;
        });
    }

    public <T> CompletableFuture<T> supplySync(Supplier<T> supplier) {
        if (Bukkit.isPrimaryThread()) {
            try {
                return CompletableFuture.completedFuture(supplier.get());
            } catch (Throwable throwable) {
                return CompletableFuture.failedFuture(throwable);
            }
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(this, () -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    public void sendOperationError(CommandSender sender, Throwable throwable) {
        Throwable root = unwrap(throwable);
        String key = root.getMessage();
        if (key != null && key.contains(".")) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("reason", key);
            placeholders.put("min", String.valueOf(getConfig().getInt("clans.tag.min-length", 2)));
            placeholders.put("max", String.valueOf(getConfig().getInt("clans.tag.max-length", 6)));
            messages.send(sender, key, placeholders);
        } else {
            messages.send(sender, "general.error", Map.of("reason", key == null ? root.getClass().getSimpleName() : key));
        }
    }

    private void registerCommands() {
        ClanCommand executor = new ClanCommand(this);
        PluginCommand command = Objects.requireNonNull(getCommand("clan"), "Command /clan is missing from plugin.yml");
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(guiManager, this);
        pluginManager.registerEvents(new PlayerConnectionListener(this), this);
        pluginManager.registerEvents(new ClanProtectionListener(this), this);
        pluginManager.registerEvents(new CombatListener(this), this);
        pluginManager.registerEvents(new ArtifactListener(this), this);
    }

    private void registerIntegrations() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIHook(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
