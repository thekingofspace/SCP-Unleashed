package net.rai.scp_unleashed.archive.client;

import java.util.ArrayList;
import java.util.List;

public final class TerminalLog {
    public static final int CharsPerTick = 2;
    public static final int RushedCharsPerTick = 5;
    public static final int RushQueue = 3;

    private final int Capacity;
    private final List<Entry> Lines = new ArrayList<>();
    private int Active;
    private int Revealed;

    public TerminalLog(int Capacity) {
        this.Capacity = Capacity;
    }

    public record Entry(String Text, int Colour) {
    }

    public void push(String Text, int Colour) {
        Lines.add(new Entry(Text, Colour));
        while (Lines.size() > Capacity + 8 && Active > 0) {
            Lines.removeFirst();
            Active--;
        }
    }

    public void flush() {
        Active = Lines.size();
        Revealed = 0;
    }

    public boolean isTyping() {
        return Active < Lines.size();
    }

    public int tick() {
        if (!isTyping()) {
            return 0;
        }
        int Budget = Lines.size() - Active > RushQueue ? RushedCharsPerTick : CharsPerTick;
        int Typed = 0;
        while (Budget > 0 && Active < Lines.size()) {
            Entry Current = Lines.get(Active);
            int Left = Current.Text().length() - Revealed;
            int Step = Math.min(Budget, Left);
            Revealed += Step;
            Typed += Step;
            Budget -= Step;
            if (Revealed >= Current.Text().length()) {
                Active++;
                Revealed = 0;
            }
        }
        return Typed;
    }

    public List<Entry> visible() {
        int Shown = Math.min(Lines.size(), Active + 1);
        int From = Math.max(0, Shown - Capacity);
        List<Entry> Result = new ArrayList<>();
        for (int Index = From; Index < Shown; Index++) {
            Entry Line = Lines.get(Index);
            if (Index == Active) {
                Result.add(new Entry(Line.Text().substring(0, Math.min(Revealed, Line.Text().length())), Line.Colour()));
            } else {
                Result.add(Line);
            }
        }
        return Result;
    }
}
