package de.joris.punish.command;

import de.joris.punish.Punish;
import de.joris.punish.manager.DataManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BanLogsCommand implements CommandExecutor, TabCompleter {

    private final Punish plugin;

    public BanLogsCommand(Punish plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            msg(sender, plugin.getPrefix() + plugin.msg("banlogs.usage"));
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
        msg(sender, plugin.getPrefix() + plugin.msg("banlogs.header", "player", name));

        // Ban History
        msg(sender, "");
        msg(sender, plugin.msg("banlogs.ban-history-header"));
        List<String> banHist = dm.getBanHistory(uuid);
        if (banHist.isEmpty()) {
            msg(sender, plugin.msg("banlogs.ban-history-none"));
        } else {
            int start = Math.max(0, banHist.size() - 10);
            for (int i = banHist.size() - 1; i >= start; i--) {
                msg(sender, plugin.msg("banlogs.ban-entry", "entry", banHist.get(i)));
            }
        }

        // Mute History
        msg(sender, "");
        msg(sender, plugin.msg("banlogs.mute-history-header"));
        List<String> muteHist = dm.getMuteHistory(uuid);
        if (muteHist.isEmpty()) {
            msg(sender, plugin.msg("banlogs.mute-history-none"));
        } else {
            int start = Math.max(0, muteHist.size() - 10);
            for (int i = muteHist.size() - 1; i >= start; i--) {
                msg(sender, plugin.msg("banlogs.mute-entry", "entry", muteHist.get(i)));
            }
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
