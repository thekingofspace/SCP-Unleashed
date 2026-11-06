package net.rai.scp_unleashed.containmentzone.shutter.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.containmentzone.CellShutterBlock;
import net.rai.scp_unleashed.containmentzone.ContainmentZoneRegistry;
import net.rai.scp_unleashed.containmentzone.shutter.ShutterAssembly;
import net.rai.scp_unleashed.containmentzone.shutter.ShutterBlockEntity;
import net.rai.scp_unleashed.containmentzone.shutter.ShutterShapes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class ShutterCrashGameTests {
    public static final String Arena = ShutterGameTests.Arena;
    public static final int Ground = 2;
    public static final int Wall = 10;
    public static final int Edge = 6;
    public static final int Timeout = 400;
    public static final int Samples = 6000;
    public static final int[][] StalledPlaces = {{2, 1}, {3, 1}, {3, 2}, {4, 1}, {4, 2}, {4, 3}, {5, 1}, {5, 2}};
    public static final int[] StalledProgress = {0x3ddf05f0, 0x3e03cff5, 0x39818ac3, 0x3e634c3a, 0x3e7b3d52, 0x3e91dd62, 0x3e442354, 0x3e8c947d};

    private ShutterCrashGameTests() {
    }

    private static List<BlockPos> build(GameTestHelper Helper, int Width, int Height) {
        ServerLevel World = Helper.getLevel();
        for (int X = Edge - 2; X <= Edge + Width + 1; X++) {
            for (int Z = Wall - 3; Z <= Wall + 3; Z++) {
                Helper.setBlock(new BlockPos(X, Ground - 1, Z), Blocks.STONE);
                for (int Y = Ground; Y <= Ground + Height + 1; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.AIR);
                }
            }
        }
        List<BlockPos> Cells = new ArrayList<>();
        for (int Column = 0; Column < Width; Column++) {
            for (int Row = 0; Row < Height; Row++) {
                Cells.add(Helper.absolutePos(new BlockPos(Edge + Column, Ground + Row, Wall)));
            }
        }
        place(World, Cells);
        return Cells;
    }

    private static void place(ServerLevel World, List<BlockPos> Cells) {
        BlockState Shutter = ContainmentZoneRegistry.LockdownShutter.get().defaultBlockState().setValue(CellShutterBlock.FACING, Direction.NORTH);
        for (BlockPos Cell : Cells) {
            World.setBlock(Cell, Shutter, Block.UPDATE_ALL);
        }
        for (BlockPos Cell : Cells) {
            BlockState Current = World.getBlockState(Cell);
            BlockState Settled = Block.updateFromNeighbourShapes(Current, World, Cell);
            if (Settled != Current) {
                World.setBlock(Cell, Settled, Block.UPDATE_CLIENTS);
            }
        }
    }

    private static boolean legacyStalls(int Height, int Row, float Progress) {
        float Bar = ShutterShapes.barBottom(Height, Progress);
        float Ceiling = ShutterShapes.curtainTop(Height);
        float Floor = 16.0f * Row;
        float Roof = Floor + 16.0f;
        if (Bar >= Ceiling || Roof <= Bar || Floor >= Ceiling) {
            return false;
        }
        float SlatHigh = Math.min(Roof, Ceiling);
        float Cursor = Math.max(Floor, Bar + ShutterShapes.BarHeight);
        for (int Step = 0; Step < 64; Step++) {
            if (!(Cursor < SlatHigh - 1.0E-4f)) {
                return false;
            }
            float Band = (float) Math.floor((Cursor - Bar) / 16.0f);
            Cursor = Math.min(SlatHigh, Bar + 16.0f * (Band + 1.0f));
        }
        return true;
    }

    private static void checkBands(GameTestHelper Helper, int Height, int Row, float Progress) {
        ShutterShapes.Column Place = new ShutterShapes.Column(Row, Height);
        List<ShutterShapes.Band> Bands = ShutterShapes.slats(Place, Progress);
        float Bar = ShutterShapes.barBottom(Height, Progress);
        float Ceiling = ShutterShapes.curtainTop(Height);
        float Floor = 16.0f * Row;
        float Roof = Floor + 16.0f;
        String Where = " (height " + Height + ", row " + Row + ", progress " + Progress + ")";
        if (!(Bar < Ceiling) || Roof <= Bar || Floor >= Ceiling) {
            Helper.assertTrue(Bands.isEmpty(), "slat bands drawn outside the curtain" + Where);
            return;
        }
        float Low = Math.max(Floor, Bar + ShutterShapes.BarHeight) - Floor;
        float High = Math.min(Roof, Ceiling) - Floor;
        if (High - Low <= ShutterShapes.BandEpsilon) {
            Helper.assertTrue(Bands.isEmpty(), "slat bands drawn in an empty span" + Where);
            return;
        }
        Helper.assertTrue(!Bands.isEmpty() && Bands.size() <= 2, "a cell drew " + Bands.size() + " slat bands" + Where);
        Helper.assertTrue(Math.abs(Bands.get(0).Low() - Low) < 1.0e-3f, "the slats do not start at the bar" + Where);
        Helper.assertTrue(Math.abs(Bands.get(Bands.size() - 1).High() - High) < 1.0e-3f, "the slats do not reach the housing" + Where);
        for (int Index = 0; Index < Bands.size(); Index++) {
            ShutterShapes.Band Slat = Bands.get(Index);
            Helper.assertTrue(Slat.High() > Slat.Low(), "an empty slat band" + Where);
            Helper.assertTrue(Slat.VTop() >= 0.0f && Slat.VTop() <= 16.0f && Slat.VBottom() >= 0.0f && Slat.VBottom() <= 16.0f, "slat UVs leave the texture" + Where);
            if (Index > 0) {
                Helper.assertTrue(Math.abs(Bands.get(Index - 1).High() - Slat.Low()) < 1.0e-3f, "a gap between slat bands" + Where);
            }
        }
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "shutter_slatBands")
    public static void slatBands(GameTestHelper Helper) {
        for (int Index = 0; Index < StalledProgress.length; Index++) {
            int Height = StalledPlaces[Index][0];
            int Row = StalledPlaces[Index][1];
            float Progress = Float.intBitsToFloat(StalledProgress[Index]);
            Helper.assertTrue(legacyStalls(Height, Row, Progress), "the recorded input no longer stalls the old slat loop (height " + Height + ", row " + Row + ")");
            checkBands(Helper, Height, Row, Progress);
        }
        RandomSource Random = RandomSource.create(860L);
        for (int Height = 1; Height <= 8; Height++) {
            for (int Row = 0; Row < Height; Row++) {
                for (int Sample = 0; Sample < Samples; Sample++) {
                    float Previous = Random.nextFloat();
                    float Current = Mth.approach(Previous, Random.nextBoolean() ? 1.0f : 0.0f, 1.0f / ShutterBlockEntity.TravelTicks);
                    checkBands(Helper, Height, Row, Mth.lerp(Random.nextFloat(), Previous, Current));
                }
                checkBands(Helper, Height, Row, 0.0f);
                checkBands(Helper, Height, Row, 1.0f);
                checkBands(Helper, Height, Row, Float.NaN);
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "shutter_redstoneStorm")
    public static void redstoneStorm(GameTestHelper Helper) {
        List<BlockPos> Cells = build(Helper, 4, 3);
        BlockPos Behind = Cells.get(5).south();
        AtomicInteger Flips = new AtomicInteger();
        Helper.onEachTick(() -> {
            if (Helper.getTick() > 5 && Helper.getTick() < 125) {
                boolean On = Flips.incrementAndGet() % 3 != 0;
                Helper.getLevel().setBlock(Behind, On ? Blocks.REDSTONE_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
        Helper.startSequence()
                .thenIdle(130)
                .thenExecute(() -> Helper.getLevel().setBlock(Behind, Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL))
                .thenWaitUntil(() -> {
                    for (BlockPos Cell : Cells) {
                        ShutterBlockEntity Shutter = ShutterAssembly.entity(Helper.getLevel(), Cell);
                        Helper.assertTrue(Shutter != null && Shutter.getProgress() >= 1.0f, "the stormed shutter never settled open at " + Helper.relativePos(Cell));
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "shutter_breakWhileMoving")
    public static void breakWhileMoving(GameTestHelper Helper) {
        List<BlockPos> Cells = build(Helper, 3, 3);
        BlockPos Behind = Cells.get(8).south();
        BlockPos Middle = Cells.get(4);
        BlockPos Anchor = ShutterAssembly.anchor(Cells);
        Helper.startSequence()
                .thenExecuteAfter(5, () -> Helper.getLevel().setBlock(Behind, Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL))
                .thenExecuteAfter(ShutterBlockEntity.TravelTicks / 2, () -> {
                    Helper.getLevel().destroyBlock(Middle, true);
                    Helper.getLevel().destroyBlock(Anchor, true);
                })
                .thenExecuteAfter(4, () -> Helper.getLevel().setBlock(Behind, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL))
                .thenExecuteAfter(ShutterBlockEntity.TravelTicks / 2, () -> {
                    place(Helper.getLevel(), List.of(Middle));
                    Helper.getLevel().setBlock(Behind, Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                })
                .thenWaitUntil(() -> {
                    for (BlockPos Cell : Cells) {
                        if (Cell.equals(Anchor)) {
                            continue;
                        }
                        BlockState State = Helper.getLevel().getBlockState(Cell);
                        Helper.assertTrue(State.getBlock() instanceof CellShutterBlock && State.getValue(CellShutterBlock.Open), "a shutter part never reopened at " + Helper.relativePos(Cell));
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "shutter_explodedWhileMoving")
    public static void explodedWhileMoving(GameTestHelper Helper) {
        List<BlockPos> Cells = build(Helper, 3, 2);
        BlockPos Behind = Cells.get(0).south();
        BlockPos Front = Cells.get(2).north(2);
        Helper.startSequence()
                .thenExecuteAfter(5, () -> Helper.getLevel().setBlock(Behind, Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL))
                .thenExecuteAfter(ShutterBlockEntity.TravelTicks / 2, () -> Helper.getLevel().explode(null, Front.getX() + 0.5, Front.getY() + 0.5, Front.getZ() + 0.5, 3.0f,
                        Level.ExplosionInteraction.BLOCK))
                .thenIdle(ShutterBlockEntity.TravelTicks + 10)
                .thenExecute(() -> {
                    for (BlockPos Cell : Cells) {
                        BlockState State = Helper.getLevel().getBlockState(Cell);
                        if (State.getBlock() instanceof CellShutterBlock) {
                            Helper.assertTrue(ShutterAssembly.entity(Helper.getLevel(), Cell) != null, "a surviving shutter lost its block entity at " + Helper.relativePos(Cell));
                        }
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "shutter_pistonShoves")
    public static void pistonShoves(GameTestHelper Helper) {
        List<BlockPos> Cells = build(Helper, 2, 2);
        BlockPos Piston = Cells.get(0).north(2);
        BlockPos Trigger = Piston.north();
        Helper.getLevel().setBlock(Piston, Blocks.STICKY_PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        AtomicInteger Pulses = new AtomicInteger();
        Helper.onEachTick(() -> {
            if (Helper.getTick() > 5 && Helper.getTick() < 120 && Helper.getTick() % 4 == 0) {
                boolean On = Pulses.incrementAndGet() % 2 == 1;
                Helper.getLevel().setBlock(Trigger, On ? Blocks.REDSTONE_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
        Helper.startSequence()
                .thenIdle(130)
                .thenExecute(() -> {
                    for (BlockPos Cell : Cells) {
                        Helper.assertTrue(Helper.getLevel().getBlockState(Cell).getBlock() instanceof CellShutterBlock, "a piston moved a shutter part at " + Helper.relativePos(Cell));
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "shutter_worldCeiling")
    public static void worldCeiling(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Base = Helper.absolutePos(new BlockPos(Edge, Ground, Wall));
        int Top = World.getMaxBuildHeight() - 1;
        List<BlockPos> Cells = List.of(new BlockPos(Base.getX(), Top - 1, Base.getZ()), new BlockPos(Base.getX(), Top, Base.getZ()),
                new BlockPos(Base.getX() + 1, Top - 1, Base.getZ()), new BlockPos(Base.getX() + 1, Top, Base.getZ()));
        BlockPos Behind = Cells.get(0).south();
        List<BlockPos> Bottom = List.of(new BlockPos(Base.getX(), World.getMinBuildHeight(), Base.getZ()), new BlockPos(Base.getX(), World.getMinBuildHeight() + 1, Base.getZ()));
        List<BlockPos> Replaced = new ArrayList<>(Cells);
        Replaced.addAll(Bottom);
        Replaced.add(Behind);
        List<BlockState> Before = new ArrayList<>();
        for (BlockPos Cell : Replaced) {
            Before.add(World.getBlockState(Cell));
        }
        Runnable Restore = () -> {
            for (int Index = 0; Index < Replaced.size(); Index++) {
                World.setBlock(Replaced.get(Index), Before.get(Index), Block.UPDATE_ALL);
            }
        };
        place(World, Cells);
        place(World, Bottom);
        Helper.startSequence()
                .thenExecuteAfter(5, () -> World.setBlock(Behind, Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL))
                .thenWaitUntil(() -> {
                    for (BlockPos Cell : Cells) {
                        ShutterBlockEntity Shutter = ShutterAssembly.entity(World, Cell);
                        Helper.assertTrue(Shutter != null && Shutter.getProgress() >= 1.0f, "the shutter at the build limit never opened at y " + Cell.getY());
                    }
                })
                .thenExecute(() -> World.setBlock(Behind, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL))
                .thenWaitUntil(() -> {
                    for (BlockPos Cell : Cells) {
                        ShutterBlockEntity Shutter = ShutterAssembly.entity(World, Cell);
                        Helper.assertTrue(Shutter != null && Shutter.getProgress() <= 0.0f, "the shutter at the build limit never closed at y " + Cell.getY());
                    }
                    for (BlockPos Cell : Bottom) {
                        Helper.assertTrue(World.getBlockState(Cell).getBlock() instanceof CellShutterBlock, "the shutter at the bottom of the world vanished");
                    }
                })
                .thenExecute(Restore)
                .thenSucceed();
    }
}
