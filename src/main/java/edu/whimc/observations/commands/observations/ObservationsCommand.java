package edu.whimc.observations.commands.observations;

import edu.whimc.observations.Observations;
import edu.whimc.observations.commands.AbstractSubCommand;
import edu.whimc.observations.utils.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ObservationsCommand implements CommandExecutor, TabCompleter {

    private final Map<String, AbstractSubCommand> subCommands = new HashMap<>();

    public ObservationsCommand(Observations plugin) {
        subCommands.put("info", new ObservationsInfo(plugin, "observations", "info"));
        subCommands.put("list", new ObservationsList(plugin, "observations", "list"));
        subCommands.put("near", new ObservationsNear(plugin, "observations", "near"));
        subCommands.put("purge", new ObservationsPurge(plugin, "observations", "purge"));
        subCommands.put("reactivate", new ObservationsReactivate(plugin, "observations", "reactivate"));
        subCommands.put("remove", new ObservationsRemove(plugin, "observations", "remove"));
        subCommands.put("removeall", new ObservationsRemoveAll(plugin, "observations", "removeall"));
        subCommands.put("setexpiration", new ObservationsSetExpiration(plugin, "observations", "setexpiration"));
        subCommands.put("teleport", new ObservationsTeleport(plugin, "observations", "teleport"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (args.length == 0) {
            sendCommands(sender);
            return true;
        }

        AbstractSubCommand subCmd = subCommands.getOrDefault(args[0].toLowerCase(), null);
        if (subCmd == null) {
            sendCommands(sender);
            return true;
        }

        return subCmd.executeSubCommand(sender, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return subCommands.keySet().stream().sorted().collect(Collectors.toList());
        }

        if (args.length == 1) {
            return subCommands.keySet()
                    .stream()
                    .filter(v -> v.startsWith(args[0].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        }

        AbstractSubCommand subCmd = subCommands.getOrDefault(args[0].toLowerCase(), null);
        if (subCmd == null) {
            return null;
        }

        return subCmd.executeOnTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    private void sendCommands(CommandSender sender) {
        Utils.msgNoPrefix(sender, "&7&m------------------&r &8&l[&9&lObservations&8&l]&r &7&m------------------", "");
        subCommands.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(e -> Utils.msgNoPrefix(sender, e.getValue().getHelpLine()));
        Utils.msgNoPrefix(sender, "", "&7&m-----------------------------------------------------");
    }

}

