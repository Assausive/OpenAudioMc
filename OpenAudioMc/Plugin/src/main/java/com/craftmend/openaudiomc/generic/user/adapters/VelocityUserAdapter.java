package com.craftmend.openaudiomc.generic.user.adapters;

import com.craftmend.openaudiomc.api.basic.ActorCategory;
import com.craftmend.openaudiomc.api.user.User;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Objects;
import java.util.UUID;

import static com.craftmend.openaudiomc.generic.platform.Platform.translateColors;

@AllArgsConstructor
public class VelocityUserAdapter implements User<CommandSource> {

    // parses the §-codes our messages carry, including the §x§r§r§g§g§b§b hex form.
    // sending those as plain text would make the client read each pair as a separate
    // legacy color, so the last digit of the hex would win instead of the actual color
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§')
            .hexCharacter('#')
            .hexColors()
            .build();

    // note: always build click/hover events through the static factories below, never through
    // ClickEvent.Action/HoverEvent.Action constants. we compile against the adventure bundled with
    // velocity-api 3.1.0, but newer proxies (velocity 4.x) ship adventure 5, where those enum
    // constants are gone and direct references blow up with a NoSuchFieldError at runtime
    private CommandSource sender;

    @Override
    public void sendMessage(String string) {
        for (String s : string.split("\\\\n")) {
            sender.sendMessage(LEGACY.deserialize(s));
        }
    }

    @Override
    public ActorCategory getCategory() {
        if (sender instanceof Player) {
            return ActorCategory.PLAYER;
        }
        return ActorCategory.CONSOLE;
    }

    @Override
    public void sendClickableCommandMessage(String t, String hoverMessage, String command) {
        String[] lines = t.split("\\\\n");
        if (lines.length > 1) {
            for (String line : lines) {
                sendClickableCommandMessage(line, hoverMessage, command);
            }
            return;
        }

        String msgText = translateColors(t);
        Component message = LEGACY.deserialize(Objects.requireNonNull(msgText));

        message = message.clickEvent(ClickEvent.runCommand("/" + command));

        String hoverText = translateColors(hoverMessage);
        message = message.hoverEvent(HoverEvent.showText(LEGACY.deserialize(Objects.requireNonNull(hoverText))));

        sender.sendMessage(message);
    }

    @Override
    public void sendClickableUrlMessage(String t, String hoverMessage, String url) {
        String[] lines = t.split("\\\\n");
        if (lines.length > 1) {
            for (String line : lines) {
                sendClickableUrlMessage(line, hoverMessage, url);
            }
            return;
        }

        String msgText = translateColors(t);
        Component message = LEGACY.deserialize(Objects.requireNonNull(msgText));

        message = message.clickEvent(ClickEvent.openUrl(url));

        String hoverText = translateColors(hoverMessage);
        message = message.hoverEvent(HoverEvent.showText(LEGACY.deserialize(Objects.requireNonNull(hoverText))));

        sender.sendMessage(message);
    }

    @Override
    public boolean isAdministrator() {
        return sender.hasPermission("openaudiomc.*") || sender.hasPermission("openaudiomc.tips");
    }

    @Override
    public boolean hasPermission(String permission) {
        return sender.hasPermission("openaudiomc.*") || sender.hasPermission(permission);
    }

    @Override
    public void makeExecuteCommand(String command) {
        // TODO: Possible?
    }

    @Override
    public UUID getUniqueId() {
        if (sender instanceof Player) {
            return ((Player) sender).getUniqueId();
        }
        return null;
    }

    @Override
    public String getIpAddress() {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.getRemoteAddress() == null) return "unknown";
            if (player.getRemoteAddress().getAddress() == null) return "unknown";
            String ip = player.getRemoteAddress().getAddress().getHostName();
            if (ip != null) {
                return ip;
            }
        }
        return "unknown";
    }

    @Override
    public String getWorld() {
        return "world"; // Velocity does not have a world concept like Bukkit, so we return a placeholder
    }

    @Override
    public CommandSource getOriginal() {
        return sender;
    }

    @Override
    public String getName() {
        if (sender instanceof Player) {
            return ((Player) sender).getUsername();
        }
        return "VelocityConsole";
    }
}
