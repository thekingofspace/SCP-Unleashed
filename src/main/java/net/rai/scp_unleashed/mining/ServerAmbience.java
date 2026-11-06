package net.rai.scp_unleashed.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.computer.ComputerDeviceBlock;

import java.util.HashMap;
import java.util.Map;

public final class ServerAmbience {
    public static final int MaxSeen = 256;
    public static final float BusyLoad = 1.0f;

    private static final Map<BlockPos, Sighting> Seen = new HashMap<>();

    private ServerAmbience() {
    }

    public static void idle(Level World, BlockPos Pos, RandomSource Random, float Load) {
        if (!World.isClientSide || Seen.size() >= MaxSeen && !Seen.containsKey(Pos)) {
            return;
        }
        Seen.put(Pos.immutable(), new Sighting(World.getBlockState(Pos).getBlock(), Load));
    }

    public static Map<BlockPos, Sighting> seen() {
        return Seen;
    }

    public static void forget() {
        Seen.clear();
    }

    public static Mode modeOf(BlockState State, Sighting Last) {
        if (State.getBlock() != Last.Kind()) {
            return Mode.Off;
        }
        if (State.hasProperty(PointMinerBlock.Status)) {
            return State.getValue(PointMinerBlock.Status) == MinerStatus.Running ? Mode.Busy : Mode.Off;
        }
        if (State.hasProperty(ComputerDeviceBlock.Powered)) {
            return State.getValue(ComputerDeviceBlock.Powered) ? Mode.Idle : Mode.Off;
        }
        return Mode.Off;
    }

    public static float loadOf(Mode Shown, Sighting Last) {
        return Shown == Mode.Busy ? BusyLoad : Last.Load();
    }

    public enum Mode {
        Off,
        Idle,
        Busy
    }

    public record Sighting(Block Kind, float Load) {
    }
}
