package edu.whimc.observationdisplayer.libraries;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class SpigotCallback {

    private Map<UUID, Consumer<Player>> callbacks = new HashMap<>();

    private Map<UUID, Set<UUID>> playerCallbacks = new HashMap<>();

    public SpigotCallback(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void command(PlayerCommandPreprocessEvent e){
                if (!e.getMessage().startsWith("/spigot:callback")) {
                    return;
                }
                e.setCancelled(true);
                String[] args = e.getMessage().split(" ");

                if (args.length != 2 ) {
                    return;
                }
                if (args[1].split("-").length != 5) {
                    return;
                }
                UUID callbackUUID = UUID.fromString(args[1]);
                UUID playerUUID = e.getPlayer().getUniqueId();

                if (!SpigotCallback.this.callbacks.containsKey(callbackUUID)) {
                    return;
                }

                Consumer<Player> callback = SpigotCallback.this.callbacks.remove(callbackUUID);
                SpigotCallback.this.playerCallbacks.get(playerUUID).remove(callbackUUID);
                callback.accept(e.getPlayer());
            }
        }, plugin);
    }

    public void clearCallbacks(Player player) {
        if (!this.playerCallbacks.containsKey(player.getUniqueId())) {
            return;
        }
        for (UUID uuid : this.playerCallbacks.get(player.getUniqueId())) {
            this.callbacks.remove(uuid);
        }
    }

    public void createCommand(UUID playerUUID, TextComponent text, Consumer<Player> consumer){
        UUID callbackUUID = UUID.randomUUID();
        this.callbacks.put(callbackUUID, consumer);
        if (this.playerCallbacks.containsKey(playerUUID)) {
            this.playerCallbacks.get(playerUUID).add(callbackUUID);
        } else {
            this.playerCallbacks.put(playerUUID, new HashSet<>(Arrays.asList(callbackUUID)));
        }
        text.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/spigot:callback " + callbackUUID));
    }
}