package net.rai.scp_unleashed.anomaly.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.daevitegod.DaeviteGod;
import net.rai.scp_unleashed.daevitegod.DaeviteGodRegistry;
import net.rai.scp_unleashed.daevitegod.DaeviteGodTargets;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassCrime;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.dclass.DClassTrait;
import net.rai.scp_unleashed.dclass.duty.DClassGaze;
import net.rai.scp_unleashed.labware.LabExposure;
import net.rai.scp_unleashed.raid.RaidDirector;
import net.rai.scp_unleashed.scp001.Scp001Prey;
import net.rai.scp_unleashed.scp001.Scp001Prototype;
import net.rai.scp_unleashed.scp001.Scp001Registry;
import net.rai.scp_unleashed.scp008.Scp008Infection;
import net.rai.scp_unleashed.scp008.Scp008Registry;
import net.rai.scp_unleashed.scp008.Scp008Zombie;
import net.rai.scp_unleashed.scp009.Scp009Doom;
import net.rai.scp_unleashed.scp009.Scp009Exposure;
import net.rai.scp_unleashed.scp009.Scp009Registry;
import net.rai.scp_unleashed.scp012.Scp012Exposure;
import net.rai.scp_unleashed.scp016.Scp016Affliction;
import net.rai.scp_unleashed.scp016.Scp016Spawn;
import net.rai.scp_unleashed.scp017.Scp017Shadow;
import net.rai.scp_unleashed.scp020.Scp020Affliction;
import net.rai.scp_unleashed.scp020.Scp020Infection;
import net.rai.scp_unleashed.scp020.Scp020Mold;
import net.rai.scp_unleashed.scp027.Scp027Goals;
import net.rai.scp_unleashed.scp031.Scp031Targets;
import net.rai.scp_unleashed.scp035.Scp035Consume;
import net.rai.scp_unleashed.scp035.Scp035Sight;
import net.rai.scp_unleashed.scp049.Scp049Persons;
import net.rai.scp_unleashed.scp058.Scp058Heart;
import net.rai.scp_unleashed.scp075.Scp075Snail;
import net.rai.scp_unleashed.scp076.Scp076Abel;
import net.rai.scp_unleashed.scp076.Scp076Registry;
import net.rai.scp_unleashed.scp079.Scp079Exposure;
import net.rai.scp_unleashed.scp087.Scp087Hunter;
import net.rai.scp_unleashed.scp087.Scp087Registry;
import net.rai.scp_unleashed.scp093.Scp093Torso;
import net.rai.scp_unleashed.scp096.Scp096Persons;
import net.rai.scp_unleashed.scp096.Scp096Registry;
import net.rai.scp_unleashed.scp096.Scp096ShyGuy;
import net.rai.scp_unleashed.scp096.Scp096Sight;
import net.rai.scp_unleashed.scp098.Scp098Host;
import net.rai.scp_unleashed.scp106.Scp106Targeting;
import net.rai.scp_unleashed.scp129.Scp129Infection;
import net.rai.scp_unleashed.scp131.Scp131Pod;
import net.rai.scp_unleashed.scp149.Scp149Fly;
import net.rai.scp_unleashed.scp149.Scp149Host;
import net.rai.scp_unleashed.scp173.Scp173Registry;
import net.rai.scp_unleashed.scp173.Scp173Sight;
import net.rai.scp_unleashed.scp173.Scp173Statue;
import net.rai.scp_unleashed.scp178.Scp178Watchers;
import net.rai.scp_unleashed.scp205.Scp205Devil;
import net.rai.scp_unleashed.scp217.Scp217Clockwork;
import net.rai.scp_unleashed.scp224.Scp224Sight;
import net.rai.scp_unleashed.scp253.Scp253Infection;
import net.rai.scp_unleashed.scp280.Scp280Registry;
import net.rai.scp_unleashed.scp280.Scp280Shade;
import net.rai.scp_unleashed.scp303.Scp303Sight;
import net.rai.scp_unleashed.scp323.Scp323Hosts;
import net.rai.scp_unleashed.scp323.Scp323Sight;
import net.rai.scp_unleashed.scp337.Scp337Mass;
import net.rai.scp_unleashed.scp337.Scp337Quarry;
import net.rai.scp_unleashed.scp337.Scp337Registry;
import net.rai.scp_unleashed.scp352.Scp352Crone;
import net.rai.scp_unleashed.scp352.Scp352Prey;
import net.rai.scp_unleashed.scp352.Scp352Registry;
import net.rai.scp_unleashed.scp354.Scp354Prey;
import net.rai.scp_unleashed.scp354.beasts.Scp354Beast;
import net.rai.scp_unleashed.scp354.beasts.Scp354BeastRegistry;
import net.rai.scp_unleashed.scp407.Scp407Hearing;
import net.rai.scp_unleashed.scp407.Scp407Titan;
import net.rai.scp_unleashed.scp409.Scp409Exposure;
import net.rai.scp_unleashed.scp409.Scp409Infection;
import net.rai.scp_unleashed.scp427.Scp427Flesh;
import net.rai.scp_unleashed.scp427.Scp427Prey;
import net.rai.scp_unleashed.scp427.Scp427Registry;
import net.rai.scp_unleashed.scp428.Scp428Quarry;
import net.rai.scp_unleashed.scp439.Scp439Brute;
import net.rai.scp_unleashed.scp439.Scp439Creature;
import net.rai.scp_unleashed.scp439.Scp439Hive;
import net.rai.scp_unleashed.scp439.Scp439Infection;
import net.rai.scp_unleashed.scp439.Scp439Queen;
import net.rai.scp_unleashed.scp444.Scp444Affliction;
import net.rai.scp_unleashed.scp457.Scp457;
import net.rai.scp_unleashed.scp457.Scp457Goals;
import net.rai.scp_unleashed.scp457.Scp457Registry;
import net.rai.scp_unleashed.security.TurretTargeting;
import net.rai.scp_unleashed.stranger.StrangerEncounters;
import net.rai.scp_unleashed.z0003.Z0003Petrify;
import net.rai.scp_unleashed.z0003.Z0003VialItem;
import net.rai.scp_unleashed.z0003.gametest.Z0003TestPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class StatueTargetGameTests {
    public static final String Arena = "mtf_arena";
    public static final String DutyArena = "dclassroles_arena";
    public static final String BurnArena = "scp457_arena";
    public static final String HiveArena = "scp439_arena";
    public static final String MouldArena = "scp020_arena";
    public static final double Floor = 2.0;
    public static final int Watch = 60;
    public static final int LightTicks = 5;
    public static final int Held = 100000;
    public static final float Wounded = 4.0f;

    private StatueTargetGameTests() {
    }

    private record Check(String Name, Predicate<LivingEntity> Picks) {
    }

    private static int stand(GameTestHelper Helper) {
        for (int Y = 0; Y < 5; Y++) {
            if (Helper.getBlockState(new BlockPos(1, Y, 1)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(1, Y + 1, 1)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
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

    private static void clearSkies(ServerLevel World) {
        World.setWeatherParameters(20000000, 0, false, false);
        World.setRainLevel(0.0f);
        World.setThunderLevel(0.0f);
    }

    private static DClass person(GameTestHelper Helper, double X, double Y, double Z) {
        DClass Made = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(X, Y, Z));
        Made.identify(DClassTrait.Stoic, DClassCrime.Smuggling, 0);
        Made.assignNumber();
        return Made;
    }

    private static Villager villager(GameTestHelper Helper, double X, double Y, double Z) {
        return Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(X, Y, Z));
    }

    private static Z0003TestPlayer player(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer User = new Z0003TestPlayer(World, Name);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, Entity Subject) {
        if (Subject.isRemoved()) {
            return;
        }
        if (Subject instanceof Z0003TestPlayer User) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        } else {
            Subject.discard();
        }
    }

    private static void tidy(GameTestHelper Helper, List<? extends Entity> Spawned) {
        for (Entity Each : Spawned) {
            dismiss(Helper, Each);
        }
    }

    private static void stone(GameTestHelper Helper, LivingEntity Body) {
        Helper.assertTrue(Z0003Petrify.petrify(Body), "could not turn " + Body.getName().getString() + " to stone");
        Helper.assertTrue(Targeting.isStatue(Body), Body.getName().getString() + " is not a statue after petrifying");
    }

    private static void pour(GameTestHelper Helper, LivingEntity Body) {
        FakePlayer Rescuer = new FakePlayer(Helper.getLevel(), new GameProfile(UUID.randomUUID(), "statue_rescuer"));
        Rescuer.setItemInHand(InteractionHand.MAIN_HAND, Z0003VialItem.create(1));
        Helper.assertTrue(Z0003VialItem.pour(Rescuer, InteractionHand.MAIN_HAND, Body).consumesAction(), "the vial did not crack " + Body.getName().getString());
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

    private static Scp173Statue sculpture(GameTestHelper Helper, double X, double Z) {
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

    private static void clearHive(GameTestHelper Helper) {
        AABB Area = Helper.getBounds().inflate(24.0);
        for (Entity Found : Helper.getLevel().getEntitiesOfClass(Entity.class, Area, Found -> Found instanceof Scp439Creature || Found instanceof Scp439Hive)) {
            Found.discard();
        }
    }

    private static void checkAll(GameTestHelper Helper, LivingEntity Body, String Who, List<Check> Checks) {
        for (Check Each : Checks) {
            Helper.assertTrue(Each.Picks().test(Body), Each.Name() + " does not pick " + Who + " before it is stone");
        }
        stone(Helper, Body);
        for (Check Each : Checks) {
            Helper.assertFalse(Each.Picks().test(Body), Each.Name() + " still picks " + Who + " made of stone");
        }
        Helper.assertTrue(Z0003Petrify.free(Body), "could not free " + Who);
        for (Check Each : Checks) {
            Helper.assertTrue(Each.Picks().test(Body), Each.Name() + " does not pick " + Who + " again once freed");
        }
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "statue_targets_people")
    public static void statuesFallOutOfEveryPeopleCheck(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Helper.setBlock(new BlockPos(10, (int) Floor, 6), Blocks.LIGHT);
        DClass Prisoner = person(Helper, 10.5, Floor, 6.5);
        Scp001Prototype Prototype = Scp001Registry.Prototype.get().create(World);
        Scp076Abel Abel = Scp076Registry.Abel.get().create(World);
        Scp087Hunter Stalker = Scp087Registry.Hunter.get().create(World);
        Scp280Shade Shade = Scp280Registry.Shade.get().create(World);
        Scp337Mass Mass = Scp337Registry.Mass.get().create(World);
        Scp352Crone Crone = Scp352Registry.Crone.get().create(World);
        Scp354Beast Beast = Scp354BeastRegistry.Brute.get().create(World);
        Scp427Flesh Flesh = Scp427Registry.Flesh.get().create(World);
        Scp457 Burning = Scp457Registry.Burning.get().create(World);
        DaeviteGod God = DaeviteGodRegistry.God.get().create(World);
        List<Entity> Hunters = new ArrayList<>(List.of(Prototype, Abel, Stalker, Shade, Mass, Crone, Beast, Flesh, Burning, God));
        for (Entity Each : Hunters) {
            Vec3 Spot = Helper.absoluteVec(new Vec3(14.5, Floor, 6.5));
            Each.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        }
        List<Check> Checks = List.of(
                new Check("SCP-001 prey", Target -> Scp001Prey.prey(Prototype, Target)),
                new Check("SCP-008 infection", Scp008Infection::canCatch),
                new Check("SCP-016 quarry", Scp016Spawn::isQuarry),
                new Check("SCP-016 affliction", Scp016Affliction::canCatch),
                new Check("SCP-017 hunt", Scp017Shadow::huntable),
                new Check("SCP-020 mould", Scp020Affliction::canCatch),
                new Check("SCP-027 swarm", Scp027Goals::isPerson),
                new Check("SCP-031 embrace", Scp031Targets::isValid),
                new Check("SCP-035 mask", Scp035Consume::canConsume),
                new Check("SCP-049 cure", Target -> Scp049Persons.isPerson(Target, false)),
                new Check("SCP-058 hunt", Scp058Heart::isPrey),
                new Check("SCP-075 hunt", Scp075Snail::isQuarry),
                new Check("SCP-076 hunt", Target -> Scp076Abel.isPrey(Abel, Target)),
                new Check("SCP-079 exposure", Scp079Exposure::isSubject),
                new Check("SCP-087 hunt", Target -> Scp087Hunter.isPrey(Stalker, Target)),
                new Check("SCP-093 hunt", Scp093Torso::isQuarry),
                new Check("SCP-096 witness", Scp096Persons::isWitness),
                new Check("SCP-096 sight", Scp096Persons::canWatch),
                new Check("SCP-096 chase", Scp096Persons::huntable),
                new Check("SCP-098 spores", Scp098Host::canCarry),
                new Check("SCP-106 hunt", Target -> Scp106Targeting.rank(World, Target) != Scp106Targeting.RankNone),
                new Check("SCP-129 strains", Scp129Infection::canCatch),
                new Check("SCP-131 pods", Scp131Pod::isPerson),
                new Check("SCP-149 flies", Scp149Fly::isQuarry),
                new Check("SCP-149 host", Scp149Host::canHost),
                new Check("SCP-173 watcher", DClassGaze::watcher),
                new Check("SCP-205 devil", Scp205Devil::prey),
                new Check("SCP-217 clockwork", Scp217Clockwork::canCatch),
                new Check("SCP-224 clock", Target -> !Scp224Sight.isExempt(Target)),
                new Check("SCP-253 growth", Scp253Infection::canCatch),
                new Check("SCP-280 shade", Target -> Scp280Shade.huntable(Shade, Target)),
                new Check("SCP-303 witness", Scp303Sight::person),
                new Check("SCP-323 skull", Scp323Hosts::canHost),
                new Check("SCP-337 lash", Target -> Scp337Quarry.isQuarry(Mass, Target)),
                new Check("SCP-352 prey", Target -> Scp352Prey.prey(Crone, Target)),
                new Check("SCP-354 lake", Scp354Prey::isPrey),
                new Check("SCP-354 beasts", Target -> Scp354Beast.isQuarry(Beast, Target)),
                new Check("SCP-407 song", Scp407Hearing::counts),
                new Check("SCP-407 titan", Scp407Titan::isQuarry),
                new Check("SCP-409 crystal", Scp409Exposure::canCatch),
                new Check("SCP-427 flesh", Target -> Scp427Prey.prey(Flesh, Target)),
                new Check("SCP-428 quarry", Scp428Quarry::isQuarry),
                new Check("SCP-439 brutes", Scp439Brute::isIntruder),
                new Check("SCP-439 queen", Scp439Queen::canHost),
                new Check("SCP-457 hunt", Target -> Scp457Goals.isPrey(Burning, Target)),
                new Check("Turrets and guards", TurretTargeting::eligible),
                new Check("Daevite God", Target -> DaeviteGodTargets.isPrey(God, Target)),
                new Check("Infection immunity", Target -> !InfectionImmunity.isImmune(Target)));
        Helper.runAfterDelay(LightTicks, () -> {
            checkAll(Helper, Prisoner, "a D-Class", Checks);
            for (Entity Each : Hunters) {
                Each.discard();
            }
            dismiss(Helper, Prisoner);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "statue_targets_players")
    public static void stonePlayersFallOutOfEveryPlayerCheck(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer User = player(Helper, 6.5, 6.5, "statue_checks");
        Scp457 Burning = Scp457Registry.Burning.get().create(World);
        List<Check> Checks = List.of(
                new Check("SCP-012 composition", Target -> !Scp012Exposure.isExempt(User)),
                new Check("SCP-017 hunt", Scp017Shadow::huntable),
                new Check("SCP-035 lure", Target -> Scp035Sight.isDrawable(User)),
                new Check("SCP-096 witness", Scp096Persons::isWitness),
                new Check("SCP-106 hunt", Target -> Scp106Targeting.rank(World, Target) != Scp106Targeting.RankNone),
                new Check("SCP-178 watchers", Scp178Watchers::eligible),
                new Check("SCP-224 clock", Target -> !Scp224Sight.isExempt(Target)),
                new Check("SCP-303 viewer", Target -> Scp303Sight.viewer(User)),
                new Check("SCP-323 lure", Target -> Scp323Sight.isDrawable(User)),
                new Check("SCP-409 crystal", Scp409Exposure::canCatch),
                new Check("SCP-439 queen", Scp439Queen::canHost),
                new Check("SCP-457 hunt", Target -> Scp457Goals.isPrey(Burning, Target)),
                new Check("Raids", Target -> RaidDirector.defender(User)),
                new Check("The stranger", Target -> StrangerEncounters.isValidReader(User)),
                new Check("Turrets and guards", TurretTargeting::eligible),
                new Check("Infection immunity", Target -> !InfectionImmunity.isImmune(Target)));
        checkAll(Helper, User, "a player", Checks);
        Burning.discard();
        dismiss(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = BurnArena, timeoutTicks = 500, batch = "statue_targets_457")
    public static void scp457IgnoresStoneVillager(GameTestHelper Helper) {
        clearSkies(Helper.getLevel());
        int Ground = stand(Helper);
        Scp457 Burning = Helper.spawn(Scp457Registry.Burning.get(), new Vec3(8.5, Ground, 16.5));
        Villager Stone = villager(Helper, 8.5, Ground, 12.5);
        stone(Helper, Stone);
        Helper.startSequence()
                .thenExecuteFor(Watch, () -> {
                    Helper.assertTrue(Burning.getTarget() != Stone, "SCP-457 went after a villager made of stone");
                    Helper.assertFalse(Scp457Goals.isPrey(Burning, Stone), "SCP-457 counts a stone villager as prey");
                })
                .thenExecute(() -> pour(Helper, Stone))
                .thenWaitUntil(() -> Helper.assertFalse(Targeting.isStatue(Stone), "the villager is still stone"))
                .thenWaitUntil(() -> Helper.assertTrue(Burning.getTarget() == Stone, "SCP-457 did not go after the villager once it was freed"))
                .thenExecute(() -> tidy(Helper, List.of(Burning, Stone)))
                .thenSucceed();
    }

    @GameTest(template = HiveArena, timeoutTicks = 700, batch = "statue_targets_439")
    public static void queenIgnoresStoneVillager(GameTestHelper Helper) {
        Villager Stone = villager(Helper, 12.5, Floor, 8.5);
        stone(Helper, Stone);
        Scp439Queen Queen = Scp439Hive.release(Helper.getLevel(), Helper.absoluteVec(new Vec3(8.5, Floor, 8.5)), 270.0f, new CompoundTag());
        Helper.assertTrue(Queen != null, "the queen could not be released");
        Helper.assertFalse(Scp439Queen.canHost(Stone), "a stone villager can host SCP-439");
        Helper.assertTrue(Queen.choosePrey() != Stone, "the queen picked a stone villager");
        Helper.startSequence()
                .thenExecuteFor(Watch, () -> {
                    Helper.assertFalse(Scp439Infection.isInfected(Stone), "the queen infected a stone villager");
                    Helper.assertTrue(Queen.prey() != Stone, "the queen is hunting a stone villager");
                })
                .thenExecute(() -> pour(Helper, Stone))
                .thenWaitUntil(() -> Helper.assertFalse(Targeting.isStatue(Stone), "the villager is still stone"))
                .thenWaitUntil(() -> Helper.assertTrue(Scp439Infection.isInfected(Stone), "the queen never went after the villager once it was freed"))
                .thenExecute(() -> {
                    clearHive(Helper);
                    dismiss(Helper, Stone);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "statue_targets_008")
    public static void plagueZombieIgnoresStoneVillager(GameTestHelper Helper) {
        Villager Stone = villager(Helper, 6.5, Floor, 9.5);
        stone(Helper, Stone);
        Scp008Zombie Zombie = Helper.spawn(Scp008Registry.ZombieType.get(), new Vec3(6.5, Floor, 5.5));
        Helper.startSequence()
                .thenExecuteFor(Watch, () -> Helper.assertTrue(Zombie.getTarget() != Stone, "an SCP-008 zombie went after a villager made of stone"))
                .thenExecute(() -> pour(Helper, Stone))
                .thenWaitUntil(() -> Helper.assertFalse(Targeting.isStatue(Stone), "the villager is still stone"))
                .thenWaitUntil(() -> Helper.assertTrue(Zombie.getTarget() == Stone, "the SCP-008 zombie did not go after the villager once it was freed"))
                .thenExecute(() -> {
                    Scp008Infection.cure(Stone);
                    tidy(Helper, List.of(Zombie, Stone));
                })
                .thenSucceed();
    }

    @GameTest(template = DutyArena, timeoutTicks = 900, batch = "statue_targets_173")
    public static void stoneDClassDoesNotHold173(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        Scp173Statue Sculpture = sculpture(Helper, 12.5, 6.5);
        DClass Victim = person(Helper, 12.5, Floor, 9.0);
        DClass Watcher = person(Helper, 17.5, Floor, 13.5);
        face(Victim, Helper.absoluteVec(new Vec3(12.5, 3.6, 22.5)));
        face(Watcher, Sculpture.sightBox().getCenter());
        DClassGaze.holdEyes(Watcher, Held);
        Vec3 Start = Sculpture.position();
        Helper.startSequence()
                .thenExecuteFor(20, () -> Helper.assertTrue(Victim.isAlive(), "SCP-173 killed a D-Class while another D-Class watched it"))
                .thenExecute(() -> {
                    Helper.assertTrue(Scp173Sight.isObserved(World, Sculpture), "the watching D-Class does not hold SCP-173 before it is stone");
                    stone(Helper, Watcher);
                    Helper.assertFalse(DClassGaze.watcher(Watcher), "a stone D-Class still counts as a watcher");
                    Helper.assertFalse(Scp173Sight.personObservers(World, Sculpture).contains(Watcher), "a stone D-Class is listed as watching SCP-173");
                    Helper.assertFalse(Scp173Sight.isObserved(World, Sculpture), "a stone D-Class still holds SCP-173");
                })
                .thenWaitUntil(() -> Helper.assertTrue(!Victim.isAlive(), "SCP-173 stayed put although its only watcher was stone"))
                .thenExecute(() -> {
                    Helper.assertTrue(Sculpture.position().distanceTo(Start) > 0.5, "SCP-173 killed without moving");
                    Helper.assertTrue(Watcher.isAlive() && Targeting.isStatue(Watcher), "SCP-173 broke the stone D-Class");
                    tidy(Helper, List.of(Sculpture, Watcher));
                })
                .thenSucceed();
    }

    @GameTest(template = DutyArena, timeoutTicks = 1200, batch = "statue_targets_096")
    public static void stoneDClassDoesNotTrigger096(GameTestHelper Helper) {
        quiet(Helper);
        Scp096ShyGuy Guy = Helper.spawn(Scp096Registry.ShyGuy.get(), new Vec3(12.5, Floor, 17.5));
        DClass Viewer = person(Helper, 12.5, Floor, 9.5);
        face(Viewer, Scp096Sight.faceOf(Guy));
        stone(Helper, Viewer);
        int[] Shown = {0};
        Helper.onEachTick(() -> {
            if (!Guy.isCalm()) {
                return;
            }
            if (!Guy.canBeSeen()) {
                Guy.forceCalm(Helper.getLevel());
            }
            face(Viewer, Scp096Sight.faceOf(Guy));
            face(Guy, Viewer.getEyePosition());
            if (Guy.canBeSeen() && Targeting.isStatue(Viewer) && Scp096Sight.clear(Helper.getLevel(), Viewer.getEyePosition(), Scp096Sight.faceOf(Guy))) {
                Shown[0]++;
            }
        });
        Helper.startSequence()
                .thenExecuteFor(Watch * 2, () -> {
                    Helper.assertFalse(Scp096Persons.isWitness(Viewer), "a stone D-Class counts as a witness to SCP-096");
                    Helper.assertTrue(Guy.isCalm(), "a stone D-Class looking at SCP-096's face set it off");
                    Helper.assertFalse(Guy.targetKeys().contains(Viewer.getUUID()), "SCP-096 marked a stone D-Class as a target");
                })
                .thenExecute(() -> Helper.assertTrue(Shown[0] >= Watch, "SCP-096 hid its face for most of the test, so the stone D-Class never really saw it: " + Shown[0]))
                .thenExecute(() -> pour(Helper, Viewer))
                .thenWaitUntil(() -> Helper.assertFalse(Targeting.isStatue(Viewer), "the D-Class is still stone"))
                .thenWaitUntil(() -> Helper.assertTrue(Guy.targetKeys().contains(Viewer.getUUID()), "the freed D-Class did not set SCP-096 off: calm " + Guy.isCalm()
                        + " seen " + Guy.canBeSeen() + " witness " + Scp096Persons.isWitness(Viewer) + " watch " + Scp096Persons.canWatch(Viewer) + " sees "
                        + Scp096Sight.npcSees(Helper.getLevel(), Viewer, Scp096Sight.faceOf(Guy), Scp096Sight.facingOf(Guy)) + " guy " + Guy.position() + " viewer " + Viewer.position()
                        + " yaw " + Viewer.getYRot() + " pitch " + Viewer.getXRot() + " alive " + Viewer.isAlive() + " shut " + Viewer.eyesShut()))
                .thenExecute(() -> tidy(Helper, List.of(Guy, Viewer)))
                .thenSucceed();
    }

    @GameTest(template = BurnArena, timeoutTicks = 400, batch = "statue_targets_457drop")
    public static void scp457DropsPetrifiedTarget(GameTestHelper Helper) {
        clearSkies(Helper.getLevel());
        int Ground = stand(Helper);
        Scp457 Burning = Helper.spawn(Scp457Registry.Burning.get(), new Vec3(8.5, Ground, 18.5));
        DClass Prisoner = person(Helper, 8.5, Ground, 11.5);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Burning.getTarget() == Prisoner, "SCP-457 never went after the D-Class"))
                .thenExecute(() -> stone(Helper, Prisoner))
                .thenExecuteAfter(2, () -> Helper.assertTrue(Burning.getTarget() != Prisoner, "SCP-457 kept chasing a D-Class turned to stone"))
                .thenExecuteFor(Watch, () -> {
                    Helper.assertTrue(Burning.getTarget() != Prisoner, "SCP-457 went back to a D-Class made of stone");
                    Helper.assertTrue(Prisoner.isAlive() && Targeting.isStatue(Prisoner), "the stone D-Class did not stay stone");
                })
                .thenExecute(() -> tidy(Helper, List.of(Burning, Prisoner)))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "statue_targets_008drop")
    public static void plagueZombieDropsPetrifiedTarget(GameTestHelper Helper) {
        Villager Victim = villager(Helper, 6.5, Floor, 13.5);
        Scp008Zombie Zombie = Helper.spawn(Scp008Registry.ZombieType.get(), new Vec3(6.5, Floor, 5.5));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Zombie.getTarget() == Victim, "the SCP-008 zombie never went after the villager"))
                .thenExecute(() -> stone(Helper, Victim))
                .thenExecuteAfter(2, () -> Helper.assertTrue(Zombie.getTarget() != Victim, "the SCP-008 zombie kept chasing a villager turned to stone"))
                .thenExecuteFor(Watch, () -> {
                    Helper.assertTrue(Zombie.getTarget() != Victim, "the SCP-008 zombie went back to a stone villager");
                    Helper.assertFalse(Scp008Infection.isInfected(Victim), "the SCP-008 zombie infected a stone villager");
                })
                .thenExecute(() -> tidy(Helper, List.of(Zombie, Victim)))
                .thenSucceed();
    }

    @GameTest(template = HiveArena, timeoutTicks = 200, batch = "statue_targets_439drop")
    public static void queenDropsPetrifiedPrey(GameTestHelper Helper) {
        Villager Victim = villager(Helper, 14.5, Floor, 8.5);
        Scp439Queen Queen = Scp439Hive.release(Helper.getLevel(), Helper.absoluteVec(new Vec3(8.5, Floor, 8.5)), 270.0f, new CompoundTag());
        Helper.assertTrue(Queen != null, "the queen could not be released");
        Queen.setPrey(Victim);
        Helper.assertTrue(Queen.prey() == Victim, "the queen did not take the villager as prey");
        stone(Helper, Victim);
        Helper.assertTrue(Queen.prey() == null, "the queen kept a stone villager as prey");
        Helper.startSequence()
                .thenExecuteFor(Watch, () -> Helper.assertFalse(Scp439Infection.isInfected(Victim), "the queen infected a villager turned to stone"))
                .thenExecute(() -> {
                    clearHive(Helper);
                    dismiss(Helper, Victim);
                })
                .thenSucceed();
    }

    @GameTest(template = DutyArena, timeoutTicks = 900, batch = "statue_targets_096drop")
    public static void scp096DropsPetrifiedTarget(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        Scp096ShyGuy Guy = Helper.spawn(Scp096Registry.ShyGuy.get(), new Vec3(12.5, Floor, 20.5));
        DClass Prisoner = person(Helper, 12.5, Floor, 4.5);
        Guy.forceRage(World, Prisoner);
        Helper.assertTrue(Guy.targetKeys().contains(Prisoner.getUUID()), "SCP-096 did not mark the D-Class");
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Guy.isRaging(), "SCP-096 never started raging"))
                .thenExecute(() -> stone(Helper, Prisoner))
                .thenWaitUntil(() -> Helper.assertTrue(Guy.isCalming() || Guy.isCalm(), "SCP-096 kept raging at a D-Class turned to stone"))
                .thenExecute(() -> {
                    Helper.assertTrue(Prisoner.isAlive() && Targeting.isStatue(Prisoner), "SCP-096 broke the stone D-Class");
                    tidy(Helper, List.of(Guy, Prisoner));
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "statue_targets_immune")
    public static void stonePeopleCatchNothing(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer User = player(Helper, 6.5, 6.5, "statue_immune");
        DClass Prisoner = person(Helper, 9.5, Floor, 6.5);
        DClass Carrier = person(Helper, 12.5, Floor, 6.5);
        stone(Helper, User);
        stone(Helper, Prisoner);
        Helper.assertTrue(Scp409Infection.infect(Carrier), "the carrier could not catch SCP-409");
        for (LivingEntity Body : List.<LivingEntity>of(User, Prisoner)) {
            String Name = Body instanceof Z0003TestPlayer ? "a stone player" : "a stone D-Class";
            Helper.assertTrue(InfectionImmunity.isImmune(Body), Name + " is not immune to infection");
            Helper.assertFalse(Scp008Infection.canCatch(Body) || Scp008Infection.infect(Body), Name + " caught SCP-008");
            Helper.assertFalse(Scp409Exposure.touchCrystal(Body) || Scp409Exposure.pass(Carrier, Body) || Scp409Infection.infect(Body), Name + " caught SCP-409");
            Helper.assertFalse(Scp009Doom.doom(Body), Name + " was doomed by SCP-009");
            Helper.assertFalse(Scp020Affliction.canCatch(Body) || Scp020Affliction.dose(Body), Name + " breathed in SCP-020");
        }
        Scp008Infection.exposeAround(World, User.getEyePosition(), LabExposure.NothingIgnored, 6.0);
        Helper.startSequence()
                .thenExecuteFor(Watch, () -> {
                    for (LivingEntity Body : List.<LivingEntity>of(User, Prisoner)) {
                        Helper.assertFalse(Scp008Infection.isInfected(Body), "a statue caught SCP-008 from the air");
                        Helper.assertFalse(Scp409Infection.isInfected(Body), "a statue caught SCP-409");
                        Helper.assertFalse(Scp009Doom.isDoomed(Body), "a statue was doomed by SCP-009");
                        Helper.assertFalse(Scp020Affliction.isAfflicted(Body), "a statue caught SCP-020");
                    }
                })
                .thenExecute(() -> {
                    Scp409Infection.cure(Carrier);
                    tidy(Helper, List.of(User, Prisoner, Carrier));
                })
                .thenSucceed();
    }

    @GameTest(template = MouldArena, timeoutTicks = 120, batch = "statue_targets_hazards")
    public static void statuesCatchNothingFromHazards(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer User = player(Helper, 9.5, 7.5, "statue_hazards");
        DClass Prisoner = person(Helper, 5.5, Floor, 7.5);
        DClass Control = person(Helper, 12.5, Floor, 7.5);
        List<BlockPos> Walls = List.of(new BlockPos(5, 2, 9), new BlockPos(9, 2, 9), new BlockPos(12, 2, 9));
        for (BlockPos Wall : Walls) {
            Helper.setBlock(Wall, Blocks.STONE);
            Helper.assertTrue(Scp020Infection.infect(World, Helper.absolutePos(Wall), Direction.NORTH), "SCP-020 could not be grown on the wall");
        }
        stone(Helper, User);
        stone(Helper, Prisoner);
        User.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Scp009Registry.Shard.get()));
        Vec3 Ice = Helper.absoluteVec(new Vec3(9.0, 3.0, 4.5));
        Helper.startSequence()
                .thenExecuteFor(Watch, () -> {
                    Scp009Exposure.expose(World, Ice, Pos -> false);
                    Scp009Doom.checkHeld(User);
                    for (LivingEntity Body : List.<LivingEntity>of(User, Prisoner, Control)) {
                        Scp409Exposure.touchCrystal(Body);
                        Scp020Mold.breathe(Body);
                    }
                    for (LivingEntity Body : List.<LivingEntity>of(User, Prisoner)) {
                        String Name = Body instanceof Z0003TestPlayer ? "a stone player" : "a stone D-Class";
                        Helper.assertFalse(Scp009Doom.isDoomed(Body), Name + " was doomed by red ice");
                        Helper.assertFalse(Scp409Infection.isInfected(Body), Name + " caught SCP-409 from its crystals");
                        Helper.assertFalse(Scp020Affliction.isAfflicted(Body), Name + " breathed in SCP-020 from the walls");
                    }
                })
                .thenExecute(() -> {
                    Helper.assertTrue(Scp009Doom.isDoomed(Control), "the living D-Class beside the statues was not doomed, so red ice never reached them");
                    Helper.assertTrue(Scp409Infection.isInfected(Control), "the living D-Class did not catch SCP-409, so the crystals never reached the statues");
                    Helper.assertTrue(Scp020Affliction.isAfflicted(Control), "the living D-Class did not breathe in SCP-020, so the walls never reached the statues");
                    Scp009Doom.clear(Control);
                    Scp409Infection.cure(Control);
                    Scp020Affliction.cure(Control);
                    for (BlockPos Wall : Walls) {
                        Scp020Infection.clear(World, Helper.absolutePos(Wall));
                        Helper.setBlock(Wall, Blocks.AIR);
                    }
                    tidy(Helper, List.of(User, Prisoner, Control));
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "statue_targets_cure")
    public static void petrifyingCuresLikeScp006(GameTestHelper Helper) {
        Z0003TestPlayer User = player(Helper, 6.5, 6.5, "statue_cure");
        DClass Prisoner = person(Helper, 12.5, Floor, 6.5);
        Helper.assertTrue(Scp008Infection.infect(User), "the player could not catch SCP-008");
        Helper.assertTrue(Scp409Infection.infect(User), "the player could not catch SCP-409");
        Helper.assertTrue(Scp444Affliction.afflict(User), "the player could not be silenced by SCP-444");
        Helper.assertTrue(Scp009Doom.doom(Prisoner), "the D-Class could not be doomed by SCP-009");
        Helper.assertTrue(Scp020Affliction.afflict(Prisoner), "the D-Class could not catch SCP-020");
        User.addEffect(new MobEffectInstance(MobEffects.POISON, 400));
        User.addEffect(new MobEffectInstance(MobEffects.HUNGER, 400));
        Prisoner.addEffect(new MobEffectInstance(MobEffects.WITHER, 400));
        Prisoner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400));
        User.setHealth(Wounded);
        Prisoner.setHealth(Wounded);
        stone(Helper, User);
        stone(Helper, Prisoner);
        checkCured(Helper, User, Prisoner, "the moment they were turned to stone");
        Helper.startSequence()
                .thenExecute(() -> {
                    pour(Helper, User);
                    pour(Helper, Prisoner);
                })
                .thenWaitUntil(() -> Helper.assertFalse(Targeting.isStatue(User) || Targeting.isStatue(Prisoner), "they are still stone"))
                .thenExecuteFor(20, () -> checkCured(Helper, User, Prisoner, "after being freed"))
                .thenExecute(() -> {
                    Scp444Affliction.clear(User);
                    tidy(Helper, List.of(User, Prisoner));
                })
                .thenSucceed();
    }

    private static void checkCured(GameTestHelper Helper, Z0003TestPlayer User, DClass Prisoner, String When) {
        Helper.assertFalse(Scp008Infection.isInfected(User), "SCP-008 was not cured " + When);
        Helper.assertFalse(Scp409Infection.isInfected(User) || Scp409Infection.hasModifiers(User), "SCP-409 was not cured " + When);
        Helper.assertTrue(Scp444Affliction.isAfflicted(User), "SCP-444's silence, which SCP-006 cannot lift, was lifted " + When);
        Helper.assertFalse(Scp009Doom.isDoomed(Prisoner), "SCP-009 was not cured " + When);
        Helper.assertFalse(Scp020Affliction.isAfflicted(Prisoner), "SCP-020 was not cured " + When);
        Helper.assertFalse(User.hasEffect(MobEffects.POISON) || User.hasEffect(MobEffects.HUNGER), "the player kept harmful effects " + When);
        Helper.assertFalse(Prisoner.hasEffect(MobEffects.WITHER) || Prisoner.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the D-Class kept harmful effects " + When);
        Helper.assertTrue(User.getHealth() >= User.getMaxHealth(), "the player was not healed " + When + ": " + User.getHealth());
        Helper.assertTrue(Prisoner.getHealth() >= Prisoner.getMaxHealth(), "the D-Class was not healed " + When + ": " + Prisoner.getHealth());
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "statue_targets_revive")
    public static void revivalRestoresHealth(GameTestHelper Helper) {
        Z0003TestPlayer User = player(Helper, 6.5, 6.5, "statue_revive");
        DClass Prisoner = person(Helper, 12.5, Floor, 6.5);
        stone(Helper, User);
        stone(Helper, Prisoner);
        for (LivingEntity Body : List.<LivingEntity>of(User, Prisoner)) {
            Body.setHealth(Wounded);
            Body.setRemainingFireTicks(200);
            Body.setTicksFrozen(200);
        }
        Helper.startSequence()
                .thenExecute(() -> {
                    pour(Helper, User);
                    pour(Helper, Prisoner);
                })
                .thenWaitUntil(() -> Helper.assertFalse(Targeting.isStatue(User) || Targeting.isStatue(Prisoner), "they are still stone"))
                .thenExecute(() -> {
                    for (LivingEntity Body : List.<LivingEntity>of(User, Prisoner)) {
                        String Name = Body instanceof Z0003TestPlayer ? "the freed player" : "the freed D-Class";
                        Helper.assertTrue(Body.getHealth() >= Body.getMaxHealth(), Name + " did not come back at full health: " + Body.getHealth());
                        Helper.assertTrue(Body.getRemainingFireTicks() <= 0, Name + " came back on fire");
                        Helper.assertTrue(Body.getTicksFrozen() <= 0, Name + " came back frozen");
                    }
                    tidy(Helper, List.of(User, Prisoner));
                })
                .thenSucceed();
    }
}
