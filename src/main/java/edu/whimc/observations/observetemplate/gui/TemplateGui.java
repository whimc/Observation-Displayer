package edu.whimc.observations.observetemplate.gui;

import edu.whimc.observations.Observations;
import edu.whimc.observations.commands.ObserveCommand;
import edu.whimc.observations.observetemplate.TemplateManager;
import edu.whimc.observations.observetemplate.models.ObservationTemplate;
import edu.whimc.observations.observetemplate.models.ObservationType;
import edu.whimc.observations.utils.Utils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class TemplateGui implements Listener {

    private final Observations plugin;
    private final TemplateManager manager;


    private final Map<Integer, Consumer<Player>> slotActions = new HashMap<>();

    /* The inventory that will hold the GUI */
    private Inventory inventory;
    /* The name of the GUI */
    private String inventoryName;
    /* The size of the GUI */
    private int inventorySize;

    /* The item used for filler slots */
    private ItemStack fillerItem;

    /* The item used for the cancel button */
    private ItemStack cancelItem;
    /* The position of the cancel button */
    private int cancelPosition;

    /* The item used for the uncategorized observation button */
    private ItemStack uncategorizedItem;
    /* The position of the uncategorized observation button */
    private int uncategorizedPosition;

    public TemplateGui(Observations plugin, TemplateManager manager) {
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
        setLore(this.cancelItem, getStringList(Path.CANCEL_LORE));

        this.uncategorizedItem = new ItemStack(Material.matchMaterial(getString(Path.UNCATEGORIZED_ITEM)));
        this.uncategorizedPosition = getInt(Path.UNCATEGORIZED_POSITION);
        setName(this.uncategorizedItem, getString(Path.UNCATEGORIZED_NAME));
        setLore(this.uncategorizedItem, getStringList(Path.UNCATEGORIZED_LORE));

        this.inventory = Bukkit.createInventory(null, this.inventorySize, Utils.color(this.inventoryName));

        // Add in filler items
        for (int slot = 0; slot < this.inventory.getSize(); slot++) {
            this.inventory.setItem(slot, this.fillerItem);
        }

        // Add cancel item
        this.inventory.setItem(this.cancelPosition, this.cancelItem);
        setAction(this.cancelPosition, p -> Utils.msg(p, "Observation canceled!"));

        // Add uncategorized observation item
        this.inventory.setItem(this.uncategorizedPosition, this.uncategorizedItem);
        setAction(this.uncategorizedPosition, p -> {
                    if (!p.hasPermission(ObserveCommand.FREE_HAND_PERM)) {
                        Utils.msg(p,
                                "&cYou do not have the required permission!",
                                "  &f&o" + ObserveCommand.FREE_HAND_PERM);
                        return;
                    }

                    plugin.getSignMenuFactory()
                            .newMenu(Collections.singletonList(ChatColor.UNDERLINE + "Your Observation"))
                            .reopenIfFail(false)
                            .response((signPlayer, strings) -> {
                                String response = StringUtils.join(Arrays.copyOfRange(strings, 1, strings.length), ' ').trim();
                                if (response.isEmpty()) {
                                    return false;
                                }
                                ObserveCommand.makeObservation(this.plugin, response, signPlayer);
                                return true;
                            })
                            .open(p);
                }
        );

        // Add template-specific items
        for (ObservationType type : ObservationType.values()) {
            ObservationTemplate template = this.manager.getTemplate(type);

            ItemStack item = new ItemStack(template.getGuiItem());
            setName(item, template.getGuiItemName());
            setLore(item, template.getGuiLore());

            this.inventory.setItem(template.getGuiPosition(), item);
        }
    }

    public void setAction(int slot, Consumer<Player> action) {
        this.slotActions.put(slot, action);
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

        // Do nothing if there's no action for the given slot
        Consumer<Player> action = this.slotActions.getOrDefault(event.getSlot(), null);
        if (action == null) {
            return;
        }

        // Close the inventory and execute the action for the slot
        player.closeInventory();
        action.accept(player);
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

    public List<String> getStringList(Path path) {
        return this.plugin.getConfig().getStringList(Path.ROOT.getPath() + path.getPath());
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
        CANCEL_LORE("cancel.lore"),

        UNCATEGORIZED_ITEM("uncategorized.item"),
        UNCATEGORIZED_POSITION("uncategorized.position"),
        UNCATEGORIZED_NAME("uncategorized.name"),
        UNCATEGORIZED_LORE("uncategorized.lore"),
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
