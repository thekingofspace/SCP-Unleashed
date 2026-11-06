package net.rai.scp_unleashed.scp001scarlet.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.ArchivePool;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidContext;
import net.rai.scp_unleashed.raid.RaidDirector;
import net.rai.scp_unleashed.raid.RaidWorld;
import net.rai.scp_unleashed.scp001scarlet.ScarletConfig;
import net.rai.scp_unleashed.scp001scarlet.ScarletMeter;
import net.rai.scp_unleashed.scp001scarlet.ScarletReadCondition;
import net.rai.scp_unleashed.scp001scarlet.ScarletWorld;
import net.rai.scp_unleashed.scp001scarlet.ScarletXk;
import net.rai.scp_unleashed.scp001scarlet.Scp001ScarletRegistry;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp001scarlet.cult.ScarletCultFaction;
import net.rai.scp_unleashed.scp001scarlet.cult.ScarletCultist;
import net.rai.scp_unleashed.scp001scarlet.cult.ScarletRitualStyle;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThrone;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThroneEmitter;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThroneSite;
import net.rai.scp_unleashed.worldend.WorldEndConfig;
import net.rai.scp_unleashed.worldend.WorldEndRules;
import net.rai.scp_unleashed.worldend.WorldEndState;
import net.rai.scp_unleashed.worldend.WorldEnding;

import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class ScarletGameTests {
    public static final String Arena = "scarlet_arena";
    public static final BlockPos Middle = new BlockPos(16, 1, 16);

    private ScarletGameTests() {
    }

    private static MinecraftServer server(GameTestHelper Helper) {
        return Helper.getLevel().getServer();
    }

    private static ScarletState fresh(GameTestHelper Helper) {
        MinecraftServer Server = server(Helper);
        ScarletState Made = new ScarletState();
        Made.setDirty();
        Server.overworld().getDataStorage().set("scp_unleashed_scarlet", Made);
        ScarletWorld.get(Server).wipe();
        ScarletThroneSite.get(Server).forget();
        ScarletThroneEmitter.clear();
        FactionControl.reset(Server, ScarletCultFaction.Instance);
        ScarletConfig.overrideForTests(-1.0, -1.0);
        ScarletCultFaction.ignoreNightForTests(false);
        ScarletMeter.forget();
        return ScarletState.get(Server);
    }

    private static void finish(GameTestHelper Helper) {
        fresh(Helper);
        Helper.succeed();
    }

    private static FakePlayer reader(GameTestHelper Helper) {
        FakePlayer Made = new FakePlayer(Helper.getLevel(), new GameProfile(UUID.randomUUID(), "scarlet_reader"));
        Vec3 Where = Vec3.atBottomCenterOf(Helper.absolutePos(Middle));
        Made.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        return Made;
    }

    private static void advanceDay(MinecraftServer Server, long Ticks) {
        ServerLevel World = Server.overworld();
        World.setDayTime(World.getDayTime() + Ticks);
    }

    private static void ground(GameTestHelper Helper, BlockPos Centre) {
        for (int Dx = -6; Dx <= 6; Dx++) {
            for (int Dz = -6; Dz <= 6; Dz++) {
                Helper.setBlock(Centre.offset(Dx, -1, Dz), Blocks.STONE.defaultBlockState());
                for (int Dy = 0; Dy <= 3; Dy++) {
                    Helper.setBlock(Centre.offset(Dx, Dy, Dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static RaidContext ritual(GameTestHelper Helper, int Size) {
        MinecraftServer Server = server(Helper);
        ground(Helper, Middle);
        RaidWorld Data = RaidWorld.get(Server);
        Raid Old = Data.activeFor(ScarletCultFaction.Instance.id());
        if (Old != null) {
            Data.remove(Old);
        }
        FactionState State = FactionControl.state(Server, ScarletCultFaction.Instance);
        FakePlayer Host = reader(Helper);
        Raid Started = RaidDirector.launch(Server, Data, ScarletCultFaction.Instance, ScarletRitualStyle.Instance, State,
                RaidDirector.forcedCandidate(Host, ScarletRitualStyle.Instance), true, Server.overworld().getGameTime());
        Helper.assertTrue(Started != null, "the ritual raid did not launch");
        RaidContext Context = RaidDirector.context(Server, Started, Server.overworld().getGameTime());
        Helper.assertTrue(Context != null, "no raid context");
        Helper.assertTrue(ScarletRitualStyle.gather(Context, Helper.absolutePos(Middle), Size), "the cultists did not gather");
        return Context;
    }

    private static void sweep(GameTestHelper Helper) {
        AABB Area = new AABB(Helper.absolutePos(BlockPos.ZERO)).expandTowards(32.0, 12.0, 32.0).inflate(8.0);
        for (ScarletCultist Each : Helper.getLevel().getEntitiesOfClass(ScarletCultist.class, Area)) {
            Each.discard();
        }
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scarlet_read")
    public static void readingSetsReadAndStirringOnce(GameTestHelper Helper) {
        ScarletState State = fresh(Helper);
        MinecraftServer Server = server(Helper);
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp001ScarletRegistry.DocumentId);
        Helper.assertTrue(Document != null, "the SCP-001 Scarlet King file is missing");
        Helper.assertTrue(Document.Pool() == ArchivePool.Apocalyptic, "the file is not in the apocalyptic pool");
        Helper.assertTrue(Document.Condition() instanceof ScarletReadCondition, "the file does not use the read condition");
        Helper.assertTrue(State.phase() == ScarletState.Phase.Asleep && !State.read(), "a fresh world is not asleep");
        FakePlayer First = reader(Helper);
        ScarletMeter.read(First);
        Helper.assertTrue(State.read(), "reading did not mark the file as read");
        Helper.assertTrue(State.phase() == ScarletState.Phase.Stirring, "reading did not wake the King");
        Helper.assertTrue(ScarletMeter.seen(First), "the reader did not see the meter");
        long Woke = State.phaseTick();
        Helper.runAfterDelay(3, () -> {
            FakePlayer Second = reader(Helper);
            ScarletMeter.read(Second);
            Helper.assertTrue(State.phaseTick() == Woke, "a second read changed the phase again");
            Helper.assertTrue(State.phase() == ScarletState.Phase.Stirring, "a second read moved the phase");
            Helper.assertTrue(ScarletMeter.seen(Second), "the second reader did not see the meter");
            finish(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scarlet_chains")
    public static void chainsBreakAtSeventhsAndNeverMend(GameTestHelper Helper) {
        ScarletState Alone = new ScarletState();
        Alone.manifestation(1.0f / 7.0f - 0.01f);
        Helper.assertTrue(Alone.broken() == 0, "a chain broke before the first seventh");
        Alone.manifestation(1.0f / 7.0f);
        Helper.assertTrue(Alone.broken() == 1, "the first chain did not break at one seventh");
        Alone.manifestation(0.6f);
        Helper.assertTrue(Alone.broken() == 4, "four chains should be broken at 60%, got " + Alone.broken());
        Alone.manifestation(0.0f);
        Helper.assertTrue(Alone.broken() == 4, "lowering the meter mended a chain");
        Alone.manifestation(1.0f);
        Helper.assertTrue(Alone.broken() == ScarletState.Chains, "not every chain broke at 100%");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scarlet_climb")
    public static void meterClimbsOverTime(GameTestHelper Helper) {
        ScarletState State = fresh(Helper);
        MinecraftServer Server = server(Helper);
        ScarletConfig.overrideForTests(0.5, -1.0);
        ScarletMeter.read(reader(Helper));
        ScarletMeter.tick(Server);
        advanceDay(Server, 12000L);
        ScarletMeter.tick(Server);
        Helper.assertTrue(Math.abs(State.manifestation() - 0.25f) < 0.01f, "half a day at 50% per day should give 25%, got " + State.manifestation());
        Helper.assertTrue(State.broken() == 1, "one chain should be broken at 25%, got " + State.broken());
        Helper.assertTrue(ScarletWorld.get(Server).told() == 1, "the chain snap was not announced");
        advanceDay(Server, 12000L);
        ScarletMeter.tick(Server);
        Helper.assertTrue(State.broken() == 3, "three chains should be broken at 50%, got " + State.broken());
        State.manifestation(0.1f);
        ScarletMeter.tick(Server);
        Helper.assertTrue(State.broken() == 3, "lowering the meter mended a chain");
        finish(Helper);
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scarlet_phases")
    public static void cultistsOnlyInValidPhases(GameTestHelper Helper) {
        ScarletState State = fresh(Helper);
        MinecraftServer Server = server(Helper);
        ScarletCultFaction.ignoreNightForTests(true);
        FactionState Faction = FactionControl.state(Server, ScarletCultFaction.Instance);
        for (ScarletState.Phase Each : ScarletState.Phase.values()) {
            State.phase(Each, Server.overworld().getGameTime());
            ScarletCultFaction.Instance.tick(Server, Faction);
            boolean Expected = Each == ScarletState.Phase.Stirring || Each == ScarletState.Phase.Xk2;
            Helper.assertTrue(ScarletCultFaction.allowed(Server) == Expected, "cultists allowed=" + ScarletCultFaction.allowed(Server) + " in " + Each);
            if (Each != ScarletState.Phase.Freed) {
                Helper.assertTrue(ScarletCultFaction.Instance.canRaid(Faction) == Expected, "cultists could raid=" + !Expected + " in " + Each);
            }
        }
        Helper.assertTrue(!ScarletCultFaction.Instance.canRaid(Faction), "cultists can still raid once the King is dead");
        finish(Helper);
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scarlet_killed")
    public static void killedGroupGivesNoBump(GameTestHelper Helper) {
        ScarletState State = fresh(Helper);
        MinecraftServer Server = server(Helper);
        ScarletMeter.read(reader(Helper));
        State.manifestation(0.3f);
        RaidContext Context = ritual(Helper, 3);
        Raid Current = Context.Current();
        Helper.runAfterDelay(2, () -> {
            for (Mob Each : Context.loadedRaiders()) {
                Each.hurt(Helper.getLevel().damageSources().playerAttack(reader(Helper)), 500.0f);
            }
        });
        Helper.runAfterDelay(8, () -> {
            RaidContext Later = RaidDirector.context(Server, Current, Server.overworld().getGameTime());
            if (Later != null && Current.live()) {
                ScarletRitualStyle.Instance.tick(Later);
            }
            Helper.assertTrue(!Current.live(), "the ritual is still live after its cultists died");
            Helper.assertTrue(!ScarletRitualStyle.completed(Current), "a killed group completed its ritual");
            Helper.assertTrue(Math.abs(State.manifestation() - 0.3f) < 0.0005f, "a killed group still raised the meter: " + State.manifestation());
            sweep(Helper);
            finish(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scarlet_completed")
    public static void completedRitualRaisesTheMeter(GameTestHelper Helper) {
        ScarletState State = fresh(Helper);
        MinecraftServer Server = server(Helper);
        ScarletMeter.read(reader(Helper));
        State.manifestation(0.3f);
        RaidContext Context = ritual(Helper, 4);
        Raid Current = Context.Current();
        BlockPos Centre = Helper.absolutePos(Middle);
        Helper.assertTrue(Helper.getLevel().getBlockState(Centre).is(Scp001ScarletRegistry.Brazier.get()), "the cultists placed no brazier");
        Helper.runAfterDelay(2, () -> {
            for (Mob Each : Context.loadedRaiders()) {
                if (Each instanceof ScarletCultist Member) {
                    Member.kneeling(true);
                }
            }
            ScarletRitualStyle.progress(Current, ScarletConfig.ritualTicks() - 1);
            RaidContext Later = RaidDirector.context(Server, Current, Server.overworld().getGameTime());
            Helper.assertTrue(Later != null, "no raid context");
            ScarletRitualStyle.Instance.tick(Later);
            Helper.assertTrue(ScarletRitualStyle.completed(Current), "the ritual did not complete");
            float Bump = State.manifestation() - 0.3f;
            Helper.assertTrue(Bump >= (float) ScarletConfig.ritualMin() - 0.0005f && Bump <= (float) ScarletConfig.ritualMax() + 0.0005f, "the ritual bump was " + Bump);
            Helper.assertTrue(!Current.live(), "the ritual raid did not end");
            Helper.assertTrue(Helper.getLevel().getBlockState(Centre).is(Scp001ScarletRegistry.Scorch.get()), "no sigil scorch was left behind");
        });
        Helper.runAfterDelay(6, () -> {
            List<ScarletCultist> Left = Helper.getLevel().getEntitiesOfClass(ScarletCultist.class, new AABB(Centre).inflate(8.0));
            Helper.assertTrue(Left.isEmpty(), "cultists stayed after the ritual: " + Left.size());
            finish(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scarlet_xk2")
    public static void fullMeterStartsXk2WithoutAThrone(GameTestHelper Helper) {
        ScarletState State = fresh(Helper);
        MinecraftServer Server = server(Helper);
        ScarletMeter.read(reader(Helper));
        State.manifestation(1.0f);
        ScarletMeter.tick(Server);
        Helper.assertTrue(State.phase() == ScarletState.Phase.Xk2, "a full meter did not start XK-2, phase " + State.phase());
        ScarletXk.tick(Server);
        Helper.assertFalse(State.thronePlaced(), "XK-2 placed a Throne on its own");
        Helper.assertFalse(ScarletThroneSite.get(Server).planned(), "XK-2 planned a Throne site on its own");
        BlockPos Throne = Helper.absolutePos(Middle);
        List<BlockPos> Pillars = ScarletThrone.pillars(Server.overworld(), Throne);
        Helper.assertTrue(Pillars.size() == ScarletThrone.Pillars, "the Throne does not report seven pillars");
        Helper.assertTrue(Pillars.get(0).getZ() < Throne.getZ() && Pillars.get(0).getX() == Throne.getX(), "the first pillar is not due north");
        Helper.assertTrue(Pillars.get(1).getX() > Throne.getX(), "the pillars do not run clockwise");
        Helper.assertTrue(ScarletThrone.dais(Server.overworld(), Throne).getY() == Throne.getY() + ScarletThrone.DaisHeight, "the dais is not on top of the floor");
        finish(Helper);
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scarlet_pause")
    public static void riteAndFightPauseTheClock(GameTestHelper Helper) {
        ScarletState State = fresh(Helper);
        MinecraftServer Server = server(Helper);
        ScarletMeter.read(reader(Helper));
        ScarletXk.begin(Server);
        ScarletWorld Data = ScarletWorld.get(Server);
        ScarletXk.tick(Server);
        advanceDay(Server, 2000L);
        ScarletXk.tick(Server);
        long Ran = Data.clock();
        Helper.assertTrue(Ran == 2000L, "the XK-2 clock did not run, at " + Ran);
        for (ScarletState.Phase Hold : new ScarletState.Phase[]{ScarletState.Phase.Rite, ScarletState.Phase.Fight}) {
            State.phase(Hold, Server.overworld().getGameTime());
            ScarletXk.tick(Server);
            advanceDay(Server, 5000L);
            ScarletXk.tick(Server);
            Helper.assertTrue(Data.clock() == Ran, "the clock moved during " + Hold + ": " + Data.clock());
            Helper.assertTrue(!Data.endingBegun(), "the world end began during " + Hold);
        }
        State.phase(ScarletState.Phase.Xk2, Server.overworld().getGameTime());
        ScarletXk.tick(Server);
        advanceDay(Server, 1000L);
        ScarletXk.tick(Server);
        Helper.assertTrue(Data.clock() == Ran + 1000L, "the clock did not resume after the pause: " + Data.clock());
        finish(Helper);
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scarlet_freed")
    public static void freedStopsTheMeterAndTheCult(GameTestHelper Helper) {
        ScarletState State = fresh(Helper);
        MinecraftServer Server = server(Helper);
        ScarletConfig.overrideForTests(0.5, -1.0);
        ScarletCultFaction.ignoreNightForTests(true);
        ScarletMeter.read(reader(Helper));
        State.manifestation(0.4f);
        ScarletWorld.get(Server).sky(0.6f);
        State.phase(ScarletState.Phase.Freed, Server.overworld().getGameTime());
        ScarletMeter.tick(Server);
        advanceDay(Server, 12000L);
        ScarletMeter.tick(Server);
        ScarletXk.tick(Server);
        Helper.assertTrue(Math.abs(State.manifestation() - 0.4f) < 0.0005f, "the meter moved after the King died: " + State.manifestation());
        Helper.assertTrue(!ScarletCultFaction.allowed(Server), "cultists are allowed after the King died");
        Helper.assertTrue(ScarletWorld.get(Server).clearing(), "the scarlet sky is not clearing");
        FactionState Faction = FactionControl.state(Server, ScarletCultFaction.Instance);
        ScarletCultFaction.Instance.tick(Server, Faction);
        Helper.assertTrue(!ScarletCultFaction.Instance.canRaid(Faction), "the cult can still raid after the King died");
        finish(Helper);
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scarlet_ending")
    public static void worldEndIsADryRun(GameTestHelper Helper) {
        ScarletState State = fresh(Helper);
        MinecraftServer Server = server(Helper);
        Helper.assertTrue(WorldEnding.dryRun(Server), "the GameTest server must be a dry run");
        WorldEndRules.setEndings(Server, true);
        WorldEndConfig.overrideForTests(1);
        WorldEnding.clearDryRun();
        ScarletConfig.overrideForTests(-1.0, 0.01);
        ScarletMeter.read(reader(Helper));
        ScarletXk.begin(Server);
        ScarletXk.tick(Server);
        advanceDay(Server, 400L);
        ScarletXk.tick(Server);
        Helper.assertTrue(ScarletWorld.get(Server).endingBegun(), "the XK-2 clock ran out without starting the end");
        Helper.assertTrue(WorldEnding.stage(Server) == WorldEndState.Stage.Freezing, "the world end did not start, stage " + WorldEnding.stage(Server));
        Helper.assertTrue(ScarletXk.ours(Server), "the world end does not name the Scarlet King as its source");
        Helper.runAfterDelay(WorldEnding.FreezeTicks + 20, () -> {
            List<String> Records = WorldEnding.dryRunRecords();
            Helper.assertTrue(Records.stream().anyMatch(Line -> Line.startsWith("delete ")), "the dry run did not record the deletion: " + Records);
            Helper.assertTrue(WorldEnding.stage(Server) == WorldEndState.Stage.Released, "the dry run did not release players");
            Helper.assertTrue(WorldEnding.pending() == null && Server.isRunning(), "the dry run queued a deletion or stopped the server");
            WorldEndConfig.overrideForTests(-1);
            WorldEnding.clearDryRun();
            Server.overworld().setBlock(Helper.absolutePos(Middle), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            finish(Helper);
        });
    }
}
