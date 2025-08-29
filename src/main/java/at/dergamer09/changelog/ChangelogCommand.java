package at.dergamer09.changelog;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChangelogCommand implements CommandExecutor, TabCompleter {
    private final ChangelogPlugin plugin;
    private final ChangelogGUI gui;

    public ChangelogCommand(ChangelogPlugin plugin) {
        this.plugin = plugin;
        this.gui = new ChangelogGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return handleDefaultCommand(sender);
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "add":
                return handleAddCommand(sender, args);
            case "remove", "delete":
                return handleRemoveCommand(sender, args);
            case "list":
                return handleListCommand(sender);
            case "help":
                return handleHelpCommand(sender);
            case "reload":
                return handleReloadCommand(sender);
            default:
                sender.sendMessage(ChatColor.RED + "❌ Unbekannter Befehl. Nutze " + ChatColor.YELLOW + "/changelog help" + ChatColor.RED + " für Hilfe.");
                return true;
        }
    }

    private boolean handleDefaultCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "❌ Nur Spieler können das Changelog-GUI öffnen.");
            sender.sendMessage(ChatColor.YELLOW + "💡 Nutze " + ChatColor.WHITE + "/changelog list" + ChatColor.YELLOW + " um Einträge in der Konsole anzuzeigen.");
            return true;
        }

        // Open GUI on main thread - the GUI class handles async database operations
        gui.open(player, 0);
        return true;
    }

    private boolean handleAddCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("changelog.admin")) {
            sender.sendMessage(ChatColor.RED + "❌ Keine Berechtigung. Benötigt: " + ChatColor.YELLOW + "changelog.admin");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "❌ Verwendung: " + ChatColor.YELLOW + "/changelog add <Nachricht>");
            sender.sendMessage(ChatColor.GRAY + "Beispiel: " + ChatColor.WHITE + "/changelog add Neue Features hinzugefügt");
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        // Validate message length
        if (message.length() > 500) {
            sender.sendMessage(ChatColor.RED + "❌ Die Nachricht ist zu lang. Maximum: 500 Zeichen.");
            return true;
        }

        // Add entry asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getDatabase().addEntry(message);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.GREEN + "✓ Changelog-Eintrag erfolgreich hinzugefügt!");
                    sender.sendMessage(ChatColor.GRAY + "📝 Nachricht: " + ChatColor.WHITE + message);
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to add changelog entry: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.RED + "❌ Fehler beim Hinzufügen des Eintrags. Siehe Konsole für Details.");
                });
            }
        });
        return true;
    }

    private boolean handleRemoveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("changelog.admin")) {
            sender.sendMessage(ChatColor.RED + "❌ Keine Berechtigung. Benötigt: " + ChatColor.YELLOW + "changelog.admin");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "❌ Verwendung: " + ChatColor.YELLOW + "/changelog remove <ID>");
            sender.sendMessage(ChatColor.GRAY + "💡 Nutze " + ChatColor.WHITE + "/changelog list" + ChatColor.GRAY + " um IDs zu sehen.");
            return true;
        }

        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "❌ Ungültige ID. Bitte gib eine Zahl ein.");
            return true;
        }

        // Remove entry asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getDatabase().removeEntry(id);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.GREEN + "✓ Changelog-Eintrag #" + id + " erfolgreich entfernt!");
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to remove changelog entry: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.RED + "❌ Fehler beim Entfernen des Eintrags. Siehe Konsole für Details.");
                });
            }
        });
        return true;
    }

    private boolean handleListCommand(CommandSender sender) {
        if (!sender.hasPermission("changelog.admin")) {
            sender.sendMessage(ChatColor.RED + "❌ Keine Berechtigung. Benötigt: " + ChatColor.YELLOW + "changelog.admin");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "📋 Lade Changelog-Einträge...");
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            ResultSet rs = null;
            try {
                rs = plugin.getDatabase().getEntries();
                List<String> entries = new ArrayList<>();
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String date = rs.getString("date");
                    String message = rs.getString("message");
                    String formattedDate = date.length() > 10 ? date.substring(0, 10) : date;
                    entries.add(ChatColor.AQUA + "#" + id + ChatColor.GRAY + " [" + formattedDate + "] " + ChatColor.WHITE + message);
                }

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (entries.isEmpty()) {
                        sender.sendMessage(ChatColor.YELLOW + "📝 Keine Changelog-Einträge gefunden.");
                        sender.sendMessage(ChatColor.GRAY + "💡 Nutze " + ChatColor.WHITE + "/changelog add <Nachricht>" + ChatColor.GRAY + " um einen Eintrag hinzuzufügen.");
                    } else {
                        sender.sendMessage(ChatColor.GOLD + "📋 " + ChatColor.BOLD + "Changelog-Einträge (" + entries.size() + "):");
                        sender.sendMessage(ChatColor.GRAY + "─────────────────────────────────");
                        for (String entry : entries) {
                            sender.sendMessage(entry);
                        }
                        sender.sendMessage(ChatColor.GRAY + "─────────────────────────────────");
                    }
                });

            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to list changelog entries: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.RED + "❌ Fehler beim Laden der Einträge. Siehe Konsole für Details.");
                });
            } finally {
                if (rs != null) {
                    try {
                        rs.getStatement().close();
                        rs.close();
                        plugin.getDatabase().releaseReadLock(); // Release the read lock
                    } catch (SQLException e) {
                        plugin.getLogger().warning("Failed to close ResultSet: " + e.getMessage());
                    }
                }
            }
        });
        return true;
    }

    private boolean handleHelpCommand(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "📖 " + ChatColor.BOLD + "Changelog Plugin - Hilfe");
        sender.sendMessage(ChatColor.GRAY + "─────────────────────────────────");
        sender.sendMessage(ChatColor.YELLOW + "/changelog" + ChatColor.GRAY + " - Öffnet das Changelog-GUI");
        
        if (sender.hasPermission("changelog.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/changelog add <Nachricht>" + ChatColor.GRAY + " - Fügt einen Eintrag hinzu");
            sender.sendMessage(ChatColor.YELLOW + "/changelog remove <ID>" + ChatColor.GRAY + " - Entfernt einen Eintrag");
            sender.sendMessage(ChatColor.YELLOW + "/changelog list" + ChatColor.GRAY + " - Listet alle Einträge auf");
            sender.sendMessage(ChatColor.YELLOW + "/changelog reload" + ChatColor.GRAY + " - Lädt die Konfiguration neu");
        }
        
        sender.sendMessage(ChatColor.YELLOW + "/changelog help" + ChatColor.GRAY + " - Zeigt diese Hilfe");
        sender.sendMessage(ChatColor.GRAY + "─────────────────────────────────");
        sender.sendMessage(ChatColor.DARK_GRAY + "Plugin von DerGamer09 • Version 1.0.0");
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("changelog.admin")) {
            sender.sendMessage(ChatColor.RED + "❌ Keine Berechtigung. Benötigt: " + ChatColor.YELLOW + "changelog.admin");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "🔄 Lade Konfiguration neu...");
        
        try {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "✓ Konfiguration erfolgreich neu geladen!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reload config: " + e.getMessage());
            sender.sendMessage(ChatColor.RED + "❌ Fehler beim Neuladen der Konfiguration. Siehe Konsole für Details.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();
            
            List<String> commands = Arrays.asList("help");
            if (sender.hasPermission("changelog.admin")) {
                commands = Arrays.asList("add", "remove", "delete", "list", "help", "reload");
            }
            
            for (String cmd : commands) {
                if (cmd.startsWith(input)) {
                    completions.add(cmd);
                }
            }
            return completions;
        }
        return new ArrayList<>();
    }

    public ChangelogGUI getGui() {
        return gui;
    }
}
