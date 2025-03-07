package at.dergamer09.changelog;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class Changelog extends JavaPlugin implements CommandExecutor, Listener {

    private static final int PAGE_SIZE = 20;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.getCommand("changelog").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("changelog")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                int page = 0;
                if (args.length > 0) {
                    try {
                        page = Integer.parseInt(args[0]) - 1;
                    } catch (NumberFormatException ignored) {
                    }
                }
                openChangelogGUI(player, page);
            } else {
                sender.sendMessage(ChatColor.RED + "Dieser Befehl kann nur von einem Spieler ausgeführt werden.");
            }
            return true;
        }
        return false;
    }

    private void openChangelogGUI(Player player, int page) {
        FileConfiguration config = this.getConfig();
        List<String> changelog = config.getStringList("changelog");
        int totalPages = (int) Math.ceil((double) changelog.size() / PAGE_SIZE);

        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Changelog - Seite " + (page + 1));

        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = page * PAGE_SIZE + i;
            if (index >= changelog.size()) break;

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + "Update " + (index + 1));
                meta.setLore(List.of(ChatColor.WHITE + changelog.get(index)));
                item.setItemMeta(meta);
            }
            gui.setItem(i, item);
        }

        if (page > 0) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName(ChatColor.GREEN + "Vorherige Seite");
                prevPage.setItemMeta(prevMeta);
            }
            gui.setItem(21, prevPage);
        }

        if (page < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName(ChatColor.GREEN + "Nächste Seite");
                nextPage.setItemMeta(nextMeta);
            }
            gui.setItem(23, nextPage);
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith(ChatColor.GOLD + "Changelog - Seite")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();

            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
                String itemName = event.getCurrentItem().getItemMeta().getDisplayName();
                int currentPage = Integer.parseInt(event.getView().getTitle().replaceAll("[^0-9]", "")) - 1;
                if (itemName.contains("Vorherige Seite")) {
                    openChangelogGUI(player, currentPage - 1);
                } else if (itemName.contains("Nächste Seite")) {
                    openChangelogGUI(player, currentPage + 1);
                }
            }
        }
    }
}

