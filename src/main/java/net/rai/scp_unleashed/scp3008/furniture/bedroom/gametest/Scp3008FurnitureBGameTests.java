package net.rai.scp_unleashed.scp3008.furniture.bedroom.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.props.PropSeatEntity;
import net.rai.scp_unleashed.props.PropsRegistry;
import net.rai.scp_unleashed.scp3008.furniture.bedroom.Scp3008FurnitureBBlock;
import net.rai.scp_unleashed.scp3008.furniture.bedroom.Scp3008FurnitureBPiece;
import net.rai.scp_unleashed.scp3008.furniture.bedroom.Scp3008FurnitureBRegistry;
import net.rai.scp_unleashed.scp3008.furniture.bedroom.Scp3008FurnitureBStorage;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp3008FurnitureBGameTests {
    public static final String Arena = "gateway_arena";
    public static final int SettleTicks = 10;
    public static final List<String> Departments = List.of("living", "dining", "kitchen", "bedroom", "bathroom", "office", "children", "lighting", "textiles", "outdoor", "warehouse");
    public static final List<String> Mounts = List.of("scp3008_floor_standing", "scp3008_wall_mounted", "scp3008_ceiling_hung");

    private Scp3008FurnitureBGameTests() {
    }

    private static TagKey<Block> tag(String Path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path));
    }

    private static Scp3008FurnitureBBlock block(Scp3008FurnitureBPiece Piece) {
        return Scp3008FurnitureBRegistry.block(Piece);
    }

    private static void clear(GameTestHelper Helper, BlockPos Local, int Reach) {
        for (int X = -Reach; X <= Reach; X++) {
            for (int Z = -Reach; Z <= Reach; Z++) {
                Helper.setBlock(Local.offset(X, -1, Z), Blocks.SMOOTH_STONE);
                for (int Y = 0; Y <= 2; Y++) {
                    Helper.setBlock(Local.offset(X, Y, Z), Blocks.AIR);
                }
            }
        }
    }

    private static BlockPos stand(GameTestHelper Helper, Scp3008FurnitureBPiece Piece, BlockPos Local, Direction Facing) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(Local);
        BlockState State = block(Piece).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Facing);
        World.setBlock(Pos, State, Block.UPDATE_ALL);
        Scp3008FurnitureBBlock.placeWhole(World, Pos, State);
        return Pos;
    }

    private static void expectWhole(GameTestHelper Helper, Scp3008FurnitureBPiece Piece, BlockPos Pos, Direction Facing) {
        ServerLevel World = Helper.getLevel();
        Scp3008FurnitureBBlock Furniture = block(Piece);
        for (int Part = 0; Part < Piece.parts(); Part++) {
            BlockPos Spot = Furniture.partPos(Pos, Facing, Part);
            BlockState There = World.getBlockState(Spot);
            String Where = Piece.Name + " facing " + Facing + " part " + Part;
            Helper.assertTrue(There.is(Furniture), Where + " is missing");
            Helper.assertTrue(There.getValue(HorizontalDirectionalBlock.FACING) == Facing, Where + " faces the wrong way");
            Helper.assertTrue(Furniture.part(There) == Part, Where + " holds the wrong part index");
            Helper.assertFalse(There.getShape(World, Spot).isEmpty(), Where + " has an empty shape");
            Helper.assertTrue(Furniture.main(Spot, There).equals(Pos), Where + " does not find its main block");
        }
    }

    private static void expectGone(GameTestHelper Helper, Scp3008FurnitureBPiece Piece, BlockPos Pos, Direction Facing) {
        ServerLevel World = Helper.getLevel();
        Scp3008FurnitureBBlock Furniture = block(Piece);
        for (int Part = 0; Part < Piece.parts(); Part++) {
            Helper.assertFalse(World.getBlockState(Furniture.partPos(Pos, Facing, Part)).is(Furniture), Piece.Name + " part " + Part + " outlived a broken sibling");
        }
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp3008_furnb_place")
    public static void everyPieceStandsInEveryFacing(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Local = new BlockPos(16, 2, 16);
        for (Scp3008FurnitureBPiece Piece : Scp3008FurnitureBPiece.values()) {
            Scp3008FurnitureBBlock Furniture = block(Piece);
            for (Direction Facing : Direction.Plane.HORIZONTAL) {
                clear(Helper, Local, 2);
                BlockPos Pos = stand(Helper, Piece, Local, Facing);
                expectWhole(Helper, Piece, Pos, Facing);
                World.destroyBlock(Furniture.partPos(Pos, Facing, Piece.parts() - 1), false);
                expectGone(Helper, Piece, Pos, Facing);
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp3008_furnb_item")
    public static void itemsPlaceWholePiecesOrRefuse(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Local = new BlockPos(16, 2, 16);
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        User.setYRot(0.0f);
        for (Scp3008FurnitureBPiece Piece : Scp3008FurnitureBPiece.values()) {
            Scp3008FurnitureBBlock Furniture = block(Piece);
            clear(Helper, Local, 2);
            BlockPos Pos = Helper.absolutePos(Local);
            InteractionResult Placed = placeWithItem(User, Furniture, Pos);
            Helper.assertTrue(Placed.consumesAction(), Piece.Name + " could not be placed from its item");
            BlockState Main = World.getBlockState(Pos);
            Direction Facing = Main.getValue(HorizontalDirectionalBlock.FACING);
            Helper.assertTrue(Facing == Direction.NORTH, Piece.Name + " was placed facing " + Facing + " instead of towards the player");
            expectWhole(Helper, Piece, Pos, Facing);
            if (Piece.multi()) {
                clear(Helper, Local, 2);
                World.setBlock(Furniture.partPos(Pos, Direction.NORTH, Piece.parts() - 1), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                InteractionResult Blocked = placeWithItem(User, Furniture, Pos);
                Helper.assertFalse(Blocked.consumesAction(), Piece.Name + " was placed into an occupied spot");
                Helper.assertFalse(World.getBlockState(Pos).is(Furniture), Piece.Name + " left a lone main block when it did not fit");
            }
        }
        Helper.succeed();
    }

    private static InteractionResult placeWithItem(Player User, Scp3008FurnitureBBlock Furniture, BlockPos Pos) {
        ItemStack Stack = new ItemStack(Furniture.asItem());
        User.setItemInHand(InteractionHand.MAIN_HAND, Stack);
        BlockPos Floor = Pos.below();
        BlockHitResult Hit = new BlockHitResult(Vec3.atCenterOf(Floor).add(0.0, 0.5, 0.0), Direction.UP, Floor, false);
        return ((BlockItem) Stack.getItem()).place(new BlockPlaceContext(User, InteractionHand.MAIN_HAND, Stack, Hit));
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furnb_tags")
    public static void everyPieceIsTaggedOnce(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        for (Scp3008FurnitureBPiece Piece : Scp3008FurnitureBPiece.values()) {
            BlockState State = block(Piece).defaultBlockState();
            List<String> Found = new ArrayList<>();
            for (String Department : Departments) {
                if (State.is(tag("scp3008_furniture/" + Department))) {
                    Found.add(Department);
                }
            }
            Helper.assertTrue(Found.equals(List.of(Piece.Department)), Piece.Name + " has departments " + Found + " instead of " + Piece.Department);
            List<String> Placements = new ArrayList<>();
            for (String Mount : Mounts) {
                if (State.is(tag(Mount))) {
                    Placements.add(Mount);
                }
            }
            Helper.assertTrue(Placements.equals(List.of(Piece.Placement.TagPath)), Piece.Name + " has placement tags " + Placements);
            Helper.assertTrue(State.is(tag("scp3008_liftable")), Piece.Name + " cannot be lifted");
            Helper.assertTrue(State.is(tag("scp3008_multiblock")) == Piece.multi(), Piece.Name + " has the wrong multiblock tag");
            Helper.assertTrue(State.is(tag("scp3008_salvage/table_legs")) == (Piece == Scp3008FurnitureBPiece.Desk), Piece.Name + " has the wrong table leg salvage tag");
            Helper.assertFalse(State.is(tag("scp3008_salvage/cabinet_doors")), Piece.Name + " should not drop cabinet doors");
            Helper.assertFalse(State.is(tag("scp3008_unbreakable")), Piece.Name + " is tagged unbreakable");
            boolean Axe = State.is(BlockTags.MINEABLE_WITH_AXE);
            boolean Pickaxe = State.is(BlockTags.MINEABLE_WITH_PICKAXE);
            Helper.assertTrue(Axe != Pickaxe && (Axe ? "axe" : "pickaxe").equals(Piece.Finish.Tool), Piece.Name + " has the wrong mining tool tags");
            LootTable Loot = World.getServer().reloadableRegistries().getLootTable(block(Piece).getLootTable());
            Helper.assertTrue(Loot != LootTable.EMPTY, Piece.Name + " has no loot table");
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp3008_furnb_seats")
    public static void seatsHoldAndCabinetsDoNot(GameTestHelper Helper) {
        List<Scp3008FurnitureBPiece> Seats = List.of(Scp3008FurnitureBPiece.DoubleBed, Scp3008FurnitureBPiece.SingleBed, Scp3008FurnitureBPiece.OfficeChair, Scp3008FurnitureBPiece.Toilet);
        List<PropSeatEntity> Placed = new ArrayList<>();
        for (int Index = 0; Index < Seats.size(); Index++) {
            BlockPos Local = new BlockPos(3 + Index * 5, 2, 6);
            clear(Helper, Local, 2);
            BlockPos Pos = stand(Helper, Seats.get(Index), Local, Direction.NORTH);
            Placed.add(seat(Helper, Pos, Seats.get(Index).Name));
        }
        BlockPos Local = new BlockPos(24, 2, 20);
        clear(Helper, Local, 2);
        PropSeatEntity Stray = seat(Helper, stand(Helper, Scp3008FurnitureBPiece.Wardrobe, Local, Direction.NORTH), "wardrobe");
        Helper.runAfterDelay(SettleTicks, () -> {
            for (int Index = 0; Index < Placed.size(); Index++) {
                Helper.assertTrue(Placed.get(Index).isAlive() && Placed.get(Index).isVehicle(), Seats.get(Index).Name + " threw its rider off");
            }
            Helper.assertFalse(Stray.isAlive(), "a seat on the wardrobe stayed");
            Helper.succeed();
        });
    }

    private static PropSeatEntity seat(GameTestHelper Helper, BlockPos Pos, String Name) {
        ServerLevel World = Helper.getLevel();
        PropSeatEntity Seat = new PropSeatEntity(PropsRegistry.Seat.get(), World);
        Seat.moveTo(Pos.getX() + 0.5, Pos.getY(), Pos.getZ() + 0.5, 0.0f, 0.0f);
        World.addFreshEntity(Seat);
        ArmorStand Rider = EntityType.ARMOR_STAND.create(World);
        Helper.assertTrue(Rider != null, "no armor stand");
        Rider.moveTo(Pos.getX() + 0.5, Pos.getY() + 1.0, Pos.getZ() + 0.5, 0.0f, 0.0f);
        World.addFreshEntity(Rider);
        Helper.assertTrue(Rider.startRiding(Seat, true), "the rider could not sit on " + Name);
        return Seat;
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp3008_furnb_storage")
    public static void storageTakesLootOpensAndSpills(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        ResourceKey<LootTable> Dungeon = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/simple_dungeon"));
        int Index = 0;
        for (Scp3008FurnitureBPiece Piece : Scp3008FurnitureBPiece.values()) {
            if (!Piece.storage()) {
                continue;
            }
            Scp3008FurnitureBBlock Furniture = block(Piece);
            BlockPos Local = new BlockPos(4 + (Index % 4) * 7, 2, 6 + (Index / 4) * 10);
            Index++;
            clear(Helper, Local, 2);
            BlockPos Pos = stand(Helper, Piece, Local, Direction.NORTH);
            BlockState Main = World.getBlockState(Pos);
            Scp3008FurnitureBStorage Storage = Furniture.storageAt(World, Pos, Main);
            Helper.assertTrue(Storage != null, Piece.Name + " has no storage");
            Helper.assertTrue(Storage instanceof RandomizableContainer, Piece.Name + " storage is not randomizable");
            Helper.assertTrue(Storage.getContainerSize() == Piece.Slots, Piece.Name + " holds " + Storage.getContainerSize() + " slots");
            for (int Part = 0; Part < Piece.parts(); Part++) {
                BlockPos Spot = Furniture.partPos(Pos, Direction.NORTH, Part);
                Helper.assertTrue(Furniture.storageAt(World, Spot, World.getBlockState(Spot)) == Storage, Piece.Name + " part " + Part + " does not reach the storage");
            }
            Storage.setLootTable(Dungeon, 3008L);
            Storage.unpackLootTable(null);
            Helper.assertFalse(Storage.isEmpty(), Piece.Name + " stayed empty after its loot table");
            if (Piece.Opens != Scp3008FurnitureBPiece.Opening.None) {
                Storage.startOpen(User);
                expectOpen(Helper, Piece, Pos, true);
                Storage.stopOpen(User);
                expectOpen(Helper, Piece, Pos, false);
            }
            Storage.clearContent();
            Storage.setItem(0, new ItemStack(Items.DIAMOND));
            World.destroyBlock(Furniture.partPos(Pos, Direction.NORTH, Piece.parts() - 1), false);
            expectGone(Helper, Piece, Pos, Direction.NORTH);
            AABB Around = new AABB(Pos).inflate(3.0);
            boolean Spilled = World.getEntitiesOfClass(ItemEntity.class, Around).stream().anyMatch(Drop -> Drop.getItem().is(Items.DIAMOND));
            Helper.assertTrue(Spilled, Piece.Name + " swallowed its contents when it broke");
            World.getEntitiesOfClass(ItemEntity.class, Around).forEach(ItemEntity::discard);
        }
        Helper.succeed();
    }

    private static void expectOpen(GameTestHelper Helper, Scp3008FurnitureBPiece Piece, BlockPos Pos, boolean Wanted) {
        ServerLevel World = Helper.getLevel();
        Scp3008FurnitureBBlock Furniture = block(Piece);
        for (int Part = 0; Part < Piece.parts(); Part++) {
            BlockState There = World.getBlockState(Furniture.partPos(Pos, Direction.NORTH, Part));
            Helper.assertTrue(There.getValue(Scp3008FurnitureBBlock.Open) == Wanted, Piece.Name + " part " + Part + " is not " + (Wanted ? "open" : "shut"));
        }
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furnb_lamp")
    public static void deskLampTogglesAndSitsOnDesks(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos Local = new BlockPos(8, 2, 8);
        clear(Helper, Local, 2);
        BlockPos Pos = stand(Helper, Scp3008FurnitureBPiece.DeskLamp, Local, Direction.NORTH);
        BlockState Lamp = World.getBlockState(Pos);
        Helper.assertFalse(Lamp.getValue(Scp3008FurnitureBBlock.Lit), "the lamp starts lit");
        Helper.assertFalse(Lamp.getValue(Scp3008FurnitureBBlock.Lowered), "the lamp sinks into the floor");
        Lamp.useWithoutItem(World, User, new BlockHitResult(Vec3.atCenterOf(Pos), Direction.NORTH, Pos, false));
        Helper.assertTrue(World.getBlockState(Pos).getValue(Scp3008FurnitureBBlock.Lit), "the lamp did not switch on");
        Helper.assertTrue(World.getBlockState(Pos).getLightEmission(World, Pos) == Scp3008FurnitureBBlock.LampLight, "the lit lamp gives no light");
        World.getBlockState(Pos).useWithoutItem(World, User, new BlockHitResult(Vec3.atCenterOf(Pos), Direction.NORTH, Pos, false));
        Helper.assertFalse(World.getBlockState(Pos).getValue(Scp3008FurnitureBBlock.Lit), "the lamp did not switch off");
        Helper.assertTrue(World.getBlockState(Pos).getLightEmission(World, Pos) == 0, "the dark lamp still glows");
        BlockPos DeskLocal = new BlockPos(20, 2, 20);
        clear(Helper, DeskLocal, 2);
        BlockPos Desk = stand(Helper, Scp3008FurnitureBPiece.Desk, DeskLocal, Direction.NORTH);
        BlockPos OnDesk = stand(Helper, Scp3008FurnitureBPiece.DeskLamp, DeskLocal.above(), Direction.NORTH);
        Helper.assertTrue(World.getBlockState(OnDesk).getValue(Scp3008FurnitureBBlock.Lowered), "the lamp floats above the desk top");
        World.destroyBlock(Desk, false);
        Helper.assertFalse(World.getBlockState(OnDesk).getValue(Scp3008FurnitureBBlock.Lowered), "the lamp stayed lowered after the desk left");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furnb_water")
    public static void bathSinkToiletAndShowerWork(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos TubLocal = new BlockPos(6, 2, 6);
        clear(Helper, TubLocal, 2);
        BlockPos Tub = stand(Helper, Scp3008FurnitureBPiece.Bathtub, TubLocal, Direction.NORTH);
        Scp3008FurnitureBBlock Bathtub = block(Scp3008FurnitureBPiece.Bathtub);
        BlockPos TubEnd = Bathtub.partPos(Tub, Direction.NORTH, 1);
        useItem(Helper, User, Tub, new ItemStack(Items.WATER_BUCKET));
        Helper.assertTrue(World.getBlockState(Tub).getValue(Scp3008FurnitureBBlock.Filled) && World.getBlockState(TubEnd).getValue(Scp3008FurnitureBBlock.Filled), "the bath did not fill both halves");
        Helper.assertTrue(User.getMainHandItem().is(Items.BUCKET), "filling the bath did not hand back the bucket");
        useItem(Helper, User, TubEnd, new ItemStack(Items.GLASS_BOTTLE));
        Helper.assertTrue(User.getMainHandItem().is(Items.POTION), "a bottle did not fill from the bath");
        useItem(Helper, User, TubEnd, new ItemStack(Items.BUCKET));
        Helper.assertTrue(User.getMainHandItem().is(Items.WATER_BUCKET), "the bath did not give its water back");
        Helper.assertFalse(World.getBlockState(Tub).getValue(Scp3008FurnitureBBlock.Filled) || World.getBlockState(TubEnd).getValue(Scp3008FurnitureBBlock.Filled), "the bath stayed full");
        BlockPos SinkLocal = new BlockPos(14, 2, 6);
        clear(Helper, SinkLocal, 2);
        BlockPos Sink = stand(Helper, Scp3008FurnitureBPiece.Vanity, SinkLocal, Direction.NORTH);
        useItem(Helper, User, Sink, new ItemStack(Items.GLASS_BOTTLE));
        Helper.assertTrue(User.getMainHandItem().is(Items.POTION), "the sink did not fill a bottle");
        BlockPos ToiletLocal = new BlockPos(22, 2, 6);
        clear(Helper, ToiletLocal, 2);
        BlockPos Toilet = stand(Helper, Scp3008FurnitureBPiece.Toilet, ToiletLocal, Direction.NORTH);
        User.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        User.setShiftKeyDown(true);
        InteractionResult Flushed = World.getBlockState(Toilet).useWithoutItem(World, User, new BlockHitResult(Vec3.atCenterOf(Toilet), Direction.NORTH, Toilet, false));
        Helper.assertTrue(Flushed.consumesAction(), "the toilet did not flush");
        User.setShiftKeyDown(false);
        BlockPos ShowerLocal = new BlockPos(10, 2, 20);
        clear(Helper, ShowerLocal, 2);
        BlockPos Shower = stand(Helper, Scp3008FurnitureBPiece.Shower, ShowerLocal, Direction.NORTH);
        VoxelShape Door = Shapes.box(0.4, 0.4, 0.0, 0.6, 0.6, 0.03);
        Helper.assertTrue(Shapes.joinIsNotEmpty(World.getBlockState(Shower).getCollisionShape(World, Shower), Door, BooleanOp.AND), "the shut shower has no door");
        World.getBlockState(Shower).useWithoutItem(World, User, new BlockHitResult(Vec3.atCenterOf(Shower), Direction.NORTH, Shower, false));
        BlockPos ShowerTop = Shower.above();
        Helper.assertTrue(World.getBlockState(Shower).getValue(Scp3008FurnitureBBlock.Open) && World.getBlockState(ShowerTop).getValue(Scp3008FurnitureBBlock.Open), "the shower door did not open on both halves");
        Helper.assertFalse(Shapes.joinIsNotEmpty(World.getBlockState(Shower).getCollisionShape(World, Shower), Door, BooleanOp.AND), "the open shower still blocks the doorway");
        World.getBlockState(ShowerTop).useWithoutItem(World, User, new BlockHitResult(Vec3.atCenterOf(ShowerTop), Direction.NORTH, ShowerTop, false));
        Helper.assertFalse(World.getBlockState(Shower).getValue(Scp3008FurnitureBBlock.Open), "the shower door did not shut from the top half");
        Helper.succeed();
    }

    private static void useItem(GameTestHelper Helper, Player User, BlockPos Pos, ItemStack Stack) {
        ServerLevel World = Helper.getLevel();
        User.setItemInHand(InteractionHand.MAIN_HAND, Stack);
        ItemInteractionResult Result = World.getBlockState(Pos).useItemOn(Stack, World, User, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(Pos), Direction.UP, Pos, false));
        Helper.assertTrue(Result.consumesAction(), "using " + Stack.getItem() + " on " + World.getBlockState(Pos).getBlock().getDescriptionId() + " did nothing");
    }
}
