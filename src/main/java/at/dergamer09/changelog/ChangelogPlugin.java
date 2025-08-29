package at.dergamer09.changelog;

import org.bukkit.plugin.java.JavaPlugin;

public class ChangelogPlugin extends JavaPlugin {

    private DatabaseManager databaseManager;
    private ChangelogCommand changelogCommand;

    @Override
    public void onEnable() {
        // Save default configuration
        saveDefaultConfig();
        
        // Initialize database
        databaseManager = new DatabaseManager(this);
        databaseManager.init();
        
        if (!databaseManager.isConnected()) {
            getLogger().severe("Failed to connect to database. Plugin will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize command handler
        changelogCommand = new ChangelogCommand(this);
        getCommand("changelog").setExecutor(changelogCommand);
        getCommand("changelog").setTabCompleter(changelogCommand);
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(changelogCommand.getGui(), this);
        
        getLogger().info("Changelog Plugin v" + getDescription().getVersion() + " by DerGamer09 has been enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("Changelog Plugin has been disabled!");
    }

    public DatabaseManager getDatabase() {
        return databaseManager;
    }

    public ChangelogCommand getChangelogCommand() {
        return changelogCommand;
    }
}
