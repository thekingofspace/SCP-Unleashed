package net.rai.scp_unleashed.antimemetic;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class AntiMemeticEffect extends MobEffect {
    public static final int Colour = 0xB8A4EE;

    public AntiMemeticEffect() {
        super(MobEffectCategory.BENEFICIAL, Colour);
    }
}
