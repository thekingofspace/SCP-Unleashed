package net.rai.scp_unleashed.scp5001.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001DeviceBlock;

import java.util.ArrayList;
import java.util.List;

public final class Scp5001Arena {
    public record Layout(BlockPos Core, List<BlockPos> Rods, List<BlockPos> Lasers, List<BlockPos> Generators, List<BlockPos> Stations, BlockPos Console) {
    }

    private Scp5001Arena() {
    }

    public static Direction toward(BlockPos From, BlockPos To) {
        int Dx = To.getX() - From.getX();
        int Dz = To.getZ() - From.getZ();
        if (Math.abs(Dx) >= Math.abs(Dz)) {
            return Dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return Dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    public static Layout build(ServerLevel World, BlockPos Core, int Radius) {
        List<BlockPos> Rods = new ArrayList<>();
        List<BlockPos> Lasers = new ArrayList<>();
        List<BlockPos> Generators = new ArrayList<>();
        List<BlockPos> Stations = new ArrayList<>();
        place(World, Core, Scp5001EventRegistry.ChamberCore.get().defaultBlockState(), Scp5001EventData.Kind.Core);
        for (int Index = 0; Index < 8; Index++) {
            float Angle = Index * Mth.TWO_PI / 8.0f;
            int Height = Index % 2 == 0 ? 6 : 11;
            BlockPos At = Core.offset(Math.round(Mth.cos(Angle) * Radius), Height, Math.round(Mth.sin(Angle) * Radius));
            place(World, At, Scp5001EventRegistry.RodEmitter.get().defaultBlockState().setValue(Scp5001DeviceBlock.Facing, toward(At, Core)), Scp5001EventData.Kind.Rod);
            Rods.add(At);
        }
        for (int Index = 0; Index < 6; Index++) {
            float Angle = Index * Mth.TWO_PI / 6.0f + 0.4f;
            BlockPos At = Core.offset(Math.round(Mth.cos(Angle) * Radius), 17, Math.round(Mth.sin(Angle) * Radius));
            place(World, At, Scp5001EventRegistry.LaserEmitter.get().defaultBlockState().setValue(Scp5001DeviceBlock.Facing, toward(At, Core)), Scp5001EventData.Kind.Laser);
            Lasers.add(At);
        }
        int Corner = Math.max(5, Radius - 3);
        for (int Index = 0; Index < 4; Index++) {
            int Sx = Index == 0 || Index == 3 ? -1 : 1;
            int Sz = Index < 2 ? -1 : 1;
            BlockPos At = Core.offset(Sx * Corner, 0, Sz * Corner);
            place(World, At, Scp5001EventRegistry.Generator.get().defaultBlockState(), Scp5001EventData.Kind.Generator);
            Generators.add(At);
            BlockPos Station = At.offset(-Sx * 3, 0, 0);
            World.setBlock(Station, Scp5001EventRegistry.GeneratorConsole.get().defaultBlockState().setValue(Scp5001DeviceBlock.Facing, Sx < 0 ? Direction.WEST : Direction.EAST),
                    Block.UPDATE_ALL);
            Stations.add(Station);
        }
        BlockPos Console = Core.offset(0, 0, -Corner);
        World.setBlock(Console, Scp5001EventRegistry.ConsoleMain.get().defaultBlockState().setValue(Scp5001DeviceBlock.Facing, Direction.NORTH), Block.UPDATE_ALL);
        Scp5001State.get(World.getServer()).core(Core);
        return new Layout(Core, Rods, Lasers, Generators, Stations, Console);
    }

    private static void place(ServerLevel World, BlockPos At, BlockState State, Scp5001EventData.Kind Kind) {
        World.setBlock(At, State, Block.UPDATE_ALL);
        Scp5001Event.registerDevice(World, At, Kind);
    }

    public static void clear(ServerLevel World, Layout Made) {
        List<BlockPos> All = new ArrayList<>();
        All.add(Made.Core());
        All.addAll(Made.Rods());
        All.addAll(Made.Lasers());
        All.addAll(Made.Generators());
        All.addAll(Made.Stations());
        All.add(Made.Console());
        for (BlockPos Each : All) {
            World.setBlock(Each, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
