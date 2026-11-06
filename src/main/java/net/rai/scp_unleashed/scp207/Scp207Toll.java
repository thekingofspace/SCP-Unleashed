package net.rai.scp_unleashed.scp207;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class Scp207Toll {
    public static final int Beat = 10;
    public static final int MinuteTicks = 1200;
    public static final float LethalMinutesPerDose = 45.0f;
    public static final float TollPerDoseTick = 1.0f / (LethalMinutesPerDose * MinuteTicks);
    public static final int DoseGrace = 6 * MinuteTicks;
    public static final int DoseFade = 8 * MinuteTicks;
    public static final float RecoverMinutes = 30.0f;
    public static final float RecoverPerTick = 1.0f / (RecoverMinutes * MinuteTicks);
    public static final int CalmCap = 1000000;
    public static final int MaxSpeed = 5;
    public static final int SpeedTicks = 60;
    public static final int SpeedRefresh = 30;
    public static final int PrimeTicks = 1200;
    public static final int RecentTicks = PrimeTicks;
    public static final int FuseTicks = 60;
    public static final float[] Omens = {0.25f, 0.5f, 0.8f};
    public static final String MessagePrefix = "message.scp_unleashed.scp207.";

    private Scp207Toll() {
    }

    public record State(int Doses, float Toll, int Calm, int Fade, int Primed, int Fuse, int Recent) {
        public static final State Clear = new State(0, 0.0f, 0, 0, 0, 0, 0);
        public static final Codec<State> StateCodec = RecordCodecBuilder.create(Instance -> Instance.group(
                Codec.INT.optionalFieldOf("doses", 0).forGetter(State::Doses),
                Codec.FLOAT.optionalFieldOf("toll", 0.0f).forGetter(State::Toll),
                Codec.INT.optionalFieldOf("calm", 0).forGetter(State::Calm),
                Codec.INT.optionalFieldOf("fade", 0).forGetter(State::Fade),
                Codec.INT.optionalFieldOf("primed", 0).forGetter(State::Primed),
                Codec.INT.optionalFieldOf("fuse", 0).forGetter(State::Fuse),
                Codec.INT.optionalFieldOf("recent", 0).forGetter(State::Recent)
        ).apply(Instance, State::new));

        public State {
            Doses = Math.max(0, Doses);
            Toll = Mth.clamp(Toll, 0.0f, 1.0f);
            Calm = Mth.clamp(Calm, 0, CalmCap);
            Fade = Math.max(0, Fade);
            Primed = Math.max(0, Primed);
            Fuse = Math.max(0, Fuse);
            Recent = Math.max(0, Recent);
        }

        public State(int Doses, float Toll, int Calm, int Fade, int Primed, int Fuse) {
            this(Doses, Toll, Calm, Fade, Primed, Fuse, 0);
        }

        public boolean isEmpty() {
            return Doses == 0 && Toll <= 0.0f && Primed == 0 && Fuse == 0 && Recent == 0;
        }

        public float rate(boolean Paused) {
            if (Paused) {
                return 0.0f;
            }
            if (Doses > 0) {
                return Doses * TollPerDoseTick;
            }
            return Toll > 0.0f ? -RecoverPerTick : 0.0f;
        }

        public State withFuse(int Left) {
            return new State(Doses, Toll, Calm, Fade, Primed, Left, Recent);
        }
    }

    public static State stateOf(LivingEntity Subject) {
        State Found = Subject.getExistingDataOrNull(Scp207Registry.Toll.get());
        return Found == null ? State.Clear : Found;
    }

    public static void store(LivingEntity Subject, State Next) {
        if (Next.isEmpty()) {
            Subject.removeData(Scp207Registry.Toll.get());
        } else {
            Subject.setData(Scp207Registry.Toll.get(), Next);
        }
    }

    public static boolean isPaused(ServerPlayer User) {
        return User.isCreative() || User.isSpectator();
    }

    public static DamageSource source(LivingEntity Subject) {
        return Subject.damageSources().source(Scp207Registry.ExposureDamage);
    }

    public static void drink(LivingEntity Drinker, boolean One) {
        if (!(Drinker instanceof ServerPlayer User) || !User.isAlive()) {
            return;
        }
        State Current = stateOf(User);
        if (One) {
            boolean Mixed = Current.Recent() > 0 && Current.Fuse() == 0;
            State Next = new State(Current.Doses(), Current.Toll(), Current.Calm(), Current.Fade(), Mixed ? 0 : PrimeTicks, Mixed ? FuseTicks : Current.Fuse(),
                    Mixed ? 0 : Current.Recent());
            store(User, Next);
            User.addEffect(new MobEffectInstance(MobEffects.JUMP, PrimeTicks, 0, false, false, true));
            User.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, PrimeTicks, 0, false, false, true));
            Scp207Registry.playLayers(User.level(), User.getEyePosition(), Scp207Registry.FizzLayers, SoundSource.PLAYERS, 0.7f, 0.7f);
            if (Mixed) {
                Scp207Burst.light(User);
            }
            Scp207Sync.toll(User, Next, true);
            return;
        }
        boolean Combo = Current.Primed() > 0 && Current.Fuse() == 0;
        int Doses = Current.Doses() + 1;
        State Next = new State(Doses, Current.Toll(), 0, 0, Combo ? 0 : Current.Primed(), Combo ? FuseTicks : Current.Fuse(), Combo ? 0 : RecentTicks);
        store(User, Next);
        quicken(User, Doses);
        Scp207Registry.playLayers(User.level(), User.getEyePosition(), Scp207Registry.FizzLayers, SoundSource.PLAYERS, 0.6f, 1.05f);
        if (Combo) {
            Scp207Burst.light(User);
        }
        Scp207Sync.toll(User, Next, true);
    }

    public static void quicken(ServerPlayer User, int Doses) {
        if (Doses <= 0) {
            return;
        }
        int Amplifier = Math.min(Doses, MaxSpeed) - 1;
        MobEffectInstance Existing = User.getEffect(MobEffects.MOVEMENT_SPEED);
        if (Existing != null && Existing.getAmplifier() > Amplifier) {
            return;
        }
        if (Existing == null || Existing.getAmplifier() < Amplifier || Existing.getDuration() < SpeedRefresh) {
            User.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SpeedTicks, Amplifier, true, false, true));
        }
    }

    public static void tick(ServerPlayer User) {
        State Current = User.getExistingDataOrNull(Scp207Registry.Toll.get());
        if (Current == null) {
            return;
        }
        if (!User.isAlive()) {
            return;
        }
        ServerLevel World = User.serverLevel();
        if (Current.Fuse() > 0) {
            int Left = Current.Fuse() - 1;
            Current = Current.withFuse(Left);
            store(User, Current);
            if (Left <= 0) {
                Scp207Burst.detonate(User);
                return;
            }
            Scp207Burst.fizz(User, Left);
        }
        if (Math.floorMod(World.getGameTime() + User.getId(), Beat) == 0) {
            beat(User, Current);
        }
    }

    private static void beat(ServerPlayer User, State Current) {
        boolean Paused = isPaused(User);
        int Doses = Current.Doses();
        int Fade = Current.Fade();
        int Calm = Math.min(CalmCap, Current.Calm() + Beat);
        if (Doses > 0 && Calm >= DoseGrace) {
            Fade += Beat;
            if (Fade >= DoseFade) {
                Doses--;
                Fade = 0;
            }
        }
        State Shifted = new State(Doses, Current.Toll(), Calm, Fade, Math.max(0, Current.Primed() - Beat), Current.Fuse(), Math.max(0, Current.Recent() - Beat));
        float Rate = Shifted.rate(Paused);
        float Toll = Mth.clamp(Current.Toll() + Rate * Beat, 0.0f, 1.0f);
        State Next = new State(Doses, Toll, Calm, Fade, Shifted.Primed(), Current.Fuse(), Shifted.Recent());
        omen(User, Current.Toll(), Toll);
        if (Doses > 0) {
            quicken(User, Doses);
        }
        if (Toll >= 1.0f && !Paused) {
            claim(User);
            return;
        }
        store(User, Next);
        Scp207Sync.toll(User, Next, false);
    }

    private static void omen(ServerPlayer User, float Before, float After) {
        for (int Index = 0; Index < Omens.length; Index++) {
            if (Before < Omens[Index] && After >= Omens[Index]) {
                User.displayClientMessage(Component.translatable(MessagePrefix + "omen." + Index).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                User.level().playSound(null, User.getX(), User.getEyeY(), User.getZ(), Scp207Registry.TollSwell.get(), SoundSource.PLAYERS, 0.8f, 0.9f - Index * 0.1f);
            }
        }
    }

    public static void claim(ServerPlayer User) {
        User.removeData(Scp207Registry.Toll.get());
        Scp207Sync.toll(User, State.Clear, true);
        User.hurt(source(User), Float.MAX_VALUE);
    }

    public static void set(ServerPlayer User, State Next) {
        store(User, Next);
        if (Next.Doses() > 0) {
            quicken(User, Next.Doses());
        }
        Scp207Sync.toll(User, Next, true);
    }

    public static void clear(ServerPlayer User) {
        User.removeData(Scp207Registry.Toll.get());
        Scp207Sync.toll(User, State.Clear, true);
    }
}
