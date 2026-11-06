package net.rai.scp_unleashed.door.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.door.DoorKind;
import net.rai.scp_unleashed.door.DoorRegistry;
import net.rai.scp_unleashed.door.PanelDoorBlock;

import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class DoorClearanceGameTests {
    public static final String Arena = "gun_perf_arena";
    public static final int Floor = 1;
    public static final int WallZ = 4;
    public static final int OpenTicks = 40;

    private DoorClearanceGameTests() {
    }

    private static BlockPos door(GameTestHelper Helper, PanelDoorBlock Frame, int X, boolean Opened) {
        ServerLevel World = Helper.getLevel();
        DoorKind Kind = Frame.getKind();
        for (int Dx = -Kind.getWidth(); Dx <= 1; Dx++) {
            for (int Dz = WallZ - 2; Dz <= WallZ + 2; Dz++) {
                Helper.setBlock(new BlockPos(X + Dx, Floor, Dz), Blocks.STONE);
                for (int Y = Floor + 1; Y <= Floor + Kind.getHeight() + 1; Y++) {
                    Helper.setBlock(new BlockPos(X + Dx, Y, Dz), Blocks.AIR);
                }
            }
        }
        BlockPos Base = Helper.absolutePos(new BlockPos(X, Floor + 1, WallZ));
        BlockState State = Frame.defaultBlockState().setValue(PanelDoorBlock.Facing, Direction.NORTH).setValue(PanelDoorBlock.Open, Opened);
        for (int Part = 0; Part < Kind.getCellCount(); Part++) {
            BlockPos Cell = PanelDoorBlock.cellPos(Base, Direction.NORTH, Kind.columnOf(Part), Kind.rowOf(Part));
            World.setBlock(Cell, State.setValue(Frame.partProperty(), Part), Block.UPDATE_ALL);
        }
        if (Opened) {
            Helper.setBlock(new BlockPos(X, Floor + 1 + Kind.getHeight(), WallZ), Blocks.REDSTONE_BLOCK);
        }
        return Base;
    }

    private static AABB walker(EntityType<?> Kind, BlockPos Base, PanelDoorBlock Frame) {
        BlockPos Last = PanelDoorBlock.cellPos(Base, Direction.NORTH, Frame.getKind().getWidth() - 1, 0);
        Vec3 Spot = Vec3.atBottomCenterOf(Base).add(Vec3.atBottomCenterOf(Last)).scale(0.5);
        return Kind.getDimensions().makeBoundingBox(Spot);
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "door_clearance")
    public static void openDoorsLetTallMobsThrough(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        List<PanelDoorBlock> Frames = List.of(DoorRegistry.SingleSlidingDoor.get(), DoorRegistry.RoomDoor.get());
        BlockPos[] Opened = new BlockPos[Frames.size()];
        BlockPos[] Closed = new BlockPos[Frames.size()];
        for (int Index = 0; Index < Frames.size(); Index++) {
            Opened[Index] = door(Helper, Frames.get(Index), 3 + Index * 10, true);
            Closed[Index] = door(Helper, Frames.get(Index), 8 + Index * 10, false);
        }
        Helper.runAfterDelay(OpenTicks, () -> {
            for (int Index = 0; Index < Frames.size(); Index++) {
                PanelDoorBlock Frame = Frames.get(Index);
                String Name = Frame.getKind().getSerializedName();
                for (EntityType<?> Kind : List.of(EntityType.ZOMBIE, EntityType.VILLAGER, EntityType.SKELETON)) {
                    AABB Through = walker(Kind, Opened[Index], Frame);
                    Helper.assertTrue(World.noBlockCollision(null, Through), "a " + EntityType.getKey(Kind).getPath() + " does not fit through an open " + Name);
                    AABB Blocked = walker(Kind, Closed[Index], Frame);
                    Helper.assertFalse(World.noBlockCollision(null, Blocked), "a " + EntityType.getKey(Kind).getPath() + " fits through a closed " + Name);
                }
            }
            Helper.succeed();
        });
    }
}
