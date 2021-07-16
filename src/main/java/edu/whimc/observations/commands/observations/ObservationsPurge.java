package edu.whimc.observations.commands.observations;

import edu.whimc.observations.Observations;
import edu.whimc.observations.commands.AbstractSubCommand;
import edu.whimc.observations.models.Observation;
import edu.whimc.observations.utils.Utils;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.stream.Collectors;

public class ObservationsPurge extends AbstractSubCommand {

    public ObservationsPurge(Observations plugin, String baseCommand, String subCommand) {
        super(plugin, baseCommand, subCommand);
        super.description("Remove temporarily reactivated observations");
    }

    @Override
    protected boolean onCommand(CommandSender sender, String[] args) {
        List<Observation> toRemove = Observation.getObservations().stream()
                .filter(Observation::isTemporary)
                .collect(Collectors.toList());
        toRemove.stream().forEachOrdered(Observation::deleteObservation);

        Utils.msg(sender, "&aRemoved &2" + toRemove.size() + " &atemporary observation(s)!");
        return true;
    }

}
