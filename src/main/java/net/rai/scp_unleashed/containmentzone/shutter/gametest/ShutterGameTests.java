package net.rai.scp_unleashed.containmentzone.shutter.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.containmentzone.CellShutterBlock;
import net.rai.scp_unleashed.containmentzone.ContainmentZoneRegistry;
import net.rai.scp_unleashed.containmentzone.shutter.ShutterAssembly;
import net.rai.scp_unleashed.containmentzone.shutter.ShutterBlockEntity;
import net.rai.scp_unleashed.decor.PillarPart;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class ShutterGameTests {
    public static final String Arena = "dclassroles_arena";
    public static final int Ground = 2;
    public static final int Wall = 10;
    public static final int Edge = 6;
    public static final int Travel = ShutterBlockEntity.TravelTicks;
    public static final int Timeout = 400;

    private ShutterGameTests() {
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
        BlockState Shutter = ContainmentZoneRegistry.LockdownShutter.get().defaultBlockState().setValue(CellShutterBlock.FACING, Direction.NORTH);
        List<BlockPos> Cells = new ArrayList<>();
        for (int Column = 0; Column < Width; Column++) {
            for (int Row = 0; Row < Height; Row++) {
                BlockPos Cell = Helper.absolutePos(new BlockPos(Edge + Column, Ground + Row, Wall));
                World.setBlock(Cell, Shutter, Block.UPDATE_ALL);
                Cells.add(Cell);
            }
        }
        for (BlockPos Cell : Cells) {
            BlockState Current = World.getBlockState(Cell);
            BlockState Settled = Block.updateFromNeighbourShapes(Current, World, Cell);
            if (Settled != Current) {
                World.setBlock(Cell, Settled, Block.UPDATE_CLIENTS);
            }
        }
        return Cells;
    }

    private static ShutterBlockEntity entity(GameTestHelper Helper, BlockPos Cell) {
        ShutterBlockEntity Shutter = ShutterAssembly.entity(Helper.getLevel(), Cell);
        if (Shutter == null) {
            throw new IllegalStateException("no shutter block entity at " + Helper.relativePos(Cell));
        }
        return Shutter;
    }

    private static boolean isOpen(GameTestHelper Helper, BlockPos Cell) {
        BlockState State = Helper.getLevel().getBlockState(Cell);
        return State.getValue(CellShutterBlock.Open) && entity(Helper, Cell).getProgress() >= 1.0f;
    }

    private static boolean isClosed(GameTestHelper Helper, BlockPos Cell) {
        BlockState State = Helper.getLevel().getBlockState(Cell);
        return !State.getValue(CellShutterBlock.Open) && State.getValue(CellShutterBlock.Shut) && entity(Helper, Cell).getProgress() <= 0.0f;
    }

    private static String describe(GameTestHelper Helper, BlockPos Cell) {
        BlockState State = Helper.getLevel().getBlockState(Cell);
        ShutterBlockEntity Shutter = entity(Helper, Cell);
        return String.format(" (open %s, shut %s, progress %.3f, pending %s, lead %s, tick %d)", State.getValue(CellShutterBlock.Open), State.getValue(CellShutterBlock.Shut),
                Shutter.getProgress(), Shutter.isPending(), Shutter.isLead(), Helper.getTick());
    }

    private static void assertAllOpen(GameTestHelper Helper, List<BlockPos> Cells, String Name) {
        for (BlockPos Cell : Cells) {
            Helper.assertTrue(isOpen(Helper, Cell), "the " + Name + " shutter is not fully open at " + Helper.relativePos(Cell) + describe(Helper, Cell));
        }
    }

    private static void assertAllClosed(GameTestHelper Helper, List<BlockPos> Cells, String Name) {
        for (BlockPos Cell : Cells) {
            Helper.assertTrue(isClosed(Helper, Cell), "the " + Name + " shutter is not fully closed at " + Helper.relativePos(Cell) + describe(Helper, Cell));
        }
    }

    private static void assertInStep(GameTestHelper Helper, List<BlockPos> Cells, String Name) {
        float First = entity(Helper, Cells.get(0)).getProgress();
        for (BlockPos Cell : Cells) {
            Helper.assertTrue(Math.abs(entity(Helper, Cell).getProgress() - First) < 1.0e-4f, "the " + Name + " shutter moved out of step at " + Helper.relativePos(Cell));
        }
    }

    private static BlockPos behind(BlockPos Cell) {
        return Cell.south();
    }

    private static void power(GameTestHelper Helper, BlockPos Cell, boolean On) {
        Helper.getLevel().setBlock(behind(Cell), On ? Blocks.REDSTONE_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void cycle(GameTestHelper Helper, List<BlockPos> Cells, BlockPos Powered, String Name) {
        Helper.onEachTick(() -> assertInStep(Helper, Cells, Name));
        Helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    assertAllClosed(Helper, Cells, Name);
                    power(Helper, Powered, true);
                })
                .thenWaitUntil(() -> assertAllOpen(Helper, Cells, Name))
                .thenExecute(() -> {
                    Helper.assertTrue(ContainmentZoneRegistry.LockdownShutter.get().defaultBlockState().getValue(CellShutterBlock.Shut), "a fresh shutter is not closed");
                    Helper.assertTrue(Helper.getLevel().getBlockState(Powered).getAnalogOutputSignal(Helper.getLevel(), Powered) == 15, "an open shutter does not give a full comparator signal");
                    power(Helper, Powered, false);
                })
                .thenWaitUntil(() -> assertAllClosed(Helper, Cells, Name))
                .thenExecute(() -> Helper.assertTrue(Helper.getLevel().getBlockState(Powered).getAnalogOutputSignal(Helper.getLevel(), Powered) == 0,
                        "a closed shutter gives a comparator signal"))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "shutter_single")
    public static void singleShutterFollowsPower(GameTestHelper Helper) {
        List<BlockPos> Cells = build(Helper, 1, 1);
        cycle(Helper, Cells, Cells.get(0), "single");
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "shutter_stacked")
    public static void stackedShutterMovesTogether(GameTestHelper Helper) {
        List<BlockPos> Cells = build(Helper, 1, 3);
        Helper.assertTrue(Helper.getLevel().getBlockState(Cells.get(2)).getValue(CellShutterBlock.Part) == PillarPart.Top, "the stack has no housing on top");
        cycle(Helper, Cells, Cells.get(2), "stacked");
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "shutter_wide")
    public static void wideShutterMovesTogether(GameTestHelper Helper) {
        List<BlockPos> Cells = build(Helper, 3, 3);
        Helper.assertTrue(Helper.getLevel().getBlockState(Cells.get(4)).getValue(CellShutterBlock.Left), "the middle column did not join its neighbour");
        cycle(Helper, Cells, Cells.get(Cells.size() - 3), "wide");
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "shutter_inverted")
    public static void lockdownModeInvertsPower(GameTestHelper Helper) {
        List<BlockPos> Cells = build(Helper, 3, 3);
        ServerLevel World = Helper.getLevel();
        BlockPos Powered = Cells.get(0);
        Helper.onEachTick(() -> assertInStep(Helper, Cells, "lockdown"));
        Helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    assertAllClosed(Helper, Cells, "lockdown");
                    Helper.assertTrue(ShutterAssembly.toggleMode(World, Cells.get(8)), "the screwdriver toggle did not switch to lockdown");
                    for (BlockPos Cell : Cells) {
                        Helper.assertTrue(World.getBlockState(Cell).getValue(CellShutterBlock.Lockdown), "lockdown did not reach every part");
                    }
                })
                .thenWaitUntil(() -> assertAllOpen(Helper, Cells, "unpowered lockdown"))
                .thenExecute(() -> power(Helper, Powered, true))
                .thenWaitUntil(() -> assertAllClosed(Helper, Cells, "powered lockdown"))
                .thenExecute(() -> power(Helper, Powered, false))
                .thenWaitUntil(() -> assertAllOpen(Helper, Cells, "released lockdown"))
                .thenExecute(() -> Helper.assertFalse(ShutterAssembly.toggleMode(World, Cells.get(3)), "the toggle did not switch back"))
                .thenWaitUntil(() -> assertAllClosed(Helper, Cells, "normal again"))
                .thenSucceed();
    }

    private static AABB walker(GameTestHelper Helper, int Column, double Depth) {
        Vec3 Spot = Helper.absoluteVec(new Vec3(Edge + Column + 0.5, Ground, Wall + Depth));
        return EntityType.PLAYER.getDimensions().makeBoundingBox(Spot);
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "shutter_collision")
    public static void collisionFollowsTheCurtain(GameTestHelper Helper) {
        List<BlockPos> Cells = build(Helper, 3, 3);
        ServerLevel World = Helper.getLevel();
        Helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    for (int Column = 0; Column < 3; Column++) {
                        Helper.assertFalse(World.noBlockCollision(null, walker(Helper, Column, 0.5)), "a closed shutter lets a player through column " + Column);
                    }
                    power(Helper, Cells.get(1), true);
                })
                .thenWaitUntil(() -> assertAllOpen(Helper, Cells, "collision"))
                .thenExecute(() -> {
                    for (int Column = 0; Column < 3; Column++) {
                        Helper.assertTrue(World.noBlockCollision(null, walker(Helper, Column, 0.5)), "an open shutter blocks a player in column " + Column);
                    }
                    AABB Head = new AABB(Helper.absoluteVec(new Vec3(Edge + 0.2, Ground + 2.7, Wall + 0.3)), Helper.absoluteVec(new Vec3(Edge + 2.8, Ground + 2.95, Wall + 0.7)));
                    Helper.assertFalse(World.noBlockCollision(null, Head), "the open shutter lost its housing");
                })
                .thenSucceed();
    }

    private static Arrow arrow(GameTestHelper Helper, int Column) {
        ServerLevel World = Helper.getLevel();
        Vec3 Start = Helper.absoluteVec(new Vec3(Edge + Column + 0.5, Ground + 1.0, Wall - 2.5));
        Arrow Shot = new Arrow(World, Start.x, Start.y, Start.z, new ItemStack(Items.ARROW), null);
        Shot.pickup = AbstractArrow.Pickup.DISALLOWED;
        Shot.setDeltaMovement(0.0, 0.0, 1.2);
        World.addFreshEntity(Shot);
        return Shot;
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "shutter_projectile")
    public static void closedShutterStopsArrows(GameTestHelper Helper) {
        List<BlockPos> Cells = build(Helper, 3, 3);
        double Plane = Helper.absoluteVec(new Vec3(0.0, 0.0, Wall + 0.5)).z;
        List<Arrow> Shots = new ArrayList<>();
        Helper.startSequence()
                .thenExecuteAfter(5, () -> Shots.add(arrow(Helper, 1)))
                .thenExecuteAfter(20, () -> {
                    Helper.assertTrue(Shots.get(0).getZ() < Plane, "an arrow went through the closed shutter");
                    Shots.get(0).discard();
                    power(Helper, Cells.get(1), true);
                })
                .thenWaitUntil(() -> assertAllOpen(Helper, Cells, "projectile"))
                .thenExecute(() -> Shots.add(arrow(Helper, 1)))
                .thenExecuteAfter(20, () -> {
                    Helper.assertTrue(Shots.get(1).getZ() > Plane + 0.5, "an arrow could not pass the open shutter");
                    Shots.get(1).discard();
                })
                .thenSucceed();
    }

    private static Zombie stand(GameTestHelper Helper, int Column) {
        Zombie Body = Helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(Edge + Column + 0.5, Ground, Wall + 0.5));
        Body.setPersistenceRequired();
        return Body;
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "shutter_entitysafe")
    public static void closingShutterSparesWhatIsUnderIt(GameTestHelper Helper) {
        List<BlockPos> Cells = build(Helper, 1, 3);
        ServerLevel World = Helper.getLevel();
        BlockPos Bottom = Cells.get(0);
        List<Zombie> Bodies = new ArrayList<>();
        Helper.onEachTick(() -> {
            for (Zombie Body : Bodies) {
                if (!Body.isRemoved()) {
                    Helper.assertTrue(Body.getHealth() >= Body.getMaxHealth(), "the shutter hurt what stood under it");
                    Helper.assertTrue(World.noBlockCollision(Body, Body.getBoundingBox().deflate(1.0e-3)), "the shutter closed onto what stood under it");
                }
            }
        });
        Helper.startSequence()
                .thenExecuteAfter(5, () -> power(Helper, Bottom, true))
                .thenWaitUntil(() -> assertAllOpen(Helper, Cells, "safety"))
                .thenExecute(() -> {
                    Bodies.add(stand(Helper, 0));
                    power(Helper, Bottom, false);
                })
                .thenExecuteFor(60, () -> assertAllOpen(Helper, Cells, "occupied"))
                .thenExecute(() -> Bodies.get(0).discard())
                .thenWaitUntil(() -> {
                    float Progress = entity(Helper, Bottom).getProgress();
                    Helper.assertTrue(!World.getBlockState(Bottom).getValue(CellShutterBlock.Open) && Progress < 0.9f, "the empty shutter never started closing");
                })
                .thenExecute(() -> Bodies.add(stand(Helper, 0)))
                .thenWaitUntil(() -> Helper.assertTrue(World.getBlockState(Bottom).getValue(CellShutterBlock.Open), "the closing shutter did not back off"))
                .thenWaitUntil(() -> assertAllOpen(Helper, Cells, "reversed"))
                .thenExecuteFor(20, () -> assertAllOpen(Helper, Cells, "still occupied"))
                .thenExecute(() -> Bodies.get(1).discard())
                .thenWaitUntil(() -> assertAllClosed(Helper, Cells, "cleared"))
                .thenSucceed();
    }

    private static void reload(GameTestHelper Helper, List<BlockPos> Cells) {
        ServerLevel World = Helper.getLevel();
        for (BlockPos Cell : Cells) {
            ShutterBlockEntity Old = entity(Helper, Cell);
            CompoundTag Saved = Old.saveWithFullMetadata(World.registryAccess());
            float Before = Old.getProgress();
            World.removeBlockEntity(Cell);
            BlockEntity Loaded = BlockEntity.loadStatic(Cell, World.getBlockState(Cell), Saved, World.registryAccess());
            Helper.assertTrue(Loaded instanceof ShutterBlockEntity, "the shutter did not load back");
            World.setBlockEntity(Loaded);
            Helper.assertTrue(Math.abs(entity(Helper, Cell).getProgress() - Before) < 1.0e-6f, "the shutter lost its position on load");
        }
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "shutter_persist")
    public static void shutterStateSurvivesReload(GameTestHelper Helper) {
        List<BlockPos> Cells = build(Helper, 2, 2);
        ServerLevel World = Helper.getLevel();
        BlockPos Powered = Cells.get(0);
        Helper.startSequence()
                .thenExecuteAfter(5, () -> power(Helper, Powered, true))
                .thenExecuteAfter(Travel / 2, () -> {
                    float Middle = entity(Helper, Powered).getProgress();
                    Helper.assertTrue(Middle > 0.2f && Middle < 0.8f, "the shutter was not mid travel, at " + Middle);
                    reload(Helper, Cells);
                })
                .thenWaitUntil(() -> assertAllOpen(Helper, Cells, "reloaded"))
                .thenExecute(() -> reload(Helper, Cells))
                .thenExecuteFor(20, () -> assertAllOpen(Helper, Cells, "reloaded open"))
                .thenExecute(() -> {
                    power(Helper, Powered, false);
                    ShutterAssembly.toggleMode(World, Powered);
                })
                .thenExecuteAfter(5, () -> reload(Helper, Cells))
                .thenExecuteFor(Travel + 10, () -> assertAllOpen(Helper, Cells, "lockdown kept open"))
                .thenExecute(() -> {
                    for (BlockPos Cell : Cells) {
                        Helper.assertTrue(World.getBlockState(Cell).getValue(CellShutterBlock.Lockdown), "lockdown mode was lost");
                    }
                    ShutterAssembly.toggleMode(World, Powered);
                })
                .thenWaitUntil(() -> assertAllClosed(Helper, Cells, "closed after reload"))
                .thenExecute(() -> {
                    for (BlockPos Cell : Cells) {
                        World.removeBlockEntity(Cell);
                    }
                    Helper.getLevel().setBlock(behind(Powered), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                })
                .thenExecuteFor(Travel, () -> {
                    for (BlockPos Cell : Cells) {
                        BlockState State = World.getBlockState(Cell);
                        Helper.assertTrue(!State.getValue(CellShutterBlock.Open) && State.getValue(CellShutterBlock.Shut), "a shutter without saved data opened by itself");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "shutter_pulse")
    public static void shortPulseRunsOneCleanCycle(GameTestHelper Helper) {
        List<BlockPos> Cells = build(Helper, 3, 1);
        ServerLevel World = Helper.getLevel();
        BlockPos Powered = Cells.get(2);
        int[] Flips = {0};
        boolean[] Last = {false};
        float[] Peak = {0.0f};
        boolean[] Started = {false};
        Helper.onEachTick(() -> {
            if (!Started[0]) {
                return;
            }
            boolean Now = World.getBlockState(Cells.get(0)).getValue(CellShutterBlock.Open);
            Peak[0] = Math.max(Peak[0], entity(Helper, Cells.get(0)).getProgress());
            if (Now != Last[0]) {
                Flips[0]++;
                Last[0] = Now;
                if (!Now) {
                    Helper.assertTrue(Peak[0] >= 1.0f, "the shutter turned back before it finished opening, at " + Peak[0]);
                }
            }
        });
        Helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    Started[0] = true;
                    power(Helper, Powered, true);
                })
                .thenExecuteAfter(2, () -> power(Helper, Powered, false))
                .thenWaitUntil(() -> Helper.assertTrue(Flips[0] == 2 && isClosed(Helper, Cells.get(0)), "the pulse has not finished one cycle, flips " + Flips[0]))
                .thenExecuteFor(Travel, () -> Helper.assertTrue(Flips[0] == 2, "the shutter flickered after a short pulse, flips " + Flips[0]))
                .thenExecute(() -> assertAllClosed(Helper, Cells, "pulsed"))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "shutter_tab")
    public static void shutterSitsInTheFacilityTab(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        CreativeModeTab.ItemDisplayParameters Parameters = new CreativeModeTab.ItemDisplayParameters(World.enabledFeatures(), true, World.registryAccess());
        CreativeModeTab Facility = ModCreativeTabs.Facility.get();
        CreativeModeTab Decoration = ModCreativeTabs.Decoration.get();
        Facility.buildContents(Parameters);
        Decoration.buildContents(Parameters);
        ItemStack Shutter = new ItemStack(ContainmentZoneRegistry.LockdownShutter.get());
        Helper.assertTrue(Facility.getDisplayItems().stream().anyMatch(Stack -> ItemStack.isSameItem(Stack, Shutter)), "the shutter is missing from the Facility tab");
        Helper.assertFalse(Decoration.getDisplayItems().stream().anyMatch(Stack -> ItemStack.isSameItem(Stack, Shutter)), "the shutter is still in the Decoration tab");
        Helper.assertTrue(Decoration.getDisplayItems().stream().anyMatch(Stack -> ItemStack.isSameItem(Stack, new ItemStack(ContainmentZoneRegistry.PaddedPanel.get()))),
                "the other containment pieces left the Decoration tab");
        Helper.succeed();
    }
}
