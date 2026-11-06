package net.rai.scp_unleashed.z0003.gametest;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.mtf.MtfMissions;
import net.rai.scp_unleashed.mtf.MtfOperative;
import net.rai.scp_unleashed.mtf.MtfRegistry;
import net.rai.scp_unleashed.mtf.MtfTier;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageRegistry;
import net.rai.scp_unleashed.z0003.Z0003Arming;
import net.rai.scp_unleashed.z0003.Z0003Device;
import net.rai.scp_unleashed.z0003.Z0003DeviceItem;
import net.rai.scp_unleashed.z0003.Z0003Frozen;
import net.rai.scp_unleashed.z0003.Z0003Petrify;
import net.rai.scp_unleashed.z0003.Z0003Phrase;
import net.rai.scp_unleashed.z0003.Z0003Registry;
import net.rai.scp_unleashed.z0003.Z0003Statue;
import net.rai.scp_unleashed.z0003.Z0003VialItem;
import net.rai.scp_unleashed.z0003.Z0003Voice;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Z0003GameTests {
    public static final String Arena = "z0003_arena";
    public static final double Search = 40.0;
    public static final double Still = 1.0E-3;

    private static final Logger Log = LogUtils.getLogger();

    private Z0003GameTests() {
    }

    private static int stand(GameTestHelper Helper) {
        for (int Y = 0; Y < 4; Y++) {
            if (Helper.getBlockState(new BlockPos(1, Y, 1)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(1, Y + 1, 1)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static Vec3 at(GameTestHelper Helper, double X, double Z) {
        return Helper.absoluteVec(new Vec3(X, stand(Helper), Z));
    }

    private static Z0003TestPlayer player(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer User = new Z0003TestPlayer(World, Name);
        Vec3 Spot = at(Helper, X, Z);
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, Entity Subject) {
        if (Subject.isRemoved()) {
            return;
        }
        if (Subject instanceof Z0003TestPlayer User) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        } else {
            Subject.discard();
        }
    }

    private static Villager villager(GameTestHelper Helper, double X, double Z) {
        return Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(X, stand(Helper), Z));
    }

    private static DClass dclass(GameTestHelper Helper, double X, double Z) {
        return Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(X, stand(Helper), Z));
    }

    private static MtfOperative operative(GameTestHelper Helper, double X, double Z) {
        return Helper.spawnWithNoFreeWill(MtfRegistry.OperativeType.get(), new Vec3(X, stand(Helper), Z));
    }

    private static ItemStack armed(ServerLevel World, float Radius, int Ticks) {
        ItemStack Stack = Z0003DeviceItem.create();
        Z0003DeviceItem.arm(Stack, new Z0003Arming(Radius, World.getGameTime() + Ticks));
        return Stack;
    }

    private static Z0003Device placed(GameTestHelper Helper, double X, double Lift, double Z, ItemStack Stack) {
        Vec3 Spot = at(Helper, X, Z).add(0.0, Lift, 0.0);
        return Z0003Device.released(Helper.getLevel(), Spot, Vec3.ZERO, null, Stack);
    }

    private static <T extends Entity> List<T> around(GameTestHelper Helper, Class<T> Kind) {
        Vec3 Centre = at(Helper, 16.0, 16.0);
        return Helper.getLevel().getEntitiesOfClass(Kind, new AABB(Centre, Centre).inflate(Search), Entity::isAlive);
    }

    private static void clean(GameTestHelper Helper) {
        for (Z0003Device Found : around(Helper, Z0003Device.class)) {
            Found.discard();
        }
        for (ItemEntity Found : around(Helper, ItemEntity.class)) {
            Found.discard();
        }
    }

    private static boolean statue(Entity Subject) {
        return Z0003Petrify.isStatue(Subject);
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "z0003_phrases")
    public static void phrasesParse(GameTestHelper Helper) {
        expect(Helper, "Effect 2 meters in 1 second", 2.0f, 20);
        expect(Helper, "in five seconds, effect ten meters", 10.0f, 100);
        expect(Helper, "Twenty-five metres in 3s", 25.0f, 60);
        expect(Helper, "EFFECT 2.5 M IN 0.5 S", 2.5f, 10);
        expect(Helper, "a meter in half a second", 1.0f, 10);
        expect(Helper, "two and a half meters in a second", 2.5f, 20);
        expect(Helper, "one hundred and twenty meters in ninety seconds", 64.0f, 1800);
        expect(Helper, "effect 30 meters in 2 minutes", 30.0f, 2400);
        expect(Helper, "effect zero meters in zero seconds", 1.0f, 0);
        expect(Helper, "2m 1s", 2.0f, 20);
        expect(Helper, "effect 9000 meters in 9000 seconds", 64.0f, 12000);
        Z0003Phrase.Setting Huge = Z0003Phrase.parse("effect 500 meters in 1 second").orElseThrow();
        Helper.assertTrue(Huge.RadiusClamped() && !Huge.DelayClamped() && Huge.Radius() == Z0003Phrase.MaxRadius, "500 m was not flagged as clamped: " + Huge);
        Helper.assertTrue(clampKey(Huge).equals(Z0003Voice.RadiusClampedKey), "the clamped radius gave no clear message: " + clampKey(Huge));
        Z0003Phrase.Setting Plain = Z0003Phrase.parse("effect 5 meters in 2 seconds").orElseThrow();
        Helper.assertTrue(!Plain.RadiusClamped() && !Plain.DelayClamped() && clampKey(Plain).equals(Z0003Voice.ArmedKey), "an in-range setting was flagged");
        Z0003Phrase.Setting Both = Z0003Phrase.parse("effect 500 meters in 20 minutes").orElseThrow();
        Helper.assertTrue(clampKey(Both).equals(Z0003Voice.BothClampedKey), "clamping both values gave the wrong message: " + clampKey(Both));
        Helper.assertTrue(Z0003Phrase.parse("hello there").isEmpty(), "plain chat armed the device");
        Helper.assertTrue(Z0003Phrase.parse("go 5 meters north").isEmpty(), "a distance alone armed the device");
        Helper.assertTrue(Z0003Phrase.parse("back in 3 seconds").isEmpty(), "a time alone armed the device");
        Helper.assertTrue(Z0003Phrase.parse("").isEmpty(), "an empty line armed the device");
        Helper.succeed();
    }

    private static String clampKey(Z0003Phrase.Setting Setting) {
        return Z0003Voice.armedMessage(Setting).getContents() instanceof TranslatableContents Text ? Text.getKey() : "";
    }

    private static void expect(GameTestHelper Helper, String Line, float Radius, int Ticks) {
        Optional<Z0003Phrase.Setting> Parsed = Z0003Phrase.parse(Line);
        Helper.assertTrue(Parsed.isPresent(), "could not read \"" + Line + "\"");
        Helper.assertTrue(Math.abs(Parsed.get().Radius() - Radius) < 1.0E-4f, "\"" + Line + "\" gave radius " + Parsed.get().Radius());
        Helper.assertTrue(Parsed.get().DelayTicks() == Ticks, "\"" + Line + "\" gave " + Parsed.get().DelayTicks() + " ticks");
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "z0003_chatArms")
    public static void chatArmsHeldDevice(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer Holder = player(Helper, 6.5, 6.5, "z0003_holder");
        Z0003TestPlayer Idle = player(Helper, 26.5, 26.5, "z0003_idle");
        Holder.setItemInHand(InteractionHand.MAIN_HAND, Z0003DeviceItem.create());
        Holder.getInventory().setItem(8, Z0003DeviceItem.create());
        Idle.setItemInHand(InteractionHand.MAIN_HAND, Z0003DeviceItem.create());
        String Line = "Effect 2 meters in 1 second";
        Component Shown = CommonHooks.onServerChatSubmittedEvent(Holder, Line, Component.literal(Line));
        Helper.assertTrue(Shown != null, "the spoken setting was hidden from chat");
        Z0003Arming Setting = Z0003DeviceItem.armingOf(Holder.getMainHandItem());
        Helper.assertTrue(Setting != null, "the held device did not take the setting");
        Helper.assertTrue(Math.abs(Setting.Radius() - 2.0f) < 1.0E-4f && Setting.Due() - World.getGameTime() == 20, "the setting was read wrong: " + Setting);
        Helper.assertFalse(Z0003DeviceItem.isArmed(Holder.getInventory().getItem(8)), "a device in the backpack heard the speaker");
        Helper.assertFalse(Z0003DeviceItem.isArmed(Idle.getMainHandItem()), "another player's device heard the speaker");
        CommonHooks.onServerChatSubmittedEvent(Idle, "nice weather today", Component.literal("nice weather today"));
        Helper.assertFalse(Z0003DeviceItem.isArmed(Idle.getMainHandItem()), "plain chat armed a device");
        dismiss(Helper, Holder);
        dismiss(Helper, Idle);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "z0003_thrownTriggers")
    public static void thrownDeviceTriggersAfterDelay(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer Thrower = player(Helper, 4.5, 16.5, "z0003_thrower");
        Thrower.setYRot(-90.0f);
        Thrower.setXRot(-10.0f);
        long Due = World.getGameTime() + 20;
        Z0003Device Thrown = Z0003Device.thrown(World, Thrower, armed(World, 1.0f, 20), 0.6f);
        Helper.startSequence().thenExecuteAfter(10, () -> {
            Helper.assertTrue(Thrown.isArmed() && !Thrown.isActive(), "the thrown device went off early");
        }).thenWaitUntil(() -> Helper.assertTrue(Thrown.isActive(), "the thrown device never went off")).thenExecute(() -> {
            Helper.assertTrue(World.getGameTime() >= Due, "the field started before its delay");
            Helper.assertTrue(Thrown.distanceTo(Thrower) > 2.0, "the device did not travel");
            clean(Helper);
            dismiss(Helper, Thrower);
        }).thenSucceed();
    }

    private static boolean resting(Z0003Device Device) {
        return Device.onGround() && Device.getDeltaMovement().lengthSqr() < 1.0E-6;
    }

    private static double flatDistance(Entity From, Entity To) {
        double Dx = To.getX() - From.getX();
        double Dz = To.getZ() - From.getZ();
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "z0003_range", skyAccess = true)
    public static void fullChargeRange(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer Straight = player(Helper, 2.5, 8.5, "z0003_level");
        Z0003TestPlayer Lofted = player(Helper, 1.5, 1.5, "z0003_lofted");
        Z0003TestPlayer Soft = player(Helper, 2.5, 16.5, "z0003_soft");
        Straight.setYRot(-90.0f);
        Lofted.setYRot(-45.0f);
        Lofted.setXRot(-25.0f);
        Soft.setYRot(-90.0f);
        Z0003Device Flat = Z0003Device.thrown(World, Straight, Z0003DeviceItem.create(), 1.0f);
        Z0003Device Arc = Z0003Device.thrown(World, Lofted, Z0003DeviceItem.create(), 1.0f);
        Z0003Device Short = Z0003Device.thrown(World, Soft, Z0003DeviceItem.create(), Z0003DeviceItem.charge(Z0003DeviceItem.MinChargeTicks));
        long Thrown = World.getGameTime();
        Helper.startSequence().thenWaitUntil(() -> {
            Helper.assertTrue(resting(Flat) && resting(Arc) && resting(Short), "the devices are still moving");
        }).thenExecute(() -> {
            long Settled = World.getGameTime() - Thrown;
            double FlatRange = flatDistance(Straight, Flat);
            double ArcRange = flatDistance(Lofted, Arc);
            double ShortRange = flatDistance(Soft, Short);
            Helper.assertTrue(FlatRange >= 20.0 && FlatRange <= 23.5, "a level full throw went " + FlatRange + " blocks");
            Helper.assertTrue(ArcRange >= 26.5 && ArcRange <= 30.5, "a full throw aimed 25 degrees up went " + ArcRange + " blocks");
            Helper.assertTrue(ShortRange >= 8.5 && ShortRange <= 11.0, "the shortest throw went " + ShortRange + " blocks");
            Log.info("Z-0003 throw ranges: level {} blocks, 25 degrees up {} blocks, shortest {} blocks, settled after {} ticks",
                    String.format("%.2f", FlatRange), String.format("%.2f", ArcRange), String.format("%.2f", ShortRange), Settled);
            Helper.assertTrue(Settled <= 45, "the devices took " + Settled + " ticks to settle");
            for (Z0003Device Each : List.of(Flat, Arc, Short)) {
                Helper.assertTrue(Each.bounces() <= Z0003Device.MaxBounces, "a device bounced " + Each.bounces() + " times");
            }
            clean(Helper);
            dismiss(Helper, Straight);
            dismiss(Helper, Lofted);
            dismiss(Helper, Soft);
        }).thenSucceed();
    }

    private static List<Z0003Device> thrownBy(GameTestHelper Helper, Entity Thrower) {
        return around(Helper, Z0003Device.class).stream().filter(Found -> Found.getOwner() == Thrower).toList();
    }

    private static boolean sent(Z0003TestPlayer Receiver, int Index, boolean Empty) {
        return Receiver.SlotUpdates.stream().anyMatch(Update -> Update.getContainerId() == InventoryMenu.CONTAINER_ID && Update.getSlot() == Index
                && (Empty ? Update.getItem().isEmpty() : Z0003DeviceItem.is(Update.getItem())));
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "z0003_throwHand")
    public static void throwEmptiesHandAtOnce(GameTestHelper Helper) {
        Z0003TestPlayer Thrower = player(Helper, 4.5, 4.5, "z0003_tosser");
        Z0003TestPlayer Lefty = player(Helper, 4.5, 12.5, "z0003_lefty");
        Z0003TestPlayer Maker = player(Helper, 4.5, 20.5, "z0003_maker");
        Z0003TestPlayer Hasty = player(Helper, 4.5, 28.5, "z0003_hasty");
        Maker.setGameMode(GameType.CREATIVE);
        for (Z0003TestPlayer Each : List.of(Thrower, Lefty, Maker, Hasty)) {
            Each.setYRot(-90.0f);
            Each.getInventory().selected = 2;
            Each.setItemInHand(Each == Lefty ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, Z0003DeviceItem.create());
            Each.initInventoryMenu();
            Each.SlotUpdates.clear();
        }
        int MainIndex = Z0003DeviceItem.handSlot(Thrower, InteractionHand.MAIN_HAND);
        int OffIndex = Z0003DeviceItem.handSlot(Lefty, InteractionHand.OFF_HAND);
        Thrower.release(InteractionHand.MAIN_HAND, Z0003DeviceItem.FullChargeTicks);
        Helper.assertTrue(Thrower.getMainHandItem().isEmpty(), "the thrown device stayed in the hand");
        Helper.assertTrue(Thrower.getInventory().getItem(2).isEmpty(), "the thrown device stayed in its hotbar slot");
        Helper.assertTrue(Thrower.getInventory().countItem(Z0003Registry.Device.get()) == 0, "the thrown device stayed in the inventory");
        Helper.assertFalse(Thrower.isUsingItem(), "the thrower kept winding up after the throw");
        Helper.assertTrue(sent(Thrower, MainIndex, true), "the empty hand was not sent to the thrower right away");
        Helper.assertTrue(thrownBy(Helper, Thrower).size() == 1, "the throw did not make exactly one device");
        Lefty.release(InteractionHand.OFF_HAND, Z0003DeviceItem.FullChargeTicks);
        Helper.assertTrue(Lefty.getOffhandItem().isEmpty(), "the device thrown from the off hand stayed there");
        Helper.assertTrue(sent(Lefty, OffIndex, true), "the empty off hand was not sent right away");
        Helper.assertTrue(thrownBy(Helper, Lefty).size() == 1, "the off hand throw did not make exactly one device");
        Maker.release(InteractionHand.MAIN_HAND, Z0003DeviceItem.FullChargeTicks);
        Helper.assertTrue(Z0003DeviceItem.is(Maker.getMainHandItem()), "a creative throw used up the device");
        List<Z0003Device> Copies = thrownBy(Helper, Maker);
        Helper.assertTrue(Copies.size() == 1 && Copies.get(0).isCreativeCopy(), "the creative throw did not make one marked device");
        Hasty.release(InteractionHand.MAIN_HAND, Z0003DeviceItem.MinChargeTicks - 1);
        Helper.assertTrue(Z0003DeviceItem.is(Hasty.getMainHandItem()), "a short tap took the device from the hand");
        Helper.assertTrue(thrownBy(Helper, Hasty).isEmpty(), "a short tap threw the device");
        Helper.assertTrue(sent(Hasty, MainIndex, false), "the kept device was not sent back after a short tap");
        Helper.assertTrue(Copies.get(0).interact(Maker, InteractionHand.MAIN_HAND).consumesAction(), "the creative copy could not be picked up");
        Helper.assertTrue(Maker.getInventory().countItem(Z0003Registry.Device.get()) == 1, "picking up the creative copy gave a second device");
        Z0003Device Own = thrownBy(Helper, Thrower).get(0);
        Helper.assertTrue(Own.interact(Thrower, InteractionHand.MAIN_HAND).consumesAction(), "the thrown device could not be picked up");
        Helper.assertTrue(Thrower.getInventory().countItem(Z0003Registry.Device.get()) == 1, "the thrown device did not come back on pickup");
        clean(Helper);
        for (Z0003TestPlayer Each : List.of(Thrower, Lefty, Maker, Hasty)) {
            dismiss(Helper, Each);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "z0003_wallStop")
    public static void wallStopsWithoutRebound(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        int Floor = stand(Helper);
        int Wall = 8;
        for (int Y = Floor; Y < Floor + 4; Y++) {
            for (int Z = 13; Z <= 19; Z++) {
                Helper.setBlock(new BlockPos(Wall, Y, Z), Blocks.STONE);
            }
        }
        Z0003TestPlayer Thrower = player(Helper, 2.5, 16.5, "z0003_pitcher");
        Thrower.setYRot(-90.0f);
        Thrower.setXRot(-5.0f);
        Z0003Device Thrown = Z0003Device.thrown(World, Thrower, Z0003DeviceItem.create(), 1.0f);
        double[] Furthest = {Double.NEGATIVE_INFINITY};
        Helper.onEachTick(() -> Furthest[0] = Math.max(Furthest[0], Helper.relativeVec(Thrown.position()).x));
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(resting(Thrown), "the device is still moving")).thenExecute(() -> {
            double Final = Helper.relativeVec(Thrown.position()).x;
            Helper.assertTrue(Furthest[0] > Wall - 1.0, "the device never reached the wall: " + Furthest[0]);
            Helper.assertTrue(Final >= Furthest[0] - 0.75, "the device bounced back from the wall, from " + Furthest[0] + " to " + Final);
            Helper.assertTrue(Thrown.bounces() <= Z0003Device.MaxBounces, "the device bounced " + Thrown.bounces() + " times");
            clean(Helper);
            dismiss(Helper, Thrower);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "z0003_tetherReach")
    public static void tetherHoldsAtFullReach(GameTestHelper Helper) {
        Z0003TestPlayer Holder = player(Helper, 16.5, 16.5, "z0003_reacher");
        Holder.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.LEAD, 1));
        Z0003Device Flying = placed(Helper, 16.5, 10.0, 16.5, Z0003DeviceItem.create());
        Flying.tether(Holder, true);
        Holder.getOffhandItem().shrink(1);
        Flying.setNoGravity(true);
        Flying.setDeltaMovement(0.0, 4.0, 0.0);
        double[] Longest = {0.0};
        Helper.onEachTick(() -> {
            if (!Flying.isRemoved()) {
                Longest[0] = Math.max(Longest[0], Flying.fieldCentre().distanceTo(Z0003Device.anchorOf(Holder)));
            }
        });
        Helper.startSequence().thenExecuteAfter(40, () -> {
            double Reach = Flying.fieldCentre().distanceTo(Z0003Device.anchorOf(Holder));
            Helper.assertTrue(Flying.isTetheredTo(Holder), "the line broke on its own at " + Reach + " blocks");
            Helper.assertTrue(Reach >= Z0003Device.TetherLength - 2.0, "the line never ran out: " + Reach);
            Helper.assertTrue(Longest[0] <= Z0003Device.TetherLength + 4.5, "the device flew past the end of the line: " + Longest[0]);
            Helper.assertTrue(Reach <= Z0003Device.TetherLength + 0.75, "the taut line did not hold the device: " + Reach);
            Helper.assertFalse(Flying.hurt(Holder.damageSources().playerAttack(Holder), 6.0f), "the device took a hit");
            Helper.assertTrue(Flying.isTetheredTo(Holder), "a hit cut the line");
            Helper.assertTrue(Flying.yank(Holder), "the yank did nothing at full reach");
        }).thenWaitUntil(() -> Helper.assertTrue(Flying.isRemoved(), "the device never came back")).thenExecute(() -> {
            Helper.assertTrue(Holder.getInventory().contains(Z0003DeviceItem.create()), "the device did not return to the holder");
            Helper.assertTrue(Holder.getInventory().countItem(Items.LEAD) == 1, "the lead was not returned");
            clean(Helper);
            dismiss(Helper, Holder);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "z0003_tetherCut")
    public static void shearsCutTheLine(GameTestHelper Helper) {
        Z0003TestPlayer Holder = player(Helper, 10.5, 16.5, "z0003_cutter");
        Holder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        Z0003Device Cut = placed(Helper, 14.5, 0.0, 16.5, Z0003DeviceItem.create());
        Z0003Device Left = placed(Helper, 20.5, 0.0, 16.5, Z0003DeviceItem.create());
        Cut.tether(Holder, true);
        Left.tether(Holder, true);
        Helper.startSequence().thenExecuteAfter(3, () -> {
            Helper.assertTrue(Cut.isTetheredTo(Holder) && Left.isTetheredTo(Holder), "the lines dropped before anything happened");
            Helper.assertTrue(Cut.interact(Holder, InteractionHand.MAIN_HAND).consumesAction(), "the shears did nothing");
            Helper.assertFalse(Cut.isTethered(), "the shears did not cut the line");
            Helper.assertTrue(Holder.getMainHandItem().getDamageValue() == 1, "the shears were not worn");
            Helper.assertTrue(Holder.getInventory().countItem(Items.LEAD) == 1, "the cut lead did not come back");
            Helper.assertTrue(Left.isTetheredTo(Holder), "cutting one line cut both");
            Helper.assertTrue(Z0003Device.releaseAll(Holder) == 1, "the leaving holder did not let go");
            Helper.assertFalse(Left.isTethered(), "the line stayed on after the holder left");
            Helper.assertTrue(Holder.getInventory().countItem(Items.LEAD) == 2, "the second lead did not come back");
            clean(Helper);
            dismiss(Helper, Holder);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "z0003_droppedTriggers")
    public static void droppedItemTriggers(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Vec3 Spot = at(Helper, 16.5, 16.5);
        long Due = World.getGameTime() + 15;
        ItemEntity Dropped = new ItemEntity(World, Spot.x, Spot.y + 0.2, Spot.z, armed(World, 1.0f, 15));
        World.addFreshEntity(Dropped);
        Helper.startSequence().thenExecuteAfter(8, () -> {
            Helper.assertTrue(Dropped.isAlive(), "the dropped device went off early");
        }).thenWaitUntil(() -> {
            Helper.assertTrue(Dropped.isRemoved(), "the dropped item is still lying there");
            Helper.assertTrue(around(Helper, Z0003Device.class).stream().anyMatch(Z0003Device::isActive), "no active device replaced the dropped item");
        }).thenExecute(() -> {
            Helper.assertTrue(World.getGameTime() >= Due, "the dropped device went off before its delay");
            clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "z0003_hovers")
    public static void hoversWhenTriggeredMidAir(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0003Device Falling = placed(Helper, 16.5, 4.0, 16.5, armed(World, 1.0f, 3));
        double[] Height = new double[1];
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Falling.isActive(), "the device never went off")).thenExecute(() -> {
            Height[0] = Falling.getY();
            Helper.assertTrue(Height[0] > at(Helper, 16.5, 16.5).y + 2.0, "the device reached the ground before going off");
        }).thenExecuteAfter(20, () -> {
            Helper.assertTrue(Falling.isActive(), "the field ended too soon");
            Helper.assertTrue(Math.abs(Falling.getY() - Height[0]) < Still, "the active device moved from " + Height[0] + " to " + Falling.getY());
            Helper.assertFalse(Falling.onGround(), "the active device dropped to the floor");
            clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "z0003_sphere")
    public static void sphereCatchesInsideOnly(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer Inside = player(Helper, 13.5, 16.5, "z0003_inside");
        Z0003TestPlayer Outside = player(Helper, 24.5, 16.5, "z0003_outside");
        DClass Subject = dclass(Helper, 16.5, 13.5);
        Villager Local = villager(Helper, 19.5, 16.5);
        MtfOperative Soldier = operative(Helper, 16.5, 19.5);
        DClass Far = dclass(Helper, 16.5, 25.5);
        Villager Distant = villager(Helper, 7.5, 16.5);
        Z0003Device Knot = placed(Helper, 16.5, 0.0, 16.5, armed(World, 5.0f, 0));
        int Wait = Z0003Device.growTicks(5.0f) + 10;
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Knot.isActive(), "the device never went off")).thenExecuteAfter(Wait, () -> {
            Helper.assertTrue(statue(Inside), "the player inside the field was not caught");
            Helper.assertTrue(statue(Subject), "the D-Class inside the field was not caught");
            Helper.assertTrue(statue(Local), "the villager inside the field was not caught");
            Helper.assertTrue(statue(Soldier), "the MTF operative inside the field was not caught");
            Helper.assertFalse(statue(Outside), "the player outside the field was caught");
            Helper.assertFalse(statue(Far), "the D-Class outside the field was caught");
            Helper.assertFalse(statue(Distant), "the villager outside the field was caught");
            clean(Helper);
            for (Entity Each : List.<Entity>of(Inside, Outside, Subject, Local, Soldier, Far, Distant)) {
                dismiss(Helper, Each);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "z0003_walls")
    public static void reachesThroughWalls(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        int Floor = stand(Helper);
        for (int Y = Floor; Y < Floor + 4; Y++) {
            for (int Z = 12; Z <= 20; Z++) {
                Helper.setBlock(new BlockPos(19, Y, Z), Blocks.STONE);
            }
        }
        for (int X = 15; X <= 17; X++) {
            for (int Y = Floor; Y <= Floor + 3; Y++) {
                for (int Z = 20; Z <= 22; Z++) {
                    boolean Hollow = X == 16 && Z == 21 && Y > Floor - 1 && Y < Floor + 2;
                    Helper.setBlock(new BlockPos(X, Y, Z), Hollow ? Blocks.AIR : Blocks.STONE);
                }
            }
        }
        Villager Hidden = villager(Helper, 21.5, 16.5);
        DClass Boxed = dclass(Helper, 16.5, 21.5);
        Z0003Device Knot = placed(Helper, 16.5, 0.0, 16.5, armed(World, 6.0f, 0));
        int Wait = Z0003Device.growTicks(6.0f) + 10;
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Knot.isActive(), "the device never went off")).thenExecuteAfter(Wait, () -> {
            Helper.assertTrue(statue(Hidden), "the villager behind the wall was not caught");
            Helper.assertTrue(statue(Boxed), "the D-Class sealed in the box was not caught");
            clean(Helper);
            dismiss(Helper, Hidden);
            dismiss(Helper, Boxed);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "z0003_thrower")
    public static void throwerIsCaught(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer Thrower = player(Helper, 10.5, 16.5, "z0003_careless");
        Thrower.setYRot(-90.0f);
        Thrower.setXRot(30.0f);
        Z0003Device.thrown(World, Thrower, armed(World, 10.0f, 6), 0.0f);
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(statue(Thrower), "the thrower escaped their own field")).thenExecute(() -> {
            clean(Helper);
            dismiss(Helper, Thrower);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "z0003_held")
    public static void heldDeviceGoesOffInHand(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer Holder = player(Helper, 16.5, 16.5, "z0003_holding");
        Holder.setItemInHand(InteractionHand.MAIN_HAND, armed(World, 2.0f, 5));
        Helper.onEachTick(() -> Holder.getInventory().tick());
        Helper.startSequence().thenWaitUntil(() -> {
            Helper.assertTrue(Holder.getMainHandItem().isEmpty(), "the device stayed in the hand");
            Helper.assertTrue(statue(Holder), "the holder was not caught");
        }).thenExecute(() -> {
            Helper.assertTrue(around(Helper, Z0003Device.class).stream().anyMatch(Z0003Device::isActive), "no active device left the hand");
            clean(Helper);
            dismiss(Helper, Holder);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "z0003_frozen")
    public static void statueHoldsPose(GameTestHelper Helper) {
        Villager Frozen = villager(Helper, 12.5, 12.5);
        DClass Body = dclass(Helper, 20.5, 20.5);
        Frozen.yBodyRot = 37.0f;
        Frozen.setYHeadRot(52.0f);
        Frozen.setDeltaMovement(0.3, 0.0, 0.1);
        Frozen.getNavigation().moveTo(Frozen.getX() + 8.0, Frozen.getY(), Frozen.getZ(), 1.0);
        Body.getNavigation().moveTo(Body.getX() - 8.0, Body.getY(), Body.getZ(), 1.0);
        Helper.assertTrue(Z0003Petrify.petrify(Frozen), "the villager could not be turned to stone");
        Helper.assertTrue(Z0003Petrify.petrify(Body), "the D-Class could not be turned to stone");
        Vec3 Before = Frozen.position();
        Vec3 BodyBefore = Body.position();
        float Health = Frozen.getHealth();
        Helper.startSequence().thenExecuteAfter(40, () -> {
            Helper.assertTrue(Frozen.position().distanceTo(Before) < Still, "the villager statue moved");
            Helper.assertTrue(Body.position().distanceTo(BodyBefore) < Still, "the D-Class statue moved");
            Z0003Statue Statue = Z0003Petrify.statueOf(Frozen);
            Helper.assertTrue(Statue != null && Math.abs(Statue.BodyYaw() - 37.0f) < 0.01f && Math.abs(Statue.HeadYaw() - 52.0f) < 0.01f,
                    "the pose was not saved: " + Statue);
            Helper.assertFalse(Frozen.hurt(Frozen.damageSources().generic(), 5.0f), "the statue took damage");
            Helper.assertTrue(Frozen.getHealth() == Health, "the statue lost health");
            Helper.assertTrue(Frozen.isAlive() && Body.isAlive(), "a statue died");
            dismiss(Helper, Frozen);
            dismiss(Helper, Body);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "z0003_vial")
    public static void vialFreesStatue(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer Rescuer = player(Helper, 14.5, 16.5, "z0003_rescuer");
        Villager Held = villager(Helper, 16.5, 16.5);
        Z0003Petrify.petrify(Held);
        Rescuer.setItemInHand(InteractionHand.MAIN_HAND, Z0003VialItem.create(1));
        InteractionResult Result = Rescuer.interactOn(Held, InteractionHand.MAIN_HAND);
        Helper.assertTrue(Result.consumesAction(), "pouring the vial did nothing: " + Result);
        Helper.assertTrue(Z0003VialItem.is(Rescuer.getMainHandItem()) && Rescuer.getMainHandItem().getCount() == 1, "the vial was used up");
        Z0003Statue Statue = Z0003Petrify.statueOf(Held);
        Helper.assertTrue(Statue != null && Statue.cracking(), "the stone did not start to crack");
        long Poured = World.getGameTime();
        Helper.startSequence().thenWaitUntil(() -> Helper.assertFalse(statue(Held), "the villager is still stone")).thenExecute(() -> {
            Helper.assertTrue(World.getGameTime() - Poured >= Z0003Petrify.MobCrackTicks, "the stone broke before it finished cracking");
            Helper.assertTrue(Held.isAlive(), "the freed villager died");
            dismiss(Helper, Held);
            dismiss(Helper, Rescuer);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "z0003_endlessVial")
    public static void oneVialFreesSeveral(GameTestHelper Helper) {
        Z0003TestPlayer Rescuer = player(Helper, 16.5, 12.5, "z0003_endless");
        Villager First = villager(Helper, 12.5, 16.5);
        DClass Second = dclass(Helper, 16.5, 16.5);
        Villager Third = villager(Helper, 20.5, 16.5);
        List<LivingEntity> Held = List.of(First, Second, Third);
        for (LivingEntity Each : Held) {
            Helper.assertTrue(Z0003Petrify.petrify(Each), "could not turn " + Each.getName().getString() + " to stone");
        }
        Rescuer.setItemInHand(InteractionHand.MAIN_HAND, Z0003VialItem.create(1));
        Helper.onEachTick(() -> Rescuer.getCooldowns().tick());
        Helper.assertTrue(Z0003VialItem.pour(Rescuer, InteractionHand.MAIN_HAND, First).consumesAction(), "the first pour did nothing");
        Helper.assertFalse(Z0003VialItem.pour(Rescuer, InteractionHand.MAIN_HAND, Second).consumesAction(), "the vial poured again with no pause");
        Helper.startSequence().thenExecuteAfter(Z0003VialItem.CooldownTicks + 1, () -> {
            Helper.assertTrue(Z0003VialItem.pour(Rescuer, InteractionHand.MAIN_HAND, Second).consumesAction(), "the second pour did nothing");
        }).thenExecuteAfter(Z0003VialItem.CooldownTicks + 1, () -> {
            Helper.assertTrue(Z0003VialItem.pour(Rescuer, InteractionHand.MAIN_HAND, Third).consumesAction(), "the third pour did nothing");
        }).thenWaitUntil(() -> {
            for (LivingEntity Each : Held) {
                Helper.assertFalse(statue(Each), Each.getName().getString() + " is still stone");
            }
        }).thenExecute(() -> {
            ItemStack Vial = Rescuer.getMainHandItem();
            Helper.assertTrue(Z0003VialItem.is(Vial) && Vial.getCount() == 1, "the vial did not survive three pours: " + Vial);
            for (LivingEntity Each : Held) {
                Helper.assertTrue(Each.isAlive(), Each.getName().getString() + " died");
                dismiss(Helper, Each);
            }
            dismiss(Helper, Rescuer);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "z0003_frozenRender")
    public static void statueRenderStateHolds(GameTestHelper Helper) {
        Villager Still = villager(Helper, 12.5, 12.5);
        DClass Walker = dclass(Helper, 20.5, 20.5);
        Walker.walkAnimation.update(0.7f, 1.0f);
        Walker.walkAnimation.update(0.7f, 1.0f);
        Walker.attackAnim = 0.4f;
        Walker.oAttackAnim = 0.4f;
        Still.yBodyRot = 63.0f;
        Still.setYHeadRot(71.0f);
        Helper.assertTrue(Z0003Petrify.petrify(Still) && Z0003Petrify.petrify(Walker), "could not turn the subjects to stone");
        Z0003Frozen StillShot = Z0003Frozen.capture(Still).still();
        Z0003Frozen WalkerShot = Z0003Frozen.capture(Walker).still();
        Helper.startSequence().thenExecuteAfter(30, () -> {
            Helper.assertTrue(Z0003Frozen.capture(Still).samePose(StillShot), "the villager statue drifted: " + Z0003Frozen.capture(Still) + " from " + StillShot);
            Helper.assertTrue(Z0003Frozen.capture(Walker).samePose(WalkerShot), "the D-Class statue drifted: " + Z0003Frozen.capture(Walker) + " from " + WalkerShot);
            Z0003Frozen Live = Z0003Frozen.capture(Walker);
            Walker.tickCount += 137;
            Walker.yBodyRot += 40.0f;
            Walker.walkAnimation.update(0.2f, 0.5f);
            Walker.attackAnim = 0.9f;
            WalkerShot.apply(Walker);
            Helper.assertTrue(Z0003Frozen.capture(Walker).matches(WalkerShot), "the snapshot did not restore the pose: " + Z0003Frozen.capture(Walker));
            Z0003Frozen Again = Z0003Frozen.capture(Walker);
            Walker.walkAnimation.update(0.3f, 1.0f);
            Again.apply(Walker);
            Helper.assertTrue(Z0003Frozen.capture(Walker).matches(Again), "the snapshot drifted across repeated frames");
            Live.apply(Walker);
            Helper.assertTrue(Z0003Frozen.capture(Walker).matches(Live), "the live pose did not come back after rendering");
            dismiss(Helper, Still);
            dismiss(Helper, Walker);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "z0003_timeout")
    public static void timeoutFreesPlayersOnly(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0003Registry.setPlayersTimeOut(World.getServer(), true);
        Z0003TestPlayer Waiting = player(Helper, 12.5, 16.5, "z0003_waiting");
        Villager Kept = villager(Helper, 20.5, 16.5);
        DClass Subject = dclass(Helper, 16.5, 20.5);
        long Backdated = World.getGameTime() - (Z0003Petrify.TimeoutTicks - Z0003Petrify.CrackTicks);
        for (LivingEntity Each : List.<LivingEntity>of(Waiting, Kept, Subject)) {
            Helper.assertTrue(Z0003Petrify.petrify(Each), "could not turn " + Each.getName().getString() + " to stone");
            Each.setData(Z0003Registry.Statue.get(), Z0003Petrify.statueOf(Each).since(Backdated));
        }
        Helper.startSequence().thenWaitUntil(() -> Helper.assertFalse(statue(Waiting), "the player is still stone after 30 seconds")).thenExecuteAfter(10, () -> {
            Helper.assertTrue(statue(Kept), "the villager broke free on the player timer");
            Helper.assertTrue(statue(Subject), "the D-Class broke free on the player timer");
            Z0003Registry.setPlayersTimeOut(World.getServer(), false);
            dismiss(Helper, Waiting);
            dismiss(Helper, Kept);
            dismiss(Helper, Subject);
        }).thenSucceed();
    }

    @AfterBatch(batch = "z0003_timeout")
    public static void afterTimeout(ServerLevel World) {
        Z0003Registry.setPlayersTimeOut(World.getServer(), false);
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "z0003_ruleOff")
    public static void timeoutOffKeepsPlayers(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0003Registry.setPlayersTimeOut(World.getServer(), false);
        Z0003TestPlayer Waiting = player(Helper, 12.5, 16.5, "z0003_patient");
        Helper.assertTrue(Z0003Petrify.petrify(Waiting), "the player could not be turned to stone");
        Waiting.setData(Z0003Registry.Statue.get(), Z0003Petrify.statueOf(Waiting).since(World.getGameTime() - Z0003Petrify.TimeoutTicks * 2L));
        Helper.startSequence().thenExecuteAfter(60, () -> {
            Helper.assertTrue(statue(Waiting), "the player broke free with the rule off");
            dismiss(Helper, Waiting);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "z0003_pickup")
    public static void pickupOnlyWhenInactive(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer Finder = player(Helper, 6.5, 6.5, "z0003_finder");
        Z0003Device Resting = placed(Helper, 7.5, 0.0, 6.5, Z0003DeviceItem.create());
        Z0003Device Busy = placed(Helper, 24.5, 0.0, 24.5, armed(World, 1.0f, 0));
        Helper.startSequence().thenExecuteAfter(5, () -> {
            InteractionResult Taken = Resting.interact(Finder, InteractionHand.MAIN_HAND);
            Helper.assertTrue(Taken.consumesAction(), "the resting device could not be picked up: " + Taken);
            Helper.assertTrue(Resting.isRemoved(), "the picked up device stayed in the world");
            Helper.assertTrue(Finder.getInventory().contains(Z0003DeviceItem.create()), "the device did not reach the inventory");
            Helper.assertTrue(Busy.isActive(), "the second device is not active");
            InteractionResult Refused = Busy.interact(Finder, InteractionHand.MAIN_HAND);
            Helper.assertFalse(Refused.consumesAction(), "the active device was picked up");
            Helper.assertFalse(Busy.isRemoved(), "the active device left the world");
            clean(Helper);
            dismiss(Helper, Finder);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "z0003_yank")
    public static void yankCancelsField(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer Holder = player(Helper, 4.5, 16.5, "z0003_handler");
        Holder.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.LEAD, 2));
        Villager Bystander = villager(Helper, 18.5, 16.5);
        Z0003Device Tethered = placed(Helper, 12.5, 0.0, 16.5, armed(World, 8.0f, 2));
        Tethered.tether(Holder, true);
        Holder.getOffhandItem().shrink(1);
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Tethered.isActive(), "the tethered device never went off")).thenExecute(() -> {
            Helper.assertTrue(Tethered.yank(Holder), "the yank did nothing");
            Helper.assertFalse(Tethered.isActive(), "the field kept running after the yank");
        }).thenWaitUntil(() -> Helper.assertTrue(Tethered.isRemoved(), "the device never came back")).thenExecute(() -> {
            Helper.assertTrue(Holder.getInventory().contains(Z0003DeviceItem.create()), "the device did not return to the holder");
            Helper.assertTrue(Holder.getInventory().countItem(Items.LEAD) == 2, "the lead was not returned");
            Helper.assertFalse(statue(Bystander), "the cancelled field still reached the bystander");
            Helper.assertFalse(statue(Holder), "the cancelled field caught the holder");
            clean(Helper);
            dismiss(Helper, Bystander);
            dismiss(Helper, Holder);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "z0003_reload")
    public static void survivesReload(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Villager Held = villager(Helper, 10.5, 10.5);
        Held.yBodyRot = 81.0f;
        Z0003Petrify.petrify(Held);
        Z0003Statue Original = Z0003Petrify.statueOf(Held);
        Villager Copy = reload(Helper, Held, Villager.class);
        Helper.assertTrue(Original.equals(Z0003Petrify.statueOf(Copy)), "the villager statue did not survive a reload: " + Z0003Petrify.statueOf(Copy));
        Z0003TestPlayer User = player(Helper, 14.5, 14.5, "z0003_saved");
        Z0003Petrify.petrify(User);
        Z0003Statue PlayerStatue = Z0003Petrify.statueOf(User);
        CompoundTag Saved = new CompoundTag();
        User.saveWithoutId(Saved);
        dismiss(Helper, User);
        Z0003TestPlayer Returned = new Z0003TestPlayer(World, "z0003_returned");
        Returned.load(Saved);
        Helper.assertTrue(PlayerStatue.equals(Z0003Petrify.statueOf(Returned)), "the player statue did not survive a reload");
        Z0003Device Waiting = placed(Helper, 20.5, 0.0, 20.5, armed(World, 3.0f, 400));
        Z0003Device WaitingCopy = reload(Helper, Waiting, Z0003Device.class);
        Helper.assertTrue(WaitingCopy.isArmed() && WaitingCopy.due() == Waiting.due() && WaitingCopy.radius() == 3.0f, "the armed device lost its setting");
        Z0003Device Running = placed(Helper, 24.5, 0.0, 10.5, armed(World, 2.0f, 0));
        Running.activate();
        Z0003Device RunningCopy = reload(Helper, Running, Z0003Device.class);
        Helper.assertTrue(RunningCopy.isActive() && RunningCopy.fieldStart() == Running.fieldStart(), "the active field did not survive a reload");
        dismiss(Helper, Held);
        clean(Helper);
        Helper.succeed();
    }

    private static <T extends Entity> T reload(GameTestHelper Helper, Entity Source, Class<T> Kind) {
        CompoundTag Saved = new CompoundTag();
        Helper.assertTrue(Source.save(Saved), Source.getType().getDescriptionId() + " refused to save");
        Optional<Entity> Copy = EntityType.create(Saved, Helper.getLevel());
        Helper.assertTrue(Copy.isPresent() && Kind.isInstance(Copy.get()), "the saved entity did not load back");
        return Kind.cast(Copy.get());
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "z0003_storage")
    public static void storedDeviceStaysInert(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        int Floor = stand(Helper);
        BlockPos Spot = new BlockPos(16, Floor, 16);
        Helper.setBlock(Spot, StorageRegistry.SmallContainer.get().defaultBlockState());
        StorageContainerBlockEntity Locker = (StorageContainerBlockEntity) World.getBlockEntity(Helper.absolutePos(Spot));
        Helper.assertTrue(Locker != null, "the storage container has no block entity");
        Z0003TestPlayer Keeper = player(Helper, 16.5, 13.5, "z0003_keeper");
        ItemStack Kept = armed(World, 3.0f, 10);
        Kept.set(DataComponents.CUSTOM_NAME, Component.literal("Knot"));
        Keeper.setItemInHand(InteractionHand.MAIN_HAND, Kept);
        Helper.assertTrue(Locker.refusalFor(Kept.copyWithCount(1)) == null, "the container refused the device");
        Helper.assertTrue(Locker.refusalFor(Z0003VialItem.create(1)) == null, "the container refused a vial");
        Locker.store(Keeper, Keeper.getMainHandItem());
        Helper.assertTrue(Keeper.getMainHandItem().isEmpty(), "the device stayed in the hand");
        Helper.assertTrue(Z0003DeviceItem.is(Locker.peekContents()), "the container does not hold the device");
        Helper.assertFalse(Z0003DeviceItem.isArmed(Locker.peekContents()), "the stored device is still counting down");
        Helper.startSequence().thenExecuteAfter(40, () -> {
            Helper.assertTrue(around(Helper, Z0003Device.class).isEmpty(), "the stored device went off");
            Helper.assertTrue(Z0003DeviceItem.is(Locker.peekContents()), "the device left the container");
            Locker.take(Keeper, InteractionHand.MAIN_HAND);
            ItemStack Back = Keeper.getMainHandItem();
            Helper.assertTrue(Z0003DeviceItem.is(Back) && !Z0003DeviceItem.isArmed(Back), "the device did not come back intact");
            Helper.assertTrue("Knot".equals(Back.getHoverName().getString()), "the device lost its name in storage");
            Helper.assertFalse(statue(Keeper), "the keeper was caught");
            Helper.setBlock(Spot, Blocks.AIR);
            clean(Helper);
            dismiss(Helper, Keeper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "z0003_mtf")
    public static void operativeFreedAndRecalled(GameTestHelper Helper) {
        Z0003TestPlayer Caller = player(Helper, 16.5, 16.5, "z0003_caller");
        Caller.enlist(true);
        MtfMissions.deployNow(Caller, MtfTier.Wavering);
        List<MtfOperative> Squad = around(Helper, MtfOperative.class).stream().filter(Member -> Caller.getUUID().equals(Member.callerId())).toList();
        Helper.assertTrue(Squad.size() >= 2, "the squad did not arrive: " + Squad.size());
        MtfOperative Freed = Squad.get(0);
        MtfOperative Left = Squad.get(1);
        Helper.assertTrue(Z0003Petrify.petrify(Freed) && Z0003Petrify.petrify(Left), "the operatives could not be turned to stone");
        Caller.setItemInHand(InteractionHand.MAIN_HAND, Z0003VialItem.create(1));
        Helper.assertTrue(Z0003VialItem.pour(Caller, InteractionHand.MAIN_HAND, Freed).consumesAction(), "the vial did nothing to the operative");
        Helper.startSequence().thenWaitUntil(() -> Helper.assertFalse(statue(Freed), "the operative is still stone")).thenExecute(() -> {
            Helper.assertTrue(Freed.isAlive(), "the freed operative is gone");
            Helper.assertTrue(statue(Left), "the other operative broke free");
            MtfMissions.cancel(Helper.getLevel().getServer(), Caller.getUUID());
        }).thenExecuteAfter(2, () -> {
            Helper.assertTrue(Left.isRemoved(), "the stone operative stayed after the squad was recalled");
            Helper.assertTrue(Freed.isRemoved(), "the freed operative stayed after the squad was recalled");
            Caller.enlist(false);
            dismiss(Helper, Caller);
        }).thenSucceed();
    }
}
