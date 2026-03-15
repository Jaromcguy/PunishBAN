package de.joris.punish.listener;

import de.joris.punish.Punish;
import de.joris.punish.manager.DataManager;
import de.joris.punish.util.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class JoinListener implements Listener {

    private final Punish plugin;

    public JoinListener(Punish plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        DataManager dm = plugin.getDataManager();

        // Update player data
        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null;
        dm.updatePlayer(uuid, player.getName(), ip);

        // Ban check
        if (dm.isBanned(uuid)) {
            long endMs = dm.getBanEnd(uuid);
            String reason = dm.getBanReason(uuid);
            event.joinMessage(null);

            if (endMs == -1) {
                player.kick(leg(plugin.msg("join.ban-kick-perm", "reason", reason, "discord", plugin.getDiscord())));
                return;
            }

            if (endMs > System.currentTimeMillis()) {
                long leftSeconds = (endMs - System.currentTimeMillis()) / 1000;
                String timeStr = TimeUtil.formatSeconds(leftSeconds);
                player.kick(leg(plugin.msg("join.ban-kick-temp", "reason", reason, "time", timeStr, "discord", plugin.getDiscord())));
                return;
            }

            // Ban expired
            dm.removeBan(uuid);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (plugin.getDataManager().isBanned(uuid)) {
            event.quitMessage(null);
        }
    }

    private Component leg(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }
}
