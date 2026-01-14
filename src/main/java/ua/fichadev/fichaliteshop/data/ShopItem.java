package ua.fichadev.fichaliteshop.data;

import org.bukkit.inventory.ItemStack;

public class ShopItem {
    private String id;
    private String name;
    private ItemStack itemStack;
    private int price;
    private boolean isAuction;
    private int stepPrice;
    private boolean isSapphireCurrency;

    public ShopItem(String id, String name, ItemStack itemStack, int price, boolean isAuction, int stepPrice, boolean isSapphireCurrency) {
        this.id = id;
        this.name = name;
        this.itemStack = itemStack.clone();
        this.price = price;
        this.isAuction = isAuction;
        this.stepPrice = stepPrice;
        this.isSapphireCurrency = isSapphireCurrency;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public ItemStack getItemStack() {
        return this.itemStack.clone();
    }

    public int getPrice() {
        return this.price;
    }

    public boolean isAuction() {
        return this.isAuction;
    }

    public int getStepPrice() {
        return this.stepPrice;
    }

    public boolean isSapphireCurrency() {
        return this.isSapphireCurrency;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}