package net.rai.scp_unleashed.scp902.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.scp902.Scp902Box;
import net.rai.scp_unleashed.scp902.Scp902BoxBlock;
import net.rai.scp_unleashed.scp902.Scp902BoxBlockEntity;
import net.rai.scp_unleashed.scp902.Scp902BoxItem;
import net.rai.scp_unleashed.scp902.Scp902Cleanup;
import net.rai.scp_unleashed.scp902.Scp902Copy;
import net.rai.scp_unleashed.scp902.Scp902Privacy;
import net.rai.scp_unleashed.scp902.Scp902Prop;
import net.rai.scp_unleashed.scp902.Scp902Registry;
import net.rai.scp_unleashed.scp902.Scp902Scene;
import net.rai.scp_unleashed.scp902.Scp902State;
import net.rai.scp_unleashed.scp902.Scp902Trip;
import net.rai.scp_unleashed.scp902.Scp902Wastes;
import net.rai.scp_unleashed.stranger.StrangerEncounterCondition;
import net.rai.scp_unleashed.stranger.StrangerGift;
import net.rai.scp_unleashed.stranger.StrangerPlan;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp902GameTests {
    public static final String Arena = "scp902_arena";
    public static final BlockPos Spot = new BlockPos(8, 1, 8);
    public static final int Departed = Scp902Scene.DepartTicks + 3;
    public static final double PathWalk = 0.075;
    public static final double PathDrift = 0.023;
    public static final int WindowTicks = 40;
    public static final double MinimumWindow = 1.5;
    public static final double MinimumProgress = 50.0;

    private Scp902GameTests() {
    }

    public static void wastes(GameTestHelper Helper) {
        if (Helper.getLevel().getServer().getLevel(Scp902Registry.Wastes) == null) {
            Scp902Wastes.standIn(Level.END);
        }
    }

    public static void fresh(GameTestHelper Helper) {
        wastes(Helper);
        Scp902State.get(Helper.getLevel().getServer()).resetAll();
        Scp902Trip.forgetAll();
        Scp902Cleanup.forget(Helper.getLevel().getServer());
    }

    private static Scp902TestPlayer player(GameTestHelper Helper, double X, double Z, String Name) {
        wastes(Helper);
        ServerLevel World = Helper.getLevel();
        Scp902TestPlayer User = new Scp902TestPlayer(World, Name);
        Vec3 Where = Helper.absoluteVec(new Vec3(X, 1.0, Z));
        User.moveTo(Where.x, Where.y, Where.z, 37.0f, 12.0f);
        User.setYHeadRot(37.0f);
        World.addNewPlayer(User);
        User.enlist(true);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(ServerPlayer User) {
        Scp902State State = Scp902State.get(User.server);
        Scp902State.Trip Each = State.trip(User.getUUID());
        if (Each != null) {
            Scp902Trip.abandon(User, Each);
        }
        if (User instanceof Scp902TestPlayer Tester) {
            Tester.enlist(false);
        }
        if (!User.isRemoved()) {
            User.serverLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static Scp902BoxBlockEntity place(GameTestHelper Helper, BlockPos Relative) {
        Helper.setBlock(Relative, Scp902Registry.Box.get().defaultBlockState().setValue(Scp902BoxBlock.Facing, Direction.NORTH));
        if (Helper.getLevel().getBlockEntity(Helper.absolutePos(Relative)) instanceof Scp902BoxBlockEntity Tin) {
            return Tin;
        }
        throw new IllegalStateException("The box at " + Relative + " has no block entity");
    }

    private static InteractionResult click(ServerPlayer User, BlockPos Absolute, boolean Sneaking) {
        User.setShiftKeyDown(Sneaking);
        InteractionResult Result = User.gameMode.useItemOn(User, User.serverLevel(), User.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(Absolute).add(0.0, 0.1, 0.0), Direction.UP, Absolute, false));
        User.setShiftKeyDown(false);
        return Result;
    }

    private static Map<EquipmentSlot, ItemStack> dress(ServerPlayer User) {
        Map<EquipmentSlot, ItemStack> Worn = new EnumMap<>(EquipmentSlot.class);
        Worn.put(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        Worn.put(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        Worn.put(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));
        Worn.put(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
        Worn.put(EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH, 23));
        for (Map.Entry<EquipmentSlot, ItemStack> Entry : Worn.entrySet()) {
            User.setItemSlot(Entry.getKey(), Entry.getValue().copy());
        }
        User.getInventory().add(new ItemStack(Items.BREAD, 7));
        User.getInventory().add(Scp902Box.fresh());
        return Worn;
    }

    private static List<ItemStack> snapshot(ServerPlayer User) {
        List<ItemStack> Copy = new ArrayList<>();
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            Copy.add(User.getInventory().getItem(Slot).copy());
        }
        return Copy;
    }

    private static boolean same(List<ItemStack> Before, ServerPlayer User) {
        for (int Slot = 0; Slot < Before.size(); Slot++) {
            ItemStack Now = User.getInventory().getItem(Slot);
            if (!ItemStack.matches(Before.get(Slot), Now)) {
                return false;
            }
        }
        return true;
    }

    private static int boxes(ServerPlayer User) {
        int Count = 0;
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            if (Scp902BoxItem.is(User.getInventory().getItem(Slot))) {
                Count += User.getInventory().getItem(Slot).getCount();
            }
        }
        return Count;
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp902_tickingPlaced")
    public static void tickingPlaced(GameTestHelper Helper) {
        Scp902BoxBlockEntity Tin = place(Helper, Spot);
        Helper.startSequence().thenIdle(Scp902Box.Beat * 2 + 2).thenExecute(() -> {
            Helper.assertTrue(Tin.beats() >= 2, "The placed box ticked " + Tin.beats() + " times in two seconds");
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp902_tickingHeld")
    public static void tickingHeld(GameTestHelper Helper) {
        fresh(Helper);
        Scp902TestPlayer User = player(Helper, 4.5, 4.5, "scp902_holder");
        try {
            Helper.assertFalse(Scp902Box.heldBeat(User, Scp902Box.Beat * 10L), "An empty-handed player ticked");
            User.setItemInHand(InteractionHand.OFF_HAND, Scp902Box.fresh());
            Helper.assertTrue(Scp902Box.heldBeat(User, Scp902Box.Beat * 10L), "The box held in the off hand did not tick on the beat");
            Helper.assertFalse(Scp902Box.heldBeat(User, Scp902Box.Beat * 10L + 7L), "The held box ticked off the beat");
            User.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            User.setItemInHand(InteractionHand.MAIN_HAND, Scp902Box.fresh());
            Helper.assertTrue(Scp902Box.heldBeat(User, Scp902Box.Beat * 11L), "The box held in the main hand did not tick");
        } finally {
            dismiss(User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp902_staysEmpty")
    public static void staysEmpty(GameTestHelper Helper) {
        fresh(Helper);
        ServerLevel World = Helper.getLevel();
        place(Helper, Spot);
        BlockPos At = Helper.absolutePos(Spot);
        Helper.setBlock(Spot.above(), Blocks.HOPPER.defaultBlockState());
        Helper.setBlock(Spot.below().east(), Blocks.AIR.defaultBlockState());
        HopperBlockEntity Feeder = (HopperBlockEntity) World.getBlockEntity(At.above());
        Feeder.setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        Scp902TestPlayer User = player(Helper, 8.5, 5.5, "scp902_filler");
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLD_INGOT, 5));
        click(User, At, false);
        Helper.assertTrue(Scp902BoxBlock.isOpen(World.getBlockState(At)), "Using the box did not open it");
        Helper.assertTrue(User.getMainHandItem().is(Items.GOLD_INGOT) && User.getMainHandItem().getCount() == 5, "The box took something from the player's hand");
        Helper.assertTrue(World.getCapability(Capabilities.ItemHandler.BLOCK, At, null) == null, "The box exposes an item handler");
        Helper.assertTrue(World.getCapability(Capabilities.ItemHandler.BLOCK, At, Direction.UP) == null, "The box exposes an item handler on top");
        Helper.startSequence().thenIdle(40).thenExecute(() -> {
            Helper.assertTrue(Feeder.getItem(0).getCount() == 16, "The hopper pushed " + (16 - Feeder.getItem(0).getCount()) + " items into the box");
            Helper.assertTrue(World.getBlockEntity(At) instanceof Scp902BoxBlockEntity, "The box lost its block entity");
            dismiss(User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp902_shiftUseSendsAndCopies")
    public static void shiftUseSendsAndCopies(GameTestHelper Helper) {
        fresh(Helper);
        ServerLevel World = Helper.getLevel();
        place(Helper, Spot);
        BlockPos At = Helper.absolutePos(Spot);
        Scp902TestPlayer User = player(Helper, 8.5, 6.5, "scp902_traveller");
        Map<EquipmentSlot, ItemStack> Worn = dress(User);
        Vec3 Stood = User.position();
        List<ItemStack> Before = snapshot(User);
        click(User, At, true);
        Scp902State.Trip Each = Scp902State.get(World.getServer()).trip(User.getUUID());
        Helper.assertTrue(Each != null, "Sneak-using the box did not start the trip");
        Helper.assertFalse(Scp902BoxBlock.isOpen(World.getBlockState(At)), "The trip also opened the lid");
        Helper.assertTrue(World.getEntity(Each.Copy) == null, "The stand-in appeared before the player left");
        Helper.assertTrue(World.getEntitiesOfClass(Scp902Copy.class, new AABB(At).inflate(16.0)).isEmpty(), "A stand-in is already standing near the box");
        Helper.assertTrue(same(Before, User), "Starting the trip changed the inventory");
        Helper.startSequence().thenIdle(Scp902Scene.DepartTicks - 2).thenExecute(() -> {
            Helper.assertFalse(Scp902Wastes.inside(User), "The player left before the departure fade ended");
            Helper.assertTrue(World.getEntity(Each.Copy) == null, "The stand-in appeared during the departure fade");
        }).thenIdle(Departed - Scp902Scene.DepartTicks + 2).thenExecute(() -> {
            Helper.assertTrue(Scp902Wastes.inside(User), "The player did not arrive in the wastes");
            Helper.assertTrue(Each.Stage == Scp902State.StageScene && Each.Placed, "The trip did not place the stand-in after arriving");
            Scp902Copy Copy = World.getEntity(Each.Copy) instanceof Scp902Copy Found ? Found : null;
            Helper.assertTrue(Copy != null, "No stand-in appeared after the player arrived");
            Helper.assertTrue(Copy.position().distanceTo(Stood) < 1.0e-6 && Math.abs(Copy.getYRot() - 37.0f) < 1.0e-3 && Math.abs(Copy.getXRot() - 12.0f) < 1.0e-3,
                    "The stand-in is not where the player stood: " + Copy.position() + " vs " + Stood);
            for (Map.Entry<EquipmentSlot, ItemStack> Entry : Worn.entrySet()) {
                ItemStack Shown = Copy.getItemBySlot(Entry.getKey());
                Helper.assertTrue(ItemStack.isSameItemSameComponents(Entry.getValue(), Shown), "The stand-in shows " + Shown + " for " + Entry.getKey().getName()
                        + " instead of " + Entry.getValue());
                Helper.assertTrue(User.getItemBySlot(Entry.getKey()) != Shown, "The stand-in shares the player's own " + Entry.getKey().getName() + " stack");
            }
            Helper.assertTrue(Copy.getItemBySlot(EquipmentSlot.OFFHAND).getCount() == 1, "The stand-in holds a full stack copy");
            Helper.assertTrue(World.getEntitiesOfClass(Scp902Copy.class, new AABB(At).inflate(16.0)).size() == 1, "There is more than one stand-in");
            Helper.assertTrue(same(Before, User), "Arriving in the wastes changed the inventory");
            dismiss(User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp902_landsOnDuckboards")
    public static void landsOnDuckboards(GameTestHelper Helper) {
        fresh(Helper);
        Scp902TestPlayer User = player(Helper, 12.5, 4.5, "scp902_lander");
        Helper.assertTrue(Scp902Trip.begin(User), "The trip did not start");
        Scp902State.Trip Each = Scp902State.get(Helper.getLevel().getServer()).trip(User.getUUID());
        Helper.startSequence().thenIdle(Departed).thenExecute(() -> {
            ServerLevel Wastes = User.serverLevel();
            Helper.assertTrue(Scp902Wastes.inside(Wastes), "The player is not in the wastes");
            BlockPos Feet = BlockPos.containing(User.position());
            Helper.assertTrue(Wastes.isLoaded(Feet) && Wastes.getChunkSource().hasChunk(Feet.getX() >> 4, Feet.getZ() >> 4), "The arrival chunk is not loaded");
            Helper.assertTrue(Scp902State.get(Wastes.getServer()).built(Each.Slot, Scp902Wastes.Signature), "The trench was not built for this slot");
            Helper.assertTrue(Wastes.getBlockState(Feet).is(Scp902Registry.Props.get(Scp902Prop.Duckboard).get()), "The player is not standing on the duckboards: " + Wastes.getBlockState(Feet));
            Helper.assertTrue(Wastes.getBlockState(Feet.above()).isAir() && Wastes.getBlockState(Feet.above(2)).isAir(), "There is no headroom above the arrival");
            Helper.assertTrue(Wastes.noCollision(User), "The player arrived inside a block");
            Helper.assertTrue(Math.abs(User.getY() - Scp902Wastes.ArrivalY) < 0.01, "The player landed at height " + User.getY());
            Helper.assertTrue(Wastes.getBlockState(Scp902Wastes.box(Each.Slot)).is(Scp902Registry.TrenchBox.get()), "The box is not in the trench in front of the player");
            Helper.assertTrue(Math.abs(User.getYRot() - Scp902Wastes.ArrivalYaw) < 0.01, "The player is not facing down the trench");
            dismiss(User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp902_copyUnhurtable")
    public static void copyUnhurtable(GameTestHelper Helper) {
        fresh(Helper);
        ServerLevel World = Helper.getLevel();
        Scp902TestPlayer User = player(Helper, 6.5, 6.5, "scp902_original");
        dress(User);
        Helper.assertTrue(Scp902Trip.begin(User), "The trip did not start");
        Helper.startSequence().thenIdle(Departed).thenExecute(() -> {
            Scp902Copy Copy = Scp902Trip.copyOf(World.getServer(), User.getUUID());
            Helper.assertTrue(Copy != null, "No stand-in");
            Vec3 Was = Copy.position();
            Helper.assertFalse(Copy.hurt(World.damageSources().playerAttack(User), 100.0f), "The stand-in took damage");
            Helper.assertFalse(Copy.hurt(World.damageSources().genericKill(), Float.MAX_VALUE), "The stand-in took killing damage");
            Copy.kill();
            Copy.push(3.0, 1.0, 3.0);
            Copy.knockback(5.0, 1.0, 1.0);
            Helper.assertTrue(Copy.isAlive() && !Copy.isRemoved(), "The stand-in died");
            Helper.assertFalse(Copy.isPickable() || Copy.isPushable() || Copy.isAttackable(), "The stand-in can be targeted or pushed");
            Helper.assertFalse(Copy.canTakeItem(new ItemStack(Items.DIAMOND_HELMET)), "The stand-in accepts equipment");
            Copy.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            Helper.assertTrue(Copy.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "The stand-in's helmet could be taken off");
            Helper.assertTrue(Copy.position().distanceTo(Was) < 1.0e-6, "The stand-in moved");
        }).thenIdle(5).thenExecute(() -> {
            Scp902Copy Copy = Scp902Trip.copyOf(World.getServer(), User.getUUID());
            Helper.assertTrue(Copy != null && Copy.isAlive(), "The stand-in is gone");
            Helper.assertTrue(World.getEntitiesOfClass(ItemEntity.class, Copy.getBoundingBox().inflate(4.0)).isEmpty(), "Items dropped near the stand-in");
            dismiss(User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp902_returnRestores")
    public static void returnRestores(GameTestHelper Helper) {
        fresh(Helper);
        ServerLevel World = Helper.getLevel();
        Scp902TestPlayer User = player(Helper, 5.5, 9.5, "scp902_returner");
        dress(User);
        List<ItemStack> Before = snapshot(User);
        Vec3 Stood = User.position();
        Helper.assertTrue(Scp902Trip.begin(User), "The trip did not start");
        Helper.startSequence().thenIdle(Departed).thenExecute(() -> {
            Scp902Copy Copy = Scp902Trip.copyOf(World.getServer(), User.getUUID());
            Helper.assertTrue(Copy != null, "No stand-in after arriving");
            Helper.assertTrue(Scp902Wastes.inside(User), "The player did not arrive");
            Helper.assertTrue(Scp902Trip.home(User), "There was no trip to end");
            Helper.assertTrue(User.serverLevel() == World && User.position().distanceTo(Stood) < 1.0e-6, "The player was not put back where the stand-in stood: " + User.position());
            Helper.assertTrue(Copy.isRemoved(), "The stand-in is still there");
            Helper.assertTrue(Scp902State.get(World.getServer()).trip(User.getUUID()) == null, "The trip is still open");
            Helper.assertTrue(same(Before, User), "The trip changed the inventory");
            Helper.assertTrue(boxes(User) == 1, "The player has " + boxes(User) + " boxes");
            dismiss(User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp902_oneTimeOnly")
    public static void oneTimeOnly(GameTestHelper Helper) {
        fresh(Helper);
        ServerLevel World = Helper.getLevel();
        place(Helper, Spot);
        BlockPos At = Helper.absolutePos(Spot);
        Scp902TestPlayer User = player(Helper, 8.5, 6.5, "scp902_once");
        Helper.assertTrue(Scp902Trip.begin(User), "The first trip did not start");
        Helper.startSequence().thenIdle(Departed).thenExecute(() -> {
            Scp902Trip.home(User);
            Helper.assertTrue(Scp902State.get(World.getServer()).visited(User.getUUID()), "The trip was not remembered");
            Helper.assertFalse(Scp902Trip.begin(User), "A second trip started");
            click(User, At, true);
            Helper.assertTrue(Scp902State.get(World.getServer()).trip(User.getUUID()) == null, "Sneak-using the box started another trip");
            Helper.assertTrue(Scp902BoxBlock.isOpen(World.getBlockState(At)), "Sneak-using the box afterwards did not just open it");
            User.setItemInHand(InteractionHand.MAIN_HAND, Scp902Box.fresh());
            User.setShiftKeyDown(true);
            User.getMainHandItem().use(World, User, InteractionHand.MAIN_HAND);
            User.setShiftKeyDown(false);
            Helper.assertTrue(Scp902State.get(World.getServer()).trip(User.getUUID()) == null, "Sneak-using the held box started another trip");
            Helper.assertTrue(Scp902BoxItem.latch(User.getMainHandItem()).Open(), "Sneak-using the held box afterwards did not open it");
            dismiss(User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp902_rejoinRecovers")
    public static void rejoinRecovers(GameTestHelper Helper) {
        fresh(Helper);
        ServerLevel World = Helper.getLevel();
        Scp902TestPlayer User = player(Helper, 10.5, 10.5, "scp902_dropped");
        Vec3 Stood = User.position();
        Helper.assertTrue(Scp902Trip.begin(User), "The trip did not start");
        Helper.startSequence().thenIdle(Departed).thenExecute(() -> {
            Scp902Copy Copy = Scp902Trip.copyOf(World.getServer(), User.getUUID());
            Helper.assertTrue(Copy != null, "No stand-in after arriving");
            Scp902State State = Scp902State.get(World.getServer());
            CompoundTag Saved = State.save(new CompoundTag(), World.registryAccess());
            Scp902State Reloaded = Scp902State.load(Saved, World.registryAccess());
            Scp902State.Trip Kept = Reloaded.trip(User.getUUID());
            Helper.assertTrue(Kept != null && Kept.Copy.equals(Copy.getUUID()) && Kept.Spot.distanceTo(Stood) < 1.0e-6 && Kept.Dimension == World.dimension(),
                    "The trip did not survive a save and load");
            Helper.assertTrue(Reloaded.visited(User.getUUID()), "The visit did not survive a save and load");
            Helper.assertTrue(Scp902Wastes.inside(User), "The player is not in the wastes");
            Scp902Trip.loggedIn(User);
            Helper.assertTrue(User.serverLevel() == World && User.position().distanceTo(Stood) < 1.0e-6, "Rejoining did not put the player back");
            Helper.assertTrue(Copy.isRemoved(), "Rejoining left the stand-in behind");
            Helper.assertTrue(State.trip(User.getUUID()) == null, "Rejoining left the trip open");
            dismiss(User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp902_killReturnsHome")
    public static void killReturnsHome(GameTestHelper Helper) {
        fresh(Helper);
        ServerLevel World = Helper.getLevel();
        Scp902TestPlayer User = player(Helper, 3.5, 12.5, "scp902_doomed");
        Vec3 Stood = User.position();
        Helper.assertTrue(Scp902Trip.begin(User), "The trip did not start");
        Helper.startSequence().thenIdle(Departed).thenExecute(() -> {
            Helper.assertTrue(Scp902Wastes.inside(User), "The player did not arrive");
            User.hasChangedDimension();
            Helper.assertFalse(User.hurt(User.damageSources().fall(), 1000.0f), "The player was hurt in the wastes");
            User.kill();
            Helper.assertTrue(User.isAlive() && User.getHealth() > 0.0f, "The player died in the wastes");
        }).thenIdle(3).thenExecute(() -> {
            Helper.assertTrue(User.serverLevel() == World && User.position().distanceTo(Stood) < 1.0e-6, "Killing the player did not send them home");
            Helper.assertTrue(User.isAlive(), "The player is dead at home");
            dismiss(User);
        }).thenSucceed();
    }

    public record Walk(int Rescues, double Lowest, double Progress, double WorstWindow, int LongestStill) {
    }

    public static Walk walk(ServerPlayer User, Scp902State.Trip Each, int Ticks) {
        ServerLevel Wastes = User.serverLevel();
        Vec3 Start = User.position();
        for (int Ahead = -16; Ahead <= Scp902Wastes.WalkEndZ * -1 + 32; Ahead += 16) {
            Wastes.getChunk(BlockPos.containing(Start.x, Start.y, Start.z - Ahead));
        }
        int Rescues = 0;
        double Lowest = Start.y;
        double Fall = 0.0;
        double WorstWindow = Double.MAX_VALUE;
        double WindowStart = Start.z;
        int Still = 0;
        int LongestStill = 0;
        for (int Tick = 0; Tick < Ticks; Tick++) {
            Each.Ticks = Math.min(Scp902Scene.FinishAt, Scp902Scene.ArriveFade + Tick);
            Vec3 Before = User.position();
            Fall = User.onGround() ? -0.0784 : (Fall - 0.08) * 0.98;
            User.move(MoverType.SELF, new Vec3(0.0, Fall, -(PathWalk + PathDrift)));
            if (User.onGround()) {
                Fall = 0.0;
            }
            if (Scp902Trip.guard(User, Each)) {
                Rescues++;
            }
            Vec3 After = User.position();
            Lowest = Math.min(Lowest, After.y);
            Still = After.distanceToSqr(Before) < 1.0e-4 ? Still + 1 : 0;
            LongestStill = Math.max(LongestStill, Still);
            if (Tick % WindowTicks == WindowTicks - 1) {
                WorstWindow = Math.min(WorstWindow, WindowStart - After.z);
                WindowStart = After.z;
            }
        }
        return new Walk(Rescues, Lowest, Start.z - User.getZ(), WorstWindow, LongestStill);
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp902_walkPathClear")
    public static void walkPathClear(GameTestHelper Helper) {
        fresh(Helper);
        Scp902TestPlayer User = player(Helper, 13.5, 13.5, "scp902_walker");
        Helper.assertTrue(Scp902Trip.begin(User), "The trip did not start");
        Scp902State.Trip Each = Scp902State.get(Helper.getLevel().getServer()).trip(User.getUUID());
        Helper.startSequence().thenIdle(Departed).thenExecute(() -> {
            User.hasChangedDimension();
            Helper.assertTrue(Scp902Wastes.inside(User), "The player did not arrive");
            Walk Done = walk(User, Each, Scp902Scene.TextAt - Scp902Scene.ArriveFade);
            double Floor = Scp902Wastes.ArrivalY - 0.0125;
            Helper.assertTrue(Done.Rescues() == 0, "The walk needed " + Done.Rescues() + " rescues, so the path is broken");
            Helper.assertTrue(Done.Lowest() >= Floor - 0.6, "The player dropped to " + Done.Lowest() + " on the path");
            Helper.assertTrue(Done.LongestStill() <= Scp902Trip.StuckTicks, "The player stood stuck for " + Done.LongestStill() + " ticks");
            Helper.assertTrue(Done.WorstWindow() >= MinimumWindow, "A " + WindowTicks + "-tick stretch only gained " + Done.WorstWindow() + " blocks toward the sun");
            Helper.assertTrue(Done.Progress() >= MinimumProgress, "The walk only reached " + Done.Progress() + " blocks");
            dismiss(User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp902_unstickRescues")
    public static void unstickRescues(GameTestHelper Helper) {
        fresh(Helper);
        Scp902TestPlayer User = player(Helper, 13.5, 9.5, "scp902_blocked");
        Helper.assertTrue(Scp902Trip.begin(User), "The trip did not start");
        Scp902State.Trip Each = Scp902State.get(Helper.getLevel().getServer()).trip(User.getUUID());
        Helper.startSequence().thenIdle(Departed).thenExecute(() -> {
            User.hasChangedDimension();
            ServerLevel Wastes = User.serverLevel();
            BlockPos Centre = Scp902Wastes.centre(Each.Slot);
            List<BlockPos> Changed = new ArrayList<>();
            List<BlockState> Saved = new ArrayList<>();
            for (int X = -1; X <= 1; X++) {
                for (int Y = 61; Y <= 62; Y++) {
                    Changed.add(Centre.offset(X, Y, -4));
                }
            }
            Changed.add(Centre.offset(0, 61, -9));
            Changed.add(Centre.offset(0, 60, -9));
            for (BlockPos Spot0 : Changed) {
                Saved.add(Wastes.getBlockState(Spot0));
            }
            try {
                for (int Index = 0; Index < 6; Index++) {
                    Wastes.setBlock(Changed.get(Index), Scp902Registry.TrenchMud.get().defaultBlockState(), 3);
                }
                Wastes.setBlock(Changed.get(6), Blocks.AIR.defaultBlockState(), 3);
                Wastes.setBlock(Changed.get(7), Blocks.AIR.defaultBlockState(), 3);
                Walk Done = walk(User, Each, 200);
                Helper.assertTrue(Done.Rescues() >= 2, "The guard rescued the player " + Done.Rescues() + " times from a wall and a hole");
                Helper.assertTrue(User.getZ() < Centre.getZ() - 10.0, "The player never got past the wall and the hole: z " + User.getZ());
                Helper.assertTrue(User.getY() >= Scp902Wastes.ArrivalY - 0.65, "The player ended in the hole at " + User.getY());
                Helper.assertTrue(Done.LongestStill() <= Scp902Trip.StuckTicks + 1, "The player stood stuck for " + Done.LongestStill() + " ticks");
            } finally {
                for (int Index = 0; Index < Changed.size(); Index++) {
                    Wastes.setBlock(Changed.get(Index), Saved.get(Index), 3);
                }
                dismiss(User);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp902_twoTravellersPrivate")
    public static void twoTravellersPrivate(GameTestHelper Helper) {
        fresh(Helper);
        ServerLevel World = Helper.getLevel();
        Scp902TestPlayer First = player(Helper, 3.5, 3.5, "scp902_first");
        Scp902TestPlayer Second = player(Helper, 12.5, 12.5, "scp902_second");
        Scp902TestPlayer Watcher = player(Helper, 3.5, 12.5, "scp902_watcher");
        Helper.assertTrue(Scp902Trip.begin(First) && Scp902Trip.begin(Second), "The two trips did not start");
        Scp902State State = Scp902State.get(World.getServer());
        Scp902State.Trip One = State.trip(First.getUUID());
        Scp902State.Trip Two = State.trip(Second.getUUID());
        Helper.assertTrue(One.Slot != Two.Slot, "Both travellers were given slot " + One.Slot);
        Helper.startSequence().thenIdle(Departed).thenExecute(() -> {
            Helper.assertTrue(Scp902Wastes.inside(First) && Scp902Wastes.inside(Second), "The travellers did not arrive");
            Helper.assertTrue(Math.abs(First.getX() - Second.getX()) >= Scp902Wastes.SlotSpacing - 1.0, "The travellers are only " + Math.abs(First.getX() - Second.getX()) + " blocks apart");
            Helper.assertFalse(First.broadcastToPlayer(Second), "The second traveller would be sent the first");
            Helper.assertFalse(Second.broadcastToPlayer(First), "The first traveller would be sent the second");
            ServerLevel Wastes = First.serverLevel();
            ArmorStand Marker = new ArmorStand(Wastes, First.getX() + 1.0, First.getY(), First.getZ() - 2.0);
            Wastes.addFreshEntity(Marker);
            Helper.assertTrue(Marker.broadcastToPlayer(First), "The first traveller cannot see an entity in their own scene");
            Helper.assertFalse(Marker.broadcastToPlayer(Second), "The second traveller would be sent an entity from the first scene");
            Watcher.setGameMode(GameType.SPECTATOR);
            Watcher.teleportTo(Wastes, First.getX() + 2.0, First.getY() + 3.0, First.getZ(), 0.0f, 0.0f);
            Helper.assertFalse(Scp902Privacy.hidden(First, Watcher) || Scp902Privacy.hidden(Marker, Watcher), "A spectating admin is shut out of the scene");
            Helper.assertTrue(First.broadcastToPlayer(Watcher), "A spectating admin cannot see the traveller");
            Marker.discard();
            dismiss(First);
            dismiss(Second);
            dismiss(Watcher);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp902_slotClearedAfterReturn")
    public static void slotClearedAfterReturn(GameTestHelper Helper) {
        fresh(Helper);
        ServerLevel World = Helper.getLevel();
        Scp902TestPlayer User = player(Helper, 4.5, 8.5, "scp902_tidy");
        Scp902State State = Scp902State.get(World.getServer());
        int[] Held = new int[1];
        List<BlockPos> Probes = new ArrayList<>();
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(State.dirtySlots().isEmpty(), "Earlier slots are still being cleared")).thenExecute(() -> {
            Helper.assertTrue(Scp902Trip.begin(User), "The trip did not start");
            Held[0] = State.trip(User.getUUID()).Slot;
        }).thenIdle(Departed).thenExecute(() -> {
            int Slot = Held[0];
            ServerLevel Wastes = User.serverLevel();
            Helper.assertTrue(Wastes.getBlockState(Scp902Wastes.box(Slot)).is(Scp902Registry.TrenchBox.get()), "The trench was not built");
            Wastes.addFreshEntity(new ArmorStand(Wastes, User.getX(), User.getY(), User.getZ() - 3.0));
            Helper.assertTrue(Scp902Trip.home(User), "There was no trip to end");
            Helper.assertTrue(State.dirty(Slot), "Ending the trip did not queue the slot for clearing");
            BlockPos Centre = Scp902Wastes.centre(Slot);
            Probes.add(Scp902Wastes.box(Slot));
            Probes.add(Centre.offset(0, 61, 0));
            Probes.add(Centre.offset(0, 61, -40));
            Probes.add(Centre.offset(2, 63, -10));
            Probes.add(Centre.offset(3, 64, -20));
            Probes.add(Centre.offset(-1, 60, -30));
        }).thenWaitUntil(() -> Helper.assertFalse(State.dirty(Held[0]), "The slot is still being cleared")).thenExecute(() -> {
            int Slot = Held[0];
            ServerLevel Wastes = Scp902Wastes.level(World.getServer());
            for (BlockPos Probe : Probes) {
                Helper.assertTrue(Wastes.getBlockState(Probe) == Scp902Wastes.flat(Probe.getY()), "The slot still has " + Wastes.getBlockState(Probe) + " at " + Probe);
            }
            Helper.assertTrue(Wastes.getBlockEntity(Scp902Wastes.box(Slot)) == null, "The box block entity was left behind");
            Helper.assertTrue(Wastes.getEntitiesOfClass(Entity.class, Scp902Cleanup.area(Slot), Found -> !(Found instanceof Player)).isEmpty(), "Entities were left in the slot");
            Helper.assertFalse(State.built(Slot, Scp902Wastes.Signature), "The slot is still marked as built");
            Helper.assertTrue(State.freeSlot() <= Slot, "The cleared slot was not released");
            dismiss(User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp902_slotReusedCleanly")
    public static void slotReusedCleanly(GameTestHelper Helper) {
        fresh(Helper);
        ServerLevel World = Helper.getLevel();
        Scp902TestPlayer First = player(Helper, 4.5, 4.5, "scp902_earlier");
        Scp902TestPlayer Later = player(Helper, 11.5, 11.5, "scp902_later");
        Scp902State State = Scp902State.get(World.getServer());
        int[] Slot = new int[1];
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(State.dirtySlots().isEmpty(), "Earlier slots are still being cleared")).thenExecute(() -> {
            Helper.assertTrue(Scp902Trip.begin(First), "The first trip did not start");
            Slot[0] = State.trip(First.getUUID()).Slot;
        }).thenIdle(Departed).thenExecute(() -> {
            ServerLevel Wastes = First.serverLevel();
            Wastes.setBlock(Scp902Wastes.centre(Slot[0]).offset(0, 61, -6), Blocks.AIR.defaultBlockState(), 3);
            Scp902Trip.home(First);
        }).thenWaitUntil(() -> Helper.assertFalse(State.dirty(Slot[0]), "The slot is still being cleared")).thenExecute(() -> {
            Helper.assertTrue(Scp902Trip.begin(Later), "The second trip did not start");
            Helper.assertTrue(State.trip(Later.getUUID()).Slot == Slot[0], "The second trip did not reuse the cleared slot");
        }).thenIdle(Departed).thenExecute(() -> {
            ServerLevel Wastes = Later.serverLevel();
            Helper.assertTrue(Scp902Wastes.inside(Later), "The second traveller did not arrive");
            Helper.assertTrue(Wastes.getBlockState(Scp902Wastes.box(Slot[0])).is(Scp902Registry.TrenchBox.get()), "The reused slot has no box");
            Helper.assertTrue(Wastes.getBlockState(Scp902Wastes.centre(Slot[0]).offset(0, 61, -6)).is(Scp902Registry.Props.get(Scp902Prop.Duckboard).get()),
                    "The reused slot kept the earlier damage instead of being built fresh");
            Helper.assertTrue(Wastes.noCollision(Later), "The second traveller arrived inside a block");
            dismiss(First);
            dismiss(Later);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp902_sceneBuiltOnce")
    public static void sceneBuiltOnce(GameTestHelper Helper) {
        fresh(Helper);
        wastes(Helper);
        ServerLevel Wastes = Scp902Wastes.level(Helper.getLevel().getServer());
        Helper.assertTrue(Wastes != null, "The wastes dimension is missing");
        int Slot = 61;
        Scp902State State = Scp902State.get(Helper.getLevel().getServer());
        boolean Already = State.built(Slot, Scp902Wastes.Signature);
        int Builds = State.builds();
        boolean First = Scp902Wastes.ensureBuilt(Wastes, Slot);
        boolean Second = Scp902Wastes.ensureBuilt(Wastes, Slot);
        Helper.assertTrue(Already || First, "The scene was not built");
        Helper.assertFalse(Second, "The scene was built twice");
        Helper.assertTrue(State.builds() == Builds + (Already ? 0 : 1), "The build count moved by " + (State.builds() - Builds));
        Helper.assertTrue(Wastes.getBlockState(Scp902Wastes.box(Slot)).is(Scp902Registry.TrenchBox.get()), "There is no box in the trench: " + Wastes.getBlockState(Scp902Wastes.box(Slot)));
        Vec3 Arrival = Scp902Wastes.arrival(Slot);
        Helper.assertTrue(Wastes.noCollision(EntityType.PLAYER.getDimensions().makeBoundingBox(Arrival)), "The arrival spot is blocked: " + Wastes.getBlockState(BlockPos.containing(Arrival)));
        Helper.assertFalse(Wastes.noCollision(EntityType.PLAYER.getDimensions().makeBoundingBox(Arrival.subtract(0.0, 0.25, 0.0))), "The player would fall at the arrival spot");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp902_strangerDelivers")
    public static void strangerDelivers(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp902Registry.DocumentId);
        Helper.assertTrue(Document != null, "The SCP-902 dossier did not load");
        Helper.assertTrue(Document.Classification() == DocumentClass.Euclid, "SCP-902 is not filed as Euclid");
        Helper.assertTrue(Document.Condition() instanceof StrangerEncounterCondition, "SCP-902 does not arrive with the stranger");
        StrangerPlan Plan = ((StrangerEncounterCondition) Document.Condition()).plan();
        Helper.assertTrue(Plan.Gifts().size() == 1, "The stranger brings " + Plan.Gifts().size() + " gifts");
        StrangerGift Gift = Plan.Gifts().get(0);
        ItemStack Stack = Gift.toStack();
        Helper.assertTrue(Scp902BoxItem.is(Stack) && Stack.getCount() == 1, "The stranger's gift is not the box: " + Stack);
        Helper.assertFalse(Scp902BoxItem.latch(Stack).Open(), "The gifted box would arrive open");
        Helper.succeed();
    }
}
