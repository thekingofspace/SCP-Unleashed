package net.rai.scp_unleashed.scp093;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp093Visions {
    public static final int StaggerEvery = 28;
    public static final double StaggerReach = 6.0;
    public static final double StaggerPush = 0.18;
    public static final double WanderSpeed = 1.05;
    public static final int ParticleEvery = 12;
    private static final Map<ResourceKey<Level>, Set<UUID>> Afflicted = new ConcurrentHashMap<>();

    private Scp093Visions() {
    }

    public static void afflict(ServerLevel World, LivingEntity Target) {
        if (Target instanceof Player) {
            return;
        }
        Afflicted.computeIfAbsent(World.dimension(), Key -> ConcurrentHashMap.newKeySet()).add(Target.getUUID());
    }

    public static void forget(LivingEntity Target) {
        Set<UUID> Found = Afflicted.get(Target.level().dimension());
        if (Found != null) {
            Found.remove(Target.getUUID());
        }
    }

    public static void clear() {
        Afflicted.clear();
    }

    public static boolean afflicted(LivingEntity Target) {
        Scp093Doses Current = Scp093Tears.stored(Target);
        return Current.claimed() || Current.seeing(Target.level().getGameTime());
    }

    public static void tick(ServerLevel World) {
        Set<UUID> Found = Afflicted.get(World.dimension());
        if (Found == null || Found.isEmpty()) {
            return;
        }
        for (UUID Id : new ArrayList<>(Found)) {
            Entity Target = World.getEntity(Id);
            if (!(Target instanceof LivingEntity Living) || !Living.isAlive() || Target.isRemoved()) {
                Found.remove(Id);
                continue;
            }
            boolean Claiming = Scp093Tears.tickClaim(Living);
            if (!Living.isAlive()) {
                Found.remove(Id);
                continue;
            }
            if (!Claiming && !afflicted(Living)) {
                Found.remove(Id);
                continue;
            }
            behave(World, Living, Claiming);
        }
    }

    private static void behave(ServerLevel World, LivingEntity Living, boolean Claiming) {
        RandomSource Random = Living.getRandom();
        if (Living.tickCount % ParticleEvery == 0) {
            World.sendParticles(new DustParticleOptions(Scp093Tears.AmberDust, 0.9f), Living.getX(), Living.getEyeY() + 0.1, Living.getZ(), 2, 0.25, 0.2, 0.25, 0.0);
        }
        if (!(Living instanceof PathfinderMob Walker)) {
            return;
        }
        if (Claiming) {
            Walker.getNavigation().stop();
            Walker.setDeltaMovement(Walker.getDeltaMovement().multiply(0.0, 1.0, 0.0));
            Walker.setXRot(Math.min(60.0f, Walker.getXRot() + 3.0f));
            return;
        }
        if ((Walker.tickCount + Walker.getId()) % StaggerEvery == 0) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Reach = 2.0 + Random.nextDouble() * StaggerReach;
            Vec3 Goal = Walker.position().add(Math.cos(Angle) * Reach, 0.0, Math.sin(Angle) * Reach);
            Walker.getNavigation().moveTo(Goal.x, Goal.y, Goal.z, WanderSpeed);
            if (Random.nextFloat() < 0.4f && Walker.onGround()) {
                Walker.setDeltaMovement(Walker.getDeltaMovement().add(Math.cos(Angle + 1.5) * StaggerPush, 0.0, Math.sin(Angle + 1.5) * StaggerPush));
                Walker.hurtMarked = true;
            }
        }
        if (Random.nextInt(9) == 0) {
            float Yaw = Walker.getYHeadRot() + (Random.nextFloat() - 0.5f) * 140.0f;
            Vec3 Look = Walker.getEyePosition().add(Vec3.directionFromRotation(-10.0f + Random.nextFloat() * 50.0f, Yaw).scale(4.0));
            Walker.getLookControl().setLookAt(Look.x, Look.y, Look.z, 60.0f, 60.0f);
        }
    }

    public static void tickAll(MinecraftServer Server) {
        for (ServerLevel World : Server.getAllLevels()) {
            tick(World);
        }
    }
}
