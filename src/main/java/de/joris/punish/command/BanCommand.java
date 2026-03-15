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

public class BanCommand implements CommandExecutor, TabCompleter {

    private final Punish plugin;

    public BanCommand(Punish plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            msg(sender, plugin.getPrefix() + plugin.msg("ban.usage"));
            msg(sender, plugin.msg("ban.usage-presets"));
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
                msg(sender, plugin.getPrefix() + plugin.msg("general.player-not-found-joined"));
                return true;
            }
            displayName = dm.getPlayerName(targetUuid);
        }

        // Resolve preset
        ConfigurationSection presets = plugin.getConfig().getConfigurationSection("ban-presets");
        String presetKey = findPresetKey(presets, args[1]);
        if (presetKey == null) {
            msg(sender, plugin.getPrefix() + plugin.msg("ban.unknown-preset", "preset", args[1]));
            msg(sender, plugin.msg("ban.usage-presets"));
            return true;
        }

        String displayReason = presets.getString(presetKey + ".display", args[1]);
        String defaultDuration = presets.getString(presetKey + ".duration", "1d");

        // Parse remaining args: [dauer] [grund...]
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

        if (durationSec == -2) durationSec = TimeUtil.parseDuration(defaultDuration);

        String fullReason = customReason != null && !customReason.isEmpty()
                ? displayReason + " - " + customReason
                : displayReason;

        // Check permanent keywords in reason
        for (String kw : plugin.getConfig().getStringList("permanent-keywords")) {
            if (fullReason.toLowerCase().contains(kw.toLowerCase())) {
                durationSec = -1;
                break;
            }
        }

        // Apply ban
        long nowMs = System.currentTimeMillis();
        long endMs = (durationSec == -1) ? -1 : nowMs + (durationSec * 1000);
        String byName = sender instanceof Player ? sender.getName() : "CONSOLE";

        dm.setBan(targetUuid, byName, fullReason, nowMs, endMs);

        // History
        String timestamp = dm.formatTimestamp(nowMs);
        String historyLine;
        if (durationSec == -1) {
            historyLine = "PERMABAN | by=" + byName + " | " + fullReason + " | " + timestamp;
        } else {
            historyLine = "TEMPBAN (" + TimeUtil.formatSeconds(durationSec) + ") | by=" + byName + " | " + fullReason + " | " + timestamp;
        }
        if (onlineTarget == null) historyLine += " (offline)";
        dm.addBanHistory(targetUuid, historyLine);

        // Broadcast
        String suffix = onlineTarget == null ? plugin.msg("general.offline-suffix") : "";
        if (durationSec == -1) {
            broadcast(plugin.getPrefix() + plugin.msg("ban.broadcast-perm", "player", displayName, "suffix", suffix));
        } else {
            broadcast(plugin.getPrefix() + plugin.msg("ban.broadcast-temp", "player", displayName, "time", TimeUtil.formatSeconds(durationSec), "suffix", suffix));
        }

        // Notify sender
        msg(sender, plugin.msg("ban.success"));

        // Kick if online
        if (onlineTarget != null) {
            String kickMsg;
            if (durationSec == -1) {
                kickMsg = plugin.msg("ban.kick-perm", "reason", fullReason, "discord", plugin.getDiscord());
            } else {
                kickMsg = plugin.msg("ban.kick-temp", "time", TimeUtil.formatSeconds(durationSec), "reason", fullReason, "discord", plugin.getDiscord());
            }
            onlineTarget.kick(leg(kickMsg));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return null; // default: online players
        if (args.length == 2) {
            ConfigurationSection sec = plugin.getConfig().getConfigurationSection("ban-presets");
            if (sec == null) return Collections.emptyList();
            return sec.getKeys(false).stream()
                    .filter(k -> k.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3) {
            return List.of("1d", "2d", "3d", "4d", "7d", "14d", "30d", "permanent").stream()
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

    private void broadcast(String s) {
        Component comp = leg(s);
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(comp);
        Bukkit.getConsoleSender().sendMessage(comp);
    }

    private Component leg(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }
}
