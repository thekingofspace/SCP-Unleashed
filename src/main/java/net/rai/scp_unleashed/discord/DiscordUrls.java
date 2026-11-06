package net.rai.scp_unleashed.discord;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DiscordUrls {
    public static final Pattern WebhookPattern = Pattern.compile(
            "^https://(?:(?:ptb|canary)\\.)?(?:discord|discordapp)\\.com/api(?:/v\\d{1,2})?/webhooks/(\\d{5,25})/([A-Za-z0-9_\\-]{20,120})(?:\\?thread_id=\\d{5,25})?$");
    public static final Pattern LoopbackPattern = Pattern.compile(
            "^http://127\\.0\\.0\\.1:\\d{2,5}/api/webhooks/(\\d{1,25})/([A-Za-z0-9_\\-]{1,120})(?:\\?thread_id=\\d{1,25})?$");
    public static final Pattern ImagePattern = Pattern.compile("^https://[A-Za-z0-9.\\-]+(?::\\d{1,5})?/[^\\s\"<>]*$");
    public static final int MaxUrl = 512;

    private DiscordUrls() {
    }

    public static Optional<URI> parse(String Raw, boolean AllowLoopback) {
        if (Raw == null) {
            return Optional.empty();
        }
        String Clean = Raw.strip();
        if (Clean.isEmpty() || Clean.length() > MaxUrl) {
            return Optional.empty();
        }
        boolean Real = WebhookPattern.matcher(Clean).matches();
        boolean Local = AllowLoopback && LoopbackPattern.matcher(Clean).matches();
        if (!Real && !Local) {
            return Optional.empty();
        }
        try {
            return Optional.of(URI.create(Clean));
        } catch (IllegalArgumentException Problem) {
            return Optional.empty();
        }
    }

    public static boolean isImage(String Raw) {
        return Raw != null && Raw.length() <= MaxUrl && ImagePattern.matcher(Raw).matches();
    }

    public static String label(String Raw) {
        if (Raw == null || Raw.isBlank()) {
            return "none";
        }
        try {
            byte[] Digest = MessageDigest.getInstance("SHA-256").digest(Raw.strip().getBytes(StandardCharsets.UTF_8));
            return "webhook#" + HexFormat.of().formatHex(Digest, 0, 3);
        } catch (NoSuchAlgorithmException Problem) {
            return "webhook#" + Integer.toHexString(Raw.strip().hashCode() & 0xFFFFFF);
        }
    }

    public static String scrub(String Text, String Url) {
        if (Text == null) {
            return "";
        }
        String Clean = Text;
        if (Url != null && !Url.isBlank()) {
            String Trimmed = Url.strip();
            Clean = Clean.replace(Trimmed, label(Trimmed));
            Matcher Parts = WebhookPattern.matcher(Trimmed);
            Matcher Local = LoopbackPattern.matcher(Trimmed);
            Matcher Found = Parts.matches() ? Parts : Local.matches() ? Local : null;
            if (Found != null) {
                Clean = Clean.replace(Found.group(2), "***").replace(Found.group(1), "***");
            }
        }
        return Clean.replaceAll("(?i)https?://\\S*webhooks/\\S*", "[webhook]");
    }
}
