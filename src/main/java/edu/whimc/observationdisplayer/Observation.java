package edu.whimc.observationdisplayer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.handler.TouchHandler;

import edu.whimc.observationdisplayer.utils.Utils;

public class Observation {

    private static List<Observation> observations = new ArrayList<>();

    private ObservationDisplayer plugin;
    private int id;
    private Timestamp timestamp;
    private String playerName;
    private Location holoLoc;
    private Location viewLoc;
    private String observation;
    private Hologram hologram;
    private Timestamp expiration;
    private boolean temporary;

    private Observation() {}

    public static Observation createObservation(ObservationDisplayer plugin, Player player, Location viewLoc,
            String observation, Timestamp expiration) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Observation obs = new Observation(plugin, -1, timestamp, player.getName(), viewLoc, observation, expiration, false, true);
        observations.add(obs);
        return obs;
    }

    public static void loadTemporaryObservation(ObservationDisplayer plugin, int id, Timestamp timestamp,
            String playerName, Location viewLoc, String observation, Timestamp expiration) {
        Observation obs = new Observation(plugin, id, timestamp, playerName, viewLoc, observation, expiration, true, false);
        observations.add(obs);
    }

    public static void loadObservation(ObservationDisplayer plugin, int id, Timestamp timestamp,
            String playerName, Location viewLoc, String observation, Timestamp expiration) {
        Observation obs = new Observation(plugin, id, timestamp, playerName, viewLoc, observation, expiration, false, false);
        observations.add(obs);
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

    private Observation(ObservationDisplayer plugin, int id, Timestamp timestamp, String playerName,
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
        Hologram holo = HologramsAPI.createHologram(plugin, holoLoc);
        ObservationClick clickListener = new ObservationClick(viewLoc);

        holo.appendItemLine(new ItemStack(Material.OAK_SIGN))
                .setTouchHandler(clickListener);
        holo.appendTextLine(ChatColor.translateAlternateColorCodes('&', observation))
                .setTouchHandler(clickListener);
        holo.appendTextLine(ChatColor.GRAY + playerName + " - " + Utils.getDate(timestamp))
                .setTouchHandler(clickListener);

        if (this.expiration != null) {
            holo.appendTextLine(ChatColor.GRAY + "Expires " + Utils.getDate(expiration))
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

    private class ObservationClick implements TouchHandler {

        private Location loc;

        public ObservationClick(Location loc) {
            this.loc = loc;
        }

        @Override
        public void onTouch(Player player) {
            player.teleport(loc);
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
        this.holoLoc.getBlockY() + ", "+ this.holoLoc.getBlockZ() + "&7)";
    }

    public void deleteAndSetInactive() {
        deleteAndSetInactive(() -> {});
    }

    public void deleteAndSetInactive(Runnable callback) {
        plugin.getQueryer().makeSingleObservationInactive(this.id, callback);
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
