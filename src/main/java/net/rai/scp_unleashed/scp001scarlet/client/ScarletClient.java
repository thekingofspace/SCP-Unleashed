package net.rai.scp_unleashed.scp001scarlet.client;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp001scarlet.network.ScarletSyncPayload;

public final class ScarletClient {
    public static final float FillRate = 0.004f;
    public static final float SkyRate = 0.003f;
    private static float Manifestation;
    private static float Shown;
    private static float ShownO;
    private static int Broken;
    private static ScarletState.Phase Phase = ScarletState.Phase.Asleep;
    private static boolean Seen;
    private static float SkyTarget;
    private static float Sky;
    private static float SkyO;
    private static boolean HasThrone;
    private static BlockPos Throne = BlockPos.ZERO;
    private static long Clock;

    private ScarletClient() {
    }

    public static void receive(ScarletSyncPayload Payload) {
        Manifestation = Mth.clamp(Payload.Manifestation(), 0.0f, 1.0f);
        Broken = Payload.Broken();
        ScarletState.Phase[] All = ScarletState.Phase.values();
        Phase = All[Mth.clamp(Payload.Phase(), 0, All.length - 1)];
        if (!Seen && Payload.Seen()) {
            Shown = Manifestation;
            ShownO = Manifestation;
        }
        Seen = Payload.Seen();
        SkyTarget = Mth.clamp(Payload.Sky(), 0.0f, 1.0f);
        HasThrone = Payload.HasThrone();
        Throne = Payload.Throne();
    }

    public static void reset() {
        Manifestation = 0.0f;
        Shown = 0.0f;
        ShownO = 0.0f;
        Broken = 0;
        Phase = ScarletState.Phase.Asleep;
        Seen = false;
        SkyTarget = 0.0f;
        Sky = 0.0f;
        SkyO = 0.0f;
        HasThrone = false;
        Throne = BlockPos.ZERO;
    }

    public static void tick() {
        Clock++;
        ShownO = Shown;
        Shown += Mth.clamp(Manifestation - Shown, -FillRate, FillRate);
        SkyO = Sky;
        Sky += Mth.clamp(SkyTarget - Sky, -SkyRate, SkyRate);
    }

    public static float shown(float Partial) {
        return Mth.lerp(Partial, ShownO, Shown);
    }

    public static float sky(float Partial) {
        return Mth.lerp(Partial, SkyO, Sky);
    }

    public static float manifestation() {
        return Manifestation;
    }

    public static int broken() {
        return Broken;
    }

    public static ScarletState.Phase phase() {
        return Phase;
    }

    public static boolean seen() {
        return Seen;
    }

    public static boolean meterVisible() {
        return Seen && (Phase == ScarletState.Phase.Stirring || Phase == ScarletState.Phase.Xk2 || Phase == ScarletState.Phase.Rite || Phase == ScarletState.Phase.Fight);
    }

    public static boolean beamVisible() {
        return HasThrone && (Phase == ScarletState.Phase.Xk2 || Phase == ScarletState.Phase.Rite || Phase == ScarletState.Phase.Fight);
    }

    public static BlockPos throne() {
        return Throne;
    }

    public static long clock() {
        return Clock;
    }
}
