package edu.whimc.observationdisplayer.observetemplate;

import java.util.HashMap;
import java.util.Map;

import edu.whimc.observationdisplayer.ObservationDisplayer;
import edu.whimc.observationdisplayer.observetemplate.gui.TemplateGui;
import edu.whimc.observationdisplayer.observetemplate.gui.TemplateSelection;
import edu.whimc.observationdisplayer.observetemplate.models.ObservationTemplate;
import edu.whimc.observationdisplayer.observetemplate.models.ObservationType;
import edu.whimc.observationdisplayer.observetemplate.models.SpigotCallback;

public class TemplateManager {

    private TemplateGui gui;

    private SpigotCallback spigotCallback;

    private Map<ObservationType, ObservationTemplate> templates = new HashMap<>();

    public TemplateManager(ObservationDisplayer plugin) {
        this.gui = new TemplateGui(plugin);
        this.spigotCallback = new SpigotCallback(plugin);

        for (ObservationType type : ObservationType.values()) {
            ObservationTemplate template = new ObservationTemplate(plugin, type);

            this.templates.put(type, template);
            this.gui.addConsumer(type, player -> {
                new TemplateSelection(plugin, this.spigotCallback, player, getTemplate(type));
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
