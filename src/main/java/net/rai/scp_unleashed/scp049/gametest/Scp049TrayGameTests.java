package net.rai.scp_unleashed.scp049.gametest;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp049.Scp049Cured;
import net.rai.scp_unleashed.scp049.Scp049Discovery;
import net.rai.scp_unleashed.scp049.Scp049Doctor;
import org.slf4j.Logger;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp049TrayGameTests {
    public static final String Arena = Scp049AppleGameTests.Arena;
    public static final int Floor = Scp049AppleGameTests.Floor;
    public static final double Search = 48.0;
    private static final Logger Log = LogUtils.getLogger();

    private Scp049TrayGameTests() {
    }

    private static Scp049Doctor doctor(GameTestHelper Helper, int X, int Z, double FaceX, double FaceZ) {
        ServerLevel World = Helper.getLevel();
        BlockPos Tray = Helper.absolutePos(new BlockPos(X, Floor, Z));
        Scp049Doctor Doctor = Scp049Discovery.stage(World, Tray, Helper.absoluteVec(new Vec3(FaceX, Floor, FaceZ)));
        if (Doctor == null) {
            throw new IllegalStateException("SCP-049 could not be staged for the tray test at " + Tray);
        }
        return Doctor;
    }

    private static void place(GameTestHelper Helper, Scp049Doctor Doctor, double X, double Z) {
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        Doctor.moveTo(Spot.x, Spot.y, Spot.z, 180.0f, 0.0f);
        Doctor.getNavigation().stop();
    }

    private static DClass subject(GameTestHelper Helper, int X, int Z) {
        DClass Made = DClassRegistry.DClassType.get().spawn(Helper.getLevel(), Helper.absolutePos(new BlockPos(X, Floor, Z)), MobSpawnType.SPAWN_EGG);
        if (Made == null) {
            throw new IllegalStateException("the D-Class could not be spawned");
        }
        Made.setNoAi(true);
        return Made;
    }

    private static int risen(GameTestHelper Helper) {
        Vec3 Centre = Helper.absoluteVec(new Vec3(24.0, Floor, 24.0));
        return Helper.getLevel().getEntitiesOfClass(Scp049Cured.class, new AABB(Centre, Centre).inflate(Search), Scp049Cured::isAlive).size();
    }

    private static void tidy(GameTestHelper Helper) {
        Helper.killAllEntitiesOfClass(Scp049Doctor.class);
        Helper.killAllEntitiesOfClass(Scp049Cured.class);
        Helper.killAllEntitiesOfClass(DClass.class);
        Helper.killAllEntitiesOfClass(ItemEntity.class);
    }

    private static String state(GameTestHelper Helper, Scp049Doctor Doctor) {
        return " [routine " + Doctor.routine() + ", action " + Doctor.action() + ", held " + Doctor.heldId() + ", quarry " + Doctor.quarryId() + ", roaming "
                + Doctor.isRoaming() + ", bar " + Doctor.trayBar(Helper.getLevel()) + ", spots " + Doctor.shunnedSpots() + ", alarms " + Doctor.alarms() + ", at "
                + Doctor.blockPosition() + "]";
    }

    private static void killedNotCarried(GameTestHelper Helper, Scp049Doctor Doctor, DClass Victim, String Why) {
        boolean[] Taken = new boolean[1];
        Helper.onEachTick(() -> Taken[0] |= Victim.getUUID().equals(Doctor.heldId()));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(!Victim.isAlive(), "SCP-049 never killed the D-Class " + Why + state(Helper, Doctor)))
                .thenExecute(() -> {
                    Log.info("SCP-049 killed the D-Class {}, tray bar {}", Why, Doctor.trayBar(Helper.getLevel()));
                    Helper.assertTrue(!Taken[0], "SCP-049 carried the D-Class off " + Why + state(Helper, Doctor));
                    Helper.assertTrue(!Doctor.trayUsable(Helper.getLevel()), "SCP-049 still counts its tray as usable " + Why + state(Helper, Doctor));
                })
                .thenIdle(Scp049Doctor.RiseDelay + 20)
                .thenExecute(() -> {
                    Helper.assertTrue(risen(Helper) == 0, "the D-Class rose as SCP-049-2 " + Why + state(Helper, Doctor));
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp049_tray_apple")
    public static void killsWhenAppleBarsTray(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 24, 24.5, 16.0);
        place(Helper, Doctor, 24.5, 14.5);
        ItemEntity Apple = Helper.spawnItem(Items.APPLE, 29.5f, Floor, 27.5f);
        DClass Victim = subject(Helper, 24, 10);
        Helper.onEachTick(() -> Helper.assertTrue(Doctor.alarms() == 0 && Apple.isAlive(), "SCP-049 fled or the apple vanished, so this proves nothing" + state(Helper, Doctor)));
        killedNotCarried(Helper, Doctor, Victim, "while an apple lay beside its tray");
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp049_tray_walled")
    public static void killsWhenTrayWalledOff(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 24, 24.5, 16.0);
        place(Helper, Doctor, 24.5, 13.5);
        for (int X = 20; X <= 28; X++) {
            for (int Z = 20; Z <= 28; Z++) {
                if (X != 20 && X != 28 && Z != 20 && Z != 28) {
                    continue;
                }
                for (int Y = Floor; Y <= Floor + 2; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.STONE);
                }
            }
        }
        DClass Victim = subject(Helper, 24, 9);
        killedNotCarried(Helper, Doctor, Victim, "while its tray was walled off");
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp049_tray_removed")
    public static void killsWhenTrayRemoved(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 24, 24.5, 16.0);
        Helper.setBlock(new BlockPos(24, Floor, 24), Blocks.AIR);
        DClass Victim = subject(Helper, 24, 19);
        killedNotCarried(Helper, Doctor, Victim, "after its tray was removed");
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scp049_tray_carried")
    public static void killsCarriedWhenTrayBarred(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 24, 24.5, 16.0);
        DClass Victim = subject(Helper, 24, 12);
        boolean[] Operated = new boolean[1];
        ItemEntity[] Apple = new ItemEntity[1];
        Helper.onEachTick(() -> Operated[0] |= Doctor.routine() == Scp049Doctor.Phase.Operate);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Victim.getUUID().equals(Doctor.heldId()), "SCP-049 never took hold of the D-Class" + state(Helper, Doctor)))
                .thenExecute(() -> Apple[0] = Helper.spawnItem(Items.APPLE, 29.5f, Floor, 27.5f))
                .thenWaitUntil(() -> Helper.assertTrue(!Victim.isAlive(), "SCP-049 kept carrying the D-Class to a tray beside an apple" + state(Helper, Doctor)))
                .thenExecute(() -> {
                    Helper.assertTrue(Apple[0].isAlive(), "the apple vanished, so this proves nothing");
                    Helper.assertTrue(Doctor.alarms() == 0, "SCP-049 fled from the apple instead of keeping clear of its tray" + state(Helper, Doctor));
                    Helper.assertTrue(Doctor.heldId() == null, "SCP-049 is still holding someone" + state(Helper, Doctor));
                })
                .thenIdle(Scp049Doctor.RiseDelay + 20)
                .thenExecute(() -> {
                    Helper.assertTrue(!Operated[0], "SCP-049 operated although its tray was barred" + state(Helper, Doctor));
                    Helper.assertTrue(risen(Helper) == 0, "the carried D-Class rose as SCP-049-2" + state(Helper, Doctor));
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1600, batch = "scp049_tray_resume")
    public static void resumesSurgeryWhenTrayClears(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 24, 24.5, 16.0);
        place(Helper, Doctor, 24.5, 14.5);
        ItemEntity Apple = Helper.spawnItem(Items.APPLE, 29.5f, Floor, 27.5f);
        DClass[] Victim = new DClass[1];
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Doctor.isRoaming(), "SCP-049 did not keep clear of a tray beside an apple" + state(Helper, Doctor)))
                .thenExecute(() -> {
                    Helper.assertTrue(!Doctor.trayUsable(Helper.getLevel()), "SCP-049 counts a tray beside an apple as usable" + state(Helper, Doctor));
                    Helper.assertTrue(Doctor.alarms() == 0, "SCP-049 fled, so this proves nothing" + state(Helper, Doctor));
                    Apple.discard();
                })
                .thenWaitUntil(() -> Helper.assertTrue(!Doctor.isRoaming() && Doctor.routine() == Scp049Doctor.Phase.Wait, "SCP-049 did not go back to its tray once the apple was gone"
                        + state(Helper, Doctor)))
                .thenExecute(() -> Victim[0] = subject(Helper, 24, 19))
                .thenWaitUntil(() -> Helper.assertTrue(Victim[0].getUUID().equals(Doctor.heldId()), "SCP-049 did not take hold of the D-Class once its tray was clear"
                        + state(Helper, Doctor)))
                .thenWaitUntil(() -> Helper.assertTrue(!Victim[0].isAlive(), "SCP-049 never finished operating on the D-Class" + state(Helper, Doctor)))
                .thenWaitUntil(() -> Helper.assertTrue(risen(Helper) >= 1, "nothing rose after SCP-049 operated at its cleared tray" + state(Helper, Doctor)))
                .thenExecute(() -> tidy(Helper))
                .thenSucceed();
    }
}
