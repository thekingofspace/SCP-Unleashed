package net.rai.scp_unleashed.scp111;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp111Goals {
    public static final int RepathTicks = 10;
    public static final int LeaveDistance = 10;
    public static final int LeaveHeight = 3;
    public static final int NibbleSearch = 6;
    public static final int NibbleRescan = 200;
    public static final int NibblePatience = 400;
    public static final double NibbleReach = 0.45;

    private Scp111Goals() {
    }

    public static final class HoldStill extends Goal {
        private final Scp111Snail Holder;

        public HoldStill(Scp111Snail Holder) {
            this.Holder = Holder;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Holder.locksMovement();
        }

        @Override
        public void start() {
            Holder.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Holder.getNavigation().stop();
        }
    }

    public static final class LeaveHome extends Goal {
        private final Scp111Snail Leaver;
        private int Repath;

        public LeaveHome(Scp111Snail Leaver) {
            this.Leaver = Leaver;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Leaver.leavingFrom() != null && !Leaver.locksMovement();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            Leaver.getNavigation().stop();
        }

        @Override
        public void tick() {
            BlockPos Home = Leaver.leavingFrom();
            if (Home == null) {
                return;
            }
            if (--Repath <= 0 || Leaver.getNavigation().isDone()) {
                Repath = RepathTicks * 4;
                Vec3 Away = DefaultRandomPos.getPosAway(Leaver, LeaveDistance, LeaveHeight, Vec3.atCenterOf(Home));
                if (Away != null) {
                    Leaver.getNavigation().moveTo(Away.x, Away.y, Away.z, Scp111Snail.LeaveModifier);
                }
            }
        }
    }

    public static final class SeekGreens extends Goal {
        private final Scp111Snail Eater;
        private int Repath;

        public SeekGreens(Scp111Snail Eater) {
            this.Eater = Eater;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Eater.noticedFood() != null && !Eater.locksMovement() && Eater.leavingFrom() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            Eater.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ItemEntity Food = Eater.noticedFood();
            if (Food == null) {
                return;
            }
            Eater.getLookControl().setLookAt(Food.getX(), Food.getY(), Food.getZ());
            if (Eater.mouthReach(Food.position()) <= Scp111Snail.EatReach && Math.abs(Food.getY() - Eater.getY()) < 0.6) {
                Eater.beginEat(Food);
                return;
            }
            if (--Repath <= 0) {
                Repath = RepathTicks;
                if (!Eater.getNavigation().moveTo(Food.getX(), Food.getY(), Food.getZ(), Scp111Snail.ForageModifier) || Eater.getNavigation().isDone()) {
                    Eater.getMoveControl().setWantedPosition(Food.getX(), Food.getY(), Food.getZ(), Scp111Snail.ForageModifier);
                }
            }
        }
    }

    public static final class NibbleLeaves extends Goal {
        private final Scp111Snail Grazer;
        @Nullable
        private BlockPos Leaf;
        @Nullable
        private BlockPos Stand;
        private int Cooldown;
        private int Spent;
        private int Repath;

        public NibbleLeaves(Scp111Snail Grazer) {
            this.Grazer = Grazer;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            BlockPos Current = Grazer.nibbling();
            if (Current != null) {
                if (Grazer.canKeepNibbling() && Grazer.leavingFrom() == null && Grazer.level().getBlockState(Current).is(BlockTags.LEAVES)) {
                    Leaf = Current;
                    Stand = standBeside(Current);
                    return Stand != null;
                }
                Grazer.stopNibbling();
            }
            if (Cooldown > 0) {
                Cooldown--;
                return false;
            }
            if (!Grazer.canNibble() || Grazer.leavingFrom() != null) {
                return false;
            }
            Cooldown = NibbleRescan + Grazer.getRandom().nextInt(NibbleRescan);
            Spent = 0;
            return find();
        }

        @Override
        public boolean canContinueToUse() {
            return Leaf != null && Stand != null && Spent < NibblePatience && Grazer.isHungry() && Grazer.leavingFrom() == null
                    && Grazer.level().getBlockState(Leaf).is(BlockTags.LEAVES) && Grazer.noticedFood() == null;
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            Grazer.getNavigation().stop();
            if (!Grazer.isActing()) {
                Grazer.stopNibbling();
            }
            Leaf = null;
            Stand = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Nullable
        private BlockPos standBeside(BlockPos Current) {
            BlockPos Nearest = null;
            double Best = Double.MAX_VALUE;
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                BlockPos Spot = Current.relative(Side);
                double Distance = Vec3.atBottomCenterOf(Spot).distanceToSqr(Grazer.position());
                if (Distance < Best) {
                    Best = Distance;
                    Nearest = Spot;
                }
            }
            return Nearest;
        }

        @Override
        public void tick() {
            Spent++;
            if (Leaf == null || Stand == null || Grazer.isActing()) {
                return;
            }
            Vec3 Target = Vec3.atBottomCenterOf(Stand).add(Vec3.atLowerCornerOf(Leaf.subtract(Stand)).scale(0.3));
            double Dx = Target.x - Grazer.getX();
            double Dz = Target.z - Grazer.getZ();
            if (Dx * Dx + Dz * Dz <= NibbleReach * NibbleReach && Math.abs(Target.y - Grazer.getY()) < 0.6) {
                Grazer.getNavigation().stop();
                Grazer.beginNibble(Leaf);
                return;
            }
            if (--Repath <= 0) {
                Repath = RepathTicks;
                if (!Grazer.getNavigation().moveTo(Target.x, Target.y, Target.z, Scp111Snail.ForageModifier)) {
                    Grazer.getMoveControl().setWantedPosition(Target.x, Target.y, Target.z, Scp111Snail.ForageModifier);
                }
            }
        }

        private boolean find() {
            Level World = Grazer.level();
            BlockPos Centre = Grazer.blockPosition();
            double Best = Double.MAX_VALUE;
            Leaf = null;
            Stand = null;
            for (BlockPos Cell : BlockPos.betweenClosed(Centre.offset(-NibbleSearch, -1, -NibbleSearch), Centre.offset(NibbleSearch, 1, NibbleSearch))) {
                if (!World.getBlockState(Cell).is(BlockTags.LEAVES)) {
                    continue;
                }
                for (Direction Side : Direction.Plane.HORIZONTAL) {
                    BlockPos Spot = Cell.relative(Side);
                    if (!World.getBlockState(Spot).getCollisionShape(World, Spot).isEmpty() || !World.getBlockState(Spot).getFluidState().isEmpty()) {
                        continue;
                    }
                    BlockPos Under = Spot.below();
                    if (!World.getBlockState(Under).isFaceSturdy(World, Under, Direction.UP)) {
                        continue;
                    }
                    double Distance = Spot.distSqr(Centre);
                    if (Distance < Best) {
                        Best = Distance;
                        Leaf = Cell.immutable();
                        Stand = Spot.immutable();
                    }
                }
            }
            return Leaf != null;
        }
    }
}
