package net.rai.scp_unleashed.scp012.client;

import java.util.ArrayList;
import java.util.List;

public final class ScoreGlyphs {
    public static final int Body = 1;
    public static final int Light = 2;
    public static final int Dark = 3;
    public static final List<Glyph> All = List.of(
            glyph("quarter", true, false,
                    "....#",
                    "....#",
                    "....#",
                    "....#",
                    "....#",
                    ".hh##",
                    "h####",
                    "####d",
                    ".!dd."),
            glyph("quarter_low", true, false,
                    ".hh#.",
                    "h####",
                    "####d",
                    "#!dd.",
                    "#....",
                    "#....",
                    "#....",
                    "#....",
                    "#...."),
            glyph("eighth", true, false,
                    "....#h..",
                    "....##h.",
                    "....#.#.",
                    "....#..#",
                    "....#..#",
                    ".hh##.#.",
                    "h####...",
                    "####d...",
                    ".!dd...."),
            glyph("beamed", true, false,
                    "...hhhhhh#",
                    "...#######",
                    "...#.....#",
                    "...#.....#",
                    "...#.....#",
                    ".hh#..hhh#",
                    "h###.h####",
                    "###d.####d",
                    ".!d...!dd."),
            glyph("half", true, false,
                    "....#",
                    "....#",
                    "....#",
                    "....#",
                    "....#",
                    ".hh##",
                    "h#..#",
                    "#..dd",
                    ".!dd."),
            glyph("whole", true, false,
                    ".hhh#.",
                    "h#..##",
                    "##..#d",
                    ".!dd#."),
            glyph("quarter_rest", false, false,
                    ".h...",
                    "..#..",
                    "...#.",
                    "..##.",
                    ".##..",
                    "..#..",
                    "...#.",
                    "..h#.",
                    ".#d..",
                    ".d..."),
            glyph("eighth_rest", false, false,
                    "hh..h",
                    "##.#.",
                    "...#.",
                    "..#..",
                    "..#..",
                    ".d..."),
            glyph("treble", true, true,
                    "....h#.",
                    "...#..#",
                    "...#..#",
                    "...#.#.",
                    "....##.",
                    "...##..",
                    "..##...",
                    ".##....",
                    "h#..hh.",
                    "#..#..#",
                    "#..#..#",
                    ".#.#.#.",
                    "..###..",
                    "....#..",
                    ".h..#..",
                    ".!dd..."),
            glyph("bass", false, true,
                    ".hhh...",
                    "h...#.h",
                    "#h..#..",
                    "....#.h",
                    "...#...",
                    "..#....",
                    ".#.....",
                    "d......"),
            glyph("sharp", false, false,
                    ".#.#.",
                    "hhhhh",
                    ".#.#.",
                    "hhhhh",
                    ".#.#.",
                    ".d.d."),
            glyph("flat", false, false,
                    "h...",
                    "#...",
                    "#...",
                    "#hh.",
                    "#..#",
                    "#.#.",
                    "#d..")
    );

    private ScoreGlyphs() {
    }

    private static Glyph glyph(String Name, boolean Sounding, boolean Clef, String... Rows) {
        int Height = Rows.length;
        int Width = 0;
        for (String Row : Rows) {
            Width = Math.max(Width, Row.length());
        }
        int[] Cells = new int[Width * Height];
        List<int[]> Drips = new ArrayList<>();
        for (int Y = 0; Y < Height; Y++) {
            String Row = Rows[Y];
            for (int X = 0; X < Row.length(); X++) {
                char Mark = Row.charAt(X);
                int Value = switch (Mark) {
                    case '#', '!' -> Body;
                    case 'h' -> Light;
                    case 'd' -> Dark;
                    default -> 0;
                };
                Cells[Y * Width + X] = Value;
                if (Mark == '!') {
                    Drips.add(new int[]{X, Y});
                }
            }
        }
        return new Glyph(Name, Width, Height, Cells, Drips.toArray(new int[0][]), Sounding, Clef);
    }

    public record Glyph(String Name, int Width, int Height, int[] Cells, int[][] Drips, boolean Sounding, boolean Clef) {
        public int at(int X, int Y) {
            return X < 0 || Y < 0 || X >= Width || Y >= Height ? 0 : Cells[Y * Width + X];
        }
    }
}
