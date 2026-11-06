package net.rai.scp_unleashed.scp173.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassCrime;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.dclass.DClassTrait;
import net.rai.scp_unleashed.dclass.duty.DClassDuties;
import net.rai.scp_unleashed.dclass.duty.DClassGaze;
import net.rai.scp_unleashed.dclass.duty.DClassRole;
import net.rai.scp_unleashed.mtf.MtfOperative;
import net.rai.scp_unleashed.mtf.MtfRegistry;
import net.rai.scp_unleashed.scp173.Scp173Registry;
import net.rai.scp_unleashed.scp173.Scp173Sight;
import net.rai.scp_unleashed.scp173.Scp173Statue;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp173SightGameTests {
    public static final String Arena = "dclassroles_arena";
    public static final double Floor = 2.0;
    public static final int Ground = 2;
    public static final int Held = 100000;
    public static final int HoldTicks = 60;
    public static final int OperativeHold = 30;
    public static final int Settle = 5;
    public static final double Still = 1.0e-3;
    public static final double OffAxis = 58.0;
    public static final double NearHeight = 22.0;
    public static final double FarHeight = 40.0;

    private Scp173SightGameTests() {
    }

    private static void quiet(GameTestHelper Helper) {
        Helper.onEachTick(() -> {
            for (Monster Stray : Helper.getLevel().getEntitiesOfClass(Monster.class, Helper.getBounds().inflate(8.0))) {
                if ("minecraft".equals(EntityType.getKey(Stray.getType()).getNamespace())) {
                    Stray.discard();
                }
            }
        });
    }

    private static DClass person(GameTestHelper Helper, double X, double Z, boolean Thinking, DClassTrait Trait) {
        return person(Helper, new Vec3(X, Floor, Z), Thinking, Trait);
    }

    private static DClass person(GameTestHelper Helper, Vec3 Spot, boolean Thinking, DClassTrait Trait) {
        DClass Made = Thinking ? Helper.spawn(DClassRegistry.DClassType.get(), Spot) : Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), Spot);
        Made.identify(Trait, DClassCrime.Smuggling, 0);
        Made.assignNumber();
        return Made;
    }

    private static DClass person(GameTestHelper Helper, double X, double Z) {
        return person(Helper, X, Z, false, DClassTrait.Stoic);
    }

    private static MtfOperative operative(GameTestHelper Helper, double X, double Z) {
        MtfOperative Made = Helper.spawnWithNoFreeWill(MtfRegistry.OperativeType.get(), new Vec3(X, Floor, Z));
        Made.setState(MtfOperative.StateActive);
        return Made;
    }

    private static Scp173Statue statue(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        Scp173Statue Made = Scp173Registry.Statue.get().create(World);
        if (Made == null) {
            throw new IllegalStateException("SCP-173 could not be created");
        }
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        Made.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addFreshEntity(Made);
        return Made;
    }

    private static void face(LivingEntity Body, Vec3 Target) {
        Vec3 Eye = Body.getEyePosition();
        double Dx = Target.x - Eye.x;
        double Dy = Target.y - Eye.y;
        double Dz = Target.z - Eye.z;
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        float Pitch = (float) -(Mth.atan2(Dy, Math.sqrt(Dx * Dx + Dz * Dz)) * Mth.RAD_TO_DEG);
        Body.setYRot(Yaw);
        Body.yRotO = Yaw;
        Body.setYHeadRot(Yaw);
        Body.yHeadRotO = Yaw;
        Body.setYBodyRot(Yaw);
        Body.yBodyRotO = Yaw;
        Body.setXRot(Pitch);
        Body.xRotO = Pitch;
    }

    private static void faceAway(LivingEntity Body, Vec3 From) {
        Vec3 Eye = Body.getEyePosition();
        face(Body, new Vec3(Eye.x * 2.0 - From.x, Eye.y, Eye.z * 2.0 - From.z));
    }

    private static Vec3 sightOf(Scp173Statue Statue) {
        return Statue.sightBox().getCenter();
    }

    private static void wall(GameTestHelper Helper, int Z, int Height, Block Kind) {
        for (int X = 1; X <= 22; X++) {
            for (int Y = Ground; Y < Ground + Height; Y++) {
                Helper.setBlock(new BlockPos(X, Y, Z), Kind);
            }
        }
    }

    private static void box(GameTestHelper Helper, int Cx, int Cz) {
        for (int X = Cx - 1; X <= Cx + 1; X++) {
            for (int Z = Cz - 1; Z <= Cz + 1; Z++) {
                for (int Y = Ground; Y <= Ground + 2; Y++) {
                    if (X != Cx || Z != Cz || Y == Ground + 2) {
                        Helper.setBlock(new BlockPos(X, Y, Z), Blocks.STONE);
                    }
                }
            }
        }
    }

    private static void tidy(List<? extends Entity> Bodies) {
        for (Entity Body : Bodies) {
            if (!Body.isRemoved()) {
                Body.discard();
            }
        }
    }

    private static String where(Scp173Statue Statue, Vec3 Start) {
        return " (moved " + String.format("%.2f", Statue.position().distanceTo(Start)) + ", target " + Statue.hunt().target() + ")";
    }

    private static String fate(LivingEntity Victim) {
        return " [removed " + Victim.getRemovalReason() + ", health " + Victim.getHealth() + ", last damage " + Victim.getLastDamageSource() + "]";
    }

    private static void killed(GameTestHelper Helper, LivingEntity Victim, String Why) {
        Helper.assertTrue(!Victim.isAlive(), Why);
        Helper.assertTrue(Victim.isDeadOrDying(), Victim.getName().getString() + " vanished instead of being killed" + fate(Victim));
    }

    private static void held(GameTestHelper Helper, Scp173Statue Statue, Vec3 Start, LivingEntity Victim, String Why) {
        Helper.assertTrue(Victim.isAlive(), "SCP-173 killed " + Victim.getName().getString() + " while " + Why + where(Statue, Start) + fate(Victim)
                + " observed " + Scp173Sight.isObserved(Helper.getLevel(), Statue) + " by " + Scp173Sight.personObservers(Helper.getLevel(), Statue));
        Helper.assertTrue(Statue.position().distanceTo(Start) < Still, "SCP-173 moved while " + Why + where(Statue, Start));
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "s173sight_dclassAway")
    public static void dclassFacingAwayIsKilled(GameTestHelper Helper) {
        quiet(Helper);
        Scp173Statue Statue = statue(Helper, 12.5, 16.5);
        DClass Victim = person(Helper, 12.5, 11.5);
        faceAway(Victim, sightOf(Statue));
        DClassGaze.holdEyes(Victim, Held);
        Vec3 Start = Statue.position();
        Helper.runAtTickTime(2, () -> Helper.assertTrue(Scp173Sight.personObservers(Helper.getLevel(), Statue).isEmpty(), "a D-Class facing away counts as watching SCP-173"));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(!Victim.isAlive(), "SCP-173 has not killed the D-Class facing away" + where(Statue, Start)))
                .thenExecute(() -> Helper.assertTrue(Statue.position().distanceTo(Start) > 0.5, "SCP-173 killed without moving"))
                .thenExecute(() -> tidy(List.of(Statue, Victim)))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "s173sight_offAxis")
    public static void dclassOutsideViewIsKilled(GameTestHelper Helper) {
        quiet(Helper);
        Scp173Statue Statue = statue(Helper, 12.5, 12.5);
        DClass Victim = person(Helper, 12.5, 6.5);
        DClass Glancer = person(Helper, 4.5, 12.5);
        faceAway(Victim, sightOf(Statue));
        Vec3 Toward = sightOf(Statue).subtract(Glancer.getEyePosition());
        double Turn = Math.toRadians(OffAxis);
        Vec3 Turned = new Vec3(Toward.x * Math.cos(Turn) - Toward.z * Math.sin(Turn), Toward.y, Toward.x * Math.sin(Turn) + Toward.z * Math.cos(Turn));
        face(Glancer, Glancer.getEyePosition().add(Turned));
        DClassGaze.holdEyes(Victim, Held);
        DClassGaze.holdEyes(Glancer, Held);
        Vec3 Start = Statue.position();
        Helper.runAtTickTime(2, () -> Helper.assertTrue(!Scp173Sight.personObservers(Helper.getLevel(), Statue).contains(Glancer), "a D-Class looking " + OffAxis + " degrees away counts as watching SCP-173"));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(!Victim.isAlive(), "SCP-173 stayed frozen for a D-Class that had it outside its view" + where(Statue, Start)))
                .thenExecute(() -> tidy(List.of(Statue, Victim, Glancer)))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "s173sight_dclassHolds")
    public static void dclassFacingHolds173(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        Scp173Statue Statue = statue(Helper, 12.5, 6.5);
        DClass Victim = person(Helper, 12.5, 9.0);
        DClass Watcher = person(Helper, 17.5, 15.5);
        faceAway(Victim, sightOf(Statue));
        face(Watcher, sightOf(Statue));
        DClassGaze.holdEyes(Watcher, Held);
        Vec3 Start = Statue.position();
        Helper.startSequence()
                .thenExecuteFor(HoldTicks, () -> held(Helper, Statue, Start, Victim, "a D-Class watched it with a clear view"))
                .thenExecute(() -> {
                    Helper.assertTrue(Scp173Sight.isObserved(World, Statue), "SCP-173 is not observed by the D-Class facing it");
                    Helper.assertTrue(Scp173Sight.personObservers(World, Statue).contains(Watcher), "the D-Class facing SCP-173 is not listed as an observer");
                    tidy(List.of(Statue, Victim, Watcher));
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "s173sight_wall")
    public static void wallBreaksTheGaze(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        Scp173Statue Statue = statue(Helper, 12.5, 6.5);
        DClass Victim = person(Helper, 12.5, 3.5);
        DClass Watcher = person(Helper, 12.5, 14.5);
        faceAway(Victim, sightOf(Statue));
        face(Watcher, sightOf(Statue));
        DClassGaze.holdEyes(Watcher, Held);
        DClassGaze.holdEyes(Victim, Held);
        Vec3 Start = Statue.position();
        Helper.startSequence()
                .thenExecuteFor(40, () -> held(Helper, Statue, Start, Victim, "a D-Class watched it before the wall went up"))
                .thenExecute(() -> wall(Helper, 10, 4, Blocks.STONE_BRICKS))
                .thenExecuteAfter(Settle, () -> {
                    Helper.assertTrue(!Scp173Sight.personObservers(World, Statue).contains(Watcher), "a D-Class behind a full wall still counts as watching SCP-173");
                    Helper.assertTrue(!Scp173Sight.isObserved(World, Statue), "SCP-173 is still observed through a full wall");
                })
                .thenWaitUntil(() -> Helper.assertTrue(!Victim.isAlive(), "SCP-173 stayed frozen although its only watcher was behind a wall" + where(Statue, Start)))
                .thenExecute(() -> {
                    Helper.assertTrue(Statue.position().distanceTo(Start) > 0.5, "SCP-173 killed without moving");
                    tidy(List.of(Statue, Victim, Watcher));
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "s173sight_operative")
    public static void operativeHoldsUntilTurnedAway(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        Scp173Statue Statue = statue(Helper, 12.5, 8.5);
        MtfOperative Operative = operative(Helper, 12.5, 12.5);
        face(Operative, sightOf(Statue));
        DClassGaze.holdEyes(Operative, Held);
        Vec3 Start = Statue.position();
        Helper.startSequence()
                .thenExecuteFor(OperativeHold, () -> held(Helper, Statue, Start, Operative, "an MTF operative watched it"))
                .thenExecute(() -> {
                    Helper.assertTrue(Scp173Sight.personObservers(World, Statue).contains(Operative), "the MTF operative facing SCP-173 is not listed as an observer");
                    faceAway(Operative, sightOf(Statue));
                    Helper.assertTrue(!Scp173Sight.isObserved(World, Statue), "SCP-173 is still observed after the operative turned away");
                })
                .thenWaitUntil(() -> killed(Helper, Operative, "SCP-173 has not killed the operative who turned away" + where(Statue, Start)))
                .thenExecute(() -> {
                    Helper.assertTrue(Statue.position().distanceTo(Start) > 0.5, "SCP-173 killed the operative without moving");
                    tidy(List.of(Statue, Operative));
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "s173sight_glass")
    public static void glassFollowsThePlayerRule(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        wall(Helper, 10, 4, Blocks.GLASS);
        Scp173Statue Statue = statue(Helper, 12.5, 6.5);
        DClass Victim = person(Helper, 12.5, 3.5);
        DClass Watcher = person(Helper, 12.5, 14.5);
        faceAway(Victim, sightOf(Statue));
        face(Watcher, sightOf(Statue));
        DClassGaze.holdEyes(Watcher, Held);
        Vec3 Start = Statue.position();
        Helper.startSequence()
                .thenExecuteFor(HoldTicks, () -> held(Helper, Statue, Start, Victim, "a D-Class watched it through glass"))
                .thenExecute(() -> {
                    boolean PlayerSees = Scp173Sight.clear(World, Watcher.getEyePosition(), sightOf(Statue));
                    Helper.assertTrue(PlayerSees, "the player sight ray does not pass through glass");
                    Helper.assertTrue(Scp173Sight.personObservers(World, Statue).contains(Watcher) == PlayerSees, "a D-Class and a player disagree about seeing through glass");
                    tidy(List.of(Statue, Victim, Watcher));
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "s173sight_range", skyAccess = true)
    public static void outOfRangeDoesNotHold(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        Scp173Statue Statue = statue(Helper, 12.5, 12.5);
        DClass Victim = person(Helper, 12.5, 9.5);
        DClass Watcher = person(Helper, new Vec3(12.5, Floor + NearHeight, 12.5), false, DClassTrait.Stoic);
        Watcher.setNoGravity(true);
        faceAway(Victim, sightOf(Statue));
        DClassGaze.holdEyes(Watcher, Held);
        DClassGaze.holdEyes(Victim, Held);
        double[] Height = {NearHeight};
        Helper.onEachTick(() -> {
            if (Watcher.isAlive()) {
                Vec3 Above = Helper.absoluteVec(new Vec3(12.5, Floor + Height[0], 12.5));
                Watcher.moveTo(Above.x, Above.y, Above.z);
                Watcher.setDeltaMovement(Vec3.ZERO);
                face(Watcher, sightOf(Statue));
            }
        });
        Vec3 Start = Statue.position();
        Helper.startSequence()
                .thenExecuteAfter(2, () -> Helper.assertTrue(Scp173Sight.personObservers(World, Statue).contains(Watcher), "a D-Class looking down from " + NearHeight + " blocks does not see SCP-173"))
                .thenExecuteFor(40, () -> held(Helper, Statue, Start, Victim, "a D-Class watched it from within range"))
                .thenExecute(() -> Height[0] = FarHeight)
                .thenExecuteAfter(2, () -> Helper.assertTrue(!Scp173Sight.isObserved(World, Statue), "SCP-173 is still observed by a D-Class " + FarHeight + " blocks away"))
                .thenWaitUntil(() -> Helper.assertTrue(!Victim.isAlive(), "SCP-173 stayed frozen for a watcher out of range" + where(Statue, Start)))
                .thenExecute(() -> tidy(List.of(Statue, Victim, Watcher)))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "s173sight_darkness")
    public static void darknessShortensSight(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        Scp173Statue Statue = statue(Helper, 12.5, 20.5);
        DClass Victim = person(Helper, 12.5, 22.5);
        DClass Watcher = person(Helper, 12.5, 1.5);
        faceAway(Victim, sightOf(Statue));
        face(Watcher, sightOf(Statue));
        DClassGaze.holdEyes(Watcher, Held);
        DClassGaze.holdEyes(Victim, Held);
        Vec3 Start = Statue.position();
        Helper.startSequence()
                .thenExecuteFor(40, () -> held(Helper, Statue, Start, Victim, "a D-Class watched it from 19 blocks in plain light"))
                .thenExecute(() -> {
                    Watcher.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 2000, 0, false, false));
                    Helper.assertTrue(!Scp173Sight.isObserved(World, Statue), "a D-Class under Darkness still sees SCP-173 19 blocks away");
                })
                .thenWaitUntil(() -> Helper.assertTrue(!Victim.isAlive(), "SCP-173 stayed frozen for a watcher whose sight Darkness cut short" + where(Statue, Start)))
                .thenExecute(() -> tidy(List.of(Statue, Victim, Watcher)))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "s173sight_mixed")
    public static void mixedWatchersHoldUntilTheWatcherLooksAway(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        Scp173Statue Statue = statue(Helper, 12.5, 12.5);
        DClass Watcher = person(Helper, 12.5, 20.5);
        MtfOperative Operative = operative(Helper, 15.5, 12.5);
        DClass Prisoner = person(Helper, 12.5, 7.5);
        face(Watcher, sightOf(Statue));
        faceAway(Operative, sightOf(Statue));
        faceAway(Prisoner, sightOf(Statue));
        DClassGaze.holdEyes(Watcher, Held);
        DClassGaze.holdEyes(Operative, Held);
        DClassGaze.holdEyes(Prisoner, Held);
        Vec3 Start = Statue.position();
        Helper.startSequence()
                .thenExecuteFor(OperativeHold, () -> {
                    held(Helper, Statue, Start, Prisoner, "one person out of three watched it");
                    held(Helper, Statue, Start, Operative, "one person out of three watched it");
                })
                .thenExecute(() -> {
                    List<LivingEntity> Seen = Scp173Sight.personObservers(World, Statue);
                    Helper.assertTrue(Seen.size() == 1 && Seen.contains(Watcher), "the observers should be only the D-Class facing SCP-173 but were " + Seen);
                    faceAway(Watcher, sightOf(Statue));
                })
                .thenWaitUntil(() -> killed(Helper, Operative, "SCP-173 has not killed the unwatched operative" + where(Statue, Start)))
                .thenWaitUntil(() -> killed(Helper, Prisoner, "SCP-173 has not killed the unwatched prisoner" + where(Statue, Start)))
                .thenExecute(() -> tidy(List.of(Statue, Watcher, Prisoner, Operative)))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "s173sight_janitorAway")
    public static void janitorFacingAwayIsKilled(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        Scp173Statue Statue = statue(Helper, 12.5, 4.5);
        DClass Janitor = person(Helper, 12.5, 12.5, true, DClassTrait.Insomniac);
        Helper.assertTrue(DClassDuties.assignRole(Janitor, DClassRole.Janitor), "the D-Class refused the janitor role");
        faceAway(Janitor, sightOf(Statue));
        DClassGaze.holdEyes(Janitor, Held);
        Vec3 Start = Statue.position();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(!Janitor.isAlive(), "SCP-173 has not killed the janitor with its back turned, observers " + Scp173Sight.personObservers(World, Statue)
                        + " head " + Math.round(Janitor.getYHeadRot()) + where(Statue, Start)))
                .thenExecute(() -> tidy(List.of(Statue, Janitor)))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "s173sight_janitorWall")
    public static void janitorLosesSightBehindAWall(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        Scp173Statue Statue = statue(Helper, 12.5, 6.5);
        DClass Victim = person(Helper, 12.5, 3.5);
        DClass Janitor = person(Helper, 12.5, 14.5, true, DClassTrait.Insomniac);
        Helper.assertTrue(DClassDuties.assignRole(Janitor, DClassRole.Janitor), "the D-Class refused the janitor role");
        faceAway(Victim, sightOf(Statue));
        face(Janitor, sightOf(Statue));
        DClassGaze.holdEyes(Janitor, Held);
        DClassGaze.holdEyes(Victim, Held);
        Vec3 Start = Statue.position();
        Helper.startSequence()
                .thenExecuteFor(80, () -> held(Helper, Statue, Start, Victim, "a janitor kept its eyes on it"))
                .thenExecute(() -> {
                    Helper.assertTrue(DClassGaze.state(Janitor).watching(), "the janitor facing SCP-173 does not know it is watching it");
                    wall(Helper, 10, 4, Blocks.STONE_BRICKS);
                })
                .thenWaitUntil(() -> Helper.assertTrue(!Victim.isAlive(), "SCP-173 stayed frozen although the janitor was walled off" + where(Statue, Start)))
                .thenExecuteAfter(DClassGaze.WatchCheck + 1, () -> Helper.assertTrue(!DClassGaze.state(Janitor).watching() || !Janitor.isAlive(), "the walled off janitor still believes it watches SCP-173"))
                .thenExecute(() -> tidy(List.of(Statue, Victim, Janitor)))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "s173sight_unreachable")
    public static void huntSkipsUnreachablePrey(GameTestHelper Helper) {
        quiet(Helper);
        Scp173Statue Statue = statue(Helper, 12.5, 12.5);
        List<Entity> Bodies = new ArrayList<>();
        Bodies.add(Statue);
        int[][] Cells = {{8, 12}, {16, 12}, {12, 8}};
        for (int[] Cell : Cells) {
            box(Helper, Cell[0], Cell[1]);
            DClass Boxed = person(Helper, Cell[0] + 0.5, Cell[1] + 0.5);
            Bodies.add(Boxed);
        }
        DClass Reachable = person(Helper, 12.5, 21.5);
        faceAway(Reachable, sightOf(Statue));
        DClassGaze.holdEyes(Reachable, Held);
        Bodies.add(Reachable);
        Vec3 Start = Statue.position();
        Helper.startSequence()
                .thenWaitUntil(() -> killed(Helper, Reachable, "SCP-173 never went for the reachable D-Class past three sealed ones" + where(Statue, Start)))
                .thenExecute(() -> {
                    for (int Index = 1; Index <= Cells.length; Index++) {
                        Helper.assertTrue(Bodies.get(Index).isAlive(), "SCP-173 reached a sealed D-Class");
                    }
                    tidy(Bodies);
                })
                .thenSucceed();
    }
}
