package net.rai.scp_unleashed.daevite.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.daevite.network.DaeviteSkyPayload;
import org.jetbrains.annotations.Nullable;

public final class DaeviteSkyClient {
    public static final int Stale = 60;
    public static final float Response = 0.04f;
    public static final float FogRed = 0.17f;
    public static final float FogGreen = 0.025f;
    public static final float FogBlue = 0.03f;
    public static final float SkyFar = 3.0f;
    public static final float LandFar = 44.0f;
    @Nullable
    private static BlockPos Centre;
    private static float Target;
    private static int Radius = 96;
    private static long Seen = -1000L;
    private static float Current;
    private static float Previous;

    private DaeviteSkyClient() {
    }

    public static void receive(DaeviteSkyPayload Payload) {
        Minecraft Game = Minecraft.getInstance();
        Centre = Payload.Centre();
        Target = Mth.clamp(Payload.Strength(), 0.0f, 1.0f);
        Radius = Math.max(16, Payload.Radius());
        Seen = Game.level != null ? Game.level.getGameTime() : 0L;
    }

    public static void tick(Minecraft Game) {
        Previous = Current;
        float Want = 0.0f;
        if (Game.level != null && Game.player != null && Centre != null && Game.level.getGameTime() - Seen < Stale) {
            Vec3 Eye = Game.player.getEyePosition();
            double Dx = Eye.x - (Centre.getX() + 0.5);
            double Dz = Eye.z - (Centre.getZ() + 0.5);
            double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
            float Near = Distance <= Radius ? 1.0f : (float) Mth.clamp(1.0 - (Distance - Radius) / (Radius * 0.6), 0.0, 1.0);
            Want = Target * Near;
        }
        Current += (Want - Current) * Response;
        if (Current < 0.002f && Want <= 0.0f) {
            Current = 0.0f;
        }
    }

    public static float strength(float Partial) {
        return Mth.lerp(Partial, Previous, Current);
    }

    public static void reset() {
        Centre = null;
        Current = 0.0f;
        Previous = 0.0f;
        Target = 0.0f;
    }
}
