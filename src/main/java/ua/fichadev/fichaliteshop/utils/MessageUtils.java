package ua.fichadev.fichaliteshop.utils;

import ua.fichadev.fichaliteshop.FichaLiteShop;

public class MessageUtils {

    public static String replacePlaceholders(FichaLiteShop plugin, String message) {
        if (message == null) return "";

        return message
                .replace("%getCoinsCurrencyName%", plugin.getCoinsCurrencyName())
                .replace("%getSapphiresCurrencyName%", plugin.getSapphiresCurrencyName());
    }

    public static String getMessage(FichaLiteShop plugin, String key) {
        String message = plugin.getConfig().getString("messages." + key, "");
        return ColorUtils.color(replacePlaceholders(plugin, message));
    }

    public static String getMessage(FichaLiteShop plugin, String key, int requiredAmount) {
        String message = plugin.getConfig().getString("messages." + key, "");
        message = replacePlaceholders(plugin, message);
        message = message.replace("%required%", TimeUtils.formatNumber(requiredAmount));
        return ColorUtils.color(message);
    }

    public static String getMessageWithCurrency(FichaLiteShop plugin, String key, int amount, boolean isSapphire) {
        String message = plugin.getConfig().getString("messages." + key, "");

        String currencyColor = isSapphire ? "&#FB0FD4" : "&#fca000";
        String currencyName = isSapphire
                ? plugin.getSapphiresCurrencyName()
                : plugin.getCoinsCurrencyName();

        String coloredAmount = currencyColor + TimeUtils.formatNumber(amount);
        String coloredCurrency = currencyColor + currencyName;

        message = message
                .replace("%amount%", coloredAmount)
                .replace("%getCoinsCurrencyName%", coloredCurrency)
                .replace("%getSapphiresCurrencyName%", currencyColor + plugin.getSapphiresCurrencyName());

        return ColorUtils.color(message);
    }
}