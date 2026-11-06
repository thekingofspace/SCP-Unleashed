package net.rai.scp_unleashed.anomaly.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlRefusing;
import net.rai.scp_unleashed.control.Controllable;
import net.rai.scp_unleashed.daevitegod.DaeviteGod;
import net.rai.scp_unleashed.daevitegod.DaeviteGodRegistry;
import net.rai.scp_unleashed.daevitegod.DaeviteGodTargets;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.mtf.MtfMissions;
import net.rai.scp_unleashed.mtf.MtfOperative;
import net.rai.scp_unleashed.mtf.MtfRegistry;
import net.rai.scp_unleashed.mtf.MtfTier;
import net.rai.scp_unleashed.mtf.gametest.MtfTestPlayer;
import net.rai.scp_unleashed.scp001.Scp001Prey;
import net.rai.scp_unleashed.scp008.Scp008Infection;
import net.rai.scp_unleashed.scp008.Scp008Registry;
import net.rai.scp_unleashed.scp008.Scp008Zombie;
import net.rai.scp_unleashed.scp016.Scp016Affliction;
import net.rai.scp_unleashed.scp016.Scp016Duelist;
import net.rai.scp_unleashed.scp016.Scp016Events;
import net.rai.scp_unleashed.scp016.Scp016Registry;
import net.rai.scp_unleashed.scp016.Scp016Spawn;
import net.rai.scp_unleashed.scp017.Scp017Registry;
import net.rai.scp_unleashed.scp017.Scp017Shadow;
import net.rai.scp_unleashed.scp020.Scp020Affliction;
import net.rai.scp_unleashed.scp027.Scp027Goals;
import net.rai.scp_unleashed.scp031.Scp031Targets;
import net.rai.scp_unleashed.scp035.Scp035Consume;
import net.rai.scp_unleashed.scp035.Scp035Sight;
import net.rai.scp_unleashed.scp049.Scp049Persons;
import net.rai.scp_unleashed.scp058.Scp058Heart;
import net.rai.scp_unleashed.scp066.Scp066Entity;
import net.rai.scp_unleashed.scp076.Scp076Abel;
import net.rai.scp_unleashed.scp076.Scp076Registry;
import net.rai.scp_unleashed.scp079.Scp079Exposure;
import net.rai.scp_unleashed.scp082.Scp082Persons;
import net.rai.scp_unleashed.scp087.Scp087Hunter;
import net.rai.scp_unleashed.scp087.Scp087Registry;
import net.rai.scp_unleashed.scp093.Scp093Transit;
import net.rai.scp_unleashed.scp093.Scp093Visions;
import net.rai.scp_unleashed.scp098.Scp098Host;
import net.rai.scp_unleashed.scp129.Scp129Infection;
import net.rai.scp_unleashed.scp129.Scp129Strain;
import net.rai.scp_unleashed.scp131.Scp131Pod;
import net.rai.scp_unleashed.scp140.Scp140Ink;
import net.rai.scp_unleashed.scp148.Scp148Shield;
import net.rai.scp_unleashed.scp149.Scp149Fly;
import net.rai.scp_unleashed.scp149.Scp149Host;
import net.rai.scp_unleashed.scp217.Scp217Clockwork;
import net.rai.scp_unleashed.scp253.Scp253Infection;
import net.rai.scp_unleashed.scp280.Scp280Registry;
import net.rai.scp_unleashed.scp280.Scp280Shade;
import net.rai.scp_unleashed.scp303.Scp303Sight;
import net.rai.scp_unleashed.scp323.Scp323Hosts;
import net.rai.scp_unleashed.scp323.Scp323Prey;
import net.rai.scp_unleashed.scp337.Scp337Mass;
import net.rai.scp_unleashed.scp337.Scp337Quarry;
import net.rai.scp_unleashed.scp337.Scp337Registry;
import net.rai.scp_unleashed.scp343.Scp343Judgement;
import net.rai.scp_unleashed.scp352.Scp352Prey;
import net.rai.scp_unleashed.scp354.Scp354Prey;
import net.rai.scp_unleashed.scp354.beasts.Scp354Beast;
import net.rai.scp_unleashed.scp354.beasts.Scp354BeastRegistry;
import net.rai.scp_unleashed.scp407.Scp407Doom;
import net.rai.scp_unleashed.scp407.Scp407Hearing;
import net.rai.scp_unleashed.scp407.Scp407Titan;
import net.rai.scp_unleashed.scp409.Scp409Exposure;
import net.rai.scp_unleashed.scp409.Scp409Infection;
import net.rai.scp_unleashed.scp427.Scp427Prey;
import net.rai.scp_unleashed.scp444.Scp444Affliction;
import net.rai.scp_unleashed.scp444.Scp444Spread;
import net.rai.scp_unleashed.scp457.Scp457;
import net.rai.scp_unleashed.scp457.Scp457Goals;
import net.rai.scp_unleashed.scp457.Scp457Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class MtfTargetGameTests {
    public static final String Arena = "mtf_arena";
    public static final double Floor = 2.0;
    public static final double Corner = 18.5;
    public static final int Settle = 10;

    private MtfTargetGameTests() {
    }

    private record Check(String Name, Predicate<LivingEntity> Picks) {
    }

    private static MtfTestPlayer caller(GameTestHelper Helper, String Name) {
        ServerLevel World = Helper.getLevel();
        MtfTestPlayer User = new MtfTestPlayer(World, Name);
        Vec3 Spot = Helper.absoluteVec(new Vec3(Corner, Floor, Corner));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.enlist(true);
        User.setGameMode(GameType.CREATIVE);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, MtfTestPlayer User) {
        MtfMissions.cancel(Helper.getLevel().getServer(), User.getUUID());
        User.enlist(false);
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static List<MtfOperative> squad(GameTestHelper Helper, MtfTestPlayer User) {
        AABB Area = Helper.getBounds().inflate(16.0);
        return Helper.getLevel().getEntitiesOfClass(MtfOperative.class, Area, Member -> User.getUUID().equals(Member.callerId()) && !Member.isRemoved());
    }

    private static List<MtfOperative> deploy(GameTestHelper Helper, MtfTestPlayer User) {
        Helper.assertTrue(MtfMissions.deployNow(User, MtfTier.Faint) == MtfMissions.Outcome.Called, "the MTF squad was not deployed");
        List<MtfOperative> Found = squad(Helper, User);
        Helper.assertTrue(!Found.isEmpty(), "no MTF operative was deployed");
        for (MtfOperative Member : Found) {
            Member.setNoAi(true);
        }
        return Found;
    }

    private static MtfOperative operative(GameTestHelper Helper, MtfTestPlayer User, double X, double Z) {
        List<MtfOperative> Found = deploy(Helper, User);
        MtfOperative Chosen = Found.get(0);
        for (int Index = 1; Index < Found.size(); Index++) {
            Found.get(Index).discard();
        }
        place(Helper, Chosen, X, Z);
        return Chosen;
    }

    private static void place(GameTestHelper Helper, LivingEntity Body, double X, double Z) {
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        Body.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
    }

    private static DClass prisoner(GameTestHelper Helper, double X, double Z) {
        return Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(X, Floor, Z));
    }

    private static void tidy(List<? extends Entity> Spawned) {
        for (Entity Each : Spawned) {
            if (!Each.isRemoved()) {
                Each.discard();
            }
        }
    }

    private static void darkRoom(GameTestHelper Helper, int MinX, int MinZ) {
        for (int X = MinX; X <= MinX + 6; X++) {
            for (int Z = MinZ; Z <= MinZ + 6; Z++) {
                for (int Y = 1; Y <= 5; Y++) {
                    boolean Shell = X == MinX || X == MinX + 6 || Z == MinZ || Z == MinZ + 6 || Y == 1 || Y == 5;
                    Helper.setBlock(new BlockPos(X, Y, Z), Shell ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
    }

    private static Scp017Shadow hunter(GameTestHelper Helper, double X, double Z) {
        Scp017Shadow Made = Helper.spawn(Scp017Registry.Shadow.get(), new Vec3(X, Floor, Z));
        CompoundTag Saved = new CompoundTag();
        Made.addAdditionalSaveData(Saved);
        Saved.putByte("Mode", Scp017Shadow.ModeHunting);
        Made.readAdditionalSaveData(Saved);
        return Made;
    }

    private static boolean consumed(LivingEntity Victim) {
        DamageSource Last = Victim.getLastDamageSource();
        return !Victim.isAlive() && Last != null && Last.is(Scp017Registry.ConsumedDamage);
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "mtf_targets_people")
    public static void anomaliesSeeOperativesAsPeople(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MtfTestPlayer User = caller(Helper, "mtf_targets_people");
        MtfOperative Operative = operative(Helper, User, 6.5, 6.5);
        DClass Prisoner = prisoner(Helper, 10.5, 6.5);
        Scp076Abel Abel = Scp076Registry.Abel.get().create(World);
        Scp087Hunter Stalker = Scp087Registry.Hunter.get().create(World);
        Scp280Shade Shade = Scp280Registry.Shade.get().create(World);
        Scp337Mass Mass = Scp337Registry.Mass.get().create(World);
        Scp354Beast Beast = Scp354BeastRegistry.Brute.get().create(World);
        Scp457 Burning = Scp457Registry.Burning.get().create(World);
        DaeviteGod God = DaeviteGodRegistry.God.get().create(World);
        List<Check> Checks = List.of(
                new Check("SCP-001 prey", Target -> Scp001Prey.rank(Target) == Scp001Prey.PersonRank),
                new Check("SCP-008 infection", Scp008Infection::canCatch),
                new Check("SCP-016 affliction", Scp016Affliction::canCatch),
                new Check("SCP-017 hunt", Scp017Shadow::huntable),
                new Check("SCP-020 mould", Scp020Affliction::canCatch),
                new Check("SCP-027 swarm", Scp027Goals::isPerson),
                new Check("SCP-031 embrace", Scp031Targets::isPerson),
                new Check("SCP-035 lure", Scp035Sight::isPerson),
                new Check("SCP-035 mask", Scp035Consume::canConsume),
                new Check("SCP-049 cure", Target -> Scp049Persons.isPerson(Target, false)),
                new Check("SCP-058 hunt", Scp058Heart::isPrey),
                new Check("SCP-066 prey", Scp066Entity::isPrey),
                new Check("SCP-076 hunt", Target -> Scp076Abel.isPrey(Abel, Target)),
                new Check("SCP-079 exposure", Scp079Exposure::isSubject),
                new Check("SCP-082 grab", Target -> Scp082Persons.kindOf(Target) == Scp082Persons.Kind.Person),
                new Check("SCP-087 hunt", Target -> Scp087Hunter.isPrey(Stalker, Target)),
                new Check("SCP-093 transit", Scp093Transit::isPersonLike),
                new Check("SCP-098 spores", Scp098Host::canCarry),
                new Check("SCP-129 strains", Scp129Infection::canCatch),
                new Check("SCP-131 pods", Target -> Scp131Pod.isPerson(Target)),
                new Check("SCP-140 ink", Scp140Ink::isPerson),
                new Check("SCP-148 ward", Scp148Shield::canBeWarded),
                new Check("SCP-149 flies", Scp149Fly::isPerson),
                new Check("SCP-149 host", Scp149Host::canHost),
                new Check("SCP-217 clockwork", Scp217Clockwork::canCatch),
                new Check("SCP-253 growth", Scp253Infection::canCatch),
                new Check("SCP-280 shade", Target -> Scp280Shade.huntable(Shade, Target)),
                new Check("SCP-303 witness", Scp303Sight::person),
                new Check("SCP-323 prey", Scp323Prey::isPerson),
                new Check("SCP-323 skull", Scp323Hosts::canHost),
                new Check("SCP-337 lash", Target -> Scp337Quarry.isQuarry(Mass, Target)),
                new Check("SCP-343 judgement", Scp343Judgement::isPerson),
                new Check("SCP-352 prey", Scp352Prey::isPerson),
                new Check("SCP-354 lake", Scp354Prey::isPrey),
                new Check("SCP-354 beasts", Target -> Scp354Beast.isQuarry(Beast, Target)),
                new Check("SCP-407 song", Scp407Hearing::counts),
                new Check("SCP-407 titan", Scp407Titan::isQuarry),
                new Check("SCP-409 crystal", Target -> Scp409Exposure.isPerson(Target)),
                new Check("SCP-427 flesh", Scp427Prey::isPerson),
                new Check("SCP-444 silence", Target -> !Scp444Spread.isImmune(Target)),
                new Check("SCP-457 hunt", Target -> Scp457Goals.isPrey(Burning, Target)),
                new Check("Daevite God", Target -> DaeviteGodTargets.isPrey(God, Target)));
        for (Check Each : Checks) {
            Helper.assertTrue(Each.Picks().test(Prisoner), Each.Name() + " no longer picks a D-Class");
            Helper.assertTrue(Each.Picks().test(Operative), Each.Name() + " ignores an MTF operative");
        }
        tidy(List.of(Prisoner, Abel, Stalker, Shade, Mass, Beast, Burning, God));
        dismiss(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "mtf_targets_people")
    public static void operativesStayOutOfDClassFeatures(GameTestHelper Helper) {
        MtfTestPlayer User = caller(Helper, "mtf_targets_roles");
        Entity Operative = operative(Helper, User, 12.5, 12.5);
        Helper.assertTrue(ControlBodies.isPersonLike(Operative), "an MTF operative is not treated as a person");
        Helper.assertFalse(Operative instanceof DClass, "an MTF operative counts as a D-Class");
        Helper.assertFalse(Operative instanceof Controllable, "an MTF operative can be taken over like a D-Class");
        Helper.assertTrue(Operative instanceof ControlRefusing, "an MTF operative does not refuse the controller");
        Helper.assertFalse(MtfRegistry.OperativeType.get().canSerialize(), "MTF operatives would be saved to disk");
        dismiss(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "mtf_targets_bite")
    public static void plagueZombiesBiteOperatives(GameTestHelper Helper) {
        MtfTestPlayer User = caller(Helper, "mtf_targets_bite");
        MtfOperative Operative = operative(Helper, User, 3.5, 9.5);
        DClass Prisoner = prisoner(Helper, 19.5, 9.5);
        Scp008Zombie ForOperative = Helper.spawn(Scp008Registry.ZombieType.get(), new Vec3(3.5, Floor, 5.5));
        Scp008Zombie ForPrisoner = Helper.spawn(Scp008Registry.ZombieType.get(), new Vec3(19.5, Floor, 5.5));
        boolean[] Hunted = {false, false};
        Helper.onEachTick(() -> {
            Hunted[0] |= ForOperative.getTarget() == Operative;
            Hunted[1] |= ForPrisoner.getTarget() == Prisoner;
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Hunted[1], "an SCP-008 zombie never went after a D-Class");
            Helper.assertTrue(Hunted[0], "an SCP-008 zombie never went after an MTF operative");
            Helper.assertTrue(Scp008Infection.isInfected(Prisoner), "the bitten D-Class was not infected");
            Helper.assertTrue(Scp008Infection.isInfected(Operative), "the bitten MTF operative was not infected");
            Scp008Infection.cure(Prisoner);
            Scp008Infection.cure(Operative);
            tidy(List.of(ForOperative, ForPrisoner, Prisoner));
            dismiss(Helper, User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "mtf_targets_spawn")
    public static void bloodborneSpawnsHuntOperatives(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MtfTestPlayer User = caller(Helper, "mtf_targets_spawn");
        MtfOperative Operative = operative(Helper, User, 3.5, 9.5);
        DClass Prisoner = prisoner(Helper, 19.5, 9.5);
        Scp016Spawn ForOperative = Scp016Events.raise(World, Scp016Registry.SkitterType.get(), Helper.absoluteVec(new Vec3(3.5, Floor, 5.5)), 0, false);
        Scp016Spawn ForPrisoner = Scp016Events.raise(World, Scp016Registry.SkitterType.get(), Helper.absoluteVec(new Vec3(19.5, Floor, 5.5)), 0, false);
        Helper.assertTrue(ForOperative != null && ForPrisoner != null, "the SCP-016 spawns could not be raised");
        boolean[] Hunted = {false, false};
        Helper.onEachTick(() -> {
            Hunted[0] |= ForOperative.getTarget() == Operative;
            Hunted[1] |= ForPrisoner.getTarget() == Prisoner;
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Hunted[1], "an SCP-016 spawn never went after a D-Class");
            Helper.assertTrue(Hunted[0], "an SCP-016 spawn never went after an MTF operative");
            Scp016Affliction.cure(Prisoner);
            Scp016Affliction.cure(Operative);
            tidy(List.of(ForOperative, ForPrisoner, Prisoner));
            dismiss(Helper, User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "mtf_targets_duel")
    public static void duelistsChallengeArmouredOperatives(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MtfTestPlayer User = caller(Helper, "mtf_targets_duel");
        MtfOperative Operative = operative(Helper, User, 6.5, 12.5);
        DClass Prisoner = prisoner(Helper, 15.5, 12.5);
        Scp016Spawn Fencer = Scp016Events.raise(World, Scp016Registry.DuelistType.get(), Helper.absoluteVec(new Vec3(11.0, Floor, 8.5)), 0, false);
        Helper.assertTrue(Fencer != null, "the SCP-016 duelist could not be raised");
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Scp016Duelist.threat(Operative) > Scp016Duelist.threat(Prisoner) + Scp016Duelist.ChallengeMargin,
                    "an armoured MTF operative is not more of a threat than a D-Class");
            Helper.assertTrue(Fencer.getTarget() == Operative, "the SCP-016 duelist did not challenge the armoured MTF operative");
            Scp016Affliction.cure(Prisoner);
            Scp016Affliction.cure(Operative);
            tidy(List.of(Fencer, Prisoner));
            dismiss(Helper, User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 240, batch = "mtf_targets_shadow")
    public static void shadowTakesOperativesInTheDark(GameTestHelper Helper) {
        MtfTestPlayer User = caller(Helper, "mtf_targets_shadow");
        darkRoom(Helper, 1, 1);
        darkRoom(Helper, 14, 1);
        List<Entity> Spawned = new ArrayList<>();
        MtfOperative[] Operative = new MtfOperative[1];
        DClass[] Prisoner = new DClass[1];
        Helper.runAfterDelay(Settle, () -> {
            Operative[0] = operative(Helper, User, 4.5, 5.9);
            Prisoner[0] = prisoner(Helper, 17.5, 5.9);
            Spawned.add(Prisoner[0]);
            Spawned.add(hunter(Helper, 4.5, 4.5));
            Spawned.add(hunter(Helper, 17.5, 4.5));
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Operative[0] != null && Prisoner[0] != null, "the room is not ready yet");
            Helper.assertTrue(consumed(Prisoner[0]), "SCP-017 did not take a D-Class in the dark");
            Helper.assertTrue(consumed(Operative[0]), "SCP-017 did not take an MTF operative in the dark");
            tidy(Spawned);
            dismiss(Helper, User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "mtf_targets_silence")
    public static void silenceReachesOperatives(GameTestHelper Helper) {
        MtfTestPlayer User = caller(Helper, "mtf_targets_silence");
        MtfOperative Operative = operative(Helper, User, Corner - 2.5, Corner);
        DClass Prisoner = prisoner(Helper, Corner, Corner - 2.5);
        List<LivingEntity> Caught = Scp444Spread.speak(User);
        Helper.assertTrue(Caught.contains(Prisoner) && Scp444Affliction.isAfflicted(Prisoner), "SCP-444 no longer reaches a D-Class");
        Helper.assertTrue(Caught.contains(Operative) && Scp444Affliction.isAfflicted(Operative), "SCP-444 did not reach an MTF operative");
        Scp444Affliction.clear(Prisoner);
        Scp444Affliction.clear(Operative);
        tidy(List.of(Prisoner));
        dismiss(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "mtf_targets_recall")
    public static void recalledOperativesLeaveNothingBehind(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MtfTestPlayer User = caller(Helper, "mtf_targets_recall");
        List<MtfOperative> Squad = deploy(Helper, User);
        DClass Prisoner = prisoner(Helper, 6.5, 6.5);
        BlockPos Hive = Helper.absolutePos(new BlockPos(12, 2, 12));
        List<Check> Infections = List.of(
                new Check("SCP-008", Scp008Infection::infect),
                new Check("SCP-016", Scp016Affliction::afflict),
                new Check("SCP-020", Scp020Affliction::afflict),
                new Check("SCP-098", Target -> Scp098Host.infect(Target, World, Hive)),
                new Check("SCP-129", Target -> Scp129Infection.infect(Target, Scp129Strain.One)),
                new Check("SCP-149", Scp149Host::infect),
                new Check("SCP-217", Scp217Clockwork::infect),
                new Check("SCP-253", Scp253Infection::infect),
                new Check("SCP-409", Scp409Infection::infect),
                new Check("SCP-444", Scp444Affliction::afflict));
        for (Check Each : Infections) {
            Helper.assertTrue(Each.Picks().test(Prisoner), Each.Name() + " no longer infects a D-Class");
            for (MtfOperative Member : Squad) {
                Helper.assertTrue(Each.Picks().test(Member), Each.Name() + " did not infect an MTF operative");
            }
        }
        Scp407Doom Doom = Scp407Doom.get(World.getServer());
        for (MtfOperative Member : Squad) {
            Scp093Visions.afflict(World, Member);
            Doom.add(Member.getUUID());
        }
        MtfMissions.cancel(World.getServer(), User.getUUID());
        Helper.runAfterDelay(2, () -> {
            for (MtfOperative Member : Squad) {
                Helper.assertTrue(Member.isRemoved(), "an infected MTF operative was not recalled");
                Helper.assertTrue(World.getEntity(Member.getUUID()) == null, "a recalled MTF operative is still in the world");
                Helper.assertFalse(Doom.isPending(Member.getUUID()), "SCP-407 still waits for a recalled MTF operative");
            }
        });
        Helper.runAfterDelay(60, () -> {
            Helper.assertTrue(squad(Helper, User).isEmpty(), "a recalled MTF operative came back");
            Helper.assertTrue(MtfMissions.get(User.getUUID()) == null, "the recalled mission is still running");
            Scp008Infection.cure(Prisoner);
            Scp016Affliction.cure(Prisoner);
            Scp020Affliction.cure(Prisoner);
            Scp129Infection.cure(Prisoner);
            Scp149Host.cure(Prisoner);
            Scp217Clockwork.cure(Prisoner);
            Scp253Infection.cure(Prisoner);
            Scp409Infection.cure(Prisoner);
            Scp098Host.cure(Prisoner);
            Scp444Affliction.clear(Prisoner);
            tidy(List.of(Prisoner));
            dismiss(Helper, User);
            Helper.succeed();
        });
    }
}
