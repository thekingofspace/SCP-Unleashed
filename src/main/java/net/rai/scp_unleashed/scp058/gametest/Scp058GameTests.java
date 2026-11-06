package net.rai.scp_unleashed.scp058.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp058.Scp058Heart;
import net.rai.scp_unleashed.scp058.Scp058Navigation;
import net.rai.scp_unleashed.scp058.Scp058Registry;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp058GameTests {
    public static final String Cell = "scp058_cell";
    public static final String Arena = "scp058_arena";
    public static final int HoldLimit = Scp058Heart.PinTicks + Scp058Navigation.StallTicks + 20;
    public static final int DropBy = 60;
    public static final int LureTicks = 200;
    public static final int LureEvery = 20;
    public static final double CellFloor = 2.0;
    public static final double CellCeiling = 7.0;
    private static final Vec3 CeilingCorner = new Vec3(2.6, CellCeiling - Scp058Heart.Height - 0.05, 2.6);
    private static final Vec3 AboveRoof = new Vec3(2.5, CellCeiling + 1.0, 2.5);

    private Scp058GameTests() {
    }

    private static Scp058Heart heart(GameTestHelper Helper, Vec3 Rel) {
        ServerLevel World = Helper.getLevel();
        Scp058Heart Heart = Scp058Registry.Heart.get().create(World);
        Helper.assertTrue(Heart != null, "could not build SCP-058");
        Vec3 Spot = Helper.absoluteVec(Rel);
        Heart.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addFreshEntity(Heart);
        return Heart;
    }

    private static Scp058Heart cornered(GameTestHelper Helper) {
        Scp058Heart Heart = heart(Helper, CeilingCorner);
        Vec3 Lure = Helper.absoluteVec(AboveRoof);
        Heart.getNavigation().moveTo(Lure.x, Lure.y, Lure.z, 1.0);
        return Heart;
    }

    private static void watchCling(GameTestHelper Helper, Scp058Heart Heart, int Limit) {
        int[] Streak = {0};
        Helper.onEachTick(() -> {
            if (!Heart.isAlive()) {
                return;
            }
            Streak[0] = Heart.isClimbing() && !Heart.onGround() ? Streak[0] + 1 : 0;
            if (Streak[0] > Limit) {
                Helper.fail("SCP-058 clung to a wall or ceiling for more than " + Limit + " ticks at " + Heart.position());
            }
        });
    }

    private static void mustDrop(GameTestHelper Helper, Scp058Heart Heart) {
        double Floor = Helper.absoluteVec(new Vec3(0.0, CellFloor, 0.0)).y;
        boolean[] Dropped = {false};
        Helper.onEachTick(() -> {
            if (!Dropped[0] && Heart.onGround() && Heart.getY() < Floor + 0.5) {
                Dropped[0] = true;
            }
            if (!Dropped[0] && Helper.getTick() > DropBy) {
                Helper.fail("SCP-058 stayed up in the ceiling corner at " + Heart.position());
            }
        });
    }

    private static boolean hurt(LivingEntity Prey) {
        return !Prey.isAlive() || Prey.getHealth() < Prey.getMaxHealth();
    }

    @GameTest(template = Cell, timeoutTicks = 600, batch = "scp058_leaves_ceiling_corner")
    public static void leavesCeilingCorner(GameTestHelper Helper) {
        Scp058Heart Heart = cornered(Helper);
        watchCling(Helper, Heart, HoldLimit);
        mustDrop(Helper, Heart);
        Vec3 Lure = Helper.absoluteVec(AboveRoof);
        Helper.onEachTick(() -> {
            if (Helper.getTick() < LureTicks && Helper.getTick() % LureEvery == 0) {
                Heart.getNavigation().moveTo(Lure.x, Lure.y, Lure.z, 1.0);
            }
        });
        Vec3[] Landing = {null};
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Heart.isAlive(), "SCP-058 is gone");
            Helper.assertTrue(Helper.getTick() >= LureTicks, "SCP-058 is still being lured above the roof");
            if (Landing[0] == null) {
                Helper.assertTrue(Heart.onGround() && !Heart.isClimbing(), "SCP-058 has not come down yet");
                Landing[0] = Heart.position();
            }
            if (Heart.getNavigation().isDone()) {
                Heart.prowl();
            }
            double DeltaX = Heart.getX() - Landing[0].x;
            double DeltaZ = Heart.getZ() - Landing[0].z;
            Helper.assertTrue(DeltaX * DeltaX + DeltaZ * DeltaZ > 2.25, "SCP-058 has not wandered off yet");
            Helper.assertTrue(Heart.onGround(), "SCP-058 is not walking on the floor");
        });
    }

    @GameTest(template = Cell, timeoutTicks = 400, batch = "scp058_hunts_from_ceiling_corner")
    public static void huntsFromCeilingCorner(GameTestHelper Helper) {
        Scp058Heart Heart = cornered(Helper);
        Pig Prey = Helper.spawn(EntityType.PIG, new Vec3(9.5, CellFloor, 9.5));
        watchCling(Helper, Heart, HoldLimit);
        mustDrop(Helper, Heart);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Heart.getTarget() == Prey || !Prey.isAlive(), "SCP-058 is not hunting the pig");
            Helper.assertTrue(hurt(Prey), "SCP-058 has not struck the pig");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp058_hunts_in_open_room", skyAccess = true)
    public static void huntsInOpenRoom(GameTestHelper Helper) {
        Scp058Heart Heart = heart(Helper, new Vec3(3.5, 2.0, 3.5));
        Pig Prey = Helper.spawn(EntityType.PIG, new Vec3(11.5, 2.0, 11.5));
        watchCling(Helper, Heart, HoldLimit);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Heart.getTarget() == Prey || !Prey.isAlive(), "SCP-058 is not hunting the pig");
            Helper.assertTrue(hurt(Prey), "SCP-058 has not struck the pig");
        });
    }

    @GameTest(template = Cell, timeoutTicks = 500, batch = "scp058_lets_go_of_the_ceiling")
    public static void letsGoOfTheCeiling(GameTestHelper Helper) {
        Scp058Heart Heart = heart(Helper, new Vec3(6.5, CellFloor, 8.5));
        Pig Prey = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, CellFloor, 12.5));
        Heart.setTarget(Prey);
        watchCling(Helper, Heart, HoldLimit);
        boolean[] Climbed = {false};
        Helper.onEachTick(() -> {
            if (Heart.isClimbing() && Heart.getY() > Helper.absoluteVec(new Vec3(0.0, CellFloor + 2.0, 0.0)).y) {
                Climbed[0] = true;
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Climbed[0], "SCP-058 never climbed toward the pig");
            Helper.assertTrue(Heart.onGround() && !Heart.isClimbing(), "SCP-058 has not let go yet");
            Helper.assertTrue(Heart.getTarget() == Prey, "SCP-058 dropped its target");
        });
    }
}
