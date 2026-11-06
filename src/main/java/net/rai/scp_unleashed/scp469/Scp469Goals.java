package net.rai.scp_unleashed.scp469;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp469Goals {
    public static final int RepathTicks = 10;
    public static final double RepathDistance = 1.5;
    public static final int ScanTicks = 10;
    public static final int RoamInterval = 90;
    public static final int RoamRange = 10;
    public static final int BatterTicks = 120;
    public static final int BatterBlow = 20;
    public static final double BatterReach = 2.6;

    private Scp469Goals() {
    }

    public static class Hold extends Goal {
        private final Scp469Angel Owner;

        public Hold(Scp469Angel Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Owner.busy();
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.busy();
        }

        @Override
        public void start() {
            Owner.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Owner.getNavigation().stop();
            LivingEntity Held = Owner.victim();
            if (Held != null) {
                Owner.getLookControl().setLookAt(Held, 30.0f, 30.0f);
            }
        }
    }

    public static class Hunt extends Goal {
        private final Scp469Angel Owner;
        private int Repath;
        @Nullable
        private Vec3 Aimed;

        public Hunt(Scp469Angel Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !Owner.busy() && (Owner.getTarget() != null || Owner.lastSeen() != null);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Repath = 0;
            Aimed = null;
        }

        @Override
        public void stop() {
            Owner.getNavigation().stop();
            Aimed = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Owner.getTarget();
            boolean Visible = Target != null && Owner.getSensing().hasLineOfSight(Target);
            Vec3 Goal0 = Visible ? Target.position() : Owner.lastSeen();
            if (Goal0 == null) {
                return;
            }
            if (Visible) {
                Owner.getLookControl().setLookAt(Target, 30.0f, 30.0f);
                double Distance = Owner.flatDistance(Target);
                if (Distance < Scp469Angel.LungeRange && Distance > Owner.reach() && Owner.canLunge()) {
                    Owner.beginLunge();
                    Repath = 0;
                }
                if (Owner.canStrike(Target) && Owner.attackReady()) {
                    Owner.strike(Target);
                    return;
                }
            }
            if (--Repath <= 0 || Aimed == null || Aimed.distanceToSqr(Goal0) > RepathDistance * RepathDistance || Owner.getNavigation().isDone()) {
                Repath = RepathTicks;
                Aimed = Goal0;
                if (Visible) {
                    Owner.getNavigation().moveTo(Target, Owner.huntSpeed());
                } else {
                    Owner.getNavigation().moveTo(Goal0.x, Goal0.y, Goal0.z, Owner.huntSpeed());
                }
            }
        }
    }

    public static class Roam extends Goal {
        private final Scp469Angel Owner;
        @Nullable
        private Vec3 Wanted;

        public Roam(Scp469Angel Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Owner.busy() || Owner.getTarget() != null || Owner.lastSeen() != null || Owner.getRandom().nextInt(RoamInterval) != 0) {
                return false;
            }
            Wanted = DefaultRandomPos.getPos(Owner, RoamRange, 4);
            return Wanted != null;
        }

        @Override
        public boolean canContinueToUse() {
            return !Owner.busy() && Owner.getTarget() == null && Owner.lastSeen() == null && !Owner.getNavigation().isDone();
        }

        @Override
        public void start() {
            if (Wanted != null) {
                Owner.getNavigation().moveTo(Wanted.x, Wanted.y, Wanted.z, Scp469Angel.RoamModifier);
            }
        }

        @Override
        public void stop() {
            Owner.getNavigation().stop();
            Wanted = null;
        }
    }

    public static class Batter extends Goal {
        private final Scp469Angel Owner;
        @Nullable
        private BlockPos Door;
        private int Time;
        private int Shown = -1;

        public Batter(Scp469Angel Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        public static boolean breakable(BlockState State) {
            return State.is(BlockTags.WOODEN_DOORS) && State.getBlock() instanceof DoorBlock && !State.getValue(DoorBlock.OPEN);
        }

        @Nullable
        private BlockPos lower(ServerLevel World, BlockPos Pos) {
            BlockState State = World.getBlockState(Pos);
            if (!breakable(State)) {
                return null;
            }
            return State.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? Pos.below() : Pos;
        }

        @Nullable
        private BlockPos find(ServerLevel World) {
            Path Route = Owner.getNavigation().getPath();
            if (Route != null && !Route.isDone()) {
                int Last = Math.min(Route.getNextNodeIndex() + 3, Route.getNodeCount());
                for (int Index = Math.max(0, Route.getNextNodeIndex() - 1); Index < Last; Index++) {
                    Node Step = Route.getNode(Index);
                    BlockPos Pos = new BlockPos(Step.x, Step.y, Step.z);
                    if (Owner.distanceToSqr(Pos.getX() + 0.5, Owner.getY(), Pos.getZ() + 0.5) > BatterReach * BatterReach * 1.6) {
                        continue;
                    }
                    for (BlockPos Probe : new BlockPos[]{Pos, Pos.above()}) {
                        BlockPos Found = lower(World, Probe);
                        if (Found != null) {
                            return Found;
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public boolean canUse() {
            if (!(Owner.level() instanceof ServerLevel World) || Owner.busy() || !EventHooks.canEntityGrief(World, Owner)) {
                return false;
            }
            if (Owner.getTarget() == null && Owner.lastSeen() == null && !Owner.horizontalCollision) {
                return false;
            }
            Door = find(World);
            return Door != null && Owner.distanceToSqr(Door.getX() + 0.5, Owner.getY(), Door.getZ() + 0.5) < BatterReach * BatterReach;
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.level() instanceof ServerLevel World && Door != null && !Owner.busy() && breakable(World.getBlockState(Door)) && Time <= BatterTicks
                    && Owner.distanceToSqr(Door.getX() + 0.5, Owner.getY(), Door.getZ() + 0.5) < (BatterReach + 1.0) * (BatterReach + 1.0);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            Time = 0;
            Shown = -1;
            Owner.getNavigation().stop();
        }

        @Override
        public void stop() {
            if (Owner.level() instanceof ServerLevel World && Door != null) {
                World.destroyBlockProgress(Owner.getId(), Door, -1);
            }
            Door = null;
        }

        @Override
        public void tick() {
            if (!(Owner.level() instanceof ServerLevel World) || Door == null) {
                return;
            }
            Owner.getLookControl().setLookAt(Door.getX() + 0.5, Door.getY() + 1.0, Door.getZ() + 0.5);
            Time++;
            if (Time % BatterBlow == 0) {
                World.levelEvent(LevelEvent.SOUND_ZOMBIE_WOODEN_DOOR, Door, 0);
                Scp469Effects.layers(World, Owner, Scp469Registry.BatterLayers, 1.0f, 0.75f + Owner.getRandom().nextFloat() * 0.1f);
                Scp469Effects.batter(World, Door);
            }
            int Progress = (int) ((float) Time / BatterTicks * 10.0f);
            if (Progress != Shown) {
                World.destroyBlockProgress(Owner.getId(), Door, Progress);
                Shown = Progress;
            }
            if (Time >= BatterTicks) {
                BlockState State = World.getBlockState(Door);
                World.removeBlock(Door, false);
                World.levelEvent(LevelEvent.SOUND_ZOMBIE_DOOR_CRASH, Door, 0);
                World.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, Door, Block.getId(State));
                World.destroyBlockProgress(Owner.getId(), Door, -1);
                Door = null;
            }
        }
    }

    public static class Seek extends Goal {
        private final Scp469Angel Owner;
        private int Scan;

        public Seek(Scp469Angel Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!(Owner.level() instanceof ServerLevel World) || --Scan > 0) {
                return false;
            }
            Scan = ScanTicks;
            LivingEntity Current = Owner.getTarget();
            LivingEntity Found = Scp469Prey.nearest(World, Owner, Scp469Angel.FollowRange);
            if (Found == null || Found == Current) {
                return false;
            }
            if (Current != null && Scp469Prey.prey(Owner, Current) && !Scp469Prey.clearlyCloser(Owner, Found, Current)) {
                return false;
            }
            Owner.setTarget(Found);
            Owner.remember(Found.position(), Found.getUUID());
            return false;
        }
    }

    public static class Avenge extends Goal {
        private final Scp469Angel Owner;
        private int Seen;

        public Avenge(Scp469Angel Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            LivingEntity Attacker = Owner.getLastHurtByMob();
            int Stamp = Owner.getLastHurtByMobTimestamp();
            if (Attacker == null || Stamp == Seen || !Scp469Prey.prey(Owner, Attacker)) {
                return false;
            }
            Seen = Stamp;
            if (Owner.getTarget() == null) {
                Owner.setTarget(Attacker);
                Owner.remember(Attacker.position(), Attacker.getUUID());
            }
            return false;
        }
    }
}
