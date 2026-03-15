package de.joris.punish.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+)([dhm])$", Pattern.CASE_INSENSITIVE);

    /**
     * Parses a duration string into seconds.
     * Returns -1 for permanent, -2 for invalid input.
     */
    public static long parseDuration(String input) {
        if (input == null) return -2;
        if (input.equals("-1") || input.equalsIgnoreCase("permanent") || input.equalsIgnoreCase("perma")) {
            return -1;
        }
        Matcher m = DURATION_PATTERN.matcher(input);
        if (!m.matches()) return -2;
        long amount = Long.parseLong(m.group(1));
        return switch (m.group(2).toLowerCase()) {
            case "d" -> amount * 86400;
            case "h" -> amount * 3600;
            case "m" -> amount * 60;
            default -> -2;
        };
    }

    public static boolean isDurationString(String input) {
        if (input == null) return false;
        if (input.equals("-1") || input.equalsIgnoreCase("permanent") || input.equalsIgnoreCase("perma")) return true;
        return DURATION_PATTERN.matcher(input).matches();
    }

    public static String formatSeconds(long totalSeconds) {
        if (totalSeconds <= 0) return "0m";
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0 || sb.isEmpty()) sb.append(minutes).append("m");
        return sb.toString().trim();
    }
}
