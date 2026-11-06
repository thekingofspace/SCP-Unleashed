package net.rai.scp_unleashed.scp939.compressor;

import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.antimemetic.AntiMemeticPillsItem;
import net.rai.scp_unleashed.antimemetic.AntiMemeticRegistry;
import net.rai.scp_unleashed.scp939.Scp939Breath;
import net.rai.scp_unleashed.scp939.Scp939Registry;

import java.util.ArrayList;
import java.util.List;

public final class Amnestics {
    public static final int FallbackTicks = 2400;
    public static final int BaseTicks = 600;
    public static final int TicksPerLevel = 300;
    public static final int MaxLevel = 3;
    public static final float HighExposure = 0.5f;
    public static final float ExposurePerUse = 6.0f;
    public static final float ExposurePerLevel = 3.0f;

    private Amnestics() {
    }

    public static boolean refreshable(Holder<MobEffect> Effect) {
        return Effect.is(Scp939Registry.Amnestics);
    }

    public static int fullTicks(Holder<MobEffect> Effect, int Level) {
        if (Effect.is(AntiMemeticRegistry.Shielded)) {
            return AntiMemeticPillsItem.EffectTicks;
        }
        if (Effect.is(Scp939Registry.Amnestic)) {
            return amnesticTicks(Level);
        }
        return FallbackTicks;
    }

    public static int amnesticTicks(int Level) {
        return BaseTicks + TicksPerLevel * Mth.clamp(Level, 0, MaxLevel);
    }

    public static List<Holder<MobEffect>> refresh(LivingEntity User) {
        List<Holder<MobEffect>> Refreshed = new ArrayList<>();
        for (MobEffectInstance Active : new ArrayList<>(User.getActiveEffects())) {
            Holder<MobEffect> Effect = Active.getEffect();
            if (!refreshable(Effect) || Active.isInfiniteDuration()) {
                continue;
            }
            int Full = fullTicks(Effect, Active.getAmplifier());
            if (Active.getDuration() >= Full) {
                continue;
            }
            User.removeEffectNoUpdate(Effect);
            User.addEffect(new MobEffectInstance(Effect, Full, Active.getAmplifier(), Active.isAmbient(), Active.isVisible(), Active.showIcon()));
            Refreshed.add(Effect);
        }
        return Refreshed;
    }

    public static float strain(LivingEntity User) {
        return User.hasData(Scp939Registry.Strain) ? User.getData(Scp939Registry.Strain) : 0.0f;
    }

    public static int level(float Strain, float Exposure) {
        int Level = Math.max(0, (int) Math.floor(Strain) - 1) + (Exposure >= HighExposure ? 1 : 0);
        return Mth.clamp(Level, 0, MaxLevel);
    }

    public static int inhale(LivingEntity User) {
        float Strain = strain(User) + 1.0f;
        User.setData(Scp939Registry.Strain, Strain);
        int Level = level(Strain, Scp939Breath.share(User));
        refresh(User);
        int Ticks = amnesticTicks(Level);
        MobEffectInstance Current = User.getEffect(Scp939Registry.Amnestic);
        if (Current != null && Current.getAmplifier() > Level) {
            Level = Current.getAmplifier();
            Ticks = Math.max(Ticks, amnesticTicks(Level));
        }
        if (Current != null) {
            User.removeEffectNoUpdate(Scp939Registry.Amnestic);
        }
        User.addEffect(new MobEffectInstance(Scp939Registry.Amnestic, Ticks, Level, false, true, true));
        Scp939Breath.expose(User, ExposurePerUse + ExposurePerLevel * Level);
        return Level;
    }
}
