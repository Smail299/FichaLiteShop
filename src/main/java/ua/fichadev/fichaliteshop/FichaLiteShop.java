package ua.fichadev.fichaliteshop;

import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ua.fichadev.fichaliteshop.commands.LiteShopCommand;
import ua.fichadev.fichaliteshop.commands.ShopCommand;
import ua.fichadev.fichaliteshop.listeners.InventoryListener;
import ua.fichadev.fichaliteshop.managers.AuctionManager;
import ua.fichadev.fichaliteshop.managers.DataManager;
import ua.fichadev.fichaliteshop.managers.ShopManager;

public class FichaLiteShop extends JavaPlugin {
    private static FichaLiteShop instance;
    private Economy economy;
    private PlayerPointsAPI playerPointsAPI;
    private DataManager dataManager;
    private ShopManager shopManager;
    private AuctionManager auctionManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (!setupEconomy() || !setupPlayerPoints()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        dataManager = new DataManager(this);
        shopManager = new ShopManager(this);
        auctionManager = new AuctionManager(this);

        registerCommands();
        registerListeners();

        shopManager.startRefreshTask();
        shopManager.refreshAllPlayers();
        auctionManager.startAuctionCheckTask();
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAllData();
        }
    }

    private void registerCommands() {
        LiteShopCommand liteShopCommand = new LiteShopCommand(this);
        ShopCommand shopCommand = new ShopCommand(this);

        getCommand("shop").setExecutor(shopCommand);
        getCommand("shop").setTabCompleter(shopCommand);
        getCommand("liteshop").setExecutor(liteShopCommand);
        getCommand("liteshop").setTabCompleter(liteShopCommand);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new InventoryListener(this), this);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        return economy != null;
    }

    private boolean setupPlayerPoints() {
        if (getServer().getPluginManager().getPlugin("PlayerPoints") == null) {
            return false;
        }

        PlayerPoints playerPoints = (PlayerPoints) getServer().getPluginManager().getPlugin("PlayerPoints");
        playerPointsAPI = playerPoints.getAPI();
        return playerPointsAPI != null;
    }

    public static FichaLiteShop getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public PlayerPointsAPI getPlayerPointsAPI() {
        return playerPointsAPI;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public String getSapphiresCurrencyName() {
        return getConfig().getString("currencies.sapphires", "сапфиров ☀");
    }

    public String getCoinsCurrencyName() {
        return getConfig().getString("currencies.coins", "монеток ¤");
    }
}