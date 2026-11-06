package net.rai.scp_unleashed.scp001;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.scp001.network.Scp001GlarePayload;
import net.rai.scp_unleashed.senses.Senses;

public final class Scp001Flash {
    public static final double Range = 16.0;
    public static final double FacingFull = 0.5;
    public static final double FacingPartial = -0.1;
    public static final float FullStrength = 1.0f;
    public static final float PartialStrength = 0.55f;
    public static final float GlareStrength = 0.2f;
    public static final int FullGlare = 100;
    public static final int PartialGlare = 60;
    public static final int WeakGlare = 30;
    public static final int FullNausea = 200;
    public static final int PartialNausea = 100;

    private Scp001Flash() {
    }

    public enum Exposure {
        None, Glare, Partial, Full
    }

    public static Exposure exposure(ServerLevel World, ServerPlayer Viewer, Vec3 Orb) {
        if (Viewer.isSpectator() || !Viewer.isAlive() || Viewer.level() != World || !Targeting.canWitness(Viewer)) {
            return Exposure.None;
        }
        Vec3 Eye = Viewer.getEyePosition();
        if (Eye.distanceTo(Orb) > Range || !Scp001Warp.sees(World, Eye, Orb)) {
            return Exposure.None;
        }
        if (Senses.isBlindfolded(Viewer)) {
            return Exposure.None;
        }
        double Facing = Viewer.getLookAngle().dot(Orb.subtract(Eye).normalize());
        if (Facing >= FacingFull) {
            return Exposure.Full;
        }
        return Facing >= FacingPartial ? Exposure.Partial : Exposure.Glare;
    }

    public static boolean anyWitness(ServerLevel World, Scp001Prototype Source) {
        Vec3 Orb = Source.orbPosition();
        for (ServerPlayer Viewer : World.players()) {
            if (Scp001Prey.prey(Source, Viewer) && exposure(World, Viewer, Orb).ordinal() >= Exposure.Partial.ordinal()) {
                return true;
            }
        }
        return false;
    }

    public static int burst(ServerLevel World, Scp001Prototype Source) {
        Vec3 Orb = Source.orbPosition();
        World.playSound(null, Orb.x, Orb.y, Orb.z, Scp001Registry.FlashBurst.get(), SoundSource.HOSTILE, 2.0f, 1.0f);
        World.playSound(null, Orb.x, Orb.y, Orb.z, Scp001Registry.FlashShatter.get(), SoundSource.HOSTILE, 1.4f, 1.0f);
        World.sendParticles(ParticleTypes.FLASH, Orb.x, Orb.y, Orb.z, 2, 0.0, 0.0, 0.0, 0.0);
        World.sendParticles(ParticleTypes.END_ROD, Orb.x, Orb.y, Orb.z, 30, 0.2, 0.2, 0.2, 0.25);
        World.sendParticles(new DustParticleOptions(Scp001Prototype.Blue, 1.6f), Orb.x, Orb.y, Orb.z, 20, 0.6, 0.6, 0.6, 0.0);
        int Affected = 0;
        for (ServerPlayer Viewer : World.players()) {
            if (apply(World, Viewer, Orb) != Exposure.None) {
                Affected++;
            }
        }
        return Affected;
    }

    private static void send(ServerPlayer Viewer, float Strength, int Ticks) {
        if (Viewer.connection != null) {
            PacketDistributor.sendToPlayer(Viewer, new Scp001GlarePayload(Strength, Ticks));
        }
    }

    public static Exposure apply(ServerLevel World, ServerPlayer Viewer, Vec3 Orb) {
        Exposure Seen = exposure(World, Viewer, Orb);
        switch (Seen) {
            case Full -> {
                send(Viewer, FullStrength, FullGlare);
                Viewer.addEffect(new MobEffectInstance(MobEffects.CONFUSION, FullNausea, 0, false, false, true));
            }
            case Partial -> {
                send(Viewer, PartialStrength, PartialGlare);
                Viewer.addEffect(new MobEffectInstance(MobEffects.CONFUSION, PartialNausea, 0, false, false, true));
            }
            case Glare -> send(Viewer, GlareStrength, WeakGlare);
            default -> {
            }
        }
        return Seen;
    }
}
