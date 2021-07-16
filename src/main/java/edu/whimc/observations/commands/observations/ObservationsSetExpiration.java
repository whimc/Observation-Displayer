package edu.whimc.observations.commands.observations;

import edu.whimc.observations.Observations;
import edu.whimc.observations.commands.AbstractSubCommand;
import edu.whimc.observations.models.Observation;
import edu.whimc.observations.utils.Utils;
import org.bukkit.command.CommandSender;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

public class ObservationsSetExpiration extends AbstractSubCommand {

    public ObservationsSetExpiration(Observations plugin, String baseCommand, String subCommand) {
        super(plugin, baseCommand, subCommand);
        super.description("Sets or removes the expiration of an observation");
        super.arguments("id \"expiration...\"|'none'");
    }

    @Override
    protected boolean onCommand(CommandSender sender, String[] args) {
        Integer id = Utils.parseIntWithError(sender, args[0]);
        if (id == null) {
            return true;
        }

        Observation obs = Utils.getObervationWithError(sender, id.toString());
        if (obs == null) {
            Utils.msgNoPrefix(sender, "&7Load this observation with \"/observation reactivate\"");
            return true;
        }

        Timestamp newExpiration = Utils.parseDate(args[1]);
        if (!args[1].equalsIgnoreCase("none") && newExpiration == null) {
            Utils.msg(sender, "&cCould not parse date \"&4" + args[1] + "&c\"!",
                    "&7Example date: \"" + Utils.getDateNow() + "\"");
            return true;
        }

        obs.setExpiration(newExpiration);
        obs.reRender();
        plugin.getQueryer().setExpiration(obs.getId(), newExpiration, () -> {
            if (newExpiration == null) {
                Utils.msg(sender, "&aRemoved the expiration from \"&2" + obs.getId() + "&a\"");
            } else {
                Utils.msg(sender, "&aExpiration of \"&2" + obs.getId() + "&a\" set to \"&2" + Utils.getDate(newExpiration) + "&a\"");
            }
        });
        return true;
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Observation.getObservationsTabComplete(args[0]);
        }
        return Arrays.asList("none", "\"" + Utils.getDateNow() + "\"");
    }

}
