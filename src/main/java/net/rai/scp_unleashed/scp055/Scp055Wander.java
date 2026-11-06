package net.rai.scp_unleashed.scp055;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Scp055Wander {
    public static final int Radius = 6;
    public static final int Rise = 3;
    public static final int MaxNodes = 240;
    public static final int MinPath = 2;
    public static final int MaxPath = 5;
    public static final int MaxWalks = 3;
    public static final int MaxSteps = 10;
    public static final float WalkSpeed = 0.13f;
    public static final int WalkSlack = 8;
    public static final int ClimbSlack = 4;
    public static final int FiddleTicks = 22;
    public static final int FirstLookMin = 12;
    public static final int FirstLookSpread = 8;
    public static final int LookMin = 12;
    public static final int LookSpread = 12;
    public static final float WalkChance = 0.6f;
    public static final float FiddleChance = 0.8f;
    public static final float LookAhead = 0.55f;

    private Scp055Wander() {
    }

    public static List<Scp055Step> plan(BlockGetter World, Player User, RandomSource Random, int Budget) {
        List<Scp055Step> Steps = new ArrayList<>();
        BlockPos Current = User.blockPosition();
        boolean CanWalk = User.onGround() && !User.isPassenger() && !User.getAbilities().flying && !User.isFallFlying() && !User.isInWater()
                && !User.isInLava() && !User.isSleeping() && standable(World, Current);
        float Facing = User.getYRot();
        int Remaining = Budget;
        int Walks = 0;
        boolean Fiddled = false;
        Set<BlockPos> Visited = new HashSet<>();
        Visited.add(Current);
        int First = Math.min(Remaining, FirstLookMin + Random.nextInt(FirstLookSpread + 1));
        Facing = turn(Facing, Random, 40, 110);
        Steps.add(Scp055Step.look(First, Facing, -10.0f + Random.nextFloat() * 30.0f));
        Remaining -= First;
        while (Remaining > 0 && Steps.size() < MaxSteps - 1) {
            float Roll = Random.nextFloat();
            if (CanWalk && Walks < MaxWalks && Roll < WalkChance) {
                List<BlockPos> Path = wander(World, Current, Random, Visited);
                if (Path != null) {
                    int Ticks = walkTicks(Current, Path);
                    if (Ticks <= Remaining) {
                        Steps.add(Scp055Step.walk(Ticks, Path));
                        Remaining -= Ticks;
                        BlockPos Last = Path.get(Path.size() - 1);
                        BlockPos Before = Path.size() > 1 ? Path.get(Path.size() - 2) : Current;
                        Facing = yawTowards(Last.getX() - Before.getX(), Last.getZ() - Before.getZ());
                        Visited.addAll(Path);
                        Current = Last;
                        Walks++;
                        continue;
                    }
                }
            }
            if (!Fiddled && Roll < FiddleChance && Remaining >= FiddleTicks) {
                Steps.add(Scp055Step.fiddle(FiddleTicks, Facing, 52.0f + Random.nextFloat() * 14.0f));
                Remaining -= FiddleTicks;
                Fiddled = true;
                continue;
            }
            int Ticks = Math.min(Remaining, LookMin + Random.nextInt(LookSpread + 1));
            if (Random.nextBoolean()) {
                Facing = turn(Facing, Random, 50, 150);
                Steps.add(Scp055Step.look(Ticks, Facing, -25.0f + Random.nextFloat() * 50.0f));
            } else {
                Steps.add(Scp055Step.pause(Ticks));
            }
            Remaining -= Ticks;
        }
        if (Remaining > 0) {
            Steps.add(Scp055Step.pause(Remaining));
        }
        return Steps;
    }

    private static float turn(float Facing, RandomSource Random, int Least, int Most) {
        float Amount = Least + Random.nextInt(Most - Least + 1);
        return Mth.wrapDegrees(Facing + (Random.nextBoolean() ? Amount : -Amount));
    }

    public static float yawTowards(double Dx, double Dz) {
        return (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
    }

    public static int walkTicks(BlockPos Start, List<BlockPos> Path) {
        double Length = 0.0;
        int Climbs = 0;
        BlockPos Previous = Start;
        for (BlockPos Next : Path) {
            Length += Math.sqrt(Previous.distSqr(new BlockPos(Next.getX(), Previous.getY(), Next.getZ())));
            if (Next.getY() > Previous.getY()) {
                Climbs++;
            }
            Previous = Next;
        }
        return Mth.ceil(Length / WalkSpeed) + WalkSlack + Climbs * ClimbSlack;
    }

    private static List<BlockPos> wander(BlockGetter World, BlockPos Origin, RandomSource Random, Set<BlockPos> Visited) {
        Map<BlockPos, BlockPos> Parents = new HashMap<>();
        Map<BlockPos, Integer> Depths = new HashMap<>();
        ArrayDeque<BlockPos> Queue = new ArrayDeque<>();
        Parents.put(Origin, Origin);
        Depths.put(Origin, 0);
        Queue.add(Origin);
        List<BlockPos> Candidates = new ArrayList<>();
        while (!Queue.isEmpty() && Parents.size() < MaxNodes) {
            BlockPos Here = Queue.poll();
            int Depth = Depths.get(Here);
            if (Depth >= MinPath && !Visited.contains(Here)) {
                Candidates.add(Here);
            }
            if (Depth >= MaxPath) {
                continue;
            }
            List<Direction> Sides = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
            for (int Index = Sides.size() - 1; Index > 0; Index--) {
                Collections.swap(Sides, Index, Random.nextInt(Index + 1));
            }
            for (Direction Side : Sides) {
                BlockPos Next = step(World, Here, Side);
                if (Next == null || Parents.containsKey(Next)) {
                    continue;
                }
                if (Math.abs(Next.getX() - Origin.getX()) > Radius || Math.abs(Next.getZ() - Origin.getZ()) > Radius || Math.abs(Next.getY() - Origin.getY()) > Rise) {
                    continue;
                }
                Parents.put(Next, Here);
                Depths.put(Next, Depth + 1);
                Queue.add(Next);
            }
        }
        if (Candidates.isEmpty()) {
            return null;
        }
        BlockPos Goal = Candidates.get(Random.nextInt(Candidates.size()));
        List<BlockPos> Path = new ArrayList<>();
        for (BlockPos Walker = Goal; !Walker.equals(Origin); Walker = Parents.get(Walker)) {
            Path.add(Walker);
        }
        Collections.reverse(Path);
        return Path;
    }

    private static BlockPos step(BlockGetter World, BlockPos Here, Direction Side) {
        BlockPos Flat = Here.relative(Side);
        if (standable(World, Flat)) {
            return Flat;
        }
        BlockPos Down = Flat.below();
        if (passable(World, Flat) && passable(World, Flat.above()) && standable(World, Down)) {
            return Down;
        }
        BlockPos Up = Flat.above();
        if (passable(World, Here.above(2)) && standable(World, Up)) {
            return Up;
        }
        return null;
    }

    public static boolean standable(BlockGetter World, BlockPos Feet) {
        return passable(World, Feet) && passable(World, Feet.above()) && support(World, Feet.below()) && !nearFire(World, Feet);
    }

    public static boolean passable(BlockGetter World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return State.getCollisionShape(World, Pos).isEmpty() && !hazard(World, Pos, State);
    }

    public static boolean support(BlockGetter World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return State.isFaceSturdy(World, Pos, Direction.UP) && !hazard(World, Pos, State);
    }

    public static boolean hazard(BlockGetter World, BlockPos Pos, BlockState State) {
        if (!State.getFluidState().isEmpty() || NodeEvaluator.isBurningBlock(State)) {
            return true;
        }
        PathType Custom = State.getBlockPathType(World, Pos, null);
        if (Custom != null && Custom != PathType.OPEN && Custom != PathType.WALKABLE && Custom != PathType.BLOCKED) {
            return true;
        }
        return State.is(Blocks.CACTUS) || State.is(Blocks.SWEET_BERRY_BUSH) || State.is(Blocks.WITHER_ROSE) || State.is(Blocks.POINTED_DRIPSTONE)
                || State.is(Blocks.POWDER_SNOW) || State.is(Blocks.COBWEB) || State.is(Blocks.HONEY_BLOCK) || State.is(Blocks.SCAFFOLDING)
                || State.is(BlockTags.TRAPDOORS) || State.getBlock() instanceof BasePressurePlateBlock || State.getBlock() instanceof TripWireBlock
                || State.getBlock() instanceof BaseRailBlock || State.getBlock() instanceof Scp055CaseBlock;
    }

    private static boolean nearFire(BlockGetter World, BlockPos Feet) {
        for (BlockPos Near : BlockPos.betweenClosed(Feet.offset(-1, -1, -1), Feet.offset(1, 1, 1))) {
            BlockState State = World.getBlockState(Near);
            if (NodeEvaluator.isBurningBlock(State) || State.getFluidState().is(FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
    }

    public static boolean safeAhead(BlockGetter World, Player User, double Dx, double Dz) {
        double Length = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Length < 1.0E-4) {
            return true;
        }
        Vec3 Ahead = User.position().add(Dx / Length * LookAhead, 0.0, Dz / Length * LookAhead);
        BlockPos Feet = BlockPos.containing(Ahead.x, User.getY() + 0.01, Ahead.z);
        if (hazard(World, Feet, World.getBlockState(Feet)) || hazard(World, Feet.above(), World.getBlockState(Feet.above())) || nearFire(World, Feet)) {
            return false;
        }
        if (!World.getBlockState(Feet).getCollisionShape(World, Feet).isEmpty()) {
            return true;
        }
        if (support(World, Feet.below())) {
            return true;
        }
        BlockPos Lower = Feet.below();
        return passable(World, Lower) && support(World, Lower.below());
    }
}
