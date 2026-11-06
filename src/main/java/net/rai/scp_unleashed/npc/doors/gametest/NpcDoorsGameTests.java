package net.rai.scp_unleashed.npc.doors.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.door.DoorRegistry;
import net.rai.scp_unleashed.door.KeycardScannerBlock;
import net.rai.scp_unleashed.door.PanelDoorBlock;
import net.rai.scp_unleashed.npc.doors.NpcDoors;

import java.util.List;
import java.util.Optional;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class NpcDoorsGameTests {
    public static final String Arena = "npc_doors_arena";

    private NpcDoorsGameTests() {
    }

    private static int stand(GameTestHelper Helper) {
        for (int Y = 0; Y < 4; Y++) {
            if (!Helper.getBlockState(new BlockPos(1, Y, 1)).isAir() && Helper.getBlockState(new BlockPos(1, Y + 1, 1)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static void wall(GameTestHelper Helper, int Base) {
        for (int Z = 1; Z <= 9; Z++) {
            for (int Y = Base; Y < Base + 4; Y++) {
                Helper.setBlock(new BlockPos(6, Y, Z), Blocks.STONE_BRICKS);
            }
        }
    }

    private static BlockPos wideDoor(GameTestHelper Helper, int Base) {
        PanelDoorBlock Door = DoorRegistry.RoomDoor.get();
        BlockPos Origin = new BlockPos(6, Base, 5);
        for (int Part = 0; Part < Door.getKind().getCellCount(); Part++) {
            BlockPos Cell = PanelDoorBlock.cellPos(Origin, Direction.EAST, Door.getKind().columnOf(Part), Door.getKind().rowOf(Part));
            Helper.getLevel().setBlock(Helper.absolutePos(Cell), Door.defaultBlockState().setValue(PanelDoorBlock.Facing, Direction.EAST).setValue(PanelDoorBlock.Open, false)
                    .setValue(Door.partProperty(), Part), Block.UPDATE_CLIENTS);
        }
        return Helper.absolutePos(Origin);
    }

    private static boolean open(GameTestHelper Helper, BlockPos Relative) {
        BlockState State = Helper.getBlockState(Relative);
        return State.hasProperty(DoorBlock.OPEN) && State.getValue(DoorBlock.OPEN);
    }

    private static void opener(GameTestHelper Helper, BlockPos LeverAt, BlockState Lever, String Name) {
        int Base = stand(Helper);
        wall(Helper, Base);
        BlockPos Door = wideDoor(Helper, Base);
        Helper.setBlock(LeverAt, Lever);
        Villager User = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(4.5, Base, 4.5));
        Helper.runAfterDelay(2, () -> {
            Helper.assertTrue(NpcDoors.blocks(Helper.getLevel(), Door), Name + ": the closed wide door does not block");
            List<BlockPos> Cells = NpcDoors.extent(Helper.getLevel(), Door.above());
            Helper.assertTrue(Cells.size() == 4, Name + ": the wide door spans " + Cells.size() + " blocks");
            Optional<BlockPos> Found = NpcDoors.findOpener(Helper.getLevel(), Door.above());
            Helper.assertTrue(Found.isPresent() && Found.get().equals(Helper.absolutePos(LeverAt)), Name + ": the lever was not found, got " + Found);
            Helper.assertTrue(NpcDoors.useOpener(User, Found.get()), Name + ": the lever could not be used");
            Helper.assertTrue(Helper.getBlockState(LeverAt).getValue(LeverBlock.POWERED), Name + ": the lever did not flip");
        });
        Helper.runAfterDelay(6, () -> {
            Helper.assertTrue(open(Helper, new BlockPos(6, Base, 5)), Name + ": the lever did not open the door");
            Helper.assertTrue(!NpcDoors.blocks(Helper.getLevel(), Door), Name + ": an open door still blocks");
            Helper.assertTrue(!NpcDoors.useOpener(User, Helper.absolutePos(LeverAt)), Name + ": the lever was flipped back while the door is open");
            Helper.assertTrue(Helper.getBlockState(LeverAt).getValue(LeverBlock.POWERED), Name + ": the lever was toggled back");
            User.discard();
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "npc_doors_openerBeside")
    public static void openerBeside(GameTestHelper Helper) {
        int Base = stand(Helper);
        opener(Helper, new BlockPos(5, Base + 1, 3), Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, Direction.WEST), "beside");
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "npc_doors_openerAbove")
    public static void openerAbove(GameTestHelper Helper) {
        int Base = stand(Helper);
        opener(Helper, new BlockPos(5, Base + 2, 4), Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, Direction.WEST), "above");
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "npc_doors_openerBelow")
    public static void openerBelow(GameTestHelper Helper) {
        int Base = stand(Helper);
        opener(Helper, new BlockPos(5, Base, 5), Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.FACING, Direction.WEST), "below");
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "npc_doors_scannerCards")
    public static void scannerCards(GameTestHelper Helper) {
        int Base = stand(Helper);
        wall(Helper, Base);
        BlockState Door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST).setValue(DoorBlock.OPEN, false);
        Helper.setBlock(new BlockPos(6, Base, 5), Door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        Helper.setBlock(new BlockPos(6, Base + 1, 5), Door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        Helper.setBlock(new BlockPos(5, Base + 1, 4), DoorRegistry.KeycardScanner.get().defaultBlockState().setValue(KeycardScannerBlock.FACING, Direction.WEST)
                .setValue(KeycardScannerBlock.Clearance, 3));
        Villager User = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(4.5, Base, 4.5));
        BlockPos DoorAt = Helper.absolutePos(new BlockPos(6, Base, 5));
        BlockPos ScannerAt = Helper.absolutePos(new BlockPos(5, Base + 1, 4));
        ItemStack Low = new ItemStack(DoorRegistry.KeycardLevel1.get());
        ItemStack Two = new ItemStack(DoorRegistry.KeycardLevel2.get());
        ItemStack Three = new ItemStack(DoorRegistry.KeycardLevel3.get());
        ItemStack Five = new ItemStack(DoorRegistry.KeycardLevel5.get());
        Helper.runAfterDelay(2, () -> {
            Helper.assertTrue(NpcDoors.blocks(Helper.getLevel(), DoorAt) && NpcDoors.extent(Helper.getLevel(), DoorAt.above()).size() == 2, "the iron door is not read as a 2 block door");
            Optional<BlockPos> Found = NpcDoors.findScanner(Helper.getLevel(), DoorAt);
            Helper.assertTrue(Found.isPresent() && Found.get().equals(ScannerAt), "the scanner was not found, got " + Found);
            Helper.assertTrue(NpcDoors.findOpener(Helper.getLevel(), DoorAt).isEmpty(), "a scanner was taken for a button or lever");
            Helper.assertTrue(NpcDoors.pickCard(Helper.getLevel(), ScannerAt, List.of(Low, Two)).isEmpty(), "a low card was picked for a level 3 scanner");
            Optional<ItemStack> Picked = NpcDoors.pickCard(Helper.getLevel(), ScannerAt, List.of(Five, Low, Three));
            Helper.assertTrue(Picked.isPresent() && Picked.get() == Three, "the lowest sufficient card was not picked");
            Helper.assertTrue(!NpcDoors.swipe(User, Low, ScannerAt), "a level 1 card was accepted by a level 3 scanner");
        });
        Helper.runAfterDelay(4, () -> Helper.assertTrue(!open(Helper, new BlockPos(6, Base, 5)), "a refused card opened the door"));
        Helper.runAfterDelay(30, () -> {
            Helper.assertTrue(NpcDoors.swipe(User, Three, ScannerAt), "a level 3 card was refused");
            Helper.assertTrue(Three.getCount() == 1, "the swipe used up the card");
        });
        Helper.runAfterDelay(33, () -> {
            Helper.assertTrue(open(Helper, new BlockPos(6, Base, 5)), "an accepted card did not open the door");
            User.discard();
            Helper.succeed();
        });
    }
}
