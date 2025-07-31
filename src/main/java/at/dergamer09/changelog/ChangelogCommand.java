package at.dergamer09.changelog;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ChangelogCommand implements CommandExecutor {
    private final ChangelogPlugin plugin;

    public ChangelogCommand(ChangelogPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    new ChangelogGUI(plugin).open(player, 0);
                });
            } else {
                sender.sendMessage(ChatColor.RED + "Only players can open the GUI.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (!sender.hasPermission("changelog.admin")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "/changelog add <Text>");
                return true;
            }
            String text = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            plugin.getDatabase().addEntry(text);
            sender.sendMessage(ChatColor.GREEN + "Entry added.");
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (!sender.hasPermission("changelog.admin")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "/changelog remove <ID>");
                return true;
            }
            try {
                int id = Integer.parseInt(args[1]);
                plugin.getDatabase().removeEntry(id);
                sender.sendMessage(ChatColor.GREEN + "Entry removed.");
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid ID.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            if (!sender.hasPermission("changelog.admin")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            try {
                ResultSet rs = plugin.getDatabase().getEntries();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String date = rs.getString("date");
                    String msg = rs.getString("message");
                    sender.sendMessage("#" + id + " [" + date + "] " + msg);
                }
            } catch (SQLException e) {
                sender.sendMessage("Error reading entries.");
            }
            return true;
        }

        return false;
    }
}
