package io.github.pengzixuan30.gamesai.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.pengzixuan30.gamesai.GamesAI;

public class GamesAIDatabase {

    private static final String TABLE_NAME = "public_data";
    private final String dbPath;

    public GamesAIDatabase(String dbPath) {
        this.dbPath = dbPath;
    }

    public GamesAIDatabase() {
        this(getDatabasePath().toString());
    }

    private static final String CONFIG_DIR_NAME = "games_ai";
    private static final String DATABASE_DIR_NAME = "database";
    private static final String DATABASE_FILE_NAME = "database.db";

    public static java.nio.file.Path getDatabasePath() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
                .resolve(CONFIG_DIR_NAME)
                .resolve(DATABASE_DIR_NAME)
                .resolve(DATABASE_FILE_NAME);
    }

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            GamesAI.LOGGER.error("[GamesAIDatabase] SQLite driver not found", e);
        }
    }

    private Connection connect() throws SQLException {
        java.nio.file.Path path = java.nio.file.Path.of(dbPath);
        java.nio.file.Path parent = path.getParent();
        if (parent != null && !java.nio.file.Files.exists(parent)) {
            try {
                java.nio.file.Files.createDirectories(parent);
            } catch (java.io.IOException e) {
                GamesAI.LOGGER.error("[GamesAIDatabase] Failed to create directory: {}", parent, e);
            }
        }

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "key TEXT NOT NULL UNIQUE, "
                    + "value TEXT NOT NULL"
                    + ")");
        }
        return conn;
    }

    public boolean writeData(String key, String value) {
        String sql = "INSERT OR REPLACE INTO " + TABLE_NAME + " (key, value) VALUES (?, ?)";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
            GamesAI.LOGGER.debug("[GamesAIDatabase] write: {} = {}", key, value);
            return true;
        } catch (SQLException e) {
            GamesAI.LOGGER.error("[GamesAIDatabase] write failed: {} = {}", key, value, e);
            return false;
        }
    }

    public boolean appendData(String key, String value) {
        String old = readData(key);
        String next = (old == null) ? value : old + value;
        return writeData(key, next);
    }

    public boolean deleteData(String key) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE key = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.executeUpdate();
            GamesAI.LOGGER.debug("[GamesAIDatabase] delete: {}", key);
            return true;
        } catch (SQLException e) {
            GamesAI.LOGGER.error("[GamesAIDatabase] delete failed: {}", key, e);
            return false;
        }
    }

    public String readData(String key) {
        String sql = "SELECT value FROM " + TABLE_NAME + " WHERE key = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("value");
            }
        } catch (SQLException e) {
            GamesAI.LOGGER.error("[GamesAIDatabase] read failed: {}", key, e);
        }
        return null;
    }

    public Map<String, String> dataList() {
        Map<String, String> result = new LinkedHashMap<>();
        String sql = "SELECT key, value FROM " + TABLE_NAME + " ORDER BY id";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.put(rs.getString("key"), rs.getString("value"));
        } catch (SQLException e) {
            GamesAI.LOGGER.error("[GamesAIDatabase] dataList failed", e);
        }
        return result;
    }

    public List<String> getAllKeys() {
        List<String> keys = new ArrayList<>();
        String sql = "SELECT key FROM " + TABLE_NAME + " ORDER BY id";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) keys.add(rs.getString("key"));
        } catch (SQLException e) {
            GamesAI.LOGGER.error("[GamesAIDatabase] getAllKeys failed", e);
        }
        return keys;
    }
}
