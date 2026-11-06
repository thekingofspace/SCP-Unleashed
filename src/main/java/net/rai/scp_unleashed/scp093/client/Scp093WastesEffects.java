package net.rai.scp_unleashed.scp093.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

public class Scp093WastesEffects extends DimensionSpecialEffects {
    public static final Vec3 Haze = new Vec3(0.902, 0.749, 0.518);
    public static final float[] Glow = new float[]{0.98f, 0.62f, 0.26f, 0.5f};

    public Scp093WastesEffects() {
        super(Float.NaN, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 FogColour, float Brightness) {
        return Haze;
    }

    @Override
    public boolean isFoggyAt(int X, int Z) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float TimeOfDay, float PartialTick) {
        return Glow.clone();
    }
}
