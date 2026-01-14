package ua.fichadev.fichaliteshop.commands;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ua.fichadev.fichaliteshop.FichaLiteShop;
import ua.fichadev.fichaliteshop.gui.ShopGUI;
import ua.fichadev.fichaliteshop.gui.WarehouseGUI;

public class ShopCommand implements CommandExecutor, TabCompleter {
    private final FichaLiteShop plugin;

    public ShopCommand(FichaLiteShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда для игроков");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("chest")) {
            new WarehouseGUI(plugin, player, 0).open();
            return true;
        }

        plugin.getShopManager().refreshPlayerShop(player.getUniqueId(), false);
        new ShopGUI(plugin, player).open();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("chest");
        }
        return completions;
    }
}