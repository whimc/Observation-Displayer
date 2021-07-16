package edu.whimc.observations.commands.observations;

import edu.whimc.observations.Observations;
import edu.whimc.observations.commands.AbstractSubCommand;
import edu.whimc.observations.models.Observation;
import edu.whimc.observations.utils.Utils;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ObservationsRemove extends AbstractSubCommand {

    public ObservationsRemove(Observations plugin, String baseCommand, String subCommand) {
        super(plugin, baseCommand, subCommand);
        super.description("Removes an observation");
        super.arguments("id");
    }

    @Override
    protected boolean onCommand(CommandSender sender, String[] args) {
        Observation obs = Utils.getObervationWithError(sender, args[0]);
        if (obs == null) return true;

        obs.deleteAndSetInactive(() -> {
            Utils.msg(sender, "&aObservation \"&2" + obs.getId() + "&a\" removed!");
        });

        return true;
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        return Observation.getObservationsTabComplete(args[0]);
    }
}
