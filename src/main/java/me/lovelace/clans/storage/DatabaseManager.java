package me.lovelace.clans.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.lovelace.clans.ClansPlugin;
import org.bukkit.configuration.ConfigurationSection;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class DatabaseManager implements AutoCloseable {
    private final ClansPlugin plugin;
    private HikariDataSource dataSource;
    private ExecutorService executor;
    private DatabaseType type;

    public DatabaseManager(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        type = DatabaseType.parse(plugin.getConfig().getString("database.type", "SQLITE"));
        int poolSize = Math.max(1, plugin.getConfig().getInt("database.pool-size", type == DatabaseType.SQLITE ? 1 : 4));

        HikariConfig config = new HikariConfig();
        config.setPoolName("Clans-" + type.name().toLowerCase(Locale.ROOT));
        config.setMaximumPoolSize(type == DatabaseType.SQLITE ? 1 : poolSize);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000L);
        config.setValidationTimeout(5_000L);

        if (type == DatabaseType.MYSQL) {
            configureMySql(config);
        } else {
            configureSqlite(config);
        }

        dataSource = new HikariDataSource(config);
        executor = Executors.newFixedThreadPool(type == DatabaseType.SQLITE ? 1 : poolSize, new ClanThreadFactory());
        createSchema();
    }

    public DataSource dataSource() {
        return Objects.requireNonNull(dataSource, "DatabaseManager is not initialized");
    }

    public ExecutorService executor() {
        return Objects.requireNonNull(executor, "DatabaseManager is not initialized");
    }

    public DatabaseType type() {
        return type;
    }

    private void configureSqlite(HikariConfig config) {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Unable to create plugin data folder");
        }
        File databaseFile = new File(plugin.getDataFolder(), "clans.db");
        config.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setConnectionInitSql("PRAGMA foreign_keys = ON");
    }

    private void configureMySql(HikariConfig config) {
        ConfigurationSection mysql = plugin.getConfig().getConfigurationSection("database.mysql");
        String host = mysql == null ? "localhost" : mysql.getString("host", "localhost");
        int port = mysql == null ? 3306 : mysql.getInt("port", 3306);
        String database = mysql == null ? "minecraft" : mysql.getString("database", "minecraft");
        boolean useSsl = mysql != null && mysql.getBoolean("use-ssl", false);
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + useSsl
                + "&allowPublicKeyRetrieval=true"
                + "&useUnicode=true"
                + "&characterEncoding=utf8");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setUsername(mysql == null ? "root" : mysql.getString("username", "root"));
        config.setPassword(mysql == null ? "" : mysql.getString("password", ""));

        ConfigurationSection properties = mysql == null ? null : mysql.getConfigurationSection("properties");
        if (properties != null) {
            for (String key : properties.getKeys(false)) {
                config.addDataSourceProperty(key, properties.get(key));
            }
        }
    }

    private void createSchema() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS clans (
                        id VARCHAR(36) PRIMARY KEY,
                        name VARCHAR(32) NOT NULL,
                        tag VARCHAR(16) NOT NULL UNIQUE,
                        tag_color VARCHAR(64) NOT NULL,
                        description TEXT NOT NULL,
                        emblem_material VARCHAR(64) NOT NULL,
                        level INT NOT NULL,
                        experience BIGINT NOT NULL,
                        chest_rows INT NOT NULL,
                        spirit_level INT NOT NULL,
                        spirit_energy BIGINT NOT NULL,
                        spirit_awakened_until BIGINT NOT NULL,
                        created_at BIGINT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS clan_members (
                        clan_id VARCHAR(36) NOT NULL,
                        player_id VARCHAR(36) NOT NULL,
                        rank VARCHAR(32) NOT NULL,
                        joined_at BIGINT NOT NULL,
                        last_seen BIGINT NOT NULL,
                        PRIMARY KEY (clan_id, player_id),
                        FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS clan_territories (
                        world VARCHAR(128) NOT NULL,
                        chunk_x INT NOT NULL,
                        chunk_z INT NOT NULL,
                        clan_id VARCHAR(36) NOT NULL,
                        advanced_claim_id VARCHAR(36),
                        claimed_by VARCHAR(36) NOT NULL,
                        claimed_at BIGINT NOT NULL,
                        PRIMARY KEY (world, chunk_x, chunk_z),
                        FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS clan_diplomacy (
                        source_clan_id VARCHAR(36) NOT NULL,
                        target_clan_id VARCHAR(36) NOT NULL,
                        relation VARCHAR(32) NOT NULL,
                        updated_at BIGINT NOT NULL,
                        PRIMARY KEY (source_clan_id, target_clan_id),
                        FOREIGN KEY (source_clan_id) REFERENCES clans(id) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS clan_upgrades (
                        clan_id VARCHAR(36) NOT NULL,
                        upgrade_name VARCHAR(64) NOT NULL,
                        upgrade_level INT NOT NULL,
                        PRIMARY KEY (clan_id, upgrade_name),
                        FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS clan_chests (
                        clan_id VARCHAR(36) PRIMARY KEY,
                        content BLOB NOT NULL,
                        updated_at BIGINT NOT NULL,
                        FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE
                    )
                    """);
        } catch (SQLException exception) {
            throw new StorageException("Unable to create database schema", exception);
        }
    }

    @Override
    public void close() {
        if (executor != null) {
            executor.shutdown();
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private static final class ClanThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "Clans-Storage-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
