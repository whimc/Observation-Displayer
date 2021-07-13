package edu.whimc.observationdisplayer.observetemplate.gui;

import edu.whimc.observationdisplayer.ObservationDisplayer;
import edu.whimc.observationdisplayer.observetemplate.TemplateManager;
import edu.whimc.observationdisplayer.observetemplate.models.ObservationTemplate;
import edu.whimc.observationdisplayer.observetemplate.models.ObservationType;
import edu.whimc.observationdisplayer.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class TemplateGui implements Listener {

    private final Map<Integer, ObservationType> templateSlots = new HashMap<>();
    private final Map<ObservationType, Consumer<Player>> templateActions = new HashMap<>();
    private final ObservationDisplayer plugin;
    private final TemplateManager manager;
    private String inventoryName;
    private int inventorySize;
    private ItemStack fillerItem;
    private ItemStack cancelItem;
    private int cancelPosition;
    private Inventory inventory;

    public TemplateGui(ObservationDisplayer plugin, TemplateManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadTemplateInventory();
    }

    private void loadTemplateInventory() {
        this.inventoryName = getString(Path.INVENTORY_NAME);
        this.inventorySize = 9 * getInt(Path.INVENTORY_ROWS);

        this.fillerItem = new ItemStack(Material.matchMaterial(getString(Path.FILLER_ITEM)));
        setName(this.fillerItem, " ");

        this.cancelItem = new ItemStack(Material.matchMaterial(getString(Path.CANCEL_ITEM)));
        this.cancelPosition = getInt(Path.CANCEL_POSITION);
        setName(this.cancelItem, getString(Path.CANCEL_NAME));

        this.inventory = Bukkit.createInventory(null, this.inventorySize, Utils.color(this.inventoryName));

        // Add in filler items
        for (int slot = 0; slot < this.inventory.getSize(); slot++) {
            this.inventory.setItem(slot, this.fillerItem);
        }

        // Add cancel item
        this.inventory.setItem(this.cancelPosition, this.cancelItem);

        // Add template-specific items
        for (ObservationType type : ObservationType.values()) {
            ObservationTemplate template = this.manager.getTemplate(type);

            ItemStack item = new ItemStack(template.getGuiItem());
            setName(item, template.getGuiItemName());
            setLore(item, template.getGuiLore());

            this.templateSlots.put(template.getGuiPosition(), type);
            this.inventory.setItem(template.getGuiPosition(), item);
        }
    }

    public void addConsumer(ObservationType type, Consumer<Player> action) {
        this.templateActions.put(type, action);
    }

    public void openTemplateInventory(Player player) {
        player.openInventory(this.inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory() != this.inventory) {
            return;
        }

        event.setCancelled(true);

        // Only care about clicks in our inventory
        if (event.getClickedInventory() != this.inventory) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();

        // Do nothing if they didn't click anything important
        if (clicked == null || clicked == this.fillerItem) {
            return;
        }

        // Only care if the clicker was a player
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Close the inventory if they click the cancel button
        if (clicked.equals(this.cancelItem)) {
            event.getWhoClicked().closeInventory();
            Utils.msg(player, "Observation canceled!");
            return;
        }

        ObservationType type = this.templateSlots.getOrDefault(event.getSlot(), null);
        if (type == null) {
            return;
        }

        // Close the inventory and execute the action for this template type
        player.closeInventory();
        this.templateActions.getOrDefault(type, p -> { /* no-op */ }).accept(player);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory() == this.inventory) {
            event.setCancelled(true);
        }
    }

    private void setName(ItemStack item, String name) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.color(name));
        item.setItemMeta(meta);
    }

    private void setLore(ItemStack item, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        meta.setLore(lore
                .stream()
                .map(Utils::color)
                .collect(Collectors.toList()));
        item.setItemMeta(meta);
    }

    public String getString(Path path) {
        return this.plugin.getConfig().getString(Path.ROOT.getPath() + path.getPath());
    }

    public int getInt(Path path) {
        return this.plugin.getConfig().getInt(Path.ROOT.getPath() + path.getPath());
    }

    private enum Path {

        ROOT("template-gui."),

        FILLER_ITEM("filler-item"),

        INVENTORY_NAME("inventory-name"),
        INVENTORY_ROWS("rows"),

        CANCEL_ITEM("cancel.item"),
        CANCEL_POSITION("cancel.position"),
        CANCEL_NAME("cancel.name"),
        ;

        private final String path;

        Path(String path) {
            this.path = path;
        }

        public String getPath() {
            return this.path;
        }

    }

}
