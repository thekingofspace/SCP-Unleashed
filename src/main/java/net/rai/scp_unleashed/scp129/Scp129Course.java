package net.rai.scp_unleashed.scp129;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp129Course {
    public static final int Length = 7200;
    public static final int Stages = 4;
    public static final int[] StageStarts = {0, 1500, 3600, 5400};
    public static final int Symptomatic = 400;
    public static final int[] DamageEvery = {400, 200, 120, 60};
    public static final float[] DamageAmount = {1.0f, 1.0f, 2.0f, 2.0f};
    public static final float[] HealShare = {0.75f, 0.5f, 0.25f, 0.0f};
    public static final int[] WeaknessLevels = {-1, 0, 1, 1};
    public static final int[] SlownessLevels = {-1, -1, 0, 1};
    public static final int[] FatigueLevels = {-1, -1, -1, 0};
    public static final int EffectRefresh = 40;
    public static final int EffectTicks = 100;
    public static final List<Holder<MobEffect>> Effects = List.of(MobEffects.WEAKNESS, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.DIG_SLOWDOWN);

    private Scp129Course() {
    }

    public static int stageOf(int Course) {
        if (Course <= 0) {
            return 0;
        }
        int Stage = 1;
        for (int Index = 1; Index < StageStarts.length; Index++) {
            if (Course >= StageStarts[Index]) {
                Stage = Index + 1;
            }
        }
        return Stage;
    }

    public static int symptomStage(Scp129Infection.State Current) {
        return Current.isLethal() && Current.Course() >= Symptomatic ? stageOf(Current.Course()) : 0;
    }

    public static float healShare(LivingEntity Subject) {
        Scp129Infection.State Current = Subject.getExistingDataOrNull(Scp129Registry.Infection.get());
        if (Current == null || !Current.isLethal() || Current.Course() <= 0 || Scp129Infection.isPaused(Subject)) {
            return 1.0f;
        }
        return HealShare[Math.min(Stages, stageOf(Current.Course())) - 1];
    }

    public static int secondsLeft(int Course) {
        return Math.max(0, (Length - Course + 19) / 20);
    }

    public static void advance(LivingEntity Subject, ServerLevel World, int Before, int After) {
        int Stage = stageOf(After);
        int Index = Stage - 1;
        if (Before > 0 && stageOf(Before) != Stage) {
            announce(Subject, World, Stage);
        }
        if (Before <= 0 || Before / EffectRefresh != After / EffectRefresh) {
            applyEffects(Subject, Stage);
        }
        int Every = DamageEvery[Index];
        if (Every > 0 && Before / Every != After / Every) {
            Subject.hurt(source(Subject), DamageAmount[Index]);
        }
        if (After >= Length) {
            Subject.hurt(source(Subject), Float.MAX_VALUE);
        }
    }

    public static DamageSource source(LivingEntity Subject) {
        return Subject.damageSources().source(Scp129Registry.SporeDamage);
    }

    private static void announce(LivingEntity Subject, ServerLevel World, int Stage) {
        float Pitch = Scp129Symptoms.voice(Subject) * 0.94f;
        World.playSound(null, Subject.getX(), Subject.getEyeY(), Subject.getZ(), Scp129Registry.Wheeze.get(), Subject instanceof Player ? SoundSource.PLAYERS : SoundSource.NEUTRAL,
                0.7f, Pitch);
        if (Subject instanceof ServerPlayer User && Stage >= 2) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp129.stage." + Stage).withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC), true);
        }
    }

    public static void applyEffects(LivingEntity Subject, int Stage) {
        if (Stage <= 0) {
            return;
        }
        int Index = Math.min(Stages, Stage) - 1;
        effect(Subject, MobEffects.WEAKNESS, WeaknessLevels[Index]);
        effect(Subject, MobEffects.MOVEMENT_SLOWDOWN, SlownessLevels[Index]);
        effect(Subject, MobEffects.DIG_SLOWDOWN, FatigueLevels[Index]);
    }

    private static void effect(LivingEntity Subject, Holder<MobEffect> Effect, int Amplifier) {
        if (Amplifier < 0) {
            return;
        }
        MobEffectInstance Current = Subject.getEffect(Effect);
        if (Current != null && !isOurs(Current) && Current.getAmplifier() >= Amplifier) {
            return;
        }
        Subject.addEffect(new MobEffectInstance(Effect, EffectTicks, Amplifier, true, false, true));
    }

    public static void clearEffects(LivingEntity Subject) {
        for (Holder<MobEffect> Effect : Effects) {
            if (isOurs(Subject.getEffect(Effect))) {
                Subject.removeEffect(Effect);
            }
        }
    }

    private static boolean isOurs(@Nullable MobEffectInstance Instance) {
        return Instance != null && Instance.isAmbient() && !Instance.isVisible() && !Instance.isInfiniteDuration() && Instance.getDuration() <= EffectTicks;
    }
}
