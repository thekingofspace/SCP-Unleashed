package net.rai.scp_unleashed.scp939;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.gear.GearTags;
import net.rai.scp_unleashed.scp939.network.Scp939BreathPayload;

import java.util.Optional;

public final class Scp939Breath {
    public static final float MaxExposure = 100.0f;
    public static final double BreathRange = 10.0;
    public static final int TickEvery = 10;
    public static final float GainPerSecond = 0.4f;
    public static final float HazeBoost = 2.0f;
    public static final int DecayTicks = 3000;
    public static final float DecayStep = 1.0f;
    public static final float StrainDecay = 1.0f / 1200.0f;
    public static final float SendExposure = 0.25f;
    public static final float SendBreath = 0.03f;
    public static final int SendQuiet = 100;

    private Scp939Breath() {
    }

    public static float exposure(LivingEntity Target) {
        return Target.hasData(Scp939Registry.Exposure) ? Target.getData(Scp939Registry.Exposure) : 0.0f;
    }

    public static float share(LivingEntity Target) {
        return exposure(Target) / MaxExposure;
    }

    public static void setExposure(LivingEntity Target, float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, MaxExposure);
        if (Clamped <= 0.0f) {
            Target.removeData(Scp939Registry.Exposure);
        } else {
            Target.setData(Scp939Registry.Exposure, Clamped);
        }
    }

    public static void expose(LivingEntity Target, float Amount) {
        setExposure(Target, exposure(Target) + Amount);
    }

    public static boolean masked(LivingEntity Target) {
        return GearTags.protectsFromToxicGas(Target);
    }

    public static boolean breathes(LivingEntity Target) {
        return Target.isAlive() && !InfectionImmunity.isImmune(Target);
    }

    public static float nearness(ServerLevel World, LivingEntity Target) {
        float Best = 0.0f;
        AABB Area = Target.getBoundingBox().inflate(BreathRange);
        for (Scp939 Hunter : World.getEntitiesOfClass(Scp939.class, Area, LivingEntity::isAlive)) {
            float Close = 1.0f - (float) (Hunter.distanceTo(Target) / BreathRange);
            Best = Math.max(Best, Mth.clamp(Close, 0.0f, 1.0f));
        }
        return Best;
    }

    public static float roomHaze(ServerLevel World, LivingEntity Target) {
        Optional<Scp939Rooms.Room> Found = Scp939Rooms.roomAt(World, Target.blockPosition());
        return Found.map(Room -> Scp939Rooms.haze(World, Room)).orElse(0.0f);
    }

    public static float hazeAround(ServerLevel World, LivingEntity Target, float Near) {
        if (Near <= 0.0f && World.getEntitiesOfClass(Scp939.class, Target.getBoundingBox().inflate(Scp939Rooms.SpanAcross), Scp939::isHazed).isEmpty()) {
            return 0.0f;
        }
        return roomHaze(World, Target);
    }

    public static float breath(ServerLevel World, LivingEntity Target) {
        if (!breathes(Target) || masked(Target)) {
            return 0.0f;
        }
        float Near = nearness(World, Target);
        return Mth.clamp(Math.max(Near, hazeAround(World, Target, Near)), 0.0f, 1.0f);
    }

    public static float gain(float Near, float Haze, int Ticks) {
        return GainPerSecond * (float) Math.pow(Math.max(0.0f, Near), 1.5) * (1.0f + HazeBoost * Haze) * Ticks / 20.0f;
    }

    public static void tick(ServerPlayer User) {
        float Strain = User.hasData(Scp939Registry.Strain) ? User.getData(Scp939Registry.Strain) : 0.0f;
        if (Strain > 0.0f) {
            float Left = Strain - StrainDecay;
            if (Left <= 0.0f) {
                User.removeData(Scp939Registry.Strain);
            } else {
                User.setData(Scp939Registry.Strain, Left);
            }
        }
        if ((User.tickCount + User.getId()) % TickEvery != 0) {
            return;
        }
        breathe(User, TickEvery);
    }

    public static float breathe(ServerPlayer User, int Ticks) {
        ServerLevel World = User.serverLevel();
        float Near = 0.0f;
        float Haze = 0.0f;
        boolean Masked = masked(User);
        if (breathes(User)) {
            Near = nearness(World, User);
            Haze = hazeAround(World, User, Near);
        }
        float Breath = Masked ? 0.0f : Math.max(Near, Haze);
        if (Breath > 0.0f) {
            expose(User, gain(Breath, Haze, Ticks));
            User.setData(Scp939Registry.Calm, 0);
        } else if (!User.isCreative() && !User.isSpectator()) {
            int Calm = User.getData(Scp939Registry.Calm) + Ticks;
            if (Calm >= DecayTicks) {
                Calm = 0;
                if (exposure(User) > 0.0f) {
                    expose(User, -DecayStep);
                }
            }
            User.setData(Scp939Registry.Calm, Calm);
        }
        sync(User, Breath, Haze, Masked);
        return Breath;
    }

    public static long pack(float Exposure, float Breath, float Haze, boolean Masked) {
        long E = Math.round(Mth.clamp(Exposure, 0.0f, MaxExposure) * 4.0f);
        long B = Math.round(Mth.clamp(Breath, 0.0f, 1.0f) * 100.0f);
        long H = Math.round(Mth.clamp(Haze, 0.0f, 1.0f) * 100.0f);
        return E | B << 16 | H << 32 | (Masked ? 1L : 0L) << 48;
    }

    public static void sync(ServerPlayer User, float Breath, float Haze, boolean Masked) {
        float Exposure = exposure(User);
        long Packed = pack(Exposure, Breath, Haze, Masked);
        long Sent = User.getData(Scp939Registry.Sent);
        if (Packed == Sent && (Breath <= 0.0f || User.tickCount % SendQuiet != 0)) {
            return;
        }
        if (Sent >= 0) {
            float OldExposure = (Sent & 0xFFFF) / 4.0f;
            float OldBreath = ((Sent >> 16) & 0xFFFF) / 100.0f;
            float OldHaze = ((Sent >> 32) & 0xFFFF) / 100.0f;
            boolean OldMasked = ((Sent >> 48) & 1L) != 0L;
            boolean Small = Math.abs(OldExposure - Exposure) < SendExposure && Math.abs(OldBreath - Breath) < SendBreath && Math.abs(OldHaze - Haze) < SendBreath
                    && OldMasked == Masked && (Breath > 0.0f) == (OldBreath > 0.0f);
            if (Small && User.tickCount % SendQuiet != 0) {
                return;
            }
        }
        User.setData(Scp939Registry.Sent, Packed);
        PacketDistributor.sendToPlayer(User, new Scp939BreathPayload(Exposure, Breath, Haze, Masked));
    }

    public static void resend(ServerPlayer User) {
        User.setData(Scp939Registry.Sent, -1L);
        PacketDistributor.sendToPlayer(User, new Scp939BreathPayload(exposure(User), 0.0f, 0.0f, masked(User)));
    }

    public static void reset(Player User) {
        User.removeData(Scp939Registry.Exposure);
        User.removeData(Scp939Registry.Strain);
        User.removeData(Scp939Registry.Calm);
    }
}
