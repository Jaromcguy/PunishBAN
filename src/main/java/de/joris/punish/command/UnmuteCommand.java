package de.joris.punish.command;

import de.joris.punish.Punish;
import de.joris.punish.manager.DataManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class UnmuteCommand implements CommandExecutor, TabCompleter {

    private final Punish plugin;

    public UnmuteCommand(Punish plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            msg(sender, plugin.getPrefix() + plugin.msg("unmute.usage"));
            return true;
        }

        DataManager dm = plugin.getDataManager();
        UUID uuid = dm.getUuidByName(args[0]);

        if (uuid == null) {
            msg(sender, plugin.getPrefix() + plugin.msg("general.player-not-found"));
            return true;
        }

        if (!dm.isMuted(uuid)) {
            msg(sender, plugin.getPrefix() + plugin.msg("unmute.not-muted", "player", args[0]));
            return true;
        }

        dm.removeMute(uuid);
        String byName = sender instanceof Player ? sender.getName() : "CONSOLE";
        dm.addMuteHistory(uuid, "UNMUTE | by=" + byName + " | " + dm.formatTimestamp(System.currentTimeMillis()));
        msg(sender, plugin.getPrefix() + plugin.msg("unmute.success", "player", args[0]));
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
