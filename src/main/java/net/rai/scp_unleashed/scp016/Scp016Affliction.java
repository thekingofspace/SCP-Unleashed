package net.rai.scp_unleashed.scp016;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.gear.GearTags;
import net.rai.scp_unleashed.labware.LabExposure;
import net.rai.scp_unleashed.scp016.network.Scp016StatePayload;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public final class Scp016Affliction {
    public static final int Duration = 6000;
    public static final int Stages = 4;
    public static final int[] StageStarts = {0, 1200, 3000, 4800};
    public static final int[] SpeedLevels = {0, 0, 1, 2};
    public static final int[] StrengthLevels = {-1, 0, 1, 2};
    public static final int[] FleckEvery = {90, 45, 22, 9};
    public static final int RefreshTicks = 20;
    public static final int SyncTicks = 20;

    private Scp016Affliction() {
    }

    public static boolean isAfflicted(LivingEntity Subject) {
        return Subject.getExistingDataOrNull(Scp016Registry.Affliction.get()) != null;
    }

    public static boolean isDetectable(LivingEntity Subject) {
        return isAfflicted(Subject) || Subject instanceof Scp016Spawn;
    }

    public static int elapsed(LivingEntity Subject) {
        Integer Found = Subject.getExistingDataOrNull(Scp016Registry.Affliction.get());
        return Found == null ? 0 : Found;
    }

    public static int remaining(LivingEntity Subject) {
        return Math.max(0, Duration - elapsed(Subject));
    }

    public static int stageOf(int Elapsed) {
        int Stage = 1;
        for (int Index = 1; Index < StageStarts.length; Index++) {
            if (Elapsed >= StageStarts[Index]) {
                Stage = Index + 1;
            }
        }
        return Stage;
    }

    public static float progressOf(int Elapsed) {
        return Mth.clamp(Elapsed / (float) Duration, 0.0f, 1.0f);
    }

    public static boolean afflict(LivingEntity Subject) {
        if (!canCatch(Subject) || isAfflicted(Subject)) {
            return false;
        }
        Subject.setData(Scp016Registry.Affliction.get(), 0);
        applyBuffs(Subject, 1);
        if (Subject instanceof ServerPlayer User) {
            sync(User, 1, 0.0f);
        }
        return true;
    }

    public static boolean cure(LivingEntity Subject) {
        if (!isAfflicted(Subject)) {
            return false;
        }
        Subject.removeData(Scp016Registry.Affliction.get());
        clearBuffs(Subject);
        if (Subject instanceof ServerPlayer User) {
            sync(User, 0, 0.0f);
        }
        return true;
    }

    public static void forget(LivingEntity Subject) {
        Subject.removeData(Scp016Registry.Affliction.get());
        if (Subject instanceof ServerPlayer User) {
            sync(User, 0, 0.0f);
        }
    }

    public static boolean hasten(LivingEntity Subject, int Ticks) {
        if (!isAfflicted(Subject)) {
            return false;
        }
        int Elapsed = Math.min(Duration - 1, elapsed(Subject) + Math.max(0, Ticks));
        Subject.setData(Scp016Registry.Affliction.get(), Elapsed);
        int Stage = stageOf(Elapsed);
        applyBuffs(Subject, Stage);
        if (Subject instanceof ServerPlayer User) {
            sync(User, Stage, progressOf(Elapsed));
        }
        return true;
    }

    public static boolean canCatch(LivingEntity Subject) {
        if (Subject instanceof Scp016Spawn || !Subject.isAlive() || InfectionImmunity.isImmune(Subject)) {
            return false;
        }
        if (Subject instanceof Player Target) {
            return !Target.isCreative() && !Target.isSpectator();
        }
        return Subject instanceof AbstractVillager || ControlBodies.isPersonLike(Subject);
    }

    public static boolean isPaused(LivingEntity Subject) {
        return Subject instanceof Player Target && (Target.isCreative() || Target.isSpectator());
    }

    public static boolean isProtected(LivingEntity Subject) {
        return GearTags.wearsFullHazmatSuit(Subject);
    }

    public static void exposeAround(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Radius) {
        for (ServerPlayer Exposed : LabExposure.playersInSight(World, Source, Ignored, Radius, Target -> canCatch(Target) && !isAfflicted(Target) && !isProtected(Target))) {
            afflict(Exposed);
        }
        for (LivingEntity Exposed : ControlBodies.inSight(World, Source, Ignored, Radius, Target -> canCatch(Target) && !isAfflicted(Target) && !isProtected(Target))) {
            afflict(Exposed);
        }
        AABB Reach = AABB.ofSize(Source, Radius * 2.0, Radius * 2.0, Radius * 2.0);
        for (AbstractVillager Exposed : World.getEntitiesOfClass(AbstractVillager.class, Reach,
                Target -> Target.distanceToSqr(Source) <= Radius * Radius && canCatch(Target) && !isAfflicted(Target) && LabExposure.sees(World, Source, Target, Ignored))) {
            afflict(Exposed);
        }
    }

    public static void tick(LivingEntity Subject) {
        if (!(Subject.level() instanceof ServerLevel World) || !Subject.isAlive() || isPaused(Subject)) {
            return;
        }
        int Before = elapsed(Subject);
        int Now = Before + 1;
        Subject.setData(Scp016Registry.Affliction.get(), Now);
        if (Now >= Duration) {
            finish(Subject);
            return;
        }
        int Stage = stageOf(Now);
        boolean Changed = Stage != stageOf(Before);
        if (Changed) {
            announce(World, Subject, Stage);
        }
        if (Changed || Math.floorMod(Now + Subject.getId(), RefreshTicks) == 0) {
            applyBuffs(Subject, Stage);
        }
        if (Subject instanceof ServerPlayer User && (Changed || Now % SyncTicks == 0)) {
            sync(User, Stage, progressOf(Now));
        }
        if (Math.floorMod(Now + Subject.getId() * 7, FleckEvery[Stage - 1]) == 0) {
            AABB Box = Subject.getBoundingBox();
            World.sendParticles(Scp016Registry.Fleck.get(), Box.minX + World.getRandom().nextDouble() * Box.getXsize(), Box.minY + World.getRandom().nextDouble() * Box.getYsize(),
                    Box.minZ + World.getRandom().nextDouble() * Box.getZsize(), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public static void finish(LivingEntity Subject) {
        DamageSource Source = Subject.damageSources().source(Scp016Registry.SplitDamage);
        Subject.hurt(Source, Float.MAX_VALUE);
        if (Subject.isAlive()) {
            Subject.setHealth(0.0f);
            Subject.die(Source);
        }
    }

    private static void announce(ServerLevel World, LivingEntity Subject, int Stage) {
        float Pitch = 1.1f - 0.12f * Stage;
        if (Subject instanceof ServerPlayer User) {
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp016Registry.StageLayers) {
                User.playNotifySound(Layer.get(), SoundSource.PLAYERS, 0.8f, Pitch);
            }
        } else {
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp016Registry.StageLayers) {
                World.playSound(null, Subject.getX(), Subject.getY(), Subject.getZ(), Layer.get(), SoundSource.NEUTRAL, 0.6f, Pitch);
            }
        }
        World.sendParticles(Scp016Registry.Fleck.get(), Subject.getX(), Subject.getY() + Subject.getBbHeight() * 0.55, Subject.getZ(), 4 + Stage * 2,
                Subject.getBbWidth() * 0.4, Subject.getBbHeight() * 0.3, Subject.getBbWidth() * 0.4, 0.01);
    }

    public static void applyBuffs(LivingEntity Subject, int Stage) {
        int Index = Mth.clamp(Stage, 1, Stages) - 1;
        buff(Subject, MobEffects.MOVEMENT_SPEED, SpeedLevels[Index]);
        buff(Subject, MobEffects.DAMAGE_BOOST, StrengthLevels[Index]);
    }

    public static void clearBuffs(LivingEntity Subject) {
        if (isOurs(Subject.getEffect(MobEffects.MOVEMENT_SPEED))) {
            Subject.removeEffect(MobEffects.MOVEMENT_SPEED);
        }
        if (isOurs(Subject.getEffect(MobEffects.DAMAGE_BOOST))) {
            Subject.removeEffect(MobEffects.DAMAGE_BOOST);
        }
    }

    private static void buff(LivingEntity Subject, Holder<MobEffect> Effect, int Amplifier) {
        MobEffectInstance Current = Subject.getEffect(Effect);
        if (Amplifier < 0) {
            if (isOurs(Current)) {
                Subject.removeEffect(Effect);
            }
            return;
        }
        if (Current != null && (Current.getAmplifier() > Amplifier || isOurs(Current) && Current.getAmplifier() == Amplifier)) {
            return;
        }
        Subject.addEffect(new MobEffectInstance(Effect, MobEffectInstance.INFINITE_DURATION, Amplifier, true, false, true));
    }

    private static boolean isOurs(@Nullable MobEffectInstance Instance) {
        return Instance != null && Instance.isInfiniteDuration() && Instance.isAmbient() && !Instance.isVisible();
    }

    public static void sync(ServerPlayer User, int Stage, float Progress) {
        if (User.connection != null) {
            PacketDistributor.sendToPlayer(User, new Scp016StatePayload(Stage, Progress));
        }
    }
}
