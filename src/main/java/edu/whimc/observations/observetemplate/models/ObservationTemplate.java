package edu.whimc.observations.observetemplate.models;

import edu.whimc.observations.Observations;
import edu.whimc.observations.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ObservationTemplate {

    private final List<ObservationPrompt> prompts = new ArrayList<>();

    private final ObservationType type;

    private final Material guiItem;

    private final String guiItemName;

    private final int guiPosition;

    private final List<String> guiLore;

    public ObservationTemplate(Observations plugin, ObservationType type) {
        this.type = type;
        String path = "templates." + type.name() + ".prompts";

        List<Map<?, ?>> entries = plugin.getConfig().getMapList(path);
        for (Map<?, ?> entry : entries) {
            this.prompts.add(new ObservationPrompt(entry));
        }

        FileConfiguration config = plugin.getConfig();
        String pathPrefix = "templates." + type.name() + ".gui.";

        this.guiItem = Material.matchMaterial(config.getString(pathPrefix + "item"));
        this.guiItemName = config.getString(pathPrefix + "name");
        this.guiPosition = config.getInt(pathPrefix + "position");
        this.guiLore = config.getStringList(pathPrefix + "lore");
    }

    public List<ObservationPrompt> getPrompts() {
        return this.prompts;
    }

    public ObservationType getType() {
        return this.type;
    }

    public Material getGuiItem() {
        return this.guiItem;
    }

    public String getGuiItemName() {
        return this.guiItemName;
    }

    public int getGuiPosition() {
        return this.guiPosition;
    }

    public List<String> getGuiLore() {
        return this.guiLore;
    }

    public String getColor() {
        return ChatColor.getLastColors(Utils.color(this.guiItemName));
    }

}
