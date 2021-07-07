package edu.whimc.observationdisplayer.quests;

import edu.whimc.observationdisplayer.events.ObserveEvent;
import me.blackvein.quests.CustomObjective;
import me.blackvein.quests.Quest;
import me.blackvein.quests.Quests;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;

/**
 * A custom observation objective using the Quest plugin.
 */
public class QuestObservationObjective extends CustomObjective {

    public QuestObservationObjective() {
        this.setName("Observation Objective");
        this.setAuthor("Pieter Svenson");
        this.setShowCount(true);
        this.setCountPrompt("Enter the number of observations the player must make:");
        this.setDisplay("Make %count% observations");
    }

    @EventHandler
    public void onObserve(ObserveEvent event) {

        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("Quests");
        if (plugin == null) {
            return;
        }
        Quests quests = (Quests) plugin;

        for (Quest quest : quests.getQuester(event.getPlayer().getUniqueId()).getCurrentQuests().keySet()) {
            incrementObjective(event.getPlayer(), this, 1, quest);
        }
    }

}
