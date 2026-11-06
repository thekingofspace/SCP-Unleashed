package net.rai.scp_unleashed.gateway.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.block.OutletBlock;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.gateway.ContainedGatewayBlock;
import net.rai.scp_unleashed.gateway.ContainedGatewayBlockEntity;
import net.rai.scp_unleashed.gateway.DimensionalShardItem;
import net.rai.scp_unleashed.gateway.GatewayDestination;
import net.rai.scp_unleashed.gateway.GatewayDestinations;
import net.rai.scp_unleashed.gateway.GatewayEvents;
import net.rai.scp_unleashed.gateway.GatewayRegistry;
import net.rai.scp_unleashed.gateway.GatewayStash;
import net.rai.scp_unleashed.gateway.GatewayTravel;
import net.rai.scp_unleashed.registry.ModBlocks;
import net.rai.scp_unleashed.scp4666.Scp4666Destination;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.wire.WireLink;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import net.rai.scp_unleashed.wire.WireTier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class GatewayGameTests {
    public static final String Arena = "gateway_arena";
    public static final double Floor = 2.0;
    public static final Item[] Pocket = {Items.COBBLESTONE, Items.DIRT, Items.OAK_LOG, Items.BREAD, Items.TORCH, Items.ARROW, Items.STRING, Items.COAL, Items.IRON_INGOT,
            Items.GOLD_INGOT, Items.REDSTONE, Items.LAPIS_LAZULI, Items.EMERALD, Items.DIAMOND, Items.APPLE, Items.CARROT, Items.POTATO, Items.WHEAT, Items.SUGAR, Items.PAPER,
            Items.BOOK, Items.FEATHER, Items.FLINT, Items.GUNPOWDER, Items.BONE, Items.CLAY_BALL, Items.BRICK, Items.GLASS, Items.SAND, Items.GRAVEL, Items.SPRUCE_LOG,
            Items.BIRCH_LOG, Items.SNOWBALL, Items.LEATHER, Items.SLIME_BALL, Items.ENDER_PEARL};

    private GatewayGameTests() {
    }

    public static FakePlayer person(GameTestHelper Helper, double X, double Z, String Name) {
        return person(Helper, X, Z, Name, UUID.randomUUID());
    }

    public static FakePlayer person(GameTestHelper Helper, double X, double Z, String Name, UUID Id) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new GatewayTraveller(World, new GameProfile(Id, Name));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        whenDone(Helper, () -> clean(User));
        return User;
    }

    public static void clean(ServerPlayer User) {
        GatewayStash.get(User.server).discard(User.getUUID());
        if (!User.isRemoved()) {
            User.serverLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    public static void whenDone(GameTestHelper Helper, Runnable Cleanup) {
        Helper.testInfo.addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo Info) {
            }

            @Override
            public void testPassed(GameTestInfo Info, GameTestRunner Runner) {
                Cleanup.run();
            }

            @Override
            public void testFailed(GameTestInfo Info, GameTestRunner Runner) {
                Cleanup.run();
            }

            @Override
            public void testAddedForRerun(GameTestInfo Old, GameTestInfo Fresh, GameTestRunner Runner) {
            }
        });
    }

    public static void guard(GameTestHelper Helper, Runnable Body) {
        try {
            Body.run();
        } catch (RuntimeException Problem) {
            if (Problem instanceof GameTestAssertException) {
                throw Problem;
            }
            StringBuilder Trace = new StringBuilder(String.valueOf(Problem));
            StackTraceElement[] Frames = Problem.getStackTrace();
            for (int Index = 0; Index < Math.min(14, Frames.length); Index++) {
                Trace.append(" | ").append(Frames[Index]);
            }
            Helper.fail(Trace.toString());
        }
    }

    public static GatewayDestination winter() {
        return Scp4666Destination.standIn(Level.NETHER);
    }

    public static Map<Item, Integer> fill(ServerPlayer User) {
        for (int Slot = 0; Slot < 36; Slot++) {
            User.getInventory().setItem(Slot, new ItemStack(Pocket[Slot], 1 + Slot % 16));
        }
        User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        User.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        User.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.GOLDEN_BOOTS));
        User.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        User.giveExperienceLevels(7);
        return counts(User, List.of());
    }

    public static Map<Item, Integer> counts(ServerPlayer User, List<ItemEntity> Dropped) {
        Map<Item, Integer> Found = new HashMap<>();
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            ItemStack Stack = User.getInventory().getItem(Slot);
            if (!Stack.isEmpty()) {
                Found.merge(Stack.getItem(), Stack.getCount(), Integer::sum);
            }
        }
        for (ItemEntity Item : Dropped) {
            Found.merge(Item.getItem().getItem(), Item.getItem().getCount(), Integer::sum);
        }
        return Found;
    }

    public static boolean empty(ServerPlayer User) {
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            if (!User.getInventory().getItem(Slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static String describe(Map<Item, Integer> Counts) {
        StringBuilder Text = new StringBuilder();
        Counts.entrySet().stream().sorted(Map.Entry.comparingByKey((First, Second) -> BuiltInRegistries.ITEM.getKey(First).compareTo(BuiltInRegistries.ITEM.getKey(Second))))
                .forEach(Entry -> Text.append(BuiltInRegistries.ITEM.getKey(Entry.getKey()).getPath()).append('=').append(Entry.getValue()).append(' '));
        return Text.toString().trim();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "gateway_placement")
    public static void placementNeedsRoomAndBreaksAsOne(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = person(Helper, 10.5, 4.5, "gateway_builder");
        User.setYRot(180.0f);
        BlockPos Origin = Helper.absolutePos(new BlockPos(10, 2, 10));
        ItemStack Item = new ItemStack(GatewayRegistry.GatewayItem.get());
        User.setItemInHand(InteractionHand.MAIN_HAND, Item);
        BlockHitResult Hit = new BlockHitResult(Vec3.atCenterOf(Origin.below()).add(0.0, 0.5, 0.0), Direction.UP, Origin.below(), false);
        Direction Front = new BlockPlaceContext(World, User, InteractionHand.MAIN_HAND, Item, Hit).getHorizontalDirection().getOpposite();
        BlockPos Blocker = ContainedGatewayBlock.cellPos(Origin, Front, ContainedGatewayBlock.index(0, 2, 0));
        World.setBlockAndUpdate(Blocker, Blocks.STONE.defaultBlockState());
        BlockState Blocked = GatewayRegistry.Gateway.get().getStateForPlacement(new BlockPlaceContext(World, User, InteractionHand.MAIN_HAND, Item, Hit));
        Helper.assertTrue(Blocked == null, "the gateway could be placed with a block in its frame");
        World.setBlockAndUpdate(Blocker, Blocks.AIR.defaultBlockState());
        BlockState Clear = GatewayRegistry.Gateway.get().getStateForPlacement(new BlockPlaceContext(World, User, InteractionHand.MAIN_HAND, Item, Hit));
        BlockPlaceContext Probe = new BlockPlaceContext(World, User, InteractionHand.MAIN_HAND, Item, Hit);
        Helper.assertTrue(Clear != null, "the gateway could not be placed in open space at " + Probe.getClickedPos() + " blocked " + LargeBlock.blockedCells(Probe,
                GatewayRegistry.Gateway.get().getFootprint(Probe)));
        ContainedGatewayBlock.assemble(World, Origin, Direction.NORTH);
        for (int Index = 0; Index < ContainedGatewayBlock.CellCount; Index++) {
            BlockState Cell = World.getBlockState(ContainedGatewayBlock.cellPos(Origin, Direction.NORTH, Index));
            Helper.assertTrue(Cell.is(GatewayRegistry.Gateway.get()) && Cell.getValue(ContainedGatewayBlock.Part) == Index, "cell " + Index + " is " + Cell);
        }
        BlockPos Master = ContainedGatewayBlock.cellPos(Origin, Direction.NORTH, ContainedGatewayBlock.MasterPart);
        Helper.assertTrue(World.getBlockEntity(Master) instanceof ContainedGatewayBlockEntity, "the master cell has no block entity");
        ContainedGatewayBlockEntity Gate = (ContainedGatewayBlockEntity) World.getBlockEntity(Master);
        Gate.setShard(GatewayRegistry.shardFor(Scp4666Destination.Id));
        World.destroyBlock(ContainedGatewayBlock.cellPos(Origin, Direction.NORTH, ContainedGatewayBlock.index(0, 2, 0)), false);
        for (int Index = 0; Index < ContainedGatewayBlock.CellCount; Index++) {
            BlockPos Cell = ContainedGatewayBlock.cellPos(Origin, Direction.NORTH, Index);
            Helper.assertTrue(World.getBlockState(Cell).isAir(), "cell " + Index + " survived breaking the frame");
        }
        List<ItemEntity> Dropped = World.getEntitiesOfClass(ItemEntity.class, new AABB(Origin).inflate(3.0), Found -> Found.getItem().is(GatewayRegistry.Shard.get()));
        Helper.assertTrue(Dropped.size() == 1, "breaking the frame dropped " + Dropped.size() + " shards");
        Helper.assertTrue(GatewayDestinations.of(Dropped.get(0).getItem()) != null, "the dropped shard lost its destination");
        Dropped.forEach(Entity::discard);
        Helper.succeed();
    }

    private static OutletBlockEntity outlet(ServerLevel World, BlockPos Pos, Direction Facing) {
        World.setBlock(Pos, ModBlocks.Outlet.get().defaultBlockState().setValue(OutletBlock.Facing, Facing), 3);
        return (OutletBlockEntity) World.getBlockEntity(Pos);
    }

    private static void link(OutletBlockEntity First, OutletBlockEntity Second) {
        int Slot = WireLink.freeSlot(First.getLinks(), Second.getLinks());
        First.addLink(new WireLink(Second.getBlockPos(), Second.getFacing(), Slot, WireTier.Advanced, false, null));
        Second.addLink(new WireLink(First.getBlockPos(), First.getFacing(), Slot, WireTier.Advanced, false, null));
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "gateway_power")
    public static void powerAndShardOpenTheGate(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = person(Helper, 4.5, 4.5, "gateway_engineer");
        BlockPos Origin = Helper.absolutePos(new BlockPos(16, 2, 16));
        ContainedGatewayBlock.assemble(World, Origin, Direction.NORTH);
        BlockPos Master = ContainedGatewayBlock.cellPos(Origin, Direction.NORTH, ContainedGatewayBlock.MasterPart);
        ContainedGatewayBlockEntity Gate = (ContainedGatewayBlockEntity) World.getBlockEntity(Master);
        BlockPos Port = ContainedGatewayBlock.portPos(Origin, Direction.NORTH);
        OutletBlockEntity Near = outlet(World, Port, Direction.WEST);
        Helper.assertTrue(Near != null && World.getBlockState(Port).is(ModBlocks.Outlet.get()), "an outlet would not mount on the power port");
        BlockPos Source = Port.west(4);
        World.setBlock(Source, ModBlocks.CreativeEnergySource.get().defaultBlockState(), 3);
        OutletBlockEntity Far = outlet(World, Source.east(), Direction.EAST);
        link(Near, Far);
        Helper.assertTrue(Near.getLinks().size() == 1, "the test wire did not link");
        WireNetworkManager.get(World).markDirty();
        BlockPos Elsewhere = ContainedGatewayBlock.cellPos(Origin, Direction.NORTH, ContainedGatewayBlock.index(1, 1, 0)).relative(Direction.EAST);
        Helper.assertTrue(!ModBlocks.Outlet.get().defaultBlockState().setValue(OutletBlock.Facing, Direction.EAST).canSurvive(World, Elsewhere), "outlets mount away from the port");
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Gate.status() == ContainedGatewayBlockEntity.StatusStandby, "the powered gate never reached standby: " + Gate.status()))
                .thenExecute(() -> {
                    ItemStack Blank = new ItemStack(GatewayRegistry.Shard.get());
                    Helper.assertTrue(!Gate.insert(User, Blank), "a blank shard was accepted");
                    ItemStack Attuned = GatewayRegistry.shardFor(winter().Id());
                    Helper.assertTrue(Gate.insert(User, Attuned), "an attuned shard was refused");
                    Helper.assertTrue(Attuned.isEmpty(), "inserting did not take the shard");
                })
                .thenWaitUntil(() -> Helper.assertTrue(Gate.status() == ContainedGatewayBlockEntity.StatusOpen, "the gate never opened: " + Gate.status()))
                .thenExecute(() -> Helper.assertTrue(Gate.getPowerDemand() == ContainedGatewayBlockEntity.OpenDraw, "open draw is " + Gate.getPowerDemand()))
                .thenExecute(() -> World.setBlock(Source, Blocks.AIR.defaultBlockState(), 3))
                .thenWaitUntil(() -> Helper.assertTrue(Gate.status() == ContainedGatewayBlockEntity.StatusOff, "the gate stayed lit without power: " + Gate.status()))
                .thenExecute(() -> {
                    Helper.assertTrue(Gate.takeOut(User), "the shard could not be taken out");
                    Helper.assertTrue(User.getInventory().countItem(GatewayRegistry.Shard.get()) == 1, "taking the shard out did not hand it back");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "gateway_registry")
    public static void destinationRegistryResolvesShards(GameTestHelper Helper) {
        GatewayDestination Winter = GatewayDestinations.get(Scp4666Destination.Id);
        Helper.assertTrue(Winter != null, "the winter destination is not registered");
        Helper.assertTrue(Winter.Dimension() == Scp4666Registry.Dimension, "the winter destination points at " + Winter.Dimension());
        Helper.assertTrue(GatewayDestinations.inDimension(Scp4666Registry.Dimension) == Winter, "the winter dimension does not map back to its destination");
        Helper.assertTrue(GatewayDestinations.sealed(Scp4666Registry.Dimension), "the winter is not sealed");
        Helper.assertTrue(!GatewayDestinations.sealed(Helper.getLevel().dimension()), "the overworld counts as sealed");
        ItemStack Shard = GatewayRegistry.shardFor(Winter.Id());
        Helper.assertTrue(GatewayDestinations.of(Shard) == Winter, "the winter shard does not resolve");
        Helper.assertTrue(DimensionalShardItem.variantOf(Shard) == Winter.ShardVariant(), "the shard model variant is wrong");
        Helper.assertTrue(GatewayDestinations.of(new ItemStack(GatewayRegistry.Shard.get())) == null, "a blank shard resolves to a destination");
        Helper.assertTrue(GatewayDestinations.get(Scp4666Registry.location("nowhere")) == null, "an unknown id resolves");
        Helper.assertTrue(Winter.Rules().StashBelongings() && Winter.Rules().WipeOnDeath() && Winter.Rules().KeepFoundOnLeave(), "the winter rules are wrong");
        boolean Refused = false;
        try {
            GatewayDestinations.register(Winter);
        } catch (IllegalStateException Expected) {
            Refused = true;
        }
        Helper.assertTrue(Refused, "a destination could be registered twice");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "gateway_stash_leave")
    public static void leavingKeepsFindsAndReturnsEverything(GameTestHelper Helper) {
        guard(Helper, () -> leavingKeepsFindsAndReturnsEverythingBody(Helper));
        Helper.succeed();
    }

    private static void leavingKeepsFindsAndReturnsEverythingBody(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = person(Helper, 8.5, 8.5, "gateway_leaver");
        Map<Item, Integer> Before = fill(User);
        GatewayStash.Spot Back = new GatewayStash.Spot(World.dimension(), Helper.absoluteVec(new Vec3(8.5, Floor, 8.5)), 0.0f);
        Helper.assertTrue(GatewayTravel.enter(User, winter(), Back), "the gateway refused a normal entry");
        Helper.assertTrue(User.level().dimension() == winter().Dimension(), "the player did not arrive in the winter");
        Helper.assertTrue(empty(User), "the player arrived carrying " + describe(counts(User, List.of())));
        Helper.assertTrue(User.experienceLevel == 0, "the player arrived with experience");
        GatewayStash Stash = GatewayStash.get(User.server);
        Helper.assertTrue(Stash.holds(User.getUUID()) && GatewayStash.matches(User, Stash.entry(User.getUUID())), "no matching stash was recorded");
        User.getInventory().add(new ItemStack(Scp4666Registry.Cocoa.get(), 3));
        User.getInventory().add(new ItemStack(Scp4666Registry.Doll.get()));
        User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        for (int Slot = 0; Slot < 36; Slot++) {
            if (User.getInventory().getItem(Slot).isEmpty()) {
                User.getInventory().setItem(Slot, new ItemStack(Items.SPRUCE_PLANKS, 5));
            }
        }
        Map<Item, Integer> Expected = new HashMap<>(Before);
        counts(User, List.of()).forEach((Key, Value) -> Expected.merge(Key, Value, Integer::sum));
        GatewayTravel.leave(User);
        Helper.assertTrue(User.level().dimension() == World.dimension(), "leaving did not bring the player home");
        List<ItemEntity> Dropped = World.getEntitiesOfClass(ItemEntity.class, new AABB(BlockPos.containing(Back.Pos())).inflate(4.0));
        Map<Item, Integer> After = counts(User, Dropped);
        Helper.assertTrue(After.equals(Expected), "items changed on the way out. expected " + describe(Expected) + " got " + describe(After));
        Helper.assertTrue(!Dropped.isEmpty(), "a full inventory dropped nothing at the exit");
        Helper.assertTrue(User.experienceLevel == 7, "experience came back as " + User.experienceLevel);
        Helper.assertTrue(!Stash.holds(User.getUUID()) && GatewayStash.marker(User) == null, "the stash was not cleared");
        Dropped.forEach(Entity::discard);
    }

    public static ServerPlayer dieAndRespawn(GameTestHelper Helper, ServerPlayer User, Vec3 Home) {
        ServerLevel Winter = User.serverLevel();
        DamageSource Source = Winter.damageSources().genericKill();
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(User, Source));
        ItemEntity Loose = new ItemEntity(Winter, User.getX(), User.getY(), User.getZ(), new ItemStack(Scp4666Registry.Cocoa.get()));
        boolean Spilled = Winter.addFreshEntity(Loose);
        Helper.assertTrue(!Spilled, "an item spilled from a body in the winter");
        List<ItemEntity> Drops = new ArrayList<>();
        Drops.add(new ItemEntity(Winter, User.getX(), User.getY(), User.getZ(), new ItemStack(Scp4666Registry.Doll.get())));
        LivingDropsEvent Dropping = NeoForge.EVENT_BUS.post(new LivingDropsEvent(User, Source, Drops, false));
        Helper.assertTrue(Dropping.isCanceled(), "death drops were allowed in the winter");
        LivingExperienceDropEvent Orbs = NeoForge.EVENT_BUS.post(new LivingExperienceDropEvent(User, null, 30));
        Helper.assertTrue(Orbs.isCanceled(), "experience dropped in the winter");
        ServerLevel Home0 = Helper.getLevel();
        FakePlayer Fresh = new GatewayTraveller(Home0, User.getGameProfile());
        Fresh.restoreFrom(User, false);
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(Fresh, User, true));
        Winter.removePlayerImmediately(User, Entity.RemovalReason.KILLED);
        Fresh.moveTo(Home.x, Home.y, Home.z, 0.0f, 0.0f);
        Home0.addNewPlayer(Fresh);
        whenDone(Helper, () -> clean(Fresh));
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(Fresh, false));
        return Fresh;
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "gateway_stash_death")
    public static void dyingLosesFindsButReturnsEverything(GameTestHelper Helper) {
        guard(Helper, () -> dyingLosesFindsButReturnsEverythingBody(Helper));
        Helper.succeed();
    }

    private static void dyingLosesFindsButReturnsEverythingBody(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = person(Helper, 12.5, 12.5, "gateway_dier");
        Map<Item, Integer> Before = fill(User);
        Vec3 Home = Helper.absoluteVec(new Vec3(12.5, Floor, 12.5));
        Helper.assertTrue(GatewayTravel.enter(User, winter(), new GatewayStash.Spot(World.dimension(), Home, 0.0f)), "the gateway refused entry");
        User.getInventory().add(new ItemStack(Scp4666Registry.Cocoa.get(), 2));
        User.getInventory().add(new ItemStack(Scp4666Registry.Cane.get()));
        boolean Kept = World.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        World.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, World.getServer());
        try {
            ServerPlayer Fresh = dieAndRespawn(Helper, User, Home);
            Map<Item, Integer> After = counts(Fresh, List.of());
            Helper.assertTrue(After.equals(Before), "after dying the player holds " + describe(After) + " instead of " + describe(Before));
            Helper.assertTrue(Fresh.experienceLevel == 7, "experience came back as " + Fresh.experienceLevel);
            Helper.assertTrue(!GatewayStash.get(World.getServer()).holds(Fresh.getUUID()), "the stash survived the respawn");
        } finally {
            World.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(Kept, World.getServer());
        }
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "gateway_stash_rollback")
    public static void rolledBackStashNeverDuplicates(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = person(Helper, 6.5, 20.5, "gateway_rollback");
        Map<Item, Integer> Before = fill(User);
        GatewayStash Stash = GatewayStash.get(World.getServer());
        GatewayStash.Spot Back = new GatewayStash.Spot(World.dimension(), User.position(), 0.0f);
        Stash.stash(User, Scp4666Destination.Id, Back, true);
        Helper.assertTrue(empty(User), "stashing left items behind");
        fill(User);
        User.experienceLevel = 7;
        GatewayStash.setMarker(User, null);
        GatewayEvents.settle(User);
        Map<Item, Integer> After = counts(User, List.of());
        Helper.assertTrue(After.equals(Before), "a rolled back stash duplicated items: " + describe(After));
        Helper.assertTrue(!Stash.holds(User.getUUID()), "the stale stash was kept");
        Stash.stash(User, Scp4666Destination.Id, Back, true);
        GatewayEvents.settle(User);
        Helper.assertTrue(counts(User, List.of()).equals(Before), "a matching stash did not come back");
        Helper.succeed();
    }
}
