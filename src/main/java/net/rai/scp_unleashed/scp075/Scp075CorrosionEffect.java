package net.rai.scp_unleashed.scp075;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class Scp075CorrosionEffect extends MobEffect {
    public static final int Colour = 0x6E4A22;

    public Scp075CorrosionEffect() {
        super(MobEffectCategory.HARMFUL, Colour);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int Duration, int Amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity Target, int Amplifier) {
        return Scp075Corrosion.tick(Target, Amplifier);
    }

    @Override
    public ParticleOptions createParticleOptions(MobEffectInstance Effect) {
        return Scp075Registry.Drip.get();
    }
}
