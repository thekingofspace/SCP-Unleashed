package net.rai.scp_unleashed.scp354.beasts.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.scp354.beasts.Scp354Beast;
import net.rai.scp_unleashed.scp354.beasts.Scp354BeastRegistry;
import net.rai.scp_unleashed.scp354.beasts.Scp354Brute;
import net.rai.scp_unleashed.scp354.beasts.Scp354Charger;
import net.rai.scp_unleashed.scp354.beasts.Scp354Crawler;
import net.rai.scp_unleashed.scp354.beasts.Scp354Leaper;
import net.rai.scp_unleashed.scp354.beasts.Scp354Wingbeast;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp354BeastGameTests {
    public static final String Arena = Scp_unleashed.ModId + ":scp354b_arena";
    public static final int WallZ = 23;
    public static final double Floor = 2.0;

    private record Kind(String Name, Supplier<? extends EntityType<? extends Scp354Beast>> Type, Supplier<? extends EntityType<? extends Scp354Beast>> Kin,
                        BiConsumer<GameTestHelper, Kind> Signature, int SignatureTicks) {
    }

    private Scp354BeastGameTests() {
    }

    private static List<Kind> kinds() {
        return List.of(new Kind("brute", Scp354BeastRegistry.Brute, Scp354BeastRegistry.Crawler, Scp354BeastGameTests::pound, 120),
                new Kind("crawler", Scp354BeastRegistry.Crawler, Scp354BeastRegistry.Leaper, Scp354BeastGameTests::climb, 200),
                new Kind("wingbeast", Scp354BeastRegistry.Wingbeast, Scp354BeastRegistry.Charger, Scp354BeastGameTests::swoop, 160),
                new Kind("leaper", Scp354BeastRegistry.Leaper, Scp354BeastRegistry.Brute, Scp354BeastGameTests::yank, 120),
                new Kind("charger", Scp354BeastRegistry.Charger, Scp354BeastRegistry.Wingbeast, Scp354BeastGameTests::charge, 200));
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> Tests = new ArrayList<>();
        for (Kind Entry : kinds()) {
            Tests.add(test(Entry, "targetsclosest", 160, Helper -> targetsClosest(Helper, Entry)));
            Tests.add(test(Entry, "ignoreskin", 140, Helper -> ignoresKin(Helper, Entry)));
            Tests.add(test(Entry, "signature", Entry.SignatureTicks(), Helper -> Entry.Signature().accept(Helper, Entry)));
            Tests.add(test(Entry, "emerge", 140, Helper -> emerge(Helper, Entry)));
            Tests.add(test(Entry, "stasis", 60, Helper -> stasis(Helper, Entry)));
        }
        return Tests;
    }

    private static TestFunction test(Kind Entry, String Label, int Ticks, Consumer<GameTestHelper> Body) {
        String Name = "scp354b_" + Entry.Name() + "_" + Label;
        return new TestFunction(Name, Name, Arena, Ticks, 0L, true, Body);
    }

    private static <T extends Scp354Beast> T spawn(GameTestHelper Helper, EntityType<T> Type, double X, double Y, double Z, float Yaw) {
        T Creature = Helper.spawn(Type, new Vec3(X, Y, Z));
        Creature.setYRot(Yaw);
        Creature.yBodyRot = Yaw;
        Creature.yHeadRot = Yaw;
        return Creature;
    }

    private static Scp354Beast spawnKind(GameTestHelper Helper, Kind Entry, double X, double Y, double Z, float Yaw) {
        return spawn(Helper, Entry.Type().get(), X, Y, Z, Yaw);
    }

    private static Villager villager(GameTestHelper Helper, double X, double Z) {
        return Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(X, Floor, Z));
    }

    private static ServerPlayer survivor(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp354b_test"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    private static void tidy(GameTestHelper Helper, ServerPlayer... Players) {
        for (ServerPlayer User : Players) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
        Helper.killAllEntitiesOfClass(Scp354Beast.class);
        Helper.killAllEntitiesOfClass(Villager.class);
    }

    private static String state(Scp354Beast Creature) {
        return " [target " + (Creature.getTarget() == null ? "none" : Creature.getTarget().getName().getString()) + ", action " + Creature.action() + ", health "
                + Creature.getHealth() + ", at " + Creature.position() + "]";
    }

    private static void targetsClosest(GameTestHelper Helper, Kind Entry) {
        Scp354Beast Creature = spawnKind(Helper, Entry, 6.5, Floor, 6.5, 0.0f);
        Villager Far = villager(Helper, 6.5, 17.5);
        ServerPlayer Near = survivor(Helper, 10.5, 9.5);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Creature.getTarget() == Near, "the closest person was not chosen" + state(Creature));
            Helper.assertTrue(Far.isAlive(), "the farther villager was lost before the check");
            tidy(Helper, Near);
        });
    }

    private static void ignoresKin(GameTestHelper Helper, Kind Entry) {
        Scp354Beast Creature = spawnKind(Helper, Entry, 8.5, Floor, 8.5, 0.0f);
        Scp354Beast Kin = spawn(Helper, Entry.Kin().get(), 12.5, Floor, 12.5, 180.0f);
        Creature.setTarget(Kin);
        Helper.assertTrue(Creature.getTarget() == null, "a pool-born creature accepted another as its target");
        Helper.assertTrue(!Creature.canAttack(Kin), "a pool-born creature may attack another");
        Helper.runAfterDelay(100, () -> {
            Helper.assertTrue(Creature.getTarget() == null && Kin.getTarget() == null, "pool-born creatures turned on each other" + state(Creature) + state(Kin));
            Helper.assertTrue(Kin.getHealth() >= Kin.getMaxHealth() && Creature.getHealth() >= Creature.getMaxHealth(), "pool-born creatures were hurt by each other");
            tidy(Helper);
            Helper.succeed();
        });
    }

    private static void pound(GameTestHelper Helper, Kind Entry) {
        Scp354Brute Brute = (Scp354Brute) spawnKind(Helper, Entry, 11.5, Floor, 6.5, 0.0f);
        Villager Victim = villager(Helper, 11.5, 10.0);
        Villager Bystander = villager(Helper, 14.0, 8.5);
        Scp354Beast Kin = spawn(Helper, Entry.Kin().get(), 8.0, Floor, 9.5, 0.0f);
        Kin.setNoAi(true);
        Brute.setTarget(Victim);
        Helper.runAfterDelay(2, () -> Brute.beginPound(Victim));
        double[] Peak = {Victim.getY()};
        Helper.onEachTick(() -> Peak[0] = Math.max(Peak[0], Victim.getY()));
        Helper.runAfterDelay(2 + Scp354Brute.PoundHitTick + 6, () -> {
            Helper.assertTrue(Victim.getHealth() < Victim.getMaxHealth(), "the pound did not hurt the villager in front" + state(Brute));
            Helper.assertTrue(Bystander.getHealth() < Bystander.getMaxHealth(), "the shockwave did not reach the villager beside it");
            Helper.assertTrue(Peak[0] > Helper.absoluteVec(new Vec3(0.0, Floor, 0.0)).y + 0.25, "the shockwave did not throw the villager up");
            Helper.assertTrue(Kin.getHealth() >= Kin.getMaxHealth(), "the shockwave hurt another pool-born creature");
            tidy(Helper);
            Helper.succeed();
        });
    }

    private static void climb(GameTestHelper Helper, Kind Entry) {
        Scp354Crawler Crawler = (Scp354Crawler) spawnKind(Helper, Entry, 11.5, Floor, 18.5, 0.0f);
        double Start = Crawler.getY();
        boolean[] Climbed = {false};
        BlockPos Wall = new BlockPos(11, 2, WallZ);
        Vec3 Top = Helper.absoluteVec(new Vec3(11.5, 10.0, WallZ + 1.5));
        Helper.onEachTick(() -> {
            if (Crawler.isAlive()) {
                Crawler.getNavigation().moveTo(Top.x, Top.y, Top.z, 1.0);
                Climbed[0] |= Crawler.isClimbing();
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Climbed[0], "the crawler never gripped the wall" + state(Crawler));
            Helper.assertTrue(Crawler.getY() > Start + 2.5, "the crawler did not climb the wall" + state(Crawler));
            Helper.assertBlockPresent(Blocks.STONE_BRICKS, Wall);
            tidy(Helper);
        });
    }

    private static void swoop(GameTestHelper Helper, Kind Entry) {
        Scp354Wingbeast Flyer = (Scp354Wingbeast) spawnKind(Helper, Entry, 11.5, Floor + 4.5, 6.5, 0.0f);
        Villager Victim = villager(Helper, 11.5, 12.5);
        Flyer.setTarget(Victim);
        Helper.runAfterDelay(2, () -> Flyer.beginSwoop(Victim));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Victim.getHealth() < Victim.getMaxHealth(), "the swoop never reached the villager" + state(Flyer));
            tidy(Helper);
        });
    }

    private static void yank(GameTestHelper Helper, Kind Entry) {
        Scp354Leaper Leaper = (Scp354Leaper) spawnKind(Helper, Entry, 11.5, Floor, 4.5, 0.0f);
        Villager Victim = villager(Helper, 11.5, 11.5);
        double Before = Leaper.distanceTo(Victim);
        Leaper.setTarget(Victim);
        Helper.runAfterDelay(2, () -> Leaper.beginTongue(Victim));
        Helper.runAfterDelay(2 + Scp354Leaper.TongueHitTick + 14, () -> {
            Helper.assertTrue(Victim.getHealth() < Victim.getMaxHealth(), "the tongue did not strike the villager" + state(Leaper));
            Helper.assertTrue(Leaper.distanceTo(Victim) < Before - 2.5, "the tongue did not yank the villager in (" + Before + " -> " + Leaper.distanceTo(Victim) + ")");
            tidy(Helper);
            Helper.succeed();
        });
    }

    private static void charge(GameTestHelper Helper, Kind Entry) {
        Scp354Charger Charger = (Scp354Charger) spawnKind(Helper, Entry, 11.5, Floor, 6.5, 0.0f);
        Villager Victim = villager(Helper, 11.5, 12.5);
        BlockPos Wall = new BlockPos(11, 2, WallZ);
        boolean[] Stunned = {false};
        Helper.runAfterDelay(2, () -> Charger.beginCharge(0.0f, null));
        Helper.onEachTick(() -> Stunned[0] |= Charger.action() == Scp354Charger.ActionStunned);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Victim.getHealth() < Victim.getMaxHealth(), "the charge did not hit the villager in its path" + state(Charger));
            Helper.assertTrue(Stunned[0], "the charger was not stunned by the wall" + state(Charger));
            Helper.assertBlockPresent(Blocks.STONE_BRICKS, Wall);
            Helper.assertBlockPresent(Blocks.STONE_BRICKS, Wall.above());
            tidy(Helper);
        });
    }

    private static void emerge(GameTestHelper Helper, Kind Entry) {
        Scp354Beast Creature = spawnKind(Helper, Entry, 11.5, Floor, 9.5, 0.0f);
        Villager Neighbour = villager(Helper, 11.5, 11.5);
        Creature.beginEmerge();
        Helper.assertTrue(Creature.isEmerging() && Creature.isPoolBornFlag(), "beginEmerge did not start the emergence");
        float Health = Creature.getHealth();
        Helper.assertTrue(!Creature.hurt(Helper.getLevel().damageSources().generic(), 40.0f), "the creature took damage while emerging");
        Helper.assertTrue(!Creature.hurt(Helper.getLevel().damageSources().playerAttack(survivorFor(Helper)), 40.0f), "a player hurt the creature while it emerged");
        Helper.assertTrue(Creature.getHealth() >= Health, "health dropped while emerging");
        Helper.runAfterDelay(Scp354Beast.EmergeTicks - 6, () -> {
            Helper.assertTrue(Creature.isEmerging(), "the emergence ended too early");
            Helper.assertTrue(Neighbour.getHealth() >= Neighbour.getMaxHealth(), "the creature attacked while still emerging" + state(Creature));
            Helper.assertTrue(Creature.action() == Scp354Beast.ActionEmerge, "the creature began an attack while emerging");
        });
        Helper.runAfterDelay(Scp354Beast.EmergeTicks + 4, () -> {
            Helper.assertTrue(!Creature.isEmerging(), "the emergence never ended");
            Helper.assertTrue(Creature.hurt(Helper.getLevel().damageSources().generic(), 2.0f), "the creature stayed invulnerable after emerging");
            tidy(Helper);
            Helper.succeed();
        });
    }

    private static ServerPlayer survivorFor(GameTestHelper Helper) {
        ServerPlayer User = survivor(Helper, 3.5, 3.5);
        Helper.runAfterDelay(1, () -> Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED));
        return User;
    }

    private static void stasis(GameTestHelper Helper, Kind Entry) {
        ServerLevel World = Helper.getLevel();
        Scp354Beast Creature = spawnKind(Helper, Entry, 11.5, Floor, 11.5, 0.0f);
        ServerPlayer User = survivor(Helper, 4.5, 4.5);
        ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
        Helper.assertTrue(StasisPods.refusal(User, Pod, Creature) == null, "a stasis pod refuses the creature");
        Helper.assertTrue(StasisPods.capture(World, User, Pod, Creature), "the stasis pod failed to capture the creature");
        Helper.assertTrue(Creature.isRemoved() && StasisPodItem.isOccupied(Pod), "the pod does not hold the creature");
        Helper.assertTrue(StasisPods.breakOut(World, Pod, Helper.absoluteVec(new Vec3(11.5, Floor, 11.5)), null), "the creature did not come back out of the pod");
        Helper.runAfterDelay(2, () -> {
            List<? extends Mob> Released = Helper.getEntities(Entry.Type().get());
            Helper.assertTrue(!Released.isEmpty(), "no released creature in the arena");
            Helper.assertTrue(Released.get(0).isAlive(), "the released creature is not alive");
            tidy(Helper, User);
            Helper.succeed();
        });
    }
}
