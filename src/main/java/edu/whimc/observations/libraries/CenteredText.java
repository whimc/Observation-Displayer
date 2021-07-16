package edu.whimc.observations.libraries;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

public final class CenteredText {

    private final static int CENTER_PX = 158;

    public static int getMessagePxSize(String message) {
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for (char c : message.toCharArray()) {
            if (c == '§') {
                previousCode = true;
                continue;
            }

            if (previousCode) {
                previousCode = false;
                isBold = c == 'l' || c == 'L';
            } else {
                DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
        }

        return messagePxSize;
    }

    public static void sendCenteredMessage(Player player, String message, String padding) {
        if (message == null || message.isEmpty()) {
            message = "";
        }

        message = ChatColor.translateAlternateColorCodes('&', message);
        padding = ChatColor.translateAlternateColorCodes('&', padding);

        int messagePxSize = getMessagePxSize(message);
        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = CENTER_PX - halvedMessageSize;
        int paddingLength = getMessagePxSize(padding);
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while (compensated < toCompensate) {
            sb.append(padding);
            compensated += paddingLength;
        }
        player.sendMessage(sb + message + sb);
    }

}
