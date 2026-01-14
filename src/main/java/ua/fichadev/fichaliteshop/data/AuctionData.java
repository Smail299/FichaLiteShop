package ua.fichadev.fichaliteshop.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public class AuctionData {
    private String itemId;
    private ItemStack itemStack;
    private int startPrice;
    private int currentPrice;
    private int stepPrice;
    private long endTime;
    private UUID currentBidder;
    private boolean isSapphireCurrency;
    private List<Bid> bids;

    public AuctionData(String itemId, ItemStack itemStack, int startPrice, int stepPrice, long durationMillis, boolean isSapphireCurrency) {
        this.itemId = itemId;
        this.itemStack = itemStack.clone();
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.stepPrice = stepPrice;
        this.endTime = System.currentTimeMillis() + durationMillis;
        this.currentBidder = null;
        this.isSapphireCurrency = isSapphireCurrency;
        this.bids = new ArrayList();
    }

    public String getItemId() {
        return this.itemId;
    }

    public ItemStack getItemStack() {
        return this.itemStack.clone();
    }

    public int getStartPrice() {
        return this.startPrice;
    }

    public int getCurrentPrice() {
        return this.currentPrice;
    }

    public void setCurrentPrice(int currentPrice) {
        this.currentPrice = currentPrice;
    }

    public int getStepPrice() {
        return this.stepPrice;
    }

    public long getEndTime() {
        return this.endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public UUID getCurrentBidder() {
        return this.currentBidder;
    }

    public void setCurrentBidder(UUID currentBidder) {
        this.currentBidder = currentBidder;
    }

    public boolean isSapphireCurrency() {
        return this.isSapphireCurrency;
    }

    public long getRemainingTime() {
        return Math.max(0L, this.endTime - System.currentTimeMillis());
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= this.endTime;
    }

    public List<Bid> getBids() {
        return new ArrayList(this.bids);
    }

    public void addBid(Bid bid) {
        this.bids.add(bid);
    }

    public void clearBids() {
        this.bids.clear();
    }
}