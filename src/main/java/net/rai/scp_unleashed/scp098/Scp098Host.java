package net.rai.scp_unleashed.scp098;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.gear.GearTags;
import net.rai.scp_unleashed.labware.LabExposure;
import net.rai.scp_unleashed.scp148.Scp148Shield;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;

public final class Scp098Host {
    public static final int StageOneTicks = 6000;
    public static final int StageTwoTicks = 6000;
    public static final double Range = 160.0;
    public static final int NudgeMin = 500;
    public static final int NudgeMax = 1100;
    public static final float NudgeWalk = 3.0f;
    public static final int UrgeMin = 400;
    public static final int UrgeMax = 800;
    public static final double LookCone = Math.cos(Math.toRadians(45.0));
    public static final int LookInterval = 10;
    public static final int BlindTicks = 50;
    public static final int SlowLevel = 1;
    public static final double BurstReach = 7.0;
    public static final int BurstInterval = 5;
    public static final float BurstDamage = 14.0f;
    public static final int StunTicks = 60;
    public static final int CheckInterval = 40;
    public static final int NudgeLines = 8;
    public static final int UrgeLines = 5;
    public static final String Prefix = "message.scp_unleashed.scp098.";
    public static final DustParticleOptions Grit = new DustParticleOptions(new Vector3f(0.84f, 0.76f, 0.55f), 1.2f);
    private static final Map<LivingEntity, Float> Walked = new WeakHashMap<>();

    private Scp098Host() {
    }

    public record State(ResourceLocation Dimension, BlockPos Hive, int Stage, int Ticks, int Nudge) {
        public static final State None = new State(ResourceLocation.withDefaultNamespace("overworld"), BlockPos.ZERO, 0, 0, 0);
        public static final Codec<State> StateCodec = RecordCodecBuilder.create(Instance -> Instance.group(
                ResourceLocation.CODEC.fieldOf("dimension").forGetter(State::Dimension),
                BlockPos.CODEC.fieldOf("hive").forGetter(State::Hive),
                Codec.INT.optionalFieldOf("stage", 0).forGetter(State::Stage),
                Codec.INT.optionalFieldOf("ticks", 0).forGetter(State::Ticks),
                Codec.INT.optionalFieldOf("nudge", 0).forGetter(State::Nudge)
        ).apply(Instance, State::new));

        public boolean active() {
            return Stage > 0;
        }

        public State withStage(int Next, int Delay) {
            return new State(Dimension, Hive, Next, 0, Delay);
        }

        public State withTicks(int Next, int NextNudge) {
            return new State(Dimension, Hive, Stage, Next, NextNudge);
        }
    }

    public static State stateOf(LivingEntity Subject) {
        State Found = Subject.getExistingDataOrNull(Scp098Registry.Host.get());
        return Found == null ? State.None : Found;
    }

    public static boolean isHost(LivingEntity Subject) {
        return stateOf(Subject).active();
    }

    public static boolean canCarry(LivingEntity Subject) {
        if (!Subject.isAlive() || InfectionImmunity.isImmune(Subject)) {
            return false;
        }
        if (Subject instanceof Player User) {
            return !User.isCreative() && !User.isSpectator();
        }
        return ControlBodies.isPersonLike(Subject);
    }

    public static boolean isProtected(LivingEntity Subject) {
        return GearTags.wearsFullHazmatSuit(Subject);
    }

    public static boolean canHost(LivingEntity Subject) {
        return canCarry(Subject) && !isHost(Subject) && !isProtected(Subject);
    }

    public static boolean infect(LivingEntity Subject, ServerLevel World, BlockPos Hive) {
        if (!canCarry(Subject) || isHost(Subject)) {
            return false;
        }
        store(Subject, new State(World.dimension().location(), Hive.immutable(), 1, 0, 0));
        return true;
    }

    public static void setStage(LivingEntity Subject, int Stage) {
        State Current = stateOf(Subject);
        if (!Current.active()) {
            return;
        }
        store(Subject, Current.withStage(Mth.clamp(Stage, 1, 3), nudgeDelay(Subject.getRandom(), Stage)));
    }

    public static boolean cure(LivingEntity Subject) {
        boolean Was = isHost(Subject);
        Subject.removeData(Scp098Registry.Host.get());
        Walked.remove(Subject);
        return Was;
    }

    private static void store(LivingEntity Subject, State Next) {
        if (Next.active()) {
            Subject.setData(Scp098Registry.Host.get(), Next);
        } else {
            Subject.removeData(Scp098Registry.Host.get());
        }
    }

    private static int nudgeDelay(RandomSource Random, int Stage) {
        return Stage >= 3 ? UrgeMin + Random.nextInt(UrgeMax - UrgeMin + 1) : NudgeMin + Random.nextInt(NudgeMax - NudgeMin + 1);
    }

    public static Vec3 hiveCentre(State Current) {
        return Vec3.atCenterOf(Current.Hive());
    }

    @Nullable
    public static ServerLevel hiveLevel(LivingEntity Subject, State Current) {
        if (!(Subject.level() instanceof ServerLevel World) || !World.dimension().location().equals(Current.Dimension())) {
            return null;
        }
        return World;
    }

    public static boolean hiveStands(LivingEntity Subject, State Current) {
        MinecraftServer Server = Subject.getServer();
        if (Server == null) {
            return true;
        }
        ServerLevel Home = Server.getLevel(ResourceKey.create(Registries.DIMENSION, Current.Dimension()));
        return Home != null && Scp098Hives.get(Home).contains(Current.Hive());
    }

    public static boolean inRange(LivingEntity Subject, State Current) {
        return hiveLevel(Subject, Current) != null && Subject.position().distanceToSqr(hiveCentre(Current)) <= Range * Range;
    }

    public static Vec3 lookOf(LivingEntity Subject) {
        if (Subject instanceof Player) {
            return Subject.getViewVector(1.0f);
        }
        return Vec3.directionFromRotation(Subject.getXRot(), Subject.getYHeadRot());
    }

    public static boolean facesHive(LivingEntity Subject, State Current) {
        Vec3 Toward = hiveCentre(Current).subtract(Subject.getEyePosition());
        if (Toward.lengthSqr() < 1.0e-4) {
            return true;
        }
        return lookOf(Subject).dot(Toward.normalize()) >= LookCone;
    }

    public static void tick(LivingEntity Subject) {
        State Current = stateOf(Subject);
        if (!Current.active() || !canCarry(Subject)) {
            return;
        }
        if (Subject.tickCount % CheckInterval == 0 && !hiveStands(Subject, Current)) {
            fade(Subject, Current);
            return;
        }
        ServerLevel World = hiveLevel(Subject, Current);
        if (World == null) {
            return;
        }
        if (!inRange(Subject, Current)) {
            return;
        }
        int Ticks = Current.Ticks() + 1;
        int Nudge = Current.Nudge() - 1;
        if (Current.Stage() == 1) {
            if (Ticks >= StageOneTicks) {
                store(Subject, Current.withStage(2, nudgeDelay(Subject.getRandom(), 2)));
                return;
            }
            store(Subject, Current.withTicks(Ticks, 0));
            return;
        }
        if (Current.Stage() == 2) {
            if (Ticks >= StageTwoTicks) {
                store(Subject, Current.withStage(3, nudgeDelay(Subject.getRandom(), 3)));
                return;
            }
            if (Nudge <= 0 && walked(Subject)) {
                whisper(Subject, "nudge.", NudgeLines);
                Nudge = nudgeDelay(Subject.getRandom(), 2);
            }
            store(Subject, Current.withTicks(Ticks, Math.max(0, Nudge)));
            return;
        }
        boolean Facing = facesHive(Subject, Current) || Scp148Shield.blocks(Subject);
        if (Subject.tickCount % LookInterval == 0) {
            if (Facing) {
                relieve(Subject, MobEffects.BLINDNESS);
                relieve(Subject, MobEffects.MOVEMENT_SLOWDOWN);
            } else {
                Subject.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BlindTicks, 0, false, false, false));
                Subject.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BlindTicks, SlowLevel, false, false, false));
            }
        }
        if (Nudge <= 0 && !Facing) {
            whisper(Subject, "urge.", UrgeLines);
            Nudge = nudgeDelay(Subject.getRandom(), 3);
        }
        store(Subject, Current.withTicks(Math.min(Ticks, 1 << 24), Math.max(0, Nudge)));
        if (Subject.tickCount % BurstInterval == 0 && canBurst(World, Subject, Current)) {
            burst(World, Subject, Current);
        }
    }

    private static void relieve(LivingEntity Subject, Holder<MobEffect> Effect) {
        MobEffectInstance Present = Subject.getEffect(Effect);
        if (Present != null && Present.getDuration() <= BlindTicks && !Present.isVisible()) {
            Subject.removeEffect(Effect);
        }
    }

    private static boolean walked(LivingEntity Subject) {
        if (!(Subject instanceof ServerPlayer)) {
            return false;
        }
        Float Last = Walked.get(Subject);
        float Now = Subject.walkDist;
        if (Last == null || Now < Last) {
            Walked.put(Subject, Now);
            return false;
        }
        if (Now - Last >= NudgeWalk) {
            Walked.put(Subject, Now);
            return true;
        }
        return false;
    }

    private static void whisper(LivingEntity Subject, String Kind, int Count) {
        if (Subject instanceof ServerPlayer User && !Scp148Shield.blocks(User)) {
            int Line = User.getRandom().nextInt(Count);
            User.displayClientMessage(Component.translatable(Prefix + Kind + Line).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC), true);
        }
    }

    private static void fade(LivingEntity Subject, State Current) {
        cure(Subject);
        relieve(Subject, MobEffects.BLINDNESS);
        relieve(Subject, MobEffects.MOVEMENT_SLOWDOWN);
        if (Current.Stage() >= 2 && Subject instanceof ServerPlayer User) {
            User.displayClientMessage(Component.translatable(Prefix + "fade").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    public static boolean canBurst(ServerLevel World, LivingEntity Subject, State Current) {
        Vec3 Eyes = Subject.getEyePosition();
        Vec3 Centre = hiveCentre(Current);
        if (Eyes.distanceToSqr(Centre) > BurstReach * BurstReach || !World.isLoaded(Current.Hive())) {
            return false;
        }
        BlockPos Hive = Current.Hive();
        return LabExposure.clearPath(World, Eyes, Centre, Cell -> Cell.equals(Hive));
    }

    public static void burst(ServerLevel World, LivingEntity Subject, State Current) {
        Vec3 Head = Subject.getEyePosition().add(0.0, 0.25, 0.0);
        cure(Subject);
        relieve(Subject, MobEffects.BLINDNESS);
        relieve(Subject, MobEffects.MOVEMENT_SLOWDOWN);
        float Pitch = 0.95f + World.getRandom().nextFloat() * 0.1f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp098Registry.BurstLayers) {
            World.playSound(null, Head.x, Head.y, Head.z, Layer.get(), SoundSource.HOSTILE, 1.3f, Pitch);
        }
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), Head.x, Head.y, Head.z, 46, 0.25, 0.2, 0.25, 0.12);
        World.sendParticles(Grit, Head.x, Head.y, Head.z, 24, 0.35, 0.3, 0.35, 0.0);
        World.sendParticles(Scp098Registry.Spore.get(), Head.x, Head.y, Head.z, 10, 0.3, 0.25, 0.3, 0.02);
        World.sendParticles(ParticleTypes.POOF, Head.x, Head.y, Head.z, 6, 0.2, 0.1, 0.2, 0.02);
        Subject.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, StunTicks, 2));
        if (Subject instanceof Player) {
            Subject.addEffect(new MobEffectInstance(MobEffects.CONFUSION, StunTicks * 2, 0));
        }
        Scp098Crab.emergeFrom(World, Subject, Head, Current.Hive());
        Subject.hurt(World.damageSources().source(Scp098Registry.Emergence), BurstDamage);
    }
}
