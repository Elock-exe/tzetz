package skyBackpack;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public enum BackpackType {

    BACKPACK_1(1, 4510, 27, 1, "§6Backpack §e1"),
    BACKPACK_2(2, 4511, 40, 2, "§6Backpack §e2"),
    BACKPACK_3(3, 4512, 54, 3, "§6Backpack §e3");

    private final int level;
    private final int customModelData;
    private final int usableSlots;
    private final int inventoryRows; // rows of 9
    private final String displayName;

    BackpackType(int level, int customModelData, int usableSlots, int inventoryRows, String displayName) {
        this.level = level;
        this.customModelData = customModelData;
        this.usableSlots = usableSlots;
        this.inventoryRows = inventoryRows;
        this.displayName = displayName;
    }

    public int getLevel() { return level; }
    public int getCustomModelData() { return customModelData; }
    public int getUsableSlots() { return usableSlots; }
    public String getDisplayName() { return displayName; }

    /**
     * Real inventory size used to open the chest inventory.
     * BP1: 27, BP2: 45 (5 rows), BP3: 54
     */
    public int getInventorySize() {
        switch (this) {
            case BACKPACK_1: return 27;
            case BACKPACK_2: return 45;
            case BACKPACK_3: return 54;
            default: return 27;
        }
    }

    /**
     * Number of pages needed for this backpack type.
     */
    public int getPageCount() {
        switch (this) {
            case BACKPACK_1: return 1;
            case BACKPACK_2: return 1;
            case BACKPACK_3: return 1;
            default: return 1;
        }
    }

    /**
     * Get the upgrade cost from this level to the next.
     */
    public long getUpgradeCost() {
        switch (this) {
            case BACKPACK_1: return 150_000L;
            case BACKPACK_2: return 300_000L;
            default: return -1L;
        }
    }

    /**
     * Get the next BackpackType, or null if max level.
     */
    public BackpackType getNext() {
        switch (this) {
            case BACKPACK_1: return BACKPACK_2;
            case BACKPACK_2: return BACKPACK_3;
            default: return null;
        }
    }

    /**
     * Returns the total number of usable item pages
     * given that each page holds usableSlots items.
     * For now every backpack has exactly 1 page.
     */
    public int getTotalPages() {
        return 1;
    }

    /**
     * Create the ItemStack for this backpack type.
     */
    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setCustomModelData(customModelData);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Detect the BackpackType from an ItemStack. Returns null if not a backpack.
     */
    public static BackpackType fromItem(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return null;
        if (!item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasCustomModelData()) return null;
        int cmd = meta.getCustomModelData();
        for (BackpackType type : values()) {
            if (type.customModelData == cmd) return type;
        }
        return null;
    }
}
