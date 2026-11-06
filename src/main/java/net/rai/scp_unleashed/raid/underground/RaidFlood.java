package net.rai.scp_unleashed.raid.underground;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.npc.doors.NpcDoorAccess;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RaidFlood {
    public static final RaidFlood Empty = new RaidFlood(new LinkedHashMap<>(), null, false);

    private final LinkedHashMap<Long, Integer> Steps;
    @Nullable
    private final BlockPos Entrance;
    private final boolean Capped;

    private RaidFlood(LinkedHashMap<Long, Integer> Steps, @Nullable BlockPos Entrance, boolean Capped) {
        this.Steps = Steps;
        this.Entrance = Entrance;
        this.Capped = Capped;
    }

    public Map<Long, Integer> steps() {
        return Steps;
    }

    @Nullable
    public BlockPos entrance() {
        return Entrance;
    }

    public boolean capped() {
        return Capped;
    }

    public boolean empty() {
        return Steps.isEmpty();
    }

    public boolean contains(BlockPos Pos) {
        return Steps.containsKey(Pos.asLong());
    }

    public int step(BlockPos Pos) {
        Integer Found = Steps.get(Pos.asLong());
        return Found == null ? -1 : Found;
    }

    public static boolean gate(BlockState State) {
        return State.is(BlockTags.WOODEN_DOORS) || State.is(BlockTags.FENCE_GATES) || NpcDoorAccess.isGate(State);
    }

    public static boolean passable(Level World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        if (!State.getFluidState().isEmpty()) {
            return false;
        }
        return State.getCollisionShape(World, Pos).isEmpty() || gate(State);
    }

    public static boolean floored(Level World, BlockPos Feet) {
        BlockPos Under = Feet.below();
        BlockState Floor = World.getBlockState(Under);
        return Floor.isFaceSturdy(World, Under, Direction.UP) && Floor.getFluidState().isEmpty() && World.getFluidState(Feet).isEmpty();
    }

    @Nullable
    private static BlockPos move(Level World, BlockPos Here, Direction Side) {
        BlockPos Ahead = Here.relative(Side);
        if (passable(World, Ahead) && passable(World, Ahead.above())) {
            if (floored(World, Ahead)) {
                return Ahead;
            }
            for (int Drop = 1; Drop <= UndergroundRules.MaxDrop; Drop++) {
                BlockPos Down = Ahead.below(Drop);
                if (!passable(World, Down)) {
                    return null;
                }
                if (floored(World, Down)) {
                    return Down;
                }
            }
            return null;
        }
        BlockPos Up = Ahead.above();
        if (passable(World, Up) && passable(World, Up.above()) && passable(World, Here.above(2)) && floored(World, Up)) {
            return Up;
        }
        return null;
    }

    public static RaidFlood flood(Level World, Collection<BlockPos> Starts, BlockPos Centre, double Reach, int MinY, int MaxY, int MaxNodes) {
        LinkedHashMap<Long, Integer> Steps = new LinkedHashMap<>();
        ArrayDeque<BlockPos> Queue = new ArrayDeque<>();
        for (BlockPos Start : Starts) {
            if (Steps.putIfAbsent(Start.asLong(), 0) == null) {
                Queue.add(Start);
            }
        }
        BlockPos Entrance = null;
        int EntranceStep = -1;
        double Square = Reach * Reach;
        boolean Capped = false;
        while (!Queue.isEmpty()) {
            if (Steps.size() >= MaxNodes) {
                Capped = true;
                break;
            }
            BlockPos Here = Queue.poll();
            int Step = Steps.get(Here.asLong());
            boolean Open = RaidSurvey.open(World, Here);
            if (Open && Entrance == null) {
                Entrance = Here;
                EntranceStep = Step;
            }
            if (Open && EntranceStep >= 0 && Step - EntranceStep >= UndergroundRules.OpenSteps) {
                continue;
            }
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                BlockPos Next = move(World, Here, Side);
                if (Next == null || Next.getY() < MinY || Next.getY() > MaxY || Steps.containsKey(Next.asLong()) || !World.isLoaded(Next)) {
                    continue;
                }
                double Dx = Next.getX() + 0.5 - (Centre.getX() + 0.5);
                double Dz = Next.getZ() + 0.5 - (Centre.getZ() + 0.5);
                if (Dx * Dx + Dz * Dz > Square) {
                    continue;
                }
                Steps.put(Next.asLong(), Step + 1);
                Queue.add(Next);
            }
        }
        return new RaidFlood(Steps, Entrance, Capped);
    }
}
