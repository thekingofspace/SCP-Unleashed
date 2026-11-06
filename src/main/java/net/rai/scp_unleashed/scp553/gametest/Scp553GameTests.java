package net.rai.scp_unleashed.scp553.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassCrime;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.dclass.DClassTrait;
import net.rai.scp_unleashed.scp553.Scp553Butterfly;
import net.rai.scp_unleashed.scp553.Scp553Cave;
import net.rai.scp_unleashed.scp553.Scp553Colony;
import net.rai.scp_unleashed.scp553.Scp553Registry;
import net.rai.scp_unleashed.scp553.Scp553StalactiteBlockEntity;
import net.rai.scp_unleashed.scp553.Scp553World;
import net.rai.scp_unleashed.z0003.gametest.Z0003TestPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp553GameTests {
    public static final String Arena = "scp553_arena";
    public static final String Cave = "scp553_cave";
    public static final double Floor = 2.0;
    public static final BlockPos Hollow = new BlockPos(7, 5, 7);
    public static final BlockPos Nest = new BlockPos(7, 7, 7);
    public static final BlockPos HollowLow = new BlockPos(3, 3, 3);
    public static final BlockPos HollowHigh = new BlockPos(11, 7, 11);

    private Scp553GameTests() {
    }

    private static Scp553Butterfly butterfly(GameTestHelper Helper, double X, double Y, double Z) {
        Scp553Butterfly Made = Helper.spawn(Scp553Registry.Butterfly.get(), new Vec3(X, Y, Z));
        Made.setPersistenceRequired();
        Made.setBreedCooldown(Scp553Butterfly.BreedCooldown);
        return Made;
    }

    private static List<Scp553Butterfly> flock(GameTestHelper Helper, int Count, double X, double Y, double Z, double Spread) {
        List<Scp553Butterfly> Made = new ArrayList<>();
        for (int Index = 0; Index < Count; Index++) {
            double Angle = Index * Math.PI * 2.0 / Count;
            Made.add(butterfly(Helper, X + Math.cos(Angle) * Spread, Y + (Index % 3) * 0.4, Z + Math.sin(Angle) * Spread));
        }
        return Made;
    }

    private static Z0003TestPlayer player(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer User = new Z0003TestPlayer(World, Name);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        whenDone(Helper, () -> dismiss(Helper, User));
        return User;
    }

    private static DClass prisoner(GameTestHelper Helper, double X, double Z) {
        DClass Made = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(X, Floor, Z));
        Made.identify(DClassTrait.Stoic, DClassCrime.Smuggling, 0);
        Made.assignNumber();
        return Made;
    }

    private static void whenDone(GameTestHelper Helper, Runnable Cleanup) {
        Helper.testInfo.addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo Info) {
            }

            @Override
            public void testPassed(GameTestInfo Info, GameTestRunner Runner) {
                Cleanup.run();
            }

            @Override
            public void testFailed(GameTestInfo Info, GameTestRunner Runner) {
                Cleanup.run();
            }

            @Override
            public void testAddedForRerun(GameTestInfo Old, GameTestInfo Fresh, GameTestRunner Runner) {
            }
        });
    }

    private static void dismiss(GameTestHelper Helper, Entity Subject) {
        if (Subject.isRemoved()) {
            return;
        }
        if (Subject instanceof ServerPlayer User) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        } else {
            Subject.discard();
        }
    }

    private static void tidy(GameTestHelper Helper, List<? extends Entity> Made) {
        for (Entity Each : Made) {
            dismiss(Helper, Each);
        }
    }

    private static List<Scp553Butterfly> present(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(Scp553Butterfly.class, Helper.getBounds().inflate(4.0), Entity::isAlive);
    }

    private static String state(Scp553Butterfly Butterfly) {
        LivingEntity Target = Butterfly.getTarget();
        return " [population " + Butterfly.population() + ", agitated " + Butterfly.isAgitated() + ", target " + (Target == null ? "none" : Target.getName().getString())
                + ", cooldown " + Butterfly.breedCooldown() + ", perched " + Butterfly.isPerched() + ", at " + Butterfly.position() + "]";
    }

    private static boolean dark(GameTestHelper Helper, BlockPos Relative) {
        return Scp553Colony.dark(Helper.getLevel(), Helper.absolutePos(Relative));
    }

    private static List<BlockPos> stalactites(GameTestHelper Helper) {
        List<BlockPos> Found = new ArrayList<>();
        for (BlockPos Pos : BlockPos.betweenClosed(Helper.absolutePos(HollowLow), Helper.absolutePos(HollowHigh))) {
            if (Helper.getLevel().getBlockState(Pos).is(Scp553Registry.Stalactite.get())) {
                Found.add(Pos.immutable());
            }
        }
        return Found;
    }

    private static void light(GameTestHelper Helper) {
        for (BlockPos Spot : List.of(new BlockPos(4, 4, 4), new BlockPos(10, 4, 4), new BlockPos(4, 4, 10), new BlockPos(10, 4, 10), new BlockPos(7, 6, 7),
                new BlockPos(4, 6, 7), new BlockPos(10, 6, 7), new BlockPos(7, 6, 4), new BlockPos(7, 6, 10))) {
            Helper.setBlock(Spot, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15));
        }
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp553_harmless")
    public static void calmBelowTheThreshold(GameTestHelper Helper) {
        Z0003TestPlayer User = player(Helper, 12.5, 12.5, "scp553_calm");
        List<Scp553Butterfly> Made = flock(Helper, Scp553Colony.SwarmThreshold - 1, 12.5, 3.5, 12.5, 1.8);
        float Before = User.getHealth();
        Helper.runAfterDelay(60, () -> Made.get(0).hurt(Helper.getLevel().damageSources().playerAttack(User), 0.5f));
        Helper.runAfterDelay(140, () -> {
            for (Scp553Butterfly Butterfly : Made) {
                Helper.assertTrue(Butterfly.population() == Scp553Colony.SwarmThreshold - 1, "a calm butterfly counted the wrong colony size" + state(Butterfly));
                Helper.assertTrue(!Butterfly.isAgitated(), "a butterfly turned hostile below the threshold" + state(Butterfly));
                Helper.assertTrue(Butterfly.getTarget() == null, "a calm butterfly picked a target" + state(Butterfly));
            }
            Helper.assertTrue(User.getHealth() >= Before, "a calm colony hurt the player, health " + User.getHealth());
            tidy(Helper, Made);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp553_swarm")
    public static void tenOrMoreSwarmAPlayer(GameTestHelper Helper) {
        Z0003TestPlayer User = player(Helper, 12.5, 12.5, "scp553_prey");
        List<Scp553Butterfly> Made = flock(Helper, Scp553Colony.SwarmThreshold, 12.5, 3.5, 12.5, 2.2);
        float Before = User.getHealth();
        Helper.succeedWhen(() -> {
            for (Scp553Butterfly Butterfly : Made) {
                Helper.assertTrue(Butterfly.isAgitated(), "a butterfly in a colony of ten is still calm" + state(Butterfly));
            }
            Helper.assertTrue(Made.stream().filter(Butterfly -> Butterfly.getTarget() == User).count() >= Scp553Colony.SwarmThreshold / 2, "the swarm has not rallied on the player");
            Helper.assertTrue(User.getHealth() < Before, "the swarm has not hurt the player yet");
            tidy(Helper, Made);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp553_swarm_dclass")
    public static void swarmHuntsDClass(GameTestHelper Helper) {
        DClass Prisoner = prisoner(Helper, 12.5, 12.5);
        List<Scp553Butterfly> Made = flock(Helper, Scp553Colony.SwarmThreshold + 2, 12.5, 3.5, 12.5, 2.4);
        float Before = Prisoner.getHealth();
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Made.stream().anyMatch(Butterfly -> Butterfly.getTarget() == Prisoner), "no butterfly targets the D-Class");
            Helper.assertTrue(Prisoner.getHealth() < Before || !Prisoner.isAlive(), "the swarm has not hurt the D-Class yet");
            tidy(Helper, Made);
            dismiss(Helper, Prisoner);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp553_calm_again")
    public static void swarmCalmsWhenThinned(GameTestHelper Helper) {
        List<Scp553Butterfly> Made = flock(Helper, Scp553Colony.SwarmThreshold, 12.5, 3.5, 12.5, 2.0);
        Z0003TestPlayer User = player(Helper, 12.5, 12.5, "scp553_thinner");
        Helper.startSequence()
                .thenWaitUntil(() -> {
                    for (Scp553Butterfly Butterfly : Made) {
                        Helper.assertTrue(Butterfly.isAgitated(), "the colony of ten never turned" + state(Butterfly));
                    }
                })
                .thenExecute(() -> Made.get(0).hurt(Helper.getLevel().damageSources().playerAttack(User), 100.0f))
                .thenWaitUntil(() -> {
                    for (Scp553Butterfly Butterfly : Made.subList(1, Made.size())) {
                        Helper.assertTrue(!Butterfly.isAgitated(), "a butterfly stayed hostile after the colony fell below ten" + state(Butterfly));
                        Helper.assertTrue(Butterfly.getTarget() == null, "a calmed butterfly kept its target" + state(Butterfly));
                    }
                })
                .thenExecute(() -> tidy(Helper, Made))
                .thenSucceed();
    }

    @GameTest(template = Cave, timeoutTicks = 900, batch = "scp553_breed_dark")
    public static void spitsAStalactiteInTheDark(GameTestHelper Helper) {
        boolean[] Spat = {false};
        List<Scp553Butterfly> Made = new ArrayList<>();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(dark(Helper, Nest) && dark(Helper, Hollow), "the sealed cave has not gone dark yet"))
                .thenExecute(() -> {
                    Scp553Butterfly Butterfly = butterfly(Helper, 7.5, 4.5, 7.5);
                    Butterfly.setBreedCooldown(0);
                    Made.add(Butterfly);
                })
                .thenWaitUntil(() -> {
                    Scp553Butterfly Butterfly = Made.get(0);
                    if (Butterfly.isSpitting()) {
                        Spat[0] = true;
                    }
                    List<BlockPos> Found = stalactites(Helper);
                    Helper.assertTrue(Found.size() == 1, "no stalactite has formed yet" + state(Butterfly));
                    BlockPos Spot = Found.get(0);
                    Helper.assertTrue(Scp553Colony.isRock(Helper.getLevel().getBlockState(Spot.above())), "the stalactite does not hang from rock");
                    Helper.assertTrue(Spat[0], "the stalactite appeared without the spit action");
                    Helper.assertTrue(Butterfly.breedCooldown() > Scp553Butterfly.BreedCooldown - 400, "the breeding cooldown did not reset" + state(Butterfly));
                })
                .thenExecute(() -> tidy(Helper, Made))
                .thenSucceed();
    }

    private record Heard(SoundEvent Sound, Vec3 At, long Time) {
    }

    private static final List<Heard> Sounds = new CopyOnWriteArrayList<>();
    private static boolean Listening;

    private static void listen() {
        if (!Listening) {
            Listening = true;
            NeoForge.EVENT_BUS.addListener(PlayLevelSoundEvent.AtPosition.class, Event -> {
                if (Event.getSound() != null && !Event.getLevel().isClientSide()) {
                    Sounds.add(new Heard(Event.getSound().value(), Event.getPosition(), Event.getLevel().getGameTime()));
                }
            });
        }
    }

    private static List<Heard> heard(GameTestHelper Helper, SoundEvent Sound) {
        AABB Area = Helper.getBounds().inflate(4.0);
        return Sounds.stream().filter(Each -> Each.Sound() == Sound && Area.contains(Each.At())).toList();
    }

    @GameTest(template = Cave, timeoutTicks = 900, batch = "scp553_spit_sound")
    public static void spitIsHeard(GameTestHelper Helper) {
        listen();
        List<Scp553Butterfly> Made = new ArrayList<>();
        long[] Seeded = {-1L};
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(dark(Helper, Nest) && dark(Helper, Hollow), "the sealed cave has not gone dark yet"))
                .thenExecute(() -> {
                    Sounds.removeIf(Each -> Helper.getBounds().inflate(4.0).contains(Each.At()));
                    Scp553Butterfly Butterfly = butterfly(Helper, 7.5, 4.5, 7.5);
                    Butterfly.setBreedCooldown(0);
                    Made.add(Butterfly);
                })
                .thenWaitUntil(() -> {
                    Helper.assertTrue(stalactites(Helper).size() == 1, "no stalactite has formed yet" + state(Made.get(0)));
                    Seeded[0] = Helper.getLevel().getGameTime();
                })
                .thenExecute(() -> {
                    List<Heard> Hocks = heard(Helper, Scp553Registry.Spit.get());
                    List<Heard> Spits = heard(Helper, Scp553Registry.SpitLaunch.get());
                    Helper.assertTrue(!Hocks.isEmpty(), "the butterfly did not hock before spitting");
                    Helper.assertTrue(!Spits.isEmpty(), "reproducing played no spit sound");
                    Heard Spit = Spits.get(Spits.size() - 1);
                    Helper.assertTrue(Seeded[0] - Spit.Time() <= 2, "the spit sound did not play as the stalactite was seeded: " + (Seeded[0] - Spit.Time()) + " ticks early");
                    Helper.assertTrue(Hocks.get(0).Time() < Spit.Time(), "the hock came after the spit");
                    Helper.assertTrue(Spit.At().distanceTo(Made.get(0).position()) < 2.0, "the spit sound did not come from the butterfly");
                    Helper.assertTrue(!heard(Helper, Scp553Registry.SpitFleck.get()).isEmpty() && !heard(Helper, Scp553Registry.SpitSlap.get()).isEmpty(), "the spit is missing its wet layers");
                    tidy(Helper, Made);
                })
                .thenSucceed();
    }

    @GameTest(template = Cave, timeoutTicks = 400, batch = "scp553_growth_rate")
    public static void crystalsGrowFaster(GameTestHelper Helper) {
        int[] Start = {0};
        BlockPos[] Spot = {null};
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(dark(Helper, Nest), "the sealed cave has not gone dark yet"))
                .thenExecute(() -> {
                    Helper.assertTrue(Scp553StalactiteBlockEntity.HatchTicks * 2 <= 6000, "stalactites ripen no faster than before: " + Scp553StalactiteBlockEntity.HatchTicks + " ticks");
                    Spot[0] = Helper.absolutePos(Nest);
                    Helper.assertTrue(Scp553Colony.seed(Helper.getLevel(), Spot[0]), "seeding the cave ceiling failed");
                })
                .thenIdle(Scp553StalactiteBlockEntity.CheckInterval * 2)
                .thenExecute(() -> Start[0] = ((Scp553StalactiteBlockEntity) Helper.getLevel().getBlockEntity(Spot[0])).growth())
                .thenIdle(200)
                .thenExecute(() -> {
                    Scp553StalactiteBlockEntity Stalactite = (Scp553StalactiteBlockEntity) Helper.getLevel().getBlockEntity(Spot[0]);
                    int Grown = Stalactite.growth() - Start[0];
                    Helper.assertTrue(Grown >= 180 && Grown <= 220, "the stalactite grew " + Grown + " ticks in 200");
                    Helper.assertTrue(Scp553StalactiteBlockEntity.stageFor(Stalactite.growth()) == Helper.getLevel().getBlockState(Spot[0]).getValue(net.rai.scp_unleashed.scp553.Scp553StalactiteBlock.Stage),
                            "the stalactite stage lags its growth");
                    Helper.getLevel().setBlock(Spot[0], Blocks.AIR.defaultBlockState(), 3);
                })
                .thenSucceed();
    }

    @GameTest(template = Cave, timeoutTicks = 800, batch = "scp553_breed_light")
    public static void noBreedingInTheLight(GameTestHelper Helper) {
        List<Scp553Butterfly> Made = new ArrayList<>();
        BlockPos Spare = new BlockPos(4, 7, 4);
        int[] Settled = new int[1];
        Helper.startSequence()
                .thenExecute(() -> light(Helper))
                .thenWaitUntil(() -> Helper.assertTrue(!dark(Helper, Nest) && !dark(Helper, Hollow) && !dark(Helper, Spare), "the lights have not lit the cave yet"))
                .thenExecute(() -> {
                    Scp553Butterfly Butterfly = butterfly(Helper, 7.5, 4.5, 7.5);
                    Butterfly.setBreedCooldown(0);
                    Made.add(Butterfly);
                    Helper.setBlock(Spare, Scp553Registry.Stalactite.get());
                    Helper.assertTrue(!Scp553Colony.nestable(Helper.getLevel(), Helper.absolutePos(Nest)), "a lit ceiling still counts as a nest");
                })
                .thenWaitUntil(() -> Helper.assertTrue(!dark(Helper, Spare), "the light has not settled around the placed stalactite yet"))
                .thenIdle(Scp553StalactiteBlockEntity.CheckInterval * 2)
                .thenExecute(() -> Settled[0] = Helper.getLevel().getBlockEntity(Helper.absolutePos(Spare)) instanceof Scp553StalactiteBlockEntity Placed ? Placed.growth() : -1)
                .thenIdle(400)
                .thenExecute(() -> {
                    List<BlockPos> Found = stalactites(Helper);
                    Helper.assertTrue(Found.size() == 1, "a butterfly bred in the light, stalactites " + Found.size() + state(Made.get(0)));
                    Helper.assertTrue(Helper.getLevel().getBlockEntity(Helper.absolutePos(Spare)) instanceof Scp553StalactiteBlockEntity Grown && Grown.growth() == Settled[0] && Grown.stalled(),
                            "a stalactite kept growing in the light");
                    tidy(Helper, Made);
                })
                .thenSucceed();
    }

    @GameTest(template = Cave, timeoutTicks = 400, batch = "scp553_hatch")
    public static void stalactiteGrowsAndHatches(GameTestHelper Helper) {
        List<Scp553Butterfly> Before = new ArrayList<>();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(dark(Helper, Nest), "the sealed cave has not gone dark yet"))
                .thenExecute(() -> {
                    ServerLevel World = Helper.getLevel();
                    BlockPos Spot = Helper.absolutePos(Nest);
                    Helper.assertTrue(Scp553Colony.seed(World, Spot), "seeding the cave ceiling failed");
                    Helper.assertTrue(World.getBlockState(Spot).getValue(net.rai.scp_unleashed.scp553.Scp553StalactiteBlock.Stage) == 0, "a fresh stalactite is not at stage 0");
                    Scp553StalactiteBlockEntity Stalactite = (Scp553StalactiteBlockEntity) World.getBlockEntity(Spot);
                    Helper.assertTrue(Stalactite != null, "the stalactite has no block entity");
                    Stalactite.setGrowth(Scp553StalactiteBlockEntity.StageTicks * 2);
                    Helper.assertTrue(World.getBlockState(Spot).getValue(net.rai.scp_unleashed.scp553.Scp553StalactiteBlock.Stage) == 2, "growth did not raise the stage");
                    Stalactite.setGrowth(Scp553StalactiteBlockEntity.HatchTicks - Scp553StalactiteBlockEntity.CheckInterval);
                    Helper.assertTrue(World.getBlockState(Spot).getValue(net.rai.scp_unleashed.scp553.Scp553StalactiteBlock.Stage) == 3, "a nearly ripe stalactite is not at the last stage");
                    Before.addAll(present(Helper));
                })
                .thenWaitUntil(() -> {
                    Helper.assertTrue(stalactites(Helper).isEmpty(), "the ripe stalactite has not hatched yet");
                    List<Scp553Butterfly> Now = present(Helper);
                    Helper.assertTrue(Now.size() == Before.size() + 1, "hatching released " + (Now.size() - Before.size()) + " butterflies");
                    Scp553Butterfly Born = Now.stream().filter(Found -> !Before.contains(Found)).findFirst().orElseThrow();
                    Helper.assertTrue(Helper.absolutePos(Nest).equals(Born.home()), "the newborn does not call its stalactite home" + state(Born));
                    Helper.assertTrue(Born.breedCooldown() > Scp553Butterfly.BreedCooldown - 400, "the newborn can breed straight away" + state(Born));
                    tidy(Helper, Now);
                })
                .thenSucceed();
    }

    @GameTest(template = Cave, timeoutTicks = 600, batch = "scp553_caps")
    public static void crowdedColoniesStopBreeding(GameTestHelper Helper) {
        List<Scp553Butterfly> Made = new ArrayList<>();
        BlockPos Ripe = new BlockPos(4, 7, 4);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(dark(Helper, Nest) && dark(Helper, Ripe), "the sealed cave has not gone dark yet"))
                .thenExecute(() -> {
                    for (int Index = 0; Index < Scp553Colony.AreaCap; Index++) {
                        Scp553Butterfly Butterfly = butterfly(Helper, 4.5 + (Index % 5) * 1.6, 3.6 + (Index / 10) * 1.6, 4.5 + ((Index / 5) % 2) * 5.0);
                        Butterfly.setBreedCooldown(0);
                        Made.add(Butterfly);
                    }
                    Helper.setBlock(Ripe, Scp553Registry.Stalactite.get());
                    ServerLevel World = Helper.getLevel();
                    Helper.assertTrue(!Scp553Colony.roomToBreed(World, Helper.absolutePos(Nest)), "a full colony still has room to breed");
                    Helper.assertTrue(!Scp553Colony.roomToHatch(World, Helper.absolutePos(Ripe)), "a full colony still has room to hatch");
                    ((Scp553StalactiteBlockEntity) World.getBlockEntity(Helper.absolutePos(Ripe))).setGrowth(Scp553StalactiteBlockEntity.HatchTicks);
                })
                .thenIdle(300)
                .thenExecute(() -> {
                    List<BlockPos> Found = stalactites(Helper);
                    Helper.assertTrue(Found.size() == 1, "a full colony bred or hatched anyway, stalactites " + Found.size());
                    Helper.assertTrue(present(Helper).size() == Scp553Colony.AreaCap, "the colony grew past its cap to " + present(Helper).size());
                    tidy(Helper, Made);
                })
                .thenSucceed();
    }

    @GameTest(template = Cave, timeoutTicks = 200, batch = "scp553_world_cap")
    public static void worldCapHoldsBreeding(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp553World Data = Scp553World.get(World.getServer());
        List<UUID> Filler = new ArrayList<>();
        whenDone(Helper, () -> Filler.forEach(Data::leave));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(dark(Helper, Nest), "the sealed cave has not gone dark yet"))
                .thenExecute(() -> {
                    Helper.assertTrue(Scp553Colony.roomToBreed(World, Helper.absolutePos(Nest)), "an empty cave has no room to breed");
                    while (Data.count() < Scp553Colony.WorldCap) {
                        UUID Extra = UUID.randomUUID();
                        Filler.add(Extra);
                        Data.join(Extra);
                    }
                    Helper.assertTrue(!Scp553Colony.roomToBreed(World, Helper.absolutePos(Nest)), "the world cap does not stop breeding");
                    Helper.assertTrue(!Scp553Colony.roomToHatch(World, Helper.absolutePos(Nest)), "the world cap does not stop hatching");
                    Helper.assertTrue(Scp553Cave.arrive(World, Helper.absolutePos(Hollow), null).isEmpty(), "an arrival ignored the world cap");
                    Filler.forEach(Data::leave);
                    Filler.clear();
                    Helper.assertTrue(Scp553Colony.roomToBreed(World, Helper.absolutePos(Nest)), "breeding stayed blocked after the world emptied");
                })
                .thenSucceed();
    }

    @GameTest(template = Cave, timeoutTicks = 300, batch = "scp553_arrival")
    public static void arrivalFindsTheCave(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer Reader = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp553_reader"));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(dark(Helper, Hollow), "the sealed cave has not gone dark yet"))
                .thenExecute(() -> {
                    AnomalyDocument Document = AnomalyDocuments.server().get(Scp553Registry.DocumentId);
                    Helper.assertTrue(Document != null, "the scp_553 document did not load");
                    Helper.assertTrue(Scp553Cave.problem(World, Helper.absolutePos(new BlockPos(1, 5, 1))) != null, "solid rock passed as a cave");
                    Optional<BlockPos> Found = Scp553Cave.find(World, Helper.absolutePos(Hollow), 6);
                    Helper.assertTrue(Found.isPresent(), "the cave search found nothing, centre says: " + Scp553Cave.problem(World, Helper.absolutePos(Hollow)));
                    AABB Room = new AABB(Helper.absolutePos(HollowLow)).minmax(new AABB(Helper.absolutePos(HollowHigh)));
                    Helper.assertTrue(Room.contains(Vec3.atCenterOf(Found.get())), "the cave spot " + Found.get() + " is not inside the hollow " + Room);
                    boolean Placed = Scp553Cave.manifest(new AnomalySpawners.Discovery(World, Found.get(), Reader, Scp553Registry.DocumentId, Document, false));
                    Helper.assertTrue(Placed, "the arrival placed nothing");
                    List<Scp553Butterfly> Group = present(Helper);
                    Helper.assertTrue(Group.size() == Scp553Cave.StartGroup, "the arrival placed " + Group.size() + " butterflies");
                    Helper.assertTrue(Group.size() < Scp553Colony.SwarmThreshold, "the starting group is already a swarm");
                    AABB Inside = new AABB(Helper.absolutePos(HollowLow)).minmax(new AABB(Helper.absolutePos(HollowHigh)));
                    for (Scp553Butterfly Butterfly : Group) {
                        Helper.assertTrue(Inside.contains(Butterfly.position()), "a butterfly arrived outside the cave" + state(Butterfly));
                        Helper.assertTrue(Butterfly.isPersistenceRequired(), "an arriving butterfly could despawn");
                        Helper.assertTrue(Found.get().equals(Butterfly.home()), "an arriving butterfly does not call the cave home" + state(Butterfly));
                        Helper.assertTrue(Butterfly.breedCooldown() >= Scp553Butterfly.FirstBreedMin - 20 && Butterfly.breedCooldown() <= Scp553Butterfly.FirstBreedMax,
                                "an arriving butterfly has an odd breeding delay" + state(Butterfly));
                    }
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    List<Scp553Butterfly> Group = present(Helper);
                    for (Scp553Butterfly Butterfly : Group) {
                        Helper.assertTrue(!Butterfly.isAgitated() && Butterfly.getTarget() == null, "the starting group is hostile" + state(Butterfly));
                    }
                    tidy(Helper, Group);
                })
                .thenSucceed();
    }
}
