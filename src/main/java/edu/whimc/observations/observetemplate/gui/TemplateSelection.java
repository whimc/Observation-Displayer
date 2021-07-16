package edu.whimc.observations.observetemplate.gui;

import edu.whimc.observations.Observations;
import edu.whimc.observations.commands.ObserveCommand;
import edu.whimc.observations.libraries.CenteredText;
import edu.whimc.observations.libraries.SpigotCallback;
import edu.whimc.observations.models.Observation;
import edu.whimc.observations.observetemplate.models.ObservationPrompt;
import edu.whimc.observations.observetemplate.models.ObservationTemplate;
import edu.whimc.observations.utils.Utils;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class TemplateSelection implements Listener {

    private static final String CHECK = "\u2714";

    private static final String CROSS = "\u274C";

    private static final String BULLET = "\u2022";
    /**
     * Selections that are currently happening.
     */
    private static final Map<UUID, TemplateSelection> ongoingSelections = new HashMap<>();
    /**
     * Instance of main class.
     */
    private final Observations plugin;
    /**
     * Used to create clickable messages with callbacks.
     */
    private final SpigotCallback spigotCallback;
    /**
     * The selected template to fill out.
     */
    private final ObservationTemplate template;
    /**
     * The UUID of the player making the selection.
     */
    private final UUID uuid;
    /**
     * The current responses that have been chosen
     */
    private final List<String> responses = new ArrayList<>();
    /**
     * The selected prompt from the template.
     */
    private ObservationPrompt prompt = null;
    /**
     * The stage of the selection.
     */
    private TemplateSelectionStage stage;
    /**
     * The response index that is being selected.
     */
    private int responseIndex = 0;

    public TemplateSelection(Observations plugin, SpigotCallback spigotCallback, Player player, ObservationTemplate template) {
        UUID uuid = player.getUniqueId();
        if (ongoingSelections.containsKey(uuid)) {
            ongoingSelections.get(uuid).destroySelection();
            Utils.msg(player, "Your previous observation was canceled because you started a new one!");
        }

        this.plugin = plugin;
        this.spigotCallback = spigotCallback;
        this.template = template;
        this.uuid = uuid;

        // Register this class as a listener to cancel clickables if they change worlds
        Bukkit.getPluginManager().registerEvents(this, plugin);

        ongoingSelections.put(uuid, this);
        doStage(TemplateSelectionStage.SELECT_PROMPT);
    }

    private void doStage(TemplateSelectionStage nextStage) {
        // Clear all other callbacks before entering the next stage
        this.spigotCallback.clearCallbacks(getPlayer());
        this.stage = nextStage;

        switch (nextStage) {
            case SELECT_PROMPT:
                doSelectPrompt();
                return;
            case SELECT_RESPONSE:
                doSelectResponse();
                return;
            case CONFIRM:
                doConfirm();
        }
    }

    private void doSelectPrompt() {
        Player player = getPlayer();

        sendHeader();
        Utils.msgNoPrefix(player, "&lClick the template you would like to fill out:", "");

        for (ObservationPrompt curPrompt : this.template.getPrompts()) {
            sendComponent(
                    player,
                    "&8" + BULLET + " &r" + curPrompt.getPrompt(),
                    "&aClick here to select \"&r" + curPrompt.getPrompt() + "&a\"",
                    p -> {
                        this.prompt = curPrompt;
                        doStage(TemplateSelectionStage.SELECT_RESPONSE);
                    });
        }

        sendFooter(false, p -> {
            destroySelection();
            this.plugin.getTemplateManager().getGui().openTemplateInventory(player);
        });
    }

    private void doSelectResponse() {
        Player player = getPlayer();
        List<String> responses = this.prompt.getResponses(player.getWorld(), this.responseIndex);
        String highlight = this.template.getColor() + "&l";
        String filledIn = replaceFirst(getFilledInPrompt(), ObservationPrompt.FILLIN, highlight + "[&n   " + highlight + "]&r");

        sendHeader();
        Utils.msgNoPrefix(player, filledIn, "");

        Consumer<String> addResponseAndAdvanceStage = response -> {
            this.responses.add(response);
            this.responseIndex += 1;

            TemplateSelectionStage nextStage = this.stage;
            if (this.responseIndex == this.prompt.getNumberOfFillIns()) {
                nextStage = TemplateSelectionStage.CONFIRM;
            }
            doStage(nextStage);
        };

        for (String response : responses) {
            sendComponent(
                    player,
                    "&8" + BULLET + " &r" + response,
                    "&aClick here to select \"&r" + response + "&a\"",
                    p -> addResponseAndAdvanceStage.accept(response)
            );
        }
        // Send component for custom input if they have permission
        if (player.hasPermission(ObserveCommand.CUSTOM_RESPONSE_PERM)) {
            sendComponent(
                    player,
                    "&8" + BULLET + " &7Write your own response",
                    "&aClick here to write your own response!",
                    p -> this.plugin.getSignMenuFactory()
                            .newMenu(Collections.singletonList(ChatColor.UNDERLINE + "Your response"))
                            .reopenIfFail(true)
                            .response((signPlayer, strings) -> {
                                String response = StringUtils.join(Arrays.copyOfRange(strings, 1, strings.length), ' ').trim();
                                if (response.isEmpty()) {
                                    return false;
                                }
                                addResponseAndAdvanceStage.accept(response);
                                return true;
                            })
                            .open(p)
            );
        }

        sendFooter(false, p -> {
            TemplateSelectionStage nextStage = this.stage;
            if (this.responseIndex == 0) {
                nextStage = TemplateSelectionStage.SELECT_PROMPT;
            } else {
                this.responses.remove(this.responses.size() - 1);
                this.responseIndex -= 1;
            }
            doStage(nextStage);
        });
    }

    private void doConfirm() {
        String filledIn = getFilledInPrompt();
        Player player = getPlayer();

        sendHeader();

        Utils.msgNoPrefix(player,
                "&f&lSubmit the following observation?",
                "",
                filledIn);

        sendFooter(true, p -> {
            this.responses.remove(this.responses.size() - 1);
            this.responseIndex -= 1;
            doStage(TemplateSelectionStage.SELECT_RESPONSE);
        });
    }

    private String getFilledInPrompt() {
        String result = this.prompt.getPrompt();
        String highlight = this.template.getColor();
        for (String response : this.responses) {
            result = replaceFirst(result, ObservationPrompt.FILLIN, highlight + response + "&r");
        }
        return result;
    }

    private String replaceFirst(String str, String pattern, String replacement) {
        return str.replaceFirst(Pattern.quote(pattern), replacement);
    }

    private void addCallback(TextComponent component, UUID playerUUID, Consumer<Player> onClick) {
        this.spigotCallback.createCommand(playerUUID, component, onClick);
    }

    private TextComponent createComponent(String text, String hoverText, Consumer<Player> onClick) {
        TextComponent message = new TextComponent(Utils.color(text));
        message.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new ComponentBuilder(Utils.color(hoverText)).create()));
        addCallback(message, this.uuid, onClick);
        return message;
    }

    private void sendComponent(Player player, String text, String hoverText, Consumer<Player> onClick) {
        player.spigot().sendMessage(createComponent(text, hoverText, onClick));
    }

    private void sendHeader() {
        Player player = getPlayer();
        String header = "&r " + this.template.getGuiItemName() + " ";
        CenteredText.sendCenteredMessage(player, header, "&7&m &r");
    }

    private void sendFooter(boolean withConfirm, Consumer<Player> goBackCallback) {
        Player player = getPlayer();
        ComponentBuilder builder = new ComponentBuilder("");

        if (withConfirm) {
            Consumer<Player> confirmCallback = p -> {
                String text = Utils.color(getFilledInPrompt());
                int days = this.plugin.getConfig().getInt("expiration-days");
                Timestamp expiration = Timestamp.from(Instant.now().plus(days, ChronoUnit.DAYS));

                Observation.createObservation(this.plugin, player, player.getLocation(), text, expiration, this.template);

                Utils.msg(player,
                        "&7Your observation has been placed:",
                        "  &8\"&f&l" + text + "&8\"");
                destroySelection();
            };

            builder.append(createComponent(
                    "&a&l" + CHECK + " Confirm",
                    "&aClick to submit your observation!",
                    confirmCallback))
                    .append("  ");
        }

        Consumer<Player> cancelCallback = p -> {
            Utils.msg(p, "Observation canceled!");
            destroySelection();
        };

        builder.append(createComponent(
                "&c&l" + CROSS + " Cancel",
                "&cClick to cancel your observation",
                cancelCallback));

        if (goBackCallback != null) {
            builder.append("  ").append(createComponent(
                    "&e&l< Go Back",
                    "&eClick to go back to the previous panel",
                    goBackCallback));
        }

        Utils.msgNoPrefix(player, "");
        player.spigot().sendMessage(builder.create());
        CenteredText.sendCenteredMessage(player, " &7Click to make a selection ", "&7&m &r");
    }

    private Player getPlayer() {
        return Bukkit.getPlayer(this.uuid);
    }

    private void destroySelection() {
        // Clear callbacks
        this.spigotCallback.clearCallbacks(getPlayer());

        // Remove this as an ongoing selection
        ongoingSelections.remove(this.uuid);

        // Unregister events
        PlayerChangedWorldEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
    }

    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!this.uuid.equals(player.getUniqueId())) {
            return;
        }

        Utils.msg(player, "Your observation has been canceled because you changed worlds!");
        destroySelection();
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        if (this.uuid.equals(event.getPlayer().getUniqueId())) {
            destroySelection();
        }
    }

}
