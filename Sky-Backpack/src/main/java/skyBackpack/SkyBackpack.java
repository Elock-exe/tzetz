package skyBackpack;

import org.bukkit.plugin.java.JavaPlugin;

public class SkyBackpack extends JavaPlugin {

    private static SkyBackpack instance;
    private StorageManager storageManager;
    private EconomyManager economyManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        instance = this;

        storageManager = new StorageManager(this);
        storageManager.load();

        economyManager = new EconomyManager(this);
        if (!economyManager.setup()) {
            getLogger().warning("Vault / plugin d'économie introuvable. La commande /upgrade sera désactivée.");
        }

        guiManager = new GUIManager(this);

        getServer().getPluginManager().registerEvents(new BackpackListener(this), this);

        getCommand("upgrade").setExecutor(new UpgradeCommand(this));

        getLogger().info("SkyBackpack activé.");
    }

    @Override
    public void onDisable() {
        if (storageManager != null) storageManager.save();
        getLogger().info("SkyBackpack désactivé.");
    }

    public static SkyBackpack getInstance() { return instance; }
    public StorageManager getStorageManager() { return storageManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public GUIManager getGUIManager() { return guiManager; }
}
