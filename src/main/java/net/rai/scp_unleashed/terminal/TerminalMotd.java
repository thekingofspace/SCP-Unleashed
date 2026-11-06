package net.rai.scp_unleashed.terminal;

public final class TerminalMotd {
    public static final int Count = 20;
    public static final int IdleCount = 12;
    public static final String MotdPrefix = "terminal.scp_unleashed.motd.";
    public static final String IdlePrefix = "terminal.scp_unleashed.idle.";
    public static final String[] IdleTags = {
            TerminalTags.Net, TerminalTags.Env, TerminalTags.Sec, TerminalTags.Ok, TerminalTags.Sys, TerminalTags.Sec,
            TerminalTags.Env, TerminalTags.Net, TerminalTags.Sys, TerminalTags.Ok, TerminalTags.Sec, TerminalTags.Env};
    public static final int IdleMinMs = 4200;
    public static final int IdleSpanMs = 4600;

    private TerminalMotd() {
    }

    public static int pick(long Seed) {
        return (int) Math.floorMod(mix(Seed), (long) Count);
    }

    public static String motdKey(int Index) {
        return MotdPrefix + Math.floorMod(Index, Count);
    }

    public static String idleKey(int Index) {
        return IdlePrefix + Math.floorMod(Index, IdleCount);
    }

    public static String idleTag(int Index) {
        return IdleTags[Math.floorMod(Index, IdleCount)];
    }

    public static int idleIndex(long Seed, int Step) {
        return (int) Math.floorMod(mix(Seed) + Step * 5L, (long) IdleCount);
    }

    public static long idleGap(long Seed, int Step) {
        return IdleMinMs + Math.floorMod(mix(Seed + Step * 31L), (long) IdleSpanMs);
    }

    public static long mix(long Value) {
        long Mixed = Value * 0x9E3779B97F4A7C15L;
        Mixed ^= Mixed >>> 29;
        Mixed *= 0xBF58476D1CE4E5B9L;
        return Mixed ^ (Mixed >>> 32);
    }
}
