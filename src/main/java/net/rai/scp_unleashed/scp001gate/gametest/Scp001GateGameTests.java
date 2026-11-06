package net.rai.scp_unleashed.scp001gate.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.ArchivePool;
import net.rai.scp_unleashed.archive.document.DocumentClaims;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.DocumentRoller;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.gateway.gametest.GatewayGameTests;
import net.rai.scp_unleashed.scp001gate.Scp001GateAvoid;
import net.rai.scp_unleashed.scp001gate.Scp001GateCondition;
import net.rai.scp_unleashed.scp001gate.Scp001GateEmitter;
import net.rai.scp_unleashed.scp001gate.Scp001GateGarden;
import net.rai.scp_unleashed.scp001gate.Scp001GateGuardian;
import net.rai.scp_unleashed.scp001gate.Scp001GatePlan;
import net.rai.scp_unleashed.scp001gate.Scp001GatePortalBlockEntity;
import net.rai.scp_unleashed.scp001gate.Scp001GateRegistry;
import net.rai.scp_unleashed.scp001gate.Scp001GateRepel;
import net.rai.scp_unleashed.scp001gate.Scp001GateSites;
import net.rai.scp_unleashed.scp001gate.Scp001GateZones;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp001GateGameTests {
    public static final String Arena = "gateway_arena";
    public static final int FarX = 2_600_000;
    public static final int FarZ = 2_600_000;
    public static final long Seed = 1001L;
    public static final int AvoidWatchTicks = 340;

    private Scp001GateGameTests() {
    }

    public static final class Walker extends FakePlayer {
        public Walker(ServerLevel World, GameProfile Profile) {
            super(World, Profile);
        }

        @Override
        public boolean isInvulnerableTo(DamageSource Source) {
            return false;
        }
    }

    private static BlockPos centre(GameTestHelper Helper) {
        return Helper.absolutePos(new BlockPos(4, 2, 4));
    }

    private static Scp001GateGuardian guardian(GameTestHelper Helper, BlockPos Centre) {
        ServerLevel World = Helper.getLevel();
        Scp001GateGuardian Made = Scp001GateRegistry.GuardianType.get().create(World);
        Helper.assertTrue(Made != null, "the guardian type did not create an entity");
        Made.station(Centre, Direction.SOUTH.toYRot(), new Vec3(Centre.getX() + 0.5, Centre.getY() + Scp001GatePlan.GuardianLift, Centre.getZ() + 0.5));
        World.addFreshEntity(Made);
        GatewayGameTests.whenDone(Helper, () -> {
            if (!Made.isRemoved()) {
                Made.discard();
            }
        });
        return Made;
    }

    private static void force(GameTestHelper Helper, Vec3... Spots) {
        ServerLevel World = Helper.getLevel();
        for (Vec3 Spot : Spots) {
            ChunkPos Pos = new ChunkPos(BlockPos.containing(Spot));
            World.setChunkForced(Pos.x, Pos.z, true);
        }
    }

    private static Vec3 at(BlockPos Centre, double Distance, double Angle) {
        return new Vec3(Centre.getX() + 0.5 + Math.cos(Angle) * Distance, Centre.getY(), Centre.getZ() + 0.5 + Math.sin(Angle) * Distance);
    }

    private static Walker walker(GameTestHelper Helper, Vec3 Spot, String Name, GameType Mode) {
        ServerLevel World = Helper.getLevel();
        Walker User = new Walker(World, new GameProfile(UUID.randomUUID(), Name));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(Mode);
        GatewayGameTests.whenDone(Helper, () -> GatewayGameTests.clean(User));
        return User;
    }

    private static <T extends Entity> T spawn(GameTestHelper Helper, EntityType<T> Type, Vec3 Spot) {
        T Made = Type.create(Helper.getLevel());
        Helper.assertTrue(Made != null, "could not create " + Type);
        Made.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Helper.getLevel().addFreshEntity(Made);
        GatewayGameTests.whenDone(Helper, () -> {
            if (!Made.isRemoved()) {
                Made.discard();
            }
        });
        return Made;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001gate_document")
    public static void documentInApocalypticPool(GameTestHelper Helper) {
        MinecraftServer Server = Helper.getLevel().getServer();
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp001GateRegistry.DocumentId);
        Helper.assertTrue(Document != null, "the Gate Guardian file is missing");
        Helper.assertTrue(Document.Pool() == ArchivePool.Apocalyptic, "the file is not in the apocalyptic pool");
        Helper.assertTrue(Document.Classification() == DocumentClass.Keter, "the file is not Keter");
        Helper.assertTrue(!Document.isListed(), "the file is listed with the ordinary files");
        DocumentClaims Claims = DocumentClaims.get(Server);
        Claims.release(Scp001GateRegistry.DocumentId);
        Map<ResourceLocation, AnomalyDocument> Normal = DocumentRoller.eligible(Server, null, ArchivePool.Normal);
        Map<ResourceLocation, AnomalyDocument> Gold = DocumentRoller.eligible(Server, null, ArchivePool.Apocalyptic);
        Helper.assertTrue(!Normal.containsKey(Scp001GateRegistry.DocumentId), "a normal fragment can roll the file");
        Helper.assertTrue(Gold.containsKey(Scp001GateRegistry.DocumentId), "the golden pool does not hold the file");
        int Dealt = 0;
        for (int Roll = 0; Roll < 300; Roll++) {
            if (DocumentRoller.roll(Normal, 5, 5, Helper.getLevel().random).contains(Scp001GateRegistry.DocumentId)) {
                Dealt++;
            }
        }
        Helper.assertTrue(Dealt == 0, "normal rolls dealt the file");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp001gate_site")
    public static void sitePlannedOnceWithRoughNotes(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        Scp001GateSites Data = Scp001GateSites.get(Server);
        Data.forget();
        try {
            Scp001GateSites First = Scp001GateSites.ensure(Server, new BlockPos(5000, 80, 5000), 1500, 3000, World.getRandom());
            BlockPos Centre = First.centre();
            double Distance = Math.sqrt(Math.pow(Centre.getX() - 5000, 2) + Math.pow(Centre.getZ() - 5000, 2));
            Helper.assertTrue(First.planned(), "the site was not planned");
            Helper.assertTrue(Distance >= 1490 && Distance <= 3010, "the site is " + Distance + " blocks away");
            Scp001GateSites Second = Scp001GateSites.ensure(Server, new BlockPos(-9000, 80, 9000), 1500, 3000, World.getRandom());
            Helper.assertTrue(Second.centre().equals(Centre), "a second reading moved the site");
            FakePlayer Reader = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp001gate_reader"));
            Reader.moveTo(5000.5, 80, 5000.5);
            Investigation Case = new Investigation(Scp001GateRegistry.DocumentId, World.getGameTime());
            Scp001GateCondition Condition = new Scp001GateCondition(1500, 3000, 64, 120);
            List<Component> Notes = Condition.begin(Reader, Case);
            Helper.assertTrue(Scp001GateSites.get(Server).centre().equals(Centre), "reading the file moved the site");
            boolean Rough = false;
            for (Component Line : Notes) {
                if (Line.getContents() instanceof TranslatableContents Words && Words.getKey().equals("archive.scp_unleashed.notes.site")) {
                    int ShownX = (Integer) Words.getArgs()[0];
                    int ShownZ = (Integer) Words.getArgs()[1];
                    Rough = Math.abs(ShownX - Centre.getX()) <= 69 && Math.abs(ShownZ - Centre.getZ()) <= 69;
                }
            }
            Helper.assertTrue(Rough, "the notes do not give rough coordinates near the gate");
            CompoundTag State = Case.getState();
            Helper.assertTrue(State.getInt("X") == Centre.getX() && State.getInt("Z") == Centre.getZ(), "the case does not point at the gate");
            CompoundTag Saved = First.save(new CompoundTag(), World.registryAccess());
            Scp001GateSites Loaded = Scp001GateSites.load(Saved, World.registryAccess());
            Helper.assertTrue(Loaded.centre().equals(Centre) && Loaded.front() == First.front(), "the site did not survive a save");
        } finally {
            Data.forget();
            Scp001GateEmitter.clear();
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp001gate_warn")
    public static void warningAtFiftyNotFiftyOne(GameTestHelper Helper) {
        BlockPos Centre = centre(Helper);
        Vec3 Fifty = at(Centre, 50.0, 0.0);
        Vec3 FiftyOne = at(Centre, 51.0, Math.PI);
        force(Helper, Fifty, FiftyOne);
        Scp001GateGuardian Guard = guardian(Helper, Centre);
        Walker Near = walker(Helper, Fifty, "scp001gate_fifty", GameType.SURVIVAL);
        Walker Far = walker(Helper, FiftyOne, "scp001gate_fiftyone", GameType.SURVIVAL);
        long Since = Helper.getLevel().getGameTime();
        Helper.assertTrue(Scp001GateZones.tier(50.0) == 0 && Scp001GateZones.tier(51.0) == -1 && Scp001GateZones.tier(45.0) == Scp001GateZones.Tiers - 1,
                "the warning tiers are wrong");
        Helper.runAfterDelay(6, () -> {
            Helper.assertTrue(Guard.warned(Near, Since), "a player at 50 blocks was not warned");
            Helper.assertTrue(!Guard.warned(Far, Since), "a player at 51 blocks was warned");
            Helper.assertTrue(Near.isAlive() && Near.getHealth() > 0.0f, "a player at 50 blocks was struck");
            Helper.assertTrue(Guard.gaze() == -1, "the guardian turned to watch a player it is not striking");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp001gate_warnhold")
    public static void stayingInATierShowsFewTitles(GameTestHelper Helper) {
        BlockPos Centre = centre(Helper);
        Vec3 Spot = at(Centre, 49.5, 0.0);
        force(Helper, Spot);
        Scp001GateGuardian Guard = guardian(Helper, Centre);
        Walker User = walker(Helper, Spot, "scp001gate_lingerer", GameType.CREATIVE);
        Helper.runAfterDelay(200, () -> {
            int Titles = Guard.titles(User);
            Helper.assertTrue(Titles >= 1 && Titles <= 2, "ten seconds in one tier showed " + Titles + " titles");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp001gate_warntiers")
    public static void crossingTiersShowsOneEach(GameTestHelper Helper) {
        BlockPos Centre = centre(Helper);
        double[] Steps = {49.6, 48.4, 47.1, 46.0};
        Vec3 Outside = at(Centre, 52.0, 0.0);
        force(Helper, at(Centre, 49.6, 0.0), at(Centre, 46.0, 0.0), Outside);
        Scp001GateGuardian Guard = guardian(Helper, Centre);
        Walker User = walker(Helper, at(Centre, Steps[0], 0.0), "scp001gate_pilgrim_tiers", GameType.CREATIVE);
        for (int Index = 1; Index < Steps.length; Index++) {
            Vec3 Next = at(Centre, Steps[Index], 0.0);
            Helper.runAfterDelay(10 * Index, () -> User.moveTo(Next.x, Next.y, Next.z));
        }
        Vec3 Edge = at(Centre, 49.6, 0.0);
        Vec3 Inner = at(Centre, 48.4, 0.0);
        Helper.runAfterDelay(40, () -> {
            Helper.assertTrue(Guard.titles(User) == 4, "crossing four tiers showed " + Guard.titles(User) + " titles");
            User.moveTo(Outside.x, Outside.y, Outside.z);
        });
        Helper.runAfterDelay(50, () -> User.moveTo(Edge.x, Edge.y, Edge.z));
        Helper.runAfterDelay(60, () -> User.moveTo(Outside.x, Outside.y, Outside.z));
        Helper.runAfterDelay(70, () -> User.moveTo(Inner.x, Inner.y, Inner.z));
        Helper.runAfterDelay(80, () -> User.moveTo(Outside.x, Outside.y, Outside.z));
        Helper.runAfterDelay(90, () -> User.moveTo(Edge.x, Edge.y, Edge.z));
        Helper.runAfterDelay(110, () -> {
            Helper.assertTrue(Guard.titles(User) == 4, "walking the boundary showed " + (Guard.titles(User) - 4) + " extra titles");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "scp001gate_kill")
    public static void killZoneKillsPlayersAndMobsButNotCreative(GameTestHelper Helper) {
        BlockPos Centre = centre(Helper);
        Vec3 Victim = at(Centre, 45.0, 0.0);
        Vec3 Mob0 = at(Centre, 45.0, Math.PI);
        Vec3 Builder = at(Centre, 30.0, Math.PI / 2.0);
        force(Helper, Victim, Mob0, Builder);
        Scp001GateGuardian Guard = guardian(Helper, Centre);
        Walker Player = walker(Helper, Victim, "scp001gate_victim", GameType.SURVIVAL);
        Player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        Player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        Walker Creative = walker(Helper, Builder, "scp001gate_builder", GameType.CREATIVE);
        Zombie Target = spawn(Helper, EntityType.ZOMBIE, Mob0);
        Target.setNoAi(true);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Player.getHealth() <= 0.0f, "the player at 45 blocks survived");
            Helper.assertTrue(Player.getMainHandItem().is(Items.TOTEM_OF_UNDYING) && Player.getOffhandItem().is(Items.TOTEM_OF_UNDYING), "a totem was used up");
            Helper.assertTrue(!Target.isAlive(), "the zombie at 45 blocks survived");
            Helper.assertTrue(Creative.isAlive() && Creative.getHealth() > 0.0f, "a creative player was struck");
            Helper.assertTrue(Guard.isAlive(), "the guardian died");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp001gate_anomaly")
    public static void anomaliesInsideAreStruck(GameTestHelper Helper) {
        BlockPos Centre = Scp001GateFixGameTests.isolated(Helper, Scp001GateRepel.SafeRing + 12.0);
        Vec3 First = at(Centre, 20.0, 0.0);
        Vec3 Second = at(Centre, 24.0, Math.PI);
        guardian(Helper, Centre);
        EntityType<?> Statue = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp173"));
        EntityType<?> Doctor = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp049"));
        Helper.assertTrue(Statue.is(Scp001GateRegistry.Avoiders), "SCP-173 is not a gate avoider");
        Entity Immortal = spawn(Helper, Statue, First);
        Entity Mortal = spawn(Helper, Doctor, Second);
        if (Immortal instanceof Mob Body) {
            Body.setNoAi(true);
        }
        if (Mortal instanceof Mob Body) {
            Body.setNoAi(true);
        }
        Helper.onEachTick(() -> Helper.assertTrue(Immortal.isAlive() && !Immortal.isRemoved(), "an immortal anomaly pushed inside the zone was destroyed"));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Immortal.isAlive() && Scp001GateZones.horizontal(Centre, Immortal.getX(), Immortal.getZ()) > Scp001GateZones.KillRadius,
                    "an immortal anomaly pushed inside the zone was not moved out");
            Helper.assertTrue(!Mortal.isAlive(), "an anomaly inside the zone survived");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "scp001gate_avoid")
    public static void avoidersPathAroundAndNeverEnter(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Centre = Scp001GateFixGameTests.isolated(Helper, 96.0);
        EntityType<?> Reptile = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp682"));
        EntityType<?> OldMan = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp106_old_man"));
        Helper.assertTrue(Reptile.is(Scp001GateRegistry.Avoiders) && OldMan.is(Scp001GateRegistry.Avoiders), "SCP-682 and SCP-106 are not gate avoiders");
        Vec3 Start = at(Centre, 60.0, 0.0);
        Vec3 Goal = at(Centre, 60.0, Math.PI);
        List<Vec3> Ring = new ArrayList<>();
        for (int Dx = -76; Dx <= 76; Dx += 16) {
            for (int Dz = -76; Dz <= 76; Dz += 16) {
                double Reach = Math.sqrt(Dx * Dx + Dz * Dz);
                if (Reach >= 30.0 && Reach <= 80.0) {
                    Ring.add(new Vec3(Centre.getX() + 0.5 + Dx, Centre.getY(), Centre.getZ() + 0.5 + Dz));
                }
            }
        }
        force(Helper, Ring.toArray(new Vec3[0]));
        for (Vec3 Spot : Ring) {
            World.getChunk(BlockPos.containing(Spot));
        }
        World.getChunk(BlockPos.containing(Start));
        World.getChunk(BlockPos.containing(Goal));
        int Ground = World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(Start.x), (int) Math.floor(Start.z));
        guardian(Helper, Centre);
        Zombie Runner = spawn(Helper, EntityType.ZOMBIE, new Vec3(Start.x, Ground, Start.z));
        Runner.addTag(Scp001GateAvoid.Marker);
        Runner.setPersistenceRequired();
        Runner.goalSelector.removeAllGoals(Each -> true);
        Runner.targetSelector.removeAllGoals(Each -> true);
        Scp001GateAvoid.install(Runner);
        Vec3 Target = new Vec3(Goal.x, Ground, Goal.z);
        double[] Closest = {Double.MAX_VALUE};
        double StartAngle = Math.atan2(Start.z - (Centre.getZ() + 0.5), Start.x - (Centre.getX() + 0.5));
        double[] Turned = {0.0};
        int[] Watched = {0};
        Helper.onEachTick(() -> {
            Helper.assertTrue(Runner.isAlive(), "the avoider was struck, so it entered the zone (closest " + Closest[0] + ")");
            double Distance = Scp001GateZones.horizontal(Centre, Runner.getX(), Runner.getZ());
            Closest[0] = Math.min(Closest[0], Distance);
            Helper.assertTrue(Closest[0] > Scp001GateZones.KillRadius, "the avoider came within " + Closest[0] + " blocks");
            double Angle = Math.atan2(Runner.getZ() - (Centre.getZ() + 0.5), Runner.getX() - (Centre.getX() + 0.5));
            Turned[0] = Math.max(Turned[0], Math.abs(net.minecraft.util.Mth.wrapDegrees(Math.toDegrees(Angle - StartAngle))));
            Watched[0]++;
            if (World.getGameTime() % 10 == 0) {
                Scp001GateAvoid.request(Runner, Target);
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Watched[0] >= AvoidWatchTicks, "the avoider has only been watched for " + Watched[0] + " ticks");
            Helper.assertTrue(Turned[0] >= 12.0, "the avoider did not move around the zone (turned " + Turned[0] + " degrees)");
            Vec3 Inside = at(Centre, 20.0, 1.0);
            Vec3 Before = Runner.position();
            Runner.teleportTo(Inside.x, Ground, Inside.z);
            Helper.assertTrue(Runner.position().distanceTo(Before) < 1.0e-3, "the avoider teleported into the zone");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp001gate_projectile")
    public static void projectilesBurnInTheZone(GameTestHelper Helper) {
        BlockPos Centre = centre(Helper);
        Vec3 Spot = at(Centre, 30.0, 0.5).add(0.0, 6.0, 0.0);
        Vec3 Outside = at(Centre, 70.0, 0.5).add(0.0, 6.0, 0.0);
        force(Helper, Spot, Outside);
        guardian(Helper, Centre);
        Arrow Shot = spawn(Helper, EntityType.ARROW, Spot);
        Shot.setNoGravity(true);
        ThrownEnderpearl Pearl = spawn(Helper, EntityType.ENDER_PEARL, Spot.add(1.0, 0.0, 1.0));
        Pearl.setNoGravity(true);
        Arrow Away = spawn(Helper, EntityType.ARROW, Outside);
        Away.setNoGravity(true);
        Helper.runAfterDelay(4, () -> {
            Helper.assertTrue(Shot.isRemoved(), "an arrow inside the zone was not burned");
            Helper.assertTrue(Pearl.isRemoved(), "an ender pearl inside the zone was not burned");
            Helper.assertTrue(!Away.isRemoved(), "an arrow outside the zone was burned");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001gate_invulnerable")
    public static void guardianCannotBeHurt(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp001GateGuardian Guard = guardian(Helper, centre(Helper));
        Helper.assertTrue(!Guard.hurt(World.damageSources().generic(), 1.0e6f), "the guardian accepted generic damage");
        Helper.assertTrue(!Guard.hurt(World.damageSources().fellOutOfWorld(), 1.0e6f), "the guardian accepted void damage");
        Helper.assertTrue(!Guard.hurt(World.damageSources().explosion(null, null), 1.0e6f), "the guardian accepted explosion damage");
        Helper.assertTrue(Guard.isInvulnerable() && Guard.isAlive(), "the guardian is not invulnerable");
        Helper.runAfterDelay(2, () -> {
            Helper.assertTrue(Guard.isAlive() && !Guard.isRemoved(), "the guardian did not survive");
            Guard.kill();
            Helper.assertTrue(Guard.isRemoved(), "an operator kill did not remove the guardian");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001gate_spawning")
    public static void naturalSpawningCancelledInside(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Centre = centre(Helper);
        Vec3 Inside = at(Centre, 30.0, 1.0);
        Vec3 Outside = at(Centre, 70.0, 1.0);
        force(Helper, Inside, Outside);
        guardian(Helper, Centre);
        Helper.runAfterDelay(2, () -> {
            Zombie First = EntityType.ZOMBIE.create(World);
            Zombie Second = EntityType.ZOMBIE.create(World);
            Zombie Third = EntityType.ZOMBIE.create(World);
            Third.moveTo(Inside.x, Inside.y, Inside.z);
            First.moveTo(Inside.x, Inside.y, Inside.z);
            Second.moveTo(Outside.x, Outside.y, Outside.z);
            FinalizeSpawnEvent Near = new FinalizeSpawnEvent(First, World, Inside.x, Inside.y, Inside.z, World.getCurrentDifficultyAt(BlockPos.containing(Inside)), MobSpawnType.NATURAL,
                    null, null);
            FinalizeSpawnEvent Far = new FinalizeSpawnEvent(Second, World, Outside.x, Outside.y, Outside.z, World.getCurrentDifficultyAt(BlockPos.containing(Outside)),
                    MobSpawnType.NATURAL, null, null);
            FinalizeSpawnEvent Bred = new FinalizeSpawnEvent(Third, World, Inside.x, Inside.y, Inside.z, World.getCurrentDifficultyAt(BlockPos.containing(Inside)),
                    MobSpawnType.SPAWN_EGG, null, null);
            NeoForge.EVENT_BUS.post(Near);
            NeoForge.EVENT_BUS.post(Far);
            NeoForge.EVENT_BUS.post(Bred);
            Helper.assertTrue(Near.isSpawnCancelled(), "a natural spawn inside the zone was allowed");
            Helper.assertTrue(!Far.isSpawnCancelled(), "a natural spawn outside the zone was cancelled");
            Helper.assertTrue(!Bred.isSpawnCancelled(), "a spawn egg inside the zone was cancelled");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp001gate_emit")
    public static void emitterBuildsGateAndPlain(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp001GateSites Data = new Scp001GateSites();
        int Ground = World.getHeight(Heightmap.Types.OCEAN_FLOOR, FarX, FarZ);
        World.getChunk(FarX >> 4, FarZ >> 4);
        Ground = World.getHeight(Heightmap.Types.OCEAN_FLOOR, FarX, FarZ);
        Data.plan(FarX, FarZ, Ground, Direction.SOUTH, Seed);
        BlockPos Centre = Data.centre();
        ChunkPos Home = new ChunkPos(Centre);
        World.setBlock(Centre.offset(3, 6, 0), Blocks.OAK_LOG.defaultBlockState(), 3);
        Scp001GateEmitter.emitChunk(World, Data, Home);
        Helper.assertTrue(Data.done(Home.toLong()), "the centre chunk was not recorded");
        Scp001GatePlan.Kind Expected = Scp001GatePlan.cell(-6, 0, 0, Seed);
        BlockPos Jamb = Scp001GatePlan.toWorld(Centre, Direction.SOUTH, -6, 0, 0);
        if (Jamb.getX() >> 4 == Home.x && Jamb.getZ() >> 4 == Home.z) {
            Helper.assertTrue(World.getBlockState(Jamb).is(Scp001GatePlan.state(Expected, Direction.SOUTH).getBlock()), "the jamb is " + World.getBlockState(Jamb));
        }
        BlockPos Door = Scp001GatePlan.toWorld(Centre, Direction.SOUTH, 0, 3, 0);
        Helper.assertTrue(World.getBlockState(Door).is(Scp001GateRegistry.Portal.get()), "the doorway holds " + World.getBlockState(Door));
        Helper.assertTrue(World.getBlockEntity(Door) instanceof Scp001GatePortalBlockEntity Portal && Portal.bound() && Portal.centre().equals(Centre),
                "the threshold is not bound to the gate");
        Helper.assertTrue(World.getBlockState(Scp001GatePlan.toWorld(Centre, Direction.SOUTH, 0, 3, 1)).isAir(), "the passage is blocked");
        Helper.assertTrue(!World.getBlockState(Centre.offset(3, 6, 0)).is(Blocks.OAK_LOG), "a tree survived inside the plain");
        BlockPos Plain = Centre.offset(6, -1, 7);
        if (Plain.getX() >> 4 == Home.x && Plain.getZ() >> 4 == Home.z) {
            Helper.assertTrue(Scp001GatePlan.top(6, 7, Seed) == null || World.getBlockState(Plain).is(Scp001GatePlan.top(6, 7, Seed).getBlock())
                    || World.getBlockState(Plain).getBlock() != Blocks.AIR, "the plain was not laid at " + Plain);
        }
        Scp001GateSites Again = Scp001GateSites.load(Data.save(new CompoundTag(), World.registryAccess()), World.registryAccess());
        Helper.assertTrue(Again.done(Home.toLong()) && Again.finished() == 1, "progress did not survive a save");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp001gate_garden")
    public static void creativeTravelsToTheGardenAndBack(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = Helper.getLevel();
            Scp001GateGarden.standIn(Level.END);
            GatewayGameTests.whenDone(Helper, () -> Scp001GateGarden.standIn(null));
            BlockPos Gate = centre(Helper);
            FakePlayer User = GatewayGameTests.person(Helper, 4.5, 7.5, "scp001gate_pilgrim");
            User.setGameMode(GameType.CREATIVE);
            Helper.assertTrue(Scp001GateGarden.enter(User, Gate, Direction.SOUTH), "the gate refused a creative player");
            Helper.assertTrue(Scp001GateGarden.inside(User.level()), "the player did not arrive in the garden");
            ServerLevel Garden = (ServerLevel) User.level();
            BlockPos Origin = Scp001GateGarden.origin();
            BlockPos Arch = Garden.getBlockState(Origin.offset(0, 3, 0)).is(Scp001GateRegistry.Portal.get()) ? Origin.offset(0, 3, 0) : null;
            for (int Dy = -20; Dy <= 100 && Arch == null; Dy++) {
                if (Garden.getBlockState(Origin.offset(0, Dy, 0)).is(Scp001GateRegistry.Portal.get())) {
                    Arch = Origin.offset(0, Dy, 0);
                }
            }
            Helper.assertTrue(Arch != null, "the return arch was not built");
            Helper.assertTrue(Garden.getBlockEntity(Arch) instanceof Scp001GatePortalBlockEntity Portal && Portal.leadsHome(), "the return arch does not lead home");
            Helper.assertTrue(User.position().distanceTo(Vec3.atBottomCenterOf(Arch)) < 12.0, "the player did not arrive by the return arch");
            Scp001GateGarden.leave(User);
            Helper.assertTrue(User.level().dimension() == World.dimension(), "the player did not come back");
            Helper.assertTrue(User.position().distanceTo(Vec3.atBottomCenterOf(Gate)) < 10.0, "the player came back far from the gate: " + User.position());
            Helper.succeed();
        });
    }
}
