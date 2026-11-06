package net.rai.scp_unleashed.archive.client;

import net.minecraft.network.chat.Component;
import net.rai.scp_unleashed.archive.document.DossierContent;

import java.util.ArrayList;
import java.util.List;

public final class DossierLayout {
    public static final int Normal = 0;
    public static final int Bold = 1;
    public static final int Redacted = 2;
    public static final int Field = 0;
    public static final int Heading = 1;
    public static final int Paragraph = 2;
    public static final int Blank = 3;
    public static final int LinePitch = 11;
    public static final int ParagraphGap = 4;
    public static final int SectionGap = 8;
    public static final int HeadingGap = 2;
    public static final int KeepLines = 2;
    public static final int KeepWhole = 6;
    private static final String Closers = ")]\"'";
    private static final String Enders = ".!?";
    private static final String Openers = "[(\"'";
    public static final String RedactionMark = "%r";
    public static final String PercentEscape = "%%";

    private DossierLayout() {
    }

    public record Glyph(char Character, int Kind) {
    }

    public record Block(int Kind, List<Glyph> Glyphs) {
    }

    public record Line(List<Glyph> Glyphs, int Y, int Kind) {
    }

    @FunctionalInterface
    public interface Measure {
        int width(Glyph Entry);
    }

    public static List<Glyph> parse(String Text) {
        List<Glyph> Glyphs = new ArrayList<>();
        int Kind = Normal;
        int Index = 0;
        while (Index < Text.length()) {
            if (Text.startsWith(PercentEscape, Index)) {
                Glyphs.add(new Glyph('%', Kind));
                Index += PercentEscape.length();
                continue;
            }
            if (Text.startsWith(RedactionMark, Index)) {
                Kind = Kind == Redacted ? Normal : Redacted;
                Index += RedactionMark.length();
                continue;
            }
            if (Text.startsWith("[[", Index)) {
                Kind = Redacted;
                Index += 2;
                continue;
            }
            if (Text.startsWith("]]", Index)) {
                Kind = Normal;
                Index += 2;
                continue;
            }
            if (Text.startsWith("**", Index)) {
                Kind = Kind == Bold ? Normal : Kind == Normal ? Bold : Kind;
                Index += 2;
                continue;
            }
            Glyphs.add(new Glyph(Text.charAt(Index), Kind));
            Index++;
        }
        return Glyphs;
    }

    public static String plain(String Text) {
        return plain(Text, false);
    }

    public static String plain(String Text, boolean Revealed) {
        StringBuilder Builder = new StringBuilder();
        for (Glyph Entry : parse(Text)) {
            Builder.append(Entry.Kind() == Redacted && !Revealed ? '█' : Entry.Character());
        }
        return Builder.toString();
    }

    private static boolean isBreakable(Glyph Entry) {
        return Entry.Character() == ' ' && Entry.Kind() != Redacted;
    }

    private static int widthOf(List<Glyph> Glyphs, int Start, int End, Measure Measurer) {
        int Width = 0;
        for (int Index = Start; Index < End; Index++) {
            Width += Measurer.width(Glyphs.get(Index));
        }
        return Width;
    }

    private static int redactedGap(List<Glyph> Glyphs, int From, int To, int Width, Measure Measurer) {
        int Found = -1;
        int Used = 0;
        for (int Index = From; Index < To && Used <= Width; Index++) {
            Glyph Entry = Glyphs.get(Index);
            if (Index > From && Entry.Kind() == Redacted && Entry.Character() == ' ') {
                Found = Index;
            }
            Used += Measurer.width(Entry);
        }
        return Found;
    }

    public static List<int[]> wrap(List<Glyph> Glyphs, int Width, Measure Measurer) {
        List<int[]> Ranges = new ArrayList<>();
        int Count = Glyphs.size();
        int Index = 0;
        while (Index < Count) {
            while (Index < Count && isBreakable(Glyphs.get(Index))) {
                Index++;
            }
            if (Index >= Count) {
                break;
            }
            int Start = Index;
            int End = Index;
            int Resume = -1;
            int Used = 0;
            int Cursor = Index;
            while (Cursor < Count) {
                int WordEnd = Cursor;
                while (WordEnd < Count && !isBreakable(Glyphs.get(WordEnd))) {
                    WordEnd++;
                }
                int WordWidth = widthOf(Glyphs, Cursor, WordEnd, Measurer);
                if (End == Start) {
                    if (WordWidth <= Width) {
                        End = WordEnd;
                        Used = WordWidth;
                    } else {
                        int Gap = redactedGap(Glyphs, Cursor, WordEnd, Width, Measurer);
                        if (Gap > Cursor) {
                            End = Gap;
                            Resume = Gap + 1;
                            break;
                        }
                        int Fit = Cursor;
                        while (Fit < WordEnd && (Fit == Cursor || Used + Measurer.width(Glyphs.get(Fit)) <= Width)) {
                            Used += Measurer.width(Glyphs.get(Fit));
                            Fit++;
                        }
                        End = Fit;
                        break;
                    }
                } else {
                    int Gap = widthOf(Glyphs, End, Cursor, Measurer);
                    if (Used + Gap + WordWidth > Width) {
                        break;
                    }
                    End = WordEnd;
                    Used += Gap + WordWidth;
                }
                Cursor = WordEnd;
                while (Cursor < Count && isBreakable(Glyphs.get(Cursor))) {
                    Cursor++;
                }
            }
            Ranges.add(new int[]{Start, End});
            Index = Resume >= 0 ? Resume : End;
        }
        return Ranges;
    }

    public static List<List<Glyph>> lines(List<Glyph> Glyphs, int Width, Measure Measurer) {
        List<List<Glyph>> Lines = new ArrayList<>();
        for (int[] Range : wrap(Glyphs, Width, Measurer)) {
            Lines.add(List.copyOf(Glyphs.subList(Range[0], Range[1])));
        }
        return Lines;
    }

    public static List<Integer> sentenceBreaks(List<Glyph> Glyphs) {
        List<Integer> Breaks = new ArrayList<>();
        for (int Index = 0; Index < Glyphs.size(); Index++) {
            if (!isBreakable(Glyphs.get(Index))) {
                continue;
            }
            int Back = Index - 1;
            while (Back >= 0 && Closers.indexOf(Glyphs.get(Back).Character()) >= 0 && Glyphs.get(Back).Kind() != Redacted) {
                Back--;
            }
            if (Back < 0 || Enders.indexOf(Glyphs.get(Back).Character()) < 0 || Glyphs.get(Back).Kind() == Redacted) {
                continue;
            }
            int Ahead = Index + 1;
            while (Ahead < Glyphs.size() && isBreakable(Glyphs.get(Ahead))) {
                Ahead++;
            }
            if (Ahead >= Glyphs.size()) {
                continue;
            }
            Glyph Next = Glyphs.get(Ahead);
            if (Next.Kind() == Redacted || Character.isUpperCase(Next.Character()) || Character.isDigit(Next.Character()) || Openers.indexOf(Next.Character()) >= 0) {
                Breaks.add(Index);
            }
        }
        return Breaks;
    }

    private static List<Glyph> styled(String Text, int Kind) {
        List<Glyph> Glyphs = new ArrayList<>();
        for (int Index = 0; Index < Text.length(); Index++) {
            Glyphs.add(new Glyph(Text.charAt(Index), Kind));
        }
        return Glyphs;
    }

    private static List<Glyph> emphasised(String Text) {
        List<Glyph> Glyphs = new ArrayList<>();
        for (Glyph Entry : parse(Text)) {
            Glyphs.add(Entry.Kind() == Normal ? new Glyph(Entry.Character(), Bold) : Entry);
        }
        return Glyphs;
    }

    private static Block labelled(String Label, String Value) {
        List<Glyph> Glyphs = styled(Label, Bold);
        Glyphs.add(new Glyph(' ', Normal));
        Glyphs.addAll(parse(Value));
        return new Block(Field, Glyphs);
    }

    public static List<Block> paragraphs(String Text) {
        List<Block> Blocks = new ArrayList<>();
        for (String Part : Text.split("\n", -1)) {
            Blocks.add(Part.isBlank() ? new Block(Blank, List.of()) : new Block(Paragraph, parse(Part)));
        }
        return Blocks;
    }

    public static List<Block> blocks(DossierContent Content) {
        return blocks(Content, Component.translatable("gui.scp_unleashed.dossier.item_label").getString(), Component.translatable("gui.scp_unleashed.dossier.class_label").getString());
    }

    public static List<Block> blocks(DossierContent Content, String ItemLabel, String ClassLabel) {
        List<Block> Blocks = new ArrayList<>();
        Blocks.add(labelled(ItemLabel, Content.Item()));
        Blocks.add(labelled(ClassLabel, Content.ObjectClass()));
        for (DossierContent.Section Entry : Content.Sections()) {
            Entry.Heading().ifPresent(Title -> Blocks.add(new Block(Heading, emphasised(Title))));
            Blocks.addAll(paragraphs(Entry.Text()));
        }
        return Blocks;
    }

    private static int gapBefore(int Previous, int Kind) {
        if (Previous < 0 || Kind == Field) {
            return 0;
        }
        if (Kind == Heading) {
            return SectionGap;
        }
        if (Previous == Heading) {
            return HeadingGap;
        }
        return Previous == Paragraph ? ParagraphGap : SectionGap;
    }

    public static List<List<Line>> paginate(List<Block> Blocks, int Width, int MaxTop, Measure Measurer) {
        Pager Paging = new Pager(Width, MaxTop, Measurer);
        int Previous = -1;
        int Extra = 0;
        for (int Index = 0; Index < Blocks.size(); Index++) {
            Block Entry = Blocks.get(Index);
            if (Entry.Kind() == Blank) {
                Extra += LinePitch;
                continue;
            }
            int Gap = gapBefore(Previous, Entry.Kind()) + Extra;
            Extra = 0;
            if (Entry.Kind() == Heading) {
                List<Glyph> Next = null;
                for (int Later = Index + 1; Later < Blocks.size(); Later++) {
                    Block Following = Blocks.get(Later);
                    if (Following.Kind() == Paragraph) {
                        Next = Following.Glyphs();
                    }
                    if (Following.Kind() != Blank) {
                        break;
                    }
                }
                Paging.heading(Gap, Entry.Glyphs(), Next);
            } else {
                Paging.paragraph(Gap, Entry.Glyphs(), Entry.Kind());
            }
            Previous = Entry.Kind();
        }
        Paging.turn();
        if (Paging.Pages.isEmpty()) {
            Paging.Pages.add(List.of());
        }
        return Paging.Pages;
    }

    private static final class Pager {
        private final int Width;
        private final int MaxTop;
        private final Measure Measurer;
        private final List<List<Line>> Pages = new ArrayList<>();
        private List<Line> Lines = new ArrayList<>();
        private int Cursor;

        private Pager(int Width, int MaxTop, Measure Measurer) {
            this.Width = Width;
            this.MaxTop = MaxTop;
            this.Measurer = Measurer;
        }

        private int top(int Gap) {
            return Lines.isEmpty() ? 0 : Cursor + Gap;
        }

        private int capacity(int Gap) {
            int Top = top(Gap);
            return Top > MaxTop ? 0 : (MaxTop - Top) / LinePitch + 1;
        }

        private void place(int Gap, List<List<Glyph>> Placed, int Kind) {
            int Top = top(Gap);
            for (int Index = 0; Index < Placed.size(); Index++) {
                Lines.add(new Line(Placed.get(Index), Top + Index * LinePitch, Kind));
            }
            Cursor = Top + Placed.size() * LinePitch;
        }

        private void turn() {
            if (!Lines.isEmpty()) {
                Pages.add(List.copyOf(Lines));
            }
            Lines = new ArrayList<>();
            Cursor = 0;
        }

        private Pager probe() {
            Pager Copy = new Pager(Width, MaxTop, Measurer);
            Copy.Lines = new ArrayList<>(Lines);
            Copy.Cursor = Cursor;
            return Copy;
        }

        private int lineCount(List<Glyph> Glyphs) {
            return wrap(Glyphs, Width, Measurer).size();
        }

        private int sentenceSplit(int Gap, List<Glyph> Glyphs) {
            int Avail = capacity(Gap);
            if (Avail < KeepLines || lineCount(Glyphs) <= KeepWhole) {
                return -1;
            }
            List<Integer> Breaks = sentenceBreaks(Glyphs);
            for (int Index = Breaks.size() - 1; Index >= 0; Index--) {
                int Break = Breaks.get(Index);
                int Head = lineCount(Glyphs.subList(0, Break));
                if (Head > Avail) {
                    continue;
                }
                if (Head < KeepLines) {
                    return -1;
                }
                if (lineCount(Glyphs.subList(Break, Glyphs.size())) < KeepLines) {
                    continue;
                }
                return Break;
            }
            return -1;
        }

        private boolean wholeOrSplit(int Gap, List<Glyph> Glyphs) {
            return lineCount(Glyphs) <= capacity(Gap) || sentenceSplit(Gap, Glyphs) >= 0;
        }

        private boolean startsHere(int Gap, List<Glyph> Glyphs) {
            if (wholeOrSplit(Gap, Glyphs)) {
                return true;
            }
            return !new Pager(Width, MaxTop, Measurer).wholeOrSplit(0, Glyphs) && Math.min(capacity(Gap), lineCount(Glyphs) - KeepLines) >= KeepLines;
        }

        private void paragraph(int Gap, List<Glyph> Glyphs, int Kind) {
            while (true) {
                List<int[]> Ranges = wrap(Glyphs, Width, Measurer);
                if (Ranges.isEmpty()) {
                    return;
                }
                List<List<Glyph>> Wrapped = new ArrayList<>();
                for (int[] Range : Ranges) {
                    Wrapped.add(List.copyOf(Glyphs.subList(Range[0], Range[1])));
                }
                if (Wrapped.size() <= capacity(Gap)) {
                    place(Gap, Wrapped, Kind);
                    return;
                }
                int Split = sentenceSplit(Gap, Glyphs);
                if (Split >= 0) {
                    place(Gap, lines(Glyphs.subList(0, Split), Width, Measurer), Kind);
                    turn();
                    Glyphs = Glyphs.subList(Split, Glyphs.size());
                    Gap = 0;
                    continue;
                }
                if (!Lines.isEmpty() && new Pager(Width, MaxTop, Measurer).wholeOrSplit(0, Glyphs)) {
                    turn();
                    Gap = 0;
                    continue;
                }
                int Take = Math.min(capacity(Gap), Wrapped.size() - KeepLines);
                if (Take < KeepLines) {
                    turn();
                    Gap = 0;
                    continue;
                }
                place(Gap, Wrapped.subList(0, Take), Kind);
                turn();
                Glyphs = Glyphs.subList(Ranges.get(Take)[0], Glyphs.size());
                Gap = 0;
            }
        }

        private void heading(int Gap, List<Glyph> Glyphs, List<Glyph> Next) {
            List<List<Glyph>> Wrapped = lines(Glyphs, Width, Measurer);
            if (!Lines.isEmpty()) {
                boolean Fits = Wrapped.size() <= capacity(Gap);
                if (Fits && Next != null) {
                    Pager Probe = probe();
                    Probe.place(Gap, Wrapped, Heading);
                    Fits = Probe.startsHere(HeadingGap, Next);
                }
                if (!Fits) {
                    turn();
                    Gap = 0;
                }
            }
            place(Gap, Wrapped, Heading);
        }
    }
}
