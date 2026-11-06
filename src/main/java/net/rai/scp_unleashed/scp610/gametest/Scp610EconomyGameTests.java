package net.rai.scp_unleashed.scp610.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionSite;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.scp610.Scp610Bile;
import net.rai.scp_unleashed.scp610.Scp610Biomass;
import net.rai.scp_unleashed.scp610.Scp610BroodSac;
import net.rai.scp_unleashed.scp610.Scp610Creature;
import net.rai.scp_unleashed.scp610.Scp610Faction;
import net.rai.scp_unleashed.scp610.Scp610Form;
import net.rai.scp_unleashed.scp610.Scp610Founding;
import net.rai.scp_unleashed.scp610.Scp610Growth;
import net.rai.scp_unleashed.scp610.Scp610Harvester;
import net.rai.scp_unleashed.scp610.Scp610Hive;
import net.rai.scp_unleashed.scp610.Scp610Hives;
import net.rai.scp_unleashed.scp610.Scp610Infection;
import net.rai.scp_unleashed.scp610.Scp610Merging;
import net.rai.scp_unleashed.scp610.Scp610Registry;
import net.rai.scp_unleashed.scp610.Scp610Rules;
import net.rai.scp_unleashed.scp610.Scp610SiegeBrute;
import net.rai.scp_unleashed.scp610.Scp610Spawning;
import net.rai.scp_unleashed.scp610.Scp610Spewer;
import net.rai.scp_unleashed.scp610.Scp610Stalker;

import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp610EconomyGameTests {
    private Scp610EconomyGameTests() {
    }

    private static Scp610Hive hive(GameTestHelper Helper, int X, int Z) {
        Scp610Spewer Made = Scp610Founding.found(Helper.getLevel(), Scp610TestKit.ground(Helper, X, Z), 0.0f);
        Helper.assertTrue(Made != null, "no hive could be founded");
        Scp610Hive Found = Scp610Hives.hive(Helper.getLevel().getServer(), Made.hive());
        Helper.assertTrue(Found != null, "the hive has no record");
        return Found;
    }

    private static void quiet(GameTestHelper Helper, Scp610Hive Hive) {
        long Now = Helper.getLevel().getServer().overworld().getGameTime();
        state(Helper).setTimer(Scp610Biomass.TrickleTimer, Now + 100000L);
        Hive.Site.setLastReinforce(Now + 100000L);
    }

    private static FactionState state(GameTestHelper Helper) {
        return Scp610Hives.state(Helper.getLevel().getServer());
    }

    private static Scp610Creature unit(GameTestHelper Helper, Scp610Form Form, Scp610Hive Hive, double X, double Z, boolean Still) {
        Scp610Creature Made = Scp610Spawning.raise(Helper.getLevel(), Form, Helper.absoluteVec(new Vec3(X, Scp610TestKit.Ground, Z)), 0.0f, false);
        Helper.assertTrue(Made != null, "could not raise " + Form);
        Made.setNoAi(Still);
        Made.setHome(Hive.anchor());
        FactionControl.enlist(Scp610Faction.Instance, state(Helper), Made, Hive.id(), Form == Scp610Form.BroodSac ? Scp610Faction.RoleBrood : Scp610Faction.RoleUnit);
        return Made;
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp610_biomass_growth")
    public static void growthSpendsBiomassAndStopsWhenItRunsOut(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        Scp610Rules.setSpread(Helper.getLevel().getServer(), false);
        Scp610Hive Hive = hive(Helper, 16, 16);
        Helper.assertTrue(Math.abs(Hive.biomass() - Scp610Biomass.Start) < 1.0e-6, "a new hive did not start with its biomass: " + Hive.biomass());
        ServerLevel World = Helper.getLevel();
        FactionState State = state(Helper);
        Hive.setBiomass(10.0);
        int Used = 0;
        for (int Round = 0; Round < 30; Round++) {
            Hive.SurfaceCredit = Scp610Growth.CreditCap;
            Used += Scp610Growth.grow(World, State, Hive, 4);
        }
        Helper.assertTrue(Used > 0, "the hive did not grow at all with biomass to spend");
        double Expected = Math.max(0.0, 10.0 - Used * Scp610Biomass.GrowthCost);
        Helper.assertTrue(Math.abs(Hive.biomass() - Expected) < 1.0e-6, "growth did not spend " + Scp610Biomass.GrowthCost + " per block: " + Hive.biomass() + " vs " + Expected);
        Hive.setBiomass(0.0);
        int Before = Hive.flesh();
        int Starved = 0;
        for (int Round = 0; Round < 30; Round++) {
            Hive.SurfaceCredit = Scp610Growth.CreditCap;
            Hive.ChamberCredit = Scp610Growth.CreditCap;
            Starved += Scp610Growth.grow(World, State, Hive, 4);
        }
        Helper.assertTrue(Starved == 0 && Hive.flesh() == Before, "a starved hive kept growing: " + Starved + " changes");
        Scp610TestKit.restore(Helper);
        Helper.succeed();
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 900, batch = "scp610_biomass_haul")
    public static void aHarvesterDeliversMeatToTheHive(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        Scp610Rules.setSpread(Helper.getLevel().getServer(), false);
        Scp610Hive Hive = hive(Helper, 24, 24);
        quiet(Helper, Hive);
        Hive.setBiomass(0.0);
        Scp610Harvester Harvester = (Scp610Harvester) unit(Helper, Scp610Form.Harvester, Hive, 9.5, 8.5, false);
        ItemEntity Meat = new ItemEntity(Helper.getLevel(), 0.0, 0.0, 0.0, new ItemStack(Items.BEEF, 3));
        Vec3 Spot = Helper.absoluteVec(new Vec3(6.5, Scp610TestKit.Ground, 8.5));
        Meat.moveTo(Spot.x, Spot.y, Spot.z);
        Helper.getLevel().addFreshEntity(Meat);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Hive.biomass() >= 3 * Scp610Biomass.MeatGain, "the harvester has not delivered the meat yet: carrying " + Harvester.carried()
                    + ", hive holds " + Hive.biomass());
            Scp610TestKit.restore(Helper);
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 1200, batch = "scp610_biomass_hunt")
    public static void aHarvesterBringsLivestockHome(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        Scp610Rules.setSpread(Helper.getLevel().getServer(), false);
        Scp610Hive Hive = hive(Helper, 26, 26);
        quiet(Helper, Hive);
        Hive.setBiomass(0.0);
        Scp610Harvester Harvester = (Scp610Harvester) unit(Helper, Scp610Form.Harvester, Hive, 10.5, 6.5, false);
        Chicken Prey = Helper.spawn(EntityType.CHICKEN, new Vec3(6.5, Scp610TestKit.Ground + 1, 6.5));
        Prey.setNoAi(true);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(!Prey.isAlive(), "the harvester has not killed the chicken yet");
            Helper.assertTrue(Hive.biomass() >= Scp610Biomass.LivestockGain, String.format("the livestock was not delivered: carrying %s, hive holds %s, kill credits %s, at %s",
                    Harvester.carried(), Hive.biomass(), Harvester.kills(), Helper.relativeVec(Harvester.position())));
            Scp610TestKit.restore(Helper);
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 400, batch = "scp610_tender")
    public static void aTenderHealsAWoundedUnit(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        Scp610Rules.setSpread(Helper.getLevel().getServer(), false);
        Scp610Hive Hive = hive(Helper, 16, 16);
        Scp610Creature Patient = unit(Helper, Scp610Form.Shambler, Hive, 8.5, 8.5, true);
        Patient.setHealth(Patient.getMaxHealth() * 0.4f);
        float Before = Patient.getHealth();
        unit(Helper, Scp610Form.Tender, Hive, 12.5, 8.5, false);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Patient.getHealth() >= Before + 4.0f - 0.01f, "the tender has not healed the shambler: " + Patient.getHealth() + " of " + Patient.getMaxHealth());
            Scp610TestKit.restore(Helper);
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 500, batch = "scp610_weaver")
    public static void aWeaverRepairsBrokenFlesh(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        Scp610Rules.setSpread(Helper.getLevel().getServer(), false);
        ServerLevel World = Helper.getLevel();
        Scp610Hive Hive = hive(Helper, 16, 16);
        quiet(Helper, Hive);
        BlockPos Pos = Helper.absolutePos(new BlockPos(22, Scp610TestKit.Ground - 1, 16));
        Scp610Growth.place(World, Hive, Pos, Scp610Registry.Flesh.get().defaultBlockState(), true);
        Helper.assertTrue(Scp610Hives.ownerOf(World, Pos) == Hive, "the flesh block is not owned by the hive");
        World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Scp610Growth.Flags);
        Helper.assertTrue(Hive.Scars.contains(Pos.asLong()), "breaking owned flesh left no scar");
        double Before = Hive.biomass();
        unit(Helper, Scp610Form.Weaver, Hive, 19.5, 16.5, false);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Scp610Growth.isFlesh(World.getBlockState(Pos)), "the weaver has not mended the flesh yet");
            Helper.assertTrue(Scp610Hives.ownerOf(World, Pos) == Hive, "the mended flesh is not owned by the hive");
            Helper.assertFalse(Hive.Scars.contains(Pos.asLong()), "the scar is still recorded");
            Helper.assertTrue(Hive.biomass() <= Before - Scp610Biomass.GrowthCost + 1.0e-6 + Scp610Biomass.trickleFor(Hive), "mending cost no biomass");
            Scp610TestKit.restore(Helper);
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 200, batch = "scp610_brood")
    public static void aBroodSacBirthsWithinItsMeans(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        Scp610Rules.setSpread(Helper.getLevel().getServer(), false);
        ServerLevel World = Helper.getLevel();
        Scp610Hive Hive = hive(Helper, 16, 16);
        quiet(Helper, Hive);
        Scp610BroodSac Sac = (Scp610BroodSac) unit(Helper, Scp610Form.BroodSac, Hive, 10.5, 10.5, false);
        Hive.setBiomass(25.0);
        Helper.runAfterDelay(5, () -> {
            Helper.assertTrue(Sac.beginBirth(), "a fed brood sac would not give birth");
            Scp610Form Pending = Sac.pending();
            Helper.assertTrue(Pending != null && !Pending.evolved(), "a brood sac short of the apex chose an advanced unit: " + Pending);
            double Cost = Scp610Biomass.costOf(Pending);
            Helper.runAfterDelay(Scp610BroodSac.BirthTicks + 2, () -> {
                List<Scp610Creature> Born = World.getEntitiesOfClass(Scp610Creature.class, Helper.getBounds(), Found -> Found.form() == Pending);
                Helper.assertTrue(Born.size() == 1, "expected one " + Pending + ", found " + Born.size());
                Helper.assertTrue(Math.abs(Hive.biomass() - (25.0 - Cost)) < 1.0e-6, "the birth did not cost " + Cost + ": " + Hive.biomass());
                Hive.setBiomass(3.0);
                Helper.assertFalse(Sac.beginBirth(), "a starved brood sac still gave birth");
                Scp610TestKit.restore(Helper);
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 300, batch = "scp610_advanced")
    public static void advancedUnitsSpawnAtTheApexWithinCaps(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        Scp610Rules.setSpread(Helper.getLevel().getServer(), false);
        ServerLevel World = Helper.getLevel();
        Scp610Hive Hive = hive(Helper, 16, 16);
        FactionState State = state(Helper);
        quiet(Helper, Hive);
        unit(Helper, Scp610Form.Shambler, Hive, 12.5, 12.5, true);
        unit(Helper, Scp610Form.Shambler, Hive, 13.5, 12.5, true);
        unit(Helper, Scp610Form.Pig, Hive, 14.5, 12.5, true);
        Scp610BroodSac Sac = (Scp610BroodSac) unit(Helper, Scp610Form.BroodSac, Hive, 22.5, 22.5, false);
        Hive.setBiomass(100.0);
        Helper.runAfterDelay(5, () -> {
            State.setPhase(FactionPhase.Apex);
            Scp610Merging.Group Group = Scp610Merging.begin(World, Hive, World.getGameTime());
            Helper.assertTrue(Group != null && Group.Result() == Scp610Form.SiegeBrute, "two bipeds did not set out to become a siege brute: " + (Group == null ? null : Group.Result()));
            Helper.assertTrue(Math.abs(Hive.biomass() - (100.0 - Scp610Biomass.MergeCost)) < 1.0e-6, "the merge did not cost its biomass: " + Hive.biomass());
            Helper.assertTrue(Scp610Merging.fuseAll(World.getServer()) == 1, "the merge did not complete");
            List<Scp610Creature> Brutes = World.getEntitiesOfClass(Scp610Creature.class, Helper.getBounds(), Found -> Found.form() == Scp610Form.SiegeBrute);
            Helper.assertTrue(Brutes.size() == 1, "expected one siege brute, found " + Brutes.size());
            Hive.setBiomass(100.0);
            Helper.assertTrue(Sac.beginBirth(), "a fed brood sac at the apex would not give birth");
            Scp610Form Pending = Sac.pending();
            Helper.assertTrue(Pending == Scp610Form.Stalker || Pending == Scp610Form.Spitter, "a brood sac at the apex did not choose an advanced unit: " + Pending);
            Helper.runAfterDelay(Scp610BroodSac.BirthTicks + 2, () -> {
                Helper.assertTrue(Math.abs(Hive.biomass() - (100.0 - Scp610Biomass.AdvancedCost)) < 1.0e-6, "the advanced birth did not cost " + Scp610Biomass.AdvancedCost);
                for (int Index = 0; Index < 2; Index++) {
                    unit(Helper, Scp610Form.Stalker, Hive, 4.5 + Index, 26.5, true);
                }
                for (int Index = 0; Index < 3; Index++) {
                    unit(Helper, Scp610Form.Spitter, Hive, 4.5 + Index * 2.0, 4.5, true);
                }
                Helper.assertTrue(Scp610Biomass.pickAdvanced(World.random, Hive) == null, "advanced units were still offered past their caps");
                Helper.assertTrue(Scp610Biomass.advancedLoaded() <= Scp610Biomass.AdvancedLoadedCap, "too many advanced units are loaded");
                Hive.setBiomass(100.0);
                Helper.assertTrue(Scp610Merging.begin(World, Hive, World.getGameTime()) == null, "a merge began past the hive's evolved cap");
                Scp610TestKit.restore(Helper);
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp610_siege")
    public static void aSiegeBruteBreaksWallsOnlyWithGriefing(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        Scp610Rules.setSpread(Server, false);
        Scp610Creature Made = Scp610Spawning.raise(World, Scp610Form.SiegeBrute, Helper.absoluteVec(new Vec3(16.5, Scp610TestKit.Ground, 16.5)), 180.0f, false);
        Helper.assertTrue(Made instanceof Scp610SiegeBrute, "no siege brute could be raised");
        Scp610SiegeBrute Brute = (Scp610SiegeBrute) Made;
        Brute.setNoAi(true);
        Brute.setSieging(true);
        BlockPos Front = Brute.blockPosition().relative(Brute.getDirection());
        for (int Rise = 0; Rise < 3; Rise++) {
            World.setBlock(Front.above(Rise), Blocks.GLASS.defaultBlockState(), Scp610Growth.Flags);
        }
        GameRules.BooleanValue Griefing = World.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        Griefing.set(false, Server);
        Helper.assertTrue(Brute.smashAhead(World) == 0, "the siege brute broke blocks with mob griefing off");
        Griefing.set(true, Server);
        Helper.assertTrue(Brute.smashAhead(World) > 0, "the siege brute did not break the glass in its path");
        Helper.assertTrue(World.getBlockState(Front).isAir() || World.getBlockState(Front.above()).isAir(), "the glass is still standing");
        Scp610TestKit.restore(Helper);
        Helper.succeed();
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 200, batch = "scp610_stalker")
    public static void aStalkerHidesOnFleshAndSpitsInfectUnsuited(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        ServerLevel World = Helper.getLevel();
        Scp610Rules.setSpread(World.getServer(), false);
        Scp610Creature Made = Scp610Spawning.raise(World, Scp610Form.Stalker, Helper.absoluteVec(new Vec3(8.5, Scp610TestKit.Ground, 8.5)), 0.0f, false);
        Helper.assertTrue(Made instanceof Scp610Stalker, "no stalker could be raised");
        Scp610Stalker Stalker = (Scp610Stalker) Made;
        Stalker.setNoAi(true);
        BlockPos Floor = BlockPos.containing(Stalker.getX(), Stalker.getY() - 0.2, Stalker.getZ());
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                World.setBlock(Floor.offset(Dx, 0, Dz), Scp610Registry.Flesh.get().defaultBlockState(), Scp610Growth.Flags);
            }
        }
        Villager Bare = Helper.spawn(EntityType.VILLAGER, new Vec3(20.5, Scp610TestKit.Ground, 20.5));
        Bare.setNoAi(true);
        ServerPlayer Suited = Scp610TestKit.survivor(Helper, 24.5, Scp610TestKit.Ground, 24.5);
        Scp610TestKit.suit(Suited);
        Helper.assertTrue(Scp610Bile.infects(Bare, Stalker, 0.1f), "bile did not infect an unsuited villager");
        Helper.assertFalse(Scp610Bile.infects(Suited, Stalker, 0.1f), "bile got through a full suit");
        Helper.assertTrue(Scp610Infection.isInfected(Bare) && !Scp610Infection.isInfected(Suited), "the bile infections are wrong");
        Helper.runAfterDelay(Scp610Stalker.StillTicks + 20, () -> {
            Helper.assertTrue(Stalker.hidden(), "a still stalker on flesh did not hide: still " + Stalker.stillTicks() + ", on flesh " + Stalker.onFlesh() + ", at "
                    + Stalker.blockPosition() + " over " + World.getBlockState(BlockPos.containing(Stalker.getX(), Stalker.getY() - 0.2, Stalker.getZ())));
            Scp610TestKit.dismiss(Suited);
            Scp610TestKit.restore(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp610_biomass_persist")
    public static void biomassSurvivesASaveAndLoad(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        MinecraftServer Server = Helper.getLevel().getServer();
        Scp610Rules.setSpread(Server, false);
        Scp610Hive Hive = hive(Helper, 16, 16);
        Hive.setBiomass(123.5);
        Hive.scar(BlockPos.asLong(1, 2, 3));
        Hive.scar(BlockPos.asLong(4, 5, 6));
        Scp610Hives.flush(Server);
        FactionState State = state(Helper);
        CompoundTag Saved = State.save();
        FactionState Loaded = FactionState.load(Saved);
        Helper.assertTrue(Loaded != null, "the faction state did not load");
        FactionSite Site = Loaded.site(Hive.id());
        Helper.assertTrue(Site != null, "the hive site was lost");
        Scp610Hive Back = Scp610Hive.load(Site);
        Helper.assertTrue(Math.abs(Back.biomass() - 123.5) < 1.0e-6, "the biomass changed across the reload: " + Back.biomass());
        Helper.assertTrue(Back.Scars.equals(Hive.Scars), "the scars changed across the reload");
        Scp610TestKit.restore(Helper);
        Helper.succeed();
    }
}
