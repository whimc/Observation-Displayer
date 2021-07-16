package edu.whimc.observations.commands.observations;

import edu.whimc.observations.Observations;
import edu.whimc.observations.commands.AbstractSubCommand;
import edu.whimc.observations.utils.Utils;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ObservationsList extends AbstractSubCommand {

    public ObservationsList(Observations plugin, String baseCommand, String subCommand) {
        super(plugin, baseCommand, subCommand);
        super.description("Lists all active observations");
        super.arguments("[-p <player>] [-w <\"world...\">]");
        super.bypassArgumentChecks();
    }

    @Override
    protected boolean onCommand(CommandSender sender, String[] args) {
        String player = null;
        String world = null;

        for (int ind = 0; ind < args.length - 1; ind++) {
            String param = args[ind].toLowerCase();

            if (param.equalsIgnoreCase("-p")) {
                player = args[ind + 1];
            }
            if (param.equalsIgnoreCase("-w")) {
                world = args[ind + 1];
            }
        }

        Utils.debug("Player: " + player + " | World: " + world);

        if (args.length > 0 && player == null && world == null) {
            Utils.msg(sender, "&cIncorrect parameter usage!",
                    "  " + super.getUsage(0),
                    "  &7Example:", "    &7/observations &blist &3-p Poi -w &7\"&3Redstone World&7\"");
            return true;
        }

        Utils.listObservations(sender, player, world);
        return true;
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        return Utils.getFlaggedTabComplete(sender, args);
    }

}
