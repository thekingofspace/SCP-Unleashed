package net.rai.scp_unleashed.scp001scarlet.court.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunDamage;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletArena;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKing;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingData;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingRegistry;
import net.rai.scp_unleashed.scp001scarlet.king.gametest.ScarletKingGameTests;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletCourtHook;
import net.rai.scp_unleashed.scp001scarlet.court.CourtArena;
import net.rai.scp_unleashed.scp001scarlet.court.CourtDaughter;
import net.rai.scp_unleashed.scp001scarlet.court.CourtFx;
import net.rai.scp_unleashed.scp001scarlet.court.CourtKind;
import net.rai.scp_unleashed.scp001scarlet.court.CourtMinion;
import net.rai.scp_unleashed.scp001scarlet.court.CourtProp;
import net.rai.scp_unleashed.scp001scarlet.court.DaughterBride;
import net.rai.scp_unleashed.scp001scarlet.court.DaughterMoon;
import net.rai.scp_unleashed.scp001scarlet.court.DaughterEyes;
import net.rai.scp_unleashed.scp001scarlet.court.DaughterThorns;
import net.rai.scp_unleashed.scp001scarlet.court.ScarletCourt;
import net.rai.scp_unleashed.scp001scarlet.court.ScarletCourtRegistry;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class ScarletCourtGameTests {
    public static final String Arena = "scarletcourt_arena";
    public static final int Radius = 9;

    private ScarletCourtGameTests() {
    }

    private static BlockPos prepare(GameTestHelper Helper) {
        ScarletCourtRegistry.install();
        CourtArena.overrideForTests(Radius);
        ScarletCourt.fastForTests(true);
        ScarletCourt.Instance.end(Helper.getLevel());
        AABB Bounds = Helper.getBounds();
        BlockPos Centre = BlockPos.containing(Bounds.getCenter().x, Bounds.minY + 1.0, Bounds.getCenter().z);
        BlockPos Found = CourtArena.ground(Helper.getLevel(), Centre, 6);
        return Found != null ? Found : Centre;
    }

    private static void restore(GameTestHelper Helper) {
        ScarletCourt.Instance.end(Helper.getLevel());
        ScarletCourt.fastForTests(false);
        CourtArena.overrideForTests(-1);
    }

    private static List<Entity> court(GameTestHelper Helper, BlockPos Dais) {
        return Helper.getLevel().getEntitiesOfClass(Entity.class, new AABB(Dais).inflate(40.0), Each -> !Each.isRemoved() && Each.getTags().contains(CourtFx.Tag));
    }

    private static void slayPresent(ServerLevel World, int Below) {
        if (ScarletCourt.Instance.slain() >= Below) {
            return;
        }
        for (CourtDaughter Each : ScarletCourt.Instance.daughters(World)) {
            if (!Each.dying()) {
                Each.beginDying();
            }
        }
    }

    private static Pig dummy(GameTestHelper Helper, Vec3 Spot, float Yaw) {
        Pig Made = EntityType.PIG.create(Helper.getLevel());
        Made.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Made.setYHeadRot(Yaw);
        Made.setYBodyRot(Yaw);
        Made.setNoAi(true);
        Made.setNoGravity(true);
        Made.setInvulnerable(false);
        Helper.getLevel().addFreshEntity(Made);
        return Made;
    }

    private static <T extends CourtDaughter> T daughter(GameTestHelper Helper, EntityType<T> Type, BlockPos Dais) {
        T Made = Type.create(Helper.getLevel());
        Made.moveTo(Dais.getX() + 0.5, Dais.getY(), Dais.getZ() + 0.5, 0.0f, 0.0f);
        Made.addTag(CourtFx.Tag);
        Made.setup(1, Dais);
        Helper.getLevel().addFreshEntity(Made);
        ScarletCourt.Instance.adopt(Made);
        return Made;
    }

    private static float yawTowards(Vec3 From, Vec3 To) {
        return (float) (Math.atan2(To.z - From.z, To.x - From.x) * 180.0 / Math.PI) - 90.0f;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scarletcourt_hook")
    public static void hookIsRegistered(GameTestHelper Helper) {
        Helper.assertTrue(ScarletCourtRegistry.installed(), "common setup never installed the court as the Scarlet King's court hook");
        Helper.assertTrue(ScarletCourtHook.Holder.get() == null || ScarletCourtHook.Holder.get() == ScarletCourt.Instance, "another court replaced this one");
        ScarletCourtRegistry.install();
        Helper.assertTrue(ScarletCourtHook.Holder.get() == ScarletCourt.Instance, "the court hook does not point at the court");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scarletcourt_begin")
    public static void beginOpensWithWaveAndFirstDaughter(GameTestHelper Helper) {
        BlockPos Dais = prepare(Helper);
        ServerLevel World = Helper.getLevel();
        ScarletCourt.Instance.begin(World, Dais, null, 1);
        Helper.assertTrue(ScarletCourt.Instance.daughtersRemaining() == 4, "four daughters should remain right after begin");
        Helper.assertTrue(ScarletCourt.Instance.living(World) > 0, "begin spawned no opening wave");
        Helper.assertTrue(ScarletCourt.Instance.stage() == ScarletCourt.Stage.Opening, "begin did not enter the opening stage");
        Helper.succeedWhen(() -> {
            CourtDaughter Current = ScarletCourt.Instance.currentDaughter(World);
            Helper.assertTrue(Current instanceof DaughterThorns, "the Briar Crown has not entered");
            Helper.assertTrue(ScarletCourt.Instance.daughterActive(), "a daughter is present but the court reports none active");
            Helper.assertTrue(Current.entering(), "the first daughter did not play her entrance");
            restore(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scarletcourt_advance")
    public static void slainDaughterAdvancesAfterBreather(GameTestHelper Helper) {
        BlockPos Dais = prepare(Helper);
        ServerLevel World = Helper.getLevel();
        ScarletCourt.Instance.begin(World, Dais, null, 1);
        Helper.onEachTick(() -> slayPresent(World, 1));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(ScarletCourt.Instance.slain() == 1, "the first daughter has not fallen yet");
            Helper.assertTrue(ScarletCourt.Instance.daughtersRemaining() == 3, "three daughters should remain");
            Helper.assertTrue(ScarletCourt.Instance.currentDaughter(World) instanceof DaughterBride, "the Bride in Chains has not followed");
            Helper.assertTrue(World.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(Dais).inflate(30.0),
                    Drop -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(Drop.getItem().getItem()).getPath().startsWith("scarlet_")).isEmpty(), "the Briar Crown dropped loot");
            restore(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 1400, batch = "scarletcourt_all")
    public static void fourFallenLeaveNone(GameTestHelper Helper) {
        BlockPos Dais = prepare(Helper);
        ServerLevel World = Helper.getLevel();
        ScarletCourt.Instance.begin(World, Dais, null, 1);
        Helper.onEachTick(() -> slayPresent(World, 4));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(ScarletCourt.Instance.daughtersRemaining() == 0, "daughters still remain: " + ScarletCourt.Instance.daughtersRemaining());
            Helper.assertTrue(ScarletCourt.Instance.stage() == ScarletCourt.Stage.King, "the court did not turn to the King's phases");
            Helper.assertTrue(!ScarletCourt.Instance.daughterActive(), "a daughter is still reported active");
            restore(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scarletcourt_cap")
    public static void minionCapHoldsAndScales(GameTestHelper Helper) {
        BlockPos Dais = prepare(Helper);
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(ScarletCourt.cap(1) == 12 && ScarletCourt.cap(3) == 24 && ScarletCourt.cap(9) == 30, "the minion cap does not scale 12 / 24 / 30");
        ScarletCourt.Instance.begin(World, Dais, null, 1);
        for (int Round = 0; Round < 6; Round++) {
            ScarletCourt.Instance.wave(World, "horde");
        }
        int Solo = ScarletCourt.Instance.living(World);
        Helper.assertTrue(Solo <= ScarletCourt.cap(1) && Solo >= ScarletCourt.cap(1) - 2, "solo living minions broke the cap: " + Solo);
        ScarletCourt.Instance.reset(World);
        ScarletCourt.Instance.begin(World, Dais, null, 3);
        for (int Round = 0; Round < 8; Round++) {
            ScarletCourt.Instance.wave(World, "horde");
        }
        int Group = ScarletCourt.Instance.living(World);
        Helper.assertTrue(Group <= ScarletCourt.cap(3) && Group > ScarletCourt.cap(1), "three players should allow more minions under a higher cap: " + Group);
        restore(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scarletcourt_minions")
    public static void everyMinionSpawnsWithItsBehaviour(GameTestHelper Helper) {
        BlockPos Dais = prepare(Helper);
        ServerLevel World = Helper.getLevel();
        for (CourtKind Kind : CourtKind.values()) {
            CourtMinion Made = ScarletCourt.Instance.spawnMinion(World, Kind, Dais);
            Helper.assertTrue(Made != null && Made.isAlive(), Kind.id() + " did not spawn");
            Helper.assertTrue(Made.getTags().contains(CourtFx.Tag), Kind.id() + " is not marked as part of the court");
            Set<Class<? extends Goal>> Running = Made.goalSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal).map(Goal::getClass)
                    .collect(Collectors.toSet());
            boolean Own = false;
            for (Class<? extends Goal> Each : Made.behaviours()) {
                Helper.assertTrue(Running.contains(Each), Kind.id() + " lost its behaviour " + Each.getSimpleName());
                Own |= Each.getEnclosingClass() == Made.getClass();
            }
            Helper.assertTrue(Own, Kind.id() + " has no behaviour of its own");
            Helper.assertTrue(Made.isAlliedTo(ScarletCourt.Instance.spawnMinion(World, CourtKind.Thrall, Dais)), Kind.id() + " is not allied with the court");
        }
        Entity Cultist = ScarletCourt.Instance.spawnOne(World, "cultist", Dais);
        Helper.assertTrue(ScarletCourt.cultistType().isPresent() == (Cultist != null), "cultists should join the host only when their type is registered");
        restore(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scarletcourt_thorns")
    public static void briarCrownRaisesWalls(GameTestHelper Helper) {
        BlockPos Dais = prepare(Helper);
        DaughterThorns Thorns = daughter(Helper, ScarletCourtRegistry.ThornsType.get(), Dais);
        Pig Target = dummy(Helper, Vec3.atBottomCenterOf(Dais).add(0.0, 0.0, 5.0), 0.0f);
        Thorns.startMove(DaughterThorns.Walls, Target);
        Helper.succeedWhen(() -> {
            List<CourtProp> Walls = Helper.getLevel().getEntitiesOfClass(CourtProp.class, new AABB(Dais).inflate(20.0), Each -> Each.kind() == CourtProp.PropKind.ThornWall);
            Helper.assertTrue(Walls.size() >= 3, "the Briar Crown raised " + Walls.size() + " thorn walls");
            CourtProp Cage = Thorns.cageAround(Helper.getLevel(), Target);
            Helper.assertTrue(Cage != null && Cage.captive() == Target, "the briar cage did not close around its target");
            restore(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scarletcourt_bride")
    public static void brideVowPunishesDistance(GameTestHelper Helper) {
        BlockPos Dais = prepare(Helper);
        DaughterBride Bride = daughter(Helper, ScarletCourtRegistry.BrideType.get(), Dais);
        Pig Target = dummy(Helper, Vec3.atBottomCenterOf(Dais).add(2.0, 0.0, 0.0), 0.0f);
        Bride.startMove(DaughterBride.Vow, Target);
        Helper.runAfterDelay(6, () -> Helper.assertTrue(Bride.spectral(), "the Bride did not turn spectral while making her vow"));
        DaughterBride.Bond Made = Bride.bind(Helper.getLevel(), Target);
        Helper.assertTrue(Made.Second() == null, "a lone target should be bound to a pillar, not a partner");
        Vec3 Away = Target.position().subtract(Made.Post());
        Vec3 Far = Made.Post().add(CourtFx.flat(Away).scale(15.0));
        Target.moveTo(Far.x, Target.getY(), Far.z, 0.0f, 0.0f);
        float Before = Target.getHealth();
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Target.getHealth() < Before, "straying from the vow did no harm");
            Helper.assertTrue(!Bride.spectral(), "the Bride stayed spectral after her vow");
            restore(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scarletcourt_eyes")
    public static void daughterOfEyesSearsWatchers(GameTestHelper Helper) {
        BlockPos Dais = prepare(Helper);
        DaughterEyes Eyes = daughter(Helper, ScarletCourtRegistry.EyesType.get(), Dais);
        Vec3 Base = Vec3.atBottomCenterOf(Dais);
        Vec3 WatchSpot = Base.add(0.0, 0.0, 8.0);
        Vec3 ShySpot = Base.add(0.0, 0.0, -8.0);
        Pig Watcher = dummy(Helper, WatchSpot, yawTowards(WatchSpot, Base));
        Pig Shy = dummy(Helper, ShySpot, yawTowards(Base, ShySpot));
        Watcher.setXRot(-30.0f);
        Shy.setXRot(0.0f);
        float WatcherBefore = Watcher.getHealth();
        float ShyBefore = Shy.getHealth();
        Eyes.startMove(DaughterEyes.Flare, Watcher);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Watcher.getHealth() < WatcherBefore, "a watcher meeting her gaze was not seared");
            Helper.assertTrue(Shy.getHealth() >= ShyBefore, "a watcher looking away was seared");
            restore(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scarletcourt_moon")
    public static void moonPillarsSpareTheSheltered(GameTestHelper Helper) {
        BlockPos Dais = prepare(Helper);
        ServerLevel World = Helper.getLevel();
        DaughterMoon Moon = daughter(Helper, ScarletCourtRegistry.MoonType.get(), Dais);
        Vec3 Base = Vec3.atBottomCenterOf(Dais);
        Pig Open = dummy(Helper, Base.add(5.0, 0.0, 0.0), 0.0f);
        Pig Sheltered = dummy(Helper, Base.add(-5.0, 0.0, 0.0), 0.0f);
        BlockPos Roof = Sheltered.blockPosition().above(3);
        World.setBlockAndUpdate(Roof, Blocks.STONE.defaultBlockState());
        float OpenBefore = Open.getHealth();
        float ShelteredBefore = Sheltered.getHealth();
        Moon.drop(World, Open.position());
        Moon.drop(World, Sheltered.position());
        Helper.assertTrue(Moon.falls().size() == 2, "the moonlight pillars were not marked on the ground first");
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Open.getHealth() < OpenBefore, "a moonlight pillar spared someone in the open");
            Helper.assertTrue(Sheltered.getHealth() >= ShelteredBefore, "a moonlight pillar struck through a roof");
            Helper.assertTrue(Moon.moon() == DaughterMoon.Full, "the lunar cycle did not wax to a full moon");
            Helper.assertTrue(Moon.fullMoon() && !Moon.translucent(), "the full moon is not shown");
            World.setBlockAndUpdate(Roof, Blocks.AIR.defaultBlockState());
            restore(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scarletcourt_eclipse")
    public static void moonEclipseQuickensHerCycle(GameTestHelper Helper) {
        BlockPos Dais = prepare(Helper);
        DaughterMoon Moon = daughter(Helper, ScarletCourtRegistry.MoonType.get(), Dais);
        Moon.poolShare(0.49f);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Moon.eclipsed(), "the Moon Daughter did not eclipse at half health");
            Helper.assertTrue(Moon.phaseTwo(), "the eclipse did not mark her second phase");
            restore(Helper);
        });
    }

    @GameTest(template = ScarletKingGameTests.Arena, timeoutTicks = 1200, batch = "scarletcourt_king")
    public static void realKingFallsOpenAfterFourDaughters(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        CourtArena.overrideForTests(Radius);
        ScarletCourt.fastForTests(true);
        ScarletCourt.Instance.end(World);
        ScarletCourtRegistry.install();
        ScarletKingData Data = ScarletKingData.get(World.getServer());
        BlockPos Throne = Helper.absolutePos(ScarletKingGameTests.Middle).below(2);
        ScarletKing King = ScarletKingRegistry.KingType.get().create(World);
        Vec3 At = Vec3.atBottomCenterOf(Helper.absolutePos(ScarletKingGameTests.Middle));
        King.moveTo(At.x, At.y, At.z, 180.0f, 0.0f);
        King.generation(Data.generation());
        King.place(ScarletArena.of(World, Throne));
        World.addFreshEntity(King);
        ScarletState.get(World.getServer()).phase(ScarletState.Phase.Fight, World.getGameTime());
        King.beginFight(1);
        Helper.assertTrue(ScarletCourt.Instance.active(), "the King did not begin the court");
        Helper.assertTrue(ScarletCourt.Instance.daughtersRemaining() == 4, "four daughters should guard the King at the start");
        Helper.assertTrue(King.warded(), "the King is not warded while his daughters live");
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scarlet_court_king"));
        Helper.assertFalse(King.hurt(GunDamage.sourceFor(World, User, GunKind.AssaultRifle), 200.0f), "the warded King took a shot");
        Helper.onEachTick(() -> {
            Data.emptySince(-1L);
            slayPresent(World, 4);
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(ScarletCourt.Instance.daughtersRemaining() == 0, "daughters still remain: " + ScarletCourt.Instance.daughtersRemaining());
            Helper.assertTrue(King.isAlive() && !King.warded(), "the ward did not fall with the last daughter");
            float Before = King.pool();
            Helper.assertTrue(King.hurt(GunDamage.sourceFor(World, User, GunKind.AssaultRifle), 200.0f), "the unwarded King ignored a shot");
            Helper.assertTrue(King.pool() < Before, "the King's health did not drop");
            King.clearBar();
            King.discard();
            ScarletState.get(World.getServer()).phase(ScarletState.Phase.Asleep, World.getGameTime());
            Data.consumed(false);
            Data.endRite();
            Data.king(null);
            restore(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scarletcourt_reset")
    public static void resetKeepsSlainDaughters(GameTestHelper Helper) {
        BlockPos Dais = prepare(Helper);
        ServerLevel World = Helper.getLevel();
        int[] Step = {0};
        ScarletCourt.Instance.begin(World, Dais, null, 1);
        Helper.onEachTick(() -> {
            if (Step[0] == 0) {
                slayPresent(World, 1);
                if (ScarletCourt.Instance.slain() == 1 && ScarletCourt.Instance.currentDaughter(World) instanceof DaughterBride) {
                    ScarletCourt.Instance.reset(World);
                    Helper.assertTrue(court(Helper, Dais).isEmpty(), "reset left court entities behind");
                    Helper.assertTrue(ScarletCourt.Instance.daughtersRemaining() == 3, "reset forgot the slain daughter");
                    Helper.assertTrue(!ScarletCourt.Instance.active(), "reset left the court running");
                    ScarletCourt.Instance.begin(World, Dais, null, 1);
                    Step[0] = 1;
                }
            } else if (Step[0] == 1 && ScarletCourt.Instance.currentDaughter(World) instanceof DaughterBride) {
                Step[0] = 2;
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Step[0] == 2, "the court did not resume at the Bride in Chains after a reset");
            Helper.assertTrue(ScarletCourt.Instance.daughtersRemaining() == 3, "three daughters should remain after the reset");
            restore(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scarletcourt_end")
    public static void endDespawnsEverything(GameTestHelper Helper) {
        BlockPos Dais = prepare(Helper);
        ServerLevel World = Helper.getLevel();
        ScarletCourt.Instance.begin(World, Dais, null, 1);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(ScarletCourt.Instance.currentDaughter(World) != null, "no daughter has entered yet");
            Helper.assertTrue(!court(Helper, Dais).isEmpty(), "the court has no entities to clear");
            ScarletCourt.Instance.end(World);
            Helper.assertTrue(court(Helper, Dais).isEmpty(), "end left court entities behind");
            Helper.assertTrue(!ScarletCourt.Instance.active(), "end left the court running");
            restore(Helper);
        });
    }
}
