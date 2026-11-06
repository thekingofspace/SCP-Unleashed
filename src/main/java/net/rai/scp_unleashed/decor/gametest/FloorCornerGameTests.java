package net.rai.scp_unleashed.decor.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.decor.DecorRegistry;
import net.rai.scp_unleashed.decor.FacilityFloorCornerBlock;
import net.rai.scp_unleashed.decor.FacilityFloorMarkBlock;
import net.rai.scp_unleashed.decor.FacilityZone;
import net.rai.scp_unleashed.decor.ZonePart;

import java.util.EnumSet;
import java.util.Set;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class FloorCornerGameTests {
    public static final String Arena = "floor_corner_arena";
    private static final int FloorY = 1;
    private static final float[] Yaws = {200.0f, 250.0f, 290.0f, 340.0f, 20.0f, 70.0f, 110.0f, 160.0f};
    private static final Direction[] Expected = {Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.EAST, Direction.SOUTH, Direction.SOUTH, Direction.WEST, Direction.WEST};

    private FloorCornerGameTests() {
    }

    private static Set<Direction> edges(Direction Facing) {
        return EnumSet.of(Facing, Facing.getClockWise());
    }

    private static final ZonePart[] Pieces = {ZonePart.FloorMarkedCorner, ZonePart.FloorMarkedOuter};

    private static Block piece(FacilityZone Zone, ZonePart Part) {
        return DecorRegistry.zoneBlock(Zone, Part).get();
    }

    private static int dropped(GameTestHelper Helper, Item Wanted) {
        int Total = 0;
        for (ItemEntity Drop : Helper.getEntities(EntityType.ITEM)) {
            if (Drop.getItem().is(Wanted)) {
                Total += Drop.getItem().getCount();
            }
        }
        return Total;
    }

    @GameTest(template = Arena, timeoutTicks = 40)
    public static void cornerFacesTheCornerYouLookAt(GameTestHelper Helper) {
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        for (int Kind = 0; Kind < Pieces.length; Kind++) {
            Block Corner = piece(FacilityZone.Medical, Pieces[Kind]);
            Helper.assertTrue(Corner instanceof FacilityFloorCornerBlock, "the medical " + Pieces[Kind].getSerializedName() + " is not a FacilityFloorCornerBlock: " + Corner);
            for (int Index = 0; Index < Yaws.length; Index++) {
                int Cell = Kind * Yaws.length + Index;
                BlockPos Ground = new BlockPos(Cell % 5, FloorY, Cell / 5);
                BlockPos Absolute = Helper.absolutePos(Ground);
                ItemStack Stack = new ItemStack(Corner.asItem(), 2);
                User.setItemInHand(InteractionHand.MAIN_HAND, Stack);
                User.setYRot(Yaws[Index]);
                Stack.useOn(new UseOnContext(User, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(Absolute).add(0.0, 0.5, 0.0), Direction.UP, Absolute, false)));
                BlockState Placed = Helper.getBlockState(Ground.above());
                Helper.assertTrue(Placed.is(Corner), "looking at yaw " + Yaws[Index] + " did not place the " + Pieces[Kind].getSerializedName() + ", found " + Placed);
                Direction Facing = Placed.getValue(FacilityFloorMarkBlock.Facing);
                Helper.assertTrue(Facing == Expected[Index], "looking at yaw " + Yaws[Index] + " placed the " + Pieces[Kind].getSerializedName() + " facing " + Facing + ", expected " + Expected[Index]);
                Helper.assertTrue(Stack.getCount() == 1, "placing a " + Pieces[Kind].getSerializedName() + " should use one item");
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40)
    public static void cornerTurnsMirrorsAndDropsItself(GameTestHelper Helper) {
        BlockPos Origin = Helper.absolutePos(new BlockPos(0, FloorY + 1, 0));
        for (ZonePart Part : Pieces) {
            Block Corner = piece(FacilityZone.Lcz, Part);
            for (Direction Facing : Direction.Plane.HORIZONTAL) {
                BlockState State = Corner.defaultBlockState().setValue(FacilityFloorMarkBlock.Facing, Facing);
                for (Rotation Turn : Rotation.values()) {
                    Direction Turned = State.rotate(Helper.getLevel(), Origin, Turn).getValue(FacilityFloorMarkBlock.Facing);
                    Set<Direction> Wanted = EnumSet.of(Turn.rotate(Facing), Turn.rotate(Facing.getClockWise()));
                    Helper.assertTrue(edges(Turned).equals(Wanted), "rotating a " + Part.getSerializedName() + " facing " + Facing + " by " + Turn + " gave " + Turned + ", its corner should sit between " + Wanted);
                }
                for (Mirror Flip : Mirror.values()) {
                    Direction Flipped = State.mirror(Flip).getValue(FacilityFloorMarkBlock.Facing);
                    Set<Direction> Wanted = EnumSet.of(Flip.mirror(Facing), Flip.mirror(Facing.getClockWise()));
                    Helper.assertTrue(edges(Flipped).equals(Wanted), "mirroring a " + Part.getSerializedName() + " facing " + Facing + " with " + Flip + " gave " + Flipped + ", its corner should sit between " + Wanted);
                }
            }
        }
        ItemStack Pickaxe = new ItemStack(Items.WOODEN_PICKAXE);
        FacilityZone[] Zones = FacilityZone.values();
        for (int Kind = 0; Kind < Pieces.length; Kind++) {
            for (int Index = 0; Index < Zones.length; Index++) {
                Block Piece = piece(Zones[Index], Pieces[Kind]);
                String Name = Zones[Index] + " " + Pieces[Kind].getSerializedName();
                BlockState State = Piece.defaultBlockState();
                BlockState Marked = DecorRegistry.zoneBlock(Zones[Index], ZonePart.FloorMarked).get().defaultBlockState();
                int Cell = Kind * Zones.length + Index;
                BlockPos Spot = new BlockPos(Cell % 5, FloorY + 1, Cell / 5);
                Helper.assertTrue(Piece instanceof FacilityFloorCornerBlock, Name + " is not a FacilityFloorCornerBlock");
                Helper.assertTrue(State.is(BlockTags.MINEABLE_WITH_PICKAXE) && Pickaxe.isCorrectToolForDrops(State) == Pickaxe.isCorrectToolForDrops(Marked),
                        Name + " is not mined like its marked floor");
                BlockPos Absolute = Helper.absolutePos(Spot);
                Helper.assertTrue(State.getSoundType(Helper.getLevel(), Absolute, null) == Marked.getSoundType(Helper.getLevel(), Absolute, null)
                                && State.getMapColor(Helper.getLevel(), Absolute) == Marked.getMapColor(Helper.getLevel(), Absolute),
                        Name + " does not sound or map like its marked floor");
                Helper.setBlock(Spot, State);
                Helper.getLevel().destroyBlock(Absolute, true);
                Helper.assertTrue(Helper.getBlockState(Spot).isAir(), Name + " was not broken");
                int Count = dropped(Helper, Piece.asItem());
                Helper.assertTrue(Count == 1, Name + " dropped " + Count + " of itself, expected 1");
            }
        }
        Helper.succeed();
    }
}
