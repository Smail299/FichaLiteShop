package ua.fichadev.fichaliteshop.gui;

import java.text.SimpleDateFormat;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import ua.fichadev.fichaliteshop.FichaLiteShop;
import ua.fichadev.fichaliteshop.data.AuctionData;
import ua.fichadev.fichaliteshop.data.PlayerShopData;
import ua.fichadev.fichaliteshop.data.ShopItem;
import ua.fichadev.fichaliteshop.utils.ColorUtils;
import ua.fichadev.fichaliteshop.utils.TimeUtils;

public class ShopGUI implements InventoryHolder {
    private static final int[] SHOP_SLOTS = {11, 13, 15, 20, 21, 23, 24};

    private final FichaLiteShop plugin;
    private final Player player;
    private Inventory inventory;
    private BukkitTask updateTask;
    private final Map<String, Integer> purchaseCount;
    private final SimpleDateFormat dateFormat;

    public ShopGUI(FichaLiteShop plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.purchaseCount = new HashMap<>();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
    }

    public void open() {
        inventory = Bukkit.createInventory(this, 54, ColorUtils.color("Премиум-магазин"));
        updateInventory();
        startUpdateTask();
        player.openInventory(inventory);
    }

    private void updateInventory() {
        inventory.clear();

        PlayerShopData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        List<String> items = data.getCurrentItems();

        setupDecorativeItems();
        setupShopItems(data, items);
        setupAuctionItem();
        setupInfoBook();
        setupNavigationButtons();
        setupStatsMap();
    }

    private void setupDecorativeItems() {
        ItemStack gray = createGlassPane(Material.GRAY_STAINED_GLASS_PANE);
        ItemStack orange = createGlassPane(Material.ORANGE_STAINED_GLASS_PANE);

        inventory.setItem(0, gray);
        inventory.setItem(8, gray);

        int[] orangeSlots = {9, 17, 18, 26, 27, 35, 36, 44};
        for (int slot : orangeSlots) {
            inventory.setItem(slot, orange);
        }
    }

    private ItemStack createGlassPane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(ColorUtils.color("&r"));
        pane.setItemMeta(meta);
        return pane;
    }

    private void setupShopItems(PlayerShopData data, List<String> items) {
        for (int i = 0; i < Math.min(items.size(), SHOP_SLOTS.length); i++) {
            String itemId = items.get(i);
            ShopItem shopItem = plugin.getDataManager().getShopItemById(itemId);

            if (shopItem == null) continue;

            ItemStack displayItem = createShopDisplayItem(shopItem, data, itemId);
            inventory.setItem(SHOP_SLOTS[i], displayItem);
        }
    }

    private ItemStack createShopDisplayItem(ShopItem shopItem, PlayerShopData data, String itemId) {
        ItemStack displayItem = shopItem.getItemStack();
        ItemMeta meta = displayItem.getItemMeta();

        int originalPrice = shopItem.getPrice();
        double discount = data.getDiscounts().getOrDefault(itemId, 0.0);
        int finalPrice = plugin.getShopManager().getDiscountedPrice(player.getUniqueId(), itemId, originalPrice);
        int bought = purchaseCount.getOrDefault(itemId, 0);

        String currencyName = plugin.getConfig().getString("currencies.sapphires", "сапфиров ☀");

        List<String> lore = new ArrayList<>();
        if (meta != null && meta.hasLore()) {
            lore.addAll(meta.getLore());
        }

        lore.add(ColorUtils.color("&r"));
        lore.add(ColorUtils.color(" &r&l&#ff7000&n▍&r&#d5dbdc До обновления: &r&#ff7000" +
                TimeUtils.formatTime(getTimeUntilRefresh(data))));
        lore.add(ColorUtils.color(" &r&l&#ff7000&n▍&r&#d5dbdc Скидка: &r&#ffc900" +
                String.format("%.1f", discount) + "%"));
        lore.add(ColorUtils.color("&r &r&l&#ff7000▍&r&#d5dbdc Куплено: &r&#ff7000" + bought + " шт."));
        lore.add(ColorUtils.color("&r"));
        lore.add(ColorUtils.color("&r &r&#ff7000▶ &r&#ffffffКупить за &r&#aaaaaa&m" +
                TimeUtils.formatNumber(originalPrice) + "&r &#FB0FD4" +
                TimeUtils.formatNumber(finalPrice) + " " + currencyName));

        if (meta != null) {
            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }

        return displayItem;
    }

    private void setupAuctionItem() {
        AuctionData auction = plugin.getAuctionManager().getCurrentAuction();
        if (auction == null) return;

        ItemStack auctionItem = auction.getItemStack();
        ItemMeta meta = auctionItem.getItemMeta();

        String currencyName = auction.isSapphireCurrency()
                ? plugin.getConfig().getString("currencies.sapphires", "сапфиров ☀")
                : plugin.getConfig().getString("currencies.coins", "монет ¤");
        String currencyColor = auction.isSapphireCurrency() ? "&#FB0FD4" : "&#fca000";

        List<String> lore = new ArrayList<>();
        if (meta != null && meta.hasLore()) {
            lore.addAll(meta.getLore());
        }

        lore.add(ColorUtils.color("&r"));
        lore.add(ColorUtils.color(" &r&l&#ff7000&n▍&r&#ffffff До конца: &r&#ff7000" +
                TimeUtils.formatTime(auction.getRemainingTime())));
        lore.add(ColorUtils.color(" &r&l&#ff7000▍&r&#ffffff Текущая цена: " + currencyColor +
                TimeUtils.formatNumber(auction.getCurrentPrice()) + " " + currencyName));
        lore.add(ColorUtils.color("&r"));

        if (auction.getCurrentBidder() != null) {
            String bidderName = Bukkit.getOfflinePlayer(auction.getCurrentBidder()).getName();
            lore.add(ColorUtils.color(" &r&#ff7000● &r&#ffffffЛидер торгов: &r&#00bdff" + bidderName));
            lore.add(ColorUtils.color("&r"));
        }

        lore.add(ColorUtils.color(" &r&#ff7000▶ &r&#ffffffНажмите, чтобы сделать ставку"));

        if (meta != null) {
            meta.setLore(lore);
            auctionItem.setItemMeta(meta);
        }

        inventory.setItem(40, auctionItem);
    }

    private void setupInfoBook() {
        ItemStack infoBook = new ItemStack(Material.BOOK);
        ItemMeta meta = infoBook.getItemMeta();
        meta.setDisplayName(ColorUtils.color(" &r&#00c7ffПомощь по премиум-магазину"));

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.color("&r"));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍"));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍&r&#00bdff Когда обновится товар?"));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍"));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍&r&#e7e7e7 Все товары в премиум-магазине"));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍&r&#e7e7e7 обновляются раз в некоторое время."));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍&r&#e7e7e7 Узнать, когда обновится товар, вы"));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍&r&#e7e7e7 Можете в описании предмета."));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍"));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍&r&#00bdff Ограниченное число предметов"));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍"));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍&r&#e7e7e7 Некоторые товары можно купить только"));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍&r&#e7e7e7 ограниченное число раз, внимательно"));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍&r&#e7e7e7 читайте описание предмета."));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍"));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍&r&#00bdff Склад купленных товаров"));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍"));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍&r&#e7e7e7 Чтобы открыть склад купленных"));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍&r&#e7e7e7 предметов, пропишите команду:"));
        lore.add(ColorUtils.color(" &r&#00bdff&n▍&r&#00bdff /shop chest"));
        lore.add(ColorUtils.color(" &r&#00bdff▍"));
        lore.add(ColorUtils.color("&r"));

        meta.setLore(lore);
        infoBook.setItemMeta(meta);
        inventory.setItem(45, infoBook);
    }

    private void setupNavigationButtons() {
        inventory.setItem(47, createNavigationButton(Material.DIAMOND_CHESTPLATE,
                " &r&#ff7000❏ Наборы ❏", "kits"));
        inventory.setItem(49, createNavigationButton(Material.CHEST,
                " &r&#00c7ff❏ Склад купленных товаров ❏", "warehouse"));
        inventory.setItem(51, createNavigationButton(Material.POTION,
                " &r&#ff7000❏ Бустеры ❏", "boosters"));
    }

    private ItemStack createNavigationButton(Material material, String title, String type) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(ColorUtils.color(title));

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.color("&r"));

        switch (type) {
            case "kits":
                lore.add(ColorUtils.color(" &r&l&#ff7000&n▍&r&#ffffff В этом разделе &r&#ff7000можно купить"));
                lore.add(ColorUtils.color(" &r&l&#ff7000&n▍&r&#ffffff &r&#ff7000наборы топовых привилегий&r&#ffffff или"));
                lore.add(ColorUtils.color(" &r&l&#ff7000▍&r&#ffffff универсальные киты для PvP."));
                break;
            case "warehouse":
                lore.add(ColorUtils.color(" &r&l&#00c7ff&n▍&r&#ffffff В этот разделе можно &r&#00c7ffполучить"));
                lore.add(ColorUtils.color(" &r&l&#00c7ff▍&r&#00c7ff предметы &r&#ffffffс премиум-аукциона."));
                break;
            case "boosters":
                lore.add(ColorUtils.color(" &r&l&#ff7000&n▍&r&#ffffff В этом разделе &r&#ff7000можно купить"));
                lore.add(ColorUtils.color(" &r&l&#ff7000&n▍&r&#ffffff &r&#ff7000постоянные бустеры &r&#ffffffдля улучшения"));
                lore.add(ColorUtils.color(" &r&l&#ff7000▍&r&#ffffff ваших показателей в игре."));
                break;
        }

        lore.add(ColorUtils.color("&r"));
        lore.add(ColorUtils.color(" &r&#ff7000▶ &r&#ffffffНажмите, чтобы открыть"));

        meta.setLore(lore);
        button.setItemMeta(meta);
        return button;
    }

    private void setupStatsMap() {
        ItemStack statsMap = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = statsMap.getItemMeta();

        String currentDate = dateFormat.format(new Date());
        meta.setDisplayName(ColorUtils.color(" &r&#00c7ffВаши траты &r&#555555| &r&#ffffff" + currentDate));

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.color("&r"));
        lore.add(ColorUtils.color("&r"));

        Map<String, Integer> purchaseStats = plugin.getDataManager().getPlayerPurchaseStats(player.getUniqueId());

        if (purchaseStats.isEmpty()) {
            lore.add(ColorUtils.color("&r &#ff0e00✘ &r&#ffffffВы ничего &#ff0e00не покупали"));
            lore.add(ColorUtils.color("&r    &r&#ffffffв премиум-магазине."));
        } else {
            int totalSpent = purchaseStats.values().stream().mapToInt(Integer::intValue).sum();
            lore.add(ColorUtils.color(" &r&#00c7ff✓ &r&#ffffffВсего потрачено:"));
            lore.add(ColorUtils.color("    &r&#FB0FD4" + TimeUtils.formatNumber(totalSpent) + " сапфиров ☀"));
        }

        lore.add(ColorUtils.color("&r"));
        lore.add(ColorUtils.color("&r"));

        meta.setLore(lore);
        statsMap.setItemMeta(meta);
        inventory.setItem(53, statsMap);
    }

    private void startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateInventory, 20L, 20L);
    }

    public void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    public void incrementPurchaseCount(String itemId) {
        purchaseCount.put(itemId, purchaseCount.getOrDefault(itemId, 0) + 1);
    }

    private long getTimeUntilRefresh(PlayerShopData data) {
        int interval = plugin.getConfig().getBoolean("early_wipe_mode")
                ? plugin.getConfig().getInt("early_wipe_interval_minutes")
                : plugin.getConfig().getInt("refresh_interval_minutes");

        long nextRefresh = data.getLastRefresh() + (long) interval * 60 * 1000L;
        long timeLeft = nextRefresh - System.currentTimeMillis();
        return Math.max(0L, timeLeft);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}