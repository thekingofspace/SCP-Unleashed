package net.rai.scp_unleashed.ultralaser;

import java.util.List;

public final class UltralaserScore {
    public static final float OriginTicks = 5.6F;
    public static final float PulseTicks = 6.25F;
    public static final float TextLeadTicks = 2.0F;
    public static final float FireLeadTicks = 1.0F;
    public static final int BuildTick = 60;
    public static final int CountTick = 105;
    public static final int TwoTick = 130;
    public static final int ThreeTick = 154;
    public static final int ReadyTick = 164;
    public static final int PrimeTicks = 180;
    public static final int OneHitTick = 106;
    public static final int OneEchoTick = 112;
    public static final int TwoHitTick = 131;
    public static final int TwoEchoTick = 137;
    public static final int ThreeHitTick = 156;
    public static final int ThreeEchoTick = 162;
    public static final int ReadyHitTick = 168;
    public static final int ReadyEchoTick = 174;
    public static final int DropTick = 181;
    private static final List<Cue> Cues = List.of(
            new Cue(0, "priming", false),
            new Cue(BuildTick, "building", false),
            new Cue(CountTick, "one", true),
            new Cue(TwoTick, "two", true),
            new Cue(ThreeTick, "three", true),
            new Cue(ReadyTick, "ready", true),
            new Cue(PrimeTicks, "fire", true));

    public record Cue(int Tick, String Key, boolean Loud) {
    }

    private UltralaserScore() {
    }

    public static List<Cue> cues() {
        return Cues;
    }

    public static int[] hits() {
        return new int[]{OneHitTick, OneEchoTick, TwoHitTick, TwoEchoTick, ThreeHitTick, ThreeEchoTick, ReadyHitTick, ReadyEchoTick, DropTick};
    }

    public static float fireTicks() {
        return PrimeTicks - FireLeadTicks;
    }

    public static float seconds() {
        return fireTicks() / 20.0F;
    }
}
