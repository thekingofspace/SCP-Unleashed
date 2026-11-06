package net.rai.scp_unleashed.scp3008.furniture.living.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.props.PropSeatEntity;
import net.rai.scp_unleashed.props.PropsRegistry;
import net.rai.scp_unleashed.props.SeatBlock;
import net.rai.scp_unleashed.scp3008.furniture.living.Scp3008FurnitureACabinetBlock;
import net.rai.scp_unleashed.scp3008.furniture.living.Scp3008FurnitureAPairs;
import net.rai.scp_unleashed.scp3008.furniture.living.Scp3008FurnitureAPart;
import net.rai.scp_unleashed.scp3008.furniture.living.Scp3008FurnitureAPiece;
import net.rai.scp_unleashed.scp3008.furniture.living.Scp3008FurnitureARegistry;
import net.rai.scp_unleashed.scp3008.furniture.living.Scp3008FurnitureAStorageBlockEntity;
import net.rai.scp_unleashed.scp3008.furniture.living.Scp3008FurnitureAStoveBlock;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp3008FurnitureAGameTests {
    public static final String Arena = "scp444_arena";
    public static final int SettleTicks = 10;
    public static final List<String> Departments = List.of("living", "dining", "kitchen", "bedroom", "bathroom", "office", "children", "lighting", "textiles", "outdoor", "warehouse");
    public static final List<String> Placements = List.of("scp3008_floor_standing", "scp3008_wall_mounted", "scp3008_ceiling_hung");
    private static final BlockPos Spot = new BlockPos(7, 2, 7);

    private Scp3008FurnitureAGameTests() {
    }

    private static TagKey<Block> tag(String Path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path));
    }

    private static void clear(GameTestHelper Helper) {
        for (int X = 5; X <= 9; X++) {
            for (int Y = 2; Y <= 4; Y++) {
                for (int Z = 5; Z <= 9; Z++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static BlockState place(GameTestHelper Helper, Scp3008FurnitureAPiece Piece, Direction Facing) {
        Block Furniture = Scp3008FurnitureARegistry.block(Piece);
        BlockState State = Furniture.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Facing);
        BlockPos Pos = Helper.absolutePos(Spot);
        ServerLevel World = Helper.getLevel();
        if (Piece.paired()) {
            try {
                Method Whole = Furniture.getClass().getMethod("placeWhole", Level.class, BlockPos.class, BlockState.class);
                Whole.invoke(null, World, Pos, State);
            } catch (ReflectiveOperationException Problem) {
                Helper.fail(Piece.Id + " has no usable placeWhole(Level, BlockPos, BlockState): " + Problem);
            }
        } else {
            World.setBlock(Pos, State, Block.UPDATE_ALL);
        }
        return World.getBlockState(Pos);
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furna_place")
    public static void everyPiecePlacesInEveryFacing(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(Spot);
        Helper.assertTrue(Scp3008FurnitureAPiece.values().length >= 17, "agent A owes at least 17 pieces, found " + Scp3008FurnitureAPiece.values().length);
        for (Scp3008FurnitureAPiece Piece : Scp3008FurnitureAPiece.values()) {
            Block Furniture = Scp3008FurnitureARegistry.block(Piece);
            Helper.assertTrue(BuiltInRegistries.BLOCK.getKey(Furniture).getPath().equals(Piece.Id), Piece.Id + " is registered under the wrong id");
            Helper.assertTrue(BuiltInRegistries.ITEM.getKey(Furniture.asItem()).getPath().equals(Piece.Id), Piece.Id + " has no block item");
            for (Direction Facing : Direction.Plane.HORIZONTAL) {
                clear(Helper);
                BlockState State = place(Helper, Piece, Facing);
                Helper.assertTrue(State.is(Furniture) && State.getValue(HorizontalDirectionalBlock.FACING) == Facing, Piece.Id + " did not place facing " + Facing);
                Helper.assertFalse(State.getShape(World, Pos).isEmpty(), Piece.Id + " has an empty shape facing " + Facing);
                Helper.assertFalse(State.getCollisionShape(World, Pos).isEmpty(), Piece.Id + " has no collision facing " + Facing);
                if (!Piece.paired()) {
                    continue;
                }
                BlockPos Expected = Pos.relative(Piece.Reach == Scp3008FurnitureAPiece.Span.Up ? Direction.UP : Facing.getClockWise());
                BlockPos Other = Scp3008FurnitureAPairs.partner(Piece, State, Pos);
                Helper.assertTrue(Other.equals(Expected), Piece.Id + " put its second part in the wrong place facing " + Facing);
                BlockState Extra = World.getBlockState(Other);
                Helper.assertTrue(Extra.is(Furniture) && Extra.getValue(Scp3008FurnitureAPairs.Part) == Scp3008FurnitureAPart.Extra
                        && Extra.getValue(HorizontalDirectionalBlock.FACING) == Facing, Piece.Id + " did not place its second part facing " + Facing);
                Helper.assertFalse(Extra.getShape(World, Other).isEmpty(), Piece.Id + " second part has an empty shape facing " + Facing);
                World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                Helper.assertTrue(World.getBlockState(Other).isAir(), Piece.Id + " left its second part behind facing " + Facing);
                place(Helper, Piece, Facing);
                World.setBlock(Other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                Helper.assertTrue(World.getBlockState(Pos).isAir(), Piece.Id + " left its main part behind facing " + Facing);
                World.setBlock(Other, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                place(Helper, Piece, Facing);
                Helper.assertTrue(World.getBlockState(Pos).isAir(), Piece.Id + " placed half of itself into a blocked spot facing " + Facing);
            }
        }
        clear(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp3008_furna_seat")
    public static void seatsHoldTheirRiders(GameTestHelper Helper) {
        List<Scp3008FurnitureAPiece> Seats = new ArrayList<>();
        for (Scp3008FurnitureAPiece Piece : Scp3008FurnitureAPiece.values()) {
            if (Piece.Seat) {
                Seats.add(Piece);
            }
        }
        Helper.assertTrue(Seats.size() >= 4, "too few seats: " + Seats.size());
        List<PropSeatEntity> Placed = new ArrayList<>();
        for (int Index = 0; Index < Seats.size(); Index++) {
            Placed.add(seat(Helper, Seats.get(Index), new BlockPos(1 + Index * 3, 2, 3)));
        }
        PropSeatEntity Table = seat(Helper, Scp3008FurnitureAPiece.CoffeeTable, new BlockPos(2, 2, 11));
        BlockPos SofaEnd = new BlockPos(1 + Seats.indexOf(Scp3008FurnitureAPiece.Sofa) * 3 + 1, 2, 3);
        BlockState End = Helper.getLevel().getBlockState(Helper.absolutePos(SofaEnd));
        Helper.assertTrue(End.is(Scp3008FurnitureARegistry.block(Scp3008FurnitureAPiece.Sofa)) && End.getBlock() instanceof SeatBlock Host && Host.seats(End),
                "the far end of the sofa does not seat");
        Helper.runAfterDelay(SettleTicks, () -> {
            for (int Index = 0; Index < Placed.size(); Index++) {
                PropSeatEntity Seat = Placed.get(Index);
                Helper.assertTrue(Seat.isAlive() && Seat.isVehicle(), "the seat on " + Seats.get(Index).Id + " threw its rider off");
            }
            Helper.assertFalse(Table.isAlive(), "a seat on a coffee table stayed");
            Helper.succeed();
        });
    }

    private static PropSeatEntity seat(GameTestHelper Helper, Scp3008FurnitureAPiece Piece, BlockPos Local) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(Local);
        BlockState State = Scp3008FurnitureARegistry.block(Piece).defaultBlockState();
        if (Piece.paired()) {
            Scp3008FurnitureAPairs.placeWhole(World, Pos, State);
        } else {
            World.setBlockAndUpdate(Pos, State);
        }
        PropSeatEntity Seat = new PropSeatEntity(PropsRegistry.Seat.get(), World);
        Seat.moveTo(Pos.getX() + 0.5, Pos.getY(), Pos.getZ() + 0.5, 0.0f, 0.0f);
        World.addFreshEntity(Seat);
        ArmorStand Rider = EntityType.ARMOR_STAND.create(World);
        Helper.assertTrue(Rider != null, "no armor stand");
        Rider.moveTo(Pos.getX() + 0.5, Pos.getY() + 1.0, Pos.getZ() + 0.5, 0.0f, 0.0f);
        World.addFreshEntity(Rider);
        Helper.assertTrue(Rider.startRiding(Seat, true), "the rider could not sit on " + Piece.Id);
        return Seat;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furna_storage")
    public static void storageTakesLootTablesAndOpens(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(Spot);
        Player Shopper = Helper.makeMockPlayer(GameType.SURVIVAL);
        int Checked = 0;
        for (Scp3008FurnitureAPiece Piece : Scp3008FurnitureAPiece.values()) {
            if (!Piece.storage()) {
                continue;
            }
            clear(Helper);
            BlockState State = place(Helper, Piece, Direction.NORTH);
            BlockEntity Entity = World.getBlockEntity(Pos);
            Helper.assertTrue(Entity instanceof RandomizableContainerBlockEntity && Entity instanceof Scp3008FurnitureAStorageBlockEntity, Piece.Id + " has no randomizable container");
            Scp3008FurnitureAStorageBlockEntity Storage = (Scp3008FurnitureAStorageBlockEntity) Entity;
            Helper.assertTrue(Storage.getContainerSize() == Piece.Slots, Piece.Id + " holds " + Storage.getContainerSize() + " instead of " + Piece.Slots);
            ResourceKey<LootTable> Key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "chests/scp3008/" + Piece.Department));
            Storage.setLootTable(Key, 3008L);
            Helper.assertTrue(Key.equals(Storage.getLootTable()) && Storage.getLootTableSeed() == 3008L, Piece.Id + " did not keep its loot table");
            Storage.setLootTable(null);
            if (Piece.paired()) {
                BlockPos Upper = Scp3008FurnitureAPairs.partner(Piece, State, Pos);
                BlockState Top = World.getBlockState(Upper);
                Helper.assertTrue(World.getBlockEntity(Upper) == null, Piece.Id + " grew a second container in its upper part");
                Helper.assertTrue(((Scp3008FurnitureACabinetBlock) Top.getBlock()).containerPos(Top, Upper).equals(Pos), Piece.Id + " upper part does not lead to the container");
            }
            Storage.startOpen(Shopper);
            Helper.assertTrue(World.getBlockState(Pos).getValue(Scp3008FurnitureACabinetBlock.Open), Piece.Id + " did not open");
            if (Piece.paired()) {
                BlockPos Upper = Scp3008FurnitureAPairs.partner(Piece, State, Pos);
                Helper.assertTrue(World.getBlockState(Upper).getValue(Scp3008FurnitureACabinetBlock.Open), Piece.Id + " upper part did not open with it");
            }
            Storage.stopOpen(Shopper);
            Helper.assertFalse(World.getBlockState(Pos).getValue(Scp3008FurnitureACabinetBlock.Open), Piece.Id + " did not close");
            Storage.setItem(0, new ItemStack(net.minecraft.world.item.Items.BREAD, 3));
            Helper.assertTrue(Storage.getItem(0).getCount() == 3, Piece.Id + " lost a stored item");
            Storage.clearContent();
            Checked++;
        }
        Helper.assertTrue(Checked >= 6, "too few storage pieces: " + Checked);
        clear(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furna_stove")
    public static void stoveBurnersToggle(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(Spot);
        clear(Helper);
        BlockState State = place(Helper, Scp3008FurnitureAPiece.Stove, Direction.NORTH);
        Helper.assertFalse(State.getValue(Scp3008FurnitureAStoveBlock.Lit), "the cooker starts lit");
        Player Cook = Helper.makeMockPlayer(GameType.SURVIVAL);
        BlockHitResult Hit = new BlockHitResult(Vec3.atCenterOf(Pos), Direction.NORTH, Pos, false);
        State.useWithoutItem(World, Cook, Hit);
        BlockState Lit = World.getBlockState(Pos);
        Helper.assertTrue(Lit.getValue(Scp3008FurnitureAStoveBlock.Lit) && Lit.getLightEmission() == Scp3008FurnitureAStoveBlock.Glow, "the burners did not light");
        Lit.useWithoutItem(World, Cook, Hit);
        BlockState Dark = World.getBlockState(Pos);
        Helper.assertTrue(!Dark.getValue(Scp3008FurnitureAStoveBlock.Lit) && Dark.getLightEmission() == 0, "the burners did not go out");
        clear(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp3008_furna_tags")
    public static void everyPieceIsTaggedOnce(GameTestHelper Helper) {
        for (Scp3008FurnitureAPiece Piece : Scp3008FurnitureAPiece.values()) {
            BlockState State = Scp3008FurnitureARegistry.block(Piece).defaultBlockState();
            List<String> Found = new ArrayList<>();
            for (String Department : Departments) {
                if (State.is(tag("scp3008_furniture/" + Department))) {
                    Found.add(Department);
                }
            }
            Helper.assertTrue(Found.equals(List.of(Piece.Department)), Piece.Id + " departments " + Found);
            List<String> Kinds = new ArrayList<>();
            for (String Placement : Placements) {
                if (State.is(tag(Placement))) {
                    Kinds.add(Placement);
                }
            }
            Helper.assertTrue(Kinds.equals(List.of(Piece.Wall ? "scp3008_wall_mounted" : "scp3008_floor_standing")), Piece.Id + " placement tags " + Kinds);
            Helper.assertTrue(State.is(tag("scp3008_liftable")), Piece.Id + " is not liftable");
            Helper.assertFalse(State.is(tag("scp3008_unbreakable")), Piece.Id + " is unbreakable");
            Helper.assertTrue(State.is(tag("scp3008_multiblock")) == Piece.paired(), Piece.Id + " multiblock tag is wrong");
            Helper.assertTrue(State.is(tag("scp3008_salvage/table_legs")) == (Piece.Scrap == Scp3008FurnitureAPiece.Salvage.Legs), Piece.Id + " table leg salvage tag is wrong");
            Helper.assertTrue(State.is(tag("scp3008_salvage/cabinet_doors")) == (Piece.Scrap == Scp3008FurnitureAPiece.Salvage.Doors), Piece.Id + " cabinet door salvage tag is wrong");
            Helper.assertTrue(State.is(BlockTags.MINEABLE_WITH_AXE) != State.is(BlockTags.MINEABLE_WITH_PICKAXE), Piece.Id + " needs exactly one of axe or pickaxe");
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furna_drops")
    public static void piecesDropStoreSalvageOnce(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(Spot);
        AABB Around = new AABB(Pos).inflate(3.0);
        for (Scp3008FurnitureAPiece Piece : Scp3008FurnitureAPiece.values()) {
            clear(Helper);
            BlockState State = place(Helper, Piece, Direction.NORTH);
            if (Piece.paired()) {
                BlockPos Other = Scp3008FurnitureAPairs.partner(Piece, State, Pos);
                Helper.assertTrue(Block.getDrops(World.getBlockState(Other), World, Other, null).isEmpty(), Piece.Id + " second part drops loot of its own");
                World.destroyBlock(Other, true);
                Helper.assertTrue(World.getBlockState(Pos).isAir(), Piece.Id + " main part survived its second part breaking");
            } else {
                World.destroyBlock(Pos, true);
            }
            List<ItemEntity> Dropped = World.getEntitiesOfClass(ItemEntity.class, Around);
            Helper.assertFalse(Dropped.isEmpty(), Piece.Id + " dropped nothing");
            for (ItemEntity Drop : Dropped) {
                ResourceLocation Id = BuiltInRegistries.ITEM.getKey(Drop.getItem().getItem());
                Helper.assertTrue(Id.getNamespace().equals(Scp_unleashed.ModId) && Id.getPath().startsWith("scp3008_"), Piece.Id + " dropped a non-store salvage item " + Id);
                Helper.assertFalse(Drop.getItem().is(Scp3008FurnitureARegistry.block(Piece).asItem()), Piece.Id + " dropped itself");
                Drop.discard();
            }
        }
        clear(Helper);
        Helper.succeed();
    }
}
