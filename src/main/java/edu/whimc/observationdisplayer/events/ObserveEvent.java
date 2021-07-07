package edu.whimc.observationdisplayer.events;

import edu.whimc.observationdisplayer.Observation;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * A custom event to be fired whenever a player creates an observation.
 */
public class ObserveEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private Observation observation;
    private Player player;

    public ObserveEvent(Observation observation, Player player) {
        this.observation = observation;
        this.player = player;
    }

    public Observation getObservation() {
        return observation;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
