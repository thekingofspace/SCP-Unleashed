package net.rai.scp_unleashed.scp2521;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Scp2521Name {
    public static final String Gap = "[\\s\\-_.,/|:;~'\"`+*]{0,3}";
    public static final String WordGap = "[\\s\\-_.,/|:;~'\"`+*]{0,4}";
    public static final String Digits = "(?<![0-9])2" + Gap + "5" + Gap + "2" + Gap + "1(?![0-9])";
    public static final String TwentyFive = "(?:twenty" + WordGap + "five|two" + WordGap + "five|25|2" + Gap + "5)";
    public static final String TwentyOne = "(?:twenty" + WordGap + "one|two" + WordGap + "one|21|2" + Gap + "1)";
    public static final String Halves = "(?<![a-z0-9])" + TwentyFive + WordGap + "(?:hundred" + WordGap + "(?:and" + WordGap + ")?)?" + TwentyOne + "(?![a-z0-9])";
    public static final String Thousands = "(?<![a-z0-9])two" + WordGap + "thousand" + WordGap + "(?:and" + WordGap + ")?five" + WordGap + "hundred" + WordGap
            + "(?:and" + WordGap + ")?twenty" + WordGap + "one(?![a-z0-9])";
    public static final String Dot = "[\u25cf\u2022\u00b7\u2219\u26ab\u2b24\u25cb\u25e6\u2981\u2219.o0*\u00b0]";
    public static final String Bar = "\\s{0,2}[|\u00a6\u2502\u2503\u01c0!/\\\\]\\s{0,2}";
    public static final String DotGap = "\\s?";
    public static final String Notation = "(?<!" + Dot + "\\s?)(?<![|\u00a6\u2502\u2503\u01c0!/\\\\]\\s?)" + Dot + DotGap + Dot + Bar + Dot + "(?:" + DotGap + Dot + "){4}" + Bar + Dot + DotGap + Dot
            + Bar + Dot + "(?!\\s?" + Dot + ")(?!\\s{0,2}[|\u00a6\u2502\u2503\u01c0!/\\\\])";
    public static final Pattern Spoken = Pattern.compile(Digits + "|" + Halves + "|" + Thousands + "|" + Notation);
    private static final Pattern Hidden = Pattern.compile("[\u200b-\u200f\u2060\ufeff\u00ad]");

    public static final List<String> Positives = List.of("2521", "2 5 2 1", "2-5-2-1", "two five two one", "two-five-two-one", "twenty-five twenty-one",
            "twenty five twenty one", "two thousand five hundred twenty one", "two thousand five hundred and twenty-one", "scp2521", "scp-2521", "SCP 2521",
            "\u25cf\u25cf|\u25cf\u25cf\u25cf\u25cf\u25cf|\u25cf\u25cf|\u25cf", "..|.....|..|.", "oo|ooooo|oo|o", "\u2022\u2022 | \u2022\u2022\u2022\u2022\u2022 | \u2022\u2022 | \u2022",
            "i think 2521 is behind me", "TWENTYFIVE TWENTYONE", "twofivetwoone", "25-21", "25 21", "twenty-five hundred twenty-one", "2,521", "\uff12\uff15\uff12\uff11",
            "hello scp-2521!", "Two Five Two One?", "2_5_2_1", "twenty five 21");
    public static final List<String> Negatives = List.of("25210", "12521", "2522", "2512", "1252", "two five two two", "twenty five twenty two", "52521", "2 5 2 2",
            "twenty-one twenty-five", "..|.....|..|..", "..|....|..|.", "hello there", "scp-2522", "two thousand five hundred twenty two", "2.5.2.11", "125 21",
            "oo|ooooo|oo|oo", "the 25th of 21", "");

    private Scp2521Name() {
    }

    public static String normalise(String Raw) {
        if (Raw == null) {
            return "";
        }
        String Text = Normalizer.normalize(Raw, Normalizer.Form.NFKC);
        Text = Hidden.matcher(Text).replaceAll("");
        return Text.toLowerCase(Locale.ROOT);
    }

    public static boolean matches(String Raw) {
        String Text = normalise(Raw);
        return !Text.isEmpty() && Spoken.matcher(Text).find();
    }
}
