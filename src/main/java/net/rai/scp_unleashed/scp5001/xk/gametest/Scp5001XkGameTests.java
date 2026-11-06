package net.rai.scp_unleashed.scp5001.xk.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.core.Scp5001Duel;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.scp5001.xk.Scp5001Xk;
import net.rai.scp_unleashed.scp5001.xk.Scp5001XkConfig;
import net.rai.scp_unleashed.scp5001.xk.Scp5001XkRegistry;
import net.rai.scp_unleashed.scp5001.xk.XkRising;
import net.rai.scp_unleashed.scp5001.xk.XkState;
import net.rai.scp_unleashed.scp5001.xk.Yaldabaoth;
import net.rai.scp_unleashed.scp610.Scp610Creature;
import net.rai.scp_unleashed.scp610.Scp610Form;
import net.rai.scp_unleashed.scp610.Scp610Spawning;
import net.rai.scp_unleashed.scp610.gametest.Scp610TestKit;
import net.rai.scp_unleashed.worldend.WorldEndConfig;
import net.rai.scp_unleashed.worldend.WorldEndRules;
import net.rai.scp_unleashed.worldend.WorldEndState;
import net.rai.scp_unleashed.worldend.WorldEnding;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp5001XkGameTests {
    private Scp5001XkGameTests() {
    }

    private static MinecraftServer server(GameTestHelper Helper) {
        return Helper.getLevel().getServer();
    }

    private static void fresh(GameTestHelper Helper, double Chance, int First, long Sky, int Cap) {
        MinecraftServer Server = server(Helper);
        Scp610TestKit.fresh(Helper);
        Scp5001Xk.cancel(Server);
        Scp5001XkConfig.overrideForTests(Chance, First, Sky, Cap);
        Scp5001Xk.clearTicksForTests(-1);
        WorldEnding.clearDryRun();
        WorldEndRules.setEndings(Server, true);
        WorldEndConfig.overrideForTests(-1);
    }

    private static void finish(GameTestHelper Helper) {
        MinecraftServer Server = server(Helper);
        Scp5001Xk.cancel(Server);
        Scp5001XkConfig.clearOverrides();
        Scp5001Xk.clearTicksForTests(-1);
        WorldEndRules.setEndings(Server, true);
        WorldEndConfig.overrideForTests(-1);
        WorldEnding.clearDryRun();
        Scp610TestKit.clear(Helper);
        Helper.succeed();
    }

    private static void phase(GameTestHelper Helper, Scp5001State.Phase Next) {
        MinecraftServer Server = server(Helper);
        Scp5001State.get(Server).phase(Next, Server.overworld().getGameTime());
    }

    private static Vec3 above(GameTestHelper Helper) {
        return Helper.absoluteVec(new Vec3(16.0, Scp610TestKit.Ground + 12.0, 16.0));
    }

    private static Zombie victim(GameTestHelper Helper, double X, double Z) {
        Zombie Made = Helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(X, Scp610TestKit.Ground, Z));
        Made.setPersistenceRequired();
        return Made;
    }

    private static int risen(GameTestHelper Helper) {
        AABB Box = new AABB(Helper.absolutePos(BlockPos.ZERO)).inflate(40.0);
        return Helper.getLevel().getEntitiesOfClass(Scp610Creature.class, Box, Found -> Found.isAlive()).size();
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp5001xk_start")
    public static void firstPhaseStartsFromState(GameTestHelper Helper) {
        fresh(Helper, 0.0, 1000000, 100000L, 4);
        phase(Helper, Scp5001State.Phase.Xk1);
        Helper.runAfterDelay(5, () -> {
            MinecraftServer Server = server(Helper);
            XkState State = XkState.get(Server);
            Helper.assertTrue(State.stage() == XkState.Stage.Xk1, "the XK state did not enter XK-1, it is " + State.stage());
            Helper.assertTrue(State.announced(), "XK-1 was not announced");
            Helper.assertTrue(Scp5001Xk.rising(Server), "kills do not rise during XK-1");
            Helper.assertTrue(!State.flight().present(), "Yaldabaoth arrived during XK-1");
            Helper.assertTrue(Scp5001State.get(Server).phase() == Scp5001State.Phase.Xk1, "the shared phase moved on early");
            finish(Helper);
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 200, batch = "scp5001xk_rising")
    public static void killsRiseAtConfiguredOdds(GameTestHelper Helper) {
        fresh(Helper, 1.0, 1000000, 100000L, 8);
        phase(Helper, Scp5001State.Phase.Xk1);
        Helper.runAfterDelay(3, () -> {
            Zombie First = victim(Helper, 6.5, 6.5);
            First.kill();
            Helper.assertTrue(XkRising.pending() == 1, "a certain kill did not queue a rising, queued " + XkRising.pending());
            Helper.assertTrue(XkRising.pendingForms().get(0) == Scp610Form.Shambler, "a zombie should rise as a shambler, got " + XkRising.pendingForms().get(0));
            Scp5001XkConfig.overrideForTests(0.0, 1000000, 100000L, 8);
            Zombie Second = victim(Helper, 24.5, 24.5);
            Second.kill();
            Helper.assertTrue(XkRising.pending() == 1, "a kill at zero odds still queued a rising");
            ServerPlayer Player = Scp610TestKit.survivor(Helper, 16.5, Scp610TestKit.Ground, 16.5);
            Helper.assertTrue(!XkRising.eligible(Player), "players must not rise");
            Scp610TestKit.dismiss(Player);
            WitherBoss Boss = EntityType.WITHER.create(Helper.getLevel());
            Helper.assertTrue(Boss != null && !XkRising.eligible(Boss), "bosses must not rise");
            Scp610Creature Form = Scp610Spawning.raise(Helper.getLevel(), Scp610Form.Crawler, Helper.absoluteVec(new Vec3(10.5, Scp610TestKit.Ground, 20.5)), 0.0f, false);
            Helper.assertTrue(Form != null && !XkRising.eligible(Form), "SCP-610 forms must not rise again");
            Form.discard();
        });
        Helper.runAfterDelay(3 + XkRising.Delay + 10, () -> {
            Helper.assertTrue(XkRising.pending() == 0, "the rising never finished");
            Helper.assertTrue(risen(Helper) >= 1, "no SCP-610 form rose from the kill");
            finish(Helper);
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 200, batch = "scp5001xk_cap")
    public static void risingRespectsChunkCap(GameTestHelper Helper) {
        fresh(Helper, 1.0, 1000000, 100000L, 2);
        phase(Helper, Scp5001State.Phase.Xk1);
        Helper.runAfterDelay(3, () -> {
            for (int Index = 0; Index < 5; Index++) {
                victim(Helper, 3.5, 3.5).kill();
            }
            Helper.assertTrue(XkRising.pending() == 2, "the chunk cap of 2 let " + XkRising.pending() + " risings through");
        });
        Helper.runAfterDelay(3 + XkRising.Delay + 10, () -> {
            Helper.assertTrue(risen(Helper) <= 2, "more forms rose than the cap allows: " + risen(Helper));
            finish(Helper);
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 200, batch = "scp5001xk_manifest")
    public static void secondPhaseSpawnsOneInvulnerableYaldabaoth(GameTestHelper Helper) {
        fresh(Helper, 0.0, 1000000, 100000L, 4);
        phase(Helper, Scp5001State.Phase.Xk2);
        Helper.runAfterDelay(3, () -> {
            XkState State = XkState.get(server(Helper));
            Helper.assertTrue(State.stage() == XkState.Stage.Xk2, "the XK state did not enter XK-2");
            Helper.assertTrue(State.flight().present(), "Yaldabaoth did not manifest in XK-2");
            State.flight().hold(above(Helper));
        });
        Helper.runAfterDelay(20, () -> {
            ServerLevel World = Helper.getLevel();
            Helper.assertTrue(Scp5001Xk.count(World) == 1, "expected exactly one Yaldabaoth, found " + Scp5001Xk.count(World));
            Yaldabaoth Body = Scp5001Xk.body(World);
            Helper.assertTrue(Body != null, "the Yaldabaoth body is missing");
            Helper.assertTrue(Body instanceof Scp5001Duel, "Yaldabaoth must implement the duel interface");
            ServerPlayer Player = Scp610TestKit.survivor(Helper, 16.5, Scp610TestKit.Ground, 16.5);
            float Before = Body.getHealth();
            boolean Hurt = Body.hurt(World.damageSources().playerAttack(Player), 500.0f);
            Hurt |= Body.hurt(World.damageSources().generic(), 500.0f);
            Hurt |= Body.hurt(World.damageSources().lava(), 500.0f);
            Hurt |= Body.hurt(World.damageSources().fellOutOfWorld(), 500.0f);
            Hurt |= Body.hurt(World.damageSources().magic(), Float.MAX_VALUE);
            Scp610TestKit.dismiss(Player);
            Helper.assertTrue(!Hurt && Body.isAlive() && Body.getHealth() >= Before, "Yaldabaoth took damage");
            Yaldabaoth Extra = Scp5001XkRegistry.YaldabaothType.get().create(World);
            if (Extra != null) {
                Extra.moveTo(Body.getX() + 4.0, Body.getY(), Body.getZ());
                World.addFreshEntity(Extra);
            }
            Scp5001Xk.manifest(server(Helper), XkState.get(server(Helper)), false);
            XkState.get(server(Helper)).flight().hold(above(Helper));
        });
        Helper.runAfterDelay(30, () -> {
            Helper.assertTrue(Scp5001Xk.count(Helper.getLevel()) == 1, "a second Yaldabaoth appeared: " + Scp5001Xk.count(Helper.getLevel()));
            finish(Helper);
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 300, batch = "scp5001xk_sky")
    public static void skyProgressGrowsToTheEnd(GameTestHelper Helper) {
        fresh(Helper, 0.0, 1000000, 120L, 4);
        WorldEndRules.setEndings(server(Helper), false);
        phase(Helper, Scp5001State.Phase.Xk2);
        Helper.runAfterDelay(4, () -> XkState.get(server(Helper)).flight().hold(above(Helper)));
        Helper.runAfterDelay(40, () -> {
            float Sky = XkState.get(server(Helper)).sky();
            Helper.assertTrue(Sky > 0.2f && Sky < 0.45f, "sky progress after a third of the time is " + Sky);
        });
        Helper.runAfterDelay(80, () -> {
            float Sky = XkState.get(server(Helper)).sky();
            Helper.assertTrue(Sky > 0.55f && Sky < 0.8f, "sky progress after two thirds of the time is " + Sky);
        });
        Helper.runAfterDelay(140, () -> {
            MinecraftServer Server = server(Helper);
            Helper.assertTrue(XkState.get(Server).sky() >= 1.0f, "the sky never reached the end");
            Helper.assertTrue(Scp5001State.get(Server).phase() == Scp5001State.Phase.Ended, "the phase did not move to Ended");
            Helper.assertTrue(XkState.get(Server).flight().present(), "Yaldabaoth left before the end");
            finish(Helper);
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 600, batch = "scp5001xk_ending")
    public static void endingRunsAsDryRun(GameTestHelper Helper) {
        fresh(Helper, 0.0, 1000000, 100000L, 4);
        MinecraftServer Server = server(Helper);
        Path Folder = Server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        boolean Existed = Files.isDirectory(Folder);
        Helper.assertTrue(WorldEnding.dryRun(Server), "the GameTest server must be a dry run");
        phase(Helper, Scp5001State.Phase.Ended);
        Helper.runAfterDelay(5, () -> {
            Helper.assertTrue(WorldEnding.stage(Server) == WorldEndState.Stage.Freezing, "the ending did not start, stage " + WorldEnding.stage(Server));
            Helper.assertTrue(XkState.get(Server).sky() >= 1.0f, "the sky is not fully red at the end");
        });
        Helper.runAfterDelay(WorldEnding.FreezeTicks + 20, () -> {
            List<String> Records = WorldEnding.dryRunRecords();
            Helper.assertTrue(Records.stream().anyMatch(Line -> Line.startsWith("delete ")), "the dry run did not record the deletion: " + Records);
            Helper.assertTrue(WorldEnding.stage(Server) == WorldEndState.Stage.Released, "the dry run did not release players, stage " + WorldEnding.stage(Server));
            Helper.assertTrue(WorldEnding.pending() == null, "a dry run left a deletion pending");
            Helper.assertTrue(Server.isRunning(), "the dry run stopped the server");
            Helper.assertTrue(!Existed || Files.isDirectory(Folder), "the dry run deleted the world folder");
            finish(Helper);
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 600, batch = "scp5001xk_rule")
    public static void ruleOffNeverDeletes(GameTestHelper Helper) {
        fresh(Helper, 0.0, 1000000, 100000L, 4);
        MinecraftServer Server = server(Helper);
        WorldEndRules.setEndings(Server, false);
        phase(Helper, Scp5001State.Phase.Ended);
        Helper.runAfterDelay(5, () -> Helper.assertTrue(WorldEnding.running(Server), "the cutscene should still play with the rule off"));
        Helper.runAfterDelay(WorldEnding.FreezeTicks + 20, () -> {
            Helper.assertTrue(WorldEnding.dryRunRecords().isEmpty(), "the rule was off but a deletion was planned: " + WorldEnding.dryRunRecords());
            Helper.assertTrue(WorldEnding.stage(Server) == WorldEndState.Stage.Released, "players were not released with the rule off");
            Helper.assertTrue(WorldEnding.pending() == null && Server.isRunning(), "something was stopped or queued with the rule off");
            Helper.assertTrue(XkState.get(Server).flight().present() || XkState.get(Server).stage() == XkState.Stage.Ended, "the XK state was dropped after release");
            finish(Helper);
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 200, batch = "scp5001xk_paused")
    public static void summoningAndDuelPauseTheEnd(GameTestHelper Helper) {
        fresh(Helper, 0.0, 1000000, 100L, 4);
        MinecraftServer Server = server(Helper);
        phase(Helper, Scp5001State.Phase.Xk2);
        Helper.runAfterDelay(4, () -> XkState.get(Server).flight().hold(above(Helper)));
        long[] Held = new long[1];
        Helper.runAfterDelay(30, () -> {
            phase(Helper, Scp5001State.Phase.Summoning);
            Held[0] = XkState.get(Server).secondElapsed();
        });
        Helper.runAfterDelay(80, () -> {
            Helper.assertTrue(XkState.get(Server).secondElapsed() == Held[0], "Summoning did not pause the timer");
            Helper.assertTrue(XkState.get(Server).flight().present(), "Yaldabaoth must stay for the duel");
            phase(Helper, Scp5001State.Phase.Duel);
            XkState.get(Server).sky(1.0f);
        });
        Helper.runAfterDelay(130, () -> {
            Helper.assertTrue(XkState.get(Server).secondElapsed() == Held[0], "Duel did not pause the timer");
            Helper.assertTrue(!WorldEnding.running(Server), "the ending started during the duel");
            Helper.assertTrue(Scp5001State.get(Server).phase() == Scp5001State.Phase.Duel, "the duel phase was overridden");
            finish(Helper);
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 200, batch = "scp5001xk_early")
    public static void summoningDuringFirstPhaseManifestsEarly(GameTestHelper Helper) {
        fresh(Helper, 0.0, 1000000, 100000L, 4);
        MinecraftServer Server = server(Helper);
        phase(Helper, Scp5001State.Phase.Xk1);
        Helper.runAfterDelay(5, () -> {
            Helper.assertTrue(!XkState.get(Server).flight().present(), "Yaldabaoth arrived before the protocol was fed");
            phase(Helper, Scp5001State.Phase.Summoning);
        });
        Helper.runAfterDelay(10, () -> {
            Helper.assertTrue(XkState.get(Server).flight().present(), "Summoning during XK-1 did not manifest Yaldabaoth early");
            XkState.get(Server).flight().hold(above(Helper));
            phase(Helper, Scp5001State.Phase.Duel);
        });
        Helper.runAfterDelay(30, () -> {
            Yaldabaoth Body = Scp5001Xk.body(Helper.getLevel());
            Helper.assertTrue(Body != null, "no duel body exists");
            Body.duelEngage(null, above(Helper));
            Body.duelAttack(Scp5001Duel.Lunge, above(Helper).add(20.0, 0.0, 0.0));
            Body.duelReact(above(Helper).add(-10.0, 0.0, 0.0), 1.0f);
            Body.duelBind(above(Helper).add(-30.0, 0.0, 0.0), 0.5f);
            Body.duelBanish();
        });
        Helper.runAfterDelay(80, () -> {
            Helper.assertTrue(Scp5001Xk.count(Helper.getLevel()) == 0, "the banished Yaldabaoth is still in the world");
            Helper.assertTrue(!XkState.get(Server).flight().present(), "the banished Yaldabaoth is still flying");
            finish(Helper);
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 200, batch = "scp5001xk_saved")
    public static void savedClearsSkyAndState(GameTestHelper Helper) {
        fresh(Helper, 1.0, 1000000, 100L, 4);
        MinecraftServer Server = server(Helper);
        Scp5001Xk.clearTicksForTests(40);
        phase(Helper, Scp5001State.Phase.Xk2);
        Helper.runAfterDelay(4, () -> XkState.get(Server).flight().hold(above(Helper)));
        Helper.runAfterDelay(50, () -> {
            Helper.assertTrue(XkState.get(Server).sky() > 0.3f, "the sky did not redden before the rescue");
            phase(Helper, Scp5001State.Phase.Saved);
        });
        Helper.runAfterDelay(55, () -> {
            Helper.assertTrue(!Scp5001Xk.rising(Server), "kills still rise after the rescue");
            Helper.assertTrue(XkState.get(Server).stage() == XkState.Stage.Clearing, "the sky is not clearing");
        });
        Helper.runAfterDelay(110, () -> {
            XkState State = XkState.get(Server);
            Helper.assertTrue(State.sky() <= 0.0001f, "the sky did not clear, it is " + State.sky());
            Helper.assertTrue(State.stage() == XkState.Stage.Idle && State.secondElapsed() == 0L && !State.flight().present(), "the XK state was not cleared");
            Helper.assertTrue(Scp5001State.get(Server).phase() == Scp5001State.Phase.Saved, "the XK code must leave Saved for Mekhane to finish");
            finish(Helper);
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 200, batch = "scp5001xk_cancel")
    public static void cancelRestoresEverything(GameTestHelper Helper) {
        fresh(Helper, 0.0, 1000000, 60L, 4);
        MinecraftServer Server = server(Helper);
        phase(Helper, Scp5001State.Phase.Xk2);
        Helper.runAfterDelay(4, () -> XkState.get(Server).flight().hold(above(Helper)));
        Helper.runAfterDelay(80, () -> {
            Helper.assertTrue(WorldEnding.running(Server), "the ending should be running before the cancel");
            Scp5001Xk.cancel(Server);
        });
        Helper.runAfterDelay(90, () -> {
            XkState State = XkState.get(Server);
            Helper.assertTrue(Scp5001State.get(Server).phase() == Scp5001State.Phase.Dormant, "cancel did not return to Dormant");
            Helper.assertTrue(State.stage() == XkState.Stage.Idle && State.sky() == 0.0f, "cancel did not clear the XK state and sky");
            Helper.assertTrue(!WorldEnding.running(Server), "cancel did not stop the ending");
            Helper.assertTrue(Scp5001Xk.count(Helper.getLevel()) == 0, "cancel left Yaldabaoth in the world");
            finish(Helper);
        });
    }
}
