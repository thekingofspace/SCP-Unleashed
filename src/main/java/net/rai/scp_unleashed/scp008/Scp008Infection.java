package net.rai.scp_unleashed.scp008;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.gear.GearTags;
import net.rai.scp_unleashed.labware.LabExposure;

import java.util.function.Predicate;

public final class Scp008Infection {
    private Scp008Infection() {
    }

    public static boolean isInfected(LivingEntity Subject) {
        return Boolean.TRUE.equals(Subject.getExistingDataOrNull(Scp008Registry.Infection.get()));
    }

    public static boolean infect(LivingEntity Subject) {
        if (InfectionImmunity.isImmune(Subject)) {
            InfectionImmunity.absorb(Subject, Scp008Registry.DocumentId);
            return false;
        }
        if (!canCatch(Subject) || isInfected(Subject)) {
            return false;
        }
        Subject.setData(Scp008Registry.Infection.get(), Boolean.TRUE);
        return true;
    }

    public static boolean cure(LivingEntity Subject) {
        if (!isInfected(Subject)) {
            return false;
        }
        Subject.removeData(Scp008Registry.Infection.get());
        return true;
    }

    public static boolean canCatch(LivingEntity Subject) {
        if (Subject instanceof Scp008Zombie || Subject instanceof ArmorStand || !Subject.isAlive() || InfectionImmunity.isImmune(Subject)) {
            return false;
        }
        if (Subject instanceof Player Target) {
            return !Target.isCreative() && !Target.isSpectator();
        }
        return Subject.canBeSeenAsEnemy();
    }

    public static boolean isProtected(LivingEntity Subject) {
        return GearTags.wearsFullHazmatSuit(Subject);
    }

    public static void exposeAround(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Radius) {
        for (ServerPlayer Exposed : LabExposure.playersInSight(World, Source, Ignored, Radius, Target -> canCatch(Target) && !isInfected(Target) && !isProtected(Target))) {
            infect(Exposed);
        }
        for (LivingEntity Exposed : ControlBodies.inSight(World, Source, Ignored, Radius, Target -> canCatch(Target) && !isInfected(Target) && !isProtected(Target))) {
            infect(Exposed);
        }
        AABB Reach = AABB.ofSize(Source, Radius * 2.0, Radius * 2.0, Radius * 2.0);
        for (AbstractVillager Exposed : World.getEntitiesOfClass(AbstractVillager.class, Reach,
                Target -> Target.distanceToSqr(Source) <= Radius * Radius && canCatch(Target) && !isInfected(Target) && LabExposure.sees(World, Source, Target, Ignored))) {
            infect(Exposed);
        }
    }
}
