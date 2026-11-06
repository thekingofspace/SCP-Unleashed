package net.rai.scp_unleashed.scp3008.plywood.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.econ.Scp3008EconRegistry;
import net.rai.scp_unleashed.scp3008.econ.gametest.Scp3008EconGameTests;
import net.rai.scp_unleashed.scp3008.plywood.Scp3008PlywoodChestBlock;
import net.rai.scp_unleashed.scp3008.plywood.Scp3008PlywoodChestEntity;
import net.rai.scp_unleashed.scp3008.plywood.Scp3008PlywoodRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp3008PlywoodGameTests {
    public static final String Arena = Scp3008EconGameTests.Arena;
    public static final int MinimumRecipes = 24;
    public static final int PlywoodPerCraft = 4;

    private Scp3008PlywoodGameTests() {
    }

    private static FakePlayer builder(GameTestHelper Helper, String Name) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Where = Helper.absoluteVec(new Vec3(4.5, 4.2, 4.5));
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static BlockPos spot(int Index) {
        return new BlockPos(1 + (Index % 4) * 2, 1, (Index / 4) * 2);
    }

    private static List<DeferredItem<? extends Item>> everything() {
        List<DeferredItem<? extends Item>> All = new ArrayList<>(Scp3008PlywoodRegistry.Placeables);
        All.addAll(Scp3008PlywoodRegistry.Parts);
        return All;
    }

    private static void use(ServerLevel World, FakePlayer User, BlockPos Pos) {
        World.getBlockState(Pos).useWithoutItem(World, User, new BlockHitResult(Vec3.atCenterOf(Pos), Direction.NORTH, Pos, false));
    }

    private static boolean open(ServerLevel World, BlockPos Pos) {
        return World.getBlockState(Pos).getValue(BlockStateProperties.OPEN);
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_plywood_place")
    public static void everyPieceIsPlacedFromItsItem(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = builder(Helper, "plywood_builder");
        int Index = 0;
        for (DeferredItem<? extends Item> Entry : Scp3008PlywoodRegistry.Placeables) {
            Item Made = Entry.get();
            Helper.assertTrue(Made instanceof BlockItem, Entry.getId() + " is not a block item");
            Block Kind = ((BlockItem) Made).getBlock();
            BlockPos Floor = Helper.absolutePos(spot(Index));
            ItemStack Stack = new ItemStack(Made);
            User.setItemInHand(InteractionHand.MAIN_HAND, Stack);
            BlockHitResult Hit = new BlockHitResult(Vec3.atCenterOf(Floor).add(0.0, 0.5, 0.0), Direction.UP, Floor, false);
            InteractionResult Result = Stack.useOn(new UseOnContext(User, InteractionHand.MAIN_HAND, Hit));
            Helper.assertTrue(World.getBlockState(Floor.above()).is(Kind), Entry.getId() + " was not placed from its item: " + Result + " on " + World.getBlockState(Floor)
                    + " under " + World.getBlockState(Floor.above()));
            Helper.assertTrue(Stack.isEmpty(), Entry.getId() + " was not used up when placed");
            Index++;
        }
        BlockPos DoorPos = Helper.absolutePos(spot(Scp3008PlywoodRegistry.Placeables.indexOf(Scp3008PlywoodRegistry.DoorItem)).above());
        Helper.assertTrue(World.getBlockState(DoorPos.above()).is(Scp3008PlywoodRegistry.Door.get()), "the door has no upper half");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_plywood_doors")
    public static void doorsTrapdoorsAndGatesOpenAndClose(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = builder(Helper, "plywood_doorman");
        BlockPos DoorPos = Helper.absolutePos(new BlockPos(2, 2, 4));
        BlockState Lower = Scp3008PlywoodRegistry.Door.get().defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        World.setBlock(DoorPos, Lower, Block.UPDATE_ALL);
        Scp3008PlywoodRegistry.Door.get().setPlacedBy(World, DoorPos, Lower, User, ItemStack.EMPTY);
        Helper.assertTrue(World.getBlockState(DoorPos.above()).is(Scp3008PlywoodRegistry.Door.get()), "the door has no upper half");
        use(World, User, DoorPos);
        Helper.assertTrue(open(World, DoorPos) && open(World, DoorPos.above()), "the door did not open by hand");
        use(World, User, DoorPos.above());
        Helper.assertTrue(!open(World, DoorPos) && !open(World, DoorPos.above()), "the door did not close by hand");
        World.setBlockAndUpdate(DoorPos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        Helper.assertTrue(open(World, DoorPos) && World.getBlockState(DoorPos).getValue(DoorBlock.POWERED), "the door ignores redstone");
        World.setBlockAndUpdate(DoorPos.east(), Blocks.AIR.defaultBlockState());
        Helper.assertTrue(!open(World, DoorPos), "the door stayed open without power");

        BlockPos HatchPos = Helper.absolutePos(new BlockPos(5, 2, 4));
        World.setBlockAndUpdate(HatchPos, Scp3008PlywoodRegistry.Trapdoor.get().defaultBlockState());
        use(World, User, HatchPos);
        Helper.assertTrue(open(World, HatchPos), "the trapdoor did not open by hand");
        use(World, User, HatchPos);
        Helper.assertTrue(!open(World, HatchPos), "the trapdoor did not close by hand");
        World.setBlockAndUpdate(HatchPos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        Helper.assertTrue(open(World, HatchPos) && World.getBlockState(HatchPos).getValue(TrapDoorBlock.POWERED), "the trapdoor ignores redstone");
        World.setBlockAndUpdate(HatchPos.east(), Blocks.AIR.defaultBlockState());
        Helper.assertTrue(!open(World, HatchPos), "the trapdoor stayed open without power");

        BlockPos GatePos = Helper.absolutePos(new BlockPos(4, 2, 7));
        World.setBlockAndUpdate(GatePos, Scp3008PlywoodRegistry.FenceGate.get().defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
        use(World, User, GatePos);
        Helper.assertTrue(open(World, GatePos), "the fence gate did not open by hand");
        use(World, User, GatePos);
        Helper.assertTrue(!open(World, GatePos), "the fence gate did not close by hand");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_plywood_fences")
    public static void fencesAndPanelsJoinUp(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Oak = Helper.absolutePos(new BlockPos(2, 2, 4));
        BlockPos First = Oak.east();
        BlockPos Second = First.east();
        BlockPos Gate = Second.east();
        World.setBlock(Oak, Blocks.OAK_FENCE.defaultBlockState(), Block.UPDATE_ALL);
        World.setBlock(First, Scp3008PlywoodRegistry.Fence.get().defaultBlockState(), Block.UPDATE_ALL);
        World.setBlock(Second, Scp3008PlywoodRegistry.Fence.get().defaultBlockState(), Block.UPDATE_ALL);
        World.setBlock(Gate, Scp3008PlywoodRegistry.FenceGate.get().defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        BlockPos PanelA = Helper.absolutePos(new BlockPos(3, 2, 7));
        BlockPos PanelB = PanelA.east();
        World.setBlock(PanelA, Scp3008PlywoodRegistry.Panel.get().defaultBlockState(), Block.UPDATE_ALL);
        World.setBlock(PanelB, Scp3008PlywoodRegistry.Panel.get().defaultBlockState(), Block.UPDATE_ALL);
        for (BlockPos Pos : List.of(Oak, First, Second, PanelA, PanelB)) {
            World.setBlock(Pos, Block.updateFromNeighbourShapes(World.getBlockState(Pos), World, Pos), Block.UPDATE_ALL);
        }
        Helper.assertTrue(World.getBlockState(First).getValue(FenceBlock.EAST) && World.getBlockState(Second).getValue(FenceBlock.WEST),
                "two plywood fences do not join");
        Helper.assertTrue(World.getBlockState(Second).getValue(FenceBlock.EAST), "the plywood fence does not join its gate");
        Helper.assertTrue(World.getBlockState(First).getValue(FenceBlock.WEST) && World.getBlockState(Oak).getValue(FenceBlock.EAST),
                "the plywood fence does not join other wooden fences");
        Helper.assertTrue(World.getBlockState(PanelA).getValue(IronBarsBlock.EAST) && World.getBlockState(PanelB).getValue(IronBarsBlock.WEST),
                "two plywood panels do not join");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_plywood_tags")
    public static void tagsGiveWoodBehaviourWithoutPlanks(GameTestHelper Helper) {
        List<String> Problems = new ArrayList<>();
        for (DeferredItem<? extends Item> Entry : Scp3008PlywoodRegistry.Placeables) {
            BlockState State = ((BlockItem) Entry.get()).getBlock().defaultBlockState();
            if (!State.is(BlockTags.MINEABLE_WITH_AXE)) {
                Problems.add(Entry.getId() + " is not axe mineable");
            }
            if (State.is(BlockTags.PLANKS) || State.is(BlockTags.LOGS) || new ItemStack(Entry.get()).is(ItemTags.PLANKS) || new ItemStack(Entry.get()).is(ItemTags.LOGS)) {
                Problems.add(Entry.getId() + " counts as vanilla planks or logs");
            }
        }
        Map<Block, List<TagKey<Block>>> Wanted = new HashMap<>();
        Wanted.put(Scp3008PlywoodRegistry.Door.get(), List.of(BlockTags.WOODEN_DOORS, BlockTags.DOORS));
        Wanted.put(Scp3008PlywoodRegistry.Trapdoor.get(), List.of(BlockTags.WOODEN_TRAPDOORS, BlockTags.TRAPDOORS));
        Wanted.put(Scp3008PlywoodRegistry.Fence.get(), List.of(BlockTags.WOODEN_FENCES, BlockTags.FENCES));
        Wanted.put(Scp3008PlywoodRegistry.FenceGate.get(), List.of(BlockTags.FENCE_GATES));
        Wanted.put(Scp3008PlywoodRegistry.Button.get(), List.of(BlockTags.WOODEN_BUTTONS, BlockTags.BUTTONS));
        Wanted.put(Scp3008PlywoodRegistry.PressurePlate.get(), List.of(BlockTags.WOODEN_PRESSURE_PLATES, BlockTags.PRESSURE_PLATES));
        Wanted.put(Scp3008PlywoodRegistry.Slab.get(), List.of(BlockTags.WOODEN_SLABS, BlockTags.SLABS));
        Wanted.put(Scp3008PlywoodRegistry.Stairs.get(), List.of(BlockTags.WOODEN_STAIRS, BlockTags.STAIRS));
        for (Map.Entry<Block, List<TagKey<Block>>> Entry : Wanted.entrySet()) {
            for (TagKey<Block> Tag : Entry.getValue()) {
                if (!Entry.getKey().defaultBlockState().is(Tag)) {
                    Problems.add(BuiltInRegistries.BLOCK.getKey(Entry.getKey()) + " is missing " + Tag.location());
                }
            }
        }
        Helper.assertTrue(Problems.isEmpty(), "tag problems: " + String.join("; ", Problems));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp3008_plywood_chest")
    public static void chestStoresKeepsAndDropsItsContents(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(new BlockPos(4, 2, 4));
        BlockState Placed = Scp3008PlywoodRegistry.Chest.get().defaultBlockState().setValue(Scp3008PlywoodChestBlock.FACING, Direction.SOUTH);
        World.setBlockAndUpdate(Pos, Placed);
        BlockEntity Found = World.getBlockEntity(Pos);
        Helper.assertTrue(Found instanceof Scp3008PlywoodChestEntity, "the chest has no storage: " + Found);
        Scp3008PlywoodChestEntity Storage = (Scp3008PlywoodChestEntity) Found;
        Helper.assertTrue(Storage instanceof RandomizableContainer, "the chest cannot take a loot table");
        Helper.assertTrue(Storage.getContainerSize() == 27, "the chest does not hold 27 stacks: " + Storage.getContainerSize());
        Helper.assertTrue(Placed.getAnalogOutputSignal(World, Pos) == 0, "an empty chest gives a comparator signal");
        Storage.setItem(0, new ItemStack(Scp3008EconRegistry.Particleboard.get(), 5));
        Storage.setItem(26, new ItemStack(Scp3008EconRegistry.HexFittings.get(), 3));
        Storage.setChanged();
        Helper.assertTrue(World.getBlockState(Pos).getAnalogOutputSignal(World, Pos) > 0, "a filled chest gives no comparator signal");
        CompoundTag Saved = Storage.saveWithFullMetadata(World.registryAccess());
        BlockEntity Copy = BlockEntity.loadStatic(Pos, World.getBlockState(Pos), Saved, World.registryAccess());
        Helper.assertTrue(Copy instanceof Scp3008PlywoodChestEntity Loaded && Loaded.getItem(0).getCount() == 5
                && Loaded.getItem(26).is(Scp3008EconRegistry.HexFittings.get()), "the chest lost its contents when saved and loaded");
        FakePlayer User = builder(Helper, "plywood_opener");
        Storage.startOpen(User);
        Helper.assertTrue(World.getBlockState(Pos).getValue(Scp3008PlywoodChestBlock.Open), "the lid did not open");
        Storage.stopOpen(User);
        Helper.assertTrue(!World.getBlockState(Pos).getValue(Scp3008PlywoodChestBlock.Open), "the lid did not close");
        World.destroyBlock(Pos, true);
        Helper.assertTrue(World.getBlockState(Pos).isAir(), "the chest was not broken");
        Map<Item, Integer> Dropped = new HashMap<>();
        for (ItemEntity Drop : World.getEntitiesOfClass(ItemEntity.class, new AABB(Pos).inflate(2.0))) {
            Dropped.merge(Drop.getItem().getItem(), Drop.getItem().getCount(), Integer::sum);
        }
        Helper.assertTrue(Dropped.getOrDefault(Scp3008EconRegistry.Particleboard.get(), 0) == 5 && Dropped.getOrDefault(Scp3008EconRegistry.HexFittings.get(), 0) == 3,
                "the chest did not drop its contents: " + Dropped);
        Helper.assertTrue(Dropped.getOrDefault(Scp3008PlywoodRegistry.ChestItem.get(), 0) == 1, "the chest did not drop itself: " + Dropped);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_plywood_recipes")
    public static void everyPieceHasAStoreOnlyRecipe(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Set<Item> Made = new HashSet<>();
        List<String> Problems = new ArrayList<>();
        int Count = 0;
        for (RecipeHolder<?> Known : World.getRecipeManager().getRecipes()) {
            ItemStack Result = Known.value().getResultItem(World.registryAccess());
            Made.add(Result.getItem());
            ResourceLocation Id = Known.id();
            if (!Id.getNamespace().equals(Scp_unleashed.ModId) || !Id.getPath().startsWith("scp3008_plywood")) {
                continue;
            }
            Count++;
            NonNullList<Ingredient> Parts = Known.value().getIngredients();
            if (Parts.stream().allMatch(Ingredient::isEmpty)) {
                Problems.add(Id + " has no ingredients");
            }
            for (Ingredient Part : Parts) {
                if (Part.isEmpty()) {
                    continue;
                }
                ItemStack[] Choices = Part.getItems();
                if (Choices.length == 0) {
                    Problems.add(Id + " has an ingredient that resolves to nothing");
                }
                for (ItemStack Choice : Choices) {
                    if (BuiltInRegistries.ITEM.getKey(Choice.getItem()).getNamespace().equals("minecraft")) {
                        Problems.add(Id + " takes " + BuiltInRegistries.ITEM.getKey(Choice.getItem()));
                    }
                }
            }
            if (Result.isEmpty() || BuiltInRegistries.ITEM.getKey(Result.getItem()).getNamespace().equals("minecraft")) {
                Problems.add(Id + " makes " + Result);
            }
        }
        Helper.assertTrue(Count >= MinimumRecipes, "only " + Count + " plywood recipes were found");
        Helper.assertTrue(Problems.isEmpty(), "plywood recipe problems: " + String.join("; ", Problems));
        List<String> Missing = new ArrayList<>();
        for (DeferredItem<? extends Item> Entry : everything()) {
            if (!Made.contains(Entry.get())) {
                Missing.add(Entry.getId().toString());
            }
        }
        Helper.assertTrue(Missing.isEmpty(), "no recipe makes " + String.join(", ", Missing));
        ItemStack Board = new ItemStack(Scp3008EconRegistry.Particleboard.get());
        CraftingInput Pair = CraftingInput.of(1, 2, List.of(Board.copy(), Board.copy()));
        Optional<RecipeHolder<CraftingRecipe>> Glued = World.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, Pair, World);
        Helper.assertTrue(Glued.isPresent(), "two particleboards make no plywood");
        ItemStack Sheets = Glued.get().value().assemble(Pair, World.registryAccess());
        Helper.assertTrue(Sheets.is(Scp3008PlywoodRegistry.PlywoodItem.get()) && Sheets.getCount() == PlywoodPerCraft, "two particleboards made " + Sheets);
        ItemStack Sheet = new ItemStack(Scp3008PlywoodRegistry.PlywoodItem.get());
        ItemStack Fitting = new ItemStack(Scp3008EconRegistry.HexFittings.get());
        CraftingInput Ring = CraftingInput.of(3, 3, List.of(Sheet.copy(), Sheet.copy(), Sheet.copy(), Sheet.copy(), Fitting, Sheet.copy(), Sheet.copy(), Sheet.copy(), Sheet.copy()));
        Optional<RecipeHolder<CraftingRecipe>> Boxed = World.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, Ring, World);
        Helper.assertTrue(Boxed.isPresent() && Boxed.get().value().assemble(Ring, World.registryAccess()).is(Scp3008PlywoodRegistry.ChestItem.get()),
                "eight plywood around a fitting make no chest");
        Helper.succeed();
    }
}
