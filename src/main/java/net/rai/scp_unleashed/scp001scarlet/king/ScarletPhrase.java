package net.rai.scp_unleashed.scp001scarlet.king;

import java.util.List;
import java.util.Locale;

public final class ScarletPhrase {
    public static final List<String> Words = List.of("seven", "throne", "seven", "bride", "seven", "heart", "for", "the", "scarlet", "king");

    private ScarletPhrase() {
    }

    public static boolean matches(String Raw) {
        if (Raw == null || Raw.length() > 256) {
            return false;
        }
        String Clean = Raw.toLowerCase(Locale.ROOT).replace("7", " seven ").replaceAll("[^a-z]+", " ").trim();
        if (Clean.isEmpty()) {
            return false;
        }
        String[] Parts = Clean.split(" +");
        if (Parts.length != Words.size()) {
            return false;
        }
        for (int Index = 0; Index < Parts.length; Index++) {
            if (!same(Parts[Index], Words.get(Index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean same(String Said, String Wanted) {
        if (Said.equals(Wanted)) {
            return true;
        }
        return (Wanted.equals("throne") || Wanted.equals("bride") || Wanted.equals("heart")) && Said.equals(Wanted + "s");
    }
}
