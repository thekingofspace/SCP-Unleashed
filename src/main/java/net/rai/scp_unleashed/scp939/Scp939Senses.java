package net.rai.scp_unleashed.scp939;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730SpecialRooms;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp939Senses {
    public static final double SmellRange = 8.0;
    public static final double HearWalk = 18.0;
    public static final double HearRun = 28.0;
    public static final double HearMob = 16.0;
    public static final double ContainedReach = 5.0;
    public static final double ChaseRange = 32.0;
    public static final double MovingSpeed = 0.003;
    public static final int BrightLight = 12;

    private Scp939Senses() {
    }

    public static boolean isPerson(@Nullable Entity Target) {
        return Target instanceof Player || Target instanceof AbstractVillager || Target != null && ControlBodies.isPersonLike(Target);
    }

    public static boolean isGame(@Nullable Entity Target) {
        return Target instanceof Animal && !(Target instanceof TamableAnimal Pet && Pet.isTame());
    }

    public static boolean isPrey(Scp939 Hunter, @Nullable LivingEntity Target) {
        if (Target == null || Target == Hunter || !Target.isAlive() || Target.isRemoved() || Target instanceof Scp939 || !Targeting.isTargetable(Target)) {
            return false;
        }
        if (Scp939Brood.isBrood(Target)) {
            return false;
        }
        if (Target instanceof Player User && (User.isCreative() || User.isSpectator())) {
            return false;
        }
        return isPerson(Target) || isGame(Target);
    }

    public static boolean moving(LivingEntity Target) {
        double Dx = Target.getX() - Target.xo;
        double Dz = Target.getZ() - Target.zo;
        return Dx * Dx + Dz * Dz > MovingSpeed * MovingSpeed || Target.getDeltaMovement().horizontalDistanceSqr() > MovingSpeed * MovingSpeed;
    }

    public static double reach(LivingEntity Target) {
        if (Target instanceof Player User) {
            if (User.isCrouching()) {
                return SmellRange;
            }
            if (User.isSprinting()) {
                return HearRun;
            }
            return moving(User) ? HearWalk : SmellRange;
        }
        return moving(Target) ? HearMob : SmellRange;
    }

    public static boolean bright(ServerLevel World, LivingEntity Target) {
        return World.getBrightness(LightLayer.BLOCK, Target.blockPosition()) >= BrightLight;
    }

    public static boolean senses(ServerLevel World, Scp939 Hunter, LivingEntity Target) {
        double Distance = Hunter.distanceTo(Target);
        if (Hunter.isContained()) {
            Scp939Rooms.Room Room = Hunter.room();
            if (Room == null || !Room.holds(Target.blockPosition())) {
                return false;
            }
            return isGame(Target) || Distance <= ContainedReach;
        }
        if (Distance <= SmellRange) {
            return true;
        }
        if (bright(World, Target) || Scp1730SpecialRooms.silenced(Target)) {
            return false;
        }
        return Distance <= reach(Target);
    }

    public static boolean lureable(ServerLevel World, Scp939 Hunter, @Nullable LivingEntity Target) {
        if (!(Target instanceof Player) || !isPrey(Hunter, Target)) {
            return false;
        }
        double Distance = Hunter.distanceTo(Target);
        if (Distance <= Hunter.lungeRange()) {
            return false;
        }
        if (Hunter.isContained()) {
            return Distance <= SmellRange || Distance <= reach(Target);
        }
        return senses(World, Hunter, Target);
    }

    @Nullable
    public static Player lureCandidate(ServerLevel World, Scp939 Hunter) {
        Player Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (Player User : World.players()) {
            double Distance = User.distanceToSqr(Hunter);
            if (Distance < BestDistance && Distance <= HearRun * HearRun && lureable(World, Hunter, User)) {
                Best = User;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    @Nullable
    public static LivingEntity nearest(ServerLevel World, Scp939 Hunter) {
        double Range = Hunter.isContained() ? Scp939Rooms.SpanAcross : HearRun;
        List<LivingEntity> Found = World.getEntitiesOfClass(LivingEntity.class, Hunter.getBoundingBox().inflate(Range, Range * 0.5, Range),
                Each -> isPrey(Hunter, Each) && senses(World, Hunter, Each));
        LivingEntity Best = null;
        double BestScore = Double.MAX_VALUE;
        for (LivingEntity Each : Found) {
            double Score = Hunter.distanceToSqr(Each) * (isPerson(Each) ? 0.6 : 1.0);
            if (Score < BestScore) {
                Best = Each;
                BestScore = Score;
            }
        }
        return Best;
    }

    public static boolean keeps(ServerLevel World, Scp939 Hunter, @Nullable LivingEntity Target) {
        if (!isPrey(Hunter, Target)) {
            return false;
        }
        if (Hunter.isContained()) {
            Scp939Rooms.Room Room = Hunter.room();
            return Room != null && Room.holds(Target.blockPosition()) && (isGame(Target) || Hunter.distanceTo(Target) <= ContainedReach * 2.0);
        }
        return Hunter.distanceToSqr(Target) <= ChaseRange * ChaseRange;
    }
}
