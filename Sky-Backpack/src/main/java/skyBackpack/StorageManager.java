package skyBackpack;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Stores backpack contents in backpacks.yml
 *
 * Structure:
 * players:
 *   <uuid>:
 *     BACKPACK_1:
 *       page_0:
 *         slot_0: <serialized ItemStack>
 *         ...
 *     BACKPACK_2:
 *       page_0: ...
 */
public class StorageManager {

    private final SkyBackpack plugin;
    private File file;
    private YamlConfiguration config;

    // Map<playerUUID, Map<BackpackType, List<ItemStack[]>>> (one array per page)
    private final Map<UUID, Map<BackpackType, List<ItemStack[]>>> data = new HashMap<>();

    public StorageManager(SkyBackpack plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "backpacks.yml");
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) return;

        for (String uuidStr : players.getKeys(false)) {
            UUID uuid;
            try { uuid = UUID.fromString(uuidStr); } catch (Exception e) { continue; }

            Map<BackpackType, List<ItemStack[]>> playerData = new HashMap<>();

            for (BackpackType type : BackpackType.values()) {
                ConfigurationSection typeSection = players.getConfigurationSection(uuidStr + "." + type.name());
                if (typeSection == null) continue;

                List<ItemStack[]> pages = new ArrayList<>();
                int pageIndex = 0;
                while (true) {
                    ConfigurationSection pageSection = typeSection.getConfigurationSection("page_" + pageIndex);
                    if (pageSection == null) break;

                    int size = type.getUsableSlots();
                    ItemStack[] items = new ItemStack[size];
                    for (int slot = 0; slot < size; slot++) {
                        Object obj = pageSection.get("slot_" + slot);
                        if (obj instanceof ItemStack) {
                            items[slot] = (ItemStack) obj;
                        }
                    }
                    pages.add(items);
                    pageIndex++;
                }

                if (!pages.isEmpty()) playerData.put(type, pages);
            }

            if (!playerData.isEmpty()) data.put(uuid, playerData);
        }
    }

    public void save() {
        config = new YamlConfiguration();

        for (Map.Entry<UUID, Map<BackpackType, List<ItemStack[]>>> playerEntry : data.entrySet()) {
            String uuidStr = playerEntry.getKey().toString();
            for (Map.Entry<BackpackType, List<ItemStack[]>> typeEntry : playerEntry.getValue().entrySet()) {
                String typeName = typeEntry.getKey().name();
                List<ItemStack[]> pages = typeEntry.getValue();
                for (int p = 0; p < pages.size(); p++) {
                    ItemStack[] items = pages.get(p);
                    for (int slot = 0; slot < items.length; slot++) {
                        if (items[slot] != null && items[slot].getType().isAir()) continue;
                        String path = "players." + uuidStr + "." + typeName + ".page_" + p + ".slot_" + slot;
                        config.set(path, items[slot]);
                    }
                }
            }
        }

        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    // ──────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────

    private Map<BackpackType, List<ItemStack[]>> getPlayerData(UUID uuid) {
        return data.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    /**
     * Get the items for a specific page of a backpack.
     * Creates empty storage if it doesn't exist.
     */
    public ItemStack[] getPage(UUID uuid, BackpackType type, int page) {
        List<ItemStack[]> pages = getPlayerData(uuid)
                .computeIfAbsent(type, k -> new ArrayList<>());

        // Fill missing pages
        while (pages.size() <= page) {
            pages.add(new ItemStack[type.getUsableSlots()]);
        }
        return pages.get(page);
    }

    /**
     * Save the items for a specific page.
     */
    public void setPage(UUID uuid, BackpackType type, int page, ItemStack[] items) {
        List<ItemStack[]> pages = getPlayerData(uuid)
                .computeIfAbsent(type, k -> new ArrayList<>());
        while (pages.size() <= page) {
            pages.add(new ItemStack[type.getUsableSlots()]);
        }
        pages.set(page, items);
    }

    /**
     * Copy all data from one BackpackType to another for a player.
     * Used during upgrade. Existing data in target is preserved / overwritten slot by slot.
     */
    public void copyData(UUID uuid, BackpackType from, BackpackType to) {
        List<ItemStack[]> fromPages = getPlayerData(uuid).get(from);
        if (fromPages == null) return;

        List<ItemStack[]> toPages = getPlayerData(uuid)
                .computeIfAbsent(to, k -> new ArrayList<>());

        for (int p = 0; p < fromPages.size(); p++) {
            ItemStack[] src = fromPages.get(p);
            int toSize = to.getUsableSlots();
            while (toPages.size() <= p) toPages.add(new ItemStack[toSize]);
            ItemStack[] dst = toPages.get(p);
            // Copy slot by slot up to the min size
            for (int s = 0; s < Math.min(src.length, dst.length); s++) {
                dst[s] = src[s];
            }
            toPages.set(p, dst);
        }
    }

    public void autosave() {
        save();
    }
}
