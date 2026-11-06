package net.rai.scp_unleashed.scp372;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Optional;

public final class Scp372Goals {
    public static final int StrollMin = 60;
    public static final int StrollMax = 200;
    public static final int Attempts = 10;

    private Scp372Goals() {
    }

    public static Optional<Vec3> strollTarget(Scp372Insect Creature, double Radius) {
        Vec3 Centre = Creature.tetherCentre();
        if (Centre == null) {
            return Optional.empty();
        }
        RandomSource Random = Creature.getRandom();
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Reach = Math.sqrt(Random.nextDouble()) * Radius;
            BlockPos Column = BlockPos.containing(Centre.x + Math.cos(Angle) * Reach, Centre.y, Centre.z + Math.sin(Angle) * Reach);
            Optional<Vec3> Spot = Scp372Anchoring.standingSpot(Creature.level(), Column, Creature.getType().getDimensions());
            if (Spot.isPresent() && Spot.get().distanceToSqr(Centre.x, Spot.get().y, Centre.z) <= Radius * Radius) {
                return Spot;
            }
        }
        return Optional.empty();
    }

    public static class HoldStill extends Goal {
        private final Scp372Insect Creature;

        public HoldStill(Scp372Insect Creature) {
            this.Creature = Creature;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Creature.isActing();
        }

        @Override
        public void start() {
            Creature.getNavigation().stop();
        }

        @Override
        public void tick() {
            Creature.getNavigation().stop();
        }
    }

    public static class Stroll extends Goal {
        private final Scp372Insect Creature;
        private final double Modifier;
        private int Cooldown = StrollMin;
        private Vec3 Target = Vec3.ZERO;

        public Stroll(Scp372Insect Creature, double Modifier) {
            this.Creature = Creature;
            this.Modifier = Modifier;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Creature.isActing() || Creature.action() == Scp372Insect.ActionSkitter || Creature.tetherCentre() == null) {
                return false;
            }
            if (--Cooldown > 0) {
                return false;
            }
            Cooldown = StrollMin + Creature.getRandom().nextInt(StrollMax - StrollMin + 1);
            Optional<Vec3> Found = strollTarget(Creature, Scp372Insect.StrollRadius);
            Found.ifPresent(Spot -> Target = Spot);
            return Found.isPresent();
        }

        @Override
        public void start() {
            Creature.getNavigation().moveTo(Target.x, Target.y, Target.z, Modifier);
        }

        @Override
        public boolean canContinueToUse() {
            return !Creature.isActing() && !Creature.getNavigation().isDone();
        }

        @Override
        public void stop() {
            Creature.getNavigation().stop();
        }
    }
}
