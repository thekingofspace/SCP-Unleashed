package net.rai.scp_unleashed.terminal;

import java.util.ArrayList;
import java.util.List;

public final class TerminalLog {
    public static final int Limit = 64;
    public static final int FirstGapMs = 280;
    public static final int MinGapMs = 380;
    public static final int GapSpanMs = 380;
    public static final int CountStep = 24;

    public record Entry(long At, TerminalPhase Phase, String Key, List<String> Args, String Raw, String Label) {
        public String tag() {
            return TerminalTags.tag(label());
        }

        public String label() {
            return Label == null ? Phase.Tag : Label;
        }
    }

    private final List<Entry> Entries = new ArrayList<>();
    private final long Seed;
    private TerminalPhase Phase;
    private long NextFlavour;
    private int FlavourStep;
    private int LastDecile = -1;
    private int LastCount = -1;
    private String LastRaw;

    public TerminalLog(long NewSeed) {
        Seed = NewSeed;
    }

    public List<Entry> entries() {
        return Entries;
    }

    public TerminalPhase phase() {
        return Phase;
    }

    public void update(long Now, TerminalPhase Next, int Percent, int Count, String Subject) {
        String About = Subject == null ? "" : Subject;
        if (Next != Phase) {
            Phase = Next;
            LastDecile = -1;
            LastCount = -1;
            FlavourStep = 0;
            NextFlavour = Now + FirstGapMs;
            add(Now, Next.openKey(), List.of(About), null);
        }
        if (Percent >= 0 && Phase.Progressive) {
            int Decile = Math.min(10, TerminalProgress.clamp(Percent) / 10);
            if (Decile > LastDecile) {
                LastDecile = Decile;
                add(Now, Phase.progressKey(), List.of(String.valueOf(TerminalProgress.clamp(Percent)), About), null);
            }
        }
        if (Count >= 0 && Phase.Counted && (LastCount < 0 ? Count > 0 : Count - LastCount >= CountStep)) {
            LastCount = Count;
            add(Now, Phase.countKey(), List.of(String.valueOf(Count), About), null);
        }
        if (Phase.Flavours > 0 && FlavourStep < Phase.Flavours && Now >= NextFlavour) {
            add(Now, Phase.flavourKey(order(FlavourStep, Phase.Flavours)), List.of(About), null);
            FlavourStep++;
            NextFlavour = Now + gap(FlavourStep);
        }
    }

    public void raw(long Now, TerminalPhase Which, String Text) {
        if (Text == null || Text.isBlank() || Text.equals(LastRaw)) {
            return;
        }
        LastRaw = Text;
        Entries.add(new Entry(Now, Which, null, List.of(), Text, null));
        trim();
    }

    public void note(long Now, String Label, String Key, List<String> Args) {
        Entries.add(new Entry(Now, Phase == null ? TerminalPhase.Prepare : Phase, Key, List.copyOf(Args), null, Label));
        trim();
    }

    public int order(int Step, int Count) {
        return (int) Math.floorMod(Step + Math.floorMod(TerminalMotd.mix(Seed), (long) Count), (long) Count);
    }

    public long gap(int Step) {
        return MinGapMs + Math.floorMod(TerminalMotd.mix(Seed + Step * 17L), (long) GapSpanMs);
    }

    private void add(long Now, String Key, List<String> Args, String Raw) {
        Entries.add(new Entry(Now, Phase == null ? TerminalPhase.Prepare : Phase, Key, Args, Raw, null));
        trim();
    }

    private void trim() {
        while (Entries.size() > Limit) {
            Entries.remove(0);
        }
    }
}
