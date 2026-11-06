package net.rai.scp_unleashed.scp548;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class Scp548FrostbiteEffect extends MobEffect {
    public static final int Colour = 0x9ED8F2;

    public Scp548FrostbiteEffect() {
        super(MobEffectCategory.HARMFUL, Colour);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int Duration, int Amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity Target, int Amplifier) {
        return Scp548Chill.frostbite(Target, Amplifier);
    }
}
