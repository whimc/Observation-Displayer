package edu.whimc.observations.utils;

import edu.whimc.observations.models.Observation;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {

    private static boolean debug = false;
    private static String debugPrefix = "[Observations] ";

    private static final String PREFIX = "&8&l[&9&lObservations&8&l]&r ";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM d yyyy, h:mm a z");

    public static void setDebug(boolean shouldDebug) {
        debug = shouldDebug;
    }

    /**
     * Prints a debug message if "debug" is true.
     *
     * @param str Message to print
     */
    public static void debug(String str) {
        if (!debug) return;
        Bukkit.getLogger().info(color(debugPrefix + str));
    }

    /**
     * Sets the debug message prefix.
     *
     * @param prefix Prefix to be set
     */
    public static void setDebugPrefix(String prefix) {
        debugPrefix = "[" + prefix + "] ";
    }

    /**
     * Gets a nice formatted date.
     *
     * @param timestamp Timestamp of date to format
     * @return A formatted version of the given date
     */
    public static String getDate(Timestamp timestamp) {
        return DATE_FORMAT.format(new Date(timestamp.getTime()));
    }

    public static String getDateNow() {
        return getDate(new Timestamp(System.currentTimeMillis()));
    }

    public static Timestamp parseDate(String str) {
        try {
            return new Timestamp(DATE_FORMAT.parse(str).getTime());
        } catch (ParseException e) {
            return null;
        }
    }

    public static void msg(CommandSender sender, String... messages) {
        for (int ind = 0; ind < messages.length; ind++) {
            if (ind == 0) {
                sender.sendMessage(color(PREFIX + messages[ind]));
            } else {
                sender.sendMessage(color(messages[ind]));
            }
        }
    }

    public static String locationString(Location loc, boolean yawPitch) {
        NumberFormat formatter = new DecimalFormat("#0.00");

        StringBuilder message = new StringBuilder();
        message.append("&7World: &f&o" + loc.getWorld().getName());
        message.append("  &7X: &f&o" + formatter.format(loc.getX()));
        message.append("  &7Y: &f&o" + formatter.format(loc.getY()));
        message.append("  &7Z: &f&o" + formatter.format(loc.getZ()));

        if (yawPitch) {
            message.append("\n" + "    &7Pitch: &f&o" + formatter.format(loc.getPitch()));
            message.append("  &7Yaw: &f&o" + formatter.format(loc.getYaw()));
        }

        return message.toString();
    }

    public static void listObservations(CommandSender sender, String player, String world) {
        if (Observation.getObservations().size() == 0) {
            Utils.msg(sender, "&7There are currently no observations!");
            return;
        }

        Utils.msgNoPrefix(sender, "&7&m-----------------&r &9&lObservation List&r &7&m------------------",
                "  &9Player: " + (player == null ? "&7N/A" : "&8\"&7&o" + player + "&8\"") +
                        "    &9World: " + (world == null ? "&7N/A" : "&8\"&7&o" + world + "&8\""),
                "");

        List<Observation> matches = Observation.getObservations().stream()
                .filter(v -> player == null || player.equalsIgnoreCase(v.getPlayer()))
                .filter(v -> world == null || world.equalsIgnoreCase(v.getHoloLocation().getWorld().getName()))
                .collect(Collectors.toList());
        matches.stream()
                .forEachOrdered(v -> Utils.msgNoPrefix(sender, " &7- " + v.toString()));

        Utils.msgNoPrefix(sender, "&9" + matches.size() + " observations(s) found.");

        Utils.msgNoPrefix(sender, "&7&m-----------------------------------------------------");
    }

    public static String coloredSubstring(String str, int length) {
        str = color(str);
        StringBuilder newStr = new StringBuilder();
        int count = 0;
        boolean ignore = false;
        for (char chr : str.toCharArray()) {
            if (count >= length) break;
            newStr.append(chr);

            if (ignore) {
                ignore = false;
                continue;
            }

            if (chr == ChatColor.COLOR_CHAR) ignore = true;
            if (chr != ChatColor.COLOR_CHAR && !ignore) count++;
        }

        return newStr.toString().replace(ChatColor.COLOR_CHAR, '&');
    }

    public static void msgNoPrefix(CommandSender sender, Object... messages) {
        for (Object str : messages) {
            if (str instanceof BaseComponent) {
                sender.spigot().sendMessage((BaseComponent) str);
            } else {
                sender.sendMessage(color(str.toString()));
            }
        }
    }

    public static String color(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public static Integer parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer parseIntWithError(CommandSender sender, String str) {
        Integer id = parseInt(str);
        if (id == null) {
            Utils.msg(sender, "&c\"&4" + str + "&c\" is an invalid number!");
            return null;
        }

        return id;
    }

    public static Observation getObervationWithError(CommandSender sender, String strId) {
        Integer id = parseIntWithError(sender, strId);
        if (id == null) {
            return null;
        }

        Observation obs = Observation.getObservation(id);
        if (obs == null) {
            Utils.msg(sender, "&c\"&4" + id + "&c\" is not a valid observation id!");
            return null;
        }

        return obs;
    }

    public static List<String> getWorldsTabComplete(String hint) {
        return Bukkit.getWorlds().stream()
                .filter(v -> v.getName().toLowerCase().startsWith(hint))
                .map(World::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    public static List<String> getFlaggedTabComplete(CommandSender sender, String[] args) {
        List<String> res = new ArrayList<>(Arrays.asList("-p", "-w"));
        if (args.length == 1) {
            return res;
        }

        // Removed used flags
        Stream.of(args).map(String::toLowerCase).forEachOrdered(v -> res.remove(v));

        String prev = args[args.length - 2];
        String hint = args[args.length - 1].toLowerCase();
        if (prev.equalsIgnoreCase("-p")) {
            return Observation.getPlayersTabComplete(hint);
        }
        if (prev.equalsIgnoreCase("-w")) {
            return Utils.getWorldsTabComplete(hint);
        }

        return res;
    }

}
