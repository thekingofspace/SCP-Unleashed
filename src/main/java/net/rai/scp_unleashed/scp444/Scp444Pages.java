package net.rai.scp_unleashed.scp444;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.ToIntFunction;

public final class Scp444Pages {
    public static final long Seed = 444L;
    public static final int PageCount = 12;
    public static final int MainBase = 0xE000;
    public static final int MainGlyphs = 64;
    public static final int SmallBase = 0xE100;
    public static final int SmallGlyphs = 32;
    public static final int Bases = 16;
    public static final int Accents = 4;
    public static final int TextLeft = 40;
    public static final int TextTop = 20;
    public static final int TextWidth = 110;
    public static final int TextBottom = 160;
    public static final int LineHeight = 10;
    public static final int MarginLeft = 156;
    public static final int MarginTop = 26;
    public static final int MarginBottom = 156;
    public static final int PlateSize = 56;
    public static final int Plates = 8;
    public static final int InkColour = 0x2E2118;
    public static final int FadedColour = 0x5C4834;
    public static final int RedColour = 0x7A2E20;
    public static final int MainFont = 0;
    public static final int SmallFont = 1;
    public static final int[] Numerals = {0, 4, 8, 12, 16, 20, 24, 28, 1, 5};
    public static final String Narrow = " ";
    public static final String Normal = " ";
    public static final String Wide = " ";
    private static final double[] WordLengths = {0.08, 0.2, 0.24, 0.2, 0.14, 0.09, 0.05};
    private static final double[] AccentOdds = {0.55, 0.2, 0.15, 0.1};

    private Scp444Pages() {
    }

    public record Run(int X, int Y, String Text, int Face, int Colour, int Alpha, int Scale) {
    }

    public record Strike(int X, int Y, int Width, int Colour, int Alpha) {
    }

    public record Plate(int X, int Y, int Index) {
    }

    public record Note(int X, int Y, String Text, int Colour, int Alpha) {
    }

    public record Layout(List<Run> Runs, List<Strike> Strikes, List<Plate> Plates, List<Note> Notes, String Number) {
    }

    public static Random random(int Page) {
        return new Random(Seed * 1_000_003L + Page * 0x9E3779B97F4A7C15L);
    }

    public static String glyph(int Index) {
        return String.valueOf((char) (MainBase + Math.floorMod(Index, MainGlyphs)));
    }

    public static String small(int Index) {
        return String.valueOf((char) (SmallBase + Math.floorMod(Index, SmallGlyphs)));
    }

    public static String numeral(int Value) {
        String Digits = Integer.toString(Math.max(0, Value));
        StringBuilder Built = new StringBuilder();
        for (int Index = 0; Index < Digits.length(); Index++) {
            Built.append(small(Numerals[Digits.charAt(Index) - '0']));
        }
        return Built.toString();
    }

    private static int pick(Random Rng, double[] Odds) {
        double Roll = Rng.nextDouble();
        for (int Index = 0; Index < Odds.length; Index++) {
            Roll -= Odds[Index];
            if (Roll <= 0.0) {
                return Index;
            }
        }
        return Odds.length - 1;
    }

    private static int base(Random Rng) {
        double Roll = Rng.nextDouble();
        return Math.min(Bases - 1, (int) Math.floor(Bases * Roll * Roll * 0.85 + Bases * 0.15 * Rng.nextDouble()));
    }

    public static String word(Random Rng) {
        int Length = pick(Rng, WordLengths) + 1;
        StringBuilder Built = new StringBuilder();
        for (int Index = 0; Index < Length; Index++) {
            int Accent = pick(Rng, AccentOdds);
            if (Index == Length - 1 && Length > 1 && Rng.nextInt(3) == 0) {
                Accent = 2;
            }
            Built.append(glyph(base(Rng) * Accents + Accent));
        }
        return Built.toString();
    }

    public static String smallWord(Random Rng) {
        int Length = 2 + Rng.nextInt(4);
        StringBuilder Built = new StringBuilder();
        for (int Index = 0; Index < Length; Index++) {
            Built.append(small(8 + Rng.nextInt(SmallGlyphs - 8)));
        }
        return Built.toString();
    }

    private static String gap(Random Rng) {
        int Roll = Rng.nextInt(10);
        return Roll < 2 ? Narrow : Roll < 9 ? Normal : Wide;
    }

    public static Layout layout(int Page, ToIntFunction<String> MainWidth, ToIntFunction<String> SmallWidth) {
        Random Rng = random(Page);
        List<Run> Runs = new ArrayList<>();
        List<Strike> Strikes = new ArrayList<>();
        List<Plate> PlateList = new ArrayList<>();
        List<Note> Notes = new ArrayList<>();
        int Y = TextTop;
        int PlateTop = -1;
        int PlateBottom = -1;
        int PlateLeft = 0;
        int PlateRight = 0;
        if (Page == 0) {
            String Title = word(Rng) + Normal + word(Rng);
            int TitleWidth = MainWidth.applyAsInt(Title) * 2;
            Runs.add(new Run(TextLeft + (TextWidth - TitleWidth) / 2, Y, Title, MainFont, InkColour, 255, 2));
            Y += 22;
            String Under = word(Rng);
            Runs.add(new Run(TextLeft + (TextWidth - MainWidth.applyAsInt(Under)) / 2, Y, Under, MainFont, FadedColour, 220, 1));
            Y += 14;
            PlateList.add(new Plate(TextLeft + (TextWidth - PlateSize) / 2, Y, Rng.nextInt(Plates)));
            Y += PlateSize + 8;
        } else if (Rng.nextInt(5) < 2) {
            boolean Left = Rng.nextBoolean();
            int Rows = (PlateSize + LineHeight - 1) / LineHeight + 1;
            int Row = 1 + Rng.nextInt(Math.max(1, (TextBottom - TextTop) / LineHeight - Rows - 1));
            PlateTop = TextTop + Row * LineHeight;
            PlateBottom = PlateTop + Rows * LineHeight;
            PlateLeft = Left ? TextLeft : TextLeft + TextWidth - PlateSize;
            PlateRight = PlateLeft + PlateSize;
            PlateList.add(new Plate(PlateLeft, PlateTop + 2, Math.floorMod(Page * 5 + Rng.nextInt(2), Plates)));
        }
        int Paragraphs = Page == 0 ? 1 : 3 + Rng.nextInt(3);
        for (int Paragraph = 0; Paragraph < Paragraphs && Y + LineHeight <= TextBottom; Paragraph++) {
            int Lines = 2 + Rng.nextInt(Page == 0 ? 2 : 5);
            int Colour = Rng.nextInt(4) == 0 ? FadedColour : InkColour;
            int Alpha = 200 + Rng.nextInt(56);
            boolean Initial = Page != 0 && Rng.nextInt(3) == 0;
            int InitialWidth = 0;
            for (int Line = 0; Line < Lines && Y + LineHeight <= TextBottom; Line++) {
                int Left = TextLeft;
                int Right = TextLeft + TextWidth;
                if (PlateTop >= 0 && Y + LineHeight > PlateTop && Y < PlateBottom) {
                    if (PlateLeft == TextLeft) {
                        Left = PlateRight + 4;
                    } else {
                        Right = PlateLeft - 4;
                    }
                }
                if (Line == 0 && Initial) {
                    String Letter = glyph(base(Rng) * Accents);
                    InitialWidth = MainWidth.applyAsInt(Letter) * 2 + 2;
                    Runs.add(new Run(Left, Y, Letter, MainFont, RedColour, 235, 2));
                }
                if (Initial && Line < 2) {
                    Left += InitialWidth;
                } else if (Line == 0) {
                    Left += 6 + Rng.nextInt(6);
                }
                int Limit = Line == Lines - 1 ? Left + (Right - Left) * (4 + Rng.nextInt(6)) / 10 : Right;
                int X = Left;
                while (true) {
                    String Next = word(Rng);
                    int Width = MainWidth.applyAsInt(Next);
                    if (X + Width > Limit) {
                        break;
                    }
                    int Shift = Rng.nextInt(9) == 0 ? (Rng.nextBoolean() ? 1 : -1) : 0;
                    Runs.add(new Run(X, Y + Shift, Next, MainFont, Colour, Alpha, 1));
                    if (Rng.nextInt(28) == 0) {
                        Strikes.add(new Strike(X - 1, Y + 3 + Shift, Width + 1, Colour, Math.min(255, Alpha + 20)));
                    }
                    X += Width + MainWidth.applyAsInt(gap(Rng));
                }
                Y += LineHeight;
            }
            if (Initial && Lines < 2) {
                Y += LineHeight;
            }
            Y += 3 + Rng.nextInt(4);
        }
        int NoteCount = Page == 0 ? 0 : Rng.nextInt(3);
        int NoteY = MarginBottom;
        for (int Index = 0; Index < NoteCount; Index++) {
            StringBuilder Built = new StringBuilder(smallWord(Rng));
            int Words = 1 + Rng.nextInt(3);
            for (int Word = 0; Word < Words; Word++) {
                Built.append(Normal).append(smallWord(Rng));
            }
            String Text = Built.toString();
            int Length = SmallWidth.applyAsInt(Text);
            int Bottom = NoteY - Rng.nextInt(12);
            if (Bottom - Length < MarginTop) {
                break;
            }
            Notes.add(new Note(MarginLeft, Bottom, Text, RedColour, 190 + Rng.nextInt(50)));
            NoteY = Bottom - Length - 10;
        }
        return new Layout(Runs, Strikes, PlateList, Notes, numeral(Page + 1));
    }
}
