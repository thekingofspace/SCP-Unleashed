package net.rai.scp_unleashed.scp439.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp439.Scp439Brute;
import net.rai.scp_unleashed.scp439.Scp439Food;
import net.rai.scp_unleashed.scp439.Scp439Hive;
import net.rai.scp_unleashed.scp439.Scp439Look;
import net.rai.scp_unleashed.scp439.Scp439Registry;
import net.rai.scp_unleashed.scp439.Scp439Worker;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp439WorkerFoodGameTests {
    public static final String Arena = "scp439_arena";
    public static final double Floor = 2.0;
    public static final int Settle = 300;
    public static final double Moved = 1.5;

    private Scp439WorkerFoodGameTests() {
    }

    private static Vec3 at(GameTestHelper Helper, double X, double Z) {
        return Helper.absoluteVec(new Vec3(X, Floor, Z));
    }

    private static Scp439Worker worker(GameTestHelper Helper, @Nullable Scp439Hive Nest, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        Scp439Worker Made = Scp439Registry.Worker.get().create(World);
        Helper.assertTrue(Made != null, "the worker could not be made");
        Vec3 Spot = at(Helper, X, Z);
        if (Nest != null) {
            Nest.hatch(World, Made);
        } else {
            Made.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
            Helper.assertTrue(World.addFreshEntity(Made), "the worker could not be placed");
        }
        Made.moveTo(Spot.x, Spot.y, Spot.z, -90.0f, 0.0f);
        return Made;
    }

    private static Scp439Hive nest(GameTestHelper Helper, double X, double Z) {
        Scp439Hive Made = Scp439Hive.grow(Helper.getLevel(), at(Helper, X, Z), 180.0f, Scp439Look.Blank, new CompoundTag(), Scp439Hive.FullTicks);
        Helper.assertTrue(Made != null, "an SCP-439-1 nest could not be placed");
        return Made;
    }

    private static ItemEntity drop(GameTestHelper Helper, Item Kind, int Count, double X, double Z) {
        Vec3 Spot = at(Helper, X, Z);
        ItemEntity Made = new ItemEntity(Helper.getLevel(), Spot.x, Spot.y, Spot.z, new ItemStack(Kind, Count));
        Made.setNoPickUpDelay();
        Made.setDeltaMovement(Vec3.ZERO);
        Helper.getLevel().addFreshEntity(Made);
        return Made;
    }

    private static void scatter(GameTestHelper Helper, Item Kind, Vec3 Where) {
        ItemEntity Made = new ItemEntity(Helper.getLevel(), Where.x, Where.y, Where.z, new ItemStack(Kind));
        Made.setDefaultPickUpDelay();
        Helper.getLevel().addFreshEntity(Made);
    }

    private static List<ItemEntity> lying(GameTestHelper Helper, Item Kind) {
        Vec3 Centre = at(Helper, 16.0, 16.0);
        return Helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(Centre, Centre).inflate(15.5, 6.0, 15.5), Found -> Found.isAlive() && Found.getItem().is(Kind));
    }

    private static void tidy(GameTestHelper Helper) {
        Vec3 Centre = at(Helper, 16.0, 16.0);
        for (Entity Found : Helper.getLevel().getEntities((Entity) null, new AABB(Centre, Centre).inflate(15.5, 6.0, 15.5), Found -> !(Found instanceof Player))) {
            Found.discard();
        }
    }

    private static void watchCarry(GameTestHelper Helper, Scp439Worker Worker, Set<Item> Carried) {
        Helper.onEachTick(() -> {
            if (Worker.isCarrying()) {
                Carried.add(Worker.carried().getItem());
            }
        });
    }

    private static void movesOnAfterKill(GameTestHelper Helper, boolean WithNest, EntityType<? extends Mob> Kind) {
        Scp439Hive Nest = WithNest ? nest(Helper, 6.5, 16.5) : null;
        Scp439Worker Worker = worker(Helper, Nest, 16.5, 16.5);
        Mob Prey = Helper.spawnWithNoFreeWill(Kind, new Vec3(19.5, Floor, 16.5));
        Prey.setHealth(1.0f);
        Worker.setTarget(Prey);
        Vec3[] Stood = new Vec3[1];
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(!Prey.isAlive(), "the worker has not killed its prey")).thenExecute(() -> Stood[0] = Worker.position())
                .thenWaitUntil(() -> {
                    Helper.assertTrue(Worker.getTarget() == null, "the worker still holds its dead prey as a target");
                    Helper.assertTrue(Worker.position().distanceTo(Stood[0]) >= Moved || Worker.isCarrying() || Nest != null && Nest.food() > 0,
                            "the worker froze after the kill");
                }).thenExecute(() -> tidy(Helper)).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = Settle, batch = "scp439_workerKillNest")
    public static void workerMovesOnAfterAlertedKill(GameTestHelper Helper) {
        movesOnAfterKill(Helper, true, EntityType.CHICKEN);
    }

    @GameTest(template = Arena, timeoutTicks = Settle, batch = "scp439_workerKillAlone")
    public static void loneWorkerMovesOnAfterAlertedKill(GameTestHelper Helper) {
        movesOnAfterKill(Helper, false, EntityType.VILLAGER);
    }

    private static void takesFoodLeavesRest(GameTestHelper Helper, EntityType<? extends Mob> Kind, Item Food, Item Rest) {
        Scp439Hive Nest = nest(Helper, 6.5, 16.5);
        Scp439Worker Worker = worker(Helper, Nest, 12.5, 16.5);
        Mob Prey = Helper.spawnWithNoFreeWill(Kind, new Vec3(16.5, Floor, 16.5));
        Prey.setHealth(1.0f);
        Set<Item> Carried = new HashSet<>();
        watchCarry(Helper, Worker, Carried);
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(!Prey.isAlive(), "the worker has not killed its prey")).thenExecute(() -> {
                    if (lying(Helper, Rest).isEmpty()) {
                        scatter(Helper, Rest, Prey.position());
                    }
                })
                .thenWaitUntil(() -> Helper.assertTrue(Nest.food() > 0 && !Worker.isCarrying(), "the worker has not brought the " + Food + " home"))
                .thenIdle(120).thenExecute(() -> {
                    Helper.assertTrue(Carried.contains(Food), "the worker never carried the " + Food);
                    Helper.assertFalse(Carried.contains(Rest), "the worker picked up " + Rest + ", which is not food");
                    Helper.assertTrue(!lying(Helper, Rest).isEmpty(), "the " + Rest + " vanished from the ground");
                    Helper.assertTrue(lying(Helper, Food).isEmpty(), "some " + Food + " was left behind");
                    tidy(Helper);
                }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "scp439_workerCow")
    public static void workerTakesBeefLeavesLeather(GameTestHelper Helper) {
        takesFoodLeavesRest(Helper, EntityType.COW, Items.BEEF, Items.LEATHER);
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "scp439_workerSheep")
    public static void workerTakesMuttonLeavesWool(GameTestHelper Helper) {
        takesFoodLeavesRest(Helper, EntityType.SHEEP, Items.MUTTON, Items.WHITE_WOOL);
    }

    @GameTest(template = Arena, timeoutTicks = Settle, batch = "scp439_workerEdgeFood")
    public static void workerReachesFoodAtTheFarEdgeOfABlock(GameTestHelper Helper) {
        Scp439Hive Nest = nest(Helper, 6.5, 16.5);
        Scp439Worker Worker = worker(Helper, Nest, 12.5, 16.5);
        ItemEntity Edge = drop(Helper, Items.BEEF, 1, 16.95, 16.5);
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(!Edge.isAlive() || Worker.isCarrying(), "the worker stopped short of the beef at the far edge of its block"))
                .thenExecute(() -> tidy(Helper)).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp439_workerRouteFood")
    public static void workerCollectsFoodOnTheWayHome(GameTestHelper Helper) {
        Scp439Hive Nest = nest(Helper, 4.5, 16.5);
        Scp439Worker Worker = worker(Helper, Nest, 20.5, 16.5);
        Worker.setCarried(new ItemStack(Items.BEEF, 1));
        ItemEntity OnRoute = drop(Helper, Items.BEEF, 1, 12.5, 16.5);
        List<Integer> Stores = new ArrayList<>();
        Helper.onEachTick(() -> {
            if (Stores.isEmpty() || Stores.get(Stores.size() - 1) != Nest.food()) {
                Stores.add(Nest.food());
            }
        });
        int Each = Scp439Food.each(new ItemStack(Items.BEEF));
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Nest.food() > 0, "nothing was delivered")).thenIdle(20).thenExecute(() -> {
            Helper.assertTrue(!OnRoute.isAlive() || OnRoute.getItem().isEmpty(), "the beef lying on the way home was left behind");
            Helper.assertTrue(Stores.size() >= 2 && Stores.get(1) == Each * 2, "the worker did not bring the beef from the route home with its load, stores went " + Stores);
            tidy(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp439_workerRouteJunk")
    public static void workerIgnoresNonFoodOnTheWay(GameTestHelper Helper) {
        Scp439Hive Nest = nest(Helper, 4.5, 16.5);
        Scp439Worker Worker = worker(Helper, Nest, 20.5, 16.5);
        Worker.setCarried(new ItemStack(Items.BEEF, 1));
        ItemEntity Leather = drop(Helper, Items.LEATHER, 1, 13.5, 16.5);
        ItemEntity Wool = drop(Helper, Items.WHITE_WOOL, 1, 10.5, 16.5);
        Set<Item> Carried = new HashSet<>();
        watchCarry(Helper, Worker, Carried);
        int Each = Scp439Food.each(new ItemStack(Items.BEEF));
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Nest.food() > 0, "nothing was delivered")).thenIdle(200).thenExecute(() -> {
            Helper.assertTrue(Nest.food() == Each, "the stores hold " + Nest.food() + " instead of one beef");
            Helper.assertTrue(Leather.isAlive() && Wool.isAlive(), "the worker took leather or wool");
            Helper.assertFalse(Carried.contains(Items.LEATHER) || Carried.contains(Items.WHITE_WOOL), "the worker carried something that is not food");
            tidy(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = Settle, batch = "scp439_bruteKill")
    public static void bruteMovesOnAfterAlertedKill(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp439Brute Brute = Scp439Registry.Brute.get().create(World);
        Helper.assertTrue(Brute != null, "the brute could not be made");
        Vec3 Spot = at(Helper, 16.5, 16.5);
        Brute.moveTo(Spot.x, Spot.y, Spot.z, -90.0f, 0.0f);
        Helper.assertTrue(World.addFreshEntity(Brute), "the brute could not be placed");
        Mob Prey = Helper.spawnWithNoFreeWill(EntityType.CHICKEN, new Vec3(19.5, Floor, 16.5));
        Prey.setHealth(1.0f);
        Brute.setTarget(Prey);
        Vec3[] Stood = new Vec3[1];
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(!Prey.isAlive(), "the brute has not killed its prey")).thenExecute(() -> Stood[0] = Brute.position())
                .thenWaitUntil(() -> {
                    Helper.assertTrue(Brute.getTarget() == null, "the brute still holds its dead prey as a target");
                    Helper.assertTrue(Brute.position().distanceTo(Stood[0]) >= Moved, "the brute froze after the kill");
                }).thenExecute(() -> tidy(Helper)).thenSucceed();
    }
}
