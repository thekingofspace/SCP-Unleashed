package net.rai.scp_unleashed.grenade;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.anomaly.FlashEvent;
import net.rai.scp_unleashed.grenade.network.GrenadeFlashPayload;
import net.rai.scp_unleashed.senses.Senses;

public final class GrenadeFlash {
    public static final double FlashLift = 0.25;

    private GrenadeFlash() {
    }

    public static void detonate(ServerLevel World, GrenadeEntity Grenade, boolean Airless) {
        Vec3 Centre = Grenade.position().add(0.0, FlashLift, 0.0);
        double Radius = GrenadeTuning.FlashRadius;
        Entity Thrower = Grenade.getOwner();
        GrenadeSounds.play(World, Centre, GrenadeRegistry.FlashBang, SoundSource.BLOCKS, 1.0F, 1.0F);
        GrenadeSounds.play(World, Centre, GrenadeRegistry.FlashCrack, SoundSource.BLOCKS, 1.0F, 1.0F);
        World.sendParticles(ParticleTypes.FLASH, Centre.x, Centre.y, Centre.z, 1, 0.0, 0.0, 0.0, 0.0);
        World.sendParticles(ParticleTypes.END_ROD, Centre.x, Centre.y, Centre.z, 30, 0.15, 0.15, 0.15, 0.25);
        World.sendParticles(ParticleTypes.WHITE_SMOKE, Centre.x, Centre.y + 0.2, Centre.z, 18, 0.35, 0.25, 0.35, 0.03);
        FlashEvent.fire(World, Centre, Radius, Thrower);
        for (ServerPlayer User : World.players()) {
            if (User.isSpectator() || User.distanceToSqr(Centre) > Radius * Radius) {
                continue;
            }
            Exposure Hit = exposure(World, Grenade, User, Centre, Airless);
            if (Hit.White() > GrenadeTuning.FlashFloor || Hit.Ring() > GrenadeTuning.FlashFloor) {
                PacketDistributor.sendToPlayer(User, new GrenadeFlashPayload(Centre.x, Centre.y, Centre.z, Hit.White(), Hit.Ring()));
            }
        }
        for (Mob Target : World.getEntitiesOfClass(Mob.class, new AABB(Centre, Centre).inflate(Radius), Mob::isAlive)) {
            Vec3 Eye = Target.getEyePosition();
            double Distance = Eye.distanceTo(Centre);
            if (Distance > Radius || !GrenadeSight.lightReaches(World, Grenade, Centre, Eye)) {
                continue;
            }
            int Ticks = Mth.floor(Mth.lerp(nearness(Distance), GrenadeTuning.StunMinTicks, GrenadeTuning.StunMaxTicks));
            GrenadeDaze.daze(Target, Ticks, Thrower);
        }
        Grenade.discard();
    }

    public static float nearness(double Distance) {
        double Span = GrenadeTuning.FlashRadius - GrenadeTuning.FlashFullDistance;
        float Linear = (float) Mth.clamp((GrenadeTuning.FlashRadius - Distance) / Span, 0.0, 1.0);
        return (float) Math.pow(Linear, GrenadeTuning.FlashNearCurve);
    }

    public static float gaze(double Facing) {
        if (Facing >= GrenadeTuning.FacingFull) {
            return 1.0F;
        }
        float Share = (float) Mth.clamp((Facing + 1.0) / (GrenadeTuning.FacingFull + 1.0), 0.0, 1.0);
        return GrenadeTuning.AwayShare + (1.0F - GrenadeTuning.AwayShare) * Share * Share;
    }

    public static Exposure exposure(ServerLevel World, GrenadeEntity Grenade, Player User, Vec3 Centre, boolean Airless) {
        Vec3 Eye = User.getEyePosition();
        Vec3 Toward = Centre.subtract(Eye);
        double Distance = Toward.length();
        float Near = nearness(Distance);
        boolean Seen = GrenadeSight.lightReaches(World, Grenade, Centre, Eye);
        double Facing = Distance < 1.0E-3 ? 1.0 : User.getViewVector(1.0F).dot(Toward.scale(1.0 / Distance));
        float White = Seen && !Senses.isBlindfolded(User) ? Near * gaze(Facing) : 0.0F;
        float Ring = Airless ? 0.0F : Near * (Seen ? 1.0F : GrenadeTuning.WallRingShare) * (Senses.isDeafened(User) ? GrenadeTuning.EarplugRingShare : 1.0F);
        return new Exposure(White, Ring);
    }

    public record Exposure(float White, float Ring) {
    }
}
