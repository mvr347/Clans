package me.lovelace.clans.storage;

public enum DatabaseType {
    SQLITE,
    MYSQL;

    public static DatabaseType parse(String value) {
        try {
            return DatabaseType.valueOf(value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return SQLITE;
        }
    }
}
