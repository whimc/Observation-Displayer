package edu.whimc.observations.commands.observations;

import edu.whimc.observations.Observations;
import edu.whimc.observations.commands.AbstractSubCommand;
import edu.whimc.observations.models.Observation;
import edu.whimc.observations.utils.Utils;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ObservationsInfo extends AbstractSubCommand {

    public ObservationsInfo(Observations plugin, String baseCommand, String subCommand) {
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
