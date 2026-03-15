package de.joris.punish.command;

import de.joris.punish.Punish;
import de.joris.punish.manager.DataManager;
import de.joris.punish.util.TimeUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PlayerInfoCommand implements CommandExecutor, TabCompleter {

    private final Punish plugin;

    public PlayerInfoCommand(Punish plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            msg(sender, plugin.getPrefix() + plugin.msg("playerinfo.usage"));
            return true;
        }

        DataManager dm = plugin.getDataManager();
        UUID uuid = dm.getUuidByName(args[0]);

        if (uuid == null) {
            msg(sender, plugin.getPrefix() + plugin.msg("general.player-not-found"));
            return true;
        }

        String name = dm.getPlayerName(uuid);
        String sep = plugin.msg("general.separator");

        msg(sender, sep);
        msg(sender, plugin.getPrefix() + plugin.msg("playerinfo.header", "player", name));
        msg(sender, plugin.msg("playerinfo.uuid", "uuid", uuid.toString()));
        msg(sender, plugin.msg("playerinfo.first-login", "time", dm.formatTimestamp(dm.getFirstLogin(uuid))));
        msg(sender, plugin.msg("playerinfo.last-login", "time", dm.formatTimestamp(dm.getLastLogin(uuid))));

        // Ban status
        if (dm.isBanned(uuid)) {
            long endMs = dm.getBanEnd(uuid);
            if (endMs != -1 && endMs <= System.currentTimeMillis()) {
                dm.removeBan(uuid);
                msg(sender, plugin.msg("playerinfo.ban-none"));
            } else {
                msg(sender, plugin.msg("playerinfo.ban-active"));
                msg(sender, plugin.msg("playerinfo.ban-reason", "reason", dm.getBanReason(uuid)));
                msg(sender, plugin.msg("playerinfo.ban-by", "by", dm.getBanBy(uuid)));
                if (endMs == -1) {
                    msg(sender, plugin.msg("playerinfo.ban-perm"));
                } else {
                    long leftSec = (endMs - System.currentTimeMillis()) / 1000;
                    msg(sender, plugin.msg("playerinfo.ban-time-left", "time", TimeUtil.formatSeconds(Math.max(0, leftSec))));
                }
            }
        } else {
            msg(sender, plugin.msg("playerinfo.ban-none"));
        }

        // Mute status
        if (dm.isMuted(uuid)) {
            long endMs = dm.getMuteEnd(uuid);
            if (endMs <= System.currentTimeMillis()) {
                dm.removeMute(uuid);
                msg(sender, plugin.msg("playerinfo.mute-none"));
            } else {
                msg(sender, plugin.msg("playerinfo.mute-active"));
                msg(sender, plugin.msg("playerinfo.mute-reason", "reason", dm.getMuteReason(uuid)));
                msg(sender, plugin.msg("playerinfo.mute-by", "by", dm.getMuteBy(uuid)));
                long leftSec = (endMs - System.currentTimeMillis()) / 1000;
                msg(sender, plugin.msg("playerinfo.mute-time-left", "time", TimeUtil.formatSeconds(Math.max(0, leftSec))));
            }
        } else {
            msg(sender, plugin.msg("playerinfo.mute-none"));
        }

        // Alt accounts
        List<String> alts = dm.getAltNames(uuid);
        msg(sender, "");
        msg(sender, plugin.msg("playerinfo.alts-header"));
        if (alts.isEmpty()) {
            msg(sender, plugin.msg("playerinfo.alts-none"));
        } else {
            for (String alt : alts) {
                msg(sender, plugin.msg("playerinfo.alt-entry", "name", alt));
            }
        }

        // Last ban from history
        List<String> banHist = dm.getBanHistory(uuid);
        msg(sender, "");
        if (!banHist.isEmpty()) {
            msg(sender, plugin.msg("playerinfo.last-ban-entry", "entry", banHist.get(banHist.size() - 1)));
        } else {
            msg(sender, plugin.msg("playerinfo.last-ban-none"));
        }

        // Last mute from history
        List<String> muteHist = dm.getMuteHistory(uuid);
        if (!muteHist.isEmpty()) {
            msg(sender, plugin.msg("playerinfo.last-mute-entry", "entry", muteHist.get(muteHist.size() - 1)));
        } else {
            msg(sender, plugin.msg("playerinfo.last-mute-none"));
        }

        msg(sender, sep);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return args.length == 1 ? null : Collections.emptyList();
    }

    private void msg(CommandSender sender, String s) {
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(s));
    }
}
