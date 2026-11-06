package net.rai.scp_unleashed.scp939.compressor;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class AmnesticEffect extends MobEffect {
    public static final int Colour = 0xA88894;

    public AmnesticEffect() {
        super(MobEffectCategory.NEUTRAL, Colour);
    }
}
