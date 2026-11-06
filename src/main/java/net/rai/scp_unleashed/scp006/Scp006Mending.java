package net.rai.scp_unleashed.scp006;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.AnomalyCure;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class Scp006Mending {
    public static final int Interval = 5;
    public static final int ChimeGap = 20;
    public static final int Sparkles = 8;
    public static final float FullEnough = 0.01f;
    public static final float ChimeVolume = 0.7f;
    private static final Map<LivingEntity, Long> LastCheck = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> LastChime = new WeakHashMap<>();

    private Scp006Mending() {
    }

    public static boolean submerged(LivingEntity Target, ServerLevel World, BlockPos Pos) {
        FluidState Liquid = World.getFluidState(Pos);
        return !Liquid.isEmpty() && Target.getBoundingBox().minY < Pos.getY() + Liquid.getHeight(World, Pos);
    }

    public static boolean touch(ServerLevel World, LivingEntity Target) {
        if (!Target.isAlive() || Target.isSpectator()) {
            return false;
        }
        long Now = World.getGameTime();
        Long Last = LastCheck.get(Target);
        if (Last != null && Now >= Last && Now - Last < Interval) {
            return false;
        }
        LastCheck.put(Target, Now);
        return mend(Target, null);
    }

    public static boolean mend(LivingEntity Target, @Nullable Entity Source) {
        if (!(Target.level() instanceof ServerLevel World) || !cure(Target, Source)) {
            return false;
        }
        feedback(World, Target);
        return true;
    }

    public static boolean cure(LivingEntity Target, @Nullable Entity Source) {
        if (!(Target.level() instanceof ServerLevel) || !Target.isAlive() || Target.isDeadOrDying()) {
            return false;
        }
        boolean Cured = AnomalyCure.cure(Target, Source);
        Cured |= clearHarmful(Target);
        if (Target.getRemainingFireTicks() > 0) {
            Target.clearFire();
            Cured = true;
        }
        if (Target.getTicksFrozen() > 0) {
            Target.setTicksFrozen(0);
            Cured = true;
        }
        Cured |= restore(Target);
        return Cured;
    }

    public static boolean clearHarmful(LivingEntity Target) {
        boolean Removed = false;
        for (MobEffectInstance Instance : List.copyOf(Target.getActiveEffects())) {
            Holder<MobEffect> Effect = Instance.getEffect();
            if (Effect.value().getCategory() == MobEffectCategory.HARMFUL && Target.removeEffect(Effect)) {
                Removed = true;
            }
        }
        return Removed;
    }

    public static boolean restore(LivingEntity Target) {
        if (Target.getType().is(Tags.EntityTypes.BOSSES)) {
            return false;
        }
        float Before = Target.getHealth();
        float Missing = Target.getMaxHealth() - Before;
        if (Missing <= 0.0f) {
            return false;
        }
        Target.heal(Missing);
        return Target.getHealth() - Before > FullEnough;
    }

    public static long lastChime(LivingEntity Target) {
        Long Last = LastChime.get(Target);
        return Last == null ? -1L : Last;
    }

    private static void feedback(ServerLevel World, LivingEntity Target) {
        double X = Target.getX();
        double Y = Target.getY() + Target.getBbHeight() * 0.5;
        double Z = Target.getZ();
        World.sendParticles(Scp006Registry.Glint.get(), X, Y, Z, Sparkles, Target.getBbWidth() * 0.45, Target.getBbHeight() * 0.35, Target.getBbWidth() * 0.45, 0.01);
        long Now = World.getGameTime();
        Long Last = LastChime.get(Target);
        if (Last != null && Now >= Last && Now - Last < ChimeGap) {
            return;
        }
        LastChime.put(Target, Now);
        float Pitch = 0.95f + World.getRandom().nextFloat() * 0.1f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp006Registry.ChimeLayers) {
            World.playSound(null, X, Y, Z, Layer.get(), Target.getSoundSource(), ChimeVolume, Pitch);
        }
    }
}
