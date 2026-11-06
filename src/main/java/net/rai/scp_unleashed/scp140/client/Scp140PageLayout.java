package net.rai.scp_unleashed.scp140.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Scp140PageLayout {
    public static final int PanelWidth = 316;
    public static final int PanelHeight = 196;
    public static final int LeftX = 24;
    public static final int RightX = 172;
    public static final int TextTop = 17;
    public static final int TextWidth = 120;
    public static final int LineHeight = 10;
    public static final int LinesPerPage = 14;
    public static final int TitleGap = 9;
    public static final int InitialBox = 20;
    public static final int InitialIndent = 23;
    public static final int InitialLines = 2;
    public static final int RemainsGap = 5;
    public static final int PageBottom = TextTop + LinesPerPage * LineHeight;

    private Scp140PageLayout() {
    }

    public static List<String> wrap(Scp140Script Script, String Text, int Width) {
        List<String> Lines = new ArrayList<>();
        String Current = "";
        for (String Word : Text.split(" ")) {
            if (Word.isEmpty()) {
                continue;
            }
            String Trial = Current.isEmpty() ? Word : Current + " " + Word;
            if (Script.width(Trial) <= Width || Current.isEmpty()) {
                Current = Trial;
            } else {
                Lines.add(Current);
                Current = Word;
            }
        }
        if (!Current.isEmpty()) {
            Lines.add(Current);
        }
        return Lines;
    }

    public static List<String> balance(Scp140Script Script, String Text, int Width) {
        List<String> Lines = wrap(Script, Text, Width);
        if (Lines.size() != 2) {
            return Lines;
        }
        String[] Words = Text.split(" ");
        List<String> Best = Lines;
        int BestWidth = Math.max(Script.width(Lines.get(0)), Script.width(Lines.get(1)));
        for (int Split = 1; Split < Words.length; Split++) {
            String First = String.join(" ", Arrays.copyOfRange(Words, 0, Split));
            String Second = String.join(" ", Arrays.copyOfRange(Words, Split, Words.length));
            int FirstWidth = Script.width(First);
            int SecondWidth = Script.width(Second);
            int Widest = Math.max(FirstWidth, SecondWidth);
            if (FirstWidth <= Width && SecondWidth <= Width && Widest < BestWidth) {
                Best = List.of(First, Second);
                BestWidth = Widest;
            }
        }
        return Best;
    }

    public static List<Slot> slots(int Start, boolean Indented) {
        List<Slot> Found = new ArrayList<>();
        int Y = Start;
        int Index = 0;
        while (Y + LineHeight <= PageBottom) {
            int Indent = Indented && Index < InitialLines ? InitialIndent : 0;
            Found.add(new Slot(false, LeftX + Indent, Y, TextWidth - Indent));
            Y += LineHeight;
            Index++;
        }
        Y = TextTop;
        while (Y + LineHeight <= PageBottom) {
            Found.add(new Slot(true, RightX, Y, TextWidth));
            Y += LineHeight;
        }
        return Found;
    }

    private static int fill(Scp140Script Script, List<String> Words, List<Slot> Slots, List<Line> Placed) {
        int Cursor = 0;
        String Current = "";
        int Index = 0;
        while (Index < Words.size()) {
            if (Cursor >= Slots.size()) {
                return -1;
            }
            Slot Here = Slots.get(Cursor);
            String Word = Words.get(Index);
            String Trial = Current.isEmpty() ? Word : Current + " " + Word;
            if (Script.width(Trial) <= Here.Width() || Current.isEmpty()) {
                Current = Trial;
                Index++;
            } else {
                Placed.add(new Line(Here.X(), Here.Y(), Current));
                Current = "";
                Cursor++;
            }
        }
        if (!Current.isEmpty()) {
            if (Cursor >= Slots.size()) {
                return -1;
            }
            Slot Here = Slots.get(Cursor);
            Placed.add(new Line(Here.X(), Here.Y(), Current));
            Cursor++;
        }
        return Cursor;
    }

    private static List<String> words(String Text) {
        List<String> Found = new ArrayList<>();
        for (String Word : Text.split(" ")) {
            if (!Word.isEmpty()) {
                Found.add(Word);
            }
        }
        return Found;
    }

    public static Spread layout(Scp140Script Script, String Title, String Body, String Remains) {
        List<String> TitleLines = balance(Script, Title, TextWidth);
        List<Line> Titles = new ArrayList<>();
        for (int Index = 0; Index < TitleLines.size(); Index++) {
            String Text = TitleLines.get(Index);
            Titles.add(new Line(LeftX + (TextWidth - Script.width(Text)) / 2, TextTop + Index * LineHeight, Text));
        }
        int BodyTop = TextTop + TitleLines.size() * LineHeight + TitleGap;
        char Initial = Body.isEmpty() ? ' ' : Body.charAt(0);
        boolean Illuminated = Character.isLetter(Initial) && Character.toUpperCase(Initial) >= 'A' && Character.toUpperCase(Initial) <= 'Z';
        String Rest = Illuminated ? Body.substring(1) : Body;
        List<Slot> Slots = slots(BodyTop, Illuminated);
        List<Line> Lines = new ArrayList<>();
        int Cursor = fill(Script, words(Rest), Slots, Lines);
        boolean Overflow = Cursor < 0;
        List<Line> Tail = new ArrayList<>();
        if (!Remains.isEmpty() && !Overflow) {
            List<Slot> Usable = new ArrayList<>();
            for (Slot Here : Slots.subList(Cursor, Slots.size())) {
                int Y = Here.Y() + RemainsGap;
                if (Y + LineHeight <= PageBottom) {
                    Usable.add(new Slot(Here.Right(), Here.Right() ? RightX : LeftX, Y, TextWidth));
                }
            }
            Overflow = fill(Script, words(Remains), Usable, Tail) < 0;
        }
        return new Spread(Titles, Illuminated ? Character.toUpperCase(Initial) : ' ', LeftX - 1, BodyTop - 1, BodyTop - TitleGap + 1, Lines, Tail, Overflow);
    }

    public record Slot(boolean Right, int X, int Y, int Width) {
    }

    public record Line(int X, int Y, String Text) {
    }

    public record Spread(List<Line> Titles, char Initial, int InitialX, int InitialY, int RuleY, List<Line> Body, List<Line> Remains, boolean Overflow) {
        public boolean isIlluminated() {
            return Initial != ' ';
        }
    }
}
