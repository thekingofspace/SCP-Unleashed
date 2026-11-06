package net.rai.scp_unleashed.scp261;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class Scp261Effect extends MobEffect {
    private final int Interval;
    @Nullable
    private final Pulse Beat;

    public Scp261Effect(MobEffectCategory Category, int Colour) {
        this(Category, Colour, 0, null);
    }

    public Scp261Effect(MobEffectCategory Category, int Colour, int Interval, @Nullable Pulse Beat) {
        super(Category, Colour);
        this.Interval = Interval;
        this.Beat = Beat;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int Duration, int Amplifier) {
        return Beat != null && Interval > 0 && Duration % Interval == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity Target, int Amplifier) {
        if (Beat != null) {
            Beat.pulse(Target, Amplifier);
        }
        return true;
    }

    @FunctionalInterface
    public interface Pulse {
        void pulse(LivingEntity Target, int Amplifier);
    }
}
