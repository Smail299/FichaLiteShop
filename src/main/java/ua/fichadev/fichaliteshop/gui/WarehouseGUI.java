package ua.fichadev.fichaliteshop.gui;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.fichadev.fichaliteshop.FichaLiteShop;
import ua.fichadev.fichaliteshop.utils.ColorUtils;

public class WarehouseGUI implements InventoryHolder {
    private static final int ITEMS_PER_PAGE = 36;

    private final FichaLiteShop plugin;
    private final Player player;
    private Inventory inventory;
    private final int page;

    public WarehouseGUI(FichaLiteShop plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = page;
    }

    public void open() {
        inventory = Bukkit.createInventory(this, 54, ColorUtils.color("Склад купленных предметов"));
        updateInventory();
        player.openInventory(inventory);
    }

    private void updateInventory() {
        inventory.clear();

        setupBorders();
        setupWarehouseItems();
        setupNavigationButtons();
    }

    private void setupBorders() {
        ItemStack gray = createGlassPane();

        for (int i = 0; i <= 8; i++) {
            inventory.setItem(i, gray);
        }

        int[] bottomSlots = {45, 46, 47, 49, 51, 52, 53};
        for (int slot : bottomSlots) {
            inventory.setItem(slot, gray);
        }
    }

    private ItemStack createGlassPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(ColorUtils.color("&r"));
        pane.setItemMeta(meta);
        return pane;
    }

    private void setupWarehouseItems() {
        List<ItemStack> warehouseItems = plugin.getDataManager().getWarehouseItems(player.getUniqueId());

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, warehouseItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            ItemStack item = warehouseItems.get(i);
            int slot = 9 + (i - startIndex);
            inventory.setItem(slot, item);
        }
    }

    private void setupNavigationButtons() {
        ItemStack prevButton = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prevButton.getItemMeta();
        prevMeta.setDisplayName(ColorUtils.color(" &r&#00c7ff◀ Предыдущая страница"));
        prevButton.setItemMeta(prevMeta);
        inventory.setItem(48, prevButton);

        ItemStack nextButton = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta nextMeta = nextButton.getItemMeta();
        nextMeta.setDisplayName(ColorUtils.color(" &r&#00c7ffСледующая страница ▶"));
        nextButton.setItemMeta(nextMeta);
        inventory.setItem(50, nextButton);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public int getPage() {
        return page;
    }
}