package ua.fichadev.fichaliteshop.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

public class ColorUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String color(String message) {
        if (message == null) {
            return "";
        } else {
            Matcher matcher = HEX_PATTERN.matcher(message);
            StringBuffer buffer = new StringBuffer(message.length());

            while(matcher.find()) {
                String hexCode = matcher.group(1);
                String replacement = ChatColor.of("#" + hexCode).toString();
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            }

            matcher.appendTail(buffer);
            return org.bukkit.ChatColor.translateAlternateColorCodes('&', buffer.toString());
        }
    }
}