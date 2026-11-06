package net.rai.scp_unleashed.scp009;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.EffectCure;

import java.util.Set;

public class DoomedEffect extends MobEffect {
    public static final int Colour = 0xA3192C;

    public DoomedEffect() {
        super(MobEffectCategory.HARMFUL, Colour);
    }

    @Override
    public void fillEffectCures(Set<EffectCure> Cures, MobEffectInstance Instance) {
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int Duration, int Amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity Target, int Amplifier) {
        return Scp009Doom.tick(Target);
    }

    @Override
    public void onEffectAdded(LivingEntity Target, int Amplifier) {
        super.onEffectAdded(Target, Amplifier);
        if (!Target.level().isClientSide) {
            Scp009Doom.onset(Target);
        }
    }
}
