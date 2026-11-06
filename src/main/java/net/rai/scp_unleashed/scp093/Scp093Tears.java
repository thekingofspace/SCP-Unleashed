package net.rai.scp_unleashed.scp093;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.scp093.network.Scp093VisionPayload;
import org.joml.Vector3f;

import java.util.List;

public final class Scp093Tears {
    public static final float Lethal = 5.0f;
    public static final int DecayTicks = 12000;
    public static final float DecayPerTick = 1.0f / DecayTicks;
    public static final int VisionBase = 240;
    public static final int VisionPerDose = 160;
    public static final int ClaimTicks = 70;
    public static final int NauseaBase = 100;
    public static final int NauseaPerDose = 50;
    public static final int SlowFrom = 2;
    public static final int HeavySlowFrom = 4;
    public static final int SlowTicks = 200;
    public static final float SurvivorAmount = Lethal - 0.5f;
    public static final Vector3f AmberDust = new Vector3f(0.95f, 0.62f, 0.2f);

    private Scp093Tears() {
    }

    public static Scp093Doses stored(LivingEntity Target) {
        Scp093Doses Found = Target.getExistingDataOrNull(Scp093Registry.Doses);
        return Found == null ? Scp093Doses.None : Found;
    }

    public static Scp093Doses read(LivingEntity Target) {
        long Now = Target.level().getGameTime();
        return stored(Target).decayed(Now, DecayPerTick);
    }

    public static void write(LivingEntity Target, Scp093Doses Value) {
        if (Value.Amount() <= 0.0f && Value.ClaimAt() == 0L && Value.VisionUntil() <= Target.level().getGameTime()) {
            if (Target.hasData(Scp093Registry.Doses)) {
                Target.removeData(Scp093Registry.Doses);
            }
        } else {
            Target.setData(Scp093Registry.Doses, Value);
        }
        sync(Target, Value);
    }

    public static void sync(LivingEntity Target, Scp093Doses Value) {
        if (Target instanceof ServerPlayer User) {
            long Now = User.level().getGameTime();
            int Vision = (int) Mth.clamp(Value.VisionUntil() - Now, 0L, Integer.MAX_VALUE);
            int Claim = Value.ClaimAt() > 0L ? (int) Mth.clamp(Value.ClaimAt() - Now, 0L, ClaimTicks) : -1;
            PacketDistributor.sendToPlayer(User, new Scp093VisionPayload(Value.Amount(), Vision, Claim));
        }
    }

    public static boolean exempt(LivingEntity Target) {
        return Target instanceof Player User && (User.isCreative() || User.isSpectator());
    }

    public static void drink(LivingEntity Drinker) {
        if (!(Drinker.level() instanceof ServerLevel World) || !Targeting.isTargetable(Drinker)) {
            return;
        }
        long Now = World.getGameTime();
        Scp093Doses Current = read(Drinker);
        if (Current.claimed()) {
            return;
        }
        float Amount = Current.Amount() + 1.0f;
        int Doses = Mth.ceil(Amount);
        long Vision = Math.max(Current.VisionUntil(), Now + VisionBase + (long) VisionPerDose * Doses);
        long Claim = Amount >= Lethal && !exempt(Drinker) ? Now + ClaimTicks : 0L;
        if (Amount >= Lethal && exempt(Drinker)) {
            Amount = SurvivorAmount;
        }
        write(Drinker, new Scp093Doses(Amount, Now, Vision, Claim));
        Drinker.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NauseaBase + NauseaPerDose * Doses, 0, false, false, true));
        if (Doses >= SlowFrom) {
            Drinker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SlowTicks, Doses >= HeavySlowFrom ? 1 : 0, false, false, true));
        }
        if (Claim > 0L) {
            Drinker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ClaimTicks + 20, 4, false, false, true));
            Drinker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ClaimTicks + 20, 0, false, false, false));
            play(World, Drinker, Scp093Registry.ClaimLayers, 1.0f, 0.8f);
        }
        World.sendParticles(new DustParticleOptions(AmberDust, 1.2f), Drinker.getX(), Drinker.getEyeY(), Drinker.getZ(), 12, 0.3, 0.25, 0.3, 0.0);
        Scp093Visions.afflict(World, Drinker);
    }

    public static void setDoses(LivingEntity Target, float Amount) {
        long Now = Target.level().getGameTime();
        float Clamped = Mth.clamp(Amount, 0.0f, Lethal - 0.01f);
        long Vision = Clamped > 0.0f ? Now + VisionBase + (long) VisionPerDose * Mth.ceil(Clamped) : 0L;
        write(Target, new Scp093Doses(Clamped, Now, Vision, 0L));
        if (Target.level() instanceof ServerLevel World && Clamped > 0.0f) {
            Scp093Visions.afflict(World, Target);
        }
    }

    public static boolean tickClaim(LivingEntity Target) {
        Scp093Doses Current = stored(Target);
        if (!Current.claimed() || !(Target.level() instanceof ServerLevel World)) {
            return false;
        }
        long Now = World.getGameTime();
        if (Now < Current.ClaimAt()) {
            return true;
        }
        if (exempt(Target)) {
            write(Target, new Scp093Doses(SurvivorAmount, Now, Current.VisionUntil(), 0L));
            return false;
        }
        Target.setData(Scp093Registry.Doses, new Scp093Doses(Current.Amount(), Now, Current.VisionUntil(), 0L));
        DamageSource Source = World.damageSources().source(Scp093Registry.TearsDamage);
        Target.invulnerableTime = 0;
        Target.hurt(Source, Float.MAX_VALUE);
        return false;
    }

    public static void play(ServerLevel World, LivingEntity Near, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Near.getX(), Near.getY(), Near.getZ(), Layer.get(), SoundSource.PLAYERS, Volume, Pitch);
        }
    }
}
