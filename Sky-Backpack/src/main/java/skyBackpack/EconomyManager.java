package skyBackpack;

import fr.chiroyuki.skyCore.api.economy.EconomyService;
import fr.chiroyuki.skyCore.impl.SkyCoreAPIProvider;
import org.bukkit.entity.Player;

public class EconomyManager {

    private final SkyBackpack plugin;

    public EconomyManager(SkyBackpack plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        try {
            return SkyCoreAPIProvider.get() != null && SkyCoreAPIProvider.get().economy() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAvailable() {
        try {
            return SkyCoreAPIProvider.get() != null && SkyCoreAPIProvider.get().economy() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private EconomyService eco() {
        return SkyCoreAPIProvider.get().economy();
    }

    public boolean has(Player player, double amount) {
        return eco().has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        return eco().withdraw(player, amount);
    }

    public String format(double amount) {
        return eco().format(amount);
    }

    public double getBalance(Player player) {
        return eco().getBalance(player);
    }
}
