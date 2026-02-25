package skyBackpack;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class UpgradeCommand implements CommandExecutor {

    private final SkyBackpack plugin;

    public UpgradeCommand(SkyBackpack plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true;
        }

        Player player = (Player) sender;
        EconomyManager eco = plugin.getEconomyManager();

        // Check Vault availability
        if (!eco.isAvailable()) {
            player.sendMessage("§cVault ou le plugin d'économie est absent. L'upgrade est désactivé.");
            return true;
        }

        // Check item in main hand
        ItemStack item = player.getInventory().getItemInMainHand();
        BackpackType type = BackpackType.fromItem(item);

        if (type == null) {
            player.sendMessage("§cTu dois avoir un backpack en main pour l'upgrade.");
            return true;
        }

        // Check if already max level
        BackpackType next = type.getNext();
        if (next == null) {
            player.sendMessage("§eCe backpack est déjà au niveau max.");
            return true;
        }

        long cost = type.getUpgradeCost();

        // Check balance
        if (!eco.has(player, cost)) {
            player.sendMessage("§cFonds insuffisants ! L'upgrade coûte §e" + eco.format(cost)
                    + "§c. Tu as §e" + eco.format(plugin.getServer().getServicesManager()
                    .getRegistration(net.milkbowl.vault.economy.Economy.class)
                    .getProvider().getBalance(player)) + "§c.");
            return true;
        }

        // Withdraw money
        if (!eco.withdraw(player, cost)) {
            player.sendMessage("§cErreur lors du retrait de l'argent. Réessaie.");
            return true;
        }

        // Copy backpack contents to new type
        plugin.getStorageManager().copyData(player.getUniqueId(), type, next);
        plugin.getStorageManager().autosave();

        // Replace item in hand with upgraded backpack
        ItemStack newItem = next.createItem();
        player.getInventory().setItemInMainHand(newItem);

        player.sendMessage("§a✔ Ton backpack a été upgradé vers §e" + next.getDisplayName()
                + "§a ! §e" + eco.format(cost) + " §aont été retirés.");

        return true;
    }
}
