package net.rai.scp_unleashed.scp049;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.jetbrains.annotations.Nullable;

public class Scp049Navigation extends GroundPathNavigation {
    public Scp049Navigation(Mob Owner, Level World) {
        super(Owner, World);
        setCanOpenDoors(true);
        setCanPassDoors(true);
        setCanFloat(true);
    }

    @Override
    protected PathFinder createPathFinder(int MaxVisited) {
        this.nodeEvaluator = new GateEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanOpenDoors(true);
        this.nodeEvaluator.setCanFloat(true);
        return new PathFinder(this.nodeEvaluator, MaxVisited);
    }

    @Nullable
    public Path routeTo(BlockPos Target, int Accuracy, float Range) {
        return createPath(ImmutableSet.of(Target), 8, false, Accuracy, Range);
    }

    public static boolean isOpenableGate(BlockState State) {
        return State.getBlock() instanceof FenceGateBlock && State.is(BlockTags.FENCE_GATES);
    }

    public static boolean isOpenableDoor(BlockGetter World, BlockPos Pos, BlockState State) {
        return State.getBlock() instanceof DoorBlock Door && Door.type().canOpenByHand() && State.is(BlockTags.WOODEN_DOORS);
    }

    public static boolean isPortal(BlockGetter World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return isOpenableGate(State) || isOpenableDoor(World, Pos, State);
    }

    public static class GateEvaluator extends WalkNodeEvaluator {
        @Override
        public PathType getPathTypeOfMob(PathfindingContext Context, int X, int Y, int Z, Mob Owner) {
            if (Owner instanceof Scp049Doctor Doctor && Doctor.shuns(X, Y, Z)) {
                return PathType.BLOCKED;
            }
            return super.getPathTypeOfMob(Context, X, Y, Z, Owner);
        }

        @Override
        public PathType getPathType(PathfindingContext Context, int X, int Y, int Z) {
            PathType Found = super.getPathType(Context, X, Y, Z);
            if (Found == PathType.FENCE) {
                BlockState State = Context.level().getBlockState(new BlockPos(X, Y, Z));
                if (isOpenableGate(State) && !State.getValue(FenceGateBlock.OPEN)) {
                    return PathType.DOOR_WOOD_CLOSED;
                }
            }
            return Found;
        }
    }
}
