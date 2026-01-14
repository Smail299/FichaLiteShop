package ua.fichadev.fichaliteshop.listeners;

import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ua.fichadev.fichaliteshop.FichaLiteShop;
import ua.fichadev.fichaliteshop.data.AuctionData;
import ua.fichadev.fichaliteshop.data.PlayerShopData;
import ua.fichadev.fichaliteshop.data.ShopItem;
import ua.fichadev.fichaliteshop.gui.AuctionGUI;
import ua.fichadev.fichaliteshop.gui.ShopGUI;
import ua.fichadev.fichaliteshop.gui.WarehouseGUI;
import ua.fichadev.fichaliteshop.utils.ColorUtils;
import ua.fichadev.fichaliteshop.utils.MessageUtils;
import ua.fichadev.fichaliteshop.utils.TimeUtils;

public class InventoryListener implements Listener {
    private final FichaLiteShop plugin;
    private static final int[] SHOP_ITEM_SLOTS = {11, 13, 15, 20, 21, 23, 24};

    public InventoryListener(FichaLiteShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Player player = (Player) e.getWhoClicked();
        Inventory inv = e.getView().getTopInventory();

        if (!(inv.getHolder() instanceof ShopGUI || inv.getHolder() instanceof AuctionGUI || inv.getHolder() instanceof WarehouseGUI)) {
            return;
        }

        e.setCancelled(true);
        if (e.getClickedInventory() == null) return;

        if (inv.getHolder() instanceof ShopGUI) {
            handleShopClick(player, e, (ShopGUI) inv.getHolder());
        } else if (inv.getHolder() instanceof AuctionGUI) {
            handleAuctionClick(player, e, (AuctionGUI) inv.getHolder());
        } else if (inv.getHolder() instanceof WarehouseGUI) {
            handleWarehouseClick(player, e, (WarehouseGUI) inv.getHolder());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Inventory inv = e.getView().getTopInventory();
        if (inv.getHolder() instanceof ShopGUI || inv.getHolder() instanceof AuctionGUI || inv.getHolder() instanceof WarehouseGUI) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;

        Inventory inv = e.getView().getTopInventory();
        if (inv.getHolder() instanceof ShopGUI) {
            ((ShopGUI) inv.getHolder()).stopUpdateTask();
        } else if (inv.getHolder() instanceof AuctionGUI) {
            ((AuctionGUI) inv.getHolder()).stopUpdateTask();
        }
    }

    private void handleShopClick(Player player, InventoryClickEvent e, ShopGUI shopGUI) {
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = e.getSlot();
        int itemIndex = getItemSlotIndex(slot);

        if (itemIndex != -1) {
            processPurchase(player, shopGUI, itemIndex);
        } else {
            handleSpecialSlots(player, shopGUI, slot);
        }
    }

    private int getItemSlotIndex(int slot) {
        for (int i = 0; i < SHOP_ITEM_SLOTS.length; i++) {
            if (SHOP_ITEM_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    private void processPurchase(Player player, ShopGUI shopGUI, int itemIndex) {
        PlayerShopData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        List<String> items = data.getCurrentItems();

        if (itemIndex >= items.size()) return;

        String itemId = items.get(itemIndex);
        ShopItem shopItem = plugin.getDataManager().getShopItemById(itemId);

        if (shopItem == null) return;

        int finalPrice = plugin.getShopManager().getDiscountedPrice(player.getUniqueId(), itemId, shopItem.getPrice());
        int points = plugin.getPlayerPointsAPI().look(player.getUniqueId());

        if (points < finalPrice) {
            player.sendMessage(MessageUtils.getMessage(plugin, "not_enough_sapphires", finalPrice));
            return;
        }

        plugin.getPlayerPointsAPI().take(player.getUniqueId(), finalPrice);
        ItemStack item = shopItem.getItemStack();
        plugin.getDataManager().addWarehouseItem(player.getUniqueId(), item);
        plugin.getDataManager().addPurchaseStat(player.getUniqueId(), itemId, finalPrice);

        String message = MessageUtils.replacePlaceholders(plugin, plugin.getConfig().getString("messages.purchase_success", ""))
                .replace("%amount%", TimeUtils.formatNumber(finalPrice));
        player.sendMessage(ColorUtils.color(message));
        player.sendMessage(MessageUtils.getMessage(plugin, "purchase_sent_warehouse"));

        shopGUI.incrementPurchaseCount(itemId);
    }

    private void handleSpecialSlots(Player player, ShopGUI shopGUI, int slot) {
        switch (slot) {
            case 40:
                shopGUI.stopUpdateTask();
                player.closeInventory();
                new AuctionGUI(plugin, player).open();
                break;
            case 47:
                player.closeInventory();
                player.performCommand("dm open kitshop");
                break;
            case 49:
                shopGUI.stopUpdateTask();
                player.closeInventory();
                new WarehouseGUI(plugin, player, 0).open();
                break;
            case 51:
                player.closeInventory();
                player.performCommand("booster");
                break;
        }
    }

    private void handleAuctionClick(Player player, InventoryClickEvent e, AuctionGUI auctionGUI) {
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = e.getSlot();

        if (slot == 45) {
            auctionGUI.stopUpdateTask();
            player.closeInventory();
            new ShopGUI(plugin, player).open();
            return;
        }

        if (clicked.getType() != Material.BLACK_STAINED_GLASS_PANE) return;

        Map<Integer, Integer> availableBids = auctionGUI.getAvailableBids();
        if (!availableBids.containsKey(slot)) return;

        int bidAmount = availableBids.get(slot);
        AuctionData auction = auctionGUI.getAuction();

        if (auction == null) {
            player.sendMessage(MessageUtils.getMessage(plugin, "auction_not_available"));
            player.closeInventory();
            return;
        }

        int playerCurrentBid = 0;
        if (auction.getCurrentBidder() != null && auction.getCurrentBidder().equals(player.getUniqueId())) {
            playerCurrentBid = auction.getCurrentPrice();
        }

        int amountToPay = bidAmount - playerCurrentBid;

        if (auction.isSapphireCurrency()) {
            int points = plugin.getPlayerPointsAPI().look(player.getUniqueId());
            if (points < amountToPay) {
                player.sendMessage(MessageUtils.getMessage(plugin, "not_enough_sapphires", amountToPay));
                return;
            }
        } else {
            if (!plugin.getEconomy().has(player, amountToPay)) {
                player.sendMessage(MessageUtils.getMessage(plugin, "not_enough_coins", amountToPay));
                return;
            }
        }

        plugin.getAuctionManager().placeBid(player, bidAmount, slot);
    }

    private void handleWarehouseClick(Player player, InventoryClickEvent e, WarehouseGUI warehouseGUI) {
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = e.getSlot();

        if (slot == 48) {
            if (warehouseGUI.getPage() > 0) {
                player.closeInventory();
                new WarehouseGUI(plugin, player, warehouseGUI.getPage() - 1).open();
            }
        } else if (slot == 50) {
            List<ItemStack> warehouseItems = plugin.getDataManager().getWarehouseItems(player.getUniqueId());
            int totalPages = Math.max(1, (int) Math.ceil((double) warehouseItems.size() / 36.0));
            int nextPage = warehouseGUI.getPage() + 1;

            if (nextPage < totalPages) {
                player.closeInventory();
                new WarehouseGUI(plugin, player, nextPage).open();
            }
        } else if (slot >= 9 && slot <= 44) {
            int index = warehouseGUI.getPage() * 36 + (slot - 9);
            ItemStack item = plugin.getDataManager().removeWarehouseItem(player.getUniqueId(), index);

            if (item != null) {
                if (player.getInventory().firstEmpty() != -1) {
                    player.getInventory().addItem(item);
                    player.sendMessage(MessageUtils.getMessage(plugin, "item_received"));
                } else {
                    player.getWorld().dropItem(player.getLocation(), item);
                    player.sendMessage(MessageUtils.getMessage(plugin, "item_dropped"));
                }

                player.closeInventory();
                new WarehouseGUI(plugin, player, warehouseGUI.getPage()).open();
            }
        }
    }
}