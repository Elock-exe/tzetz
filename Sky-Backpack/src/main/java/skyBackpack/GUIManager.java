package skyBackpack;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages open backpack GUI sessions.
 */
public class GUIManager {

    private final SkyBackpack plugin;

    // Active sessions: playerUUID -> session info
    private final Map<UUID, BackpackSession> sessions = new HashMap<>();

    // ── Filler / UI items ──────────────────────────────
    private static final ItemStack FILLER;
    private static final ItemStack ARROW_NEXT;
    private static final ItemStack ARROW_PREV;

    static {
        FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = FILLER.getItemMeta();
        fm.setDisplayName(" ");
        FILLER.setItemMeta(fm);

        ARROW_NEXT = new ItemStack(Material.ARROW);
        ItemMeta nm = ARROW_NEXT.getItemMeta();
        nm.setDisplayName("§ePage suivante");
        nm.setLore(java.util.Arrays.asList("§7Clic gauche : page suivante", "§7Clic droit : page précédente"));
        ARROW_NEXT.setItemMeta(nm);

        ARROW_PREV = new ItemStack(Material.ARROW);
        ItemMeta pm = ARROW_PREV.getItemMeta();
        pm.setDisplayName("§ePage précédente");
        pm.setLore(java.util.Arrays.asList("§7Clic gauche : page suivante", "§7Clic droit : page précédente"));
        ARROW_PREV.setItemMeta(pm);
    }

    public GUIManager(SkyBackpack plugin) {
        this.plugin = plugin;
    }

    // ──────────────────────────────────────────────────
    // Open / refresh
    // ──────────────────────────────────────────────────

    public void openBackpack(Player player, BackpackType type, int page) {
        StorageManager storage = plugin.getStorageManager();
        int totalPages = type.getTotalPages();
        page = Math.max(0, Math.min(page, totalPages - 1));

        String title = type.getDisplayName() + " §8- §7Page §f" + (page + 1) + "§7/§f" + totalPages;
        Inventory inv = Bukkit.createInventory(null, type.getInventorySize(), title);

        // Fill usable slots with stored items
        ItemStack[] stored = storage.getPage(player.getUniqueId(), type, page);
        int usable = type.getUsableSlots();
        for (int s = 0; s < usable && s < stored.length; s++) {
            inv.setItem(s, stored[s]);
        }

        // For BP2 (45 slots, 40 usable): block slots 40-43 and 44 (arrow)
        if (type == BackpackType.BACKPACK_2) {
            for (int s = 40; s <= 43; s++) {
                inv.setItem(s, FILLER.clone());
            }
        }

        // Arrow in last slot (slot inventorySize - 1)
        int arrowSlot = type.getInventorySize() - 1;
        if (totalPages > 1) {
            inv.setItem(arrowSlot, page == 0 ? ARROW_NEXT.clone() : ARROW_PREV.clone());
        } else {
            // Single page: still show arrow as visual but pages won't change
            inv.setItem(arrowSlot, FILLER.clone());
        }

        // Register session
        int finalPage = page;
        sessions.put(player.getUniqueId(), new BackpackSession(type, finalPage, inv));

        player.openInventory(inv);
    }

    // ──────────────────────────────────────────────────
    // Session helpers
    // ──────────────────────────────────────────────────

    public BackpackSession getSession(UUID uuid) { return sessions.get(uuid); }

    public void removeSession(UUID uuid) { sessions.remove(uuid); }

    public boolean hasSession(UUID uuid) { return sessions.containsKey(uuid); }

    // ──────────────────────────────────────────────────
    // UI item checks
    // ──────────────────────────────────────────────────

    public boolean isArrowSlot(BackpackType type, int slot) {
        return slot == type.getInventorySize() - 1;
    }

    public boolean isFillerSlot(BackpackType type, int slot) {
        if (type == BackpackType.BACKPACK_2) {
            return slot >= 40 && slot <= 43;
        }
        return false;
    }

    public boolean isUISlot(BackpackType type, int slot) {
        return isArrowSlot(type, slot) || isFillerSlot(type, slot);
    }

    public ItemStack getFiller() { return FILLER.clone(); }

    // ──────────────────────────────────────────────────
    // Save current inventory contents into storage
    // ──────────────────────────────────────────────────

    public void saveCurrentPage(Player player) {
        BackpackSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        BackpackType type = session.getType();
        int page = session.getPage();
        Inventory inv = session.getInventory();
        int usable = type.getUsableSlots();

        ItemStack[] stored = new ItemStack[usable];
        for (int s = 0; s < usable; s++) {
            ItemStack item = inv.getItem(s);
            if (item != null && !item.getType().isAir()) {
                stored[s] = item.clone();
            }
        }

        plugin.getStorageManager().setPage(player.getUniqueId(), type, page, stored);
        plugin.getStorageManager().autosave();
    }

    // ──────────────────────────────────────────────────
    // Inner session class
    // ──────────────────────────────────────────────────

    public static class BackpackSession {
        private final BackpackType type;
        private final int page;
        private final Inventory inventory;

        public BackpackSession(BackpackType type, int page, Inventory inventory) {
            this.type = type;
            this.page = page;
            this.inventory = inventory;
        }

        public BackpackType getType() { return type; }
        public int getPage() { return page; }
        public Inventory getInventory() { return inventory; }
    }
}
