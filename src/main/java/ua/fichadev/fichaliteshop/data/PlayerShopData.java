package ua.fichadev.fichaliteshop.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerShopData {
    private UUID playerId;
    private List<String> currentItems;
    private Map<String, Double> discounts;
    private long lastRefresh;

    public PlayerShopData(UUID playerId) {
        this.playerId = playerId;
        this.currentItems = new ArrayList();
        this.discounts = new HashMap();
        this.lastRefresh = System.currentTimeMillis();
    }

    public UUID getPlayerId() {
        return this.playerId;
    }

    public List<String> getCurrentItems() {
        return this.currentItems;
    }

    public void setCurrentItems(List<String> items) {
        this.currentItems = items;
    }

    public Map<String, Double> getDiscounts() {
        return this.discounts;
    }

    public void setDiscounts(Map<String, Double> discounts) {
        this.discounts = discounts;
    }

    public double getDiscount(String itemId) {
        return (Double)this.discounts.getOrDefault(itemId, 0.0D);
    }

    public void setDiscount(String itemId, double discount) {
        this.discounts.put(itemId, discount);
    }

    public long getLastRefresh() {
        return this.lastRefresh;
    }

    public void setLastRefresh(long lastRefresh) {
        this.lastRefresh = lastRefresh;
    }
}