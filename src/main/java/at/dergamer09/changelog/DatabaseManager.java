package at.dergamer09.changelog;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DatabaseManager {
    private final ChangelogPlugin plugin;
    private Connection connection;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private String connectionString;
    private String username;
    private String password;
    private boolean isMySQL;

    public DatabaseManager(ChangelogPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        FileConfiguration config = plugin.getConfig();
        String type = config.getString("database.type", "sqlite");
        
        try {
            if (type.equalsIgnoreCase("mysql")) {
                setupMySQL(config);
            } else {
                setupSQLite();
            }
            
            // Test connection and create table
            ensureConnection();
            createTable();
            
            plugin.getLogger().info("Database connection established successfully (" + type.toUpperCase() + ")");
        } catch (Exception e) {
            plugin.getLogger().severe("Could not connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupMySQL(FileConfiguration config) throws ClassNotFoundException {
        String host = config.getString("database.mysql.host", config.getString("database.host", "localhost"));
        int port = config.getInt("database.mysql.port", config.getInt("database.port", 3306));
        String db = config.getString("database.mysql.database", config.getString("database.name", "changelog"));
        username = config.getString("database.mysql.username", config.getString("database.user", "root"));
        password = config.getString("database.mysql.password", config.getString("database.password", ""));
        
        // Validate MySQL configuration
        if (host.isEmpty() || db.isEmpty()) {
            throw new IllegalArgumentException("MySQL host and database name cannot be empty");
        }
        
        Class.forName("com.mysql.cj.jdbc.Driver");
        connectionString = "jdbc:mysql://" + host + ":" + port + "/" + db + 
                          "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        isMySQL = true;
        
        connection = DriverManager.getConnection(connectionString, username, password);
    }

    private void setupSQLite() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        
        // Ensure plugin data folder exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        File file = new File(plugin.getDataFolder(), "changelog.db");
        connectionString = "jdbc:sqlite:" + file.getAbsolutePath();
        isMySQL = false;
        
        connection = DriverManager.getConnection(connectionString);
    }

    private void ensureConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            reconnect();
        }
    }

    private void reconnect() throws SQLException {
        plugin.getLogger().info("Reconnecting to database...");
        
        if (isMySQL) {
            connection = DriverManager.getConnection(connectionString, username, password);
        } else {
            connection = DriverManager.getConnection(connectionString);
        }
    }

    private void createTable() throws SQLException {
        lock.writeLock().lock();
        try {
            ensureConnection();
            
            String sql;
            if (isMySQL) {
                sql = "CREATE TABLE IF NOT EXISTS changelog_entries (" +
                      "id INT AUTO_INCREMENT PRIMARY KEY," +
                      "date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                      "message TEXT NOT NULL" +
                      ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            } else {
                sql = "CREATE TABLE IF NOT EXISTS changelog_entries (" +
                      "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                      "date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                      "message TEXT NOT NULL" +
                      ")";
            }
            
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sql);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addEntry(String message) {
        lock.writeLock().lock();
        try {
            ensureConnection();
            
            String sql = "INSERT INTO changelog_entries(message) VALUES(?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, message);
                int affected = ps.executeUpdate();
                
                if (affected == 0) {
                    throw new SQLException("No rows were affected when adding entry");
                }
                
                plugin.getLogger().info("Successfully added changelog entry: " + message.substring(0, Math.min(50, message.length())) + "...");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add entry: " + e.getMessage());
            throw new RuntimeException("Database error while adding entry", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeEntry(int id) {
        lock.writeLock().lock();
        try {
            ensureConnection();
            
            String sql = "DELETE FROM changelog_entries WHERE id=?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, id);
                int affected = ps.executeUpdate();
                
                if (affected == 0) {
                    plugin.getLogger().warning("No entry found with ID " + id + " to remove");
                } else {
                    plugin.getLogger().info("Successfully removed changelog entry with ID " + id);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to remove entry: " + e.getMessage());
            throw new RuntimeException("Database error while removing entry", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ResultSet getEntries() throws SQLException {
        lock.readLock().lock();
        try {
            ensureConnection();
            
            String sql = "SELECT * FROM changelog_entries ORDER BY id DESC";
            Statement stmt = connection.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            lock.readLock().unlock();
            throw e;
        }
        // Note: readLock is NOT unlocked here because ResultSet is still being used
        // The caller MUST close the ResultSet and its Statement to release the lock
    }

    public int getEntryCount() throws SQLException {
        lock.readLock().lock();
        try {
            ensureConnection();
            
            String sql = "SELECT COUNT(*) as count FROM changelog_entries";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                if (rs.next()) {
                    return rs.getInt("count");
                }
                return 0;
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean entryExists(int id) throws SQLException {
        lock.readLock().lock();
        try {
            ensureConnection();
            
            String sql = "SELECT 1 FROM changelog_entries WHERE id = ? LIMIT 1";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public void close() {
        lock.writeLock().lock();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed successfully");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error while closing database connection: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Method to release the read lock when ResultSet is closed
    public void releaseReadLock() {
        lock.readLock().unlock();
    }

    public boolean isConnected() {
        lock.readLock().lock();
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }
}
