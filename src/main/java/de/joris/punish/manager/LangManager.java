package de.joris.punish.manager;

import de.joris.punish.Punish;

public class LangManager {

    private final Punish plugin;

    public LangManager(Punish plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns the message for the given key in the configured language.
     * Replacements are key-value pairs: get("key", "player", "Steve", "reason", "Spam")
     * replaces {player} with Steve and {reason} with Spam.
     * Falls back to "de" if the key is missing in the active language.
     */
    public String get(String key, String... replacements) {
        String lang = plugin.getConfig().getString("language", "de");
        String msg = plugin.getConfig().getString("messages." + lang + "." + key);
        if (msg == null) {
            msg = plugin.getConfig().getString("messages.de." + key, key);
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return msg;
    }
}
