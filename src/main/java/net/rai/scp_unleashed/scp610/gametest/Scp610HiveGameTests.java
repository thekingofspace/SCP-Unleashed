package net.rai.scp_unleashed.scp610.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionSite;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.scp610.Scp610Apostle;
import net.rai.scp_unleashed.scp610.Scp610Chambers;
import net.rai.scp_unleashed.scp610.Scp610Config;
import net.rai.scp_unleashed.scp610.Scp610Creature;
import net.rai.scp_unleashed.scp610.Scp610Faction;
import net.rai.scp_unleashed.scp610.Scp610FleshStore;
import net.rai.scp_unleashed.scp610.Scp610Form;
import net.rai.scp_unleashed.scp610.Scp610Founding;
import net.rai.scp_unleashed.scp610.Scp610Growth;
import net.rai.scp_unleashed.scp610.Scp610Hive;
import net.rai.scp_unleashed.scp610.Scp610Hives;
import net.rai.scp_unleashed.scp610.Scp610Infection;
import net.rai.scp_unleashed.scp610.Scp610Lord;
import net.rai.scp_unleashed.scp610.Scp610Merging;
import net.rai.scp_unleashed.scp610.Scp610Owned;
import net.rai.scp_unleashed.scp610.Scp610Pact;
import net.rai.scp_unleashed.scp610.Scp610RaidStyle;
import net.rai.scp_unleashed.scp610.Scp610Rules;
import net.rai.scp_unleashed.scp610.Scp610Spawning;
import net.rai.scp_unleashed.scp610.Scp610Spewer;

import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp610HiveGameTests {
    private Scp610HiveGameTests() {
    }

    private static Scp610Spewer hive(GameTestHelper Helper, int X, int Z) {
        Scp610Spewer Made = Scp610Founding.found(Helper.getLevel(), Scp610TestKit.ground(Helper, X, Z), 0.0f);
        Helper.assertTrue(Made != null, "no hive could be founded");
        return Made;
    }

    private static Scp610Hive data(GameTestHelper Helper, Scp610Spewer Spewer) {
        Scp610Hive Found = Scp610Hives.hive(Helper.getLevel().getServer(), Spewer.hive());
        Helper.assertTrue(Found != null, "the hive has no record");
        return Found;
    }

    private static FactionState state(GameTestHelper Helper) {
        return Scp610Hives.state(Helper.getLevel().getServer());
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 600, batch = "scp610_hive_found")
    public static void anApostleFindsShelterAndBecomesASpewer(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        int Low = Scp610TestKit.Ground;
        for (int X = 11; X <= 19; X++) {
            for (int Z = 11; Z <= 19; Z++) {
                for (int Y = Low; Y <= Low + 5; Y++) {
                    boolean Shell = X == 11 || X == 19 || Z == 11 || Z == 19 || Y == Low + 5;
                    Helper.setBlock(new BlockPos(X, Y, Z), Shell ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
        }
        ServerLevel World = Helper.getLevel();
        Scp610Creature Made = Scp610Spawning.raise(World, Scp610Form.Apostle, Helper.absoluteVec(new Vec3(15.5, Low, 15.5)), 0.0f, false);
        Helper.assertTrue(Made instanceof Scp610Apostle, "no apostle could be raised");
        Scp610Apostle Apostle = (Scp610Apostle) Made;
        Apostle.setNoAi(true);
        Helper.startSequence().thenWaitUntil(() -> {
            BlockPos Feet = Apostle.blockPosition();
            Helper.assertTrue(Scp610Founding.qualifies(World, Feet), "a dark sealed room did not qualify as a hive spot: " + Scp610Founding.allowed(World, Feet) + ", ground "
                    + Scp610Founding.groundFits(World, Feet) + ", light " + World.getBrightness(LightLayer.BLOCK, Feet) + "/" + World.getBrightness(LightLayer.SKY, Feet));
        }).thenExecute(() -> Helper.assertTrue(Apostle.evolving() || Apostle.evaluate(World), "the apostle did not start to evolve in a qualifying spot"))
                .thenIdle(Scp610Apostle.EvolveTicks + 10).thenExecute(() -> {
                    List<Scp610Spewer> Found = World.getEntitiesOfClass(Scp610Spewer.class, Helper.getBounds());
                    Helper.assertTrue(Found.size() == 1, "expected one spewer, found " + Found.size());
                    Helper.assertTrue(Apostle.isRemoved(), "the apostle is still walking");
                    Helper.assertTrue(state(Helper).phase().active(), "the faction did not awaken");
                    Helper.assertTrue(Scp610Hives.list(World.getServer()).size() == 1, "the hive was not recorded");
                    Scp610TestKit.restore(Helper);
                }).thenSucceed();
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 200, batch = "scp610_hive_spew")
    public static void spewerCloudsInfectTheUnsuited(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        Scp610Rules.setSpread(Helper.getLevel().getServer(), false);
        Scp610Spewer Spewer = hive(Helper, 16, 16);
        Villager Near = Helper.spawn(EntityType.VILLAGER, new Vec3(19.5, Scp610TestKit.Ground, 16.5));
        Near.setNoAi(true);
        ServerPlayer Suited = Scp610TestKit.survivor(Helper, 16.5, Scp610TestKit.Ground, 19.5);
        Scp610TestKit.suit(Suited);
        Helper.runAfterDelay(Scp610Spewer.GrowTicks + 5, Spewer::spew);
        Helper.runAfterDelay(Scp610Spewer.GrowTicks + 5 + Scp610Spewer.ReleaseTick + 10, () -> {
            Helper.assertTrue(Scp610Infection.isInfected(Near), "the spewer's cloud did not reach the villager");
            Helper.assertFalse(Scp610Infection.isInfected(Suited), "the spewer's cloud got through a full suit");
            Scp610TestKit.dismiss(Suited);
            Scp610TestKit.restore(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 400, batch = "scp610_hive_surface")
    public static void fleshSpreadsWithinBudgetAndStopsUnderTheRule(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        MinecraftServer Server = Helper.getLevel().getServer();
        Scp610Config.overrideForTests(-1, 40, 2, 0);
        Scp610Spewer Spewer = hive(Helper, 16, 16);
        Scp610Hive Hive = data(Helper, Spewer);
        ServerLevel World = Helper.getLevel();
        FactionState State = state(Helper);
        for (int Round = 0; Round < 400; Round++) {
            Hive.SurfaceCredit = Scp610Growth.CreditCap;
            int Used = Scp610Growth.grow(World, State, Hive, Scp610Growth.budget(Server, State));
            Helper.assertTrue(Used <= 2, "one pulse made " + Used + " changes against a budget of 2");
        }
        Helper.assertTrue(Hive.flesh() > 10, "the flesh barely spread: " + Hive.flesh());
        Helper.assertTrue(Hive.flesh() <= 40, "the flesh grew past its cap: " + Hive.flesh());
        Helper.assertTrue(Hive.carved() == 0, "carving went on with no chamber allowance");
        Scp610Config.overrideForTests(-1, 400, 4, 0);
        Scp610Rules.setSpread(Server, false);
        int Before = Hive.flesh();
        Helper.runAfterDelay(80, () -> {
            Helper.assertTrue(Hive.flesh() == Before, "the flesh kept spreading with the rule off");
            Scp610Rules.setSpread(Server, true);
        });
        Helper.runAfterDelay(320, () -> {
            Helper.assertTrue(Hive.flesh() > Before, "the flesh did not resume with the rule back on");
            Scp610TestKit.restore(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp610_hive_under")
    public static void undergroundGrowthKeepsToItsBudgetAndLimit(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        MinecraftServer Server = Helper.getLevel().getServer();
        Scp610Rules.setSpread(Server, false);
        Scp610Config.overrideForTests(-1, 4000, 3, 60);
        Scp610Spewer Spewer = hive(Helper, 16, 16);
        Scp610Hive Hive = data(Helper, Spewer);
        ServerLevel World = Helper.getLevel();
        FactionState State = state(Helper);
        int Total = 0;
        for (int Round = 0; Round < 600; Round++) {
            Hive.ChamberCredit = Scp610Growth.CreditCap;
            int Used = Scp610Growth.grow(World, State, Hive, 3);
            Helper.assertTrue(Used <= 3, "one pulse carved " + Used + " blocks against a budget of 3");
            Total += Used;
        }
        Helper.assertTrue(Hive.carved() > 20, "the hive barely dug: " + Hive.carved());
        Helper.assertTrue(Hive.carved() <= 60, "the hive dug past its chamber limit: " + Hive.carved());
        Helper.assertTrue(Total > Hive.carved(), "no chamber walls were lined with flesh");
        BlockPos Start = Hive.anchor().below(3);
        Helper.assertTrue(World.getBlockState(Start).isAir(), "the gut tunnel does not start under the spewer");
        Helper.assertFalse(World.getBlockState(Hive.anchor().below(1)).isAir(), "the spewer's footing was dug out");
        Scp610TestKit.restore(Helper);
        Helper.succeed();
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 200, batch = "scp610_hive_razing")
    public static void onlyTheLastHiveEndsTheFaction(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        Scp610Rules.setSpread(Helper.getLevel().getServer(), false);
        Scp610Spewer First = hive(Helper, 6, 6);
        Scp610Spewer Second = hive(Helper, 26, 26);
        Helper.assertTrue(Scp610Hives.list(Helper.getLevel().getServer()).size() == 2, "two hives were not recorded");
        Helper.runAfterDelay(Scp610Spewer.GrowTicks + 5, () -> {
            First.kill();
            Helper.assertTrue(state(Helper).phase().active(), "killing one of two hives ended the faction");
            Helper.assertTrue(Scp610Hives.list(Helper.getLevel().getServer()).size() == 1, "the dead hive is still recorded");
        });
        Helper.runAfterDelay(Scp610Spewer.GrowTicks + 15, () -> {
            Second.kill();
            Helper.assertTrue(state(Helper).phase() == FactionPhase.Defeated, "killing the last hive did not end the faction");
            Scp610TestKit.restore(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 200, batch = "scp610_hive_merge")
    public static void unitsMergeOnlyAtTheApex(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        Scp610Rules.setSpread(Helper.getLevel().getServer(), false);
        ServerLevel World = Helper.getLevel();
        Scp610Spewer Spewer = hive(Helper, 16, 16);
        Scp610Hive Hive = data(Helper, Spewer);
        FactionState State = state(Helper);
        for (int Index = 0; Index < 3; Index++) {
            Scp610Creature Pig = Scp610Spawning.raise(World, Scp610Form.Pig, Helper.absoluteVec(new Vec3(12.5 + Index * 2.0, Scp610TestKit.Ground, 12.5)), 0.0f, false);
            Helper.assertTrue(Pig != null, "a pig unit could not be raised");
            Pig.setNoAi(true);
            FactionControl.enlist(Scp610Faction.Instance, State, Pig, Hive.id(), Scp610Faction.RoleUnit);
        }
        Helper.runAfterDelay(5, () -> {
            State.setPhase(FactionPhase.Ascendant);
            Hive.NextMerge = 0L;
            Scp610Merging.tick(World, State, Hive);
            Helper.assertTrue(Scp610Merging.pending().isEmpty(), "units began merging before the apex");
            State.setPhase(FactionPhase.Apex);
            Scp610Merging.Group Group = Scp610Merging.begin(World, Hive, World.getGameTime());
            Helper.assertTrue(Group != null && Group.Result() == Scp610Form.Amalgam, "three quadrupeds did not set out to become an amalgam");
            Helper.assertTrue(Scp610Merging.fuseAll(World.getServer()) == 1, "the merge did not complete");
            List<Scp610Creature> Left = World.getEntitiesOfClass(Scp610Creature.class, Helper.getBounds(), Found -> Found.form() == Scp610Form.Pig);
            List<Scp610Creature> Made = World.getEntitiesOfClass(Scp610Creature.class, Helper.getBounds(), Found -> Found.form() == Scp610Form.Amalgam);
            Helper.assertTrue(Left.isEmpty(), "the merged pigs are still standing");
            Helper.assertTrue(Made.size() == 1, "expected one amalgam, found " + Made.size());
            Scp610TestKit.restore(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp610_hive_persist")
    public static void theHivesSurviveASaveAndLoad(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        MinecraftServer Server = Helper.getLevel().getServer();
        Scp610Rules.setSpread(Server, false);
        Scp610Config.overrideForTests(-1, 4000, 8, 40);
        Scp610Spewer Spewer = hive(Helper, 16, 16);
        Scp610Hive Hive = data(Helper, Spewer);
        ServerLevel World = Helper.getLevel();
        FactionState State = state(Helper);
        for (int Round = 0; Round < 60; Round++) {
            Hive.SurfaceCredit = Scp610Growth.CreditCap;
            Hive.ChamberCredit = Scp610Growth.CreditCap;
            Scp610Growth.grow(World, State, Hive, 8);
        }
        Scp610Hives.flush(Server);
        CompoundTag Saved = State.save();
        FactionState Loaded = FactionState.load(Saved);
        Helper.assertTrue(Loaded != null, "the faction state did not load");
        FactionSite Site = Loaded.site(Hive.id());
        Helper.assertTrue(Site != null && Site.is(Scp610Hive.Kind), "the hive site was lost");
        Helper.assertFalse(Site.extra().contains(Scp610Hive.FleshKey), "the owned flesh is still written into the faction save");
        CompoundTag Stored = Scp610Hives.store(Server).save(new CompoundTag(), Server.registryAccess());
        Scp610FleshStore Reloaded = Scp610FleshStore.load(Stored, Server.registryAccess());
        Helper.assertTrue(Reloaded.has(Hive.id()), "the flesh store lost the hive");
        Scp610Hive Back = Scp610Hive.load(Site, Reloaded.owned(Hive.id()));
        Helper.assertTrue(Back.flesh() == Hive.flesh(), "owned flesh changed across the reload: " + Back.flesh() + " vs " + Hive.flesh());
        for (int At = 0; At < Hive.Owned.size(); At++) {
            Helper.assertTrue(Back.Owned.contains(Hive.Owned.get(At)), "the reload dropped " + BlockPos.of(Hive.Owned.get(At)));
        }
        Site.extra().putLongArray(Scp610Hive.FleshKey, new long[]{Hive.anchor().above(40).asLong()});
        Scp610Hive Legacy = Scp610Hive.load(Site, new Scp610Owned());
        Helper.assertTrue(Legacy.Migrated && Legacy.flesh() == 1, "an old save's flesh list was not migrated");
        Legacy.flush();
        Helper.assertFalse(Site.extra().contains(Scp610Hive.FleshKey), "the migrated flesh list was left in the faction save");
        Helper.assertTrue(Back.Plan.size() == Hive.Plan.size(), "the chamber plan changed across the reload");
        if (!Hive.Plan.isEmpty()) {
            Scp610Chambers.Chamber Now = Hive.Plan.get(0);
            Scp610Chambers.Chamber Then = Back.Plan.get(0);
            Helper.assertTrue(Now.Progress == Then.Progress && Now.Kind.equals(Then.Kind), "the chamber progress changed across the reload");
        }
        Helper.assertTrue(Back.carved() == Hive.carved(), "the carved count changed across the reload");
        Helper.assertTrue(Loaded.phase() == State.phase() && Loaded.era() == State.era(), "the faction phase or era changed across the reload");
        Scp610TestKit.restore(Helper);
        Helper.succeed();
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp610_raid")
    public static void moreFleshMeansBiggerRaids(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        Scp610Rules.setSpread(Helper.getLevel().getServer(), false);
        hive(Helper, 16, 16);
        FactionState State = state(Helper);
        Helper.assertFalse(Scp610RaidStyle.ready(State), "a fresh hive is already strong enough to raid");
        State.setPower(600.0, Scp610Faction.MaxPower);
        Helper.assertTrue(Scp610RaidStyle.ready(State), "a strong hive is not ready to raid");
        Helper.assertTrue(Scp610RaidStyle.hordeSize(120.0) < Scp610RaidStyle.hordeSize(1200.0), "more flesh did not mean a bigger horde");
        Helper.assertTrue(Scp610RaidStyle.hordeSize(100000.0) == Scp610RaidStyle.MaxHorde, "the horde size is not capped");
        Scp610TestKit.restore(Helper);
        Helper.succeed();
    }

    public static AABB around(Vec3 At, double Reach) {
        return new AABB(At, At).inflate(Reach);
    }

    public static int maxHives() {
        return Scp610Config.maxHives();
    }
}
