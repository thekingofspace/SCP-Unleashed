package net.rai.scp_unleashed.scp420j;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp420j.network.Scp420jFloatPayload;
import net.rai.scp_unleashed.scp420j.network.Scp420jSongPayload;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Scp420jFloat {
    public static final float TrackSeconds = 18.33f;
    public static final int TrackTicks = Mth.ceil(TrackSeconds * 20.0f);
    public static final float SongRange = 24.0f;
    public static final int RiseTicks = 90;
    public static final double Height = 7.0;
    public static final double BobAmplitude = 0.45;
    public static final int BobPeriod = 72;
    public static final int BobSettle = 40;
    public static final double Gain = 0.18;
    public static final double MaxRise = 0.22;
    public static final double MaxSink = 0.16;
    public static final double DescendSpeed = 0.09;
    public static final double RebaseDistance = 12.0;
    public static final int MaxDescentTicks = 1200;
    public static final int SlowFallTicks = 40;
    public static final int SlowFallRefresh = 20;
    public static final int LandGrace = 3;
    public static final int CooldownTicks = 20;
    public static final ResourceLocation GravityId = Scp420jRegistry.location("scp420j_float");
    private static final Map<UUID, Integer> Synced = new HashMap<>();

    private Scp420jFloat() {
    }

    public record State(int Session, int Elapsed, double BaseY, boolean Descending, int Descent, List<Integer> Spoken) {
        public static final State Clear = new State(0, 0, 0.0, false, 0, List.of());
        public static final Codec<State> StateCodec = RecordCodecBuilder.create(Instance -> Instance.group(
                Codec.INT.optionalFieldOf("session", 0).forGetter(State::Session),
                Codec.INT.optionalFieldOf("elapsed", 0).forGetter(State::Elapsed),
                Codec.DOUBLE.optionalFieldOf("base_y", 0.0).forGetter(State::BaseY),
                Codec.BOOL.optionalFieldOf("descending", false).forGetter(State::Descending),
                Codec.INT.optionalFieldOf("descent", 0).forGetter(State::Descent),
                Codec.INT.listOf().optionalFieldOf("spoken", List.of()).forGetter(State::Spoken)
        ).apply(Instance, State::new));

        public State {
            Elapsed = Math.max(0, Elapsed);
            Descent = Math.max(0, Descent);
            Spoken = List.copyOf(Spoken);
        }

        public boolean isEmpty() {
            return Session == 0;
        }

        public State withElapsed(int Ticks) {
            return new State(Session, Ticks, BaseY, Descending, Descent, Spoken);
        }

        public State withDescent(int Ticks) {
            return new State(Session, Elapsed, BaseY, Descending, Ticks, Spoken);
        }

        public State withBase(double Base) {
            return new State(Session, Elapsed, Base, Descending, Descent, Spoken);
        }

        public State withSpoken(int Line) {
            List<Integer> Next = new ArrayList<>(Spoken);
            Next.add(Line);
            return new State(Session, Elapsed, BaseY, Descending, Descent, Next);
        }

        public State descending() {
            return new State(Session, Math.max(Elapsed, TrackTicks), BaseY, true, 0, Spoken);
        }
    }

    @Nullable
    public static State stateOf(LivingEntity Subject) {
        State Found = Subject.getExistingDataOrNull(Scp420jRegistry.Floating.get());
        return Found == null || Found.isEmpty() ? null : Found;
    }

    public static boolean isFloating(LivingEntity Subject) {
        State Found = stateOf(Subject);
        return Found != null && !Found.Descending();
    }

    public static double rise(int Elapsed) {
        double Progress = Mth.clamp(Elapsed / (double) RiseTicks, 0.0, 1.0);
        return Height * Progress * Progress * Progress * (Progress * (Progress * 6.0 - 15.0) + 10.0);
    }

    public static double bob(double Elapsed) {
        double Settle = Mth.clamp((Elapsed - RiseTicks * 0.6) / BobSettle, 0.0, 1.0);
        return BobAmplitude * Settle * Math.sin((Elapsed - RiseTicks * 0.6) * Math.PI * 2.0 / BobPeriod);
    }

    public static double targetY(double BaseY, double Elapsed) {
        return BaseY + rise((int) Math.floor(Elapsed)) + bob(Elapsed);
    }

    public static double steer(double CurrentY, double BaseY, double Elapsed) {
        double Wanted = targetY(BaseY, Elapsed + 1.0) - CurrentY;
        return Mth.clamp(Wanted * Gain + (targetY(BaseY, Elapsed + 1.0) - targetY(BaseY, Elapsed)), -MaxSink, MaxRise);
    }

    public static boolean serverSteered(ServerPlayer User) {
        return User instanceof FakePlayer || User.connection == null || !User.connection.getConnection().isConnected();
    }

    public static void start(ServerPlayer User) {
        State Current = stateOf(User);
        double Base = Current != null ? Current.BaseY() : User.getY();
        int Session = nextSession(User, Current);
        State Next = new State(Session, 0, Base, false, 0, List.of());
        if (User.isPassenger()) {
            User.stopRiding();
        }
        User.setData(Scp420jRegistry.Floating.get(), Next);
        gravity(User, true);
        User.resetFallDistance();
        Vec3 Motion = User.getDeltaMovement();
        User.setDeltaMovement(Motion.x, Math.max(0.0, Motion.y), Motion.z);
        User.addEffect(new MobEffectInstance(MobEffects.SATURATION, TrackTicks, 0, false, false, true));
        Synced.put(User.getUUID(), Session);
        send(User, Next);
        song(User, true);
    }

    private static int nextSession(ServerPlayer User, @Nullable State Current) {
        int Session = User.getRandom().nextInt();
        while (Session == 0 || (Current != null && Session == Current.Session())) {
            Session = User.getRandom().nextInt();
        }
        return Session;
    }

    public static void tick(ServerPlayer User) {
        State Current = stateOf(User);
        UUID Id = User.getUUID();
        if (Current == null) {
            if (Synced.remove(Id) != null) {
                send(User, null);
                song(User, false);
            }
            gravity(User, false);
            return;
        }
        if (!User.isAlive()) {
            return;
        }
        Integer Known = Synced.get(Id);
        if (Known == null || Known != Current.Session()) {
            Synced.put(Id, Current.Session());
            if (Current.Descending()) {
                Current = Current.descending();
                slowFall(User, true);
                send(User, Current);
            } else {
                Current = descend(User, Current);
            }
        }
        User.resetFallDistance();
        if (!Current.Descending()) {
            Current = floatTick(User, Current);
        } else {
            Current = descentTick(User, Current);
        }
        if (Current != null) {
            User.setData(Scp420jRegistry.Floating.get(), Current);
        }
    }

    private static State floatTick(ServerPlayer User, State Current) {
        gravity(User, true);
        if (Math.abs(User.getY() - targetY(Current.BaseY(), Current.Elapsed())) > RebaseDistance) {
            Current = Current.withBase(User.getY() - rise(Current.Elapsed()));
            send(User, Current);
        }
        if (serverSteered(User)) {
            User.move(MoverType.SELF, new Vec3(0.0, steer(User.getY(), Current.BaseY(), Current.Elapsed()), 0.0));
            User.resetFallDistance();
        }
        int Elapsed = Current.Elapsed() + 1;
        int Line = Scp420jChatter.due(Current.Session(), Current.Spoken().size(), Elapsed);
        if (Line >= 0) {
            Scp420jChatter.say(User, Line);
            Current = Current.withSpoken(Line);
        }
        if (Elapsed == RiseTicks) {
            User.level().playSound(null, User.getX(), User.getEyeY(), User.getZ(), Scp420jRegistry.Sigh.get(), SoundSource.PLAYERS, 0.55f, 1.0f + User.getRandom().nextFloat() * 0.1f);
        }
        if (Elapsed >= TrackTicks) {
            return descend(User, Current);
        }
        return Current.withElapsed(Elapsed);
    }

    private static State descend(ServerPlayer User, State Current) {
        State Next = Current.descending();
        gravity(User, false);
        slowFall(User, true);
        song(User, false);
        send(User, Next);
        return Next;
    }

    @Nullable
    private static State descentTick(ServerPlayer User, State Current) {
        gravity(User, false);
        slowFall(User, false);
        if (serverSteered(User)) {
            User.move(MoverType.SELF, new Vec3(0.0, -DescendSpeed, 0.0));
            User.resetFallDistance();
        }
        int Descent = Current.Descent() + 1;
        boolean Settled = User.onGround() || User.isInWater() || User.isInLava() || User.onClimbable() || User.isPassenger() || User.isFallFlying()
                || User.getAbilities().flying || User.isSpectator();
        if ((Descent > LandGrace && Settled) || Descent > MaxDescentTicks) {
            land(User);
            return null;
        }
        return Current.withDescent(Descent);
    }

    private static void slowFall(ServerPlayer User, boolean Force) {
        MobEffectInstance Existing = User.getEffect(MobEffects.SLOW_FALLING);
        if (Force || Existing == null || Existing.getDuration() < SlowFallRefresh) {
            if (Existing == null || Existing.getDuration() < SlowFallTicks) {
                User.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SlowFallTicks, 0, false, false, true));
            }
        }
    }

    public static void land(ServerPlayer User) {
        State Current = stateOf(User);
        User.removeData(Scp420jRegistry.Floating.get());
        gravity(User, false);
        User.resetFallDistance();
        if (Current != null && !Current.Descending()) {
            song(User, false);
        }
        if (Synced.remove(User.getUUID()) != null) {
            send(User, null);
        }
    }

    public static void stop(ServerPlayer User) {
        State Current = stateOf(User);
        if (Current != null && !Current.Descending()) {
            song(User, false);
        }
    }

    public static void gravity(LivingEntity Subject, boolean Floating) {
        AttributeInstance Gravity = Subject.getAttribute(Attributes.GRAVITY);
        if (Gravity == null) {
            return;
        }
        if (Floating) {
            if (!Gravity.hasModifier(GravityId)) {
                Gravity.addTransientModifier(new AttributeModifier(GravityId, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        } else if (Gravity.hasModifier(GravityId)) {
            Gravity.removeModifier(GravityId);
        }
    }

    public static boolean cushion(LivingEntity Subject) {
        if (Subject.getExistingDataOrNull(Scp420jRegistry.Floating.get()) == null) {
            return false;
        }
        if (!(Subject instanceof ServerPlayer)) {
            Subject.removeData(Scp420jRegistry.Floating.get());
            gravity(Subject, false);
        }
        return true;
    }

    public static void lightUp(ServerPlayer User) {
        ServerLevel World = User.serverLevel();
        Vec3 Look = User.getLookAngle();
        Vec3 Mouth = User.getEyePosition().add(Look.scale(0.45)).add(0.0, -0.12, 0.0);
        World.sendParticles(ParticleTypes.SMOKE, Mouth.x, Mouth.y, Mouth.z, 6, 0.06, 0.04, 0.06, 0.01);
        World.sendParticles(Scp420jRegistry.Ring.get(), Mouth.x, Mouth.y + 0.05, Mouth.z, 1, 0.0, 0.0, 0.0, 0.0);
        World.sendParticles(ParticleTypes.SMALL_FLAME, Mouth.x, Mouth.y, Mouth.z, 2, 0.03, 0.02, 0.03, 0.004);
        Scp420jRegistry.playLayers(World, Mouth, Scp420jRegistry.LightLayers, 0.6f, 1.0f);
        Scp420jRegistry.playLayers(World, Mouth, Scp420jRegistry.PuffLayers, 0.55f, 0.95f + User.getRandom().nextFloat() * 0.1f);
    }

    private static void send(ServerPlayer User, @Nullable State Current) {
        if (User.connection == null) {
            return;
        }
        PacketDistributor.sendToPlayer(User, Current == null ? Scp420jFloatPayload.Cleared
                : new Scp420jFloatPayload(Current.Session(), Current.Elapsed(), Current.BaseY(), Current.Descending()));
    }

    private static void song(ServerPlayer User, boolean Playing) {
        if (User.connection == null) {
            return;
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(User, new Scp420jSongPayload(User.getId(), Playing));
    }

    public static void resend(ServerPlayer User) {
        State Current = stateOf(User);
        if (Current != null && Synced.containsKey(User.getUUID())) {
            send(User, Current);
        }
    }

    public static void forget(ServerPlayer User) {
        Synced.remove(User.getUUID());
    }

    public static void forgetAll() {
        Synced.clear();
    }

    public static void elsewhere(ServerPlayer User) {
        State Current = stateOf(User);
        if (Current == null) {
            return;
        }
        if (!Current.Descending()) {
            State Next = descend(User, Current);
            User.setData(Scp420jRegistry.Floating.get(), Next);
        } else {
            send(User, Current);
        }
    }
}
