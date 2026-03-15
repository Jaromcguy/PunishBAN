package de.joris.punish.listener;

import de.joris.punish.Punish;
import de.joris.punish.manager.DataManager;
import de.joris.punish.util.TimeUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatListener implements Listener {

    private final Punish plugin;

    // Spam-Schutz: Cooldown pro Spieler fuer Benachrichtigungen (5 Sekunden)
    private final Map<UUID, Long> notifyCooldown = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 5000;

    // Warning-Tracking: UUID -> Liste von Timestamps (letzte 24h)
    private final Map<UUID, List<Long>> warningTracker = new ConcurrentHashMap<>();
    private static final long WARNING_WINDOW_MS = 24 * 60 * 60 * 1000; // 24h
    private static final int AUTO_BAN_THRESHOLD = 3;

    public ChatListener(Punish plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        DataManager dm = plugin.getDataManager();

        // Spieler mit notify-Permission ueberspringen
        if (player.hasPermission("punish.notify")) return;

        // Mute check
        if (dm.isMuted(uuid)) {
            long endMs = dm.getMuteEnd(uuid);
            if (endMs > System.currentTimeMillis()) {
                event.setCancelled(true);
                player.sendMessage(leg(plugin.getPrefix() + plugin.msg("chat.muted", "reason", dm.getMuteReason(uuid), "discord", plugin.getDiscord())));
                return;
            } else {
                dm.removeMute(uuid);
            }
        }

        // Chat filter - case insensitive, Sonderzeichen normalisieren
        String messageText = normalize(toPlainText(event.originalMessage()));
        ConfigurationSection filterSection = plugin.getConfig().getConfigurationSection("filter");
        if (filterSection == null) return;

        for (String category : filterSection.getKeys(false)) {
            List<String> words = filterSection.getStringList(category);
            for (String word : words) {
                if (!word.isEmpty() && messageText.contains(normalize(word))) {
                    handleFilterHit(player, uuid, toPlainText(event.originalMessage()), category, dm);
                    return;
                }
            }
        }
    }

    private void handleFilterHit(Player player, UUID uuid, String message, String category, DataManager dm) {
        long now = System.currentTimeMillis();

        // RA (Rassismus) = sofort Perma-Ban, kein Warning-System
        if (category.equalsIgnoreCase("RA")) {
            warningTracker.remove(uuid);
            notifyCooldown.remove(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> autoBan(player, uuid, category, dm, true));
            return;
        }

        // Warning IMMER tracken, unabhaengig vom Cooldown
        List<Long> warnings = warningTracker.computeIfAbsent(uuid, k -> Collections.synchronizedList(new ArrayList<>()));
        warnings.removeIf(ts -> (now - ts) > WARNING_WINDOW_MS);
        warnings.add(now);
        int count = warnings.size();

        // Auto-Ban bei >= 3 Warnings in 24h (sofort, vor Notification)
        if (count >= AUTO_BAN_THRESHOLD) {
            warnings.clear();
            notifyCooldown.remove(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> autoBan(player, uuid, category, dm, false));
            return;
        }

        // Spam-Schutz: Cooldown nur fuer Staff-Benachrichtigungen
        Long lastNotify = notifyCooldown.get(uuid);
        if (lastNotify != null && (now - lastNotify) < COOLDOWN_MS) {
            return;
        }
        notifyCooldown.put(uuid, now);

        // Staff benachrichtigen
        notifyStaff(player, message, category, count);
    }

    private void autoBan(Player player, UUID uuid, String category, DataManager dm, boolean permanent) {
        ConfigurationSection presets = plugin.getConfig().getConfigurationSection("ban-presets");
        String displayReason = category;

        if (presets != null && presets.contains(category)) {
            displayReason = presets.getString(category + ".display", category);
        }

        long durationSec = permanent ? -1 : 86400;
        String fullReason = displayReason;
        long nowMs = System.currentTimeMillis();
        long endMs = (durationSec == -1) ? -1 : nowMs + (durationSec * 1000);

        dm.setBan(uuid, "SYSTEM", fullReason, nowMs, endMs);
        dm.addBanHistory(uuid, (durationSec == -1 ? "PERMABAN" : "TEMPBAN (" + TimeUtil.formatSeconds(durationSec) + ")")
                + " | by=SYSTEM (Auto) | " + fullReason + " | " + dm.formatTimestamp(nowMs));

        // Notify staff
        Component bc = leg(plugin.msg("chat.auto-ban-notify", "player", player.getName()));
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("punish.notify")) p.sendMessage(bc);
        }

        // Kick
        if (player.isOnline()) {
            String kickMsg = (durationSec == -1)
                    ? plugin.msg("chat.auto-ban-kick-perm", "reason", fullReason, "discord", plugin.getDiscord())
                    : plugin.msg("chat.auto-ban-kick-temp", "reason", fullReason, "discord", plugin.getDiscord());
            player.kick(leg(kickMsg));
        }
    }

    private void notifyStaff(Player player, String message, String category, int warningCount) {
        // [Chat] player: message
        Component line1 = leg(plugin.msg("chat.notify-line1", "player", player.getName(), "message", message));
        // [Chat] HR   [BAN] [MUTE]
        Component categoryTag = leg(plugin.msg("chat.notify-line2-prefix", "category", category));

        Component banButton = Component.text("[BAN]")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.suggestCommand("/ban " + player.getName() + " " + category + " "))
                .hoverEvent(HoverEvent.showText(Component.text(plugin.msg("chat.notify-ban-hover"), NamedTextColor.RED)));

        Component muteButton = Component.text("[MUTE]")
                .color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.suggestCommand("/mute " + player.getName() + " " + category + " "))
                .hoverEvent(HoverEvent.showText(Component.text(plugin.msg("chat.notify-mute-hover"), NamedTextColor.YELLOW)));

        Component line2 = categoryTag.append(banButton).append(Component.text(" ")).append(muteButton);

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("punish.notify")) {
                staff.sendMessage(line1);
                staff.sendMessage(line2);
            }
        }
    }

    /**
     * Normalisiert Text: lowercase, Leetspeak ersetzen, Leerzeichen behalten
     */
    private String normalize(String input) {
        return input.toLowerCase()
                .replace("1", "i")
                .replace("3", "e")
                .replace("4", "a")
                .replace("0", "o")
                .replace("@", "a")
                .replace("$", "s")
                .replace("!", "i")
                .replaceAll("[^a-zäöüß/.\\s]", "");
    }

    private String toPlainText(Component component) {
        StringBuilder sb = new StringBuilder();
        extractText(component, sb);
        return sb.toString();
    }

    private void extractText(Component component, StringBuilder sb) {
        if (component instanceof TextComponent tc) {
            sb.append(tc.content());
        }
        for (Component child : component.children()) {
            extractText(child, sb);
        }
    }

    private Component leg(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }
}
