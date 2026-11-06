package net.rai.scp_unleashed.scp001gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Scp001GateStrike {
    public static final double ClusterRadius = 5.0;

    private Scp001GateStrike() {
    }

    public static DamageSource source(ServerLevel World, @Nullable Entity Guardian) {
        return new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp001GateRegistry.StrikeDamage), Guardian, Guardian);
    }

    public static boolean exempt(Entity Body) {
        return Body instanceof Player User && (User.isCreative() || User.isSpectator());
    }

    public static boolean immortal(Entity Body) {
        return Body.isInvulnerable() || Scp001GateAvoid.avoids(Body);
    }

    public static boolean spared(Entity Body) {
        if (Body == null || Body instanceof Scp001GateGuardian) {
            return true;
        }
        if (immortal(Body)) {
            return true;
        }
        for (Entity Rider : Body.getIndirectPassengers()) {
            if (immortal(Rider)) {
                return true;
            }
        }
        Entity Mount = Body.getVehicle();
        while (Mount != null) {
            if (immortal(Mount)) {
                return true;
            }
            Mount = Mount.getVehicle();
        }
        return false;
    }

    public static boolean eligible(Entity Body) {
        if (Body == null || Body.isRemoved() || Body instanceof Scp001GateGuardian || exempt(Body) || spared(Body)) {
            return false;
        }
        if (Body instanceof LivingEntity Living) {
            return Living.isAlive();
        }
        if (Body instanceof VehicleEntity) {
            for (Entity Rider : Body.getPassengers()) {
                if (eligible(Rider)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean burnable(Entity Body) {
        return Body instanceof Projectile && !Body.isRemoved() && !spared(Body);
    }

    public static boolean clearable(Entity Body) {
        return Body instanceof LivingEntity && !(Body instanceof Player) && Body.isAlive() && eligible(Body);
    }

    public static int clear(ServerLevel World, BlockPos Centre, AABB Area) {
        int Count = 0;
        for (Entity Body : World.getEntities((Entity) null, Area, Scp001GateStrike::clearable)) {
            if (Scp001GateZones.inKill(Centre, Body.position()) && quiet(Body)) {
                Count++;
            }
        }
        return Count;
    }

    public static boolean quiet(Entity Body) {
        if (!clearable(Body)) {
            return false;
        }
        Entity Mount = Body.getVehicle();
        Body.stopRiding();
        Body.ejectPassengers();
        Body.discard();
        if (Mount instanceof VehicleEntity && !Mount.isRemoved() && !spared(Mount) && Mount.getPassengers().isEmpty()) {
            Mount.discard();
        }
        return true;
    }

    public static int smite(ServerLevel World, Entity Target, DamageSource Source) {
        if (spared(Target)) {
            return 0;
        }
        int Count = 0;
        List<Entity> Riders = new ArrayList<>(Target.getPassengers());
        for (Entity Rider : Riders) {
            if (eligible(Rider)) {
                Count += smite(World, Rider, Source);
            }
        }
        if (Target instanceof VehicleEntity) {
            Target.ejectPassengers();
            Target.kill();
            return Count + 1;
        }
        if (!(Target instanceof LivingEntity Living) || !Living.isAlive()) {
            return Count;
        }
        Entity Mount = Living.getVehicle();
        Living.invulnerableTime = 0;
        Living.hurt(Source, Float.MAX_VALUE);
        if (Living.isAlive() && !Living.isRemoved()) {
            Living.invulnerableTime = 0;
            Living.kill();
        }
        if (Living.isAlive() && !Living.isRemoved() && !(Living instanceof Player)) {
            Living.discard();
        }
        if (Mount instanceof VehicleEntity && !Mount.isRemoved() && !spared(Mount)) {
            Mount.ejectPassengers();
            Mount.kill();
        }
        burst(World, Living.position(), 1.0);
        return Count + 1;
    }

    public static void burn(ServerLevel World, Entity Projectile0) {
        Vec3 At = Projectile0.position();
        Projectile0.discard();
        World.sendParticles(ParticleTypes.FLAME, At.x, At.y, At.z, 8, 0.15, 0.15, 0.15, 0.02);
        World.sendParticles(ParticleTypes.SMOKE, At.x, At.y, At.z, 4, 0.1, 0.1, 0.1, 0.01);
        World.playSound(null, At.x, At.y, At.z, Scp001GateRegistry.Burn.get(), SoundSource.HOSTILE, 0.8f, 0.9f + World.random.nextFloat() * 0.3f);
    }

    public static void burst(ServerLevel World, Vec3 At, double Size) {
        World.sendParticles(ParticleTypes.FLAME, At.x, At.y + 0.8, At.z, (int) (14 * Size), 0.4 * Size, 0.6 * Size, 0.4 * Size, 0.04);
        World.sendParticles(ParticleTypes.LAVA, At.x, At.y + 0.4, At.z, (int) (4 * Size), 0.3 * Size, 0.2, 0.3 * Size, 0.0);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, At.x, At.y + 1.0, At.z, (int) (6 * Size), 0.4 * Size, 0.6 * Size, 0.4 * Size, 0.02);
    }
}
