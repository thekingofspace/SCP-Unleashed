package net.rai.scp_unleashed.disperser.gametest;

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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.disperser.DisperserBlock;
import net.rai.scp_unleashed.disperser.DisperserBlockEntity;
import net.rai.scp_unleashed.disperser.DisperserRegistry;
import net.rai.scp_unleashed.disperser.DisperserTuning;
import net.rai.scp_unleashed.disperser.OrganicGrenadeEntity;
import net.rai.scp_unleashed.disperser.OrganicGrenadeItem;
import net.rai.scp_unleashed.disperser.OrganicGrenadeRecipe;
import net.rai.scp_unleashed.disperser.OrganismCloud;
import net.rai.scp_unleashed.grenade.GrenadeEntity;
import net.rai.scp_unleashed.grenade.GrenadeTuning;
import net.rai.scp_unleashed.labware.LabContamination;
import net.rai.scp_unleashed.labware.LabOrganism;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.labware.LabVials;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp008.Scp008Organism;
import net.rai.scp_unleashed.scp020.Scp020Organism;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class DisperserGameTests {
    public static final String Arena = "disperser_arena";
    public static final double Floor = 2.0;
    public static final BlockPos MachinePos = new BlockPos(10, 2, 10);
    public static final Vec3 BurstSpot = new Vec3(10.5, Floor, 10.5);
    public static final int Settle = 4;

    private DisperserGameTests() {
    }

    private static ServerPlayer subject(GameTestHelper Helper, String Name, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, List<ServerPlayer> Users) {
        for (ServerPlayer User : Users) {
            if (!User.isRemoved()) {
                Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
            }
        }
    }

    private static void clearClouds(GameTestHelper Helper) {
        for (OrganismCloud Cloud : clouds(Helper)) {
            Cloud.discard();
        }
    }

    private static List<OrganismCloud> clouds(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(OrganismCloud.class, Helper.getBounds());
    }

    private static ItemStack gear(String Name) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    private static void suitUp(ServerPlayer User) {
        User.setItemSlot(EquipmentSlot.HEAD, gear("hazmat_hood"));
        User.setItemSlot(EquipmentSlot.CHEST, gear("hazmat_suit"));
        User.setItemSlot(EquipmentSlot.LEGS, gear("hazmat_trousers"));
        User.setItemSlot(EquipmentSlot.FEET, gear("hazmat_boots"));
    }

    private static LabOrganism organism(ResourceLocation Id) {
        return LabOrganisms.find(Id).orElseThrow();
    }

    private static boolean carries(LabOrganism Organism, ServerPlayer User) {
        return Organism.isPresentOn(User);
    }

    private static OrganicGrenadeEntity grenade(GameTestHelper Helper, ResourceLocation Culture) {
        Vec3 Spot = Helper.absoluteVec(BurstSpot);
        OrganicGrenadeEntity Grenade = OrganicGrenadeEntity.dispensed(Helper.getLevel(), Spot.x, Spot.y + GrenadeEntity.CentreLift + 0.01, Spot.z, Culture);
        Helper.getLevel().addFreshEntity(Grenade);
        return Grenade;
    }

    private static DisperserBlockEntity disperser(GameTestHelper Helper, BlockPos Pos, ResourceLocation Culture) {
        Helper.setBlock(Pos, DisperserRegistry.Disperser.get().defaultBlockState());
        BlockEntity Found = Helper.getBlockEntity(Pos);
        if (!(Found instanceof DisperserBlockEntity Machine)) {
            throw new IllegalStateException("no disperser block entity");
        }
        if (Culture != null) {
            Machine.swap(LabVials.sealedVial(Culture));
        }
        return Machine;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "organicgrenade_loading")
    public static void organicgrenadeLoadingKeepsCulture(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        OrganicGrenadeRecipe Recipe = new OrganicGrenadeRecipe(CraftingBookCategory.MISC);
        ItemStack Empty = new ItemStack(DisperserRegistry.OrganicGrenade.get());
        ItemStack Vial = LabVials.sealedVial(Scp008Organism.Id);
        CraftingInput Input = CraftingInput.of(2, 1, List.of(Empty, Vial));
        Helper.assertTrue(Recipe.matches(Input, World), "an empty grenade and a culture vial do not combine");
        Optional<RecipeHolder<CraftingRecipe>> Found = World.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, Input, World);
        Helper.assertTrue(Found.isPresent() && Found.get().value() instanceof OrganicGrenadeRecipe, "the loading recipe is not in the recipe manager");
        ItemStack Loaded = Found.get().value().assemble(Input, World.registryAccess());
        Helper.assertTrue(Loaded.is(DisperserRegistry.OrganicGrenade.get()) && Loaded.getCount() == 1, "loading did not give one organic grenade");
        Helper.assertTrue(Scp008Organism.Id.equals(OrganicGrenadeItem.cultureOf(Loaded)), "the loaded grenade forgot its culture");
        NonNullList<ItemStack> Left = Recipe.getRemainingItems(Input);
        Helper.assertTrue(LabVials.isVial(Left.get(1)) && !LabVials.hasSample(Left.get(1)), "the vial did not come back empty");
        Helper.assertTrue(Left.get(0).isEmpty(), "the grenade slot left something behind");
        Helper.assertFalse(Recipe.matches(CraftingInput.of(2, 1, List.of(Loaded, Vial)), World), "a loaded grenade can be loaded again");
        Helper.assertFalse(Recipe.matches(CraftingInput.of(2, 1, List.of(Empty, LabVials.sealedVial(null))), World), "an empty vial loads a grenade");
        Helper.assertFalse(Recipe.matches(CraftingInput.of(3, 1, List.of(Empty, Vial, LabVials.sealedVial(Scp008Organism.Id))), World), "two vials load one grenade");
        ItemStack Opened = LabVials.openVial(Scp008Organism.Id);
        Helper.assertTrue(Recipe.matches(CraftingInput.of(2, 1, List.of(Opened, Empty)), World), "an open culture vial does not load a grenade");
        CompoundTag SavedStack = (CompoundTag) Loaded.save(World.registryAccess());
        ItemStack Restored = ItemStack.parseOptional(World.registryAccess(), SavedStack);
        Helper.assertTrue(Scp008Organism.Id.equals(OrganicGrenadeItem.cultureOf(Restored)), "the culture did not survive saving the item");
        Vec3 Spot = Helper.absoluteVec(BurstSpot);
        OrganicGrenadeItem Launcher = (OrganicGrenadeItem) Loaded.getItem();
        OrganicGrenadeEntity Thrown = (OrganicGrenadeEntity) Launcher.asProjectile(World, Spot, Loaded, Direction.UP);
        Helper.assertTrue(Scp008Organism.Id.equals(Thrown.getCulture()), "the dispensed grenade forgot its culture");
        CompoundTag SavedEntity = new CompoundTag();
        Thrown.saveWithoutId(SavedEntity);
        OrganicGrenadeEntity Copy = new OrganicGrenadeEntity(DisperserRegistry.OrganicGrenadeType.get(), World);
        Copy.load(SavedEntity);
        Helper.assertTrue(Scp008Organism.Id.equals(Copy.getCulture()), "the thrown grenade forgot its culture across a save");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 320, batch = "organicgrenade_burst")
    public static void organicgrenadeBurstInfects(GameTestHelper Helper) {
        LabOrganism Organism = organism(Scp008Organism.Id);
        ServerPlayer Near = subject(Helper, "organic_near", 12.5, 12.5);
        ServerPlayer Edge = subject(Helper, "organic_edge", 10.5, 14.0);
        ServerPlayer Far = subject(Helper, "organic_far", 10.5, 18.5);
        ServerPlayer Late = subject(Helper, "organic_late", 3.5, 3.5);
        List<ServerPlayer> Users = List.of(Near, Edge, Far, Late);
        OrganicGrenadeEntity Grenade = grenade(Helper, Scp008Organism.Id);
        Helper.startSequence()
                .thenExecuteAfter(GrenadeTuning.FuseTicks - 10, () -> {
                    Helper.assertTrue(clouds(Helper).isEmpty(), "the grenade burst before its fuse ran out");
                    Helper.assertFalse(carries(Organism, Near), "the culture escaped before the burst");
                })
                .thenWaitUntil(() -> Helper.assertTrue(Grenade.isRemoved() && !clouds(Helper).isEmpty(), "the grenade has not burst into a cloud"))
                .thenExecuteAfter(Settle, () -> {
                    Helper.assertTrue(carries(Organism, Near), "a player two blocks from the burst was not infected");
                    Helper.assertTrue(carries(Organism, Edge), "a player at the edge of the cloud was not infected");
                    Helper.assertFalse(carries(Organism, Far), "a player eight blocks away was infected");
                    OrganismCloud Cloud = clouds(Helper).get(0);
                    Helper.assertTrue(Organism.getId().equals(Cloud.getCulture()), "the cloud carries the wrong organism");
                    Late.moveTo(Cloud.getX() + 1.5, Cloud.getY(), Cloud.getZ() - 1.5, 0.0f, 0.0f);
                })
                .thenExecuteAfter(DisperserTuning.CloudPulseTicks + Settle, () -> {
                    Helper.assertTrue(carries(Organism, Late), "a player who walked into the lingering cloud was not infected");
                    Helper.assertTrue(clouds(Helper).get(0).getPulses() >= 2, "the cloud is not pulsing");
                })
                .thenWaitUntil(() -> Helper.assertTrue(clouds(Helper).isEmpty(), "the cloud never cleared"))
                .thenExecute(() -> dismiss(Helper, Users))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "organicgrenade_every")
    public static void organicgrenadeCarriesEveryOrganism(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        List<String> Missed = new ArrayList<>();
        for (LabOrganism Organism : LabOrganisms.all()) {
            ServerPlayer User = subject(Helper, "organic_every", 12.0, 11.5);
            OrganismCloud Cloud = OrganismCloud.spawn(World, Helper.absoluteVec(BurstSpot), Organism);
            if (!carries(Organism, User)) {
                Missed.add(Organism.getId().toString());
            }
            Cloud.discard();
            dismiss(Helper, List.of(User));
        }
        Helper.assertTrue(Missed.isEmpty(), "the cloud did not infect with " + Missed);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "organicgrenade_gear")
    public static void organicgrenadeGearProtects(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        LabOrganism Plague = organism(Scp008Organism.Id);
        ServerPlayer Masked = subject(Helper, "organic_masked", 12.0, 10.5);
        Masked.setItemSlot(EquipmentSlot.HEAD, gear("gas_mask"));
        ServerPlayer Bare = subject(Helper, "organic_bare", 9.0, 10.5);
        OrganismCloud Cloud = OrganismCloud.spawn(World, Helper.absoluteVec(BurstSpot), Plague);
        Helper.assertTrue(carries(Plague, Masked), "a gas mask alone kept SCP-008 out of the cloud");
        Helper.assertTrue(carries(Plague, Bare), "the unmasked player beside them was not infected");
        Cloud.discard();
        dismiss(Helper, List.of(Masked, Bare));
        LabOrganism Mold = organism(Scp020Organism.Id);
        ServerPlayer MoldMasked = subject(Helper, "organic_mold_masked", 12.0, 10.5);
        MoldMasked.setItemSlot(EquipmentSlot.HEAD, gear("gas_mask"));
        ServerPlayer MoldBare = subject(Helper, "organic_mold_bare", 9.0, 10.5);
        OrganismCloud MoldCloud = OrganismCloud.spawn(World, Helper.absoluteVec(BurstSpot), Mold);
        Helper.assertFalse(carries(Mold, MoldMasked), "a gas mask did not keep SCP-020 out of the cloud");
        Helper.assertTrue(LabContamination.carries(MoldMasked, Scp020Organism.Id), "the masked player did not pick SCP-020 up on their gear");
        Helper.assertTrue(carries(Mold, MoldBare), "the unmasked player beside them was not afflicted by SCP-020");
        MoldCloud.discard();
        dismiss(Helper, List.of(MoldMasked, MoldBare));
        List<String> Leaked = new ArrayList<>();
        for (LabOrganism Organism : LabOrganisms.all()) {
            ServerPlayer Suited = subject(Helper, "organic_suited", 12.0, 11.5);
            suitUp(Suited);
            OrganismCloud Each = OrganismCloud.spawn(World, Helper.absoluteVec(BurstSpot), Organism);
            Each.pulse(World);
            if (carries(Organism, Suited)) {
                Leaked.add(Organism.getId().toString());
            }
            Each.discard();
            dismiss(Helper, List.of(Suited));
        }
        Helper.assertTrue(Leaked.isEmpty(), "a full hazmat suit let these through: " + Leaked);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "organicgrenade_empty")
    public static void organicgrenadeEmptyFizzles(GameTestHelper Helper) {
        ServerPlayer Near = subject(Helper, "organic_fizzle", 11.5, 11.5);
        OrganicGrenadeEntity Grenade = grenade(Helper, null);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Grenade.isRemoved(), "the empty grenade did not go off"))
                .thenExecuteAfter(Settle, () -> {
                    Helper.assertTrue(clouds(Helper).isEmpty(), "an empty grenade left a cloud");
                    Helper.assertTrue(LabOrganisms.detect(Near).isEmpty(), "an empty grenade infected someone");
                    Helper.assertTrue(Near.getHealth() >= Near.getMaxHealth(), "an empty grenade hurt someone");
                    dismiss(Helper, List.of(Near));
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "disperser_pulse")
    public static void disperserPulsesEndlessly(GameTestHelper Helper) {
        LabOrganism Organism = organism(Scp008Organism.Id);
        DisperserBlockEntity Machine = disperser(Helper, MachinePos, Scp008Organism.Id);
        BlockPos Switch = MachinePos.east();
        Helper.setBlock(Switch, Blocks.REDSTONE_BLOCK);
        ServerPlayer Near = subject(Helper, "disperser_near", 10.5, 15.0);
        ServerPlayer Masked = subject(Helper, "disperser_masked", 6.0, 10.5);
        Masked.setItemSlot(EquipmentSlot.HEAD, gear("gas_mask"));
        ServerPlayer Suited = subject(Helper, "disperser_suited", 10.5, 6.0);
        suitUp(Suited);
        ServerPlayer Far = subject(Helper, "disperser_far", 10.5, 18.5);
        List<ServerPlayer> Users = List.of(Near, Masked, Suited, Far);
        Helper.onEachTick(() -> Machine.insertPower(DisperserTuning.PowerDraw));
        int[] Seen = new int[1];
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Machine.getPulses() >= 1, "the disperser never pulsed"))
                .thenExecute(() -> {
                    Helper.assertTrue(carries(Organism, Near), "a player inside the disperser's reach was not infected");
                    Helper.assertTrue(carries(Organism, Masked), "a gas mask alone kept the disperser's SCP-008 out");
                    Helper.assertFalse(carries(Organism, Suited), "a full hazmat suit did not keep the disperser's SCP-008 out");
                    Helper.assertFalse(carries(Organism, Far), "a player beyond the disperser's reach was infected");
                    Helper.assertBlockProperty(MachinePos, DisperserBlock.Active, true);
                })
                .thenWaitUntil(() -> Helper.assertTrue(Machine.getPulses() >= 4, "the disperser stopped pulsing"))
                .thenExecute(() -> {
                    ItemStack Vial = Machine.getVial();
                    Helper.assertTrue(LabVials.isVial(Vial) && Vial.getCount() == 1 && Scp008Organism.Id.equals(LabVials.sampleOf(Vial)), "the vial was used up or changed");
                    Helper.setBlock(Switch, Blocks.AIR);
                })
                .thenExecuteAfter(Settle, () -> {
                    Helper.assertBlockProperty(MachinePos, DisperserBlock.Active, false);
                    Seen[0] = Machine.getPulses();
                })
                .thenExecuteAfter(DisperserTuning.PulseTicks * 2, () -> {
                    Helper.assertTrue(Machine.getPulses() == Seen[0], "the disperser kept pulsing without a redstone signal");
                    Helper.assertTrue(Machine.getCulture() != null, "the disperser lost its vial");
                    dismiss(Helper, Users);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "disperser_power")
    public static void disperserNeedsPowerAndSignal(GameTestHelper Helper) {
        DisperserBlockEntity Unpowered = disperser(Helper, MachinePos, Scp008Organism.Id);
        DisperserBlockEntity Unsignalled = disperser(Helper, new BlockPos(4, 2, 4), Scp008Organism.Id);
        DisperserBlockEntity Empty = disperser(Helper, new BlockPos(16, 2, 4), null);
        Helper.setBlock(MachinePos.east(), Blocks.REDSTONE_BLOCK);
        Helper.setBlock(new BlockPos(17, 2, 4), Blocks.REDSTONE_BLOCK);
        Helper.onEachTick(() -> {
            Unsignalled.insertPower(DisperserTuning.PowerDraw);
            Empty.insertPower(DisperserTuning.PowerDraw);
        });
        Helper.startSequence()
                .thenExecuteAfter(DisperserTuning.PulseTicks + DisperserTuning.WarmUpTicks + Settle, () -> {
                    Helper.assertTrue(Unpowered.getPulses() == 0, "the disperser ran without power");
                    Helper.assertTrue(Unpowered.getPowerDemand() == DisperserTuning.PowerDraw, "a signalled, loaded disperser does not ask for power");
                    Helper.assertTrue(Unsignalled.getPulses() == 0, "the disperser ran without a redstone signal");
                    Helper.assertTrue(Unsignalled.getPowerDemand() == 0, "an unsignalled disperser draws power");
                    Helper.assertTrue(Empty.getPulses() == 0, "an empty disperser pulsed");
                    Helper.assertTrue(Empty.getPowerDemand() == 0, "an empty disperser draws power");
                    Helper.assertBlockProperty(MachinePos, DisperserBlock.Active, false);
                    Helper.setBlock(new BlockPos(5, 2, 4), Blocks.REDSTONE_BLOCK);
                })
                .thenWaitUntil(() -> Helper.assertTrue(Unsignalled.getPulses() >= 1, "a powered disperser did not start once it got a signal"))
                .thenExecute(() -> Helper.assertTrue(Unpowered.getPulses() == 0, "the unpowered disperser pulsed"))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "disperser_save")
    public static void disperserSavesVial(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        DisperserBlockEntity Machine = disperser(Helper, MachinePos, Scp008Organism.Id);
        BlockPos Absolute = Helper.absolutePos(MachinePos);
        BlockState State = World.getBlockState(Absolute);
        Helper.assertTrue(State.getValue(DisperserBlock.Loaded), "loading a vial did not mark the disperser loaded");
        CompoundTag Saved = Machine.saveWithFullMetadata(World.registryAccess());
        BlockEntity Restored = BlockEntity.loadStatic(Absolute, State, Saved, World.registryAccess());
        Helper.assertTrue(Restored instanceof DisperserBlockEntity, "the saved disperser did not load back");
        DisperserBlockEntity Copy = (DisperserBlockEntity) Restored;
        Helper.assertTrue(Scp008Organism.Id.equals(Copy.getCulture()), "the vial did not survive a save and load");
        Helper.assertTrue(Copy.getVial().getCount() == 1, "the saved vial count changed");
        World.destroyBlock(Absolute, true);
        List<ItemEntity> Drops = World.getEntitiesOfClass(ItemEntity.class, new AABB(Absolute).inflate(2.0));
        boolean Dropped = Drops.stream().anyMatch(Drop -> LabVials.isVial(Drop.getItem()) && Scp008Organism.Id.equals(LabVials.sampleOf(Drop.getItem())));
        Helper.assertTrue(Dropped, "breaking the disperser lost the vial");
        for (ItemEntity Drop : Drops) {
            Drop.discard();
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "disperser_hopper")
    public static void disperserHopperInput(GameTestHelper Helper) {
        BlockPos Machine = new BlockPos(10, 3, 10);
        BlockPos Above = Machine.above();
        BlockPos Below = Machine.below();
        DisperserBlockEntity Disperser = disperser(Helper, Machine, null);
        Helper.setBlock(Above, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        Helper.setBlock(Below, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
        HopperBlockEntity Feeder = (HopperBlockEntity) Helper.getBlockEntity(Above);
        HopperBlockEntity Drain = (HopperBlockEntity) Helper.getBlockEntity(Below);
        Feeder.setItem(0, new ItemStack(Items.DIRT));
        Feeder.setItem(1, LabVials.sealedVial(null));
        Feeder.setItem(2, LabVials.sealedVial(Scp008Organism.Id));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Scp008Organism.Id.equals(Disperser.getCulture()), "the hopper did not load the culture vial"))
                .thenExecuteAfter(40, () -> {
                    Helper.assertTrue(Feeder.getItem(0).is(Items.DIRT), "the hopper pushed dirt into the disperser");
                    Helper.assertTrue(LabVials.isEmptyVial(Feeder.getItem(1)), "the hopper pushed an empty vial into the disperser");
                    Helper.assertTrue(Scp008Organism.Id.equals(Disperser.getCulture()), "the vial left the disperser");
                    Helper.assertTrue(Drain.isEmpty(), "a hopper underneath pulled the vial out");
                    Helper.assertBlockProperty(Machine, DisperserBlock.Loaded, true);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "disperser_tabs")
    public static void disperserCreativeTabs(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        CreativeModeTab.ItemDisplayParameters Parameters = new CreativeModeTab.ItemDisplayParameters(World.enabledFeatures(), true, World.registryAccess());
        CreativeModeTab Machines = ModCreativeTabs.Machines.get();
        CreativeModeTab Weapons = ModCreativeTabs.Weapons.get();
        Machines.buildContents(Parameters);
        Weapons.buildContents(Parameters);
        Helper.assertTrue(Machines.getDisplayItems().stream().anyMatch(Stack -> Stack.is(DisperserRegistry.DisperserItem.get())), "the disperser is missing from Machines");
        List<ItemStack> Listed = new ArrayList<>(Weapons.getDisplayItems());
        List<Integer> Grenades = new ArrayList<>();
        for (int Index = 0; Index < Listed.size(); Index++) {
            if (BuiltInRegistries.ITEM.getKey(Listed.get(Index).getItem()).getPath().endsWith("_grenade")) {
                Grenades.add(Index);
            }
        }
        Helper.assertTrue(Listed.stream().anyMatch(Stack -> OrganicGrenadeItem.isEmptyGrenade(Stack)), "the empty organic grenade is missing from Weapons");
        Helper.assertTrue(Listed.stream().anyMatch(Stack -> Scp008Organism.Id.equals(OrganicGrenadeItem.cultureOf(Stack))), "a loaded organic grenade is missing from Weapons");
        Helper.assertTrue(Grenades.get(Grenades.size() - 1) - Grenades.get(0) == Grenades.size() - 1, "the organic grenades are not grouped with the other grenades");
        Helper.succeed();
    }
}
