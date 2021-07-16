package edu.whimc.observations.commands.observations;

import edu.whimc.observations.Observations;
import edu.whimc.observations.commands.AbstractSubCommand;
import edu.whimc.observations.models.Observation;
import edu.whimc.observations.utils.Utils;
import org.bukkit.command.CommandSender;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

public class ObservationsReactivate extends AbstractSubCommand {

    public ObservationsReactivate(Observations plugin, String baseCommand, String subCommand) {
        super(plugin, baseCommand, subCommand);
        super.description("Temporarily reactivate inactive observations");
        super.arguments("id");
        super.arguments("\"startTime...\" \"endTime...\"");
        super.arguments("startId endId");
        super.bypassArgumentChecks();
    }

    @Override
    protected boolean onCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, "&cNo parameters given!");
            return true;
        }

        if (args.length == 1) {
            Timestamp date = Utils.parseDate(args[0]);
            if (date != null) {
                Utils.msg(sender, "&cMissing end date!");
                return true;
            }
            Integer id = Utils.parseIntWithError(sender, args[0]);
            if (id == null) {
                return true;
            }

            if (Observation.getObservation(id) != null) {
                Utils.msg(sender, "&c\"&4" + id + "&c\" is already active!");
                return true;
            }

            plugin.getQueryer().reactivateObservation(id, count -> {
                if (count == 0) {
                    Utils.msg(sender, "&cObservation \"&4" + id + "&c\" does not exist!");
                } else {
                    Utils.msg(sender, "&aTemporarily reactivated observation \"&2" + id + "&a\"");
                }
            });
            return true;
        }

        Timestamp startTime = Utils.parseDate(args[0]);
        Timestamp endTime = Utils.parseDate(args[1]);

        Integer startId = Utils.parseInt(args[0]);
        Integer endId = Utils.parseInt(args[1]);

        if (startTime != null || endTime != null) {
            if (startTime == null) {
                invalidTime(sender, args[0]);
                return true;
            }
            if (endTime == null) {
                invalidTime(sender, args[1]);
                return true;
            }

            if (endTime.before(startTime)) {
                Timestamp temp = endTime;
                endTime = startTime;
                startTime = temp;
            }

            String formattedStart = Utils.getDate(startTime);
            String formattedEnd = Utils.getDate(endTime);
            Utils.msg(sender, "&aTemporarily reactivating observations between \"&2" + formattedStart + "&a\" and \"&2" + formattedEnd + "&a\"!");
            plugin.getQueryer().reactivateObservations(startTime, endTime, count -> {
                Utils.msg(sender, "&7" + count + " observations reactivated");
            });
            return true;
        }

        if (startId != null || endId != null) {
            if (startId == null || startId < 0) {
                invalidNumber(sender, args[0]);
                return true;
            }
            if (endId == null || endId < 0) {
                invalidNumber(sender, args[1]);
                return true;
            }

            if (endId < startId) {
                Integer temp = endId;
                endId = startId;
                startId = temp;
            }

            Utils.msg(sender, "&aTemporarily reactivating observations &2" + startId + "&a through &2" + endId + "&a!");
            plugin.getQueryer().reactivateObservations(startId, endId, count -> {
                Utils.msg(sender, "&7" + count + " observations reactivated");
            });
            return true;
        }

        sendHelp(sender, "&cCould not parse date or id with params \"&f" + args[0] + "&c\", \"&f" + args[1] + "&c\"!");
        return true;
    }

    private void invalidTime(CommandSender sender, String date) {
        Utils.msg(sender, "&c\"&4" + date + "&c\" is an invalid date! Make sure you surround your dates in quotes!",
                "&7Example date: \"" + Utils.getDateNow() + "\"");
    }

    private void invalidNumber(CommandSender sender, String id) {
        Utils.msg(sender, "&c\"&4" + id + "&c\" is an invalid observation id!");
    }

    private void sendHelp(CommandSender sender, String error) {
        Utils.msg(sender, error);
        for (String usage : super.getUsages()) {
            Utils.msgNoPrefix(sender, "  " + usage);
        }
        Utils.msgNoPrefix(sender, "  &7Examples:",
                "    &7/observations &breactivate &31",
                "    &7/observations &breactivate &31 100",
                "    &7/observations &breactivate &7\"&3August 13 1999, 7:00 pm CST&7\" \"&3" + Utils.getDateNow() + "&7\"");
    }

    @Override
    public List<String> executeOnTabComplete(CommandSender sender, String[] args) {
        return Arrays.asList("\"" + Utils.getDateNow() + "\"");
    }
}
