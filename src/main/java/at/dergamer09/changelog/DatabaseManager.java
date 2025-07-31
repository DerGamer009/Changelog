package at.dergamer09.changelog;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.*;

public class DatabaseManager {
    private final ChangelogPlugin plugin;
    private Connection connection;

    public DatabaseManager(ChangelogPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        FileConfiguration config = plugin.getConfig();
        String type = config.getString("database.type", "sqlite");
        try {
            if (type.equalsIgnoreCase("mysql")) {
                String host = config.getString("database.host", "localhost");
                int port = config.getInt("database.port", 3306);
                String db = config.getString("database.name", "changelog");
                String user = config.getString("database.user", "root");
                String pass = config.getString("database.password", "");
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(
                        "jdbc:mysql://" + host + ":" + port + "/" + db,
                        user,
                        pass);
            } else {
                Class.forName("org.sqlite.JDBC");
                File file = new File(plugin.getDataFolder(), "changelog.db");
                connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            }
            createTable();
        } catch (Exception e) {
            plugin.getLogger().severe("Could not connect to database: " + e.getMessage());
        }
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS changelog_entries (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "message TEXT NOT NULL" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    public void addEntry(String message) {
        String sql = "INSERT INTO changelog_entries(message) VALUES(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add entry: " + e.getMessage());
        }
    }

    public void removeEntry(int id) {
        String sql = "DELETE FROM changelog_entries WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to remove entry: " + e.getMessage());
        }
    }

    public ResultSet getEntries() throws SQLException {
        String sql = "SELECT * FROM changelog_entries ORDER BY id DESC";
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(sql);
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            // ignore
        }
    }
}
