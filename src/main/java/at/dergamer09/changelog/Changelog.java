package at.dergamer09.changelog;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Changelog extends JavaPlugin implements CommandExecutor, Listener {

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        int pluginId = 25012;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new SingleLineChart("players_online", () -> Bukkit.getOnlinePlayers().size()));

        this.getCommand("changelog").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("--------------------------------------------------------");
        getLogger().info(ChatColor.GOLD + "Changelog has been enabled!");
        getLogger().info(ChatColor.GREEN + "Version: " + this.getDescription().getVersion());
        getLogger().info(ChatColor.GREEN + "Support: https://discord.gg/fKgyae8R4e");
        getLogger().info("--------------------------------------------------------");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("changelog")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("changelog.reload")) {
                    this.reloadConfig();
                    sender.sendMessage(getMessage("reload_success"));
                } else {
                    sender.sendMessage(getMessage("no_permission"));
                }
                return true;
            }

            if (sender instanceof Player player) {
                if (!player.hasPermission("changelog.use")) {
                    sender.sendMessage(getMessage("no_permission"));
                    return true;
                }

                int page = 0;
                if (args.length > 0) {
                    try {
                        page = Integer.parseInt(args[0]) - 1;
                    } catch (NumberFormatException ignored) {
                    }
                }
                openChangelogGUI(player, page);
            } else {
                sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
            }
            return true;
        }
        return false;
    }

    private void openChangelogGUI(Player player, int page) {
        FileConfiguration config = this.getConfig();
        List<Map<?, ?>> changelogList = config.getMapList("changelog");
        List<Integer> entrySlots = Arrays.asList(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        );

        int entriesPerPage = Math.min(config.getInt("entries_per_page", 21), entrySlots.size());
        int totalPages = (int) Math.ceil((double) Math.max(changelogList.size(), 1) / entriesPerPage);

        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        Inventory gui = Bukkit.createInventory(null, 54, getMessage("changelog_title")
                .replace("{page}", String.valueOf(page + 1))
                .replace("{max_page}", String.valueOf(totalPages)));

        // GUI Slots for Changelog Entries

        // Fill with border if enabled
        if (config.getBoolean("gui_border.enabled", false)) {
            Material borderMaterial = Material.getMaterial(config.getString("gui_border.material", "GRAY_STAINED_GLASS_PANE"));
            if (borderMaterial != null) {
                List<Integer> borderSlots = config.getIntegerList("gui_border.slots");
                if (borderSlots.isEmpty()) {
                    borderSlots = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8,
                            9, 17, 18, 26, 27, 35,
                            36, 37, 38, 39, 40, 41, 42, 43, 44);
                }
                for (int slot : borderSlots) {
                    if (slot >= 0 && slot < gui.getSize()) {
                        gui.setItem(slot, new ItemStack(borderMaterial));
                    }
                }
            }
        }

        // Add Changelog Entries in Proper Slots
        for (int i = 0; i < entriesPerPage; i++) {
            int index = page * entriesPerPage + i;
            if (index >= changelogList.size()) break;

            Map<?, ?> entry = changelogList.get(index);
            String title = ChatColor.translateAlternateColorCodes('&', (String) entry.get("title"));
            String date = ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', (String) entry.get("date"));
            List<String> content = (List<String>) entry.get("content");

            List<String> formattedContent = new ArrayList<>();
            formattedContent.add(date);
            formattedContent.add(" ");
            for (String line : content) {
                formattedContent.add(ChatColor.translateAlternateColorCodes('&', line));
            }

            Material entryMaterial = Material.getMaterial(config.getString("entry_item_material", "WRITABLE_BOOK"));
            if (entryMaterial == null) entryMaterial = Material.WRITABLE_BOOK;
            ItemStack item = new ItemStack(entryMaterial);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(title);
                meta.setLore(formattedContent);
                item.setItemMeta(meta);
            }
            gui.setItem(entrySlots.get(i), item);
        }

        // Navigation Buttons
        int previousSlot = config.getInt("navigation.previous.slot", 48);
        int nextSlot = config.getInt("navigation.next.slot", 50);
        addNavigationButton(gui, "previous", previousSlot, page > 0 ? page - 1 : -1, player);
        addNavigationButton(gui, "next", nextSlot, page < totalPages - 1 ? page + 1 : -1, player);

        // Close Button
        if (config.getBoolean("close_button.enabled", false)) {
            int closeSlot = config.getInt("close_button.slot", 49);
            Material closeMat = Material.getMaterial(config.getString("close_button.material", "ENDER_EYE"));
            if (closeMat == null) closeMat = Material.ENDER_EYE;
            ItemStack closeItem = new ItemStack(closeMat);
            ItemMeta meta = closeItem.getItemMeta();
            if (meta != null) {
                String name = config.getString("close_button.display_name");
                if (name != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                } else {
                    meta.setDisplayName(getMessage("menu_closed"));
                }
                meta.setLore(config.getStringList("close_button.lore"));
                closeItem.setItemMeta(meta);
            }
            gui.setItem(closeSlot, closeItem);
        }

        player.openInventory(gui);
    }

    private void addNavigationButton(Inventory gui, String type, int slot, int newPage, Player player) {
        FileConfiguration config = this.getConfig();
        String path = "navigation." + type;
        if (newPage == -1) return; // Don't add button if there's no next/previous page

        ItemStack navItem = new ItemStack(Material.getMaterial(config.getString(path + ".material", "FEATHER")));
        ItemMeta meta = navItem.getItemMeta();
        if (meta != null) {
            String name = config.getString(path + ".display_name");
            if (name != null) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            else meta.setDisplayName(getMessage(type + "_page"));

            List<String> lore = config.getStringList(path + ".lore");
            if (!lore.isEmpty()) {
                for (int i = 0; i < lore.size(); i++) {
                    lore.set(i, ChatColor.translateAlternateColorCodes('&', lore.get(i)));
                }
                meta.setLore(lore);
            }
            navItem.setItemMeta(meta);
        }
        gui.setItem(slot, navItem);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith(getMessage("changelog_title").split(" ")[0])) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();

            if (event.getCurrentItem() == null) return;
            FileConfiguration config = this.getConfig();

            int previousSlot = config.getInt("navigation.previous.slot", 48);
            int nextSlot = config.getInt("navigation.next.slot", 50);
            int closeSlot = config.getInt("close_button.slot", 49);

            int rawSlot = event.getRawSlot();

            String title = ChatColor.stripColor(event.getView().getTitle());
            int currentPage = 0;
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)/(\\d+)").matcher(title);
            if (matcher.find()) {
                currentPage = Integer.parseInt(matcher.group(1)) - 1;
            }

            if (rawSlot == previousSlot) {
                openChangelogGUI(player, currentPage - 1);
            } else if (rawSlot == nextSlot) {
                openChangelogGUI(player, currentPage + 1);
            } else if (rawSlot == closeSlot && config.getBoolean("close_button.enabled", false)) {
                player.closeInventory();
                player.sendMessage(getMessage("menu_closed"));
            }
        }
    }

    private String getMessage(String key) {
        String lang = getConfig().getString("language", "en");
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages." + lang + "." + key, key));
    }
}
