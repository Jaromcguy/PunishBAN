package de.joris.punish.command;

import de.joris.punish.Punish;
import de.joris.punish.manager.DataManager;
import de.joris.punish.util.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class MuteCommand implements CommandExecutor, TabCompleter {

    private final Punish plugin;

    public MuteCommand(Punish plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            msg(sender, plugin.getPrefix() + plugin.msg("mute.usage"));
            msg(sender, plugin.msg("mute.usage-presets"));
            return true;
        }

        DataManager dm = plugin.getDataManager();
        String targetName = args[0];

        // Resolve target (online or offline)
        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        UUID targetUuid;
        String displayName;

        if (onlineTarget != null) {
            targetUuid = onlineTarget.getUniqueId();
            displayName = onlineTarget.getName();
        } else {
            targetUuid = dm.getUuidByName(targetName);
            if (targetUuid == null) {
                msg(sender, plugin.getPrefix() + plugin.msg("general.player-not-found"));
                return true;
            }
            displayName = dm.getPlayerName(targetUuid);
        }

        // Resolve preset
        ConfigurationSection presets = plugin.getConfig().getConfigurationSection("mute-presets");
        String presetKey = findPresetKey(presets, args[1]);
        if (presetKey == null) {
            msg(sender, plugin.getPrefix() + plugin.msg("mute.unknown-preset", "preset", args[1]));
            msg(sender, plugin.msg("mute.usage-presets"));
            return true;
        }

        String displayReason = presets.getString(presetKey + ".display", args[1]);
        String defaultDuration = presets.getString(presetKey + ".duration", "1d");

        // Parse [dauer] [grund...]
        long durationSec;
        String customReason = null;

        if (args.length >= 3 && TimeUtil.isDurationString(args[2])) {
            durationSec = TimeUtil.parseDuration(args[2]);
            if (args.length >= 4) {
                customReason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
            }
        } else if (args.length >= 3) {
            durationSec = TimeUtil.parseDuration(defaultDuration);
            customReason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        } else {
            durationSec = TimeUtil.parseDuration(defaultDuration);
        }

        if (durationSec <= 0) {
            durationSec = TimeUtil.parseDuration(defaultDuration);
            if (durationSec <= 0) durationSec = 86400; // 1 day fallback
        }

        String fullReason = customReason != null && !customReason.isEmpty()
                ? displayReason + " - " + customReason
                : displayReason;

        // Apply mute
        long nowMs = System.currentTimeMillis();
        long endMs = nowMs + (durationSec * 1000);
        String byName = sender instanceof Player ? sender.getName() : "CONSOLE";

        dm.setMute(targetUuid, byName, fullReason, nowMs, endMs);

        // History
        String timestamp = dm.formatTimestamp(nowMs);
        String historyLine = "MUTE (" + TimeUtil.formatSeconds(durationSec) + ") | by=" + byName + " | " + fullReason + " | " + timestamp;
        dm.addMuteHistory(targetUuid, historyLine);

        // Notify
        msg(sender, plugin.msg("mute.success"));

        if (onlineTarget != null) {
            onlineTarget.sendMessage(leg(plugin.getPrefix() + plugin.msg("mute.notify-target", "reason", fullReason)));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return null;
        if (args.length == 2) {
            ConfigurationSection sec = plugin.getConfig().getConfigurationSection("mute-presets");
            if (sec == null) return Collections.emptyList();
            return sec.getKeys(false).stream()
                    .filter(k -> k.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3) {
            return List.of("30m", "1h", "2h", "6h", "12h", "1d", "2d", "4d", "7d").stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private String findPresetKey(ConfigurationSection section, String input) {
        if (section == null) return null;
        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase(input)) return key;
        }
        return null;
    }

    private void msg(CommandSender sender, String s) {
        sender.sendMessage(leg(s));
    }

    private Component leg(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }
}
