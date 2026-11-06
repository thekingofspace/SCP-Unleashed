package net.rai.scp_unleashed.z0002.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.z0002.network.Z0002DomainPayload;
import org.jetbrains.annotations.Nullable;

public final class Z0002ClientDomain {
    public static final float FogRed = 0.38f;
    public static final float FogGreen = 0.04f;
    public static final float FogBlue = 0.05f;
    public static final float FogNear = 2.0f;
    public static final float FogFar = 46.0f;
    public static final int FadeTicks = 30;
    @Nullable
    private static Vec3 Centre;
    private static int Radius;
    private static int Strength;
    private static int StrengthBefore;

    private Z0002ClientDomain() {
    }

    public static void accept(Z0002DomainPayload Payload) {
        if (Payload.Inside()) {
            Centre = new Vec3(Payload.X(), Payload.Y(), Payload.Z());
            Radius = Payload.Radius();
        } else {
            Centre = null;
        }
    }

    public static boolean inside(Minecraft Game) {
        return Centre != null && Game.player != null && Game.player.position().distanceTo(Centre) <= Radius + 2.0;
    }

    public static void tick(Minecraft Game) {
        StrengthBefore = Strength;
        if (inside(Game)) {
            Strength = Math.min(FadeTicks, Strength + 1);
        } else {
            Strength = Math.max(0, Strength - 1);
        }
    }

    public static float strength(float PartialTick) {
        return Mth.lerp(PartialTick, StrengthBefore, Strength) / FadeTicks;
    }

    public static void reset() {
        Centre = null;
        Strength = 0;
        StrengthBefore = 0;
    }
}
