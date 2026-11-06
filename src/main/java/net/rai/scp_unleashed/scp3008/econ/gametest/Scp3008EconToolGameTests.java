package net.rai.scp_unleashed.scp3008.econ.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.Scp3008Registry;
import net.rai.scp_unleashed.scp3008.econ.Scp3008Disassembly;
import net.rai.scp_unleashed.scp3008.econ.Scp3008EconRegistry;
import net.rai.scp_unleashed.scp3008.econ.item.Scp3008StoreArmorItem;
import net.rai.scp_unleashed.scp3008.gear.Scp3008GearRegistry;
import net.rai.scp_unleashed.scp3008.gear.carry.Scp3008Carry;
import net.rai.scp_unleashed.scp3008.gear.carry.Scp3008CarryParts;
import net.rai.scp_unleashed.scp3008.staff.Scp3008Staff;
import net.rai.scp_unleashed.scp3008.staff.Scp3008StaffRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp3008EconToolGameTests {
    public static final String Arena = Scp3008EconGameTests.Arena;
    public static final List<String> Multiblocks = List.of("scp3008_living_sofa", "scp3008_living_bookcase", "scp3008_bedroom_double_bed", "scp3008_bedroom_wardrobe",
            "scp3008_children_bunk_bed", "scp3008_lighting_floor_lamp", "scp3008_shelf_tall");
    public static final List<String> Storage = List.of("scp3008_kitchen_base_cabinet", "scp3008_bedroom_wardrobe", "scp3008_shelf_tall");
    private static final BlockPos Spot = new BlockPos(4, 2, 4);

    private Scp3008EconToolGameTests() {
    }

    private static Block piece(String Id) {
        return BuiltInRegistries.BLOCK.get(Scp3008EconRegistry.location(Id));
    }

    private static FakePlayer worker(GameTestHelper Helper, String Name) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Where = Helper.absoluteVec(new Vec3(4.5, 2.0, 1.5));
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        User.setGameMode(GameType.SURVIVAL);
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp3008EconRegistry.Screwdriver.get()));
        return User;
    }

    private static void clear(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        for (BlockPos Local : BlockPos.betweenClosed(new BlockPos(0, 2, 0), new BlockPos(8, 4, 8))) {
            BlockPos At = Helper.absolutePos(Local);
            if (!World.getBlockState(At).isAir()) {
                World.setBlock(At, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        for (ItemEntity Dropped : dropped(Helper)) {
            Dropped.discard();
        }
    }

    private static List<ItemEntity> dropped(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(Helper.absolutePos(BlockPos.ZERO)).inflate(12.0));
    }

    private static Map<Item, Integer> tally(GameTestHelper Helper) {
        Map<Item, Integer> Found = new HashMap<>();
        for (ItemEntity Dropped : dropped(Helper)) {
            Found.merge(Dropped.getItem().getItem(), Dropped.getItem().getCount(), Integer::sum);
        }
        return Found;
    }

    private static List<BlockPos> build(GameTestHelper Helper, Block Kind, BlockPos Local) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(Local);
        BlockState State = Kind.defaultBlockState();
        if (!Scp3008CarryParts.placeWhole(World, Pos, State) || !World.getBlockState(Pos).is(Kind)) {
            World.setBlock(Pos, State, Block.UPDATE_ALL);
            Scp3008CarryParts.placeWhole(World, Pos, World.getBlockState(Pos));
        }
        if (!World.getBlockState(Pos).is(Kind)) {
            return List.of();
        }
        return Scp3008Carry.partsOf(World, Pos, World.getBlockState(Pos));
    }

    private static BlockPos find(GameTestHelper Helper, Block Kind) {
        for (BlockPos Local : BlockPos.betweenClosed(new BlockPos(0, 2, 0), new BlockPos(8, 4, 8))) {
            BlockPos At = Helper.absolutePos(Local);
            if (Helper.getLevel().getBlockState(At).is(Kind)) {
                return At.immutable();
            }
        }
        return null;
    }

    private static void unscrew(FakePlayer User, BlockPos At) {
        User.gameMode.destroyBlock(At);
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_econ_flatpack_single")
    public static void screwdriverFlatPacksASinglePiece(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        clear(Helper);
        Block Table = piece("scp3008_living_side_table");
        List<BlockPos> Parts = build(Helper, Table, Spot);
        Helper.assertTrue(Parts.size() == 1, "the side table did not stand as a single piece: " + Parts);
        FakePlayer User = worker(Helper, "econ_single");
        unscrew(User, Parts.get(0));
        Helper.assertTrue(World.getBlockState(Parts.get(0)).isAir(), "the side table is still standing");
        Map<Item, Integer> Found = tally(Helper);
        Helper.assertTrue(Found.size() == 1 && Found.getOrDefault(Table.asItem(), 0) == 1, "unscrewing the side table dropped " + Found);
        Helper.assertTrue(User.getItemBySlot(EquipmentSlot.MAINHAND).getDamageValue() == 1, "the screwdriver did not wear");
        clear(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp3008_econ_flatpack_multi")
    public static void screwdriverFlatPacksEveryMultiblock(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = worker(Helper, "econ_multi");
        for (String Id : Multiblocks) {
            clear(Helper);
            Block Kind = piece(Id);
            Helper.assertTrue(Kind != Blocks.AIR, Id + " is not registered");
            List<BlockPos> Parts = build(Helper, Kind, Spot);
            Helper.assertTrue(Parts.size() >= 2, Id + " did not stand as a multiblock: " + Parts);
            unscrew(User, Parts.get(Parts.size() - 1));
            for (BlockPos At : Parts) {
                Helper.assertTrue(World.getBlockState(At).isAir(), Id + " left a part behind at " + At);
            }
            Map<Item, Integer> Found = tally(Helper);
            Helper.assertTrue(Found.size() == 1 && Found.getOrDefault(Kind.asItem(), 0) == 1, Id + " unscrewed into " + Found);
        }
        clear(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp3008_econ_flatpack_storage")
    public static void screwdriverSpillsContentsOnce(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = worker(Helper, "econ_storage");
        Item Snack = Scp3008EconRegistry.Crispbread.get();
        for (String Id : Storage) {
            clear(Helper);
            Block Kind = piece(Id);
            List<BlockPos> Parts = build(Helper, Kind, Spot);
            Helper.assertFalse(Parts.isEmpty(), Id + " could not be placed");
            Container Box = null;
            for (BlockPos At : Parts) {
                if (World.getBlockEntity(At) instanceof Container Found) {
                    Box = Found;
                    break;
                }
            }
            Helper.assertTrue(Box != null, Id + " has no storage");
            Box.setItem(0, new ItemStack(Snack, 7));
            Box.setItem(1, new ItemStack(Scp3008EconRegistry.Battery.get(), 2));
            unscrew(User, Parts.get(0));
            Map<Item, Integer> Found = tally(Helper);
            Helper.assertTrue(Found.getOrDefault(Snack, 0) == 7 && Found.getOrDefault(Scp3008EconRegistry.Battery.get(), 0) == 2,
                    Id + " did not spill its contents exactly once: " + Found);
            Helper.assertTrue(Found.getOrDefault(Kind.asItem(), 0) == 1 && Found.size() == 3, Id + " unscrewed into " + Found);
        }
        clear(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_econ_flatpack_salvage")
    public static void screwdriverSkipsSalvageDrops(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        clear(Helper);
        FakePlayer User = worker(Helper, "econ_salvage");
        BlockPos Pos = Helper.absolutePos(Spot);
        BlockState Table = Scp3008Registry.FoodTable.get().defaultBlockState();
        World.setBlock(Pos, Table, Block.UPDATE_ALL);
        List<ItemStack> Driven = Block.getDrops(Table, World, Pos, null, User, new ItemStack(Scp3008EconRegistry.Screwdriver.get()));
        List<ItemStack> Handed = Block.getDrops(Table, World, Pos, null, User, ItemStack.EMPTY);
        Helper.assertTrue(Driven.stream().noneMatch(Stack -> Stack.is(Scp3008GearRegistry.TableLeg.get())), "the salvage modifier still pulls legs for the screwdriver: " + Driven);
        Helper.assertTrue(Handed.stream().anyMatch(Stack -> Stack.is(Scp3008GearRegistry.TableLeg.get())), "bare hands no longer pull table legs: " + Handed);
        unscrew(User, Pos);
        Map<Item, Integer> Found = tally(Helper);
        Helper.assertTrue(Found.size() == 1 && Found.getOrDefault(Table.getBlock().asItem(), 0) == 1, "unscrewing a food table dropped " + Found);
        clear(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_econ_flatpack_guard")
    public static void screwdriverRespectsTheStore(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        clear(Helper);
        FakePlayer User = worker(Helper, "econ_guard");
        BlockPos Pos = Helper.absolutePos(Spot);
        for (Block Sealed : List.of(Scp3008Registry.Floor.get(), Scp3008Registry.Pillar.get(), Scp3008Registry.ExitDoor.get())) {
            World.setBlock(Pos, Sealed.defaultBlockState(), Block.UPDATE_ALL);
            Helper.assertTrue(Scp3008Disassembly.refused(World, User, Pos, World.getBlockState(Pos)), Sealed + " is not protected from the screwdriver");
            Helper.assertFalse(Scp3008Disassembly.unpack(User, Pos), Sealed + " was taken apart");
            Helper.assertTrue(World.getBlockState(Pos).is(Sealed), Sealed + " was removed");
        }
        World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        Helper.assertTrue(dropped(Helper).isEmpty(), "refusing the store blocks dropped items");
        BlockState Plain = piece("scp3008_kitchen_cart").defaultBlockState();
        Helper.assertFalse(Scp3008Disassembly.refused(World, User, Pos, Plain), "ordinary furniture was refused");
        clear(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp3008_econ_flatpack_place")
    public static void flatPackPlacesBackWhole(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = worker(Helper, "econ_place");
        for (String Id : List.of("scp3008_living_sofa", "scp3008_children_bunk_bed", "scp3008_bedroom_double_bed", "scp3008_shelf_tall")) {
            clear(Helper);
            Block Kind = piece(Id);
            List<BlockPos> Before = build(Helper, Kind, Spot);
            unscrew(User, Before.get(0));
            List<ItemEntity> Packed = dropped(Helper);
            Helper.assertTrue(Packed.size() == 1, Id + " did not pack into one item");
            ItemStack Stack = Packed.get(0).getItem().copy();
            Packed.get(0).discard();
            ItemStack Driver = User.getMainHandItem();
            User.setItemInHand(InteractionHand.MAIN_HAND, Stack);
            BlockPos Floor = Helper.absolutePos(Spot.below());
            BlockHitResult Hit = new BlockHitResult(Vec3.atCenterOf(Floor).add(0.0, 0.5, 0.0), Direction.UP, Floor, false);
            InteractionResult Placed = ((BlockItem) Stack.getItem()).place(new BlockPlaceContext(User, InteractionHand.MAIN_HAND, Stack, Hit));
            User.setItemInHand(InteractionHand.MAIN_HAND, Driver);
            Helper.assertTrue(Placed.consumesAction(), Id + " could not be placed back from its flat-pack");
            BlockPos Main = find(Helper, Kind);
            Helper.assertTrue(Main != null, Id + " is missing after placing it back");
            List<BlockPos> After = Scp3008Carry.partsOf(World, Main, World.getBlockState(Main));
            Helper.assertTrue(After.size() == Before.size(), Id + " came back with " + After.size() + " parts instead of " + Before.size());
        }
        clear(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_econ_set")
    public static void fullSetKeepsStaffFromNoticingFarAway(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp3008Staff Staff = Scp3008StaffRegistry.Staff.get().create(World);
        Helper.assertTrue(Staff != null, "no staff could be made");
        Vec3 Home = Helper.absoluteVec(new Vec3(4.5, 2.0, 4.5));
        Staff.moveTo(Home.x, Home.y, Home.z, 0.0f, 0.0f);
        World.addFreshEntity(Staff);
        FakePlayer User = worker(Helper, "econ_shopper");
        double Far = Scp3008Staff.FollowRange * Scp3008StoreArmorItem.NoticeShare + 6.0;
        User.moveTo(Home.x + Far, Home.y, Home.z, 0.0f, 0.0f);
        Staff.setTarget(User);
        Helper.assertTrue(Staff.getTarget() == User, "a plain shopper was not noticed");
        Staff.setTarget(null);
        User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Scp3008EconRegistry.ColanderHelmet.get()));
        User.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Scp3008EconRegistry.CushionVest.get()));
        User.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Scp3008EconRegistry.CardboardGreaves.get()));
        User.setItemSlot(EquipmentSlot.FEET, new ItemStack(Scp3008EconRegistry.FeltSlippers.get()));
        Helper.assertTrue(Scp3008StoreArmorItem.fullSet(User), "the full store set is not recognised");
        Staff.setTarget(User);
        Helper.assertTrue(Staff.getTarget() == null, "a padded shopper far away was still noticed");
        User.moveTo(Home.x + 8.0, Home.y, Home.z, 0.0f, 0.0f);
        Staff.setTarget(User);
        Helper.assertTrue(Staff.getTarget() == User, "a padded shopper close by went unnoticed");
        Staff.discard();
        Helper.succeed();
    }
}
