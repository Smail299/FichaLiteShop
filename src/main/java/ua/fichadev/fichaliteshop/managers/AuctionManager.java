package ua.fichadev.fichaliteshop.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ua.fichadev.fichaliteshop.FichaLiteShop;
import ua.fichadev.fichaliteshop.data.AuctionData;
import ua.fichadev.fichaliteshop.data.Bid;
import ua.fichadev.fichaliteshop.data.ShopItem;
import ua.fichadev.fichaliteshop.utils.ColorUtils;
import ua.fichadev.fichaliteshop.utils.MessageUtils;

public class AuctionManager {
    private final FichaLiteShop plugin;
    private AuctionData currentAuction;
    private Integer lastBidSlot;

    public AuctionManager(FichaLiteShop plugin) {
        this.plugin = plugin;
        this.lastBidSlot = null;
        generateNewAuction();
    }

    public void startAuctionCheckTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (currentAuction != null && currentAuction.isExpired()) {
                endAuction();
            }
        }, 20L, 20L);
    }

    private void generateNewAuction() {
        List<ShopItem> auctionItems = new ArrayList<>();

        for (ShopItem item : plugin.getDataManager().getAvailableItems()) {
            if (item.isAuction()) {
                auctionItems.add(item);
            }
        }

        if (auctionItems.isEmpty()) return;

        ShopItem selected = auctionItems.get(ThreadLocalRandom.current().nextInt(auctionItems.size()));
        int durationHours = plugin.getConfig().getInt("auction_duration_hours");
        long durationMillis = (long) durationHours * 60 * 60 * 1000L;

        int stepPrice = selected.getStepPrice() > 0 ? selected.getStepPrice() : Math.max(1, selected.getPrice() / 1000);

        currentAuction = new AuctionData(
                selected.getId(),
                selected.getItemStack(),
                selected.getPrice(),
                stepPrice,
                durationMillis,
                selected.isSapphireCurrency()
        );

        lastBidSlot = null;

        if (plugin.getConfig().getBoolean("notify_auction_start", true)) {
            Bukkit.broadcastMessage(MessageUtils.getMessage(plugin, "auction_started"));
        }
    }

    public AuctionData getCurrentAuction() {
        return currentAuction;
    }

    public List<Bid> getBidsForCurrentAuction() {
        return currentAuction == null ? new ArrayList<>() : currentAuction.getBids();
    }

    public Integer getLastBidSlot() {
        return lastBidSlot;
    }

    public boolean placeBid(Player player, int amount, int clickedSlot) {
        if (currentAuction == null) return false;

        if (amount <= currentAuction.getCurrentPrice()) {
            player.sendMessage(MessageUtils.getMessage(plugin, "bid_must_be_higher"));
            return false;
        }

        UUID previousBidder = currentAuction.getCurrentBidder();
        int previousAmount = currentAuction.getCurrentPrice();
        int amountToPay = amount;

        if (previousBidder != null && previousBidder.equals(player.getUniqueId())) {
            amountToPay = amount - previousAmount;
        }

        if (!processPayment(player, amountToPay)) {
            return false;
        }

        refundPreviousBidder(previousBidder, previousAmount, player);

        currentAuction.setCurrentBidder(player.getUniqueId());
        currentAuction.setCurrentPrice(amount);
        currentAuction.addBid(new Bid(player.getUniqueId(), player.getName(), amount));

        lastBidSlot = clickedSlot == 26 ? 18 : clickedSlot;

        extendAuctionIfNeeded();

        String message = MessageUtils.getMessageWithCurrency(plugin, "bid_placed", amount, currentAuction.isSapphireCurrency());
        player.sendMessage(ColorUtils.color(message));

        return true;
    }

    private boolean processPayment(Player player, int amount) {
        if (currentAuction.isSapphireCurrency()) {
            int points = plugin.getPlayerPointsAPI().look(player.getUniqueId());
            if (points < amount) {
                player.sendMessage(MessageUtils.getMessage(plugin, "not_enough_sapphires", amount));
                return false;
            }
            plugin.getPlayerPointsAPI().take(player.getUniqueId(), amount);
        } else {
            if (!plugin.getEconomy().has(player, amount)) {
                player.sendMessage(MessageUtils.getMessage(plugin, "not_enough_coins", amount));
                return false;
            }
            plugin.getEconomy().withdrawPlayer(player, amount);
        }
        return true;
    }

    private void refundPreviousBidder(UUID previousBidder, int previousAmount, Player currentPlayer) {
        if (previousBidder == null || previousBidder.equals(currentPlayer.getUniqueId())) return;

        Player prevPlayer = Bukkit.getPlayer(previousBidder);

        if (currentAuction.isSapphireCurrency()) {
            plugin.getPlayerPointsAPI().give(previousBidder, previousAmount);
            if (prevPlayer != null && prevPlayer.isOnline() && plugin.getConfig().getBoolean("notify_bid_outbid", true)) {
                String message = MessageUtils.getMessageWithCurrency(plugin, "bid_outbid", previousAmount, true);
                prevPlayer.sendMessage(ColorUtils.color(message));
            }
        } else {
            plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(previousBidder), previousAmount);
            if (prevPlayer != null && prevPlayer.isOnline() && plugin.getConfig().getBoolean("notify_bid_outbid", true)) {
                String message = MessageUtils.getMessageWithCurrency(plugin, "bid_outbid", previousAmount, false);
                prevPlayer.sendMessage(ColorUtils.color(message));
            }
        }
    }

    private void extendAuctionIfNeeded() {
        long remaining = currentAuction.getRemainingTime();
        int minTime = plugin.getConfig().getInt("auction_min_time_for_extension") * 1000;
        int extensionTime = plugin.getConfig().getInt("auction_time_extension_seconds") * 1000;

        if (remaining < minTime) {
            currentAuction.setEndTime(System.currentTimeMillis() + extensionTime);
        }
    }

    private void endAuction() {
        if (currentAuction == null) return;

        UUID winner = currentAuction.getCurrentBidder();

        if (winner != null) {
            ItemStack item = currentAuction.getItemStack();
            plugin.getDataManager().addWarehouseItem(winner, item);

            Player winnerPlayer = Bukkit.getPlayer(winner);
            if (winnerPlayer != null && winnerPlayer.isOnline()) {
                winnerPlayer.sendMessage(MessageUtils.getMessage(plugin, "auction_won"));
                winnerPlayer.sendMessage(MessageUtils.getMessage(plugin, "open_warehouse"));
            }

            if (plugin.getConfig().getBoolean("notify_auction_end", true)) {
                String message = MessageUtils.getMessage(plugin, "auction_winner");
                Bukkit.broadcastMessage(message + (winnerPlayer != null ? winnerPlayer.getName() : ""));
            }
        } else if (plugin.getConfig().getBoolean("notify_auction_end", true)) {
            Bukkit.broadcastMessage(MessageUtils.getMessage(plugin, "auction_no_bids"));
        }

        generateNewAuction();
    }
}