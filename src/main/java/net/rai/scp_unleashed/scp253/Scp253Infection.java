package net.rai.scp_unleashed.scp253;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.gear.GearTags;
import net.rai.scp_unleashed.labware.LabExposure;
import net.rai.scp_unleashed.labware.LabOrganisms;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Predicate;

public final class Scp253Infection {
    public static final int Duration = 36000;
    public static final int Stages = 5;
    public static final int[] StageStarts = {0, 4800, 12000, 20400, 28800};
    public static final float[] SlowLevels = {0.0f, 0.1f, 0.22f, 0.36f, 0.5f, 0.65f};
    public static final float[] HungerLevels = {0.002f, 0.006f, 0.013f, 0.024f, 0.04f, 0.055f};
    public static final int[] BreathEvery = {0, 0, 0, 160, 90};
    public static final int RefreshTicks = 20;
    public static final int SyncTicks = 100;
    public static final int FinishRetry = 20;
    public static final double BurstRadius = 4.0;
    public static final DustParticleOptions Fleck = new DustParticleOptions(new Vector3f(0.45f, 0.6f, 0.86f), 0.7f);
    public static final DustParticleOptions Crumble = new DustParticleOptions(new Vector3f(0.5f, 0.62f, 0.8f), 1.3f);

    private Scp253Infection() {
    }

    @Nullable
    public static Scp253State state(LivingEntity Subject) {
        return Subject.getExistingDataOrNull(Scp253Registry.Infection.get());
    }

    public static boolean isInfected(LivingEntity Subject) {
        return state(Subject) != null;
    }

    public static int elapsed(LivingEntity Subject) {
        Scp253State Found = state(Subject);
        return Found == null ? 0 : Found.Elapsed();
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

    public static float progressOf(float Elapsed) {
        return Mth.clamp(Elapsed / Duration, 0.0f, 1.0f);
    }

    private static float level(float[] Levels, int Elapsed) {
        int Index = stageOf(Elapsed) - 1;
        int Start = StageStarts[Index];
        int End = Index + 1 < Stages ? StageStarts[Index + 1] : Duration;
        float Share = Mth.clamp((Elapsed - Start) / (float) (End - Start), 0.0f, 1.0f);
        return Mth.lerp(Share, Levels[Index], Levels[Index + 1]);
    }

    public static float slownessAt(int Elapsed) {
        return level(SlowLevels, Elapsed);
    }

    public static float hungerAt(int Elapsed) {
        return level(HungerLevels, Elapsed);
    }

    public static int growthsAt(int Elapsed) {
        return Scp253Growths.count(progressOf(Elapsed));
    }

    public static boolean canCatch(LivingEntity Subject) {
        if (Subject instanceof ArmorStand || !Subject.isAlive() || InfectionImmunity.isImmune(Subject)) {
            return false;
        }
        if (Subject instanceof Player Target) {
            return !Target.isCreative() && !Target.isSpectator();
        }
        return Subject.canBeSeenAsEnemy();
    }

    public static boolean isPaused(LivingEntity Subject) {
        return Subject instanceof Player Target && (Target.isCreative() || Target.isSpectator());
    }

    public static boolean isProtected(LivingEntity Subject) {
        return GearTags.wearsFullHazmatSuit(Subject);
    }

    public static boolean infect(LivingEntity Subject) {
        if (!canCatch(Subject) || isInfected(Subject)) {
            return false;
        }
        Scp253State Fresh = new Scp253State(0, Subject.getRandom().nextLong());
        Subject.setData(Scp253Registry.Infection.get(), Fresh);
        apply(Subject, 0);
        show(Subject, Fresh);
        return true;
    }

    public static boolean cure(LivingEntity Subject) {
        if (!isInfected(Subject)) {
            return false;
        }
        Subject.removeData(Scp253Registry.Infection.get());
        clear(Subject);
        return true;
    }

    public static void clear(LivingEntity Subject) {
        AttributeInstance Speed = Subject.getAttribute(Attributes.MOVEMENT_SPEED);
        if (Speed != null) {
            Speed.removeModifier(Scp253Registry.SlownessId);
        }
        if (Subject.getExistingDataOrNull(Scp253Registry.Display.get()) != null) {
            Subject.removeData(Scp253Registry.Display.get());
        }
    }

    public static boolean setElapsed(LivingEntity Subject, int Ticks) {
        Scp253State Found = state(Subject);
        if (Found == null) {
            return false;
        }
        Scp253State Moved = Found.at(Mth.clamp(Ticks, 0, Duration - 1));
        Subject.setData(Scp253Registry.Infection.get(), Moved);
        apply(Subject, Moved.Elapsed());
        show(Subject, Moved);
        return true;
    }

    public static void exposeAround(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Radius) {
        for (ServerPlayer Exposed : LabExposure.playersInSight(World, Source, Ignored, Radius, Target -> canCatch(Target) && !isInfected(Target) && !isProtected(Target))) {
            infect(Exposed);
        }
        for (LivingEntity Exposed : ControlBodies.inSight(World, Source, Ignored, Radius, Target -> canCatch(Target) && !isInfected(Target) && !isProtected(Target))) {
            infect(Exposed);
        }
        AABB Reach = AABB.ofSize(Source, Radius * 2.0, Radius * 2.0, Radius * 2.0);
        for (AbstractVillager Exposed : World.getEntitiesOfClass(AbstractVillager.class, Reach,
                Target -> Target.distanceToSqr(Source) <= Radius * Radius && canCatch(Target) && !isInfected(Target) && LabExposure.sees(World, Source, Target, Ignored))) {
            infect(Exposed);
        }
    }

    public static void tick(LivingEntity Subject) {
        if (!(Subject.level() instanceof ServerLevel World) || !Subject.isAlive() || isPaused(Subject)) {
            return;
        }
        if (InfectionImmunity.isImmune(Subject)) {
            cure(Subject);
            return;
        }
        Scp253State Before = state(Subject);
        if (Before == null) {
            return;
        }
        if (Before.Elapsed() >= Duration) {
            if (Math.floorMod(Subject.tickCount, FinishRetry) == 0) {
                finish(Subject);
            }
            return;
        }
        int Now = Before.Elapsed() + 1;
        Scp253State After = Before.at(Now);
        Subject.setData(Scp253Registry.Infection.get(), After);
        if (Now >= Duration) {
            finish(Subject);
            return;
        }
        int Stage = stageOf(Now);
        boolean Changed = Stage != stageOf(Before.Elapsed());
        if (Changed) {
            announce(Subject, Stage);
        }
        if (Changed || Math.floorMod(Now + Subject.getId(), RefreshTicks) == 0) {
            apply(Subject, Now);
        }
        drain(Subject, Now);
        boolean Sprouted = growthsAt(Now) > growthsAt(Before.Elapsed());
        if (Sprouted) {
            sprout(World, Subject);
        }
        if (Changed || Sprouted || Math.floorMod(Now + Subject.getId(), SyncTicks) == 0 || Subject.getExistingDataOrNull(Scp253Registry.Display.get()) == null) {
            show(Subject, After);
        }
        int Every = BreathEvery[Stage - 1];
        if (Every > 0 && Math.floorMod(Now + Subject.getId() * 13, Every) == 0) {
            play(World, Subject, Scp253Registry.BreathLayers, 0.55f, 0.92f + World.getRandom().nextFloat() * 0.1f);
        }
    }

    public static void apply(LivingEntity Subject, int Elapsed) {
        AttributeInstance Speed = Subject.getAttribute(Attributes.MOVEMENT_SPEED);
        if (Speed == null) {
            return;
        }
        double Amount = -slownessAt(Elapsed);
        AttributeModifier Existing = Speed.getModifier(Scp253Registry.SlownessId);
        if (Existing == null || Math.abs(Existing.amount() - Amount) > 1.0e-4) {
            Speed.addOrUpdateTransientModifier(new AttributeModifier(Scp253Registry.SlownessId, Amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    public static void drain(LivingEntity Subject, int Elapsed) {
        float Rate = hungerAt(Elapsed);
        if (Subject instanceof Player User) {
            User.causeFoodExhaustion(Rate);
        } else if (Subject instanceof DClass Body) {
            Body.food().addExhaustion(Rate);
        }
    }

    public static void show(LivingEntity Subject, Scp253State Current) {
        Subject.setData(Scp253Registry.Display.get(), new Scp253Display(Current.Elapsed(), Subject.level().getGameTime(), Current.Seed()));
    }

    private static void announce(LivingEntity Subject, int Stage) {
        if (Subject instanceof ServerPlayer User && Stage > 1) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp253.stage." + Stage).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    private static void sprout(ServerLevel World, LivingEntity Subject) {
        play(World, Subject, Scp253Registry.GrowthLayers, 0.5f, 0.9f + World.getRandom().nextFloat() * 0.2f);
        AABB Box = Subject.getBoundingBox();
        World.sendParticles(Fleck, Subject.getX(), Box.minY + Box.getYsize() * 0.55, Subject.getZ(), 6, Box.getXsize() * 0.35, Box.getYsize() * 0.3, Box.getZsize() * 0.35, 0.0);
    }

    public static void finish(LivingEntity Subject) {
        DamageSource Source = Subject.damageSources().source(Scp253Registry.OvergrowthDamage);
        Subject.hurt(Source, Float.MAX_VALUE);
        if (Subject instanceof ServerPlayer User && ControlManager.hasPendingDeath(User)) {
            return;
        }
        if (Subject.isAlive()) {
            Subject.setHealth(0.0f);
            Subject.die(Source);
        }
    }

    public static void burst(ServerLevel World, LivingEntity Fallen) {
        play(World, Fallen, Scp253Registry.OvergrownLayers, 0.9f, 0.9f + World.getRandom().nextFloat() * 0.15f);
        AABB Box = Fallen.getBoundingBox();
        Vec3 Centre = Box.getCenter();
        World.sendParticles(Crumble, Centre.x, Centre.y, Centre.z, 36, Box.getXsize() * 0.45, Box.getYsize() * 0.4, Box.getZsize() * 0.45, 0.0);
        World.sendParticles(Fleck, Centre.x, Centre.y, Centre.z, 24, Box.getXsize() * 0.6, Box.getYsize() * 0.5, Box.getZsize() * 0.6, 0.02);
        LabOrganisms.find(Scp253Organism.Id).ifPresent(Organism -> LabOrganisms.spread(World, Centre, LabExposure.NothingIgnored, Organism, BurstRadius, Fallen));
    }

    private static void play(ServerLevel World, LivingEntity Source, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        SoundSource Channel = Source instanceof Player ? SoundSource.PLAYERS : SoundSource.NEUTRAL;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Source.getX(), Source.getY() + Source.getBbHeight() * 0.6, Source.getZ(), Layer.get(), Channel, Volume, Pitch);
        }
    }
}
