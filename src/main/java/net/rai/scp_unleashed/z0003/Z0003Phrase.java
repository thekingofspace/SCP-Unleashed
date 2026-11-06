package net.rai.scp_unleashed.z0003;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class Z0003Phrase {
    public static final float MinRadius = 1.0f;
    public static final float MaxRadius = 64.0f;
    public static final float MaxDelaySeconds = 600.0f;
    public static final int TicksPerSecond = 20;
    public static final int MaxLength = 256;

    private static final Map<String, Integer> Small = Map.ofEntries(
            Map.entry("zero", 0), Map.entry("one", 1), Map.entry("two", 2), Map.entry("three", 3), Map.entry("four", 4),
            Map.entry("five", 5), Map.entry("six", 6), Map.entry("seven", 7), Map.entry("eight", 8), Map.entry("nine", 9), Map.entry("ten", 10),
            Map.entry("eleven", 11), Map.entry("twelve", 12), Map.entry("thirteen", 13), Map.entry("fourteen", 14), Map.entry("fifteen", 15),
            Map.entry("sixteen", 16), Map.entry("seventeen", 17), Map.entry("eighteen", 18), Map.entry("nineteen", 19), Map.entry("dozen", 12));
    private static final Map<String, Integer> Tens = Map.of("twenty", 20, "thirty", 30, "forty", 40, "fourty", 40, "fifty", 50, "sixty", 60,
            "seventy", 70, "eighty", 80, "ninety", 90);
    private static final Set<String> Single = Set.of("a", "an");
    private static final Set<String> Distance = Set.of("m", "meter", "meters", "metre", "metres", "block", "blocks");
    private static final Set<String> Seconds = Set.of("s", "sec", "secs", "second", "seconds");
    private static final Set<String> Minutes = Set.of("min", "mins", "minute", "minutes");

    private Z0003Phrase() {
    }

    public record Setting(float Radius, int DelayTicks, boolean RadiusClamped, boolean DelayClamped) {
        public Setting(float Radius, int DelayTicks) {
            this(Radius, DelayTicks, false, false);
        }

        public float seconds() {
            return DelayTicks / (float) TicksPerSecond;
        }
    }

    private record Quantity(double Value, int Next) {
    }

    public static Optional<Setting> parse(String Message) {
        if (Message == null || Message.isBlank() || Message.length() > MaxLength) {
            return Optional.empty();
        }
        List<String> Words = tokens(Message);
        Double Radius = null;
        Double Delay = null;
        int Index = 0;
        while (Index < Words.size()) {
            Quantity Found = number(Words, Index);
            if (Found == null) {
                Index++;
                continue;
            }
            int Unit = Found.Next();
            if (Unit < Words.size()) {
                String Word = Words.get(Unit);
                if (Radius == null && Distance.contains(Word)) {
                    Radius = Found.Value();
                    Index = Unit + 1;
                    continue;
                }
                if (Delay == null && Seconds.contains(Word)) {
                    Delay = Found.Value();
                    Index = Unit + 1;
                    continue;
                }
                if (Delay == null && Minutes.contains(Word)) {
                    Delay = Found.Value() * 60.0;
                    Index = Unit + 1;
                    continue;
                }
            }
            Index = Math.max(Index + 1, Found.Next());
        }
        if (Radius == null || Delay == null) {
            return Optional.empty();
        }
        return Optional.of(clamp(Radius, Delay));
    }

    public static Setting clamp(double Radius, double DelaySeconds) {
        float Reach = Mth.clamp((float) Radius, MinRadius, MaxRadius);
        float Wait = Mth.clamp((float) DelaySeconds, 0.0f, MaxDelaySeconds);
        return new Setting(Reach, Math.round(Wait * TicksPerSecond), Math.abs(Reach - Radius) > 1.0E-3, Math.abs(Wait - DelaySeconds) > 1.0E-3);
    }

    static List<String> tokens(String Message) {
        String Lower = Message.toLowerCase(Locale.ROOT).replace('-', ' ').replace(',', ' ');
        Lower = Lower.replaceAll("(\\d)([a-z])", "$1 $2").replaceAll("([a-z])(\\d)", "$1 $2");
        List<String> Words = new ArrayList<>();
        for (String Piece : Lower.split("[^a-z0-9.]+")) {
            String Trimmed = trimDots(Piece);
            if (!Trimmed.isEmpty()) {
                Words.add(Trimmed);
            }
        }
        return Words;
    }

    private static String trimDots(String Piece) {
        int Start = 0;
        int End = Piece.length();
        while (Start < End && Piece.charAt(Start) == '.' && !(Start + 1 < End && Character.isDigit(Piece.charAt(Start + 1)))) {
            Start++;
        }
        while (End > Start && Piece.charAt(End - 1) == '.') {
            End--;
        }
        return Piece.substring(Start, End);
    }

    private static Quantity number(List<String> Words, int Index) {
        String First = Words.get(Index);
        if (isDigits(First)) {
            try {
                double Value = Double.parseDouble(First);
                if (!Double.isFinite(Value)) {
                    return null;
                }
                return halves(Words, new Quantity(Value, Index + 1));
            } catch (NumberFormatException Problem) {
                return null;
            }
        }
        if (First.equals("half") && Index + 1 < Words.size() && Single.contains(Words.get(Index + 1))) {
            return new Quantity(0.5, Index + 2);
        }
        if (Single.contains(First)) {
            if (Index + 1 < Words.size()) {
                String Next = Words.get(Index + 1);
                if (Next.equals("half")) {
                    return new Quantity(0.5, Index + 2);
                }
                if (Next.equals("hundred") || Next.equals("thousand") || Next.equals("dozen")) {
                    return worded(Words, Index + 1, 1.0);
                }
                if (isUnit(Next)) {
                    return new Quantity(1.0, Index + 1);
                }
            }
            return null;
        }
        if (!isNumberWord(First)) {
            return null;
        }
        return worded(Words, Index, 0.0);
    }

    private static Quantity worded(List<String> Words, int Index, double Lead) {
        double Total = 0.0;
        double Current = Lead;
        boolean Any = Lead > 0.0;
        int Cursor = Index;
        while (Cursor < Words.size()) {
            String Word = Words.get(Cursor);
            if (Small.containsKey(Word)) {
                Current += Small.get(Word);
            } else if (Tens.containsKey(Word)) {
                Current += Tens.get(Word);
            } else if (Word.equals("hundred")) {
                Current = Math.max(Current, 1.0) * 100.0;
            } else if (Word.equals("thousand")) {
                Total += Math.max(Current, 1.0) * 1000.0;
                Current = 0.0;
            } else if (Word.equals("and") && Any && Cursor + 1 < Words.size() && isNumberWord(Words.get(Cursor + 1))) {
                Cursor++;
                continue;
            } else {
                break;
            }
            Any = true;
            Cursor++;
        }
        if (!Any) {
            return null;
        }
        return halves(Words, new Quantity(Total + Current, Cursor));
    }

    private static Quantity halves(List<String> Words, Quantity Found) {
        int Cursor = Found.Next();
        if (Cursor + 2 < Words.size() && Words.get(Cursor).equals("and") && Single.contains(Words.get(Cursor + 1)) && Words.get(Cursor + 2).equals("half")) {
            return new Quantity(Found.Value() + 0.5, Cursor + 3);
        }
        return Found;
    }

    private static boolean isDigits(String Word) {
        boolean Digit = false;
        int Dots = 0;
        for (int Index = 0; Index < Word.length(); Index++) {
            char Letter = Word.charAt(Index);
            if (Character.isDigit(Letter)) {
                Digit = true;
            } else if (Letter == '.') {
                Dots++;
            } else {
                return false;
            }
        }
        return Digit && Dots <= 1;
    }

    private static boolean isNumberWord(String Word) {
        return Small.containsKey(Word) || Tens.containsKey(Word) || Word.equals("hundred") || Word.equals("thousand");
    }

    private static boolean isUnit(String Word) {
        return Distance.contains(Word) || Seconds.contains(Word) || Minutes.contains(Word);
    }
}
