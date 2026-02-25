package skyBackpack;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class BackpackListener implements Listener {

    private final SkyBackpack plugin;

    public BackpackListener(SkyBackpack plugin) {
        this.plugin = plugin;
    }

    // ──────────────────────────────────────────────────
    // Open backpack on right-click
    // ──────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        BackpackType type = BackpackType.fromItem(item);
        if (type == null) return;

        event.setCancelled(true);
        plugin.getGUIManager().openBackpack(player, type, 0);
    }

    // ──────────────────────────────────────────────────
    // InventoryClick – block UI slots + anti-dupe + navigation
    // ──────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();

        GUIManager gui = plugin.getGUIManager();
        if (!gui.hasSession(uuid)) return;

        GUIManager.BackpackSession session = gui.getSession(uuid);
        BackpackType type = session.getType();
        Inventory topInv = session.getInventory();

        // Make sure the click is in our inventory
        boolean isTopInv = event.getClickedInventory() != null
                && event.getClickedInventory().equals(topInv);
        boolean isBottomInv = event.getClickedInventory() != null
                && event.getClickedInventory().equals(player.getInventory());

        int slot = event.getSlot();
        ClickType click = event.getClick();

        // Block double-click to prevent duplication
        if (click == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }

        if (isTopInv) {
            // Block UI slots completely
            if (gui.isUISlot(type, slot)) {
                event.setCancelled(true);

                // Handle arrow navigation
                if (gui.isArrowSlot(type, slot)) {
                    gui.saveCurrentPage(player);
                    int currentPage = session.getPage();
                    int totalPages = type.getTotalPages();
                    int newPage;
                    if (click == ClickType.LEFT) {
                        newPage = (currentPage + 1) % totalPages;
                    } else if (click == ClickType.RIGHT) {
                        newPage = (currentPage - 1 + totalPages) % totalPages;
                    } else {
                        return;
                    }
                    // Re-open on next tick to avoid inventory glitch
                    final int fp = newPage;
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        gui.removeSession(uuid);
                        gui.openBackpack(player, type, fp);
                    });
                }
                return;
            }

            // Block putting backpacks inside backpacks
            ItemStack cursor = event.getCursor();
            if (cursor != null && BackpackType.fromItem(cursor) != null) {
                event.setCancelled(true);
                player.sendMessage("§cTu ne peux pas mettre un backpack dans un backpack !");
                return;
            }
            ItemStack current = event.getCurrentItem();
            if (current != null && BackpackType.fromItem(current) != null && click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                event.setCancelled(true);
                return;
            }

            // Block shift-click from top inventory (would dupe to player inv)
            if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                event.setCancelled(true);
                return;
            }
        }

        if (isBottomInv) {
            // Block shift-click from player inv putting backpacks into backpack GUI
            if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                ItemStack item = event.getCurrentItem();
                if (item != null && BackpackType.fromItem(item) != null) {
                    event.setCancelled(true);
                    player.sendMessage("§cTu ne peux pas mettre un backpack dans un backpack !");
                }
            }
        }
    }

    // ──────────────────────────────────────────────────
    // InventoryDrag – block drags that touch UI slots
    // ──────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();

        GUIManager gui = plugin.getGUIManager();
        if (!gui.hasSession(uuid)) return;

        GUIManager.BackpackSession session = gui.getSession(uuid);
        BackpackType type = session.getType();
        Inventory topInv = session.getInventory();
        int topSize = topInv.getSize();

        // Check if any dragged slot is a UI slot in the top inventory
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) { // raw slot in top inventory
                if (gui.isUISlot(type, slot)) {
                    event.setCancelled(true);
                    return;
                }
                // Also block dragging backpacks into backpack inventory
                if (event.getNewItems().containsKey(slot)) {
                    ItemStack dragged = event.getOldCursor();
                    if (dragged != null && BackpackType.fromItem(dragged) != null) {
                        event.setCancelled(true);
                        player.sendMessage("§cTu ne peux pas mettre un backpack dans un backpack !");
                        return;
                    }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────
    // InventoryClose – save on close
    // ──────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();

        GUIManager gui = plugin.getGUIManager();
        if (!gui.hasSession(uuid)) return;

        gui.saveCurrentPage(player);
        gui.removeSession(uuid);
    }

    // ──────────────────────────────────────────────────
    // PlayerQuit – save and clean up
    // ──────────────────────────────────────────────────

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        GUIManager gui = plugin.getGUIManager();
        if (gui.hasSession(uuid)) {
            gui.saveCurrentPage(player);
            gui.removeSession(uuid);
        }
        plugin.getStorageManager().autosave();
    }
}
