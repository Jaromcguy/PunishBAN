package de.joris.punish;

import de.joris.punish.command.*;
import de.joris.punish.listener.ChatListener;
import de.joris.punish.listener.JoinListener;
import de.joris.punish.manager.DataManager;
import de.joris.punish.manager.LangManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public class Punish extends JavaPlugin {

    private static Punish instance;
    private DataManager dataManager;
    private LangManager langManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        dataManager = new DataManager(this);
        dataManager.loadAll();
        langManager = new LangManager(this);

        registerCommand("ban", new BanCommand(this));
        registerCommand("unban", new UnbanCommand(this));
        registerCommand("mute", new MuteCommand(this));
        registerCommand("unmute", new UnmuteCommand(this));
        registerCommand("playerinfo", new PlayerInfoCommand(this));
        registerCommand("banlogs", new BanLogsCommand(this));
        registerCommand("punish", new PunishAdminCommand(this));

        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        getLogger().info("Punish has been enabled!");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getLogger().info("Punish has been disabled!");
    }

    private void registerCommand(String name, Object executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor((CommandExecutor) executor);
            if (executor instanceof TabCompleter tc) {
                cmd.setTabCompleter(tc);
            }
        }
    }

    public static Punish getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }

    /** Reload config.yml from disk without restarting the plugin. */
    public void reloadPlugin() {
        reloadConfig();
        langManager = new LangManager(this);
    }

    /**
     * Shortcut for {@code getLangManager().get(key, replacements)}.
     * Replacements are key-value pairs: msg("ban.success"), msg("ban.unknown-preset", "preset", name)
     */
    public String msg(String key, String... replacements) {
        return langManager.get(key, replacements);
    }

    public String getPrefix() {
        return getConfig().getString("prefix", "&8[&4&lPUNISH&8] &7");
    }

    public String getDiscord() {
        return getConfig().getString("discord", "https://discord.gg/96cJ9jQYJ4");
    }
}
