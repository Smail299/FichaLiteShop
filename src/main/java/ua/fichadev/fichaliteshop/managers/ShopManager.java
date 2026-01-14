package ua.fichadev.fichaliteshop.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ua.fichadev.fichaliteshop.FichaLiteShop;
import ua.fichadev.fichaliteshop.data.PlayerShopData;
import ua.fichadev.fichaliteshop.data.ShopItem;
import ua.fichadev.fichaliteshop.utils.MessageUtils;

public class ShopManager {
    private final FichaLiteShop plugin;

    public ShopManager(FichaLiteShop plugin) {
        this.plugin = plugin;
    }

    public void startRefreshTask() {
        int interval = getRefreshIntervalMinutes();
        long ticks = (long) interval * 60 * 20L;

        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAllPlayers, ticks, ticks);
    }

    public void refreshAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshPlayerShop(player.getUniqueId(), true);
        }
    }

    public void refreshPlayerShop(UUID playerId, boolean force) {
        PlayerShopData data = plugin.getDataManager().getPlayerData(playerId);

        if (!force) {
            int interval = getRefreshIntervalMinutes();
            long timeSinceRefresh = System.currentTimeMillis() - data.getLastRefresh();

            if (timeSinceRefresh < (long) interval * 60 * 1000L) {
                return;
            }
        }

        List<ShopItem> nonAuctionItems = getNonAuctionItems();
        int itemsPerPlayer = getItemsPerPlayer();

        List<String> selectedItems = new ArrayList<>();
        Map<String, Double> discounts = new HashMap<>();
        Random random = ThreadLocalRandom.current();

        Collections.shuffle(nonAuctionItems, random);

        double minDiscount = getMinDiscountPercent();
        double maxDiscount = getMaxDiscountPercent();

        for (int i = 0; i < Math.min(itemsPerPlayer, nonAuctionItems.size()); i++) {
            ShopItem item = nonAuctionItems.get(i);
            selectedItems.add(item.getId());

            double discount = minDiscount + random.nextDouble() * (maxDiscount - minDiscount);
            discount = Math.round(discount * 100.0) / 100.0;
            discounts.put(item.getId(), discount);
        }

        data.setCurrentItems(selectedItems);
        data.setDiscounts(discounts);
        data.setLastRefresh(System.currentTimeMillis());

        notifyPlayerIfOnline(playerId);
    }

    private List<ShopItem> getNonAuctionItems() {
        List<ShopItem> nonAuctionItems = new ArrayList<>();

        for (ShopItem item : plugin.getDataManager().getAvailableItems()) {
            if (!item.isAuction()) {
                nonAuctionItems.add(item);
            }
        }

        return nonAuctionItems;
    }

    private void notifyPlayerIfOnline(UUID playerId) {
        if (!plugin.getConfig().getBoolean("notify_new_items", true)) return;

        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.sendMessage(MessageUtils.getMessage(plugin, "shop_items_refreshed"));
        }
    }

    public int getDiscountedPrice(UUID playerId, String itemId, int originalPrice) {
        PlayerShopData data = plugin.getDataManager().getPlayerData(playerId);
        double discount = data.getDiscount(itemId);
        int discountedPrice = originalPrice - (int) (originalPrice * discount / 100.0);
        return Math.max(1, discountedPrice);
    }

    public int getRefreshIntervalMinutes() {
        boolean earlyWipeMode = plugin.getConfig().getBoolean("early_wipe_mode");
        return earlyWipeMode
                ? plugin.getConfig().getInt("early_wipe_interval_minutes")
                : plugin.getConfig().getInt("refresh_interval_minutes");
    }

    public int getItemsPerPlayer() {
        return plugin.getConfig().getInt("items_per_player");
    }

    public double getMinDiscountPercent() {
        return plugin.getConfig().getDouble("discount_min_percent");
    }

    public double getMaxDiscountPercent() {
        return plugin.getConfig().getDouble("discount_max_percent");
    }
}