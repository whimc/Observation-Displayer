package edu.whimc.observationdisplayer.models;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.handler.TouchHandler;
import edu.whimc.observationdisplayer.ObservationDisplayer;
import edu.whimc.observationdisplayer.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class Observation {

    private static final List<Observation> observations = new ArrayList<>();

    private final ObservationDisplayer plugin;
    private int id;
    private final Timestamp timestamp;
    private final String playerName;
    private final Location holoLoc;
    private final Location viewLoc;
    private final String observation;
    private Hologram hologram;
    private Timestamp expiration;
    private final boolean temporary;
    private Material hologramItem = Material.OAK_SIGN;

    public static Observation createObservation(ObservationDisplayer plugin, Player player, Location viewLoc,
                                                String observation, Timestamp expiration) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Observation obs = new Observation(plugin, -1, timestamp, player.getName(), viewLoc, observation, expiration, false, true);
        observations.add(obs);
        return obs;
    }

    public static Observation loadTemporaryObservation(ObservationDisplayer plugin, int id, Timestamp timestamp,
                                                       String playerName, Location viewLoc, String observation, Timestamp expiration) {
        Observation obs = new Observation(plugin, id, timestamp, playerName, viewLoc, observation, expiration, true, false);
        observations.add(obs);
        return obs;
    }

    public static Observation loadObservation(ObservationDisplayer plugin, int id, Timestamp timestamp,
                                              String playerName, Location viewLoc, String observation, Timestamp expiration) {
        Observation obs = new Observation(plugin, id, timestamp, playerName, viewLoc, observation, expiration, false, false);
        observations.add(obs);
        return obs;
    }

    public static void scanForExpiredObservations(ObservationDisplayer plugin) {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            long count = observations.stream()
                    .filter(v -> v.getExpiration() != null)
                    .filter(v -> Instant.now().isAfter(v.getExpiration().toInstant()))
                    .filter(v -> !v.isTemporary())
                    .collect(Collectors.toList())
                    .stream()
                    .peek(Observation::deleteObservation)
                    .count();
            if (count > 0) {
                plugin.getQueryer().makeExpiredObservationsInactive(dbCount -> {
                    Utils.debug("Removed " + count + " expired observation(s). (" + dbCount + ") from database");
                });
            }

        }, 20 * 60, 20 * 60);
    }

    protected Observation(ObservationDisplayer plugin, int id, Timestamp timestamp, String playerName,
                          Location viewLoc, String observation, Timestamp expiration, boolean temporary, boolean isNew) {
        this.plugin = plugin;
        this.timestamp = timestamp;
        this.playerName = playerName;
        this.holoLoc = viewLoc.clone().add(0, 3, 0).add(viewLoc.getDirection().multiply(2));
        this.viewLoc = viewLoc;
        this.observation = observation;
        this.expiration = expiration;
        this.temporary = temporary;

        if (!isNew) {
            this.id = id;
            createHologram();
            return;
        }

        plugin.getQueryer().storeNewObservation(this, newId -> {
            this.id = newId;
            createHologram();
        });
    }

    private void createHologram() {
        Hologram holo = HologramsAPI.createHologram(this.plugin, this.holoLoc);
        ObservationClick clickListener = new ObservationClick(this.viewLoc);

        holo.appendItemLine(new ItemStack(this.hologramItem))
                .setTouchHandler(clickListener);
        holo.appendTextLine(ChatColor.translateAlternateColorCodes('&', this.observation))
                .setTouchHandler(clickListener);
        holo.appendTextLine(ChatColor.GRAY + this.playerName + " - " + Utils.getDate(this.timestamp))
                .setTouchHandler(clickListener);

        if (this.expiration != null) {
            holo.appendTextLine(ChatColor.GRAY + "Expires " + Utils.getDate(this.expiration))
                    .setTouchHandler(clickListener);
        }

        if (this.temporary) {
            holo.appendTextLine(ChatColor.DARK_GRAY + "*temporary*")
                    .setTouchHandler(clickListener);
        }

        this.hologram = holo;
    }

    public void reRender() {
        deleteHologramOnly();
        createHologram();
    }

    public void setHologramItem(Material hologramItem) {
        this.hologramItem = hologramItem;
        this.hologram.getLine(0).removeLine();
        this.hologram.insertItemLine(0, new ItemStack(hologramItem));
    }

    private class ObservationClick implements TouchHandler {

        private final Location loc;

        public ObservationClick(Location loc) {
            this.loc = loc;
        }

        @Override
        public void onTouch(Player player) {
            player.teleport(this.loc);
        }
    }

    public static List<Observation> getObservations() {
        return observations;
    }

    public static Iterator<Observation> getObservationsIterator() {
        return observations.iterator();
    }

    public static Observation getObservation(int id) {
        for (Observation obs : observations) {
            if (obs.getId() == id) return obs;
        }

        return null;
    }

    public Hologram getHologram() {
        return this.hologram;
    }

    public String getPlayer() {
        return this.playerName;
    }

    public Location getHoloLocation() {
        return this.holoLoc;
    }

    public Location getViewLocation() {
        return this.viewLoc;
    }

    public String getObservation() {
        return this.observation;
    }

    public int getId() {
        return this.id;
    }

    public Timestamp getTimestamp() {
        return this.timestamp;
    }

    public Timestamp getExpiration() {
        return this.expiration;
    }

    public boolean hasExpired() {
        return this.timestamp != null && this.expiration.toInstant().isAfter(Instant.now());
    }

    public void setExpiration(Timestamp timestamp) {
        this.expiration = timestamp;
    }

    public boolean isTemporary() {
        return this.temporary;
    }

    @Override
    public String toString() {
        String text = Utils.color("&f&l" + this.observation);
        if (ChatColor.stripColor(text).length() > 20) {
            text = Utils.coloredSubstring(text, 20) + "&7 . . .";
        }

        return "&9&l" + this.id + ".&r &8\"" + text + "&8\" &9> &7&o" + this.playerName + " " +
                "&7(" + this.holoLoc.getWorld().getName() + ", " + this.holoLoc.getBlockX() + ", " +
                this.holoLoc.getBlockY() + ", " + this.holoLoc.getBlockZ() + "&7)";
    }

    public void deleteAndSetInactive() {
        deleteAndSetInactive(() -> {
        });
    }

    public void deleteAndSetInactive(Runnable callback) {
        this.plugin.getQueryer().makeSingleObservationInactive(this.id, callback);
        deleteObservation();
    }

    public void deleteObservation() {
        deleteHologramOnly();
        observations.remove(this);
    }

    public void deleteHologramOnly() {
        if (this.hologram != null) {
            this.hologram.delete();
            this.hologram = null;
        }
    }

    public static List<String> getObservationsTabComplete(String hint) {
        return observations.stream()
                .filter(v -> Integer.toString(v.getId()).startsWith(hint))
                .sorted(Comparator.comparing(Observation::getId))
                .map(v -> Integer.toString(v.getId()))
                .collect(Collectors.toList());
    }

    public static List<String> getPlayersTabComplete(String hint) {
        Set<String> players = observations.stream()
                .map(Observation::getPlayer)
                .distinct()
                .filter(v -> v.toLowerCase().startsWith(hint.toLowerCase()))
                .sorted()
                .collect(Collectors.toSet());
        players.addAll(Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toSet()));
        return new ArrayList<>(players);
    }

}
