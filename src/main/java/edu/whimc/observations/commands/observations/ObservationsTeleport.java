package edu.whimc.observations.commands.observations;

import edu.whimc.observations.Observations;
import edu.whimc.observations.commands.AbstractSubCommand;
import edu.whimc.observations.models.Observation;
import edu.whimc.observations.utils.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ObservationsTeleport extends AbstractSubCommand {

    public ObservationsTeleport(Observations plugin, String baseCommand, String subCommand) {
        super(plugin, baseCommand, subCommand);
        super.description("Teleports you to an observation");
        super.arguments("id");
        super.requiresPlayer();
    }

    @Override
    protected boolean onCommand(CommandSender sender, String[] args) {
        Observation obs = Utils.getObervationWithError(sender, args[0]);
        if (obs == null) return true;

        ((Player) sender).teleport(obs.getViewLocation());
        Utils.msg(sender, "&aYou have been teleported to observation \"&2" + obs.getId() + "&a\"!");
        return true;
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        return Observation.getObservationsTabComplete(args[0]);
    }

}
