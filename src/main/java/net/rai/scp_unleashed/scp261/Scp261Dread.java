package net.rai.scp_unleashed.scp261;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Comparator;
import java.util.List;

public final class Scp261Dread {
    public static final int Interval = 10;
    public static final double Radius = 10.0;
    public static final double TargetReach = 16.0;
    public static final int Cap = 12;
    public static final int[] FleeReaches = {14, 10, 6, 4};
    public static final int FleeHeight = 6;
    public static final double FleeSpeed = 1.3;

    private Scp261Dread() {
    }

    public static boolean frightens(LivingEntity Target) {
        if (!(Target instanceof PathfinderMob) || !(Target instanceof Enemy) || !Target.isAlive()) {
            return false;
        }
        if (Target.getType().is(Tags.EntityTypes.BOSSES) || Target.getType().is(Scp261Registry.Fearless)) {
            return false;
        }
        return !Scp_unleashed.ModId.equals(BuiltInRegistries.ENTITY_TYPE.getKey(Target.getType()).getNamespace());
    }

    public static boolean dreads(LivingEntity Watcher, LivingEntity Target) {
        return Target instanceof Player User && User.hasEffect(Scp261Registry.Dreadful) && frightens(Watcher)
                && Watcher.distanceToSqr(User) <= TargetReach * TargetReach;
    }

    public static void pulse(LivingEntity Target, int Amplifier) {
        if (!(Target.level() instanceof ServerLevel World) || !(Target instanceof Player User) || User.isSpectator()) {
            return;
        }
        List<PathfinderMob> Near = World.getEntitiesOfClass(PathfinderMob.class, User.getBoundingBox().inflate(Radius), Scp261Dread::frightens);
        Near.sort(Comparator.comparingDouble(Other -> Other.distanceToSqr(User)));
        for (int Index = 0; Index < Near.size() && Index < Cap; Index++) {
            scare(World, Near.get(Index), User);
        }
    }

    public static void scare(ServerLevel World, PathfinderMob Scared, Player From) {
        if (Scared.getTarget() == From) {
            Scared.setTarget(null);
        }
        for (int Reach : FleeReaches) {
            Vec3 Away = DefaultRandomPos.getPosAway(Scared, Reach, FleeHeight, From.position());
            if (Away != null && Scared.getNavigation().moveTo(Away.x, Away.y, Away.z, FleeSpeed)) {
                break;
            }
        }
        if (World.getRandom().nextInt(4) == 0) {
            World.sendParticles(ParticleTypes.SPLASH, Scared.getX(), Scared.getEyeY() + 0.3, Scared.getZ(), 3, 0.2, 0.1, 0.2, 0.05);
        }
    }
}
