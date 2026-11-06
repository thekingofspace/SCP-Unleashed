package net.rai.scp_unleashed.scp294;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public final class Scp294Names {
    public static final int MaxTyped = 32;
    public static final int MaxRaw = 128;
    public static final List<String> Prefixes = List.of("a cup of ", "cup of ", "a glass of ", "glass of ", "a mug of ", "mug of ");

    private Scp294Names() {
    }

    public static boolean allowed(char Letter) {
        return (Letter >= 'a' && Letter <= 'z') || (Letter >= 'A' && Letter <= 'Z') || (Letter >= '0' && Letter <= '9') || Letter == ' ' || Letter == '\'' || Letter == '-';
    }

    public static String sanitize(String Raw) {
        if (Raw == null) {
            return "";
        }
        String Limited = Raw.length() > MaxRaw ? Raw.substring(0, MaxRaw) : Raw;
        StringBuilder Kept = new StringBuilder();
        boolean Space = false;
        for (int Index = 0; Index < Limited.length(); Index++) {
            char Letter = Limited.charAt(Index);
            if (Letter == '§') {
                Index++;
                continue;
            }
            if (!allowed(Letter) || Character.isWhitespace(Letter)) {
                Space = Kept.length() > 0;
                continue;
            }
            if (Space) {
                Kept.append(' ');
                Space = false;
            }
            Kept.append(Character.toLowerCase(Letter));
        }
        String Clean = Kept.toString().trim();
        return Clean.length() > MaxTyped ? Clean.substring(0, MaxTyped).trim() : Clean;
    }

    public static String normalise(String Text) {
        if (Text == null) {
            return "";
        }
        String Lowered = Text.toLowerCase(Locale.ROOT).replace("'", "");
        StringBuilder Kept = new StringBuilder();
        for (int Index = 0; Index < Lowered.length(); Index++) {
            char Letter = Lowered.charAt(Index);
            boolean Plain = (Letter >= 'a' && Letter <= 'z') || (Letter >= '0' && Letter <= '9');
            Kept.append(Plain ? Letter : ' ');
        }
        String Clean = String.join(" ", Kept.toString().trim().split("\\s+")).trim();
        for (String Prefix : Prefixes) {
            if (Clean.startsWith(Prefix) && Clean.length() > Prefix.length()) {
                Clean = Clean.substring(Prefix.length());
                break;
            }
        }
        return Clean;
    }

    public static String titleCase(String Text) {
        StringBuilder Result = new StringBuilder();
        boolean Start = true;
        for (int Index = 0; Index < Text.length(); Index++) {
            char Letter = Text.charAt(Index);
            if (Letter == ' ' || Letter == '-' || Letter == '_') {
                Result.append(Letter == '_' ? ' ' : Letter);
                Start = true;
                continue;
            }
            Result.append(Start ? Character.toUpperCase(Letter) : Letter);
            Start = false;
        }
        return Result.toString();
    }

    public static long hash(String Key, long Seed) {
        long Value = 0xcbf29ce484222325L;
        for (byte Part : Key.getBytes(StandardCharsets.UTF_8)) {
            Value ^= Part & 0xFF;
            Value *= 0x100000001b3L;
        }
        return mix(Value ^ mix(Seed));
    }

    public static long mix(long Value) {
        long Result = Value + 0x9E3779B97F4A7C15L;
        Result = (Result ^ (Result >>> 30)) * 0xBF58476D1CE4E5B9L;
        Result = (Result ^ (Result >>> 27)) * 0x94D049BB133111EBL;
        return Result ^ (Result >>> 31);
    }
}
