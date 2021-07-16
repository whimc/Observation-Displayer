package edu.whimc.observations.observetemplate;

import edu.whimc.observations.Observations;
import edu.whimc.observations.libraries.SpigotCallback;
import edu.whimc.observations.observetemplate.gui.TemplateGui;
import edu.whimc.observations.observetemplate.gui.TemplateSelection;
import edu.whimc.observations.observetemplate.models.ObservationTemplate;
import edu.whimc.observations.observetemplate.models.ObservationType;

import java.util.HashMap;
import java.util.Map;

public class TemplateManager {

    private final TemplateGui gui;

    private final SpigotCallback spigotCallback;

    private final Map<ObservationType, ObservationTemplate> templates = new HashMap<>();

    public TemplateManager(Observations plugin) {
        this.spigotCallback = new SpigotCallback(plugin);

        for (ObservationType type : ObservationType.values()) {
            ObservationTemplate template = new ObservationTemplate(plugin, type);
            this.templates.put(type, template);
        }

        // The templates must be loaded before creating the GUI
        this.gui = new TemplateGui(plugin, this);

        for (ObservationTemplate template : this.templates.values()) {
            this.gui.setAction(template.getGuiPosition(), player -> {
                new TemplateSelection(plugin, this.spigotCallback, player, template);
            });
        }
    }

    public TemplateGui getGui() {
        return this.gui;
    }

    public ObservationTemplate getTemplate(ObservationType type) {
        return this.templates.get(type);
    }

}
