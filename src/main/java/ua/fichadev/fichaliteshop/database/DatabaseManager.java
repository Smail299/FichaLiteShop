package ua.fichadev.fichaliteshop.database;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DatabaseManager {

    void savePlayerShop(UUID playerId, List<String> currentItems, Map<String, Double> discounts, long lastRefresh);

    void loadPlayerShop(UUID playerId, List<String> currentItems, Map<String, Double> discounts, long[] lastRefresh);

    void addWarehouseItem(UUID playerId, ItemStack item);

    List<ItemStack> getWarehouseItems(UUID playerId);

    ItemStack removeWarehouseItem(UUID playerId, int index);

    void addPurchaseStat(UUID playerId, String itemId, int amount);

    Map<String, Integer> getPlayerPurchaseStats(UUID playerId);

    void close();
}
