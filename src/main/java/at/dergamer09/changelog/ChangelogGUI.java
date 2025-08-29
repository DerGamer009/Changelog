package at.dergamer09.changelog;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChangelogGUI implements Listener {
    private final ChangelogPlugin plugin;
    private static final int ENTRIES_PER_PAGE = 28; // 4 rows of 7 items
    private static final String GUI_TITLE_PREFIX = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Changelog";

    public ChangelogGUI(ChangelogPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        // Run database query asynchronously to avoid blocking main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<ChangelogEntry> entries = loadEntries(player);
            if (entries == null) return; // Error already handled
            
            // Switch back to main thread for GUI operations
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                createAndShowGUI(player, page, entries);
            });
        });
    }

    private List<ChangelogEntry> loadEntries(Player player) {
        List<ChangelogEntry> entries = new ArrayList<>();
        ResultSet rs = null;
        try {
            rs = plugin.getDatabase().getEntries();
            while (rs.next()) {
                int id = rs.getInt("id");
                String date = rs.getString("date");
                String message = rs.getString("message");
                entries.add(new ChangelogEntry(id, date, message));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load changelog entries: " + e.getMessage());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.RED + "❌ Fehler beim Laden des Changelogs. Bitte kontaktiere einen Administrator.");
            });
            return null;
        } finally {
            // Properly close ResultSet to prevent memory leaks and release database lock
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
        return entries;
    }

    private void createAndShowGUI(Player player, int page, List<ChangelogEntry> entries) {
        // Calculate pagination
        int maxPages = Math.max(1, (int) Math.ceil(entries.size() / (double) ENTRIES_PER_PAGE));
        page = Math.max(0, Math.min(page, maxPages - 1)); // Clamp page to valid range

        // Create inventory with improved title
        String title = GUI_TITLE_PREFIX + " " + ChatColor.WHITE + "(" + (page + 1) + "/" + maxPages + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Add decorative border
        addBorder(inv);

        // Add changelog entries
        addChangelogEntries(inv, entries, page);

        // Add navigation and control buttons
        addNavigationButtons(inv, page, maxPages, entries.size());

        player.openInventory(inv);
    }

    private void addBorder(Inventory inv) {
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        
        // Top and bottom borders
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(45 + i, border);
        }
        
        // Side borders
        for (int i = 1; i < 5; i++) {
            inv.setItem(i * 9, border);
            inv.setItem(i * 9 + 8, border);
        }
    }

    private void addChangelogEntries(Inventory inv, List<ChangelogEntry> entries, int page) {
        int start = page * ENTRIES_PER_PAGE;
        int slot = 10; // Start position (avoiding borders)
        
        for (int i = 0; i < ENTRIES_PER_PAGE && (start + i) < entries.size(); i++) {
            ChangelogEntry entry = entries.get(start + i);
            
            // Skip border positions
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot += (slot % 9 == 0) ? 1 : 2;
            }
            
            ItemStack item = createChangelogItem(entry);
            inv.setItem(slot, item);
            
            slot++;
            // Move to next row if needed
            if (slot % 9 == 8) {
                slot += 2;
            }
        }
    }

    private ItemStack createChangelogItem(ChangelogEntry entry) {
        Material material = Material.WRITABLE_BOOK;
        String displayName = ChatColor.AQUA + "" + ChatColor.BOLD + "Changelog #" + entry.getId();
        
        List<String> lore = Arrays.asList(
            "",
            ChatColor.GRAY + "📅 Datum: " + ChatColor.WHITE + formatDate(entry.getDate()),
            "",
            ChatColor.YELLOW + "📝 Änderungen:",
            ChatColor.WHITE + wrapText(entry.getMessage(), 30),
            "",
            ChatColor.DARK_GRAY + "ID: " + entry.getId()
        );
        
        return createItem(material, displayName, lore);
    }

    private void addNavigationButtons(Inventory inv, int page, int maxPages, int totalEntries) {
        // Previous page button
        if (page > 0) {
            ItemStack prev = createItem(
                Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "◀ Vorherige Seite",
                Arrays.asList(
                    "",
                    ChatColor.GRAY + "Klicke um zur vorherigen Seite zu gehen",
                    ChatColor.DARK_GRAY + "Seite " + page + "/" + maxPages
                )
            );
            inv.setItem(48, prev);
        }

        // Next page button
        if (page < maxPages - 1) {
            ItemStack next = createItem(
                Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Nächste Seite ▶",
                Arrays.asList(
                    "",
                    ChatColor.GRAY + "Klicke um zur nächsten Seite zu gehen",
                    ChatColor.DARK_GRAY + "Seite " + (page + 2) + "/" + maxPages
                )
            );
            inv.setItem(50, next);
        }

        // Close button
        ItemStack close = createItem(
            Material.RED_STAINED_GLASS_PANE,
            ChatColor.RED + "" + ChatColor.BOLD + "❌ Schließen",
            Arrays.asList(
                "",
                ChatColor.GRAY + "Klicke um das Changelog zu schließen"
            )
        );
        inv.setItem(49, close);

        // Info button
        ItemStack info = createItem(
            Material.BOOK,
            ChatColor.GOLD + "" + ChatColor.BOLD + "ℹ Information",
            Arrays.asList(
                "",
                ChatColor.YELLOW + "📊 Statistiken:",
                ChatColor.WHITE + "• Einträge gesamt: " + ChatColor.AQUA + totalEntries,
                ChatColor.WHITE + "• Aktuelle Seite: " + ChatColor.AQUA + (page + 1) + "/" + maxPages,
                "",
                ChatColor.GRAY + "Entwickelt von " + ChatColor.WHITE + "DerGamer09"
            )
        );
        inv.setItem(47, info);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatDate(String dateStr) {
        // Simple date formatting - could be enhanced with proper date parsing
        if (dateStr != null && dateStr.length() > 10) {
            return dateStr.substring(0, 10); // Just take the date part
        }
        return dateStr != null ? dateStr : "Unbekannt";
    }

    private String wrapText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text != null ? text : "";
        }
        
        StringBuilder wrapped = new StringBuilder();
        String[] words = text.split(" ");
        int currentLength = 0;
        
        for (String word : words) {
            if (currentLength + word.length() + 1 > maxLength) {
                if (wrapped.length() > 0) {
                    wrapped.append("\n").append(ChatColor.WHITE);
                }
                currentLength = 0;
            } else if (wrapped.length() > 0) {
                wrapped.append(" ");
                currentLength++;
            }
            wrapped.append(word);
            currentLength += word.length();
        }
        
        return wrapped.toString();
    }

    // Inner class to represent changelog entries
    private static class ChangelogEntry {
        private final int id;
        private final String date;
        private final String message;

        public ChangelogEntry(int id, String date, String message) {
            this.id = id;
            this.date = date;
            this.message = message;
        }

        public int getId() { return id; }
        public String getDate() { return date; }
        public String getMessage() { return message; }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith(GUI_TITLE_PREFIX)) {
            return;
        }
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        int slot = event.getRawSlot();
        int currentPage = getCurrentPage(title);
        
        // Handle navigation buttons
        switch (slot) {
            case 48: // Previous page
                if (currentPage > 0) {
                    open(player, currentPage - 1);
                }
                break;
            case 50: // Next page
                open(player, currentPage + 1);
                break;
            case 49: // Close button
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "✓ Changelog geschlossen.");
                break;
            case 47: // Info button - could add more functionality here
                player.sendMessage(ChatColor.GOLD + "ℹ " + ChatColor.YELLOW + "Changelog Plugin von DerGamer09");
                break;
            default:
                // Check if clicked on a changelog entry
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() == Material.WRITABLE_BOOK) {
                    // Could add functionality to show detailed view of changelog entry
                    player.sendMessage(ChatColor.GRAY + "💡 Detailansicht für Changelog-Einträge kommt bald!");
                }
                break;
        }
    }
    
    private int getCurrentPage(String title) {
        try {
            // Extract page number from title like "Changelog (2/5)"
            String stripped = ChatColor.stripColor(title);
            int start = stripped.indexOf('(') + 1;
            int end = stripped.indexOf('/');
            if (start > 0 && end > start) {
                return Integer.parseInt(stripped.substring(start, end)) - 1;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse page number from GUI title: " + title);
        }
        return 0;
    }
}
