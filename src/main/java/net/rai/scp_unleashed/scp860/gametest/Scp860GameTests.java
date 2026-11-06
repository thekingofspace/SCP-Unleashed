package net.rai.scp_unleashed.scp860.gametest;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.util.Mth;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp860.Scp860Builder;
import net.rai.scp_unleashed.scp860.Scp860Director;
import net.rai.scp_unleashed.scp860.Scp860DoorBlock;
import net.rai.scp_unleashed.scp860.Scp860DoorBlockEntity;
import net.rai.scp_unleashed.scp860.Scp860Forest;
import net.rai.scp_unleashed.scp860.Scp860Plan;
import net.rai.scp_unleashed.scp860.Scp860Record;
import net.rai.scp_unleashed.scp860.Scp860Registry;
import net.rai.scp_unleashed.scp860.Scp860Rules;
import net.rai.scp_unleashed.scp860.Scp860State;
import net.rai.scp_unleashed.scp860.Scp860Trip;
import net.rai.scp_unleashed.scp860.Scp860Watcher;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;
import net.rai.scp_unleashed.stranger.StrangerEncounterCondition;
import net.rai.scp_unleashed.stranger.StrangerGift;
import net.rai.scp_unleashed.stranger.StrangerPlan;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp860GameTests {
    public static final String Arena = "scp860_arena";
    public static final BlockPos DoorSpot = new BlockPos(8, 1, 12);
    public static final BlockPos SecondSpot = new BlockPos(16, 1, 12);
    private static final Logger Log = LogUtils.getLogger();
    private static final List<long[]> Held = new ArrayList<>();
    private static ServerLevel HeldLevel;

    private Scp860GameTests() {
    }

    public static void forest(GameTestHelper Helper) {
        if (Helper.getLevel().getServer().getLevel(Scp860Registry.Forest) == null) {
            Scp860Forest.standIn(Level.END);
        }
    }

    public static void fresh(GameTestHelper Helper) {
        forest(Helper);
        Scp860Director.forget(Helper.getLevel().getServer());
        Scp860Trip.forgetAll();
        ServerLevel Forest = Scp860Forest.level(Helper.getLevel().getServer());
        if (Forest != null) {
            Scp860Builder.clearEntities(Forest);
        }
    }

    public static Scp860TestPlayer player(GameTestHelper Helper, double X, double Z, String Name) {
        forest(Helper);
        ServerLevel World = Helper.getLevel();
        Scp860TestPlayer User = new Scp860TestPlayer(World, Name);
        Vec3 Where = Helper.absoluteVec(new Vec3(X, 1.0, Z));
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.enlist(true);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    public static void dismiss(ServerPlayer User) {
        Scp860Forest.record(User).clearTrip();
        Scp860Director.left(User);
        if (User instanceof Scp860TestPlayer Tester) {
            Tester.enlist(false);
        }
        if (!User.isRemoved()) {
            User.serverLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    public static void finish(GameTestHelper Helper, ServerPlayer... Users) {
        for (ServerPlayer User : Users) {
            dismiss(User);
        }
        release();
        ServerLevel Forest = Scp860Forest.level(Helper.getLevel().getServer());
        if (Forest != null) {
            Scp860Builder.clearEntities(Forest);
        }
        for (Scp860Watcher Found : Helper.getLevel().getEntitiesOfClass(Scp860Watcher.class, arena(Helper))) {
            Found.discard();
        }
    }

    public static void cleanup(GameTestHelper Helper, ServerPlayer... Users) {
        Helper.testInfo.addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo Info) {
            }

            @Override
            public void testPassed(GameTestInfo Info, GameTestRunner Runner) {
                finish(Helper, Users);
            }

            @Override
            public void testFailed(GameTestInfo Info, GameTestRunner Runner) {
                finish(Helper, Users);
            }

            @Override
            public void testAddedForRerun(GameTestInfo Old, GameTestInfo Fresh, GameTestRunner Runner) {
            }
        });
    }

    public static String moodOf(ServerPlayer User) {
        Scp860Director.Mood State = Scp860Director.peek(User.server, User.getUUID());
        return (State == null ? "no mood" : "patience " + State.Patience + " pace " + State.pace() + " off " + State.OffTicks + " pack " + State.Pack + " full " + State.Full
                + " inside " + State.Inside) + " at " + User.position() + " in " + User.level().dimension().location() + " present " + Scp860Forest.present(User)
                + " gap " + Scp860Forest.distance(User.server, User.getX(), User.getZ()) + " generation " + Scp860State.get(User.server).generation();
    }

    public static String watcherOf(Scp860Watcher Watcher) {
        return "watcher alive " + Watcher.isAlive() + " removed " + Watcher.getRemovalReason() + " bound " + Watcher.forestBound() + " hunt " + Watcher.huntTarget()
                + " at " + Watcher.position() + " health " + Watcher.getHealth();
    }

    public static AABB arena(GameTestHelper Helper) {
        return new AABB(Helper.absolutePos(BlockPos.ZERO)).inflate(40.0);
    }

    public static Scp860DoorBlockEntity door(GameTestHelper Helper, BlockPos Relative) {
        BlockPos Master = Helper.absolutePos(Relative);
        Scp860DoorBlock.placeWhole(Helper.getLevel(), Master, Direction.NORTH, Scp860Registry.Door.get(), Block.UPDATE_ALL);
        if (Helper.getLevel().getBlockEntity(Master) instanceof Scp860DoorBlockEntity Frame) {
            return Frame;
        }
        throw new GameTestAssertException("the door at " + Relative + " has no block entity");
    }

    public static void click(ServerPlayer User, BlockPos Absolute, boolean Sneaking) {
        User.setShiftKeyDown(Sneaking);
        User.gameMode.useItemOn(User, User.serverLevel(), User.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(Absolute), Direction.NORTH, Absolute, false));
        User.setShiftKeyDown(false);
    }

    public static void fill(ServerPlayer User) {
        User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        User.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        User.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH, 23));
        User.getInventory().setItem(0, new ItemStack(Items.STONE_SWORD));
        User.getInventory().setItem(3, new ItemStack(Items.BREAD, 7));
        User.getInventory().setItem(17, new ItemStack(Items.DIAMOND, 5));
        User.getInventory().setItem(30, StasisRegistry.podFor(FragmentRarity.Faint).getDefaultInstance());
    }

    public static List<ItemStack> snapshot(ServerPlayer User) {
        List<ItemStack> Copy = new ArrayList<>();
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            Copy.add(User.getInventory().getItem(Slot).copy());
        }
        return Copy;
    }

    public static boolean same(List<ItemStack> Before, ServerPlayer User) {
        for (int Slot = 0; Slot < Before.size(); Slot++) {
            if (!ItemStack.matches(Before.get(Slot), User.getInventory().getItem(Slot))) {
                return false;
            }
        }
        return true;
    }

    public static boolean empty(ServerPlayer User) {
        return User.getInventory().isEmpty();
    }

    public static int stacks(ServerPlayer User) {
        int Count = 0;
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            if (!User.getInventory().getItem(Slot).isEmpty()) {
                Count++;
            }
        }
        return Count;
    }

    public static boolean send(GameTestHelper Helper, ServerPlayer User) {
        Vec3 Home = User.position();
        boolean Entered = Scp860Trip.enter(User, new Scp860Trip.Spot(Helper.getLevel(), Home, 0.0f), Helper.getLevel().dimension(), null);
        if (Entered) {
            hold(User.serverLevel(), User.chunkPosition().x, User.chunkPosition().z);
        }
        return Entered;
    }

    public static void hold(ServerLevel World, int Cx, int Cz) {
        for (int Dx = -2; Dx <= 2; Dx++) {
            for (int Dz = -2; Dz <= 2; Dz++) {
                if (World.setChunkForced(Cx + Dx, Cz + Dz, true)) {
                    Held.add(new long[]{Cx + Dx, Cz + Dz});
                }
            }
        }
        HeldLevel = World;
    }

    public static void release() {
        if (HeldLevel != null) {
            for (long[] Chunk : Held) {
                HeldLevel.setChunkForced((int) Chunk[0], (int) Chunk[1], false);
            }
        }
        Held.clear();
    }

    public static Scp860Watcher current(Scp860Watcher Watcher) {
        if (Watcher.level() instanceof ServerLevel World && World.getEntity(Watcher.getUUID()) instanceof Scp860Watcher Found) {
            return Found;
        }
        return Watcher;
    }

    public static Scp860Watcher watcher(ServerLevel World, Vec3 Spot, ServerPlayer Watched) {
        Scp860Watcher Made = Scp860Registry.Watcher.get().create(World);
        if (Made == null) {
            throw new GameTestAssertException("SCP-860-2 could not be created");
        }
        Made.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Made.bindToForest(Scp860State.get(World.getServer()).generation());
        Made.watch(Watched);
        Made.setPersistenceRequired();
        World.addFreshEntity(Made);
        return Made;
    }

    public static Vec3 offPathSpot(ServerLevel World, ServerPlayer Near, double Low, double High) {
        Scp860Plan Layout = Scp860Forest.plan(World.getServer());
        for (int Radius = 4; Radius <= 24; Radius++) {
            for (int Step = 0; Step < 32; Step++) {
                double Angle = Step * Math.PI * 2.0 / 32.0;
                double X = Near.getX() + Math.cos(Angle) * Radius;
                double Z = Near.getZ() + Math.sin(Angle) * Radius;
                double Gap = Layout.distance(Scp860Forest.localX(X), Scp860Forest.localZ(Z));
                if (Gap < Low || Gap > High) {
                    continue;
                }
                BlockPos Feet = Scp860Director.groundAt(World, new Vec3(X, Scp860Forest.Ground + 1.0, Z));
                if (Feet != null) {
                    return Vec3.atBottomCenterOf(Feet);
                }
            }
        }
        throw new GameTestAssertException("no off-path ground was found near " + Near.position());
    }

    public static List<Scp860Watcher> watchersNear(ServerLevel World, Vec3 Centre, double Reach) {
        return World.getEntitiesOfClass(Scp860Watcher.class, new AABB(Centre, Centre).inflate(Reach));
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp860_keyInLock")
    public static void keyInLock(GameTestHelper Helper) {
        fresh(Helper);
        Scp860DoorBlockEntity Frame = door(Helper, DoorSpot);
        BlockPos Master = Frame.getBlockPos();
        Scp860TestPlayer User = player(Helper, 8.5, 9.5, "scp860_keyholder");
        try {
            for (int Part : Scp860DoorBlock.Halves) {
                BlockState Cell = Helper.getLevel().getBlockState(Scp860DoorBlock.cellPos(Master, Direction.NORTH, Part));
                Helper.assertTrue(Scp860DoorBlock.isDoor(Cell) && Cell.getValue(Scp860DoorBlock.Part) == Part, "door part " + Part + " is missing");
            }
            for (BlockPos Side : List.of(Master.east(), Master.west(), Master.above(2), Master.above().east(), Master.above().west())) {
                Helper.assertFalse(Scp860DoorBlock.isDoor(Helper.getLevel().getBlockState(Side)), "the door reaches past its two cells at " + Side);
            }
            click(User, Master, false);
            Helper.assertFalse(Frame.opened(), "the door opened without its key");
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp860Registry.Key.get()));
            click(User, Master.above(), false);
            Helper.assertTrue(Frame.keyed(), "the key did not go into the lock");
            Helper.assertTrue(User.getMainHandItem().isEmpty(), "the key stayed in the hand after going into the lock");
            click(User, Master, false);
            Helper.assertTrue(Frame.opened(), "the keyed door did not open");
            for (int Part : Scp860DoorBlock.Halves) {
                Helper.assertTrue(Helper.getLevel().getBlockState(Scp860DoorBlock.cellPos(Master, Direction.NORTH, Part)).getValue(Scp860DoorBlock.Open), "part " + Part + " is not open");
            }
            click(User, Master, false);
            Helper.assertFalse(Frame.opened(), "the door did not close again");
            click(User, Master, true);
            Helper.assertFalse(Frame.keyed(), "sneaking with an empty hand did not take the key back");
            Helper.assertTrue(User.getInventory().countItem(Scp860Registry.Key.get()) == 1, "the key did not come back to the hand");
            Frame.setOpened(true);
            Helper.assertFalse(Frame.opened(), "the door opened after the key was taken out");
        } finally {
            finish(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp860_sealedWhileInside")
    public static void sealedWhileInside(GameTestHelper Helper) {
        fresh(Helper);
        Scp860DoorBlockEntity Frame = door(Helper, DoorSpot);
        BlockPos Master = Frame.getBlockPos();
        Frame.insert(new ItemStack(Scp860Registry.Key.get()), null);
        Scp860TestPlayer Walker = player(Helper, 4.5, 4.5, "scp860_walker");
        Scp860TestPlayer Breaker = player(Helper, 8.5, 9.5, "scp860_breaker");
        try {
            Helper.assertTrue(send(Helper, Walker), "the walker could not enter the forest");
            Helper.assertTrue(Scp860Forest.occupied(Helper.getLevel().getServer()), "the forest is not occupied with a walker inside");
            Helper.assertTrue(Frame.sealed(), "the keyed door is not held while someone is inside");
            BlockPos Post = Scp860DoorBlock.cellPos(Master, Direction.NORTH, Scp860DoorBlock.Upper);
            BlockState Before = Helper.getLevel().getBlockState(Post);
            BlockEvent.BreakEvent Breaking = NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(Helper.getLevel(), Post, Before, Breaker));
            Helper.assertTrue(Breaking.isCanceled(), "breaking the held door was allowed");
            Helper.assertTrue(Before.getDestroyProgress(Breaker, Helper.getLevel(), Post) == 0.0f, "the held door can be mined");
            Helper.getLevel().explode(null, Post.getX() + 0.5, Post.getY() + 0.5, Post.getZ() - 1.5, 3.0f, Level.ExplosionInteraction.TNT);
            for (int Part : Scp860DoorBlock.Halves) {
                Helper.assertTrue(Scp860DoorBlock.isDoor(Helper.getLevel().getBlockState(Scp860DoorBlock.cellPos(Master, Direction.NORTH, Part))),
                        "an explosion broke part " + Part + " of the held door");
            }
            Helper.assertFalse(Frame.takeKey(Breaker), "the key came out while someone was inside");
            Helper.assertTrue(Frame.keyed(), "the key is gone while someone is inside");
            Helper.assertTrue(Scp860Trip.leave(Walker, Scp860Trip.Reason.Exit), "the walker could not leave");
            Helper.assertFalse(Scp860Forest.occupied(Helper.getLevel().getServer()), "the forest is still occupied after the walker left");
            Helper.assertFalse(Frame.sealed(), "the door is still held after the forest emptied");
            Helper.assertTrue(Frame.takeKey(Breaker), "the key could not be taken back once the forest was empty");
            BlockEvent.BreakEvent Later = NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(Helper.getLevel(), Post, Helper.getLevel().getBlockState(Post), Breaker));
            Helper.assertFalse(Later.isCanceled(), "the door still cannot be broken once the forest is empty");
            Helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(Master).inflate(4.0)).forEach(Entity::discard);
            Helper.getLevel().destroyBlock(Post, true);
            Helper.assertFalse(Scp860DoorBlock.isDoor(Helper.getLevel().getBlockState(Master)), "breaking the upper half left the lower half standing");
            int Doors = Helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(Master).inflate(4.0)).stream()
                    .filter(Drop -> Drop.getItem().is(Scp860Registry.DoorItem.get())).mapToInt(Drop -> Drop.getItem().getCount()).sum();
            Helper.assertTrue(Doors == 1, "breaking the upper half dropped " + Doors + " doors instead of one");
        } finally {
            finish(Helper, Walker, Breaker);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp860_roarOnce")
    public static void roarOnce(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp860TestPlayer User = player(Helper, 8.5, 9.5, "scp860_roar_target");
        List<Scp860Watcher> Pack = new ArrayList<>();
        try {
            for (int Index = 0; Index < 3; Index++) {
                Scp860Watcher Made = Scp860Registry.Watcher.get().create(World);
                if (Made == null) {
                    throw new GameTestAssertException("could not make a watcher");
                }
                Vec3 Spot = Helper.absoluteVec(new Vec3(4.5 + Index * 4.0, 1.0, 16.5 + Index * 3.0));
                Made.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
                World.addFreshEntity(Made);
                Pack.add(Made);
            }
            Scp860Director.Mood State = new Scp860Director.Mood();
            long Now = World.getGameTime();
            Scp860Director.charge(World, User, State, Pack);
            List<Scp860Watcher> Loud = Pack.stream().filter(Watcher -> Watcher.roaredAt() == Now).toList();
            Helper.assertTrue(Loud.size() == 1, Loud.size() + " watchers roared at the start of one charge instead of one");
            Scp860Watcher Closest = Pack.stream().min(java.util.Comparator.comparingDouble(Watcher -> Watcher.distanceToSqr(User))).orElseThrow();
            Helper.assertTrue(Loud.get(0) == Closest, "the roar did not come from the watcher nearest the player");
            for (Scp860Watcher Watcher : Pack) {
                Helper.assertTrue(Watcher.isHunting(User), "a pack member did not join the charge");
                Helper.assertTrue(Watcher.windup() == Scp860Director.Windup, "a pack member charged before the roar finished");
                Watcher.calm();
            }
            Scp860Director.charge(World, User, State, Pack);
            Helper.assertTrue(Pack.stream().filter(Watcher -> Watcher.roaredAt() == Now).count() == 1, "a second charge inside the cooldown roared again");
            Helper.assertTrue(Pack.stream().allMatch(Watcher -> Watcher.windup() == 0 && Watcher.isHunting(User)), "a quiet charge still waited for a roar");
        } finally {
            Pack.forEach(Entity::discard);
            finish(Helper, User);
        }
        Helper.succeed();
    }

    public static void pour(ServerLevel World, BlockPos Pos, BlockState Liquid) {
        if (World.getBlockState(Pos).isAir()) {
            World.setBlock(Pos, Liquid, Block.UPDATE_ALL);
        }
    }

    public static void doorWhole(GameTestHelper Helper, Scp860DoorBlockEntity Frame, boolean Keyed, boolean Open, String Name) {
        BlockPos Master = Frame.getBlockPos();
        for (int Part : Scp860DoorBlock.Halves) {
            BlockPos Cell = Scp860DoorBlock.cellPos(Master, Direction.NORTH, Part);
            BlockState Here = Helper.getLevel().getBlockState(Cell);
            Helper.assertTrue(Scp860DoorBlock.isDoor(Here) && Here.getValue(Scp860DoorBlock.Part) == Part, Name + " lost part " + Part + " to a fluid");
            Helper.assertTrue(Here.getValue(Scp860DoorBlock.Open) == Open, Name + " part " + Part + " changed its open state");
            Helper.assertTrue(Helper.getLevel().getFluidState(Cell).isEmpty(), Name + " part " + Part + " holds a fluid");
        }
        Helper.assertTrue(Helper.getLevel().getBlockEntity(Master) == Frame && !Frame.isRemoved(), Name + " lost its block entity");
        Helper.assertTrue(Frame.keyed() == Keyed, Name + " key changed under a fluid");
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp860_doorFluids")
    public static void doorFluids(GameTestHelper Helper) {
        fresh(Helper);
        ServerLevel World = Helper.getLevel();
        Scp860DoorBlockEntity Shut = door(Helper, DoorSpot);
        Scp860DoorBlockEntity Ajar = door(Helper, SecondSpot);
        Ajar.insert(new ItemStack(Scp860Registry.Key.get()), null);
        Ajar.setOpened(true);
        for (Scp860DoorBlockEntity Frame : List.of(Shut, Ajar)) {
            BlockPos Upper = Frame.getBlockPos().above();
            for (BlockPos Cell : List.of(Frame.getBlockPos(), Upper)) {
                BlockState Here = World.getBlockState(Cell);
                Helper.assertFalse(Here.canBeReplaced(net.minecraft.world.level.material.Fluids.WATER), "water can replace the door at " + Cell);
                Helper.assertFalse(Here.canBeReplaced(net.minecraft.world.level.material.Fluids.LAVA), "lava can replace the door at " + Cell);
                Helper.assertFalse(((Scp860DoorBlock) Here.getBlock()).canPlaceLiquid(null, World, Cell, Here, net.minecraft.world.level.material.Fluids.WATER),
                        "water can be placed into the door at " + Cell);
                ((net.minecraft.world.item.BucketItem) Items.WATER_BUCKET).emptyContents(null, World, Cell, new BlockHitResult(Vec3.atCenterOf(Cell), Direction.NORTH, Cell, false));
                ((net.minecraft.world.item.BucketItem) Items.LAVA_BUCKET).emptyContents(null, World, Cell, new BlockHitResult(Vec3.atCenterOf(Cell), Direction.SOUTH, Cell, false));
            }
            pour(World, Upper.above(), Blocks.WATER.defaultBlockState());
            pour(World, Upper.east(), net.minecraft.world.level.material.Fluids.FLOWING_WATER.getFlowing(6, false).createLegacyBlock());
            pour(World, Frame.getBlockPos().west(), Blocks.LAVA.defaultBlockState());
            pour(World, Upper.west(), net.minecraft.world.level.material.Fluids.FLOWING_LAVA.getFlowing(5, false).createLegacyBlock());
        }
        Helper.runAfterDelay(120, () -> {
            doorWhole(Helper, Shut, false, false, "the shut door");
            doorWhole(Helper, Ajar, true, true, "the open keyed door");
            int Drops = World.getEntitiesOfClass(ItemEntity.class, new AABB(Shut.getBlockPos()).inflate(12.0)).stream()
                    .filter(Drop -> Drop.getItem().is(Scp860Registry.DoorItem.get()) || Drop.getItem().is(Scp860Registry.Key.get())).mapToInt(Drop -> Drop.getItem().getCount()).sum();
            Helper.assertTrue(Drops == 0, "fluids knocked " + Drops + " door items or keys loose");
            BlockPos.betweenClosedStream(new AABB(Shut.getBlockPos()).inflate(10.0).expandTowards(0.0, 4.0, 0.0)).forEach(Pos -> {
                if (!World.getFluidState(Pos).isEmpty()) {
                    World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            });
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp860_legacyDoor")
    public static void legacyDoor(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Master = Helper.absolutePos(DoorSpot);
        BlockState Base = Scp860Registry.Door.get().defaultBlockState().setValue(Scp860DoorBlock.Facing, Direction.NORTH).setValue(Scp860DoorBlock.Open, false);
        World.setBlock(Master, Base.setValue(Scp860DoorBlock.Part, Scp860DoorBlock.Master), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        for (int Index = 0; Index < 9; Index++) {
            if (Index != Scp860DoorBlock.Master) {
                World.setBlock(Scp860DoorBlock.cellPos(Master, Direction.NORTH, Index), Base.setValue(Scp860DoorBlock.Part, Index), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
        if (!(World.getBlockEntity(Master) instanceof Scp860DoorBlockEntity Frame)) {
            throw new GameTestAssertException("the old door has no block entity");
        }
        Frame.insert(new ItemStack(Scp860Registry.Key.get()), null);
        Helper.assertTrue(Scp860DoorBlock.isDoor(World.getBlockState(Scp860DoorBlock.cellPos(Master, Direction.NORTH, 6))), "the old 3x3 frame was not laid out");
        Helper.runAfterDelay(5, () -> {
            for (int Index = 0; Index < 9; Index++) {
                BlockState Here = World.getBlockState(Scp860DoorBlock.cellPos(Master, Direction.NORTH, Index));
                if (Scp860DoorBlock.opening(Index)) {
                    Helper.assertTrue(Scp860DoorBlock.isDoor(Here) && Here.getValue(Scp860DoorBlock.Part) == Index, "the converted door lost part " + Index);
                } else {
                    Helper.assertTrue(Here.isAir(), "old frame cell " + Index + " was not removed: " + Here);
                }
            }
            Helper.assertTrue(Frame.keyed(), "the key left the lock when the old door was converted");
            long Drops = World.getEntitiesOfClass(ItemEntity.class, new AABB(Master).inflate(4.0)).size();
            Helper.assertTrue(Drops == 0, "converting the old door dropped " + Drops + " items");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp860_entryStores")
    public static void entryStores(GameTestHelper Helper) {
        fresh(Helper);
        Scp860DoorBlockEntity Frame = door(Helper, DoorSpot);
        Frame.insert(new ItemStack(Scp860Registry.Key.get()), null);
        Frame.setOpened(true);
        Scp860TestPlayer User = player(Helper, 8.5, 9.5, "scp860_entrant");
        cleanup(Helper, User);
        fill(User);
        List<ItemStack> Before = snapshot(User);
        int Count = stacks(User);
        Vec3 Front = Vec3.atBottomCenterOf(Frame.getBlockPos().north(2));
        Vec3 Into = Vec3.atBottomCenterOf(Frame.getBlockPos());
        User.moveTo(Front.x, Front.y, Front.z, 0.0f, 0.0f);
        User.setPos(Into.x, Into.y, Into.z);
        User.xo = Front.x;
        User.yo = Front.y;
        User.zo = Front.z;
        Helper.runAfterDelay(4, () -> {
            try {
                Helper.assertTrue(Scp860Forest.inside(User), "walking into the open door did not lead to the forest");
                Helper.assertTrue(empty(User), "the entrant kept items in the forest");
                Scp860Record Ledger = Scp860Forest.record(User);
                Helper.assertTrue(Ledger.Active && Ledger.Items.size() == Count, "the record holds " + Ledger.Items.size() + " stacks instead of " + Count);
                Helper.assertTrue(Ledger.ReturnPos.distanceTo(Front) < 1.6, "the return spot " + Ledger.ReturnPos + " is not in front of the door at " + Front);
                Helper.assertTrue(Scp860Trip.leave(User, Scp860Trip.Reason.Exit), "the entrant could not leave");
                Helper.assertTrue(same(Before, User), "leaving did not restore the exact inventory");
                Helper.assertTrue(Scp860Trip.restore(User) == 0, "the stash restored a second time");
                Helper.assertTrue(same(Before, User), "a second restore changed the inventory");
                Helper.assertTrue(User.level() == Helper.getLevel() && User.position().distanceTo(Front) < 1.6, "the entrant did not come back in front of the door");
            } finally {
                finish(Helper, User);
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp860_deathRestores")
    public static void deathRestores(GameTestHelper Helper) {
        fresh(Helper);
        Scp860TestPlayer User = player(Helper, 6.5, 6.5, "scp860_fallen");
        Scp860TestPlayer Fresh = null;
        try {
            fill(User);
            User.experienceLevel = 5;
            User.totalExperience = 80;
            List<ItemStack> Before = snapshot(User);
            Vec3 Home = User.position();
            Helper.assertTrue(send(Helper, User), "the player could not enter");
            ServerLevel Forest = User.serverLevel();
            DamageSource Source = Forest.damageSources().genericKill();
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(User, Source));
            List<ItemEntity> Drops = new ArrayList<>();
            Drops.add(new ItemEntity(Forest, User.getX(), User.getY(), User.getZ(), new ItemStack(Items.STICK)));
            Helper.assertTrue(NeoForge.EVENT_BUS.post(new LivingDropsEvent(User, Source, Drops, false)).isCanceled(), "death drops were allowed in the forest");
            Helper.assertTrue(NeoForge.EVENT_BUS.post(new LivingExperienceDropEvent(User, null, 30)).isCanceled(), "experience dropped in the forest");
            DimensionTransition Original = new DimensionTransition(Helper.getLevel().getServer().overworld(), Vec3.ZERO, Vec3.ZERO, 0.0f, 0.0f, DimensionTransition.DO_NOTHING);
            PlayerRespawnPositionEvent Where = NeoForge.EVENT_BUS.post(new PlayerRespawnPositionEvent(User, Original, false));
            Helper.assertTrue(Where.getDimensionTransition().newLevel() == Helper.getLevel() && Where.getDimensionTransition().pos().distanceTo(Home) < 1.6,
                    "the respawn is not at the last safe spot: " + Where.getDimensionTransition().pos());
            Fresh = new Scp860TestPlayer(Helper.getLevel(), User.getGameProfile());
            Fresh.restoreFrom(User, false);
            NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(Fresh, User, true));
            dismiss(User);
            Vec3 Back = Where.getDimensionTransition().pos();
            Fresh.moveTo(Back.x, Back.y, Back.z, 0.0f, 0.0f);
            Helper.getLevel().addNewPlayer(Fresh);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(Fresh, false));
            Helper.assertTrue(same(Before, Fresh), "dying in the forest did not give everything back");
            Helper.assertTrue(Fresh.experienceLevel == 5, "experience came back as " + Fresh.experienceLevel);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(Fresh, false));
            Helper.assertTrue(same(Before, Fresh) && !Scp860Forest.record(Fresh).Active, "a second respawn restored again");
        } finally {
            finish(Helper, User);
            if (Fresh != null) {
                finish(Helper, Fresh);
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp860_rejoinRestores")
    public static void rejoinRestores(GameTestHelper Helper) {
        fresh(Helper);
        Scp860TestPlayer User = player(Helper, 6.5, 6.5, "scp860_rejoiner");
        try {
            fill(User);
            List<ItemStack> Before = snapshot(User);
            Vec3 Home = User.position();
            Helper.assertTrue(send(Helper, User), "the player could not enter");
            Helper.assertTrue(Scp860Trip.loggedIn(User), "rejoining inside did not send the player out");
            Helper.assertTrue(User.level() == Helper.getLevel() && User.position().distanceTo(Home) < 1.6, "the rejoining player was not put back at the safe spot");
            Helper.assertTrue(same(Before, User), "rejoining did not give everything back");
            Helper.assertFalse(Scp860Trip.loggedIn(User), "a second login moved the player again");
            Helper.assertTrue(same(Before, User), "a second login changed the inventory");
            Vec3 Safe = Helper.absoluteVec(new Vec3(18.5, 1.0, 18.5));
            Scp860Trip.forget(User.getUUID());
            Helper.assertTrue(send(Helper, User), "the player could not enter a second time");
            Scp860Record Ledger = Scp860Forest.record(User);
            Ledger.ReturnDimension = ResourceKey.create(Registries.DIMENSION, Scp860Registry.location("nowhere"));
            Ledger.SafeDimension = Helper.getLevel().dimension();
            Ledger.SafePos = Safe;
            Helper.assertTrue(Scp860Trip.loggedIn(User), "rejoining with a lost door did not send the player out");
            Helper.assertTrue(User.level() == Helper.getLevel() && User.position().distanceTo(Safe) < 1.6, "the fallback did not use the last safe position: " + User.position());
            Helper.assertTrue(same(Before, User), "the fallback rejoin did not give everything back");
        } finally {
            finish(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp860_layoutResets")
    public static void layoutResets(GameTestHelper Helper) {
        fresh(Helper);
        Scp860TestPlayer First = player(Helper, 4.5, 4.5, "scp860_first");
        Scp860TestPlayer Second = player(Helper, 12.5, 4.5, "scp860_second");
        cleanup(Helper, First, Second);
        Scp860State State = Scp860State.get(Helper.getLevel().getServer());
        Helper.assertTrue(send(Helper, First), "the first player could not enter");
        Helper.assertTrue(send(Helper, Second), "the second player could not enter");
        long Seed = State.seed();
        int Generation = State.generation();
        Helper.assertTrue(Scp860Forest.inside(First) && Scp860Forest.inside(Second), "both players should be in the forest");
        Helper.assertTrue(First.position().distanceTo(Second.position()) < 4.0, "two entrants arrived apart: " + First.position() + " and " + Second.position());
        Helper.assertTrue(Scp860Forest.participants(First.serverLevel()).size() == 2, "the shared forest does not hold both players");
        Helper.runAfterDelay(3, () -> {
            Helper.assertTrue(Scp860Trip.leave(First, Scp860Trip.Reason.Exit), "the first player could not leave");
            Helper.runAfterDelay(3, () -> {
                Helper.assertTrue(State.seed() == Seed, "the forest reset while someone was still inside");
                Helper.assertTrue(Scp860Trip.leave(Second, Scp860Trip.Reason.Exit), "the second player could not leave");
                Helper.runAfterDelay(3, () -> {
                    try {
                        Helper.assertTrue(State.seed() != Seed && State.generation() == Generation + 1, "the empty forest did not reset to a new layout");
                        Helper.assertTrue(Scp860Forest.plan(Helper.getLevel().getServer()).Seed == State.seed(), "the layout plan did not follow the new seed");
                    } finally {
                        finish(Helper, First, Second);
                    }
                    Helper.succeed();
                });
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp860_sharedDoors")
    public static void sharedDoors(GameTestHelper Helper) {
        fresh(Helper);
        Scp860DoorBlockEntity One = door(Helper, DoorSpot);
        Scp860DoorBlockEntity Two = door(Helper, SecondSpot);
        One.insert(new ItemStack(Scp860Registry.Key.get()), null);
        Two.insert(new ItemStack(Scp860Registry.Key.get()), null);
        One.setOpened(true);
        Two.setOpened(true);
        Scp860TestPlayer First = player(Helper, 8.5, 9.5, "scp860_door_one");
        Scp860TestPlayer Second = player(Helper, 16.5, 9.5, "scp860_door_two");
        cleanup(Helper, First, Second);
        for (ServerPlayer User : List.of(First, Second)) {
            Scp860DoorBlockEntity Frame = User == First ? One : Two;
            Vec3 Front = Vec3.atBottomCenterOf(Frame.getBlockPos().north(2));
            Vec3 Into = Vec3.atBottomCenterOf(Frame.getBlockPos());
            User.moveTo(Front.x, Front.y, Front.z, 0.0f, 0.0f);
            User.setPos(Into.x, Into.y, Into.z);
            User.xo = Front.x;
            User.zo = Front.z;
        }
        Helper.runAfterDelay(4, () -> {
            try {
                Helper.assertTrue(Scp860Forest.inside(First) && Scp860Forest.inside(Second), "players entering through two doors did not both reach the forest");
                Helper.assertTrue(First.position().distanceTo(Second.position()) < 4.0, "two doors led to different places: " + First.position() + " and " + Second.position()
                        + " in " + First.level().dimension().location() + " and " + Second.level().dimension().location() + ", arrival " + Scp860Forest.arrival(First.server, 0));
                Helper.assertTrue(One.sealed() && Two.sealed(), "both keyed doors should be held while the forest is occupied");
            } finally {
                finish(Helper, First, Second);
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp860_offPathAttack")
    public static void offPathAttack(GameTestHelper Helper) {
        fresh(Helper);
        Scp860TestPlayer User = player(Helper, 6.5, 6.5, "scp860_stray");
        cleanup(Helper, User);
        Helper.assertTrue(send(Helper, User), "the player could not enter");
        ServerLevel Forest = User.serverLevel();
        Vec3 Stray = offPathSpot(Forest, User, 5.0, 9.0);
        User.teleportTo(Forest, Stray.x, Stray.y, Stray.z, 0.0f, 0.0f);
        Vec3 Lurk = offPathSpot(Forest, User, 5.0, 12.0);
        Scp860Watcher Watcher = watcher(Forest, Lurk, User);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Scp860Director.offPath(User), "the player is not off the path: " + moodOf(User));
            Helper.assertTrue(current(Watcher).isHunting(User), "stepping off the path did not set the watcher on the player: " + moodOf(User) + ", " + watcherOf(current(Watcher)));
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp860_patienceAggression")
    public static void patienceAggression(GameTestHelper Helper) {
        fresh(Helper);
        Scp860TestPlayer User = player(Helper, 6.5, 6.5, "scp860_idler");
        cleanup(Helper, User);
        Helper.assertTrue(send(Helper, User), "the player could not enter");
        ServerLevel Forest = User.serverLevel();
        Vec3 Lurk = offPathSpot(Forest, User, 6.0, 12.0);
        Scp860Watcher Watcher = watcher(Forest, Lurk, User);
        int[] Waited = {0};
        Helper.onEachTick(() -> {
            Waited[0]++;
            if (Waited[0] < 40) {
                if (current(Watcher).isHunting(User)) {
                    Helper.fail("the watcher attacked a player standing on the path too early: " + watcherOf(current(Watcher)));
                }
                return;
            }
            if (Waited[0] == 40 && Scp860Director.patience(User) <= 0.0f) {
                Helper.fail("standing still did not fill patience: " + moodOf(User));
            }
            if (current(Watcher).isHunting(User)) {
                Helper.assertFalse(Scp860Director.offPath(User), "the idler wandered off the path: " + moodOf(User));
                Helper.succeed();
                return;
            }
            if (Waited[0] > 330) {
                Helper.fail("running out of patience did not set the watcher on the player: " + moodOf(User) + ", " + watcherOf(current(Watcher)));
            }
            Scp860Director.Mood State = Scp860Director.peek(User.server, User.getUUID());
            if (State != null && State.Patience < 1.0f) {
                State.Patience = 1.0f;
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp860_movingStaysSafe")
    public static void movingStaysSafe(GameTestHelper Helper) {
        fresh(Helper);
        Scp860TestPlayer User = player(Helper, 6.5, 6.5, "scp860_hiker");
        cleanup(Helper, User);
        Helper.assertTrue(send(Helper, User), "the player could not enter");
        ServerLevel Forest = User.serverLevel();
        Vec3 Lurk = offPathSpot(Forest, User, 6.0, 12.0);
        Scp860Watcher Watcher = watcher(Forest, Lurk, User);
        int[] Step = {0};
        Helper.onEachTick(() -> {
            if (Step[0] < 240 && Step[0] % 3 == 0) {
                int Sample = Math.min(Scp860Forest.plan(Forest.getServer()).samples() - 1, Step[0] / 3);
                Vec3 Point = Scp860Forest.pathPoint(Forest.getServer(), Sample);
                User.teleportTo(Forest, Point.x, Point.y, Point.z, User.getYRot(), 0.0f);
            }
            Step[0]++;
            if (current(Watcher).isHunting(User)) {
                throw new GameTestAssertException("a watcher attacked a player walking the path: " + moodOf(User) + ", " + watcherOf(current(Watcher)));
            }
        });
        Helper.runAfterDelay(240, () -> {
            try {
                Helper.assertTrue(Scp860Director.patience(User) < Scp860Director.RustleAt, "walking the path left patience at " + Scp860Director.patience(User));
                Helper.assertFalse(Scp860Director.offPath(User), "the hiker left the path");
            } finally {
                finish(Helper, User);
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp860_chaser_follows_out")
    public static void chaserFollowsOut(GameTestHelper Helper) {
        fresh(Helper);
        Scp860TestPlayer User = player(Helper, 6.5, 6.5, "scp860_chased");
        Helper.assertTrue(send(Helper, User), "the player could not enter");
        ServerLevel Forest = User.serverLevel();
        Vec3 Home = Scp860Forest.record(User).ReturnPos;
        Scp860Watcher Chaser = watcher(Forest, offPathSpot(Forest, User, 4.5, 9.0), User);
        Scp860Watcher Bystander = watcher(Forest, offPathSpot(Forest, User, 9.0, 12.0), User);
        Chaser.hunt(User);
        try {
            Helper.assertTrue(Chaser.distanceTo(User) <= Scp860Watcher.FollowReach, "the chaser is too far away to follow");
            Helper.assertTrue(Scp860Trip.leave(User, Scp860Trip.Reason.Exit), "the player could not leave");
            List<Scp860Watcher> Out = watchersNear(Helper.getLevel(), Home, 8.0);
            Helper.assertTrue(Out.size() == 1, "expected one watcher to follow out, found " + Out.size());
            Scp860Watcher Followed = Out.get(0);
            Helper.assertFalse(Followed.forestBound(), "the watcher that followed out still behaves as if in the forest");
            Helper.assertTrue(Followed.angerTicks() > 0 && User.getUUID().equals(Followed.angerTarget()), "the watcher that followed out is not angry at the player");
            Helper.assertTrue(StasisPods.refusal(User, new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint)), Followed) == null, "a stasis pod refuses the watcher that followed out");
            Helper.assertTrue(Bystander.isAlive() && Bystander.level() == Forest && Bystander.forestBound(), "a watcher that was not chasing followed the player out");
        } finally {
            finish(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp860_watcherStaysBehind")
    public static void watcherStaysBehind(GameTestHelper Helper) {
        fresh(Helper);
        Scp860TestPlayer User = player(Helper, 6.5, 6.5, "scp860_watched");
        Helper.assertTrue(send(Helper, User), "the player could not enter");
        ServerLevel Forest = User.serverLevel();
        Vec3 Home = Scp860Forest.record(User).ReturnPos;
        Scp860Watcher Watcher = watcher(Forest, offPathSpot(Forest, User, 4.5, 9.0), User);
        try {
            Helper.assertTrue(Scp860Trip.leave(User, Scp860Trip.Reason.Exit), "the player could not leave");
            Helper.assertTrue(watchersNear(Helper.getLevel(), Home, 16.0).isEmpty(), "a watcher that was only watching followed the player out");
            Helper.assertTrue(Watcher.isAlive() && Watcher.level() == Forest, "the watching watcher left the forest");
        } finally {
            finish(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp860_noFollowOnDeathOrLogout")
    public static void noFollowOnDeathOrLogout(GameTestHelper Helper) {
        fresh(Helper);
        Scp860TestPlayer User = player(Helper, 6.5, 6.5, "scp860_quitter");
        Scp860TestPlayer Fresh = null;
        try {
            Helper.assertTrue(send(Helper, User), "the player could not enter");
            ServerLevel Forest = User.serverLevel();
            Vec3 Home = Scp860Forest.record(User).ReturnPos;
            Scp860Watcher First = watcher(Forest, offPathSpot(Forest, User, 4.5, 9.0), User);
            First.hunt(User);
            Helper.assertTrue(Scp860Trip.loggedIn(User), "rejoining did not send the player out");
            Helper.assertTrue(watchersNear(Helper.getLevel(), Home, 16.0).isEmpty(), "a chaser followed a player who logged out");
            Scp860Trip.forget(User.getUUID());
            Helper.assertTrue(send(Helper, User), "the player could not enter again");
            Forest = User.serverLevel();
            Scp860Watcher Second = watcher(Forest, offPathSpot(Forest, User, 4.5, 9.0), User);
            Second.hunt(User);
            DamageSource Source = Forest.damageSources().genericKill();
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(User, Source));
            DimensionTransition Original = new DimensionTransition(Helper.getLevel().getServer().overworld(), Vec3.ZERO, Vec3.ZERO, 0.0f, 0.0f, DimensionTransition.DO_NOTHING);
            PlayerRespawnPositionEvent Where = NeoForge.EVENT_BUS.post(new PlayerRespawnPositionEvent(User, Original, false));
            Fresh = new Scp860TestPlayer(Helper.getLevel(), User.getGameProfile());
            Fresh.restoreFrom(User, false);
            NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(Fresh, User, true));
            dismiss(User);
            Vec3 Back = Where.getDimensionTransition().pos();
            Fresh.moveTo(Back.x, Back.y, Back.z, 0.0f, 0.0f);
            Helper.getLevel().addNewPlayer(Fresh);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(Fresh, false));
            Helper.assertTrue(watchersNear(Helper.getLevel(), Back, 16.0).isEmpty(), "a chaser followed a player who died");
        } finally {
            finish(Helper, User);
            if (Fresh != null) {
                finish(Helper, Fresh);
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp860_stasis")
    public static void stasisAccepts(GameTestHelper Helper) {
        fresh(Helper);
        ServerLevel World = Helper.getLevel();
        Scp860TestPlayer User = player(Helper, 4.5, 4.5, "scp860_keeper");
        try {
            Scp860Watcher Target = Scp860Registry.Watcher.get().create(World);
            Helper.assertTrue(Target != null, "SCP-860-2 could not be created");
            Vec3 Spot = Helper.absoluteVec(new Vec3(12.5, 1.0, 12.5));
            Target.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
            Target.bindToForest(0);
            World.addFreshEntity(Target);
            ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
            Helper.assertTrue(StasisPods.refusal(User, Pod, Target) == null, "a stasis pod refuses SCP-860-2");
            Helper.assertTrue(StasisPods.capture(World, User, Pod, Target), "a stasis pod failed to capture SCP-860-2");
            Helper.assertTrue(Target.isRemoved(), "the captured SCP-860-2 is still in the world");
            Helper.assertTrue(StasisPods.breakOut(World, Pod, Spot, User), "the captured SCP-860-2 could not be released");
            List<Scp860Watcher> Out = World.getEntitiesOfClass(Scp860Watcher.class, new AABB(Spot, Spot).inflate(4.0));
            Helper.assertTrue(Out.size() == 1, "expected one released SCP-860-2, found " + Out.size());
            Scp860Watcher Released = Out.get(0);
            Helper.assertFalse(Released.forestBound(), "a released SCP-860-2 still behaves as if in its forest");
            Helper.assertTrue(Released.home() != null && Released.isPersistenceRequired(), "a released SCP-860-2 has no home or may despawn");
            Helper.assertFalse(Released.removeWhenFarAway(1000.0), "a released SCP-860-2 despawns when far away");
        } finally {
            finish(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp860_isolation")
    public static void isolation(GameTestHelper Helper) {
        fresh(Helper);
        Scp860TestPlayer Walker = player(Helper, 4.5, 4.5, "scp860_member");
        Scp860TestPlayer Outsider = player(Helper, 12.5, 4.5, "scp860_outsider");
        try {
            Helper.assertTrue(send(Helper, Walker), "the walker could not enter");
            ServerLevel Forest = Walker.serverLevel();
            Vec3 Near = Walker.position().add(2.0, 0.0, 2.0);
            Outsider.teleportTo(Forest, Near.x, Near.y, Near.z, 0.0f, 0.0f);
            Helper.assertTrue(Scp860Rules.bound(Walker), "the participant is not bound by the forest rules");
            Helper.assertFalse(Scp860Rules.bound(Outsider), "the forest rules bind a player who never entered");
            Helper.assertFalse(Scp860Forest.participants(Forest).contains(Outsider), "the outsider counts as a participant");
            BlockPos Ground = BlockPos.containing(Walker.position()).below();
            BlockState Below = Forest.getBlockState(Ground);
            Helper.assertTrue(NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(Forest, Ground, Below, Walker)).isCanceled(), "a participant could break the forest floor");
            Helper.assertFalse(NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(Forest, Ground, Below, Outsider)).isCanceled(), "the rules stopped an outsider breaking a block");
            Helper.assertTrue(Scp860Forest.record(Outsider).itemCount() == 0 && !Scp860Forest.record(Outsider).Active, "the outsider was stashed");
            Scp860Watcher Watcher = watcher(Forest, offPathSpot(Forest, Walker, 5.0, 10.0), Walker);
            Scp860Director.pack(Forest, Walker, new ArrayList<>(List.of(Watcher)));
            Helper.assertFalse(Watcher.isHunting(Outsider), "a watcher turned on the outsider");
        } finally {
            finish(Helper, Walker, Outsider);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp860_strangerDelivers")
    public static void strangerDelivers(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp860Registry.DocumentId);
        Helper.assertTrue(Document != null, "the SCP-860 dossier did not load");
        Helper.assertTrue(Document.Classification() == DocumentClass.Euclid, "SCP-860 is not filed as Euclid");
        Helper.assertTrue(Document.Condition() instanceof StrangerEncounterCondition, "SCP-860 does not arrive with a stranger");
        StrangerPlan Plan = ((StrangerEncounterCondition) Document.Condition()).plan();
        Helper.assertTrue("scp860_businessman".equals(Plan.Variant()), "SCP-860 is delivered by " + Plan.Variant());
        Helper.assertTrue(Plan.Gifts().stream().anyMatch(Gift -> Gift.toStack().is(Scp860Registry.Key.get())), "the businessman does not bring the key");
        Helper.assertTrue(Plan.Gifts().stream().anyMatch(Gift -> Gift.toStack().is(Scp860Registry.DoorItem.get())), "the businessman does not bring the door");
        Helper.assertTrue(AnomalySpawners.spawnerFor(Scp860Registry.DocumentId).isPresent(), "SCP-860 has no arrival hook");
        Scp860TestPlayer Reader = player(Helper, 8.5, 8.5, "scp860_reader");
        try {
            BlockPos Where = Helper.absolutePos(new BlockPos(8, 1, 8));
            AnomalySpawners.spawnerFor(Scp860Registry.DocumentId).get().spawn(new AnomalySpawners.Discovery(World, Where, Reader, Scp860Registry.DocumentId, Document, false));
            for (BlockPos Pos : BlockPos.betweenClosed(Where.offset(-16, -1, -16), Where.offset(16, 8, 16))) {
                Helper.assertFalse(Scp860DoorBlock.isDoor(World.getBlockState(Pos)), "the arrival hook placed the door in the world at " + Pos);
            }
        } finally {
            finish(Helper, Reader);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp860_lang")
    public static void langLoaded(GameTestHelper Helper) {
        Language Words = Language.getInstance();
        for (String Key : List.of("item.scp_unleashed.scp860_key", "block.scp_unleashed.scp860_door", "entity.scp_unleashed.scp860_2", "dossier.scp_unleashed.860.title",
                "dossier.scp_unleashed.860.page.0.title", "stranger.scp_unleashed.scp860.3", "archive.scp_unleashed.notes.scp860.1",
                "entity.scp_unleashed.scp860_businessman", "gui.scp_unleashed.stranger.plate.scp860_businessman", "config.jade.plugin_scp_unleashed.scp860_door",
                "config.jade.plugin_scp_unleashed.scp860_2", "jei.scp_unleashed.info.scp860_key.1", "subtitles.scp_unleashed.scp860_growl", "biome.scp_unleashed.scp860_forest")) {
            Helper.assertTrue(Words.has(Key), "the server has no text for " + Key);
        }
        Helper.assertFalse(Words.has("stranger.scp_unleashed.scp860.4"), "the businessman has more pages than his variant shows");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp860_planShape")
    public static void planShape(GameTestHelper Helper) {
        double Shortest = Double.MAX_VALUE;
        double Longest = 0.0;
        int FewestCells = Integer.MAX_VALUE;
        int MostCells = 0;
        int MostBranches = 0;
        int FewestBranches = Integer.MAX_VALUE;
        for (long Seed = 1; Seed <= 24; Seed++) {
            Scp860Plan Layout = new Scp860Plan(Seed * 7919L);
            Shortest = Math.min(Shortest, Layout.RouteLength);
            Longest = Math.max(Longest, Layout.RouteLength);
            FewestCells = Math.min(FewestCells, Layout.routeCells());
            MostCells = Math.max(MostCells, Layout.routeCells());
            MostBranches = Math.max(MostBranches, Layout.Branches);
            FewestBranches = Math.min(FewestBranches, Layout.Branches);
            Helper.assertTrue(Layout.distance(Layout.ArrivalX, Layout.ArrivalZ) < 1.0, "seed " + Seed + " arrives off the path");
            double DoorGap = Layout.distance(Layout.ExitDoorX + 0.5, Layout.ExitDoorZ + 0.5);
            Helper.assertTrue(DoorGap < 3.0, "seed " + Seed + " leaves the exit door " + DoorGap + " blocks from the path");
            Helper.assertTrue(Layout.Branches <= Scp860Plan.MaxBranches, "seed " + Seed + " has too many dead ends");
        }
        Log.info("SCP-860 layouts: route {} to {} blocks over {} to {} cells, {} to {} dead-end branches", (int) Shortest, (int) Longest, FewestCells, MostCells,
                FewestBranches, MostBranches);
        Helper.assertTrue(FewestBranches >= 2, "a layout had only " + FewestBranches + " forks");
        Helper.assertTrue(FewestCells >= 20 && MostCells <= Scp860Plan.MaxRoute + 1, "route cells ran from " + FewestCells + " to " + MostCells);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp860_creative_tabs")
    public static void creativeTabs(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        CreativeModeTab.ItemDisplayParameters Parameters = new CreativeModeTab.ItemDisplayParameters(World.enabledFeatures(), true, World.registryAccess());
        CreativeModeTab BlockTab = ModCreativeTabs.ScpBlocks.get();
        CreativeModeTab ItemTab = ModCreativeTabs.ScpItems.get();
        BlockTab.buildContents(Parameters);
        ItemTab.buildContents(Parameters);
        Helper.assertTrue(listed(BlockTab, Scp860Registry.DoorItem.get()), "the SCP-860 door is missing from SCP Blocks");
        Helper.assertTrue(listed(ItemTab, Scp860Registry.Key.get()), "SCP-860-1 is missing from SCP Items");
        Helper.assertTrue(listed(ItemTab, Scp860Registry.Egg.get()), "the SCP-860-2 spawn egg is missing from SCP Items");
        Helper.assertFalse(listed(ItemTab, Scp860Registry.DoorItem.get()), "the SCP-860 door is listed in SCP Items instead of SCP Blocks");
        Item Far = Scp860Registry.FarDoor.get().asItem();
        for (CreativeModeTab Tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            if (Tab.getType() != CreativeModeTab.Type.CATEGORY) {
                continue;
            }
            Tab.buildContents(Parameters);
            Helper.assertFalse(Far != Items.AIR && listed(Tab, Far), "the far door is listed in " + Tab.getDisplayName().getString());
        }
        Helper.succeed();
    }

    static boolean listed(CreativeModeTab Tab, Item Wanted) {
        return Tab.getDisplayItems().stream().anyMatch(Stack -> Stack.is(Wanted));
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp860_forest_integrity")
    public static void forestIntegrity(GameTestHelper Helper) {
        fresh(Helper);
        MinecraftServer Server = Helper.getLevel().getServer();
        ServerLevel Forest = Scp860Forest.level(Server);
        Helper.assertTrue(Forest != null, "the forest level is missing");
        Scp860Plan Layout = Scp860Forest.plan(Server);
        int BaseX = Scp860Forest.baseX();
        int BaseZ = Scp860Forest.baseZ();
        int[][] Centres = {{(BaseX + Mth.floor(Layout.ArrivalX)) >> 4, (BaseZ + Mth.floor(Layout.ArrivalZ)) >> 4},
                {(BaseX + Scp860Forest.Size / 2) >> 4, (BaseZ + Scp860Forest.Size / 2) >> 4},
                {(BaseX + Layout.ExitDoorX) >> 4, (BaseZ + Layout.ExitDoorZ) >> 4}};
        int Plants = 0;
        int Vines = 0;
        int Logs = 0;
        int Leaves = 0;
        int Scanned = 0;
        try {
            for (int[] Centre : Centres) {
                hold(Forest, Centre[0], Centre[1]);
                for (int Dx = -2; Dx <= 2; Dx++) {
                    for (int Dz = -2; Dz <= 2; Dz++) {
                        Scp860Builder.build(Forest, Layout, new ChunkPos(Centre[0] + Dx, Centre[1] + Dz));
                    }
                }
                int LowX = (Centre[0] - 2) << 4;
                int LowZ = (Centre[1] - 2) << 4;
                int Span = 80;
                java.util.Set<BlockPos> Held = new java.util.HashSet<>();
                java.util.ArrayDeque<BlockPos> Queue = new java.util.ArrayDeque<>();
                BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
                for (int X = LowX; X < LowX + Span; X++) {
                    for (int Z = LowZ; Z < LowZ + Span; Z++) {
                        for (int Y = Scp860Builder.Bottom + 1; Y <= Scp860Builder.Top; Y++) {
                            Cursor.set(X, Y, Z);
                            if (Scp860Builder.isLog(Forest.getBlockState(Cursor)) && Scp860Builder.isSoil(Forest.getBlockState(Cursor.below()))) {
                                Held.add(Cursor.immutable());
                                Queue.add(Cursor.immutable());
                            }
                        }
                    }
                }
                while (!Queue.isEmpty()) {
                    BlockPos Spot = Queue.poll();
                    for (Direction Side : Direction.values()) {
                        BlockPos Next = Spot.relative(Side);
                        if (!Held.contains(Next) && Scp860Builder.isLog(Forest.getBlockState(Next))) {
                            Held.add(Next);
                            Queue.add(Next);
                        }
                    }
                }
                for (int X = LowX + 16; X < LowX + 64; X++) {
                    for (int Z = LowZ + 16; Z < LowZ + 64; Z++) {
                        for (int Y = Scp860Builder.Bottom + 1; Y <= Scp860Builder.Top; Y++) {
                            Cursor.set(X, Y, Z);
                            BlockState Here = Forest.getBlockState(Cursor);
                            if (Here.isAir()) {
                                continue;
                            }
                            Scanned++;
                            BlockState Below = Forest.getBlockState(Cursor.below());
                            if (Scp860Builder.isSmallPlant(Here) && !Scp860Builder.isSoil(Below)) {
                                Plants++;
                            } else if (Here.getBlock() instanceof DoublePlantBlock) {
                                boolean Lower = Here.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER;
                                BlockState Other = Forest.getBlockState(Lower ? Cursor.above() : Cursor.below());
                                if (!Other.is(Here.getBlock()) || Lower && !Scp860Builder.isSoil(Below)) {
                                    Plants++;
                                }
                            } else if (Here.is(Blocks.VINE)) {
                                BlockState Above = Forest.getBlockState(Cursor.above());
                                for (Direction Side : Direction.Plane.HORIZONTAL) {
                                    BooleanProperty Face = VineBlock.getPropertyForFace(Side);
                                    if (!Here.getValue(Face)) {
                                        continue;
                                    }
                                    BlockState Behind = Forest.getBlockState(Cursor.relative(Side));
                                    if (!Scp860Builder.isLog(Behind) && !Scp860Builder.isLeaf(Behind) && !(Above.is(Blocks.VINE) && Above.getValue(Face))) {
                                        Vines++;
                                    }
                                }
                            } else if (Scp860Builder.isLog(Here) && !Held.contains(Cursor.immutable())) {
                                Logs++;
                            } else if (Scp860Builder.isLeaf(Here) && !leafHeld(Forest, Cursor.immutable(), Held)) {
                                Leaves++;
                            }
                        }
                    }
                }
                if (Centre == Centres[2]) {
                    dumpForest(Forest, Layout, LowX + 8, LowZ + 8, 64);
                }
                release();
            }
        } finally {
            release();
        }
        Log.info("SCP-860 forest integrity: {} blocks scanned, {} unsupported plants, {} unsupported vine faces, {} loose logs, {} loose leaves", Scanned, Plants, Vines,
                Logs, Leaves);
        Helper.assertTrue(Scanned > 1000, "the integrity scan found almost nothing to check (" + Scanned + " blocks)");
        Helper.assertTrue(Plants == 0, Plants + " plants have no support");
        Helper.assertTrue(Vines == 0, Vines + " vine faces have no support");
        Helper.assertTrue(Logs == 0, Logs + " logs are cut off from their trunk");
        Helper.assertTrue(Leaves == 0, Leaves + " leaves hang free of any log");
        Helper.succeed();
    }

    static void dumpForest(ServerLevel World, Scp860Plan Layout, int LowX, int LowZ, int Span) {
        List<String> Lines = new ArrayList<>();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int X = LowX; X < LowX + Span; X++) {
            for (int Z = LowZ; Z < LowZ + Span; Z++) {
                for (int Y = Scp860Builder.Bottom + 1; Y <= Scp860Builder.Top; Y++) {
                    BlockState Here = World.getBlockState(Cursor.set(X, Y, Z));
                    if (!Here.isAir()) {
                        Lines.add("\"" + (X - LowX) + "," + (Y - Scp860Forest.Ground) + "," + (Z - LowZ) + "\": \""
                                + net.minecraft.commands.arguments.blocks.BlockStateParser.serialize(Here) + "\"");
                    }
                }
            }
        }
        List<String> Path = new ArrayList<>();
        for (int Index = 0; Index < Layout.samples(); Index++) {
            double X = Scp860Forest.baseX() + Layout.sampleX(Index) - LowX;
            double Z = Scp860Forest.baseZ() + Layout.sampleZ(Index) - LowZ;
            if (X > -8 && Z > -8 && X < Span + 8 && Z < Span + 8) {
                Path.add("[" + X + "," + Z + "]");
            }
        }
        String Exit = "[" + (Scp860Forest.baseX() + Layout.ExitDoorX - LowX) + "," + (Scp860Forest.Ground + 1 - Scp860Forest.Ground) + ","
                + (Scp860Forest.baseZ() + Layout.ExitDoorZ - LowZ) + ",\"" + Layout.ExitFacing.getSerializedName() + "\"]";
        try {
            java.nio.file.Files.writeString(net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("scp860_forest_dump.json"),
                    "{\"exit\": " + Exit + ", \"path\": [" + String.join(",", Path) + "], \"blocks\": {" + String.join(",\n", Lines) + "}}\n");
        } catch (java.io.IOException Problem) {
            Log.warn("could not write the forest dump", Problem);
        }
    }

    static boolean leafHeld(ServerLevel World, BlockPos Start, java.util.Set<BlockPos> Logs) {
        if (Scp860Builder.isSoil(World.getBlockState(Start.below()))) {
            return true;
        }
        java.util.Map<BlockPos, Integer> Seen = new java.util.HashMap<>();
        java.util.ArrayDeque<BlockPos> Queue = new java.util.ArrayDeque<>();
        Seen.put(Start, 0);
        Queue.add(Start);
        while (!Queue.isEmpty()) {
            BlockPos Spot = Queue.poll();
            int Steps = Seen.get(Spot);
            for (Direction Side : Direction.values()) {
                BlockPos Next = Spot.relative(Side);
                BlockState There = World.getBlockState(Next);
                if (Logs.contains(Next) || Scp860Builder.isLeaf(There) && Scp860Builder.isSoil(World.getBlockState(Next.below()))) {
                    return true;
                }
                if (Steps + 1 < Scp860Builder.LeafReach && !Seen.containsKey(Next) && Scp860Builder.isLeaf(There)) {
                    Seen.put(Next, Steps + 1);
                    Queue.add(Next);
                }
            }
        }
        return false;
    }
}
