package net.rai.scp_unleashed.scp049.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp049.Scp049Discovery;
import net.rai.scp_unleashed.scp049.Scp049Doctor;
import net.rai.scp_unleashed.scp049.Scp049Fear;
import net.rai.scp_unleashed.scp049.Scp049Hold;
import net.rai.scp_unleashed.scp049.Scp049Registry;
import org.slf4j.Logger;

import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp049AppleGameTests {
    public static final String Arena = "scp049_apple_arena";
    public static final int Floor = 2;
    public static final double NoSlide = Scp049Doctor.StrideGait.Reference() * Scp049Doctor.StrideGait.MaxRate();
    private static final Logger Log = LogUtils.getLogger();

    private Scp049AppleGameTests() {
    }

    private static Scp049Doctor doctor(GameTestHelper Helper, int X, int Z, double FaceX, double FaceZ) {
        ServerLevel World = Helper.getLevel();
        BlockPos Tray = Helper.absolutePos(new BlockPos(X, Floor, Z));
        Scp049Doctor Doctor = Scp049Discovery.stage(World, Tray, Helper.absoluteVec(new Vec3(FaceX, Floor, FaceZ)));
        if (Doctor == null) {
            throw new IllegalStateException("SCP-049 could not be staged for the apple test at " + Tray);
        }
        return Doctor;
    }

    private static FakePlayer bearer(GameTestHelper Helper, double X, double Z, ItemStack Main, ItemStack Off) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp049_apple"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        User.setItemInHand(InteractionHand.MAIN_HAND, Main);
        User.setItemInHand(InteractionHand.OFF_HAND, Off);
        World.addNewPlayer(User);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, FakePlayer User) {
        Scp049Hold.release(User);
        Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
    }

    private static void tidy(GameTestHelper Helper) {
        Helper.killAllEntitiesOfClass(Scp049Doctor.class);
        Helper.killAllEntitiesOfClass(Villager.class);
        Helper.killAllEntitiesOfClass(ItemEntity.class);
    }

    private static double flat(Vec3 From, Vec3 To) {
        double Dx = To.x - From.x;
        double Dz = To.z - From.z;
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    private static String state(Scp049Doctor Doctor) {
        return " [routine " + Doctor.routine() + ", action " + Doctor.action() + ", held " + Doctor.heldId() + ", alarms " + Doctor.alarms() + ", spots " + Doctor.shunnedSpots()
                + ", corners " + Doctor.corners() + ", at " + Doctor.blockPosition() + "]";
    }

    private static boolean locked(LivingEntity Body) {
        AttributeInstance Speed = Body.getAttribute(Attributes.MOVEMENT_SPEED);
        return Speed != null && Speed.hasModifier(Scp049Hold.SpeedLock);
    }

    @GameTest(template = Arena, timeoutTicks = 420, batch = "scp049_apple_mainHand")
    public static void fleesFromMainHandApple(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 12, 24.5, 4.0);
        FakePlayer User = bearer(Helper, 24.5, 6.5, new ItemStack(Items.APPLE), ItemStack.EMPTY);
        Vec3[] Marks = new Vec3[2];
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.routine() == Scp049Doctor.Phase.Flee, "SCP-049 did not flee from an apple in the main hand" + state(Doctor)))
                .thenExecuteAfter(20, () -> Marks[0] = Doctor.position())
                .thenExecuteAfter(40, () -> {
                    Marks[1] = Doctor.position();
                    double Speed = flat(Marks[0], Marks[1]) / 40.0;
                    Log.info("SCP-049 flee speed {} blocks per tick, stride limit {}", Speed, NoSlide);
                    Helper.assertTrue(Speed > 0.05, "SCP-049 is barely moving while it flees, speed " + Speed + state(Doctor));
                    Helper.assertTrue(Speed <= NoSlide, "SCP-049 flees faster than its stride can follow, speed " + Speed + state(Doctor));
                    Helper.assertTrue(Doctor.isHurried(), "SCP-049 is not using its hurried stride while it flees" + state(Doctor));
                })
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.routine() == Scp049Doctor.Phase.Wary && Doctor.distanceTo(User) >= Scp049Doctor.FleeSafe,
                        "SCP-049 did not get clear and settle into wariness, gap " + Doctor.distanceTo(User) + state(Doctor)))
                .thenExecute(() -> {
                    Helper.assertTrue(!Scp049Hold.isHeld(User), "SCP-049 seized the player holding the apple");
                    Helper.assertTrue(Doctor.quarryId() == null, "SCP-049 still treats the apple holder as its quarry" + state(Doctor));
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp049_apple_offHand")
    public static void fleesFromOffHandApple(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 12, 24.5, 4.0);
        FakePlayer User = bearer(Helper, 24.5, 6.5, ItemStack.EMPTY, new ItemStack(Items.APPLE));
        double[] Start = new double[1];
        Helper.startSequence()
                .thenExecute(() -> Start[0] = Doctor.distanceTo(User))
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.routine() == Scp049Doctor.Phase.Flee, "SCP-049 did not flee from an apple in the off hand" + state(Doctor)))
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.distanceTo(User) >= Start[0] + 5.0, "SCP-049 did not back away from the off hand apple, gap "
                        + Doctor.distanceTo(User) + state(Doctor)))
                .thenExecute(() -> {
                    Helper.assertTrue(!Scp049Hold.isHeld(User), "SCP-049 seized the player holding the apple");
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 240, batch = "scp049_apple_dropped")
    public static void fleesFromDroppedApple(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 12, 24.5, 4.0);
        ItemEntity Apple = Helper.spawnItem(Items.APPLE, 24.5f, Floor, 7.5f);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.routine() == Scp049Doctor.Phase.Flee, "SCP-049 did not flee from an apple on the ground" + state(Doctor)))
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.distanceTo(Apple) >= Scp049Fear.BerthRadius + 2.0, "SCP-049 did not give the dropped apple a wide berth, gap "
                        + Doctor.distanceTo(Apple) + state(Doctor)))
                .thenExecute(() -> {
                    Helper.assertTrue(Doctor.isAfraid(), "SCP-049 calmed down while the apple is still there" + state(Doctor));
                    Helper.assertTrue(Doctor.shunnedSpots() >= 1, "SCP-049 does not remember where the apple lies" + state(Doctor));
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp049_apple_noApple")
    public static void ignoresPlayerWithoutApple(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 12, 24.5, 4.0);
        FakePlayer User = bearer(Helper, 24.5, 6.5, new ItemStack(Items.BREAD), new ItemStack(Items.CARROT));
        boolean[] Scared = new boolean[1];
        Helper.onEachTick(() -> Scared[0] |= Doctor.isAfraid());
        Helper.startSequence()
                .thenIdle(80)
                .thenExecute(() -> {
                    Helper.assertTrue(!Scared[0] && Doctor.alarms() == 0, "SCP-049 fled from a player with no apple" + state(Doctor));
                    Helper.assertTrue(User.getUUID().equals(Doctor.quarryId()), "SCP-049 did not take an interest in the player without an apple" + state(Doctor));
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scp049_apple_resume")
    public static void resumesAfterWaryPeriod(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 12, 24.5, 4.0);
        ItemEntity Apple = Helper.spawnItem(Items.APPLE, 24.5f, Floor, 7.5f);
        long[] WaryFrom = new long[1];
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.routine() == Scp049Doctor.Phase.Flee, "SCP-049 did not flee from the apple" + state(Doctor)))
                .thenIdle(20)
                .thenExecute(Apple::discard)
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.routine() == Scp049Doctor.Phase.Wary, "SCP-049 did not turn wary once the apple was gone" + state(Doctor)))
                .thenExecute(() -> WaryFrom[0] = Helper.getTick())
                .thenWaitUntil(() -> Helper.assertTrue(!Doctor.isAfraid(), "SCP-049 never got over the apple" + state(Doctor)))
                .thenExecute(() -> {
                    long Wary = Helper.getTick() - WaryFrom[0];
                    Helper.assertTrue(Wary >= Scp049Doctor.WaryTicks - 2, "SCP-049 resumed after only " + Wary + " ticks of wariness" + state(Doctor));
                    Scp049Doctor.Phase Now = Doctor.routine();
                    Helper.assertTrue(Now == Scp049Doctor.Phase.Return || Now == Scp049Doctor.Phase.Wait || Now == Scp049Doctor.Phase.Roam,
                            "SCP-049 did not go back to its routine" + state(Doctor));
                    Helper.assertTrue(Doctor.shunnedSpots() == 0, "SCP-049 still avoids an apple that is gone" + state(Doctor));
                })
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.routine() == Scp049Doctor.Phase.Wait, "SCP-049 did not walk back to its tray" + state(Doctor)))
                .thenExecute(() -> tidy(Helper))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1000, batch = "scp049_apple_hunt")
    public static void huntsAgainAfterFleeing(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 12, 24.5, 4.0);
        ItemEntity Apple = Helper.spawnItem(Items.APPLE, 24.5f, Floor, 7.5f);
        long[] WaryFrom = new long[1];
        FakePlayer[] User = new FakePlayer[1];
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.routine() == Scp049Doctor.Phase.Flee, "SCP-049 did not flee from the apple" + state(Doctor)))
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.routine() == Scp049Doctor.Phase.Wary, "SCP-049 did not get clear of the apple" + state(Doctor)))
                .thenExecute(() -> WaryFrom[0] = Helper.getTick())
                .thenWaitUntil(() -> Helper.assertTrue(!Doctor.isAfraid(), "SCP-049 stayed afraid of an apple it had already left behind" + state(Doctor)))
                .thenExecute(() -> {
                    long Wary = Helper.getTick() - WaryFrom[0];
                    Log.info("SCP-049 stayed wary for {} ticks before going back to its routine", Wary);
                    Helper.assertTrue(Wary <= Scp049Doctor.WaryTicks + 40, "SCP-049 stayed wary for " + Wary + " ticks while the apple lay by its tray" + state(Doctor));
                    Helper.assertTrue(Apple.isAlive(), "the apple vanished, so this proves nothing");
                })
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.isRoaming(), "SCP-049 went back to a tray that lies beside an apple" + state(Doctor)))
                .thenExecute(() -> {
                    Vec3 Here = Doctor.position().subtract(Helper.absoluteVec(Vec3.ZERO));
                    double Side = Here.x > 24.0 ? -3.0 : 3.0;
                    User[0] = bearer(Helper, Here.x + Side, Here.z, ItemStack.EMPTY, ItemStack.EMPTY);
                })
                .thenWaitUntil(() -> Helper.assertTrue(User[0].getUUID().equals(Doctor.quarryId()), "SCP-049 did not hunt again after fleeing the apple" + state(Doctor)))
                .thenExecute(() -> {
                    Helper.assertTrue(!Doctor.isAfraid(), "SCP-049 is afraid again while far from the apple" + state(Doctor));
                    dismiss(Helper, User[0]);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "scp049_apple_forget")
    public static void forgetsUnseenApple(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 24, 24.5, 16.0);
        ItemEntity Apple = Helper.spawnItem(Items.APPLE, 24.5f, Floor, 12.5f);
        long[] HiddenAt = new long[1];
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.shunnedSpots() >= 1, "SCP-049 did not notice the apple" + state(Doctor)))
                .thenExecute(() -> {
                    Helper.assertTrue(!Doctor.isAfraid(), "SCP-049 fled from an apple well out of range" + state(Doctor));
                    for (int X = 14; X <= 34; X++) {
                        for (int Y = Floor; Y <= Floor + 3; Y++) {
                            Helper.setBlock(new BlockPos(X, Y, 18), Blocks.STONE);
                        }
                    }
                    HiddenAt[0] = Helper.getTick();
                })
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.shunnedSpots() == 0, "SCP-049 still remembers an apple it has not seen for " + (Helper.getTick() - HiddenAt[0])
                        + " ticks" + state(Doctor)))
                .thenExecute(() -> {
                    long Hidden = Helper.getTick() - HiddenAt[0];
                    Log.info("SCP-049 forgot the hidden apple after {} ticks", Hidden);
                    Helper.assertTrue(Apple.isAlive(), "the apple vanished, so this proves nothing");
                    Helper.assertTrue(Hidden >= Scp049Fear.BerthTicks - 40, "SCP-049 forgot the apple after only " + Hidden + " ticks" + state(Doctor));
                    Helper.assertTrue(!Doctor.isAfraid(), "SCP-049 got scared behind the wall" + state(Doctor));
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp049_apple_carried")
    public static void dropsCarriedVillagerSafely(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 24, 24.5, 16.0);
        Villager Patient = Helper.spawn(EntityType.VILLAGER, new Vec3(24.5, Floor, 20.5));
        FakePlayer[] User = new FakePlayer[1];
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Patient.getUUID().equals(Doctor.heldId()), "SCP-049 never took hold of the villager" + state(Doctor)))
                .thenExecute(() -> {
                    Helper.assertTrue(Scp049Hold.isHeld(Patient) && locked(Patient), "the villager is not held");
                    Vec3 Here = Doctor.position().subtract(Helper.absoluteVec(Vec3.ZERO));
                    User[0] = bearer(Helper, Here.x + 5.0, Here.z, new ItemStack(Items.APPLE), ItemStack.EMPTY);
                })
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.routine() == Scp049Doctor.Phase.Flee, "SCP-049 kept working on its patient beside an apple" + state(Doctor)))
                .thenExecute(() -> {
                    Helper.assertTrue(Doctor.heldId() == null, "SCP-049 fled still holding the villager" + state(Doctor));
                    Helper.assertTrue(!Scp049Hold.isHeld(Patient), "the villager is still marked as held");
                    Helper.assertTrue(!locked(Patient), "the villager is still slowed by the hold");
                    Helper.assertTrue(!Patient.isNoAi(), "the villager's AI was not given back");
                    Helper.assertTrue(Patient.isAlive(), "the villager died when it was dropped");
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    Helper.assertTrue(Patient.isAlive() && !Scp049Hold.isHeld(Patient), "the villager was taken again or died after the drop" + state(Doctor));
                    Helper.assertTrue(Doctor.heldId() == null, "SCP-049 took hold of someone again while afraid" + state(Doctor));
                    dismiss(Helper, User[0]);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp049_apple_victim")
    public static void victimDrawsApple(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 24, 24.5, 16.0);
        FakePlayer User = bearer(Helper, 24.5, 20.5, ItemStack.EMPTY, ItemStack.EMPTY);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(User.getUUID().equals(Doctor.heldId()), "SCP-049 never took hold of the player" + state(Doctor)))
                .thenExecute(() -> {
                    Helper.assertTrue(Scp049Hold.isHeld(User) && locked(User), "the player is not held");
                    User.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.APPLE));
                })
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.routine() == Scp049Doctor.Phase.Flee, "SCP-049 kept carrying a player who drew an apple" + state(Doctor)))
                .thenExecute(() -> {
                    Helper.assertTrue(Doctor.heldId() == null && !Scp049Hold.isHeld(User), "the player was not let go" + state(Doctor));
                    Helper.assertTrue(!locked(User), "the player is still slowed by the hold");
                    Helper.assertTrue(User.isAlive(), "the player died when dropped");
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp049_apple_cornered")
    public static void cannotBePinnedForever(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 12, 24.5, 4.0);
        FakePlayer User = bearer(Helper, 24.5, 6.5, new ItemStack(Items.APPLE), ItemStack.EMPTY);
        boolean[] Chasing = {true};
        Helper.onEachTick(() -> {
            if (Chasing[0] && Doctor.isAfraid()) {
                User.moveTo(Doctor.getX(), Doctor.getY(), Doctor.getZ() - 4.0, 0.0f, 0.0f);
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.routine() == Scp049Doctor.Phase.Flee, "SCP-049 did not flee from the apple" + state(Doctor)))
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.corners() >= 1, "SCP-049 was herded by the apple without end" + state(Doctor)))
                .thenExecute(() -> {
                    Chasing[0] = false;
                    Helper.assertTrue(Doctor.isBrave() && !Doctor.isAfraid(), "SCP-049 did not steel itself once cornered" + state(Doctor));
                })
                .thenExecuteFor(40, () -> Helper.assertTrue(!Doctor.isAfraid(), "SCP-049 fled again while it was steeled" + state(Doctor)))
                .thenExecute(() -> {
                    Helper.assertTrue(!Scp049Hold.isHeld(User), "SCP-049 seized the apple holder while steeled" + state(Doctor));
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp049_apple_golden")
    public static void goldenApplesCount(GameTestHelper Helper) {
        Helper.assertTrue(new ItemStack(Items.APPLE).is(Scp049Registry.Repellent), "the apple is missing from the scp049_repellent tag");
        Helper.assertTrue(new ItemStack(Items.GOLDEN_APPLE).is(Scp049Registry.Repellent), "the golden apple is missing from the scp049_repellent tag");
        Helper.assertTrue(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE).is(Scp049Registry.Repellent), "the enchanted golden apple is missing from the scp049_repellent tag");
        Helper.assertTrue(!new ItemStack(Items.BREAD).is(Scp049Registry.Repellent), "bread counts as a repellent");
        Scp049Doctor Doctor = doctor(Helper, 24, 12, 24.5, 4.0);
        FakePlayer User = bearer(Helper, 24.5, 6.5, new ItemStack(Items.GOLDEN_APPLE), ItemStack.EMPTY);
        FakePlayer Other = bearer(Helper, 40.5, 40.5, ItemStack.EMPTY, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
        Helper.startSequence()
                .thenExecute(() -> Helper.assertTrue(Scp049Fear.isBearer(Other) && Scp049Fear.isBearer(User), "golden apples do not make their holder a bearer"))
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.routine() == Scp049Doctor.Phase.Flee, "SCP-049 did not flee from a golden apple" + state(Doctor)))
                .thenExecute(() -> {
                    dismiss(Helper, User);
                    dismiss(Helper, Other);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp049_apple_hidden")
    public static void hiddenAppleIsUnseen(GameTestHelper Helper) {
        for (int X = 19; X <= 29; X++) {
            for (int Y = Floor; Y <= Floor + 2; Y++) {
                Helper.setBlock(new BlockPos(X, Y, 8), Blocks.STONE);
            }
        }
        Scp049Doctor Doctor = doctor(Helper, 24, 12, 24.5, 4.0);
        FakePlayer User = bearer(Helper, 24.5, 6.5, new ItemStack(Items.APPLE), ItemStack.EMPTY);
        boolean[] Scared = new boolean[1];
        Helper.startSequence()
                .thenExecuteFor(50, () -> Scared[0] |= Doctor.isAfraid())
                .thenExecute(() -> {
                    Helper.assertTrue(!Scared[0], "SCP-049 fled from an apple it could not see" + state(Doctor));
                    for (int X = 19; X <= 29; X++) {
                        for (int Y = Floor; Y <= Floor + 2; Y++) {
                            Helper.setBlock(new BlockPos(X, Y, 8), Blocks.AIR);
                        }
                    }
                })
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.routine() == Scp049Doctor.Phase.Flee, "SCP-049 did not flee once the wall came down" + state(Doctor)))
                .thenExecute(() -> {
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp049_apple_detour")
    public static void pathsAroundDroppedApple(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 6, 24, 16.0, 24.5);
        ItemEntity Apple = Helper.spawnItem(Items.APPLE, 19.5f, Floor, 28.5f);
        BlockPos Goal = Helper.absolutePos(new BlockPos(34, Floor, 24));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.shunnedSpots() >= 1 && Doctor.onGround(), "SCP-049 did not notice the apple across the room" + state(Doctor)))
                .thenExecute(() -> {
                    Helper.assertTrue(!Doctor.isAfraid(), "SCP-049 fled from an apple well out of range" + state(Doctor));
                    Path Route = Doctor.getNavigation().createPath(Goal, 0);
                    Helper.assertTrue(Route != null && Route.canReach(), "SCP-049 found no way past the apple, path " + (Route == null ? "none" : Route.getNodeCount() + " nodes ending at "
                            + Route.getEndNode()) + state(Doctor));
                    double Closest = Double.MAX_VALUE;
                    for (int Index = 0; Index < Route.getNodeCount(); Index++) {
                        Closest = Math.min(Closest, flat(Vec3.atBottomCenterOf(Route.getNodePos(Index)), Apple.position()));
                    }
                    Log.info("SCP-049 detour passes {} blocks from the apple", Closest);
                    Helper.assertTrue(Closest >= Scp049Fear.BerthRadius - 1.0, "SCP-049's path runs " + Closest + " blocks from the apple" + state(Doctor));
                    Apple.discard();
                })
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.shunnedSpots() == 0 && Doctor.onGround(), "SCP-049 still avoids an apple that is gone" + state(Doctor)))
                .thenExecute(() -> {
                    Path Route = Doctor.getNavigation().createPath(Goal, 0);
                    Helper.assertTrue(Route != null && Route.canReach(), "SCP-049 found no way across the empty room" + state(Doctor));
                    double Closest = Double.MAX_VALUE;
                    for (int Index = 0; Index < Route.getNodeCount(); Index++) {
                        Closest = Math.min(Closest, flat(Vec3.atBottomCenterOf(Route.getNodePos(Index)), Helper.absoluteVec(new Vec3(19.5, Floor, 28.5))));
                    }
                    Helper.assertTrue(Closest < 6.0, "SCP-049 still detours around a spot where the apple was, closest " + Closest + state(Doctor));
                    tidy(Helper);
                })
                .thenSucceed();
    }
}
