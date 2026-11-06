package net.rai.scp_unleashed.refinery.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.refinery.OrePurifierBlock;
import net.rai.scp_unleashed.refinery.PurifierLayout;
import net.rai.scp_unleashed.refinery.RefineryRegistry;

import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class OrePurifierGameTests {
    public static final String Arena = "dclassroles_arena";
    public static final int Floor = 1;
    public static final int Ceiling = 7;
    public static final BlockPos Origin = new BlockPos(11, 2, 11);
    public static final Vec3 North = new Vec3(11.9, 2.0, 5.5);
    public static final Vec3 South = new Vec3(12.1, 2.0, 18.5);
    public static final double Arrived = 2.5;
    public static final int Walk = 360;

    private OrePurifierGameTests() {
    }

    private static List<BlockPos> build(GameTestHelper Helper, Direction Front) {
        for (int X = 1; X <= 22; X++) {
            for (int Z = 1; Z <= 22; Z++) {
                Helper.setBlock(new BlockPos(X, Floor, Z), Blocks.STONE);
                for (int Y = Floor + 1; Y <= Ceiling; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.AIR);
                }
            }
        }
        ServerLevel World = Helper.getLevel();
        BlockPos Master = Helper.absolutePos(Origin);
        BlockState State = RefineryRegistry.OrePurifier.get().defaultBlockState().setValue(OrePurifierBlock.Facing, Front).setValue(OrePurifierBlock.Part, PurifierLayout.MasterPart);
        World.setBlock(Master, State, Block.UPDATE_ALL);
        State.getBlock().setPlacedBy(World, Master, State, null, new ItemStack(RefineryRegistry.OrePurifierItem.get()));
        List<BlockPos> Cells = PurifierLayout.cells(Master, Front);
        for (BlockPos Cell : Cells) {
            Helper.assertTrue(World.getBlockState(Cell).is(RefineryRegistry.OrePurifier.get()), "the purifier did not assemble at " + Helper.relativePos(Cell));
        }
        return Cells;
    }

    private static <T extends Mob> T walker(GameTestHelper Helper, EntityType<T> Type, Vec3 Spot) {
        T Walker = Helper.spawn(Type, Spot);
        Walker.goalSelector.removeAllGoals(Each -> true);
        Walker.targetSelector.removeAllGoals(Each -> true);
        Walker.setPersistenceRequired();
        return Walker;
    }

    private static AABB footprint(List<BlockPos> Cells) {
        AABB Box = new AABB(Cells.get(0));
        for (BlockPos Cell : Cells) {
            Box = Box.minmax(new AABB(Cell));
        }
        return Box;
    }

    private static void walkTo(GameTestHelper Helper, Mob Walker, Vec3 Goal) {
        Vec3 Target = Helper.absoluteVec(Goal);
        Helper.onEachTick(() -> {
            if (Walker.isAlive() && Helper.getTick() % 10 == 0 && Walker.getNavigation().isDone()) {
                Walker.getNavigation().moveTo(Target.x, Target.y, Target.z, 1.0);
            }
        });
    }

    private static String describe(GameTestHelper Helper, Mob Walker) {
        return " (at " + Helper.relativeVec(Walker.position()) + ", tick " + Helper.getTick() + ", path done " + Walker.getNavigation().isDone() + ")";
    }

    @GameTest(template = Arena, timeoutTicks = Walk, batch = "orepurifier_pathsAround")
    public static void pathsAround(GameTestHelper Helper) {
        List<BlockPos> Cells = build(Helper, Direction.NORTH);
        IronGolem Golem = walker(Helper, EntityType.IRON_GOLEM, North);
        Vec3 Target = Helper.absoluteVec(South);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Golem.onGround(), "the golem never landed"))
                .thenExecute(() -> {
                    Path Route = Golem.getNavigation().createPath(BlockPos.containing(Target), 0);
                    Helper.assertTrue(Route != null, "no path around the purifier");
                    for (int Index = 0; Index < Route.getNodeCount(); Index++) {
                        Node Step = Route.getNode(Index);
                        BlockPos At = Step.asBlockPos();
                        Helper.assertFalse(Cells.contains(At), "the path runs through the purifier at " + Helper.relativePos(At));
                    }
                    walkTo(Helper, Golem, South);
                })
                .thenWaitUntil(() -> Helper.assertTrue(Golem.position().distanceTo(Target) < Arrived, "the golem never got around the purifier" + describe(Helper, Golem)))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = Walk, batch = "orepurifier_offTheTop")
    public static void offTheTop(GameTestHelper Helper) {
        List<BlockPos> Cells = build(Helper, Direction.EAST);
        Vec3 Top = new Vec3(Origin.getX() + 0.2, Origin.getY() + 3.0, Origin.getZ() + 0.9);
        IronGolem Golem = walker(Helper, EntityType.IRON_GOLEM, Top);
        Husk Small = walker(Helper, EntityType.HUSK, Top.add(-0.9, 0.0, 0.6));
        walkTo(Helper, Golem, North);
        walkTo(Helper, Small, South);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Golem.position().distanceTo(Helper.absoluteVec(North)) < Arrived, "the golem did not get off the purifier" + describe(Helper, Golem));
            Helper.assertTrue(Small.position().distanceTo(Helper.absoluteVec(South)) < Arrived, "the husk did not get off the purifier" + describe(Helper, Small));
        });
    }

    @GameTest(template = Arena, timeoutTicks = Walk, batch = "orepurifier_besideNotStuck")
    public static void besideNotStuck(GameTestHelper Helper) {
        List<BlockPos> Cells = build(Helper, Direction.SOUTH);
        AABB Machine = footprint(Cells);
        Vec3 Beside = Helper.relativeVec(new Vec3(Machine.minX - 0.75, Machine.minY, Machine.getCenter().z));
        Vec3 Across = Helper.relativeVec(new Vec3(Machine.maxX + 2.5, Machine.minY, Machine.getCenter().z + 0.3));
        IronGolem Golem = walker(Helper, EntityType.IRON_GOLEM, Beside);
        walkTo(Helper, Golem, Across);
        Helper.succeedWhen(() -> Helper.assertTrue(Golem.position().distanceTo(Helper.absoluteVec(Across)) < Arrived, "the golem stuck beside the purifier" + describe(Helper, Golem)));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "orepurifier_hullShapes")
    public static void hullShapes(GameTestHelper Helper) {
        for (Direction Front : Direction.Plane.HORIZONTAL) {
            for (int Part = 0; Part < PurifierLayout.CellCount; Part++) {
                BlockState State = RefineryRegistry.OrePurifier.get().defaultBlockState().setValue(OrePurifierBlock.Facing, Front).setValue(OrePurifierBlock.Part, Part);
                VoxelShape Hull = State.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
                VoxelShape Outline = State.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
                Helper.assertTrue(Hull.toAabbs().size() == 1, "the purifier hull has gaps in part " + Part + " facing " + Front);
                AABB Box = Hull.bounds();
                Helper.assertTrue(Box.minX == 0.0 && Box.minZ == 0.0 && Box.maxX == 1.0 && Box.maxZ == 1.0 && Box.minY == 0.0, "the purifier hull does not fill part " + Part + " facing " + Front);
                Helper.assertFalse(Outline.isEmpty(), "the purifier lost its outline in part " + Part);
                Helper.assertFalse(State.isPathfindable(PathComputationType.LAND), "mobs may still path into purifier part " + Part);
            }
        }
        Helper.succeed();
    }
}
