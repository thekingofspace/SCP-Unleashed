package net.rai.scp_unleashed.scp4666.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

public class Scp4666WinterEffects extends DimensionSpecialEffects {
    public static final Vec3 DayHaze = new Vec3(0.74, 0.78, 0.84);
    public static final Vec3 NightHaze = new Vec3(0.07, 0.08, 0.11);

    public Scp4666WinterEffects() {
        super(Float.NaN, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 FogColour, float Brightness) {
        return NightHaze.lerp(DayHaze, Math.max(0.0f, Math.min(1.0f, Brightness)));
    }

    @Override
    public boolean isFoggyAt(int X, int Z) {
        return true;
    }

    @Override
    public float[] getSunriseColor(float TimeOfDay, float PartialTick) {
        return null;
    }
}
