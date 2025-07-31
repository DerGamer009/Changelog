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
import java.util.List;

public class ChangelogGUI implements Listener {
    private final ChangelogPlugin plugin;

    public ChangelogGUI(ChangelogPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        int entriesPerPage = 36;
        List<ItemStack> items = new ArrayList<>();
        try {
            ResultSet rs = plugin.getDatabase().getEntries();
            while (rs.next()) {
                String date = rs.getString("date");
                String msg = rs.getString("message");
                ItemStack book = new ItemStack(Material.BOOK);
                ItemMeta meta = book.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.GRAY + date + ChatColor.WHITE + " " + msg);
                    book.setItemMeta(meta);
                }
                items.add(book);
            }
        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "Error loading changelog.");
            return;
        }
        int maxPages = (int) Math.ceil(items.size() / (double) entriesPerPage);
        if (page < 0) page = 0;
        if (page >= maxPages) page = maxPages - 1;
        if (maxPages == 0) maxPages = 1;

        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "Changelog " + (page + 1) + "/" + maxPages);
        int start = page * entriesPerPage;
        for (int i = 0; i < entriesPerPage; i++) {
            int index = start + i;
            if (index >= items.size()) break;
            inv.setItem(i, items.get(index));
        }
        // navigation
        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta pm = prev.getItemMeta();
        if (pm != null) {
            pm.setDisplayName(ChatColor.GREEN + "Previous");
            prev.setItemMeta(pm);
        }
        inv.setItem(45, prev);

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nm = next.getItemMeta();
        if (nm != null) {
            nm.setDisplayName(ChatColor.GREEN + "Next");
            next.setItemMeta(nm);
        }
        inv.setItem(53, next);

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        if (cm != null) {
            cm.setDisplayName(ChatColor.RED + "Close");
            close.setItemMeta(cm);
        }
        inv.setItem(49, close);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith(ChatColor.DARK_PURPLE + "Changelog")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            String title = ChatColor.stripColor(event.getView().getTitle());
            String[] split = title.split(" ");
            int page = 0;
            if (split.length > 1) {
                String[] p = split[1].split("/");
                if (p.length > 0) {
                    page = Integer.parseInt(p[0]) - 1;
                }
            }
            int slot = event.getRawSlot();
            if (slot == 45) {
                open(player, page - 1);
            } else if (slot == 53) {
                open(player, page + 1);
            } else if (slot == 49) {
                player.closeInventory();
            }
        }
    }
}
