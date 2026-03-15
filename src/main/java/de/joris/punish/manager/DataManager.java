package de.joris.punish.manager;

import de.joris.punish.Punish;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DataManager {

    private final Punish plugin;
    private File playersFile, bansFile, mutesFile, historyFile;
    private YamlConfiguration playersConfig, bansConfig, mutesConfig, historyConfig;
    private final Map<String, UUID> nameToUuid = new HashMap<>();
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    public DataManager(Punish plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        File dataFolder = new File(plugin.getDataFolder(), "data");
        dataFolder.mkdirs();

        playersFile = new File(dataFolder, "players.yml");
        bansFile = new File(dataFolder, "bans.yml");
        mutesFile = new File(dataFolder, "mutes.yml");
        historyFile = new File(dataFolder, "history.yml");

        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        bansConfig = YamlConfiguration.loadConfiguration(bansFile);
        mutesConfig = YamlConfiguration.loadConfiguration(mutesFile);
        historyConfig = YamlConfiguration.loadConfiguration(historyFile);

        for (String key : playersConfig.getKeys(false)) {
            String name = playersConfig.getString(key + ".name");
            if (name != null) {
                try {
                    nameToUuid.put(name.toLowerCase(), UUID.fromString(key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public void saveAll() {
        save(playersConfig, playersFile);
        save(bansConfig, bansFile);
        save(mutesConfig, mutesFile);
        save(historyConfig, historyFile);
    }

    private void save(YamlConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save " + file.getName() + ": " + e.getMessage());
        }
    }

    // ===== Player Data =====

    public void updatePlayer(UUID uuid, String name, String ip) {
        String key = uuid.toString();
        playersConfig.set(key + ".name", name);
        nameToUuid.put(name.toLowerCase(), uuid);
        if (ip != null) {
            playersConfig.set(key + ".ip", ip);
        }
        if (!playersConfig.contains(key + ".first-login")) {
            playersConfig.set(key + ".first-login", System.currentTimeMillis());
        }
        playersConfig.set(key + ".last-login", System.currentTimeMillis());
        save(playersConfig, playersFile);
    }

    public UUID getUuidByName(String name) {
        return nameToUuid.get(name.toLowerCase());
    }

    public String getPlayerName(UUID uuid) {
        return playersConfig.getString(uuid.toString() + ".name");
    }

    public String getPlayerIp(UUID uuid) {
        return playersConfig.getString(uuid.toString() + ".ip");
    }

    public long getFirstLogin(UUID uuid) {
        return playersConfig.getLong(uuid.toString() + ".first-login", 0);
    }

    public long getLastLogin(UUID uuid) {
        return playersConfig.getLong(uuid.toString() + ".last-login", 0);
    }

    public List<String> getAltNames(UUID uuid) {
        String ip = getPlayerIp(uuid);
        if (ip == null) return Collections.emptyList();
        List<String> alts = new ArrayList<>();
        for (String key : playersConfig.getKeys(false)) {
            if (key.equals(uuid.toString())) continue;
            if (ip.equals(playersConfig.getString(key + ".ip"))) {
                String n = playersConfig.getString(key + ".name");
                if (n != null) alts.add(n);
            }
        }
        return alts;
    }

    // ===== Bans =====

    public void setBan(UUID uuid, String by, String reason, long startMs, long endMs) {
        String key = uuid.toString();
        bansConfig.set(key + ".active", true);
        bansConfig.set(key + ".by", by);
        bansConfig.set(key + ".reason", reason);
        bansConfig.set(key + ".start", startMs);
        bansConfig.set(key + ".end", endMs);
        save(bansConfig, bansFile);
    }

    public void removeBan(UUID uuid) {
        bansConfig.set(uuid.toString(), null);
        save(bansConfig, bansFile);
    }

    public boolean isBanned(UUID uuid) {
        return bansConfig.getBoolean(uuid.toString() + ".active", false);
    }

    public long getBanEnd(UUID uuid) {
        return bansConfig.getLong(uuid.toString() + ".end", 0);
    }

    public String getBanReason(UUID uuid) {
        return bansConfig.getString(uuid.toString() + ".reason", "Unbekannt");
    }

    public String getBanBy(UUID uuid) {
        return bansConfig.getString(uuid.toString() + ".by", "Unbekannt");
    }

    public long getBanStart(UUID uuid) {
        return bansConfig.getLong(uuid.toString() + ".start", 0);
    }

    // ===== Mutes =====

    public void setMute(UUID uuid, String by, String reason, long startMs, long endMs) {
        String key = uuid.toString();
        mutesConfig.set(key + ".active", true);
        mutesConfig.set(key + ".by", by);
        mutesConfig.set(key + ".reason", reason);
        mutesConfig.set(key + ".start", startMs);
        mutesConfig.set(key + ".end", endMs);
        save(mutesConfig, mutesFile);
    }

    public void removeMute(UUID uuid) {
        mutesConfig.set(uuid.toString(), null);
        save(mutesConfig, mutesFile);
    }

    public boolean isMuted(UUID uuid) {
        return mutesConfig.getBoolean(uuid.toString() + ".active", false);
    }

    public long getMuteEnd(UUID uuid) {
        return mutesConfig.getLong(uuid.toString() + ".end", 0);
    }

    public String getMuteReason(UUID uuid) {
        return mutesConfig.getString(uuid.toString() + ".reason", "Unbekannt");
    }

    public String getMuteBy(UUID uuid) {
        return mutesConfig.getString(uuid.toString() + ".by", "Unbekannt");
    }

    // ===== History =====

    public void addBanHistory(UUID uuid, String line) {
        List<String> history = historyConfig.getStringList(uuid.toString() + ".bans");
        history.add(line);
        historyConfig.set(uuid.toString() + ".bans", history);
        save(historyConfig, historyFile);
    }

    public List<String> getBanHistory(UUID uuid) {
        return historyConfig.getStringList(uuid.toString() + ".bans");
    }

    public void addMuteHistory(UUID uuid, String line) {
        List<String> history = historyConfig.getStringList(uuid.toString() + ".mutes");
        history.add(line);
        historyConfig.set(uuid.toString() + ".mutes", history);
        save(historyConfig, historyFile);
    }

    public List<String> getMuteHistory(UUID uuid) {
        return historyConfig.getStringList(uuid.toString() + ".mutes");
    }

    // ===== Utility =====

    public String formatTimestamp(long millis) {
        if (millis <= 0) return "Unbekannt";
        return FORMATTER.format(Instant.ofEpochMilli(millis));
    }
}
