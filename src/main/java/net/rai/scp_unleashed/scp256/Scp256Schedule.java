package net.rai.scp_unleashed.scp256;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Scp256Schedule {
    public static final int Columns = 16;
    public static final int LeadIn = 12;
    public static final int ReturnTicks = 8;
    public static final int DingDelay = 4;
    public static final int Tail = 18;
    public static final int FullStop = 10;
    public static final int Comma = 5;
    public static final int Colon = 6;
    public static final int Hesitation = 7;
    public static final int HesitationOdds = 29;
    private final String Text;
    private final List<String> Rows;
    private final int[] Times;
    private final int[] RowOf;
    private final int[] ColumnOf;
    private final int[] SourceOf;
    private final int[] Returns;
    private final int Duration;

    private Scp256Schedule(String Text, List<String> Rows, int[] Times, int[] RowOf, int[] ColumnOf, int[] SourceOf, int[] Returns, int Duration) {
        this.Text = Text;
        this.Rows = Rows;
        this.Times = Times;
        this.RowOf = RowOf;
        this.ColumnOf = ColumnOf;
        this.SourceOf = SourceOf;
        this.Returns = Returns;
        this.Duration = Duration;
    }

    public static List<String> wrap(String Text) {
        List<String> Result = new ArrayList<>();
        StringBuilder Current = new StringBuilder();
        for (String Word : Text.trim().split(" +")) {
            if (Current.isEmpty()) {
                Current.append(Word);
            } else if (Current.length() + 1 + Word.length() <= Columns) {
                Current.append(' ').append(Word);
            } else {
                Result.add(Current.toString());
                Current = new StringBuilder(Word);
            }
            while (Current.length() > Columns) {
                Result.add(Current.substring(0, Columns));
                Current = new StringBuilder(Current.substring(Columns));
            }
        }
        if (!Current.isEmpty()) {
            Result.add(Current.toString());
        }
        return Result;
    }

    public static int mix(int Seed, int Index) {
        int Value = Seed * 0x9E3779B1 + Index * 0x85EBCA77;
        Value ^= Value >>> 15;
        Value *= 0x2C1B3C6D;
        Value ^= Value >>> 12;
        Value *= 0x297A2D39;
        Value ^= Value >>> 15;
        return Value & 0x7FFFFFFF;
    }

    public static int pauseAfter(char Previous) {
        return switch (Previous) {
            case '.', '?', '!' -> FullStop;
            case ',' -> Comma;
            case ';', ':' -> Colon;
            default -> 0;
        };
    }

    public static int strokeDelay(char Current, char Previous, int Hash) {
        if (Current == ' ') {
            return 3;
        }
        if (Current == 'x' && Previous == 'x') {
            return 1;
        }
        int Pick = Hash % 4;
        return Pick == 0 ? 1 : Pick == 3 ? 3 : 2;
    }

    public static Scp256Schedule of(String Text, int Seed) {
        String Clean = Text.trim().replaceAll(" +", " ");
        List<String> Rows = wrap(Clean);
        int Count = 0;
        for (String Row : Rows) {
            Count += Row.length();
        }
        int[] Times = new int[Count];
        int[] RowOf = new int[Count];
        int[] ColumnOf = new int[Count];
        int[] SourceOf = new int[Count];
        int[] Returns = new int[Math.max(0, Rows.size() - 1)];
        int Clock = 0;
        int Stroke = 0;
        int Source = 0;
        char Previous = ' ';
        for (int Row = 0; Row < Rows.size(); Row++) {
            String Line = Rows.get(Row);
            if (Row > 0) {
                Returns[Row - 1] = Clock + 1;
                Clock += ReturnTicks + 1;
                if (Source < Clean.length() && Clean.charAt(Source) == ' ') {
                    Source++;
                }
            }
            for (int Column = 0; Column < Line.length(); Column++) {
                char Current = Line.charAt(Column);
                int Hash = mix(Seed, Stroke);
                if (Stroke == 0) {
                    Clock += LeadIn;
                } else {
                    Clock += (Column == 0 ? 0 : strokeDelay(Current, Previous, Hash)) + pauseAfter(Previous);
                    if (Current != ' ' && Hash % HesitationOdds == 0) {
                        Clock += Hesitation;
                    }
                }
                Times[Stroke] = Clock;
                RowOf[Stroke] = Row;
                ColumnOf[Stroke] = Column;
                SourceOf[Stroke] = Math.min(Source, Math.max(0, Clean.length() - 1));
                Source++;
                Previous = Current;
                Stroke++;
            }
        }
        int Last = Count == 0 ? LeadIn : Times[Count - 1];
        return new Scp256Schedule(Clean, Rows, Times, RowOf, ColumnOf, SourceOf, Returns, Last + Tail);
    }

    public Scp256Schedule scaled(int Wanted) {
        if (Wanted <= 0 || Wanted == Duration) {
            return this;
        }
        double Factor = Wanted / (double) Duration;
        int[] NewTimes = new int[Times.length];
        for (int Index = 0; Index < Times.length; Index++) {
            NewTimes[Index] = (int) Math.round(Times[Index] * Factor);
        }
        int[] NewReturns = new int[Returns.length];
        for (int Index = 0; Index < Returns.length; Index++) {
            NewReturns[Index] = (int) Math.round(Returns[Index] * Factor);
        }
        return new Scp256Schedule(Text, Rows, NewTimes, RowOf, ColumnOf, SourceOf, NewReturns, Wanted);
    }

    public int typedAt(double Elapsed) {
        int Low = 0;
        int High = Times.length;
        while (Low < High) {
            int Middle = (Low + High) >>> 1;
            if (Times[Middle] <= Elapsed) {
                Low = Middle + 1;
            } else {
                High = Middle;
            }
        }
        return Low;
    }

    public String getText() {
        return Text;
    }

    public List<String> getRows() {
        return Rows;
    }

    public int strokes() {
        return Times.length;
    }

    public int timeOf(int Stroke) {
        return Times[Stroke];
    }

    public int rowOf(int Stroke) {
        return RowOf[Stroke];
    }

    public int columnOf(int Stroke) {
        return ColumnOf[Stroke];
    }

    public int sourceOf(int Stroke) {
        return SourceOf[Stroke];
    }

    public char charOf(int Stroke) {
        return Rows.get(RowOf[Stroke]).charAt(ColumnOf[Stroke]);
    }

    public int returns() {
        return Returns.length;
    }

    public int returnAt(int Index) {
        return Returns[Index];
    }

    public int lastStroke() {
        return Times.length == 0 ? LeadIn : Times[Times.length - 1];
    }

    public int dingAt() {
        return lastStroke() + DingDelay;
    }

    public int finalReturnAt() {
        return dingAt() + 1;
    }

    public int getDuration() {
        return Duration;
    }

    public String typedText(int Typed) {
        if (Typed <= 0 || Times.length == 0) {
            return "";
        }
        int End = SourceOf[Math.min(Typed, Times.length) - 1] + 1;
        return Text.substring(0, Math.min(End, Text.length()));
    }

    @Override
    public String toString() {
        return "Scp256Schedule" + Arrays.toString(Times) + " " + Duration;
    }
}
