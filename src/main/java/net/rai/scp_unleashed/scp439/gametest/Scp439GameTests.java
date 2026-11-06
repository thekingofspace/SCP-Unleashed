package net.rai.scp_unleashed.scp439.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassAppearance;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.gear.GearRegistry;
import net.rai.scp_unleashed.mtf.MtfOperative;
import net.rai.scp_unleashed.mtf.MtfRegistry;
import net.rai.scp_unleashed.mtf.MtfRole;
import net.rai.scp_unleashed.mtf.MtfTier;
import net.rai.scp_unleashed.scp439.Scp439ArrivalCondition;
import net.rai.scp_unleashed.scp439.Scp439Brute;
import net.rai.scp_unleashed.scp439.Scp439Colonies;
import net.rai.scp_unleashed.scp439.Scp439Creature;
import net.rai.scp_unleashed.scp439.Scp439Hive;
import net.rai.scp_unleashed.scp439.Scp439Infection;
import net.rai.scp_unleashed.scp439.Scp439Look;
import net.rai.scp_unleashed.scp439.Scp439Queen;
import net.rai.scp_unleashed.scp439.Scp439Registry;
import net.rai.scp_unleashed.scp439.Scp439Worker;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp439GameTests {
    public static final String Arena = "scp439_arena";
    public static final double Floor = 2.0;
    public static final double Search = 40.0;

    private Scp439GameTests() {
    }

    private static Vec3 at(GameTestHelper Helper, double X, double Z) {
        return Helper.absoluteVec(new Vec3(X, Floor, Z));
    }

    private static Scp439TestPlayer player(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        Scp439TestPlayer User = new Scp439TestPlayer(World, Name);
        Vec3 Spot = at(Helper, X, Z);
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, Entity User) {
        if (!User.isRemoved()) {
            if (User instanceof Scp439TestPlayer Player0) {
                Helper.getLevel().removePlayerImmediately(Player0, Entity.RemovalReason.DISCARDED);
            } else {
                User.discard();
            }
        }
    }

    private static Villager villager(GameTestHelper Helper, double X, double Z) {
        Villager Made = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(X, Floor, Z));
        Made.setVillagerData(Made.getVillagerData().setType(VillagerType.DESERT).setProfession(VillagerProfession.LIBRARIAN).setLevel(3));
        return Made;
    }

    private static MtfOperative operative(GameTestHelper Helper, double X, double Z) {
        MtfOperative Made = Helper.spawnWithNoFreeWill(MtfRegistry.OperativeType.get(), new Vec3(X, Floor, Z));
        Made.configure(UUID.randomUUID(), MtfTier.Faint, MtfRole.Rifleman, 0, Helper.getLevel().getRandom());
        return Made;
    }

    private static Scp439Queen readyQueen(GameTestHelper Helper, double X, double Z, float Yaw) {
        ServerLevel World = Helper.getLevel();
        Scp439Queen Made = Scp439Registry.Queen.get().create(World);
        Helper.assertTrue(Made != null, "the queen could not be made");
        Vec3 Spot = at(Helper, X, Z);
        Made.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Made.setYBodyRot(Yaw);
        Made.setYHeadRot(Yaw);
        Helper.assertTrue(World.addFreshEntity(Made), "the queen could not be placed");
        return Made;
    }

    private static Scp439Hive hive(GameTestHelper Helper, double X, double Z, int Ticks, CompoundTag Queen) {
        Vec3 Spot = at(Helper, X, Z);
        Scp439Hive Made = Scp439Hive.grow(Helper.getLevel(), Spot, 180.0f, Scp439Look.Blank, Queen, Ticks);
        Helper.assertTrue(Made != null, "an SCP-439-1 nest could not be placed");
        return Made;
    }

    private static <T extends Entity> List<T> around(GameTestHelper Helper, Class<T> Kind, Vec3 Centre, double Radius) {
        return Helper.getLevel().getEntitiesOfClass(Kind, new AABB(Centre, Centre).inflate(Radius), Entity::isAlive);
    }

    private static List<Scp439Hive> hivesNear(GameTestHelper Helper, Vec3 Centre) {
        return around(Helper, Scp439Hive.class, Centre, 6.0);
    }

    private static void clean(GameTestHelper Helper) {
        Vec3 Centre = at(Helper, 16.0, 16.0);
        for (Entity Found : Helper.getLevel().getEntitiesOfClass(Entity.class, new AABB(Centre, Centre).inflate(Search), Found -> Found instanceof Scp439Creature
                || Found instanceof Scp439Hive)) {
            Found.discard();
        }
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp439_queenFindsHost")
    public static void queenTargetsClosestHostAndInfects(GameTestHelper Helper) {
        Villager Near = villager(Helper, 12.5, 8.5);
        DClass Far = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(8.5, Floor, 20.5));
        Scp439Queen Queen = Scp439Hive.release(Helper.getLevel(), at(Helper, 8.5, 8.5), 270.0f, new CompoundTag());
        Helper.assertTrue(Queen != null, "the queen could not be released");
        Helper.assertTrue(Queen.choosePrey() == Near, "the queen did not pick the closest host");
        Helper.startSequence().thenWaitUntil(() -> {
            Helper.assertTrue(Scp439Infection.isInfected(Near), "the queen has not reached the villager yet");
            Helper.assertTrue(Queen.isRemoved(), "the queen did not withdraw into the host");
        }).thenExecute(() -> {
            Helper.assertFalse(Scp439Infection.isInfected(Far), "the farther D-Class was infected instead");
            Helper.assertTrue(Scp439Infection.hasModifiers(Near), "the host is not slowed");
            Helper.assertTrue(Near.isAlive(), "the bite killed the host");
            dismiss(Helper, Near);
            dismiss(Helper, Far);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp439_mtfHunt")
    public static void queenHuntsMtfOperatives(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp439Queen Queen = readyQueen(Helper, 16.5, 16.5, -90.0f);
        MtfOperative Operative = operative(Helper, 17.7, 16.5);
        DClass Far = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(16.5, Floor, 24.5));
        UUID Carrier = Operative.getUUID();
        Helper.assertTrue(Scp439Queen.canHost(Operative), "an MTF operative cannot host SCP-439");
        Helper.assertTrue(Queen.choosePrey() == Operative, "the queen did not pick the MTF operative");
        Helper.startSequence().thenWaitUntil(() -> {
            Helper.assertTrue(Scp439Infection.isInfected(Operative) && !Operative.isRemoved(), "the queen has not stung the operative");
        }).thenWaitUntil(() -> {
            Helper.assertTrue(Operative.isRemoved(), "the operative was not recalled");
        }).thenWaitUntil(() -> {
            Vec3 Spot = Operative.position();
            Helper.assertFalse(Scp439Colonies.get(World).tracksHost(Carrier), "the recalled operative is still counted as a carrier");
            Helper.assertTrue(around(Helper, Scp439Queen.class, Spot, 8.0).size() == 1, "the queen did not come back out of the recalled operative");
            Helper.assertTrue(hivesNear(Helper, Spot).isEmpty(), "a nest grew from a recalled operative");
        }).thenExecute(() -> {
            Helper.assertFalse(Scp439Infection.isInfected(Far), "the farther D-Class was infected instead");
            dismiss(Helper, Far);
            clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp439_mtfRecall")
    public static void recallDuringBurrowKeepsTheQueen(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp439Queen Queen = readyQueen(Helper, 16.5, 16.5, -90.0f);
        MtfOperative Operative = operative(Helper, 17.5, 16.5);
        UUID Carrier = Operative.getUUID();
        Vec3 Spot = Operative.position();
        Helper.assertTrue(Scp439Infection.infect(Operative, Queen), "the operative could not carry SCP-439");
        Queen.beginBurrow(Operative);
        Operative.discard();
        Helper.startSequence().thenWaitUntil(() -> {
            Helper.assertTrue(Queen.burrowing() == null, "the queen is still burrowing into a recalled carrier");
        }).thenIdle(Scp439Queen.BurrowTicks + 5).thenExecute(() -> {
            Helper.assertTrue(Queen.isAlive() && !Queen.isRemoved(), "the queen vanished with the recalled carrier");
            Helper.assertTrue(around(Helper, Scp439Queen.class, Spot, 8.0).size() == 1, "a second queen appeared");
            Helper.assertFalse(Scp439Colonies.get(World).tracksHost(Carrier), "the recalled operative is still counted as a carrier");
            Helper.assertTrue(hivesNear(Helper, Spot).isEmpty(), "a nest grew from a recalled operative");
            clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp439_mtfForget")
    public static void recalledOperativeIsForgotten(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MtfOperative Operative = operative(Helper, 16.5, 16.5);
        UUID Carrier = Operative.getUUID();
        Scp439Colonies.get(World).seeHost(Carrier, World.getGameTime());
        Helper.assertTrue(Scp439Colonies.get(World).tracksHost(Carrier), "the carrier was not recorded");
        Operative.discard();
        Helper.startSequence().thenWaitUntil(() -> {
            Helper.assertFalse(Scp439Colonies.get(World).tracksHost(Carrier), "the recalled operative is still counted as a carrier");
        }).thenExecute(() -> clean(Helper)).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp439_mtfNest")
    public static void operativeNestKeepsItsGear(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MtfOperative Operative = operative(Helper, 16.5, 16.5);
        Vec3 Spot = Operative.position();
        Helper.assertTrue(Scp439Infection.infect(Operative, new CompoundTag(), 6000), "the operative could not carry SCP-439");
        Operative.hurt(World.damageSources().source(Scp439Registry.Hollowed), Float.MAX_VALUE);
        Helper.startSequence().thenWaitUntil(() -> {
            Helper.assertFalse(hivesNear(Helper, Spot).isEmpty(), "no nest grew from the operative");
        }).thenExecute(() -> {
            Scp439Hive Nest = hivesNear(Helper, Spot).get(0);
            Scp439Look Seen = Nest.look();
            Helper.assertTrue(Seen.Kind() == Scp439Look.KindDClass && DClassAppearance.suit(Seen.Appearance()) == DClassAppearance.Fatigues, "the nest lost the operative's fatigues");
            Helper.assertTrue(Nest.gear(EquipmentSlot.CHEST).is(GearRegistry.MtfVest.get()), "the nest lost the operative's vest");
            Helper.assertTrue(Nest.gear(EquipmentSlot.LEGS).is(GearRegistry.MtfPants.get()), "the nest lost the operative's trousers");
            Helper.assertTrue(Nest.gear(EquipmentSlot.FEET).is(GearRegistry.MtfBoots.get()), "the nest lost the operative's boots");
            Helper.assertFalse(Nest.gear(EquipmentSlot.HEAD).isEmpty(), "the nest lost the operative's helmet");
            Helper.assertFalse(Scp439Colonies.get(World).tracksHost(Operative.getUUID()), "the fallen operative is still counted as a carrier");
            clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp439_villagerBecomesNest")
    public static void infectionSlowsAndEndsInVillagerNest(GameTestHelper Helper) {
        Villager Host = villager(Helper, 16.5, 16.5);
        Vec3 Where = Host.position();
        double Before = Host.getAttributeValue(Attributes.MOVEMENT_SPEED);
        Helper.assertTrue(Scp439Infection.infect(Host, new CompoundTag(), 120), "the villager could not be infected");
        Helper.assertTrue(Host.getAttributeValue(Attributes.MOVEMENT_SPEED) < Before, "the villager is not slowed");
        Helper.runAfterDelay(70, () -> Helper.assertTrue(Scp439Infection.stageOf(Host) >= 3, "the villager did not reach a later stage: " + Scp439Infection.stageOf(Host)));
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(!Host.isAlive() || Host.isRemoved(), "the villager is still alive")).thenIdle(3).thenExecute(() -> {
            List<Scp439Hive> Found = hivesNear(Helper, Where);
            Helper.assertTrue(Found.size() == 1, "expected one nest, found " + Found.size());
            Scp439Look Shown = Found.get(0).look();
            Helper.assertTrue(Shown.Kind() == Scp439Look.KindVillager, "the nest did not keep the villager look");
            Helper.assertTrue("desert".equals(Shown.Type()) && "librarian".equals(Shown.Profession()) && Shown.Level() == 3,
                    "the villager type or profession was lost: " + Shown.Type() + "/" + Shown.Profession() + "/" + Shown.Level());
            Helper.assertTrue(Host.isRemoved(), "the villager body was left behind next to the nest");
            clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp439_dclassBecomesNest")
    public static void dclassNestKeepsAppearance(GameTestHelper Helper) {
        DClass Host = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(16.5, Floor, 16.5));
        int Code = DClassAppearance.pack(3, DClassAppearance.Curly, 2, 1, 1, 2, 1, DClassAppearance.Jumpsuit);
        Host.setAppearance(Code);
        Vec3 Where = Host.position();
        Helper.assertTrue(Scp439Infection.infect(Host, new CompoundTag(), 60), "the D-Class could not be infected");
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(!Host.isAlive() || Host.isRemoved(), "the D-Class is still alive")).thenIdle(3).thenExecute(() -> {
            List<Scp439Hive> Found = hivesNear(Helper, Where);
            Helper.assertTrue(Found.size() == 1, "expected one nest, found " + Found.size());
            Scp439Look Shown = Found.get(0).look();
            Helper.assertTrue(Shown.Kind() == Scp439Look.KindDClass && Shown.Appearance() == Code, "the nest lost the D-Class appearance");
            clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp439_playerBecomesNest")
    public static void playerNestKeepsProfile(GameTestHelper Helper) {
        Scp439TestPlayer User = player(Helper, 16.5, 16.5, "scp439_host");
        Vec3 Where = User.position();
        Helper.assertTrue(Scp439Infection.infect(User, new CompoundTag(), 60), "the player could not be infected");
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(User.isDeadOrDying(), "the player is still alive")).thenIdle(3).thenExecute(() -> {
            List<Scp439Hive> Found = hivesNear(Helper, Where);
            Helper.assertTrue(Found.size() == 1, "expected one nest, found " + Found.size());
            Scp439Look Shown = Found.get(0).look();
            Helper.assertTrue(Shown.Kind() == Scp439Look.KindPlayer, "the nest did not keep the player look");
            Helper.assertTrue(Shown.profile() != null && "scp439_host".equals(Shown.profile().name().orElse("")), "the player profile was not stored");
            Helper.assertFalse(Scp439Infection.isInfected(User), "the infection outlived the player");
            dismiss(Helper, User);
            clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp439_growth")
    public static void growthStagesReachFullEncasement(GameTestHelper Helper) {
        Scp439Hive Nest = hive(Helper, 16.5, 16.5, Scp439Hive.StartTicks, new CompoundTag());
        Helper.assertTrue(Nest.stage() == 1 && !Nest.seeking(), "a fresh nest is not at the first stage");
        Nest.setGrowth(Scp439Hive.StageTicks * 2 - 3);
        Helper.runAfterDelay(6, () -> Helper.assertTrue(Nest.stage() == 3, "growth did not advance to the third stage: " + Nest.stage()));
        Helper.runAfterDelay(8, () -> Nest.setGrowth(Scp439Hive.FullTicks - 5));
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Nest.seeking(), "the nest never became fully encased")).thenExecute(() -> {
            Helper.assertTrue(Nest.stage() == Scp439Hive.Tiers, "a fully encased nest is not at the last stage");
            Helper.assertTrue(Nest.progress(0.0f) >= Scp439Hive.Tiers, "the render progress does not reach full encasement");
            clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 3000, batch = "scp439_foodLoop")
    public static void workersHuntCarryAndDeliver(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp439Hive Nest = hive(Helper, 16.5, 16.5, Scp439Hive.FullTicks, new CompoundTag());
        Scp439Worker Worker = Scp439Registry.Worker.get().create(World);
        Nest.hatch(World, Worker);
        Vec3 Start = at(Helper, 17.5, 16.5);
        Worker.moveTo(Start.x, Start.y, Start.z, -90.0f, 0.0f);
        Worker.setYHeadRot(-90.0f);
        Worker.setYBodyRot(-90.0f);
        Pig Prey = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(20.5, Floor, 16.5));
        Prey.setHealth(1.0f);
        Helper.assertTrue(Worker.hiveId() != null && Worker.hiveId().equals(Nest.getUUID()), "the worker is not bound to its nest");
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(!Prey.isAlive(), "the worker has not killed the pig")).thenWaitUntil(() -> {
            Helper.assertTrue(Worker.isCarrying() || Nest.food() > 0, "the worker has not picked up the drops (" + around(Helper, ItemEntity.class, Nest.position(), Search).size()
                    + " items lying near the nest)");
        }).thenWaitUntil(() -> {
            Helper.assertTrue(Nest.food() > 0, "nothing was delivered to the nest yet (worker carrying " + Worker.carried() + ")");
        }).thenExecute(() -> {
            Helper.assertTrue(Nest.fed() == Nest.food(), "the fed total does not match the stores");
            Helper.assertFalse(Worker.isCarrying(), "the worker kept its load after delivering");
            clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp439_brutesGuard")
    public static void brutesTurnOnIntruders(GameTestHelper Helper) {
        Scp439Hive Nest = hive(Helper, 8.5, 16.5, Scp439Hive.FullTicks, new CompoundTag());
        Scp439Brute Brute = Scp439Registry.Brute.get().create(Helper.getLevel());
        Nest.hatch(Helper.getLevel(), Brute);
        Villager Far = villager(Helper, 26.5, 16.5);
        Villager[] Near = new Villager[1];
        Helper.startSequence().thenIdle(60).thenExecute(() -> {
            Helper.assertTrue(Brute.getTarget() == null, "the brute went after a villager outside its guard radius");
            Near[0] = villager(Helper, 15.5, 16.5);
        }).thenWaitUntil(() -> Helper.assertTrue(Brute.getTarget() == Near[0], "the brute ignored a villager within its guard radius")).thenExecute(() -> {
            dismiss(Helper, Near[0]);
            dismiss(Helper, Far);
            clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp439_nestDies")
    public static void killingTheNestCollapsesItsSpawnAndFreesTheQueen(GameTestHelper Helper) {
        CompoundTag Carried = new CompoundTag();
        Carried.putInt("Generation", 2);
        Scp439Hive Nest = hive(Helper, 16.5, 16.5, Scp439Hive.FullTicks, Carried);
        ServerLevel World = Helper.getLevel();
        Scp439Worker First = Scp439Registry.Worker.get().create(World);
        Scp439Worker Second = Scp439Registry.Worker.get().create(World);
        Scp439Brute Brute = Scp439Registry.Brute.get().create(World);
        Nest.hatch(World, First);
        Nest.hatch(World, Second);
        Nest.hatch(World, Brute);
        Vec3 Where = Nest.position();
        Helper.runAfterDelay(40, () -> Nest.hurt(World.damageSources().genericKill(), Float.MAX_VALUE));
        Helper.startSequence().thenIdle(41).thenWaitUntil(() -> {
            Helper.assertTrue(!First.isAlive() && !Second.isAlive() && !Brute.isAlive(), "a member of the colony survived the nest");
        }).thenExecute(() -> {
            List<Scp439Queen> Queens = around(Helper, Scp439Queen.class, Where, 4.0);
            Helper.assertTrue(Queens.size() == 1, "expected the queen to come out of the remains, found " + Queens.size());
            Helper.assertTrue(Queens.get(0).generation() == 2, "the freed queen forgot where she came from");
            Helper.assertFalse(Scp439Colonies.get(World).hasHive(Nest.getUUID()), "the nest is still on record");
            clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp439_secondQueen")
    public static void wellFedNestRaisesADaughterUnlessSpreadIsOff(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp439Colonies.get(World).forget();
        Scp439Hive Nest = hive(Helper, 16.5, 16.5, Scp439Hive.FullTicks, new CompoundTag());
        Nest.addFood(Scp439Hive.QueenCost);
        Scp439Registry.setSpread(World.getServer(), false);
        Helper.assertFalse(Nest.raiseQueen(World), "a daughter was raised with spreading turned off");
        Scp439Registry.setSpread(World.getServer(), true);
        Helper.assertTrue(Nest.raiseQueen(World), "the fed nest did not raise a daughter");
        Helper.assertTrue(Nest.food() == 0, "raising a daughter did not use the stores");
        List<Scp439Queen> Queens = around(Helper, Scp439Queen.class, Nest.position(), 4.0);
        Helper.assertTrue(Queens.size() == 1 && Queens.get(0).isDaughter() && Queens.get(0).stillTravelling(), "no travelling daughter came out");
        Scp439Queen Daughter = Queens.get(0);
        Helper.assertTrue(Daughter.waypoint() != null && Math.sqrt(Daughter.waypoint().distSqr(Nest.blockPosition())) >= Scp439Queen.MinTravel,
                "the daughter is not heading far enough away");
        Villager Nearby = villager(Helper, 18.5, 16.5);
        Helper.assertFalse(Daughter.wants(Nearby), "the daughter would settle right next to her nest");
        dismiss(Helper, Nearby);
        clean(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp439_cap")
    public static void colonyLimitHolds(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp439Colonies Colonies = Scp439Colonies.get(World);
        Colonies.forget();
        Scp439Hive Nest = hive(Helper, 16.5, 16.5, Scp439Hive.FullTicks, new CompoundTag());
        for (int Index = 1; Index < Scp439Colonies.Cap; Index++) {
            Colonies.addHive(UUID.randomUUID(), Nest.blockPosition().offset(100 * Index, 0, 0));
        }
        Nest.addFood(Scp439Hive.QueenCost * 3);
        Helper.assertTrue(Colonies.colonies(World.getGameTime()) >= Scp439Colonies.Cap, "the test did not fill the limit");
        Helper.assertFalse(Nest.raiseQueen(World), "a daughter was raised past the limit");
        Helper.assertTrue(around(Helper, Scp439Queen.class, Nest.position(), 6.0).isEmpty(), "a queen appeared past the limit");
        Colonies.forget();
        clean(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp439_stasis")
    public static void stasisPodCapturesAndReleasesTheQueen(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp439TestPlayer Keeper = player(Helper, 12.5, 12.5, "scp439_keeper");
        Scp439Queen Queen = Scp439Registry.Queen.get().create(World);
        Vec3 Spot = at(Helper, 14.5, 12.5);
        Queen.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addFreshEntity(Queen);
        Queen.makeDaughter(BlockPos.containing(Spot), 3);
        ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
        Queen.beginEmerge();
        Helper.assertTrue(StasisPods.refusal(Keeper, Pod, Queen) != null, "a pod could take the queen while she was emerging");
        Queen.endAction();
        Helper.assertTrue(StasisPods.refusal(Keeper, Pod, Queen) == null, "a pod refuses the queen");
        Helper.assertTrue(StasisPods.capture(World, Keeper, Pod, Queen), "the pod failed to capture the queen");
        Helper.assertTrue(Queen.isRemoved() && StasisPodItem.isOccupied(Pod), "the queen is still loose after capture");
        Keeper.setItemInHand(InteractionHand.MAIN_HAND, Pod);
        BlockPos Ground = Helper.absolutePos(new BlockPos(20, 1, 20));
        UseOnContext Context = new UseOnContext(Keeper, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(Ground).add(0.0, 0.5, 0.0), Direction.UP, Ground, false));
        Helper.assertTrue(StasisPods.release(Context, Pod), "the pod failed to release the queen");
        List<Scp439Queen> Back = around(Helper, Scp439Queen.class, Vec3.atCenterOf(Ground), 3.0);
        Helper.assertTrue(Back.size() == 1 && Back.get(0).isDaughter() && Back.get(0).generation() == 3, "the queen came back different");
        dismiss(Helper, Keeper);
        clean(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp439_reload")
    public static void everythingSurvivesSaveAndReload(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp439Hive Nest = hive(Helper, 16.5, 16.5, Scp439Hive.StageTicks * 3, new CompoundTag());
        Nest.setLook(new Scp439Look(Scp439Look.KindVillager, 0, new CompoundTag(), "snow", "farmer", 4, new CompoundTag(), new CompoundTag()));
        Nest.addFood(37);
        Scp439Hive NestCopy = reload(Helper, Nest, Scp439Hive.class);
        Helper.assertTrue(NestCopy.growth() == Nest.growth() && NestCopy.food() == 37 && NestCopy.fed() == 37, "the nest forgot its growth or stores");
        Helper.assertTrue("snow".equals(NestCopy.look().Type()) && "farmer".equals(NestCopy.look().Profession()) && NestCopy.look().Level() == 4, "the nest forgot its host");
        Scp439Worker Worker = Scp439Registry.Worker.get().create(World);
        Nest.hatch(World, Worker);
        Worker.setCarried(new ItemStack(Items.PORKCHOP, 3));
        Scp439Worker WorkerCopy = reload(Helper, Worker, Scp439Worker.class);
        Helper.assertTrue(Nest.getUUID().equals(WorkerCopy.hiveId()) && WorkerCopy.home() != null, "the worker forgot its nest");
        Helper.assertTrue(WorkerCopy.carried().getCount() == 3, "the worker dropped its load in the save");
        Scp439Queen Queen = Scp439Registry.Queen.get().create(World);
        Queen.moveTo(Nest.getX(), Nest.getY(), Nest.getZ() + 3.0, 0.0f, 0.0f);
        Queen.makeDaughter(Nest.blockPosition(), 5);
        Scp439Queen QueenCopy = reload(Helper, Queen, Scp439Queen.class);
        Helper.assertTrue(QueenCopy.isDaughter() && QueenCopy.generation() == 5 && Nest.blockPosition().equals(QueenCopy.origin())
                && Queen.waypoint().equals(QueenCopy.waypoint()), "the queen forgot her journey");
        Villager Host = villager(Helper, 6.5, 6.5);
        CompoundTag Carried = new CompoundTag();
        Carried.putInt("Generation", 7);
        Helper.assertTrue(Scp439Infection.infect(Host, Carried, 6000), "the villager could not be infected");
        Scp439Infection.setElapsed(Host, 2500);
        Villager HostCopy = reload(Helper, Host, Villager.class);
        Scp439Infection.State Copied = Scp439Infection.stateOf(HostCopy);
        Helper.assertTrue(Copied != null && Copied.Elapsed == 2500 && Copied.Duration == 6000 && Copied.Queen.getInt("Generation") == 7, "the infection timer or queen was lost");
        Scp439Colonies Colonies = Scp439Colonies.get(World);
        Scp439Colonies Loaded = Scp439Colonies.load(Colonies.save(new CompoundTag(), World.registryAccess()), World.registryAccess());
        Helper.assertTrue(Loaded.hasHive(Nest.getUUID()) && Loaded.hives() == Colonies.hives(), "the colony record did not survive");
        Helper.assertTrue(Loaded.membersOf(Nest.getUUID()).contains(Worker.getUUID()), "the colony forgot the worker");
        dismiss(Helper, Host);
        clean(Helper);
        Helper.succeed();
    }

    private static <T extends Entity> T reload(GameTestHelper Helper, Entity Source, Class<T> Kind) {
        CompoundTag Saved = new CompoundTag();
        Helper.assertTrue(Source.save(Saved), Source.getType().getDescriptionId() + " refused to save");
        Optional<Entity> Copy = EntityType.create(Saved, Helper.getLevel());
        Helper.assertTrue(Copy.isPresent() && Kind.isInstance(Copy.get()), Source.getType().getDescriptionId() + " could not be rebuilt");
        return Kind.cast(Copy.get());
    }

    public static final class DeathWatch {
        private final UUID Controller;
        private final UUID Body;
        private int ControllerDeaths;
        private int ControllerCancelled;
        private int BodyDeaths;

        public DeathWatch(UUID Controller, UUID Body) {
            this.Controller = Controller;
            this.Body = Body;
        }

        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public void died(LivingDeathEvent Event) {
            UUID Dead = Event.getEntity().getUUID();
            if (Dead.equals(Controller)) {
                ControllerDeaths++;
                if (Event.isCanceled()) {
                    ControllerCancelled++;
                }
            } else if (Dead.equals(Body) && !Event.isCanceled()) {
                BodyDeaths++;
            }
        }
    }

    @GameTest(template = Arena, timeoutTicks = 360, batch = "scp439_controlled")
    public static void controlledDClassHostDiesOnce(GameTestHelper Helper) {
        Scp439TestPlayer User = player(Helper, 14.5, 14.5, "scp439_controller");
        User.enlist(true);
        DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(16.5, Floor, 16.5));
        UUID BodyId = Body.getUUID();
        Helper.assertTrue(Scp439Infection.infect(Body, new CompoundTag(), 600), "the D-Class could not be infected");
        Helper.assertTrue(ControlManager.possess(User, Body), "the test player could not take control of the D-Class");
        Helper.assertTrue(Scp439Infection.isInfected(User), "the infection did not travel with the body");
        Vec3 Where = User.position();
        DeathWatch Watch = new DeathWatch(User.getUUID(), BodyId);
        NeoForge.EVENT_BUS.register(Watch);
        Scp439Infection.setElapsed(User, 600);
        Helper.startSequence().thenWaitUntil(() -> {
            Helper.assertFalse(ControlSessions.isControlling(User), "the controlled body has not died yet");
            Helper.assertFalse(ControlManager.hasPendingDeath(User), "a deferred death is still pending");
        }).thenIdle(30).thenExecute(() -> {
            NeoForge.EVENT_BUS.unregister(Watch);
            User.enlist(false);
            Helper.assertTrue(Watch.ControllerDeaths == 1 && Watch.ControllerCancelled == 1, "the controller saw " + Watch.ControllerDeaths + " deaths, " + Watch.ControllerCancelled + " deferred");
            Helper.assertTrue(Watch.BodyDeaths == 1, "the D-Class body died " + Watch.BodyDeaths + " times");
            Helper.assertTrue(User.isAlive(), "the controller did not return home alive");
            Helper.assertFalse(Scp439Infection.isInfected(User), "the infection followed the controller home");
            Helper.assertFalse(Scp439Infection.hasModifiers(User), "the slowness followed the controller home");
            List<Scp439Hive> Found = hivesNear(Helper, Where);
            Helper.assertTrue(Found.size() == 1 && Found.get(0).look().Kind() == Scp439Look.KindDClass, "expected one D-Class nest where the body fell, found " + Found.size());
            Entity Left = Helper.getLevel().getEntity(BodyId);
            Helper.assertTrue(Left == null || !Left.isAlive(), "the infected body is still alive");
            dismiss(Helper, User);
            clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp439_cure")
    public static void anomalyCureClearsTheHostAndFreesTheQueen(GameTestHelper Helper) {
        Villager Host = villager(Helper, 16.5, 16.5);
        CompoundTag Carried = new CompoundTag();
        Carried.putInt("Generation", 4);
        Helper.assertTrue(Scp439Infection.infect(Host, Carried, 6000), "the villager could not be infected");
        Scp439Infection.setElapsed(Host, 3000);
        Helper.assertTrue(AnomalyCure.cure(Host, null), "the cure did not report success");
        Helper.assertFalse(Scp439Infection.isInfected(Host), "the villager is still infected");
        Helper.assertFalse(Scp439Infection.hasModifiers(Host), "the villager is still slowed");
        List<Scp439Queen> Queens = around(Helper, Scp439Queen.class, Host.position(), 3.0);
        Helper.assertTrue(Queens.size() == 1, "the queen did not leave the cured host");
        Scp439Queen Queen = Queens.get(0);
        Helper.assertTrue(Queen.generation() == 4 && Queen.isEmerging(), "the freed queen is not the one that was carried");
        Helper.assertFalse(Queen.wants(Host), "the queen would go straight back into the cured host");
        dismiss(Helper, Host);
        clean(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp439_arrival")
    public static void dossierQueenArrives(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp439Registry.DocumentId);
        Helper.assertTrue(Document != null, "the scp_439 dossier did not load");
        Helper.assertTrue(Document.Classification() == DocumentClass.Keter, "SCP-439 is not filed as Keter");
        Helper.assertTrue(Document.Condition() instanceof Scp439ArrivalCondition, "SCP-439 does not use its arrival condition");
        Scp439TestPlayer Reader = player(Helper, 4.5, 4.5, "scp439_reader");
        BlockPos Spot = Helper.absolutePos(new BlockPos(24, 1, 24));
        Helper.assertTrue(Scp439ArrivalCondition.manifest(new AnomalySpawners.Discovery(World, Spot, Reader, Scp439Registry.DocumentId, Document, true)),
                "the arrival spawned nothing");
        List<Scp439Queen> Queens = around(Helper, Scp439Queen.class, Vec3.atBottomCenterOf(Spot), 3.0);
        Helper.assertTrue(Queens.size() == 1 && Queens.get(0).isEmerging(), "no emerging queen at the arrival spot");
        dismiss(Helper, Reader);
        clean(Helper);
        Helper.succeed();
    }
}
