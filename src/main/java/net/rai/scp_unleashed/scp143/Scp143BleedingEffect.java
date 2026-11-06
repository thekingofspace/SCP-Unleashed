package net.rai.scp_unleashed.scp143;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class Scp143BleedingEffect extends MobEffect {
    public static final int Colour = 0x8E1824;

    public Scp143BleedingEffect() {
        super(MobEffectCategory.HARMFUL, Colour);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int Duration, int Amplifier) {
        return Duration % Scp143Bleeding.interval(Amplifier) == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity Target, int Amplifier) {
        Scp143Bleeding.tick(Target, Amplifier);
        return true;
    }

    @Override
    public ParticleOptions createParticleOptions(MobEffectInstance Effect) {
        return Scp143Registry.Drip.get();
    }
}
