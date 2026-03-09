package ua.fichadev.fichaliteshop.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import ua.fichadev.fichaliteshop.FichaLiteShop;
import ua.fichadev.fichaliteshop.data.PlayerShopData;
import ua.fichadev.fichaliteshop.data.ShopItem;
import ua.fichadev.fichaliteshop.database.DatabaseManager;
import ua.fichadev.fichaliteshop.database.impl.MySQLDatabase;
import ua.fichadev.fichaliteshop.database.impl.SQLiteDatabase;
import ua.fichadev.fichaliteshop.utils.ColorUtils;

public class DataManager {
    private final FichaLiteShop plugin;
    private final Map<UUID, PlayerShopData> playerDataMap;
    private final List<ShopItem> availableItems;
    private final DatabaseManager databaseManager;
    private File dataFile;
    private FileConfiguration dataConfig;

    public DataManager(FichaLiteShop plugin) {
        this.plugin = plugin;
        this.playerDataMap = new HashMap<>();
        this.availableItems = new ArrayList<>();

        String type = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
        if (type.equals("mysql")) {
            this.databaseManager = new MySQLDatabase(plugin);
        } else {
            this.databaseManager = new SQLiteDatabase(plugin);
        }

        loadData();
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadItems();
    }

    private void loadItems() {
        ConfigurationSection itemsSection = dataConfig.getConfigurationSection("items");
        if (itemsSection == null) return;

        for (String key : itemsSection.getKeys(false)) {
            String path = "items." + key;
            String name = dataConfig.getString(path + ".name");
            ItemStack item = dataConfig.getItemStack(path + ".item");
            int price = dataConfig.getInt(path + ".price");
            boolean isAuction = dataConfig.getBoolean(path + ".auction", false);
            int stepPrice = dataConfig.getInt(path + ".stepPrice", 0);
            boolean isSapphireCurrency = dataConfig.getBoolean(path + ".isSapphireCurrency", true);

            if (item != null) {
                availableItems.add(new ShopItem(key, name, item, price, isAuction, stepPrice, isSapphireCurrency));
            }
        }
    }

    public void saveAllData() {
        saveItems();

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Map.Entry<UUID, PlayerShopData> entry : playerDataMap.entrySet()) {
            PlayerShopData data = entry.getValue();
            databaseManager.savePlayerShop(
                    entry.getKey(),
                    data.getCurrentItems(),
                    data.getDiscounts(),
                    data.getLastRefresh()
            );
        }
    }

    private void saveItems() {
        dataConfig.set("items", null);

        for (ShopItem item : availableItems) {
            String path = "items." + item.getId();
            dataConfig.set(path + ".name", item.getName());
            dataConfig.set(path + ".item", item.getItemStack());
            dataConfig.set(path + ".price", item.getPrice());
            dataConfig.set(path + ".auction", item.isAuction());
            dataConfig.set(path + ".stepPrice", item.getStepPrice());
            dataConfig.set(path + ".isSapphireCurrency", item.isSapphireCurrency());
        }
    }

    public PlayerShopData getPlayerData(UUID playerId) {
        if (!playerDataMap.containsKey(playerId)) {
            PlayerShopData data = new PlayerShopData(playerId);

            List<String> currentItems = new ArrayList<>();
            Map<String, Double> discounts = new HashMap<>();
            long[] lastRefresh = {0};
            databaseManager.loadPlayerShop(playerId, currentItems, discounts, lastRefresh);

            if (!currentItems.isEmpty()) {
                data.setCurrentItems(currentItems);
                data.setDiscounts(discounts);
            }
            data.setLastRefresh(lastRefresh[0]);

            playerDataMap.put(playerId, data);
        }
        return playerDataMap.get(playerId);
    }

    public List<ShopItem> getAvailableItems() {
        return new ArrayList<>(availableItems);
    }

    public String addShopItem(ItemStack item, int price, boolean isAuction, int stepPrice, boolean isSapphireCurrency) {
        String id = "item_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? ChatColor.stripColor(item.getItemMeta().getDisplayName())
                : item.getType().toString();

        availableItems.add(new ShopItem(id, name, item, price, isAuction, stepPrice, isSapphireCurrency));
        saveAllData();
        return name;
    }

    public ShopItem getShopItemById(String id) {
        for (ShopItem item : availableItems) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    public boolean removeShopItem(String name) {
        for (int i = 0; i < availableItems.size(); i++) {
            if (availableItems.get(i).getName().equalsIgnoreCase(name)) {
                availableItems.remove(i);
                saveAllData();
                return true;
            }
        }
        return false;
    }

    public void sendItemsList(CommandSender sender) {
        if (availableItems.isEmpty()) {
            sender.sendMessage(ColorUtils.color("&#ff2222Список предметов пуст!"));
            return;
        }

        sender.sendMessage(ColorUtils.color(" Список предметов"));

        for (ShopItem item : availableItems) {
            String type = item.isAuction() ? "&#ff5555[Аукцион]" : "&#55ff55[Магазин]";
            String stepInfo = item.isAuction() ? " &#d5dbdc| Шаг: &#ffffff" + item.getStepPrice() : "";
            String currency = item.isSapphireCurrency() ? "сапфиров" : "монет";

            sender.sendMessage(ColorUtils.color(
                    type + " &#ffffff" + item.getName() +
                            " &#d5dbdc| Цена: &#ffffff" + item.getPrice() + " " + currency + stepInfo
            ));
        }
    }

    public void addWarehouseItem(UUID playerId, ItemStack item) {
        databaseManager.addWarehouseItem(playerId, item);
    }

    public List<ItemStack> getWarehouseItems(UUID playerId) {
        return databaseManager.getWarehouseItems(playerId);
    }

    public ItemStack removeWarehouseItem(UUID playerId, int index) {
        return databaseManager.removeWarehouseItem(playerId, index);
    }

    public void addPurchaseStat(UUID playerId, String itemId, int amount) {
        databaseManager.addPurchaseStat(playerId, itemId, amount);
    }

    public Map<String, Integer> getPlayerPurchaseStats(UUID playerId) {
        return databaseManager.getPlayerPurchaseStats(playerId);
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
