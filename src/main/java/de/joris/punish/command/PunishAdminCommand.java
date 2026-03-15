package de.joris.punish.command;

import de.joris.punish.Punish;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;

public class PunishAdminCommand implements CommandExecutor, TabCompleter {

    private final Punish plugin;
    private static final List<String> VALID_LANGS = Arrays.asList("de", "en");

    public PunishAdminCommand(Punish plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punish.admin")) {
            msg(sender, plugin.getPrefix() + plugin.msg("admin.no-permission"));
            return true;
        }

        if (args.length == 0) {
            msg(sender, plugin.getPrefix() + plugin.msg("admin.usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadPlugin();
                msg(sender, plugin.getPrefix() + plugin.msg("admin.reload-success"));
            }
            case "lang" -> {
                if (args.length < 2) {
                    msg(sender, plugin.getPrefix() + plugin.msg("admin.lang-usage"));
                    return true;
                }
                String lang = args[1].toLowerCase();
                if (!VALID_LANGS.contains(lang)) {
                    msg(sender, plugin.getPrefix() + plugin.msg("admin.lang-invalid"));
                    return true;
                }
                plugin.getConfig().set("language", lang);
                plugin.saveConfig();
                msg(sender, plugin.getPrefix() + plugin.msg("admin.lang-success", "lang", lang));
            }
            default -> msg(sender, plugin.getPrefix() + plugin.msg("admin.usage"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "lang").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("lang")) {
            return VALID_LANGS.stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return List.of();
    }

    private void msg(CommandSender sender, String s) {
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(s));
    }
}
