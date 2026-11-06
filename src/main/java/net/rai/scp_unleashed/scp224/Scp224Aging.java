package net.rai.scp_unleashed.scp224;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp224.network.Scp224AgePayload;

import java.util.List;

public final class Scp224Aging {
    public static final int Span = 300;
    public static final int RecoverRate = 4;
    public static final double MaxSlow = 0.62;
    public static final double SlowStep = 0.01;
    public static final int CheckEvery = 10;
    public static final int DeathRetry = 20;
    public static final int RecaughtBonus = 30;
    public static final int SoundSlowest = 50;
    public static final int SoundFastest = 18;
    public static final float SoundVolume = 0.55f;
    public static final int SyncSteps = 240;

    private Scp224Aging() {
    }

    public static Scp224Age age(LivingEntity Target) {
        return Target.hasData(Scp224Registry.Age) ? Target.getData(Scp224Registry.Age) : null;
    }

    public static boolean isAging(LivingEntity Target) {
        Scp224Age Current = age(Target);
        return Current != null && !Current.Released();
    }

    public static boolean isForced(LivingEntity Target) {
        return isAging(Target) && !Scp224Sight.isExempt(Target);
    }

    public static float progress(LivingEntity Target) {
        Scp224Age Current = age(Target);
        return Current == null ? 0.0f : Current.progress();
    }

    public static void seize(LivingEntity Target, ServerLevel World, BlockPos Lower, long Serial) {
        Scp224Age Current = age(Target);
        int Elapsed = 0;
        if (Current != null) {
            Elapsed = Current.Released() ? Current.Elapsed() : Math.min(Span - 1, Current.Elapsed() + RecaughtBonus);
        }
        Target.setData(Scp224Registry.Age, new Scp224Age(Math.max(1, Elapsed), GlobalPos.of(World.dimension(), Lower.immutable()), Serial, false));
    }

    public static void release(LivingEntity Target) {
        Scp224Age Current = age(Target);
        if (Current != null && !Current.Released()) {
            Target.setData(Scp224Registry.Age, Current.released());
        }
    }

    public static int releaseAll(MinecraftServer Server, long Serial) {
        int Count = 0;
        for (ServerLevel World : Server.getAllLevels()) {
            for (Entity Each : World.getAllEntities()) {
                if (Each instanceof LivingEntity Living) {
                    Scp224Age Current = age(Living);
                    if (Current != null && !Current.Released() && Current.Serial() == Serial) {
                        Living.setData(Scp224Registry.Age, Current.released());
                        stand(Living);
                        Count++;
                    }
                }
            }
        }
        return Count;
    }

    public static void clear(LivingEntity Target) {
        if (Target.hasData(Scp224Registry.Age)) {
            Target.removeData(Scp224Registry.Age);
        }
        unslow(Target);
        stand(Target);
    }

    public static boolean clockStopped(LivingEntity Target, Scp224Age Current) {
        MinecraftServer Server = Target.getServer();
        if (Server == null) {
            return false;
        }
        ServerLevel Home = Server.getLevel(Current.Clock().dimension());
        if (Home == null) {
            return true;
        }
        BlockPos Pos = Current.Clock().pos();
        if (!Home.isLoaded(Pos)) {
            return false;
        }
        BlockEntity Found = Home.getBlockEntity(Pos);
        return !(Found instanceof Scp224ClockBlockEntity Clock) || Clock.getSerial() != Current.Serial() || Clock.isStilled();
    }

    public static void tick(LivingEntity Target) {
        if (Target.level().isClientSide) {
            return;
        }
        Scp224Age Current = age(Target);
        if (Current == null) {
            if (Target.tickCount % CheckEvery == 0) {
                unslow(Target);
            }
            return;
        }
        if (Current.Released()) {
            int Left = Current.Elapsed() - RecoverRate;
            if (Left <= 0) {
                clear(Target);
                return;
            }
            Target.setData(Scp224Registry.Age, Current.withElapsed(Left));
            slow(Target, Left / (float) Span);
            stand(Target);
            return;
        }
        if (Scp224Sight.isExempt(Target)) {
            unslow(Target);
            return;
        }
        if (Target.tickCount % CheckEvery == 0 && clockStopped(Target, Current)) {
            Target.setData(Scp224Registry.Age, Current.released());
            stand(Target);
            return;
        }
        int Elapsed = Math.min(Span * 4, Current.Elapsed() + 1);
        Target.setData(Scp224Registry.Age, Current.withElapsed(Elapsed));
        float Progress = Math.min(1.0f, Elapsed / (float) Span);
        slow(Target, Progress);
        stoop(Target);
        murmur(Target, Progress);
        if (Elapsed >= Span && (Elapsed - Span) % DeathRetry == 0) {
            expire(Target);
        }
    }

    public static double slowness(float Progress) {
        float Clamped = Mth.clamp(Progress, 0.0f, 1.0f);
        return MaxSlow * Clamped * (0.6 + 0.4 * Clamped);
    }

    public static void slow(LivingEntity Target, float Progress) {
        AttributeInstance Speed = Target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (Speed == null) {
            return;
        }
        double Wanted = -Math.round(slowness(Progress) / SlowStep) * SlowStep;
        AttributeModifier Existing = Speed.getModifier(Scp224Registry.SlowId);
        if (Wanted > -SlowStep * 0.5) {
            if (Existing != null) {
                Speed.removeModifier(Scp224Registry.SlowId);
            }
            return;
        }
        if (Existing != null && Math.abs(Existing.amount() - Wanted) < SlowStep * 0.5) {
            return;
        }
        Speed.addOrUpdateTransientModifier(new AttributeModifier(Scp224Registry.SlowId, Wanted, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static void unslow(LivingEntity Target) {
        AttributeInstance Speed = Target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (Speed != null && Speed.hasModifier(Scp224Registry.SlowId)) {
            Speed.removeModifier(Scp224Registry.SlowId);
        }
    }

    public static boolean canStoop(LivingEntity Target) {
        if (Target.isInWater() || Target.isSwimming() || Target.onClimbable() || Target.isFallFlying() || Target.isSleeping()) {
            return false;
        }
        return !(Target instanceof Player User) || !User.getAbilities().flying;
    }

    public static void stoop(LivingEntity Target) {
        boolean Able = canStoop(Target);
        if (Target instanceof Player User) {
            if (Able) {
                User.setShiftKeyDown(true);
                User.setSprinting(false);
            }
            return;
        }
        if (Able) {
            Target.setShiftKeyDown(true);
            if (Target.getPose() == Pose.STANDING) {
                Target.setPose(Pose.CROUCHING);
            }
            if (Target instanceof Mob Body) {
                Body.setSprinting(false);
            }
        } else {
            stand(Target);
        }
    }

    public static void stand(LivingEntity Target) {
        if (Target instanceof Player) {
            return;
        }
        if (Target.getPose() == Pose.CROUCHING) {
            Target.setPose(Pose.STANDING);
        }
        if (Target.isShiftKeyDown()) {
            Target.setShiftKeyDown(false);
        }
    }

    private static void murmur(LivingEntity Target, float Progress) {
        RandomSource Random = Target.getRandom();
        int Every = Math.round(Mth.lerp(Progress, SoundSlowest, SoundFastest));
        if (Progress < 0.08f || Random.nextInt(Math.max(1, Every)) != 0) {
            return;
        }
        List<DeferredHolder<SoundEvent, SoundEvent>> Layers = Random.nextInt(3) == 0 ? Scp224Registry.CoughLayers : Scp224Registry.WheezeLayers;
        play(Target, Layers, SoundVolume * (0.7f + 0.3f * Progress), 0.92f + Random.nextFloat() * 0.1f);
    }

    public static void play(LivingEntity Target, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        SoundSource Source = Target instanceof Player ? SoundSource.PLAYERS : SoundSource.NEUTRAL;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            Target.level().playSound(null, Target.getX(), Target.getEyeY(), Target.getZ(), Layer.get(), Source, Volume, Pitch);
        }
    }

    public static void expire(LivingEntity Target) {
        if (Target.level() instanceof ServerLevel World) {
            World.sendParticles(ParticleTypes.WHITE_ASH, Target.getX(), Target.getY() + Target.getBbHeight() * 0.5, Target.getZ(), 24,
                    Target.getBbWidth() * 0.4, Target.getBbHeight() * 0.3, Target.getBbWidth() * 0.4, 0.01);
        }
        Target.hurt(Target.damageSources().source(Scp224Registry.OldAge), Float.MAX_VALUE);
    }

    public static int state(ServerPlayer User) {
        Scp224Age Current = age(User);
        if (Current == null) {
            return 0;
        }
        int Step = Math.round(Current.progress() * SyncSteps);
        int Forced = !Current.Released() && !Scp224Sight.isExempt(User) ? 1 : 0;
        return Step << 1 | Forced;
    }

    public static void sync(ServerPlayer User) {
        int State = state(User);
        int Sent = User.getData(Scp224Registry.SentAge);
        if (State == Sent) {
            return;
        }
        User.setData(Scp224Registry.SentAge, State);
        PacketDistributor.sendToPlayer(User, new Scp224AgePayload((State >> 1) / (float) SyncSteps, (State & 1) != 0));
    }

    public static void resend(ServerPlayer User) {
        User.setData(Scp224Registry.SentAge, -1);
        sync(User);
    }
}
