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
import ua.fichadev.fichaliteshop.utils.ColorUtils;

public class DataManager {
    private final FichaLiteShop plugin;
    private final Map<UUID, PlayerShopData> playerDataMap;
    private final List<ShopItem> availableItems;
    private final Map<UUID, List<ItemStack>> warehouseItems;
    private final Map<UUID, Map<String, Integer>> playerPurchaseStats;
    private File dataFile;
    private FileConfiguration dataConfig;

    public DataManager(FichaLiteShop plugin) {
        this.plugin = plugin;
        this.playerDataMap = new HashMap<>();
        this.availableItems = new ArrayList<>();
        this.warehouseItems = new HashMap<>();
        this.playerPurchaseStats = new HashMap<>();
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
        loadWarehouse();
        loadPurchaseStats();
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

    private void loadWarehouse() {
        ConfigurationSection warehouseSection = dataConfig.getConfigurationSection("warehouse");
        if (warehouseSection == null) return;

        for (String key : warehouseSection.getKeys(false)) {
            UUID playerId = UUID.fromString(key);
            List<ItemStack> items = new ArrayList<>();

            if (dataConfig.isList("warehouse." + key)) {
                List<?> itemsList = dataConfig.getList("warehouse." + key);
                for (Object obj : itemsList) {
                    if (obj instanceof ItemStack) {
                        items.add((ItemStack) obj);
                    }
                }
            }

            if (!items.isEmpty()) {
                warehouseItems.put(playerId, items);
            }
        }
    }

    private void loadPurchaseStats() {
        ConfigurationSection statsSection = dataConfig.getConfigurationSection("purchase_stats");
        if (statsSection == null) return;

        for (String key : statsSection.getKeys(false)) {
            UUID playerId = UUID.fromString(key);
            Map<String, Integer> stats = new HashMap<>();

            ConfigurationSection playerStats = dataConfig.getConfigurationSection("purchase_stats." + key);
            if (playerStats != null) {
                for (String itemId : playerStats.getKeys(false)) {
                    int amount = dataConfig.getInt("purchase_stats." + key + "." + itemId);
                    stats.put(itemId, amount);
                }
            }

            playerPurchaseStats.put(playerId, stats);
        }
    }

    public void saveAllData() {
        saveItems();
        saveWarehouse();
        savePurchaseStats();

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
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

    private void saveWarehouse() {
        dataConfig.set("warehouse", null);

        for (Map.Entry<UUID, List<ItemStack>> entry : warehouseItems.entrySet()) {
            dataConfig.set("warehouse." + entry.getKey().toString(), entry.getValue());
        }
    }

    private void savePurchaseStats() {
        dataConfig.set("purchase_stats", null);

        for (Map.Entry<UUID, Map<String, Integer>> entry : playerPurchaseStats.entrySet()) {
            for (Map.Entry<String, Integer> stat : entry.getValue().entrySet()) {
                dataConfig.set("purchase_stats." + entry.getKey().toString() + "." + stat.getKey(), stat.getValue());
            }
        }
    }

    public PlayerShopData getPlayerData(UUID playerId) {
        return playerDataMap.computeIfAbsent(playerId, PlayerShopData::new);
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
        List<ItemStack> items = warehouseItems.getOrDefault(playerId, new ArrayList<>());
        items.add(item);
        warehouseItems.put(playerId, items);
        saveAllData();
    }

    public List<ItemStack> getWarehouseItems(UUID playerId) {
        return new ArrayList<>(warehouseItems.getOrDefault(playerId, new ArrayList<>()));
    }

    public ItemStack removeWarehouseItem(UUID playerId, int index) {
        List<ItemStack> items = warehouseItems.get(playerId);

        if (items == null || index < 0 || index >= items.size()) {
            return null;
        }

        ItemStack removed = items.remove(index);
        saveAllData();
        return removed;
    }

    public void addPurchaseStat(UUID playerId, String itemId, int amount) {
        Map<String, Integer> stats = playerPurchaseStats.getOrDefault(playerId, new HashMap<>());
        stats.put(itemId, stats.getOrDefault(itemId, 0) + amount);
        playerPurchaseStats.put(playerId, stats);
        saveAllData();
    }

    public Map<String, Integer> getPlayerPurchaseStats(UUID playerId) {
        return new HashMap<>(playerPurchaseStats.getOrDefault(playerId, new HashMap<>()));
    }
}