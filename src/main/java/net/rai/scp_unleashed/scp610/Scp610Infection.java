package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustColorTransitionOptions;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.gear.GearTags;
import net.rai.scp_unleashed.labware.LabContact;
import net.rai.scp_unleashed.labware.LabContactEvents;
import net.rai.scp_unleashed.labware.LabExposure;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Predicate;

public final class Scp610Infection {
    public static final int Duration = 48000;
    public static final int Stages = 4;
    public static final int[] StageStarts = {0, 9600, 21600, 33600};
    public static final int MobConvertTicks = 2400;
    public static final int RefreshTicks = 20;
    public static final int FleckTicks = 40;
    public static final float[] StrikeChances = {0.15f, 0.3f, 0.5f, 0.75f};
    public static final float[] TouchChances = {0.0f, 0.05f, 0.1f, 0.2f};
    public static final float CoughChance = 0.6f;
    public static final float MobStrikeChance = 0.2f;
    public static final int CoughStage = 3;
    public static final int CoughMin = 600;
    public static final int CoughMax = 1200;
    public static final double CoughRadius = 2.5;
    public static final int ApostleStage = 3;
    public static final int CureStages = 2;
    public static final int[] HungerLevels = {-1, 0, 0, 1};
    public static final int[] SlownessLevels = {-1, -1, 0, 1};
    public static final int[] WeaknessLevels = {-1, -1, 0, 0};
    public static final int[] FatigueLevels = {-1, -1, -1, 0};
    public static final List<Holder<MobEffect>> Worked = List.of(MobEffects.HUNGER, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS, MobEffects.DIG_SLOWDOWN);
    public static final DustColorTransitionOptions Spore = new DustColorTransitionOptions(new Vector3f(0.42f, 0.16f, 0.18f), new Vector3f(0.23f, 0.1f, 0.09f), 1.4f);
    public static final DustColorTransitionOptions Speck = new DustColorTransitionOptions(new Vector3f(0.55f, 0.29f, 0.3f), new Vector3f(0.35f, 0.15f, 0.15f), 0.7f);

    private Scp610Infection() {
    }

    public static boolean isInfected(LivingEntity Subject) {
        return Subject.getExistingDataOrNull(Scp610Registry.Infection.get()) != null;
    }

    public static int elapsed(LivingEntity Subject) {
        Integer Found = Subject.getExistingDataOrNull(Scp610Registry.Infection.get());
        return Found == null ? 0 : Found;
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

    public static int stageOf(LivingEntity Subject) {
        return isInfected(Subject) ? stageOf(elapsed(Subject)) : 0;
    }

    public static boolean isSuited(LivingEntity Subject) {
        return GearTags.wearsFullHazmatSuit(Subject);
    }

    public static final List<Class<?>> Bloodless = List.of(AbstractGolem.class, AbstractSkeleton.class, Slime.class, Blaze.class, Vex.class, Allay.class, EnderDragon.class,
            WitherBoss.class, Warden.class, Ghast.class, Breeze.class, SkeletonHorse.class);

    public static boolean bloodless(LivingEntity Subject) {
        for (Class<?> Kind : Bloodless) {
            if (Kind.isInstance(Subject)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canCatch(LivingEntity Subject) {
        if (!Subject.isAlive() || Subject instanceof Scp610Creature || bloodless(Subject) || InfectionImmunity.isImmune(Subject, Scp610Registry.Immune)) {
            return false;
        }
        if (Subject instanceof Player User) {
            return !User.isCreative() && !User.isSpectator();
        }
        return true;
    }

    public static boolean isPaused(LivingEntity Subject) {
        return Subject instanceof Player User && (User.isCreative() || User.isSpectator());
    }

    public static boolean infect(LivingEntity Subject) {
        if (!canCatch(Subject) || isInfected(Subject) || isSuited(Subject)) {
            return false;
        }
        Subject.setData(Scp610Registry.Infection.get(), 0);
        applyEffects(Subject, 1);
        Scp610Sync.update(Subject);
        return true;
    }

    public static void setElapsed(LivingEntity Subject, int Ticks) {
        int Elapsed = Mth.clamp(Ticks, 0, Subject instanceof Player ? Duration : MobConvertTicks);
        Subject.setData(Scp610Registry.Infection.get(), Elapsed);
        applyEffects(Subject, stageOf(Elapsed));
        Scp610Sync.update(Subject);
    }

    public static void setStage(LivingEntity Subject, int Stage) {
        setElapsed(Subject, StageStarts[Mth.clamp(Stage, 1, Stages) - 1]);
    }

    public static boolean cure(LivingEntity Subject) {
        if (!isInfected(Subject)) {
            return false;
        }
        Subject.removeData(Scp610Registry.Infection.get());
        clearEffects(Subject);
        Scp610Sync.update(Subject);
        return true;
    }

    public static boolean treat(LivingEntity Subject) {
        if (!isInfected(Subject)) {
            return false;
        }
        int Stage = stageOf(Subject);
        if (Stage <= CureStages) {
            return cure(Subject);
        }
        setElapsed(Subject, StageStarts[Stage - 2]);
        return true;
    }

    public static void forget(LivingEntity Subject) {
        Subject.removeData(Scp610Registry.Infection.get());
        Scp610Sync.update(Subject);
    }

    public static boolean exposable(LivingEntity Target) {
        return canCatch(Target) && !isInfected(Target) && !isSuited(Target);
    }

    public static int exposeAround(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Radius) {
        int Count = 0;
        for (ServerPlayer Exposed : LabExposure.playersInSight(World, Source, Ignored, Radius, Scp610Infection::exposable)) {
            if (infect(Exposed)) {
                Count++;
            }
        }
        AABB Reach = AABB.ofSize(Source, Radius * 2.0, Radius * 2.0, Radius * 2.0);
        for (LivingEntity Exposed : World.getEntitiesOfClass(LivingEntity.class, Reach, Target -> !(Target instanceof Player) && Target.distanceToSqr(Source) <= Radius * Radius
                && exposable(Target) && LabExposure.sees(World, Source, Target, Ignored))) {
            if (infect(Exposed)) {
                Count++;
            }
        }
        return Count;
    }

    public static void cloud(ServerLevel World, Vec3 At, double Radius, int Count) {
        World.sendParticles(Spore, At.x, At.y, At.z, Count, Radius * 0.45, Radius * 0.25, Radius * 0.45, 0.02);
        World.sendParticles(Scp610Registry.SporeParticle.get(), At.x, At.y, At.z, Math.max(2, Count / 2), Radius * 0.4, Radius * 0.25, Radius * 0.4, 0.01);
    }

    public static void tick(LivingEntity Subject) {
        if (!(Subject.level() instanceof ServerLevel World) || !Subject.isAlive() || isPaused(Subject)) {
            return;
        }
        if (!(Subject instanceof Player) && InfectionImmunity.isImmune(Subject, Scp610Registry.Immune)) {
            cure(Subject);
            return;
        }
        int Before = elapsed(Subject);
        int Now = Before + 1;
        Subject.setData(Scp610Registry.Infection.get(), Now);
        if (!(Subject instanceof Player)) {
            if (Math.floorMod(Now + Subject.getId(), FleckTicks) == 0) {
                AABB Box = Subject.getBoundingBox();
                World.sendParticles(Speck, Subject.getX(), Box.minY + Box.getYsize() * 0.6, Subject.getZ(), 2, Box.getXsize() * 0.3, Box.getYsize() * 0.25, Box.getZsize() * 0.3, 0.0);
            }
            if (Now >= MobConvertTicks) {
                Scp610Hosts.convert(World, Subject);
            }
            return;
        }
        int Stage = stageOf(Now);
        boolean Changed = Stage != stageOf(Before);
        if (Changed) {
            worsen(World, Subject, Stage);
        }
        if (Changed || Math.floorMod(Now + Subject.getId(), RefreshTicks) == 0) {
            applyEffects(Subject, Stage);
        }
        if (Changed) {
            Scp610Sync.update(Subject);
        }
        if (Stage >= CoughStage && !isSuited(Subject)) {
            Scp610Trace Trace = Subject.getData(Scp610Registry.Trace.get());
            if (Trace.NextCough <= 0L) {
                Trace.NextCough = World.getGameTime() + CoughMin + World.random.nextInt(CoughMax - CoughMin);
            } else if (World.getGameTime() >= Trace.NextCough) {
                Trace.NextCough = World.getGameTime() + CoughMin + World.random.nextInt(CoughMax - CoughMin);
                cough(World, Subject);
            }
        }
        if (Now >= Duration) {
            finish(Subject);
        }
    }

    public static int cough(ServerLevel World, LivingEntity Carrier) {
        Vec3 Mouth = Carrier.getEyePosition().add(Carrier.getLookAngle().scale(0.4));
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp610Sounds.CoughLayers) {
            World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Layer.get(), SoundSource.PLAYERS, 0.9f, 0.9f + World.random.nextFloat() * 0.2f);
        }
        World.sendParticles(Spore, Mouth.x, Mouth.y, Mouth.z, 10, 0.35, 0.2, 0.35, 0.01);
        World.sendParticles(Scp610Registry.SporeParticle.get(), Mouth.x, Mouth.y, Mouth.z, 6, 0.25, 0.15, 0.25, 0.01);
        if (isSuited(Carrier)) {
            return 0;
        }
        int Count = 0;
        AABB Reach = AABB.ofSize(Mouth, CoughRadius * 2.0, CoughRadius * 2.0, CoughRadius * 2.0);
        for (LivingEntity Near : World.getEntitiesOfClass(LivingEntity.class, Reach, Target -> Target != Carrier && Target.distanceToSqr(Mouth) <= CoughRadius * CoughRadius
                && LabExposure.sees(World, Mouth, Target, LabExposure.NothingIgnored))) {
            if (LabContactEvents.transmit(Scp610Registry.organism(), Carrier, Near, LabContact.Cough)) {
                Count++;
            }
        }
        return Count;
    }

    private static void worsen(ServerLevel World, LivingEntity Subject, int Stage) {
        float Pitch = 1.05f - 0.1f * Stage;
        if (Subject instanceof ServerPlayer User) {
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp610Sounds.StageLayers) {
                User.playNotifySound(Layer.get(), SoundSource.PLAYERS, 0.8f, Pitch);
            }
        }
        AABB Box = Subject.getBoundingBox();
        World.sendParticles(Speck, Subject.getX(), Box.minY + Box.getYsize() * 0.6, Subject.getZ(), 6 + Stage * 3, Box.getXsize() * 0.4, Box.getYsize() * 0.3, Box.getZsize() * 0.4, 0.0);
    }

    public static void finish(LivingEntity Subject) {
        DamageSource Source = Subject.damageSources().source(Scp610Registry.Consumed);
        Subject.hurt(Source, Float.MAX_VALUE);
        if (Subject.isAlive()) {
            Subject.setHealth(0.0f);
            Subject.die(Source);
        }
    }

    public static boolean diedOfIt(LivingEntity Fallen, DamageSource Source) {
        return Source.is(Scp610Registry.Consumed) || isInfected(Fallen) && stageOf(Fallen) >= ApostleStage;
    }

    public static void applyEffects(LivingEntity Subject, int Stage) {
        if (!(Subject instanceof Player)) {
            return;
        }
        int Index = Mth.clamp(Stage, 1, Stages) - 1;
        effect(Subject, MobEffects.HUNGER, HungerLevels[Index]);
        effect(Subject, MobEffects.MOVEMENT_SLOWDOWN, SlownessLevels[Index]);
        effect(Subject, MobEffects.WEAKNESS, WeaknessLevels[Index]);
        effect(Subject, MobEffects.DIG_SLOWDOWN, FatigueLevels[Index]);
    }

    public static void clearEffects(LivingEntity Subject) {
        for (Holder<MobEffect> Effect : Worked) {
            if (isOurs(Subject.getEffect(Effect))) {
                Subject.removeEffect(Effect);
            }
        }
    }

    private static void effect(LivingEntity Subject, Holder<MobEffect> Effect, int Amplifier) {
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
        if (isOurs(Current)) {
            Subject.removeEffect(Effect);
        }
        Subject.addEffect(new MobEffectInstance(Effect, MobEffectInstance.INFINITE_DURATION, Amplifier, true, false, true));
    }

    public static boolean isOurs(@Nullable MobEffectInstance Instance) {
        return Instance != null && Instance.isInfiniteDuration() && Instance.isAmbient() && !Instance.isVisible();
    }

    public static float strikeChance(LivingEntity Carrier) {
        if (Carrier instanceof Scp610Creature Creature) {
            return Creature.form().strikeChance();
        }
        if (Carrier instanceof Player) {
            return StrikeChances[Mth.clamp(stageOf(Carrier), 1, Stages) - 1];
        }
        return Carrier instanceof Mob ? MobStrikeChance : 0.0f;
    }

    public static float touchChance(LivingEntity Carrier) {
        if (Carrier instanceof Scp610Creature Creature) {
            return Creature.form().mobile() ? Creature.form().touchChance() : 0.0f;
        }
        if (Carrier instanceof Player) {
            return TouchChances[Mth.clamp(stageOf(Carrier), 1, Stages) - 1];
        }
        return 0.0f;
    }
}
