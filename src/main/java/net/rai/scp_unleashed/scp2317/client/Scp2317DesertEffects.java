package net.rai.scp_unleashed.scp2317.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

public class Scp2317DesertEffects extends DimensionSpecialEffects {
    public static final Vec3 Haze = new Vec3(0.80, 0.55, 0.38);

    public Scp2317DesertEffects() {
        super(Float.NaN, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 FogColour, float Brightness) {
        return Haze.scale(0.55 + 0.45 * Brightness);
    }

    @Override
    public boolean isFoggyAt(int X, int Z) {
        return false;
    }
}
