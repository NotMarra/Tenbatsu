package dev.notmarra.tenbatsu.utils;

import java.time.Instant;

public class DurationParser {

    /**
     * Parses duration string like "1d", "2h30m", "30m", "permanent"/"perm"
     * Returns epoch second of expiry, or -1 for permanent.
     */
    public static long parse(String input) {
        if (input == null) return -1;
        String lower = input.toLowerCase().trim();
        if (lower.equals("permanent") || lower.equals("perm") || lower.equals("-1")) {
            return -1;
        }

        long totalSeconds = 0;
        StringBuilder num = new StringBuilder();

        for (char c : lower.toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
            } else {
                if (num.isEmpty()) continue;
                long value = Long.parseLong(num.toString());
                num.setLength(0);
                switch (c) {
                    case 's' -> totalSeconds += value;
                    case 'm' -> totalSeconds += value * 60;
                    case 'h' -> totalSeconds += value * 3600;
                    case 'd' -> totalSeconds += value * 86400;
                    case 'w' -> totalSeconds += value * 604800;
                    case 'o' -> totalSeconds += value * 2592000L; // month
                    case 'y' -> totalSeconds += value * 31536000L;
                    default -> { return 0; } // invalid
                }
            }
        }

        if (totalSeconds == 0) return 0; // invalid
        return Instant.now().getEpochSecond() + totalSeconds;
    }

    public static boolean isValid(String input) {
        if (input == null || input.isBlank()) return false;
        String lower = input.toLowerCase().trim();
        if (lower.equals("permanent") || lower.equals("perm") || lower.equals("-1")) return true;
        return input.matches("(?:\\d+[smhdwoy])+");
    }

    /**
     * Formats remaining time from epoch second to human-readable string
     */
    public static String format(long expiresAt) {
        if (expiresAt == -1) return "Permanent";
        long remaining = expiresAt - Instant.now().getEpochSecond();
        if (remaining <= 0) return "Expired";

        long days = remaining / 86400;
        long hours = (remaining % 86400) / 3600;
        long minutes = (remaining % 3600) / 60;
        long seconds = remaining % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (days == 0 && seconds > 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
