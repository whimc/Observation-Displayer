package edu.whimc.observations.commands.observations;

import edu.whimc.observations.Observations;
import edu.whimc.observations.commands.AbstractSubCommand;
import edu.whimc.observations.models.Observation;
import edu.whimc.observations.utils.Utils;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.stream.Collectors;

public class ObservationsRemoveAll extends AbstractSubCommand {

    public ObservationsRemoveAll(Observations plugin, String baseCommand, String subCommand) {
        super(plugin, baseCommand, subCommand);
        super.description("Removes observations matching the given query");
        super.arguments("[-p <player>] [-w <\"world...\">]");
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

        if (player == null && world == null) {
            Utils.msg(sender, "&cIncorrect parameter usage!",
                    "  " + super.getUsage(0),
                    "  &7Examples:",
                    "    &7/observations &bremoveall &3-p Poi -w &7\"&3Redstone World&7\"",
                    "    &7/observations &bremoveall &3-p Poi",
                    "    &7/observations &bremoveall &3-w NoMoon");
            return true;
        }

        String finalPlayer = player;
        String finalWorld = world;

        List<Observation> toRemove = Observation.getObservations().stream()
                .filter(v -> finalWorld == null || v.getHoloLocation().getWorld().getName().equalsIgnoreCase(finalWorld))
                .filter(v -> finalPlayer == null || v.getPlayer().equalsIgnoreCase(finalPlayer))
                .collect(Collectors.toList());
        toRemove.stream()
                .forEachOrdered(Observation::deleteObservation);

        if (toRemove.size() > 0) {
            plugin.getQueryer().makeObservationsInactive(world, player, count -> {
                Utils.msg(sender, "&7Finished removing " + count + " observation(s)");
            });
        } else {
            Utils.msg(sender, "&7No observations matched your input query!");
        }
        return true;
    }

    @Override
    protected void missingArguments(CommandSender sender, String missingArgs) {
        Utils.msg(sender, "&cMissing parameters: " + missingArgs,
                "  " + super.getUsage(0),
                "  &7Examples:",
                "    /observations removeall -p Poi -w \"Redstone World\"",
                "    /observations removeall -p Poi",
                "    /observations removeall -w NoMoon");
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        return Utils.getFlaggedTabComplete(sender, args);
    }

}
