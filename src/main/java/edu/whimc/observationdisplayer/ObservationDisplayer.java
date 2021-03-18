package edu.whimc.observationdisplayer;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import edu.whimc.observationdisplayer.commands.ObserveCommand;
import edu.whimc.observationdisplayer.commands.observations.ObservationsCommand;
import edu.whimc.observationdisplayer.models.Observation;
import edu.whimc.observationdisplayer.observetemplate.TemplateManager;
import edu.whimc.observationdisplayer.utils.Utils;
import edu.whimc.observationdisplayer.utils.sql.Queryer;

public class ObservationDisplayer extends JavaPlugin implements CommandExecutor {

    public static final String PERM_PREFIX = "whimc-observations";

    private Queryer queryer;

    private TemplateManager templateManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        Utils.setDebug(getConfig().getBoolean("debug"));

        this.queryer = new Queryer(this, q -> {
            if (q == null) {
                this.getLogger().severe("Could not create MySQL connection! Disabling plugin...");
                getCommand("observations").setExecutor(this);
                getCommand("observe").setExecutor(this);
            } else {
                Utils.setDebugPrefix(getDescription().getName());
                Utils.debug("Starting to load observations...");
                q.loadObservations(() -> {
                    Utils.debug("Finished loading observations!");
                });
                Observation.scanForExpiredObservations(this);

                Permission parent = new Permission(PERM_PREFIX + ".*");
                Bukkit.getPluginManager().addPermission(parent);

                Permission entry = new Permission(PERM_PREFIX + ".entry.*");
                entry.addParent(parent, true);
                Bukkit.getPluginManager().addPermission(entry);

                this.templateManager = new TemplateManager(this);

                getCommand("observe").setExecutor(new ObserveCommand(this));

                ObservationsCommand oc = new ObservationsCommand(this);
                getCommand("observations").setExecutor(oc);
                getCommand("observations").setTabCompleter(oc);
            }
        });
    }

    public Queryer getQueryer() {
        return this.queryer;
    }

    public TemplateManager getTemplateManager() {
        return this.templateManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Utils.msg(sender, "&cThis plugin is disabled because it was unable to connect to the configured database. " +
                "Please modify the config to ensure the credentials are correct then restart the server.");
        return true;
    }

}
