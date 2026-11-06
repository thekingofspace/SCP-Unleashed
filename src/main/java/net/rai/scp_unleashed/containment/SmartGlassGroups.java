package net.rai.scp_unleashed.containment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class SmartGlassGroups {
    public static final int GroupLimit = 512;
    private static final Map<ServerLevel, Set<BlockPos>> Pending = new WeakHashMap<>();

    private SmartGlassGroups() {
    }

    public static void markDirty(ServerLevel World, BlockPos Pos) {
        Pending.computeIfAbsent(World, Key -> new LinkedHashSet<>()).add(Pos.immutable());
    }

    public static boolean touchesActive(BlockGetter World, BlockPos Pos) {
        for (Direction Side : Direction.values()) {
            BlockState Neighbour = World.getBlockState(Pos.relative(Side));
            if (Neighbour.getBlock() instanceof SmartGlassBlock && Neighbour.getValue(SmartGlassBlock.Active)) {
                return true;
            }
        }
        return false;
    }

    public static void tickLevel(ServerLevel World) {
        Set<BlockPos> Dirty = Pending.remove(World);
        if (Dirty == null) {
            return;
        }
        Set<BlockPos> Settled = new HashSet<>();
        for (BlockPos Start : Dirty) {
            if (!Settled.contains(Start)) {
                settle(World, Start, Settled);
            }
        }
    }

    public static void forget(ServerLevel World) {
        Pending.remove(World);
    }

    private static boolean isGlass(ServerLevel World, BlockPos Pos) {
        return World.isLoaded(Pos) && World.getBlockState(Pos).getBlock() instanceof SmartGlassBlock;
    }

    private static void settle(ServerLevel World, BlockPos Start, Set<BlockPos> Settled) {
        if (!isGlass(World, Start)) {
            return;
        }
        BlockState StartState = World.getBlockState(Start);
        boolean Signal = World.hasNeighborSignal(Start);
        if (Signal != StartState.getValue(SmartGlassBlock.Powered)) {
            World.setBlock(Start, StartState.setValue(SmartGlassBlock.Powered, Signal), Block.UPDATE_CLIENTS);
        }
        List<BlockPos> Members = new ArrayList<>();
        Set<BlockPos> Seen = new HashSet<>();
        ArrayDeque<BlockPos> Queue = new ArrayDeque<>();
        Seen.add(Start);
        Queue.add(Start);
        boolean AnyPowered = false;
        while (!Queue.isEmpty() && Members.size() < GroupLimit) {
            BlockPos Current = Queue.poll();
            Members.add(Current);
            AnyPowered |= World.getBlockState(Current).getValue(SmartGlassBlock.Powered);
            for (Direction Side : Direction.values()) {
                BlockPos Next = Current.relative(Side);
                if (Seen.add(Next) && isGlass(World, Next)) {
                    Queue.add(Next);
                }
            }
        }
        for (BlockPos Member : Members) {
            BlockState State = World.getBlockState(Member);
            if (State.getValue(SmartGlassBlock.Active) != AnyPowered) {
                World.setBlock(Member, State.setValue(SmartGlassBlock.Active, AnyPowered), Block.UPDATE_CLIENTS);
            }
        }
        Settled.addAll(Members);
    }
}
