package edu.whimc.observationdisplayer.commands.observetemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.World;

public class ObservationPrompt {

    public static final String FILLIN = "{}";

    private String prompt;

    private Map<World, Map<Integer, List<String>>> responses;

    @SuppressWarnings("unchecked")
    public ObservationPrompt(Map<?, ?> entry) {
        // This is pretty hacky and will blow up if the config does not match the intended format
        this.responses = new HashMap<>();
        this.prompt = (String) entry.get("prompt");

        Map<String, Object> worlds = (Map<String, Object>) entry.get("worlds");
        for (String worldName : worlds.keySet()) {
            World world = Bukkit.getWorld(worldName);
            this.responses.put(world, (Map<Integer, List<String>>) worlds.get(worldName));
        }
    }

    public String getPrompt() {
        return this.prompt;
    }

    public List<String> getResponses(World world, int position) {
        if (!this.responses.containsKey(world)) {
            return Arrays.asList();
        }

        return this.responses.get(world).getOrDefault(position, Arrays.asList());
    }

    public int getNumberOfFillIns() {
        String temp = this.prompt.replace(FILLIN, "");
        return (this.prompt.length() - temp.length()) / FILLIN.length();
    }

}