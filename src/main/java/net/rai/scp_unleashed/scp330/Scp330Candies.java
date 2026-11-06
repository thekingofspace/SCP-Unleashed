package net.rai.scp_unleashed.scp330;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class Scp330Candies {
    public static final int Duration = 600;
    public static final int RegenerationLevel = 4;
    public static final float WrathMultiplier = 5.0f;

    private Scp330Candies() {
    }

    public static void eat(LivingEntity Eater, Scp330Colour Colour) {
        switch (Colour) {
            case Blue -> Scp330Barrier.raise(Eater);
            case Red -> Eater.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Duration, RegenerationLevel));
            case Green -> Eater.addEffect(new MobEffectInstance(Scp330Registry.Bloom, Duration, 0, false, false, true));
            case Purple -> Eater.addEffect(new MobEffectInstance(Scp330Registry.Sight, Duration, 0, false, false, true));
            case Black -> Eater.addEffect(new MobEffectInstance(Scp330Registry.Wrath, Duration, 0, false, false, true));
            case Pink -> Scp330Pop.pop(Eater);
        }
    }
}
