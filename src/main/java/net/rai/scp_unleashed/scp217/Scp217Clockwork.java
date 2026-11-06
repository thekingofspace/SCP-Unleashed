package net.rai.scp_unleashed.scp217;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.gear.GearTags;
import net.rai.scp_unleashed.labware.LabExposure;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public final class Scp217Clockwork {
    public static final int Duration = 36000;
    public static final int Stages = 4;
    public static final int[] StageStarts = {0, 9600, 19200, 28800};
    public static final int WindingTicks = 600;
    public static final int RetryTicks = 100;
    public static final int RefreshTicks = 20;
    public static final int[] SpeedLevels = {0, 0, -1, -1};
    public static final int[] StrengthLevels = {0, 0, -1, -1};
    public static final int[] SlownessLevels = {-1, -1, 0, 2};
    public static final int[] FatigueLevels = {-1, -1, 0, 0};
    public static final int[] WeaknessLevels = {-1, -1, -1, 0};
    public static final int WindingSlowness = 3;
    public static final int WindingFatigue = 1;
    public static final int WindingWeakness = 1;
    public static final int DragStage = 2;
    public static final double DragSpeed = -0.1;
    public static final double DragDamage = -1.5;
    public static final ResourceLocation DragId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp217_drag");
    public static final int[] PerfectSpeedLevels = {0, 0, 1, 1};
    public static final int[] PerfectStrengthLevels = {0, 0, 0, 1};
    public static final int PerfectCap = Duration - WindingTicks - 1;
    public static final int PulseStage = 3;
    public static final List<Holder<MobEffect>> Worked = List.of(MobEffects.MOVEMENT_SPEED, MobEffects.DAMAGE_BOOST, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.DIG_SLOWDOWN,
            MobEffects.WEAKNESS);

    private Scp217Clockwork() {
    }

    public static boolean isInfected(LivingEntity Subject) {
        return Subject.getExistingDataOrNull(Scp217Registry.Clockwork.get()) != null;
    }

    public static boolean isPerfect(LivingEntity Subject) {
        return isInfected(Subject) && Boolean.TRUE.equals(Subject.getExistingDataOrNull(Scp217Registry.Perfect.get()));
    }

    public static int limit(LivingEntity Subject) {
        return isPerfect(Subject) ? PerfectCap : Duration;
    }

    public static int elapsed(LivingEntity Subject) {
        Integer Found = Subject.getExistingDataOrNull(Scp217Registry.Clockwork.get());
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

    public static boolean isWinding(int Elapsed) {
        return Elapsed >= Duration - WindingTicks;
    }

    public static float windingProgress(int Elapsed) {
        return Mth.clamp((Elapsed - (Duration - WindingTicks)) / (float) WindingTicks, 0.0f, 1.0f);
    }

    public static int stageOf(LivingEntity Subject) {
        return isInfected(Subject) ? stageOf(elapsed(Subject)) : 0;
    }

    public static boolean canCatch(LivingEntity Subject) {
        if (!Subject.isAlive() || InfectionImmunity.isImmune(Subject)) {
            return false;
        }
        if (Subject instanceof Player Target) {
            return !Target.isCreative() && !Target.isSpectator();
        }
        return ControlBodies.isPersonLike(Subject);
    }

    public static boolean isPaused(LivingEntity Subject) {
        return Subject instanceof Player Target && (Target.isCreative() || Target.isSpectator());
    }

    public static boolean isProtected(LivingEntity Subject) {
        return GearTags.wearsFullHazmatSuit(Subject);
    }

    public static boolean infect(LivingEntity Subject) {
        return infect(Subject, false);
    }

    public static boolean infect(LivingEntity Subject, boolean Perfect) {
        if (!canCatch(Subject) || isInfected(Subject)) {
            return false;
        }
        Subject.setData(Scp217Registry.Clockwork.get(), 0);
        mark(Subject, Perfect);
        applyEffects(Subject, 1, false);
        Scp217Sync.update(Subject);
        return true;
    }

    public static boolean perfect(LivingEntity Subject) {
        if (!isInfected(Subject)) {
            return infect(Subject, true);
        }
        if (isPerfect(Subject)) {
            return false;
        }
        mark(Subject, true);
        int Elapsed = Math.min(elapsed(Subject), PerfectCap);
        Subject.setData(Scp217Registry.Clockwork.get(), Elapsed);
        applyEffects(Subject, stageOf(Elapsed), false);
        Scp217Sync.update(Subject);
        if (stageOf(Elapsed) >= PulseStage) {
            Scp217Pulse.awaken(Subject);
        }
        return true;
    }

    private static void mark(LivingEntity Subject, boolean Perfect) {
        if (Perfect) {
            Subject.setData(Scp217Registry.Perfect.get(), Boolean.TRUE);
        } else {
            Subject.removeData(Scp217Registry.Perfect.get());
        }
        Subject.removeData(Scp217Registry.PulseReady.get());
    }

    public static boolean cure(LivingEntity Subject) {
        if (!isInfected(Subject)) {
            return false;
        }
        Subject.removeData(Scp217Registry.Clockwork.get());
        Subject.removeData(Scp217Registry.Perfect.get());
        Subject.removeData(Scp217Registry.PulseReady.get());
        clearEffects(Subject);
        Scp217Sync.update(Subject);
        return true;
    }

    public static void forget(LivingEntity Subject) {
        Subject.removeData(Scp217Registry.Clockwork.get());
        Subject.removeData(Scp217Registry.Perfect.get());
        Subject.removeData(Scp217Registry.PulseReady.get());
        clearDrag(Subject);
        Scp217Sync.update(Subject);
    }

    public static boolean hasten(LivingEntity Subject, int Ticks) {
        if (!isInfected(Subject)) {
            return false;
        }
        int Before = elapsed(Subject);
        int Elapsed = Mth.clamp(Before + Ticks, 0, isPerfect(Subject) ? PerfectCap : Duration - 1);
        Subject.setData(Scp217Registry.Clockwork.get(), Elapsed);
        applyEffects(Subject, stageOf(Elapsed), isWinding(Elapsed));
        Scp217Sync.update(Subject);
        if (isPerfect(Subject) && stageOf(Before) < PulseStage && stageOf(Elapsed) >= PulseStage) {
            Scp217Pulse.awaken(Subject);
        }
        return true;
    }

    public static void exposeAround(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Radius) {
        exposeAround(World, Source, Ignored, Radius, false);
    }

    public static void exposeAround(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Radius, boolean Perfect) {
        for (ServerPlayer Exposed : LabExposure.playersInSight(World, Source, Ignored, Radius, Target -> canCatch(Target) && !isInfected(Target) && !isProtected(Target))) {
            infect(Exposed, Perfect);
        }
        for (LivingEntity Exposed : ControlBodies.inSight(World, Source, Ignored, Radius, Target -> canCatch(Target) && !isInfected(Target) && !isProtected(Target))) {
            infect(Exposed, Perfect);
        }
    }

    public static void tick(LivingEntity Subject) {
        if (!(Subject.level() instanceof ServerLevel World) || !Subject.isAlive() || isPaused(Subject)) {
            return;
        }
        boolean Perfect = isPerfect(Subject);
        int Before = elapsed(Subject);
        int Now = Math.min(Perfect ? PerfectCap : Duration, Before + 1);
        Subject.setData(Scp217Registry.Clockwork.get(), Now);
        int Stage = stageOf(Now);
        boolean Winding = isWinding(Now);
        boolean Changed = Stage != stageOf(Before) || Winding != isWinding(Before);
        if (Changed) {
            Scp217Ambience.shift(World, Subject, Stage, Perfect);
            if (Perfect && Stage == PulseStage) {
                Scp217Pulse.awaken(Subject);
            }
        }
        if (Changed || Math.floorMod(Now + Subject.getId(), RefreshTicks) == 0) {
            applyEffects(Subject, Stage, Winding);
        }
        Scp217Sync.update(Subject);
        Scp217Ambience.tick(World, Subject, Now, Stage, Winding, Perfect);
        if (Perfect) {
            Scp217Pulse.tick(World, Subject);
            return;
        }
        if (Now < Duration) {
            return;
        }
        Scp217Trace Trace = Subject.getData(Scp217Registry.Trace.get());
        if (Trace.Retry > 0) {
            Trace.Retry--;
            return;
        }
        Trace.Retry = RetryTicks;
        finish(World, Subject);
    }

    public static void finish(ServerLevel World, LivingEntity Subject) {
        Scp217Ambience.windDown(World, Subject);
        DamageSource Source = Subject.damageSources().source(Scp217Registry.WoundDown);
        Subject.hurt(Source, Float.MAX_VALUE);
    }

    public static void applyEffects(LivingEntity Subject, int Stage, boolean Winding) {
        int Index = Mth.clamp(Stage, 1, Stages) - 1;
        if (isPerfect(Subject)) {
            effect(Subject, MobEffects.MOVEMENT_SPEED, PerfectSpeedLevels[Index]);
            effect(Subject, MobEffects.DAMAGE_BOOST, PerfectStrengthLevels[Index]);
            effect(Subject, MobEffects.MOVEMENT_SLOWDOWN, -1);
            effect(Subject, MobEffects.DIG_SLOWDOWN, -1);
            effect(Subject, MobEffects.WEAKNESS, -1);
            clearDrag(Subject);
            return;
        }
        effect(Subject, MobEffects.MOVEMENT_SPEED, SpeedLevels[Index]);
        effect(Subject, MobEffects.DAMAGE_BOOST, StrengthLevels[Index]);
        effect(Subject, MobEffects.MOVEMENT_SLOWDOWN, Winding ? WindingSlowness : SlownessLevels[Index]);
        effect(Subject, MobEffects.DIG_SLOWDOWN, Winding ? WindingFatigue : FatigueLevels[Index]);
        effect(Subject, MobEffects.WEAKNESS, Winding ? WindingWeakness : WeaknessLevels[Index]);
        if (Stage == DragStage && !Winding) {
            drag(Subject.getAttribute(Attributes.MOVEMENT_SPEED), DragSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            drag(Subject.getAttribute(Attributes.ATTACK_DAMAGE), DragDamage, AttributeModifier.Operation.ADD_VALUE);
        } else {
            clearDrag(Subject);
        }
    }

    public static void clearEffects(LivingEntity Subject) {
        for (Holder<MobEffect> Effect : Worked) {
            if (isOurs(Subject.getEffect(Effect))) {
                Subject.removeEffect(Effect);
            }
        }
        clearDrag(Subject);
    }

    public static boolean hasDrag(LivingEntity Subject) {
        AttributeInstance Speed = Subject.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance Damage = Subject.getAttribute(Attributes.ATTACK_DAMAGE);
        return Speed != null && Speed.hasModifier(DragId) || Damage != null && Damage.hasModifier(DragId);
    }

    public static void clearDrag(LivingEntity Subject) {
        undrag(Subject.getAttribute(Attributes.MOVEMENT_SPEED));
        undrag(Subject.getAttribute(Attributes.ATTACK_DAMAGE));
    }

    private static void drag(@Nullable AttributeInstance Instance, double Amount, AttributeModifier.Operation Operation) {
        if (Instance != null && !Instance.hasModifier(DragId)) {
            Instance.addTransientModifier(new AttributeModifier(DragId, Amount, Operation));
        }
    }

    private static void undrag(@Nullable AttributeInstance Instance) {
        if (Instance != null && Instance.hasModifier(DragId)) {
            Instance.removeModifier(DragId);
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
}
