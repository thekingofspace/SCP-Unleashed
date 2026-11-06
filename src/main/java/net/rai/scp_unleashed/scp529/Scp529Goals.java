package net.rai.scp_unleashed.scp529;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp529Goals {
    public static final double SeekRange = 48.0;
    public static final double SeekStop = 2.5;
    public static final double VisitNear = 3.5;
    public static final double BedSpeed = 1.0;
    public static final int BedReach = 8;
    public static final double BedAccept = 1.25;
    public static final int BedPauseMin = 600;
    public static final int BedPauseMax = 1200;
    public static final double PerchSpeed = 0.8;
    public static final int PerchReach = 8;
    public static final int PerchPauseMin = 400;
    public static final int PerchPauseMax = 800;
    public static final int SeekTicks = 2400;
    public static final int Replan = 10;
    public static final double FretRange = 24.0;
    public static final double FretNear = 2.0;
    public static final int PaceReach = 6;

    private Scp529Goals() {
    }

    public static class Seek extends Goal {
        private final Scp529Cat Josie;
        @Nullable
        private Player Target;
        private boolean Visit;
        private int Ticks;
        private int Wait;

        public Seek(Scp529Cat Josie) {
            this.Josie = Josie;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Nullable
        private Player sought() {
            if (Josie.sought() == null || !(Josie.level() instanceof ServerLevel World)) {
                return null;
            }
            Player Found = World.getPlayerByUUID(Josie.sought());
            if (Found == null || Found.isSpectator() || !Found.isAlive() || Found.distanceTo(Josie) > SeekRange) {
                return null;
            }
            return Found;
        }

        @Override
        public boolean canUse() {
            if (Josie.isDistressed() || Josie.isInSittingPose() || Josie.isResting()) {
                return false;
            }
            Player Found = sought();
            Visit = false;
            if (Found == null && Josie.visitDue()) {
                Found = Josie.favourite(Scp529Cat.VisitRange);
                Visit = Found != null;
                if (Found == null) {
                    Josie.visited();
                }
            }
            if (Found == null) {
                return false;
            }
            if (Found.distanceTo(Josie) <= VisitNear) {
                greet(Found);
                return false;
            }
            Target = Found;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return Target != null && Target.isAlive() && !Josie.isDistressed() && Ticks < SeekTicks && Target.distanceTo(Josie) > SeekStop
                    && (Visit || Josie.sought() != null);
        }

        @Override
        public void start() {
            Ticks = 0;
            Wait = 0;
        }

        @Override
        public void tick() {
            Ticks++;
            if (Target == null) {
                return;
            }
            Josie.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            Wait--;
            if (Wait <= 0) {
                Wait = Replan;
                Josie.getNavigation().moveTo(Target, Scp529Cat.SeekModifier);
            }
        }

        @Override
        public void stop() {
            if (Target != null && Target.distanceTo(Josie) <= SeekStop) {
                greet(Target);
            } else if (Ticks >= SeekTicks) {
                Josie.seek(null);
                Josie.visited();
            }
            Target = null;
            Josie.getNavigation().stop();
        }

        private void greet(Player Found) {
            boolean Liked = Josie.likes(Found.getUUID());
            Josie.seek(null);
            Josie.visited();
            Josie.face(Found.position());
            Scp529Effects.voice(Josie, Liked ? Scp529Registry.Trill.get() : Scp529Registry.Mew.get(), 0.8f, 1.0f);
            if (Liked && Josie.level() instanceof ServerLevel World) {
                Scp529Effects.voice(Josie, Scp529Registry.Purr.get(), 0.6f, 1.0f);
                Scp529Effects.hearts(World, Josie, 1);
            }
        }
    }

    public static class BedNap extends MoveToBlockGoal {
        private final Scp529Cat Josie;

        public BedNap(Scp529Cat Josie) {
            super(Josie, BedSpeed, BedReach, 6);
            this.Josie = Josie;
            setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Josie.isAlive() && !Josie.isDistressed() && !Josie.isLying() && !Josie.isPassenger() && !Josie.recentlyHurt() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !Josie.isDistressed() && !Josie.recentlyHurt() && super.canContinueToUse();
        }

        @Override
        protected int nextStartTick(PathfinderMob Creature) {
            return reducedTickDelay(BedPauseMin + Creature.getRandom().nextInt(BedPauseMax - BedPauseMin + 1));
        }

        @Override
        public double acceptedDistance() {
            return BedAccept;
        }

        @Override
        public void start() {
            super.start();
            Josie.setInSittingPose(false);
        }

        @Override
        public void stop() {
            super.stop();
            Josie.setLying(false);
        }

        @Override
        public void tick() {
            super.tick();
            if (!isReachedTarget()) {
                Josie.setLying(false);
            } else if (!Josie.isLying()) {
                Josie.setLying(true);
            }
        }

        @Override
        protected boolean isValidTarget(LevelReader World, BlockPos Pos) {
            return World.isEmptyBlock(Pos.above()) && World.getBlockState(Pos).is(BlockTags.BEDS);
        }
    }

    public static class Perch extends MoveToBlockGoal {
        private final Scp529Cat Josie;

        public Perch(Scp529Cat Josie) {
            super(Josie, PerchSpeed, PerchReach);
            this.Josie = Josie;
        }

        @Override
        public boolean canUse() {
            return Josie.isAlive() && !Josie.isDistressed() && !Josie.isResting() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !Josie.isDistressed() && super.canContinueToUse();
        }

        @Override
        protected int nextStartTick(PathfinderMob Creature) {
            return reducedTickDelay(PerchPauseMin + Creature.getRandom().nextInt(PerchPauseMax - PerchPauseMin + 1));
        }

        @Override
        public void start() {
            super.start();
            Josie.setInSittingPose(false);
        }

        @Override
        public void stop() {
            super.stop();
            Josie.setInSittingPose(false);
        }

        @Override
        public void tick() {
            super.tick();
            Josie.setInSittingPose(isReachedTarget());
        }

        @Override
        protected boolean isValidTarget(LevelReader World, BlockPos Pos) {
            if (!World.isEmptyBlock(Pos.above())) {
                return false;
            }
            BlockState State = World.getBlockState(Pos);
            if (State.is(Blocks.CHEST)) {
                return ChestBlockEntity.getOpenCount(World, Pos) < 1;
            }
            if (State.is(Blocks.FURNACE)) {
                return State.getValue(FurnaceBlock.LIT);
            }
            return false;
        }
    }

    public static class Settle extends Goal {
        private final Scp529Cat Josie;
        private int Length;
        private int Ticks;

        public Settle(Scp529Cat Josie) {
            this.Josie = Josie;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        private boolean calm() {
            return Josie.isAlive() && Josie.onGround() && !Josie.isInWater() && !Josie.isDistressed() && !Josie.recentlyHurt() && !Josie.isLying()
                    && Josie.getTarget() == null && !Josie.isPassenger();
        }

        @Override
        public boolean canUse() {
            if (!calm() || Josie.isInSittingPose() || Josie.isActing() || Josie.isNapping()) {
                return false;
            }
            return Josie.getRandom().nextInt(reducedTickDelay(Scp529Cat.SettleChance)) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return calm() && Ticks < Length;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            Ticks = 0;
            Length = Scp529Cat.SettleMin + Josie.getRandom().nextInt(Scp529Cat.SettleMax - Scp529Cat.SettleMin + 1);
            Josie.getNavigation().stop();
            Josie.setInSittingPose(true);
        }

        @Override
        public void tick() {
            Ticks++;
        }

        @Override
        public void stop() {
            Josie.setInSittingPose(false);
        }
    }

    public static class Nap extends Goal {
        private final Scp529Cat Josie;
        private int Length;

        public Nap(Scp529Cat Josie) {
            this.Josie = Josie;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        private boolean settled() {
            return Josie.isAlive() && Josie.onGround() && !Josie.isInWater() && !Josie.isDistressed() && !Josie.recentlyHurt() && !Josie.isLying()
                    && Josie.getTarget() == null && !Josie.isPassenger();
        }

        @Override
        public boolean canUse() {
            if (!settled() || Josie.isInSittingPose() || Josie.isActing()) {
                return false;
            }
            return Josie.getRandom().nextInt(reducedTickDelay(Scp529Cat.NapChance)) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return settled() && Josie.napTicks() < Length;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            Length = Scp529Cat.NapMin + Josie.getRandom().nextInt(Scp529Cat.NapMax - Scp529Cat.NapMin + 1);
            Josie.getNavigation().stop();
            Josie.setNapping(true);
        }

        @Override
        public void tick() {
            Josie.napTick();
        }

        @Override
        public void stop() {
            Josie.wake();
        }
    }

    public static class Fret extends Goal {
        private final Scp529Cat Josie;
        @Nullable
        private LivingEntity Target;
        private int Wait;

        public Fret(Scp529Cat Josie) {
            this.Josie = Josie;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Nullable
        private LivingEntity pick() {
            Player Liked = Josie.favourite(FretRange);
            if (Liked != null) {
                return Liked;
            }
            return Josie.level().getNearestPlayer(Josie, FretRange);
        }

        @Override
        public boolean canUse() {
            return Josie.isAlive() && Josie.isDistressed() && !Josie.isPassenger();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Wait = 0;
            Josie.setInSittingPose(false);
        }

        @Override
        public void tick() {
            Wait--;
            if (Wait > 0) {
                if (Target != null) {
                    Josie.getLookControl().setLookAt(Target, 30.0f, 30.0f);
                }
                return;
            }
            Wait = Replan * 2;
            Target = pick();
            if (Target != null) {
                Josie.getLookControl().setLookAt(Target, 30.0f, 30.0f);
                if (Target.distanceTo(Josie) > FretNear) {
                    Josie.getNavigation().moveTo(Target, Scp529Cat.FretModifier);
                } else {
                    Josie.getNavigation().stop();
                }
                return;
            }
            if (Josie.getNavigation().isDone()) {
                Vec3 Spot = DefaultRandomPos.getPos(Josie, PaceReach, 2);
                if (Spot != null) {
                    Josie.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, Scp529Cat.FretModifier);
                }
            }
        }

        @Override
        public void stop() {
            Target = null;
            Josie.getNavigation().stop();
        }
    }
}
