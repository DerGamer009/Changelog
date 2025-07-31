package at.dergamer09.changelog;

import org.bukkit.plugin.java.JavaPlugin;

public class ChangelogPlugin extends JavaPlugin {

    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        databaseManager = new DatabaseManager(this);
        databaseManager.init();
        getCommand("changelog").setExecutor(new ChangelogCommand(this));
        getServer().getPluginManager().registerEvents(new ChangelogGUI(this), this);
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    public DatabaseManager getDatabase() {
        return databaseManager;
    }
}
