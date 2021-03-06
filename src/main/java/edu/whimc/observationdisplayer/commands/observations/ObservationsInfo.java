package edu.whimc.observationdisplayer.commands.observations;

import java.util.List;

import org.bukkit.command.CommandSender;

import edu.whimc.observationdisplayer.Observation;
import edu.whimc.observationdisplayer.ObservationDisplayer;
import edu.whimc.observationdisplayer.commands.AbstractSubCommand;
import edu.whimc.observationdisplayer.utils.Utils;

public class ObservationsInfo extends AbstractSubCommand {

    public ObservationsInfo(ObservationDisplayer plugin, String baseCommand, String subCommand) {
        super(plugin, baseCommand, subCommand);
        super.description("Gets information about an observation");
        super.arguments("id");
    }

    @Override
    protected boolean onCommand(CommandSender sender, String[] args) {
        Observation obs = Utils.getObervationWithError(sender, args[0]);
        if (obs == null) return true;

        Utils.msgNoPrefix(sender,
                "&7&m-----------------&r &9&lObservation Info&r &7&m------------------",
                "  &9ID: &7" + obs.getId(),
                "  &9Created: &7" + Utils.getDate(obs.getTimestamp()),
                "  &9Expires: &7" + (obs.getExpiration() == null ? "Never" : Utils.getDate(obs.getExpiration())),
                "  &9Player: &7" + obs.getPlayer(),
                "  &9Observation: &8\"&f&l" + obs.getObservation() + "&8\"",
                "",
                "  &9Holo Location:",
                "    " + Utils.locationString(obs.getHoloLocation(), false),
                "  &9View Location:",
                "    " + Utils.locationString(obs.getViewLocation(), true),
                "&7&m-----------------------------------------------------");
        return true;
    }

    @Override
    protected List<java.lang.String> onTabComplete(CommandSender sender, java.lang.String[] args) {
        return Observation.getObservationsTabComplete(args[0]);
    }

}
