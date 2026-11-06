package net.rai.scp_unleashed.sulfur.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.pressure.PressureNetwork;
import net.rai.scp_unleashed.sulfur.SulfurOreBlock;
import net.rai.scp_unleashed.sulfur.SulfurRegistry;
import net.rai.scp_unleashed.sulfur.SulfurTags;
import net.rai.scp_unleashed.sulfur.acid.AcidContact;
import net.rai.scp_unleashed.sulfur.tank.IndustrialTankBlock;
import net.rai.scp_unleashed.sulfur.tank.IndustrialTankBlockEntity;
import net.rai.scp_unleashed.sulfur.tank.TankStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class SulfurGameTests {
    public static final String Arena = "sulfur_arena";
    public static final int Rolls = 40;
    public static final BlockState Wall = Blocks.STONE.defaultBlockState();

    private SulfurGameTests() {
    }

    private static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static int sulfurIn(List<ItemStack> Drops) {
        int Count = 0;
        for (ItemStack Stack : Drops) {
            if (Stack.is(SulfurRegistry.Sulfur.get())) {
                Count += Stack.getCount();
            }
        }
        return Count;
    }

    @GameTest(template = Arena, batch = "sulfur_ores")
    public static void sulfurOresDropSulfur(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Holder<Enchantment> Fortune = World.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.FORTUNE);
        Holder<Enchantment> Silk = World.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH);
        List<SulfurOreBlock> Ores = List.of(SulfurRegistry.Ore.get(), SulfurRegistry.DeepslateOre.get());
        for (int Index = 0; Index < Ores.size(); Index++) {
            SulfurOreBlock Ore = Ores.get(Index);
            BlockPos Relative = new BlockPos(2 + Index * 2, 2, 2);
            Helper.setBlock(Relative, Ore);
            BlockPos Pos = Helper.absolutePos(Relative);
            BlockState State = World.getBlockState(Pos);
            String Name = BuiltInRegistries.BLOCK.getKey(Ore).getPath();
            ItemStack Pick = new ItemStack(Items.STONE_PICKAXE);
            Helper.assertTrue(State.requiresCorrectToolForDrops(), Name + " drops without the right tool");
            Helper.assertTrue(Pick.isCorrectToolForDrops(State), "a stone pickaxe cannot harvest " + Name);
            Helper.assertFalse(new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(State), "a wooden pickaxe can harvest " + Name);
            ItemStack Lucky = Pick.copy();
            Lucky.enchant(Fortune, 3);
            int Plain = 0;
            int Boosted = 0;
            for (int Roll = 0; Roll < Rolls; Roll++) {
                List<ItemStack> Drops = Block.getDrops(State, World, Pos, null, null, Pick);
                int Count = sulfurIn(Drops);
                Helper.assertTrue(Drops.size() == 1 && Count >= 2 && Count <= 4, Name + " dropped " + Drops + " instead of 2 to 4 sulfur");
                Plain += Count;
                Boosted += sulfurIn(Block.getDrops(State, World, Pos, null, null, Lucky));
            }
            Helper.assertTrue(Boosted > Plain, "fortune did not raise the " + Name + " drops (" + Plain + " plain, " + Boosted + " with fortune)");
            ItemStack Silky = Pick.copy();
            Silky.enchant(Silk, 1);
            List<ItemStack> Whole = Block.getDrops(State, World, Pos, null, null, Silky);
            Helper.assertTrue(Whole.size() == 1 && Whole.get(0).is(Ore.asItem()), "silk touch on " + Name + " dropped " + Whole);
            int Experience = 0;
            for (int Roll = 0; Roll < Rolls; Roll++) {
                Experience += Ore.getExpDrop(State, World, Pos, null, null, Pick);
            }
            Helper.assertTrue(Experience >= Rolls * SulfurOreBlock.ExperienceMin, Name + " gave too little experience: " + Experience);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, batch = "sulfur_recipes")
    public static void sulfurRecipesWork(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        RecipeManager Recipes = World.getRecipeManager();
        ItemStack Sulfur = new ItemStack(SulfurRegistry.Sulfur.get());
        for (Item Fuel : List.of(Items.COAL, Items.CHARCOAL)) {
            CraftingInput Input = CraftingInput.of(2, 2, List.of(Sulfur.copy(), Sulfur.copy(), new ItemStack(Fuel), ItemStack.EMPTY));
            Optional<RecipeHolder<CraftingRecipe>> Found = Recipes.getRecipeFor(RecipeType.CRAFTING, Input, World);
            Helper.assertTrue(Found.isPresent(), "no gunpowder recipe from sulfur and " + Fuel);
            ItemStack Result = Found.get().value().assemble(Input, World.registryAccess());
            Helper.assertTrue(Result.is(Items.GUNPOWDER) && Result.getCount() == 2, "sulfur and " + Fuel + " made " + Result);
        }
        List<ItemStack> Nine = new ArrayList<>();
        for (int Slot = 0; Slot < 9; Slot++) {
            Nine.add(Sulfur.copy());
        }
        CraftingInput Packed = CraftingInput.of(3, 3, Nine);
        ItemStack Compressed = Recipes.getRecipeFor(RecipeType.CRAFTING, Packed, World).map(Found -> Found.value().assemble(Packed, World.registryAccess())).orElse(ItemStack.EMPTY);
        Helper.assertTrue(Compressed.is(SulfurRegistry.SulfurBlockItem.get()), "nine sulfur made " + Compressed);
        CraftingInput Single = CraftingInput.of(1, 1, List.of(new ItemStack(SulfurRegistry.SulfurBlockItem.get())));
        ItemStack Loose = Recipes.getRecipeFor(RecipeType.CRAFTING, Single, World).map(Found -> Found.value().assemble(Single, World.registryAccess())).orElse(ItemStack.EMPTY);
        Helper.assertTrue(Loose.is(SulfurRegistry.Sulfur.get()) && Loose.getCount() == 9, "a sulfur block broke into " + Loose);
        for (Item Ore : List.of(SulfurRegistry.OreItem.get(), SulfurRegistry.DeepslateOreItem.get())) {
            SingleRecipeInput Input = new SingleRecipeInput(new ItemStack(Ore));
            ItemStack Smelted = Recipes.getRecipeFor(RecipeType.SMELTING, Input, World).map(Found -> Found.value().assemble(Input, World.registryAccess())).orElse(ItemStack.EMPTY);
            ItemStack Blasted = Recipes.getRecipeFor(RecipeType.BLASTING, Input, World).map(Found -> Found.value().assemble(Input, World.registryAccess())).orElse(ItemStack.EMPTY);
            Helper.assertTrue(Smelted.is(SulfurRegistry.Sulfur.get()), "smelting " + Ore + " made " + Smelted);
            Helper.assertTrue(Blasted.is(SulfurRegistry.Sulfur.get()), "blasting " + Ore + " made " + Blasted);
        }
        Helper.assertTrue(Recipes.byKey(location("sulfur/" + SulfurRegistry.TankId)).isPresent(), "the Industrial Tank has no recipe");
        Helper.succeed();
    }

    @GameTest(template = Arena, batch = "sulfur_worldgen")
    public static void sulfurOreGenerates(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(World.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).containsKey(location("sulfur_ore")), "the sulfur ore placement is missing");
        Helper.assertTrue(World.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).containsKey(location("sulfur_ore_deep")), "the deep sulfur ore placement is missing");
        Helper.assertTrue(World.registryAccess().registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS).containsKey(location("sulfur_ores")),
                "the sulfur ore biome modifier is missing");
        ConfiguredFeature<?, ?> OreVein = World.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(location("sulfur_ore"));
        Helper.assertTrue(OreVein != null, "the sulfur ore feature is missing");
        for (int X = 3; X <= 11; X++) {
            for (int Y = 2; Y <= 6; Y++) {
                for (int Z = 3; Z <= 11; Z++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Y <= 3 ? Blocks.DEEPSLATE : Blocks.STONE);
                }
            }
        }
        BlockPos Centre = Helper.absolutePos(new BlockPos(7, 4, 7));
        RandomSource Rng = RandomSource.create(682L);
        int Placed = 0;
        for (int Attempt = 0; Attempt < 8 && Placed == 0; Attempt++) {
            OreVein.place(World, World.getChunkSource().getGenerator(), Rng, Centre);
            for (BlockPos Pos : BlockPos.betweenClosed(Helper.absolutePos(new BlockPos(3, 2, 3)), Helper.absolutePos(new BlockPos(11, 6, 11)))) {
                BlockState State = World.getBlockState(Pos);
                if (State.is(SulfurRegistry.Ore.get()) || State.is(SulfurRegistry.DeepslateOre.get())) {
                    Placed++;
                }
            }
        }
        Helper.assertTrue(Placed > 0, "the sulfur ore feature placed nothing in solid stone");
        Helper.succeed();
    }

    private static IndustrialTankBlockEntity tank(GameTestHelper Helper, BlockPos Relative) {
        Helper.setBlock(Relative, SulfurRegistry.Tank.get().defaultBlockState());
        return (IndustrialTankBlockEntity) Helper.getLevel().getBlockEntity(Helper.absolutePos(Relative));
    }

    private static void load(IndustrialTankBlockEntity Machine, int Sulfur, int Water) {
        if (Sulfur > 0) {
            Machine.getStorage().setStackInSlot(IndustrialTankBlockEntity.SulfurSlot, new ItemStack(SulfurRegistry.Sulfur.get(), Sulfur));
        }
        if (Water > 0) {
            Machine.getWaterTank().fill(new FluidStack(Fluids.WATER, Water), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @GameTest(template = Arena, batch = "industrial_tank_mixing", timeoutTicks = 1000)
    public static void industrialTankNeedsPowerSulfurAndWater(GameTestHelper Helper) {
        IndustrialTankBlockEntity Dark = tank(Helper, new BlockPos(2, 2, 2));
        IndustrialTankBlockEntity Dry = tank(Helper, new BlockPos(5, 2, 2));
        IndustrialTankBlockEntity Empty = tank(Helper, new BlockPos(8, 2, 2));
        IndustrialTankBlockEntity Ready = tank(Helper, new BlockPos(11, 2, 2));
        load(Dark, 6, 2000);
        load(Dry, 6, 0);
        load(Empty, 0, 2000);
        load(Ready, 6, 2000);
        Helper.onEachTick(() -> {
            for (IndustrialTankBlockEntity Machine : List.of(Dry, Empty, Ready)) {
                Machine.insertPower(IndustrialTankBlockEntity.WorkDraw);
            }
        });
        Helper.runAfterDelay(IndustrialTankBlockEntity.BatchTicks + 30, () -> {
            Helper.assertTrue(Dark.status() == TankStatus.Unpowered, "the unpowered tank is " + Dark.status());
            Helper.assertTrue(Dry.status() == TankStatus.NoWater, "the tank without water is " + Dry.status());
            Helper.assertTrue(Empty.status() == TankStatus.NoSulfur, "the tank without sulfur is " + Empty.status());
            for (IndustrialTankBlockEntity Machine : List.of(Dark, Dry, Empty)) {
                Helper.assertTrue(Machine.getAcidTank().isEmpty(), "a tank missing power, sulfur or water made acid (" + Machine.status() + ")");
            }
            Helper.assertTrue(Ready.getAcidTank().getFluidAmount() == IndustrialTankBlockEntity.BatchAcid, "the working tank holds " + Ready.getAcidTank().getFluidAmount() + " mB of acid");
            Helper.assertTrue(Ready.getAcidTank().getFluid().is(SulfurTags.Acid), "the working tank made " + Ready.getAcidTank().getFluid().getHoverName().getString());
            Helper.assertTrue(Ready.getWaterTank().getFluidAmount() == 1000, "the working tank used " + (2000 - Ready.getWaterTank().getFluidAmount()) + " mB of water");
            Helper.assertTrue(Ready.getStorage().getStackInSlot(IndustrialTankBlockEntity.SulfurSlot).getCount() == 3, "the working tank did not use exactly three sulfur");
            Helper.assertTrue(Ready.status() == TankStatus.Running, "the working tank is " + Ready.status());
            BlockState State = Helper.getLevel().getBlockState(Ready.getBlockPos());
            Helper.assertTrue(State.getValue(IndustrialTankBlock.Active) && State.getValue(IndustrialTankBlock.Fill) == 1, "the working tank shows " + State);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, batch = "industrial_tank_buckets")
    public static void industrialTankFillsBuckets(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Relative = new BlockPos(4, 2, 4);
        IndustrialTankBlockEntity Machine = tank(Helper, Relative);
        Machine.getAcidTank().fill(new FluidStack(SulfurRegistry.Acid.get(), 2000), IFluidHandler.FluidAction.EXECUTE);
        FluidActionResult Filled = FluidUtil.tryFillContainer(new ItemStack(Items.BUCKET), Machine.getValves(), FluidType.BUCKET_VOLUME, null, true);
        Helper.assertTrue(Filled.isSuccess() && Filled.getResult().is(SulfurRegistry.AcidBucket.get()), "an empty bucket came back as " + Filled.getResult());
        Helper.assertTrue(Machine.getAcidTank().getFluidAmount() == 1000, "filling a bucket left " + Machine.getAcidTank().getFluidAmount() + " mB");
        FluidActionResult Poured = FluidUtil.tryEmptyContainer(new ItemStack(Items.WATER_BUCKET), Machine.getValves(), FluidType.BUCKET_VOLUME, null, true);
        Helper.assertTrue(Poured.isSuccess() && Machine.getWaterTank().getFluidAmount() == 1000, "a water bucket did not pour into the tank");
        FluidActionResult Refused = FluidUtil.tryEmptyContainer(new ItemStack(SulfurRegistry.AcidBucket.get()), Machine.getValves(), FluidType.BUCKET_VOLUME, null, true);
        Helper.assertFalse(Refused.isSuccess(), "acid could be poured back into the water side");
        BlockPos Pos = Helper.absolutePos(Relative);
        Direction Front = World.getBlockState(Pos).getValue(IndustrialTankBlock.Facing);
        IFluidHandler Inlet = World.getCapability(Capabilities.FluidHandler.BLOCK, Pos, Front.getClockWise());
        IFluidHandler Outlet = World.getCapability(Capabilities.FluidHandler.BLOCK, Pos, Front.getOpposite());
        IFluidHandler Bottom = World.getCapability(Capabilities.FluidHandler.BLOCK, Pos, Direction.DOWN);
        IFluidHandler Face = World.getCapability(Capabilities.FluidHandler.BLOCK, Pos, Front);
        Helper.assertTrue(Inlet != null && Outlet != null && Bottom != null, "the tank is missing a pipe port");
        Helper.assertTrue(Face == null, "the front of the tank takes pipes");
        Helper.assertTrue(Inlet.fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.SIMULATE) == 500, "the side inlet refuses water");
        Helper.assertTrue(Inlet.drain(500, IFluidHandler.FluidAction.SIMULATE).isEmpty(), "acid can be drawn from the side inlet");
        Helper.assertTrue(Outlet.drain(500, IFluidHandler.FluidAction.SIMULATE).getAmount() == 500, "the back outlet gives no acid");
        Helper.assertTrue(Outlet.fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.SIMULATE) == 0, "the back outlet takes water");
        Helper.assertTrue(Bottom.drain(500, IFluidHandler.FluidAction.SIMULATE).getAmount() == 500, "the bottom outlet gives no acid");
        BlockPos Spill = Helper.absolutePos(new BlockPos(9, 2, 9));
        BlockState Before = World.getBlockState(Spill);
        Helper.assertTrue(SulfurRegistry.AcidBucket.get().emptyContents(null, World, Spill, null, Filled.getResult()), "the acid bucket would not pour out onto " + Before);
        FluidState Puddle = World.getFluidState(Spill);
        Helper.assertTrue(Puddle.is(SulfurTags.Acid) && Puddle.isSource(), "the poured acid is " + Puddle);
        BlockState Liquid = World.getBlockState(Spill);
        ItemStack Scooped = ((BucketPickup) Liquid.getBlock()).pickupBlock(null, World, Spill, Liquid);
        Helper.assertTrue(Scooped.is(SulfurRegistry.AcidBucket.get()), "scooping acid gave " + Scooped);
        Helper.assertTrue(World.getFluidState(Spill).isEmpty(), "scooping did not remove the acid");
        Helper.succeed();
    }

    private static void pool(GameTestHelper Helper, int X0, int Z0, int Size) {
        for (int X = X0 - 1; X <= X0 + Size; X++) {
            for (int Z = Z0 - 1; Z <= Z0 + Size; Z++) {
                boolean Edge = X < X0 || Z < Z0 || X >= X0 + Size || Z >= Z0 + Size;
                Helper.setBlock(new BlockPos(X, 2, Z), Edge ? Wall : SulfurRegistry.AcidBlock.get().defaultBlockState());
                Helper.setBlock(new BlockPos(X, 3, Z), Edge ? Wall : Blocks.AIR.defaultBlockState());
            }
        }
    }

    @GameTest(template = Arena, batch = "acid_contact", timeoutTicks = 200)
    public static void acidBurnsWithItsOwnDamage(GameTestHelper Helper) {
        pool(Helper, 3, 3, 3);
        Pig Swimmer = Helper.spawn(EntityType.PIG, new Vec3(4.5, 2.0, 4.5));
        Swimmer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        float Start = Swimmer.getHealth();
        Helper.runAfterDelay(30, () -> {
            DamageSource Last = Swimmer.getLastDamageSource();
            Helper.assertTrue(Swimmer.getHealth() < Start, "the pig in acid was not hurt");
            Helper.assertTrue(Last != null && Last.is(SulfurRegistry.AcidDamage), "the pig was hurt by " + (Last == null ? "nothing" : Last.getMsgId()));
            Helper.assertTrue(Swimmer.getItemBySlot(EquipmentSlot.HEAD).getDamageValue() > 0, "the helmet did not corrode");
            Helper.assertTrue(Swimmer.isInFluidType(SulfurRegistry.AcidType.get()), "the pig does not register as being in acid");
            Helper.assertTrue(Helper.getLevel().getFluidState(Swimmer.blockPosition()).is(SulfurTags.Acid), "the acid is missing its fluid tag");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, batch = "acid_contact", timeoutTicks = 200)
    public static void acidMeltsOrdinaryItems(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        pool(Helper, 3, 3, 4);
        Optional<HolderSet.Named<Item>> Proof = BuiltInRegistries.ITEM.getTag(SulfurTags.AcidProof);
        Helper.assertTrue(Proof.isPresent() && Proof.get().size() > 0, "the acid_proof tag is empty");
        Item Anomalous = Proof.get().get(0).value();
        Item Prefixed = BuiltInRegistries.ITEM.get(location("scp330_candy_red"));
        ItemEntity Cobble = Helper.spawnItem(Items.COBBLESTONE, 4.5f, 2.2f, 4.5f);
        ItemEntity Sword = Helper.spawnItem(Items.IRON_SWORD, 5.5f, 2.2f, 4.5f);
        ItemEntity Kept = Helper.spawnItem(Anomalous, 4.5f, 2.2f, 5.5f);
        ItemEntity Candy = Prefixed == Items.AIR ? null : Helper.spawnItem(Prefixed, 5.5f, 2.2f, 5.5f);
        for (ItemEntity Dropped : List.of(Cobble, Sword, Kept)) {
            Dropped.setPickUpDelay(400);
        }
        Helper.assertFalse(AcidContact.isAcidProof(new ItemStack(Items.COBBLESTONE)), "cobblestone counts as acid proof");
        Helper.assertTrue(AcidContact.isAcidProof(new ItemStack(Anomalous)), Anomalous + " from the acid_proof tag is not acid proof");
        Helper.runAfterDelay(AcidContact.MeltTicks + 40, () -> {
            Helper.assertTrue(Cobble.isRemoved(), "the cobblestone did not melt");
            Helper.assertTrue(Sword.isRemoved(), "the iron sword did not melt");
            Helper.assertTrue(Kept.isAlive(), "the anomalous " + Anomalous + " melted");
            if (Candy != null) {
                Helper.assertTrue(Candy.isAlive(), "the SCP-330 candy melted");
            }
            Helper.assertTrue(World.getFluidState(Helper.absolutePos(new BlockPos(4, 2, 4))).is(SulfurTags.Acid), "the acid pool vanished");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, batch = "acid_flow", timeoutTicks = 200)
    public static void acidLeavesBlocksAlone(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Source = new BlockPos(8, 2, 8);
        BlockPos Torch = new BlockPos(10, 2, 8);
        BlockPos Flower = new BlockPos(8, 2, 10);
        BlockPos Stone = new BlockPos(6, 2, 8);
        Helper.setBlock(Torch, Blocks.TORCH);
        Helper.setBlock(Flower.below(), Blocks.DIRT);
        Helper.setBlock(Flower, Blocks.POPPY);
        Helper.setBlock(Stone, Blocks.STONE);
        Helper.setBlock(Source, SulfurRegistry.AcidBlock.get().defaultBlockState());
        Helper.runAfterDelay(120, () -> {
            Helper.assertTrue(World.getBlockState(Helper.absolutePos(Torch)).is(Blocks.TORCH), "the acid washed away a torch, found " + World.getBlockState(Helper.absolutePos(Torch)));
            Helper.assertTrue(World.getBlockState(Helper.absolutePos(Flower)).is(Blocks.POPPY), "the acid destroyed a flower");
            Helper.assertTrue(World.getBlockState(Helper.absolutePos(Stone)).is(Blocks.STONE), "the acid broke stone");
            Helper.assertTrue(World.getBlockState(Helper.absolutePos(Source.below())).is(Blocks.SMOOTH_STONE), "the acid ate the floor");
            FluidState Far = World.getFluidState(Helper.absolutePos(new BlockPos(8, 2, 1)));
            FluidState Beyond = World.getFluidState(Helper.absolutePos(new BlockPos(8, 2, 0)));
            Helper.assertTrue(Far.is(SulfurTags.Acid) && !Far.isSource(), "the acid did not reach seven blocks like water");
            Helper.assertTrue(Beyond.isEmpty(), "the acid flowed further than water");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, batch = "acid_flow", timeoutTicks = 200)
    public static void acidMakesNoNewSources(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        for (int X = 1; X <= 7; X++) {
            Helper.setBlock(new BlockPos(X, 2, 1), Wall);
            Helper.setBlock(new BlockPos(X, 2, 3), Wall);
        }
        Helper.setBlock(new BlockPos(1, 2, 2), Wall);
        Helper.setBlock(new BlockPos(7, 2, 2), Wall);
        Helper.setBlock(new BlockPos(2, 2, 2), SulfurRegistry.AcidBlock.get().defaultBlockState());
        Helper.setBlock(new BlockPos(4, 2, 2), SulfurRegistry.AcidBlock.get().defaultBlockState());
        Helper.runAfterDelay(60, () -> {
            FluidState Gap = World.getFluidState(Helper.absolutePos(new BlockPos(3, 2, 2)));
            Helper.assertTrue(Gap.is(SulfurTags.Acid) && !Gap.isSource(), "acid between two sources became a new source, found " + Gap + " in " + World.getBlockState(Helper.absolutePos(new BlockPos(3, 2, 2))));
            Helper.assertFalse(SulfurRegistry.AcidType.get().canConvertToSource(new FluidStack(SulfurRegistry.Acid.get(), FluidType.BUCKET_VOLUME)),
                    "the acid fluid type allows new sources");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, batch = "acid_contract")
    public static void acidContractHolds(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(SulfurRegistry.Acid.get().defaultFluidState().is(SulfurTags.Acid), "the acid source is missing scp_unleashed:sulfuric_acid");
        Helper.assertTrue(SulfurRegistry.FlowingAcid.get().defaultFluidState().is(SulfurTags.Acid), "flowing acid is missing scp_unleashed:sulfuric_acid");
        Helper.assertTrue(SulfurRegistry.Acid.get().defaultFluidState().is(PressureNetwork.FinitePool), "pumps would treat acid pools as endless");
        Helper.assertTrue(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(SulfurRegistry.AcidDamage), "the sulfuric_acid damage type is missing");
        DamageSource Burn = AcidContact.source(World);
        Helper.assertTrue(Burn.is(SulfurRegistry.AcidDamage), "the acid damage source has the wrong type");
        Helper.assertTrue(Burn.is(DamageTypeTags.NO_KNOCKBACK), "acid damage knocks entities back");
        Helper.assertTrue(Burn.is(DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES), "animals do not panic in acid");
        Helper.assertTrue(BuiltInRegistries.ITEM.getTag(SulfurTags.AcidProof).map(HolderSet.Named::size).orElse(0) > 20, "the acid_proof tag is nearly empty");
        Helper.succeed();
    }
}
