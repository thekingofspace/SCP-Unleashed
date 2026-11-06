package net.rai.scp_unleashed.scp001gate.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gateway.gametest.GatewayGameTests;
import net.rai.scp_unleashed.scp001gate.Scp001GateAvoid;
import net.rai.scp_unleashed.scp001gate.Scp001GateBlade;
import net.rai.scp_unleashed.scp001gate.Scp001GateEmitter;
import net.rai.scp_unleashed.scp001gate.Scp001GateGarden;
import net.rai.scp_unleashed.scp001gate.Scp001GateGuardian;
import net.rai.scp_unleashed.scp001gate.Scp001GatePlan;
import net.rai.scp_unleashed.scp001gate.Scp001GateRegistry;
import net.rai.scp_unleashed.scp001gate.Scp001GateRepel;
import net.rai.scp_unleashed.scp001gate.Scp001GateSites;
import net.rai.scp_unleashed.scp001gate.Scp001GateStrike;
import net.rai.scp_unleashed.scp001gate.Scp001GateZones;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp001GateFixGameTests {
    public static final String Arena = "gateway_arena";
    public static final String GeoPath = "/assets/scp_unleashed/geo/entity/scp001_gate/scp001_gate_guardian.geo.json";
    public static final String AnimationPath = "/assets/scp_unleashed/animations/entity/scp001_gate/scp001_gate_guardian.animation.json";
    public static final double AxisTolerance = 1.0e-3;
    public static final float[] Facings = {0.0f, 90.0f, 225.0f};
    public static final int HoldTicks = 40;
    public static final int StatueHoldTicks = 160;
    public static final int IsolatedX = -2_900_000;
    public static final int IsolatedZ = -2_900_000;

    private Scp001GateFixGameTests() {
    }

    private record Pose(String Clip, float Time) {
    }

    private record Joint(String Name, String Parent, float[] Pivot, float[] Rotation, List<float[][]> Cubes) {
    }

    private static BlockPos centre(GameTestHelper Helper) {
        return Helper.absolutePos(new BlockPos(4, 2, 4));
    }

    private static Vec3 at(BlockPos Centre, double Distance, double Angle) {
        return new Vec3(Centre.getX() + 0.5 + Math.cos(Angle) * Distance, Centre.getY(), Centre.getZ() + 0.5 + Math.sin(Angle) * Distance);
    }

    private static Vec3 grounded(ServerLevel World, Vec3 Spot) {
        World.getChunk(BlockPos.containing(Spot));
        int Ground = World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(Spot.x), Mth.floor(Spot.z));
        return new Vec3(Spot.x, Ground, Spot.z);
    }

    private static void force(GameTestHelper Helper, BlockPos Centre, double... Angles) {
        ServerLevel World = Helper.getLevel();
        for (double Angle : Angles) {
            for (double Reach = 0.0; Reach <= Scp001GateRepel.SafeRing + 8.0; Reach += 8.0) {
                ChunkPos Pos = new ChunkPos(BlockPos.containing(at(Centre, Reach, Angle)));
                World.setChunkForced(Pos.x, Pos.z, true);
                World.getChunk(Pos.x, Pos.z);
            }
        }
    }

    public static BlockPos isolated(GameTestHelper Helper, double Reach) {
        ServerLevel World = Helper.getLevel();
        int X = IsolatedX;
        int Z = IsolatedZ;
        ChunkPos Home = new ChunkPos(new BlockPos(X, 0, Z));
        int Span = (int) Math.ceil(Reach / 16.0) + 1;
        List<ChunkPos> Forced = new ArrayList<>();
        for (int Dx = -Span; Dx <= Span; Dx++) {
            for (int Dz = -Span; Dz <= Span; Dz++) {
                if (Dx * Dx + Dz * Dz > Span * Span) {
                    continue;
                }
                ChunkPos Each = new ChunkPos(Home.x + Dx, Home.z + Dz);
                World.setChunkForced(Each.x, Each.z, true);
                World.getChunk(Each.x, Each.z);
                Forced.add(Each);
            }
        }
        GatewayGameTests.whenDone(Helper, () -> {
            for (ChunkPos Each : Forced) {
                World.setChunkForced(Each.x, Each.z, false);
            }
        });
        return new BlockPos(X, World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z), Z);
    }

    private static Scp001GateGuardian guardian(GameTestHelper Helper, BlockPos Centre) {
        ServerLevel World = Helper.getLevel();
        Scp001GateGuardian Made = Scp001GateRegistry.GuardianType.get().create(World);
        Helper.assertTrue(Made != null, "the guardian type did not create an entity");
        Made.station(Centre, Direction.SOUTH.toYRot(), new Vec3(Centre.getX() + 0.5, Centre.getY() + Scp001GatePlan.GuardianLift, Centre.getZ() + 0.5));
        World.addFreshEntity(Made);
        tidy(Helper, Made);
        return Made;
    }

    private static void tidy(GameTestHelper Helper, Entity Made) {
        GatewayGameTests.whenDone(Helper, () -> {
            if (!Made.isRemoved()) {
                Made.discard();
            }
        });
    }

    private static <T extends Entity> T spawn(GameTestHelper Helper, EntityType<T> Type, Vec3 Spot) {
        T Made = Type.create(Helper.getLevel());
        Helper.assertTrue(Made != null, "could not create " + Type);
        Made.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Helper.getLevel().addFreshEntity(Made);
        tidy(Helper, Made);
        return Made;
    }

    private static Entity anomaly(GameTestHelper Helper, String Id, Vec3 Spot) {
        EntityType<?> Type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Id));
        Helper.assertTrue(Type.is(Scp001GateRegistry.Avoiders), Id + " is not a gate avoider");
        Entity Made = spawn(Helper, Type, Spot);
        if (Made instanceof Mob Body) {
            Body.setNoAi(true);
            Body.setPersistenceRequired();
        }
        return Made;
    }

    private static Zombie marked(GameTestHelper Helper, Vec3 Spot, boolean Thinking) {
        Zombie Made = spawn(Helper, EntityType.ZOMBIE, Spot);
        Made.addTag(Scp001GateAvoid.Marker);
        Made.setPersistenceRequired();
        Made.goalSelector.removeAllGoals(Each -> true);
        Made.targetSelector.removeAllGoals(Each -> true);
        Made.setNoAi(!Thinking);
        return Made;
    }

    private static double distance(BlockPos Centre, Entity Body) {
        return Scp001GateZones.horizontal(Centre, Body.getX(), Body.getZ());
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp001gate_immortal")
    public static void immortalsInsideArePushedOutAlive(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = Helper.getLevel();
            BlockPos Centre = isolated(Helper, Scp001GateRepel.SafeRing + 12.0);
            double[] Angles = {1.2, 2.4, 3.6, 4.8, 5.6};
            Scp001GateGuardian Guard = guardian(Helper, Centre);
            Entity Shy = anomaly(Helper, "scp096_shy_guy", grounded(World, at(Centre, 40.0, Angles[0])));
            Entity Reptile = anomaly(Helper, "scp682", grounded(World, at(Centre, 30.0, Angles[1])));
            Zombie Walker = marked(Helper, grounded(World, at(Centre, 43.5, Angles[2])), true);
            Boat Raft = spawn(Helper, EntityType.BOAT, grounded(World, at(Centre, 36.0, Angles[3])));
            Zombie Rider = marked(Helper, Raft.position(), false);
            Rider.startRiding(Raft, true);
            Villager Untouchable = spawn(Helper, EntityType.VILLAGER, grounded(World, at(Centre, 25.0, Angles[4])));
            Untouchable.setInvulnerable(true);
            Untouchable.setNoAi(true);
            List<Entity> Immortals = List.of(Shy, Reptile, Walker, Raft, Rider);
            double[] Jump = {0.0};
            Vec3[] Last = {Walker.position()};
            int[] Outside = {0};
            Helper.onEachTick(() -> {
                boolean All = true;
                for (Entity Body : Immortals) {
                    Helper.assertTrue(Body.isAlive() && !Body.isRemoved(), Body.getType() + " was destroyed by the guardian");
                    All &= distance(Centre, Body) > Scp001GateZones.KillRadius;
                }
                Helper.assertTrue(Untouchable.isAlive() && !Untouchable.isRemoved(), "an invulnerable villager was destroyed by the guardian");
                Outside[0] = All ? Outside[0] + 1 : 0;
                Jump[0] = Math.max(Jump[0], Walker.position().distanceTo(Last[0]));
                Last[0] = Walker.position();
            });
            Helper.succeedWhen(() -> {
                for (Entity Body : Immortals) {
                    Helper.assertTrue(distance(Centre, Body) > Scp001GateZones.KillRadius, Body.getType() + " is still inside the kill zone at " + distance(Centre, Body));
                }
                Helper.assertTrue(Outside[0] >= HoldTicks, "the immortals have only stayed outside for " + Outside[0] + " ticks");
                Helper.assertTrue(distance(Centre, Walker) >= Scp001GateRepel.SafeRing - 1.0, "the walking avoider stopped at " + distance(Centre, Walker));
                Helper.assertTrue(Rider.getVehicle() == Raft, "the boat lost its immortal passenger");
                Helper.assertTrue(Jump[0] < 2.5, "the walking avoider was teleported instead of shoved (jump " + Jump[0] + ")");
                Helper.assertTrue(Guard.kills() == 0, "the guardian killed " + Guard.kills() + " things");
                Helper.assertTrue(distance(Centre, Untouchable) < Scp001GateZones.KillRadius, "an untagged invulnerable villager was moved");
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp001gate_immortalstatue")
    public static void luredStatueStaysOutside(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = Helper.getLevel();
            BlockPos Centre = isolated(Helper, Scp001GateRepel.SafeRing + 12.0);
            Scp001GateGuardian Guard = guardian(Helper, Centre);
            Villager Bait = spawn(Helper, EntityType.VILLAGER, grounded(World, at(Centre, 12.0, 0.3)));
            Bait.setInvulnerable(true);
            Bait.setNoAi(true);
            Bait.setPersistenceRequired();
            Entity Statue = anomaly(Helper, "scp173", grounded(World, at(Centre, 20.0, 0.0)));
            boolean[] Left = {false};
            int[] Outside = {0};
            Helper.onEachTick(() -> {
                Helper.assertTrue(Statue.isAlive() && !Statue.isRemoved(), "SCP-173 was destroyed by the guardian");
                Helper.assertTrue(Bait.isAlive() && !Bait.isRemoved(), "the bait was destroyed");
                double Distance = distance(Centre, Statue);
                if (Distance > Scp001GateZones.KillRadius) {
                    Left[0] = true;
                    Outside[0]++;
                } else {
                    Helper.assertTrue(!Left[0], "SCP-173 stepped back into the kill zone to " + Distance);
                }
            });
            Helper.succeedWhen(() -> {
                Helper.assertTrue(Left[0], "SCP-173 was never moved out of the kill zone");
                Helper.assertTrue(Outside[0] >= StatueHoldTicks, "SCP-173 has only been outside for " + Outside[0] + " ticks");
                Helper.assertTrue(Guard.kills() == 0, "the guardian killed " + Guard.kills() + " things");
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp001gate_spared")
    public static void killPathsSpareImmortalsAndTheirMounts(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = Helper.getLevel();
            BlockPos Centre = centre(Helper);
            force(Helper, Centre, 0.0, Math.PI);
            DamageSource Source = Scp001GateStrike.source(World, null);
            Vec3 Yard = grounded(World, at(Centre, 6.0, 0.0));
            Zombie Immortal = marked(Helper, Yard, false);
            Zombie Rider = spawn(Helper, EntityType.ZOMBIE, Yard);
            Rider.setNoAi(true);
            Rider.startRiding(Immortal, true);
            Boat Raft = spawn(Helper, EntityType.BOAT, Yard.add(3.0, 0.0, 0.0));
            Zombie Passenger = marked(Helper, Raft.position(), false);
            Passenger.startRiding(Raft, true);
            Villager Flagged = spawn(Helper, EntityType.VILLAGER, Yard.add(0.0, 0.0, 3.0));
            Flagged.setInvulnerable(true);
            Flagged.setNoAi(true);
            Zombie Plain = spawn(Helper, EntityType.ZOMBIE, Yard.add(-3.0, 0.0, 0.0));
            Plain.setNoAi(true);
            Helper.assertTrue(!Scp001GateStrike.eligible(Immortal), "an avoider is a strike target");
            Helper.assertTrue(!Scp001GateStrike.eligible(Rider), "a mob riding an avoider is a strike target");
            Helper.assertTrue(!Scp001GateStrike.eligible(Raft), "a boat carrying an avoider is a strike target");
            Helper.assertTrue(!Scp001GateStrike.eligible(Flagged), "an invulnerable villager is a strike target");
            Helper.assertTrue(Scp001GateStrike.eligible(Plain), "a plain zombie is not a strike target");
            Helper.assertTrue(Scp001GateStrike.smite(World, Immortal, Source) == 0 && Immortal.isAlive(), "smiting an avoider hurt it");
            Helper.assertTrue(Scp001GateStrike.smite(World, Raft, Source) == 0 && !Raft.isRemoved() && Passenger.isAlive(), "smiting a boat with an avoider broke it");
            Helper.assertTrue(Scp001GateStrike.smite(World, Flagged, Source) == 0 && Flagged.isAlive(), "smiting an invulnerable villager hurt it");
            Helper.assertTrue(!Scp001GateStrike.burnable(Immortal), "an avoider counts as burnable");
            Helper.assertTrue(Scp001GateStrike.clear(World, Centre, new AABB(Yard, Yard).inflate(6.0)) == 1, "clearing the yard did not remove only the plain zombie");
            Helper.assertTrue(Plain.isRemoved() && Immortal.isAlive() && Rider.isAlive() && Flagged.isAlive() && Passenger.isAlive() && !Raft.isRemoved(),
                    "clearing the yard touched a spared entity");
            Vec3 Edge = grounded(World, at(Centre, 44.0, Math.PI));
            Zombie Doomed = spawn(Helper, EntityType.ZOMBIE, Edge);
            Doomed.setNoAi(true);
            Zombie Neighbour = marked(Helper, Edge.add(0.0, 0.0, 1.2), false);
            Scp001GateGuardian Guard = guardian(Helper, Centre);
            Helper.succeedWhen(() -> {
                Helper.assertTrue(!Doomed.isAlive(), "the plain zombie by the immortal survived");
                Helper.assertTrue(Neighbour.isAlive() && !Neighbour.isRemoved(), "the splash of a strike destroyed an immortal neighbour");
                Helper.assertTrue(Immortal.isAlive() && Rider.isAlive() && Passenger.isAlive() && Flagged.isAlive() && !Raft.isRemoved(), "a spared entity died");
                Helper.assertTrue(Guard.isAlive(), "the guardian died");
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp001gate_gaze")
    public static void guardianLooksForwardUntilItHasATarget(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = Helper.getLevel();
            BlockPos Centre = centre(Helper);
            force(Helper, Centre, 0.0, Math.PI / 2.0);
            Scp001GateGuardian Guard = guardian(Helper, Centre);
            float Forward = Guard.outward();
            Vec3 Edge = at(Centre, 48.5, Math.PI / 2.0);
            FakePlayer Passer = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp001gate_passer"));
            Passer.moveTo(Edge.x, Edge.y, Edge.z, 0.0f, 0.0f);
            World.addNewPlayer(Passer);
            Passer.setGameMode(GameType.SURVIVAL);
            GatewayGameTests.whenDone(Helper, () -> GatewayGameTests.clean(Passer));
            Zombie Creature = spawn(Helper, EntityType.ZOMBIE, grounded(World, at(Centre, 70.0, 0.0)));
            Creature.setNoAi(true);
            boolean[] Saw = {false};
            float[] Widest = {0.0f};
            Helper.onEachTick(() -> {
                if (Creature.isAlive() && Guard.gaze() == Creature.getId()) {
                    Saw[0] = true;
                }
                Widest[0] = Math.max(Widest[0], Math.abs(Mth.wrapDegrees(Guard.facing() - Forward)));
            });
            Helper.runAfterDelay(20, () -> {
                Helper.assertTrue(Guard.gaze() == -1, "the guardian watched a passer-by with nothing to strike");
                Helper.assertTrue(Widest[0] < 0.01f, "the guardian turned " + Widest[0] + " degrees with nothing to strike");
                Vec3 Inside = grounded(World, at(Centre, 30.0, 0.0));
                Creature.teleportTo(Inside.x, Inside.y, Inside.z);
            });
            Helper.runAfterDelay(100, () -> {
                Helper.assertTrue(!Creature.isAlive(), "the target was not struck");
                Helper.assertTrue(Saw[0], "the guardian never looked at its target");
                Helper.assertTrue(Widest[0] > 45.0f, "the guardian did not turn toward its target (" + Widest[0] + " degrees)");
                Helper.assertTrue(Guard.gaze() == -1, "the guardian kept watching after the target was gone");
                Helper.assertTrue(Math.abs(Mth.wrapDegrees(Guard.facing() - Forward)) < 0.5f, "the guardian did not return to forward: " + Guard.facing());
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp001gate_settle")
    public static void freshSiteStartsWithNoMobsInside(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = Helper.getLevel();
            BlockPos Centre = centre(Helper);
            force(Helper, Centre, 0.0, 2.0, 4.0);
            Zombie Near = spawn(Helper, EntityType.ZOMBIE, grounded(World, at(Centre, 20.0, 0.0)));
            Zombie Far = spawn(Helper, EntityType.ZOMBIE, grounded(World, at(Centre, 40.0, 2.0)));
            Cow Grazer = spawn(Helper, EntityType.COW, grounded(World, at(Centre, 30.0, 4.0)));
            Zombie Outside = spawn(Helper, EntityType.ZOMBIE, grounded(World, at(Centre, 52.0, 2.0)));
            Zombie Avoider = marked(Helper, grounded(World, at(Centre, 35.0, 4.0)), false);
            for (Mob Each : new Mob[]{Near, Far, Grazer, Outside}) {
                Each.setNoAi(true);
            }
            Scp001GateSites Plan = new Scp001GateSites();
            Plan.plan(Centre.getX(), Centre.getZ(), Centre.getY(), Direction.SOUTH, 7L);
            Zombie Chunked = spawn(Helper, EntityType.ZOMBIE, grounded(World, new Vec3(Centre.getX() + 0.5, Centre.getY(), Centre.getZ() + 0.5)));
            Chunked.setNoAi(true);
            int Settled = Scp001GateEmitter.settle(World, Plan, new ChunkPos(Centre));
            Helper.assertTrue(Settled >= 1 && Chunked.isRemoved() && Chunked.getRemovalReason() == Entity.RemovalReason.DISCARDED, "building the centre chunk left a mob inside");
            Vec3 Post = new Vec3(Centre.getX() + 0.5, Centre.getY() + Scp001GatePlan.GuardianLift, Centre.getZ() + 0.5);
            Scp001GateGuardian Guard = Scp001GateGuardian.post(World, Centre, Direction.SOUTH, Post, true);
            Helper.assertTrue(Guard != null && Guard.calm() > 0, "a fresh site guardian does not start calm");
            tidy(Helper, Guard);
            Helper.runAfterDelay(30, () -> {
                for (Mob Each : new Mob[]{Near, Far, Grazer}) {
                    Helper.assertTrue(Each.isRemoved() && Each.getRemovalReason() == Entity.RemovalReason.DISCARDED, Each.getType() + " was left inside a fresh site");
                }
                Helper.assertTrue(Outside.isAlive() && !Outside.isRemoved(), "a mob outside the zone was cleared");
                Helper.assertTrue(Avoider.isAlive() && distance(Centre, Avoider) > Scp001GateZones.KillRadius, "the immortal was not moved out alive");
                Helper.assertTrue(Guard.kills() == 0 && Guard.strikeStart() < 0, "the fresh guardian struck instead of clearing quietly");
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp001gate_gardenempty")
    public static void freshGardenHasNoMobs(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = Helper.getLevel();
            Registry<Biome> Biomes = World.registryAccess().registryOrThrow(Registries.BIOME);
            Biome Meadow = Biomes.get(Scp001GateRegistry.location("scp001_garden"));
            Helper.assertTrue(Meadow != null, "the garden biome is missing");
            MobSpawnSettings Spawns = Meadow.getMobSettings();
            for (MobCategory Category : MobCategory.values()) {
                Helper.assertTrue(Spawns.getMobs(Category).isEmpty(), "the garden biome spawns " + Category.getName());
            }
            Registry<NoiseGeneratorSettings> Noise = World.registryAccess().registryOrThrow(Registries.NOISE_SETTINGS);
            NoiseGeneratorSettings Settings = Noise.get(Scp001GateRegistry.location("scp001_garden"));
            Helper.assertTrue(Settings != null && Settings.disableMobGeneration(), "the garden generates mobs with its chunks");
            Optional<Registry<LevelStem>> Stems = World.registryAccess().registry(Registries.LEVEL_STEM);
            if (Stems.isPresent()) {
                LevelStem Stem = Stems.get().get(ResourceKey.create(Registries.LEVEL_STEM, Scp001GateRegistry.location("scp001_garden")));
                if (Stem != null) {
                    ChunkGenerator Generator = Stem.generator();
                    Helper.assertTrue(Generator instanceof NoiseBasedChunkGenerator Noisy && Noisy.generatorSettings().is(Scp001GateRegistry.location("scp001_garden")),
                            "the garden does not use its mob free generator");
                }
            }
            Scp001GateGarden.standIn(Level.END);
            GatewayGameTests.whenDone(Helper, () -> Scp001GateGarden.standIn(null));
            ServerLevel Garden = Scp001GateGarden.level(World.getServer());
            Helper.assertTrue(Garden != null, "the garden stand in is missing");
            BlockPos Home = Scp001GateGarden.prepare(Garden);
            BlockPos Origin = Scp001GateGarden.origin();
            AABB Area = new AABB(Origin).inflate(64.0, 256.0, 64.0).move(0.0, 128.0, Scp001GateGarden.TreeAhead / 2.0);
            Helper.runAfterDelay(20, () -> {
                List<Mob> Found = Garden.getEntitiesOfClass(Mob.class, Area);
                Helper.assertTrue(Found.isEmpty(), "a fresh garden holds " + Found.size() + " mobs");
                Map<MobSpawnType, Boolean> Expected = new HashMap<>();
                Expected.put(MobSpawnType.NATURAL, true);
                Expected.put(MobSpawnType.CHUNK_GENERATION, true);
                Expected.put(MobSpawnType.PATROL, true);
                Expected.put(MobSpawnType.SPAWN_EGG, false);
                Expected.put(MobSpawnType.COMMAND, false);
                Vec3 Spot = Vec3.atBottomCenterOf(Home).add(0.0, 0.0, 6.0);
                for (Map.Entry<MobSpawnType, Boolean> Entry : Expected.entrySet()) {
                    Zombie Probe = EntityType.ZOMBIE.create(Garden);
                    Probe.moveTo(Spot.x, Spot.y, Spot.z);
                    FinalizeSpawnEvent Event = new FinalizeSpawnEvent(Probe, Garden, Spot.x, Spot.y, Spot.z, Garden.getCurrentDifficultyAt(BlockPos.containing(Spot)), Entry.getKey(),
                            null, null);
                    NeoForge.EVENT_BUS.post(Event);
                    Helper.assertTrue(Event.isSpawnCancelled() == Entry.getValue(), Entry.getKey() + " spawning in the garden was " + (Event.isSpawnCancelled() ? "cancelled" : "allowed"));
                }
                Vec3 Away = Spot.add(Scp001GateGarden.StandinReach * 4.0, 0.0, 0.0);
                Zombie Stray = EntityType.ZOMBIE.create(Garden);
                Stray.moveTo(Away.x, Away.y, Away.z);
                FinalizeSpawnEvent Elsewhere = new FinalizeSpawnEvent(Stray, Garden, Away.x, Away.y, Away.z, Garden.getCurrentDifficultyAt(BlockPos.containing(Away)),
                        MobSpawnType.NATURAL, null, null);
                NeoForge.EVENT_BUS.post(Elsewhere);
                Helper.assertTrue(!Elsewhere.isSpawnCancelled(), "the stand in blocked spawns outside the garden area");
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp001gate_swingclock")
    public static void swingShowsForLateViewers(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = Helper.getLevel();
            BlockPos Centre = centre(Helper);
            force(Helper, Centre, 0.0);
            Scp001GateGuardian Guard = guardian(Helper, Centre);
            Zombie Target = spawn(Helper, EntityType.ZOMBIE, grounded(World, at(Centre, 30.0, 0.0)));
            Target.setNoAi(true);
            Guard.tickCount += 54321;
            Guard.strikeNow(World, Target);
            Guard.tickCount -= 98765;
            float Age = Guard.strikeTime(0.0f);
            Helper.assertTrue(Guard.striking(), "the swing is hidden from a viewer whose tick counter differs (age " + Age + ")");
            Helper.assertTrue(Age >= Scp001GateGuardian.StrikeImpact - 2 && Age <= Scp001GateGuardian.StrikeImpact, "the swing started at the wrong point for a late viewer: " + Age);
            Helper.assertTrue(Guard.newStrike(), "a late viewer never hears about the swing");
            long Start = Guard.strikeStart();
            Helper.runAfterDelay(Scp001GateGuardian.StrikeLength + 4, () -> {
                Helper.assertTrue(!Guard.striking(), "the swing never ended");
                Helper.assertTrue(Guard.strikeStart() == Start || Guard.strikeStart() > Start, "the swing clock went backwards");
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001gate_blade")
    public static void bladeFlamesFollowTheSword(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            Map<String, Joint> Joints = joints(Helper);
            JsonObject Clips = json(Helper, AnimationPath).getAsJsonObject("animations");
            Joint Sword = Joints.get(Scp001GateBlade.Bone);
            Helper.assertTrue(Sword != null, "the model has no sword bone");
            Helper.assertTrue(close(Sword.Pivot()[0], Scp001GateBlade.PivotX) && close(Sword.Pivot()[1], Scp001GateBlade.PivotY) && close(Sword.Pivot()[2], Scp001GateBlade.PivotZ),
                    "the blade pivot does not match the sword bone");
            float[][] Blade = null;
            float Lowest = Float.MAX_VALUE;
            for (float[][] Cube : Sword.Cubes()) {
                if (Blade == null || Cube[1][1] > Blade[1][1]) {
                    Blade = Cube;
                }
                Lowest = Math.min(Lowest, Cube[0][1]);
            }
            Helper.assertTrue(Blade != null, "the sword has no cubes");
            Helper.assertTrue(close(Blade[0][0] + Blade[1][0] / 2.0f, Scp001GateBlade.BladeX) && close(Blade[0][2] + Blade[1][2] / 2.0f, Scp001GateBlade.BladeZ),
                    "the flame line is off the blade axis");
            Helper.assertTrue(close(Lowest, Scp001GateBlade.TipY), "the flame tip " + Scp001GateBlade.TipY + " is not the blade tip " + Lowest);
            Helper.assertTrue(Scp001GateBlade.GuardY <= Blade[0][1] + Blade[1][1] && Scp001GateBlade.GuardY >= Blade[0][1], "the flame guard is off the blade");
            Helper.assertTrue(Scp001GateBlade.HalfWidth <= Blade[1][0] / 2.0f && Scp001GateBlade.HalfDepth <= Blade[1][2] / 2.0f, "the flame spread is wider than the blade");
            List<Joint> Chain = new ArrayList<>();
            for (Joint Step = Sword; Step != null; Step = Step.Parent() == null ? null : Joints.get(Step.Parent())) {
                Chain.add(0, Step);
            }
            Pose[] Poses = {new Pose("idle", 0.0f), new Pose("idle", 3.0f), new Pose("alert", 1.0f), new Pose("strike", 0.24f), new Pose("strike", 0.32f), new Pose("strike", 0.4f)};
            float Scale = Scp001GateGuardian.RenderScale;
            float Spread = (float) Math.sqrt(Scp001GateBlade.HalfWidth * Scp001GateBlade.HalfWidth + Scp001GateBlade.HalfDepth * Scp001GateBlade.HalfDepth) / 16.0f * Scale;
            Vec3 IdleTip = null;
            Vec3 SwingTip = null;
            for (Pose Each : Poses) {
                JsonObject Clip = Clips.getAsJsonObject(Each.Clip());
                Helper.assertTrue(Clip != null, "the " + Each.Clip() + " animation is missing");
                Matrix4f AtPivot = chain(Chain, Clip.getAsJsonObject("bones"), Each.Time(), false);
                Matrix4f Rendered = chain(Chain, Clip.getAsJsonObject("bones"), Each.Time(), true);
                for (float Facing : Facings) {
                    Matrix4f Placement = Scp001GateBlade.placement(Facing, Scale);
                    Matrix4f BladePose = Scp001GateBlade.pose(Placement, AtPivot);
                    Matrix4f Cubes = new Matrix4f(Placement).mul(Rendered);
                    Vec3 Guard = cube(Cubes, Scp001GateBlade.BladeX, Scp001GateBlade.GuardY, Scp001GateBlade.BladeZ);
                    Vec3 Tip = cube(Cubes, Scp001GateBlade.BladeX, Scp001GateBlade.TipY, Scp001GateBlade.BladeZ);
                    String Where = Each.Clip() + " " + Each.Time() + "s facing " + Facing;
                    Helper.assertTrue(Scp001GateBlade.guard(BladePose).distanceTo(Guard) < AxisTolerance, "the flame guard left the blade in " + Where);
                    Helper.assertTrue(Scp001GateBlade.tip(BladePose).distanceTo(Tip) < AxisTolerance, "the flame tip left the blade in " + Where);
                    Helper.assertTrue(Math.abs(Guard.distanceTo(Tip) - Scp001GateBlade.length(Scale)) < 0.01, "the blade is not at render scale in " + Where);
                    for (int Step = 0; Step <= 8; Step++) {
                        float Share = Step / 8.0f;
                        for (float Across = -1.0f; Across <= 1.0f; Across += 0.5f) {
                            for (float Through = -1.0f; Through <= 1.0f; Through += 1.0f) {
                                Vec3 Flame = Scp001GateBlade.point(BladePose, Share, Across, Through);
                                double Off = segment(Flame, Guard, Tip);
                                Helper.assertTrue(Off <= Spread + AxisTolerance, "a flame sits " + Off + " blocks off the blade in " + Where);
                            }
                        }
                    }
                    if (Facing == 0.0f && Each.Clip().equals("idle") && Each.Time() == 0.0f) {
                        IdleTip = Tip;
                    }
                    if (Facing == 0.0f && Each.Clip().equals("strike") && Each.Time() == 0.24f) {
                        SwingTip = Tip;
                    }
                }
            }
            Helper.assertTrue(IdleTip != null && SwingTip != null && IdleTip.distanceTo(SwingTip) > 3.0, "the wind up does not move the blade, so the poses prove nothing");
            Helper.succeed();
        });
    }

    private static boolean close(float A, float B) {
        return Math.abs(A - B) < 1.0e-3f;
    }

    private static Vec3 cube(Matrix4f Rendered, float X, float Y, float Z) {
        Vector3f At = Rendered.transformPosition(new Vector3f(-X / 16.0f, Y / 16.0f, Z / 16.0f));
        return new Vec3(At.x, At.y, At.z);
    }

    private static double segment(Vec3 Point, Vec3 From, Vec3 To) {
        Vec3 Along = To.subtract(From);
        double Length = Along.lengthSqr();
        double Share = Length < 1.0e-9 ? 0.0 : Mth.clamp(Point.subtract(From).dot(Along) / Length, 0.0, 1.0);
        return Point.distanceTo(From.add(Along.scale(Share)));
    }

    private static JsonObject json(GameTestHelper Helper, String Path) {
        try (InputStream Stream = Scp001GateFixGameTests.class.getResourceAsStream(Path)) {
            Helper.assertTrue(Stream != null, "missing " + Path);
            return JsonParser.parseReader(new InputStreamReader(Stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException Problem) {
            throw new IllegalStateException(Problem);
        }
    }

    private static float[] triple(JsonElement Value, float Fallback) {
        if (Value == null) {
            return new float[]{Fallback, Fallback, Fallback};
        }
        if (Value.isJsonPrimitive()) {
            float Single = Value.getAsFloat();
            return new float[]{Single, Single, Single};
        }
        JsonArray Items = Value.isJsonArray() ? Value.getAsJsonArray() : Value.getAsJsonObject().has("post") ? Value.getAsJsonObject().getAsJsonArray("post")
                : Value.getAsJsonObject().getAsJsonArray("vector");
        return new float[]{Items.get(0).getAsFloat(), Items.get(1).getAsFloat(), Items.get(2).getAsFloat()};
    }

    private static Map<String, Joint> joints(GameTestHelper Helper) {
        JsonObject Geometry = json(Helper, GeoPath).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        Map<String, Joint> Found = new HashMap<>();
        for (JsonElement Entry : Geometry.getAsJsonArray("bones")) {
            JsonObject Bone = Entry.getAsJsonObject();
            List<float[][]> Cubes = new ArrayList<>();
            if (Bone.has("cubes")) {
                for (JsonElement Item : Bone.getAsJsonArray("cubes")) {
                    JsonObject Cube = Item.getAsJsonObject();
                    Cubes.add(new float[][]{triple(Cube.get("origin"), 0.0f), triple(Cube.get("size"), 0.0f)});
                }
            }
            String Name = Bone.get("name").getAsString();
            Found.put(Name, new Joint(Name, Bone.has("parent") ? Bone.get("parent").getAsString() : null, triple(Bone.get("pivot"), 0.0f), triple(Bone.get("rotation"), 0.0f),
                    Cubes));
        }
        return Found;
    }

    private static float[] sample(JsonObject Bones, String Name, String Channel, float Time, float Fallback) {
        if (Bones == null || !Bones.has(Name) || !Bones.getAsJsonObject(Name).has(Channel)) {
            return new float[]{Fallback, Fallback, Fallback};
        }
        JsonElement Track = Bones.getAsJsonObject(Name).get(Channel);
        if (!Track.isJsonObject() || Track.getAsJsonObject().has("vector") || Track.getAsJsonObject().has("post")) {
            return triple(Track, Fallback);
        }
        float BeforeTime = -Float.MAX_VALUE;
        float AfterTime = Float.MAX_VALUE;
        float[] Before = null;
        float[] After = null;
        for (Map.Entry<String, JsonElement> Key : Track.getAsJsonObject().entrySet()) {
            float When = Float.parseFloat(Key.getKey());
            if (When <= Time && When > BeforeTime) {
                BeforeTime = When;
                Before = triple(Key.getValue(), Fallback);
            }
            if (When >= Time && When < AfterTime) {
                AfterTime = When;
                After = triple(Key.getValue(), Fallback);
            }
        }
        if (Before == null) {
            return After;
        }
        if (After == null || AfterTime <= BeforeTime) {
            return Before;
        }
        float Blend = (Time - BeforeTime) / (AfterTime - BeforeTime);
        return new float[]{Mth.lerp(Blend, Before[0], After[0]), Mth.lerp(Blend, Before[1], After[1]), Mth.lerp(Blend, Before[2], After[2])};
    }

    private static Matrix4f chain(List<Joint> Chain, JsonObject Bones, float Time, boolean Away) {
        Matrix4f Built = new Matrix4f();
        for (int Index = 0; Index < Chain.size(); Index++) {
            Joint Step = Chain.get(Index);
            float[] Turn = sample(Bones, Step.Name(), "rotation", Time, 0.0f);
            float[] Shift = sample(Bones, Step.Name(), "position", Time, 0.0f);
            float[] Size = sample(Bones, Step.Name(), "scale", Time, 1.0f);
            float Px = -Step.Pivot()[0] / 16.0f;
            float Py = Step.Pivot()[1] / 16.0f;
            float Pz = Step.Pivot()[2] / 16.0f;
            Built.translate(-Shift[0] / 16.0f, Shift[1] / 16.0f, Shift[2] / 16.0f);
            Built.translate(Px, Py, Pz);
            Built.rotateZ((Step.Rotation()[2] + Turn[2]) * Mth.DEG_TO_RAD);
            Built.rotateY(-(Step.Rotation()[1] + Turn[1]) * Mth.DEG_TO_RAD);
            Built.rotateX(-(Step.Rotation()[0] + Turn[0]) * Mth.DEG_TO_RAD);
            Built.scale(Size[0], Size[1], Size[2]);
            if (Away || Index < Chain.size() - 1) {
                Built.translate(-Px, -Py, -Pz);
            }
        }
        return Built;
    }
}
