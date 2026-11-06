package net.rai.scp_unleashed.terminal;

import java.util.ArrayList;
import java.util.List;

public final class TerminalBoot {
    public static final int MaxMs = 3000;
    public static final int RevealMs = 360;
    public static final int RowDelayMs = 60;
    public static final int RowStepMs = 45;
    public static final int LogoCharMs = 18;
    public static final String Title = "terminal.scp_unleashed.boot.title";
    public static final String Cpu = "terminal.scp_unleashed.boot.cpu";
    public static final String Memory = "terminal.scp_unleashed.boot.memory";
    public static final String Archive = "terminal.scp_unleashed.boot.archive";
    public static final String Indexed = "terminal.scp_unleashed.boot.indexed";
    public static final String Breakdown = "terminal.scp_unleashed.boot.breakdown";
    public static final String Modules = "terminal.scp_unleashed.boot.modules";
    public static final String Clearance = "terminal.scp_unleashed.boot.clearance";
    public static final String Channel = "terminal.scp_unleashed.boot.channel";
    public static final String Motto = "terminal.scp_unleashed.boot.motto";

    public record Step(String Key, boolean Tagged, int CharMs, int WorkMs, int AfterMs) {
    }

    public static final List<Step> Script = script(false);

    public static List<Step> script(boolean Census) {
        List<Step> Steps = new ArrayList<>();
        Steps.add(new Step(Title, false, 4, 0, 140));
        Steps.add(new Step("", false, 0, 0, 40));
        Steps.add(new Step(Cpu, true, 4, 140, 30));
        Steps.add(new Step(Memory, true, 4, 200, 30));
        if (Census) {
            Steps.add(new Step(Indexed, true, 4, 200, 20));
            Steps.add(new Step(Breakdown, false, 2, 0, 40));
        } else {
            Steps.add(new Step(Archive, true, 4, 180, 30));
        }
        Steps.add(new Step(Modules, true, 4, 140, 30));
        Steps.add(new Step(Clearance, true, 4, 200, 30));
        Steps.add(new Step(Channel, true, 4, 150, 90));
        Steps.add(new Step("", false, 0, 0, 40));
        Steps.add(new Step(Motto, false, 12, 0, 260));
        return List.copyOf(Steps);
    }

    private final List<Step> Steps;
    private final int[] Lengths;
    private final int[] Start;
    private final int[] Typed;
    private final int[] Resolved;
    private final int Total;
    private long Begin = -1L;
    private boolean Skipped;

    public TerminalBoot(int[] NewLengths) {
        this(Script, NewLengths);
    }

    public TerminalBoot(List<Step> NewSteps, int[] NewLengths) {
        Steps = List.copyOf(NewSteps);
        int Size = Steps.size();
        Lengths = new int[Size];
        for (int Index = 0; Index < Size; Index++) {
            Lengths[Index] = Index < NewLengths.length ? Math.max(0, NewLengths[Index]) : 0;
        }
        double Raw = 0.0;
        for (int Index = 0; Index < Size; Index++) {
            Step Each = Steps.get(Index);
            Raw += Lengths[Index] * Each.CharMs() + Each.WorkMs() + Each.AfterMs();
        }
        double Scale = Raw > MaxMs ? MaxMs / Raw : 1.0;
        Start = new int[Size];
        Typed = new int[Size];
        Resolved = new int[Size];
        double Clock = 0.0;
        for (int Index = 0; Index < Size; Index++) {
            Step Each = Steps.get(Index);
            Start[Index] = (int) Math.round(Clock);
            Clock += Lengths[Index] * Each.CharMs() * Scale;
            Typed[Index] = (int) Math.round(Clock);
            Clock += Each.WorkMs() * Scale;
            Resolved[Index] = (int) Math.round(Clock);
            Clock += Each.AfterMs() * Scale;
        }
        Total = (int) Math.round(Clock);
    }

    public int total() {
        return Total;
    }

    public int lines() {
        return Steps.size();
    }

    public List<Step> steps() {
        return Steps;
    }

    public void begin(long Now) {
        if (Begin < 0L) {
            Begin = Now;
        }
    }

    public boolean started() {
        return Begin >= 0L || Skipped;
    }

    public void skip() {
        Skipped = true;
    }

    public boolean skipped() {
        return Skipped;
    }

    public long elapsed(long Now) {
        if (Skipped) {
            return Total + RevealMs + RowDelayMs + RowStepMs * 16L;
        }
        return Begin < 0L ? 0L : Math.max(0L, Now - Begin);
    }

    public boolean shown(int Line, long Elapsed) {
        return Line >= 0 && Line < Lengths.length && Elapsed >= Start[Line];
    }

    public int chars(int Line, long Elapsed) {
        if (!shown(Line, Elapsed)) {
            return 0;
        }
        if (Elapsed >= Typed[Line] || Typed[Line] <= Start[Line]) {
            return Lengths[Line];
        }
        double Share = (Elapsed - Start[Line]) / (double) (Typed[Line] - Start[Line]);
        return Math.min(Lengths[Line], (int) Math.floor(Share * Lengths[Line]));
    }

    public boolean resolved(int Line, long Elapsed) {
        return shown(Line, Elapsed) && Elapsed >= Resolved[Line];
    }

    public int current(long Elapsed) {
        for (int Index = Lengths.length - 1; Index >= 0; Index--) {
            if (shown(Index, Elapsed)) {
                return Index;
            }
        }
        return 0;
    }

    public boolean done(long Elapsed) {
        return Elapsed >= Total;
    }

    public boolean revealed(long Elapsed) {
        return Elapsed >= Total + RevealMs;
    }

    public int logoChars(long Elapsed, int Length) {
        if (Elapsed < Total) {
            return 0;
        }
        return (int) Math.min(Length, (Elapsed - Total) / LogoCharMs + 1);
    }

    public boolean rowShown(int Index, long Elapsed) {
        return Elapsed >= Total + RowDelayMs + (long) RowStepMs * Math.max(0, Index);
    }
}
