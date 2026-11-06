package net.rai.scp_unleashed.scp354.gametest;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp354.Scp354Basin;
import net.rai.scp_unleashed.scp354.Scp354Emergence;
import net.rai.scp_unleashed.scp354.Scp354Emergent;
import net.rai.scp_unleashed.scp354.Scp354Fluid;
import net.rai.scp_unleashed.scp354.Scp354Grab;
import net.rai.scp_unleashed.scp354.Scp354Pools;
import net.rai.scp_unleashed.scp354.Scp354Registry;
import net.rai.scp_unleashed.scp354.Scp354Swim;
import net.rai.scp_unleashed.scp354.Scp354Swimmer;
import net.rai.scp_unleashed.scp354.Scp354Tentacle;
import net.rai.scp_unleashed.scp354.beasts.Scp354BeastRegistry;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.IntBinaryOperator;
import java.util.function.Supplier;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp354SwimGameTests {
    public static final String Arena = Scp_unleashed.ModId + ":" + Scp354GameTests.Arena;
    public static final int Size = Scp354GameTests.Size;
    public static final int Middle = Scp354GameTests.Middle;
    public static final int EscapeLimit = 200;
    public static final int ChaseTicks = 200;
    public static final int ThrownLimit = 200;
    public static final double BaitReach = 6.0;
    public static final double BaitHealth = 400.0;
    public static final double SurfaceSlack = 0.35;
    public static final double Progress = 2.0;
    public static final double Roam = 4.0;
    public static final int CircleTicks = 300;
    public static final double CircleRadius = 5.0;
    public static final double CircleRate = 0.06;
    public static final float HeadSlack = 76.0f;
    public static final float BodyStep = 35.0f;
    public static final float CircleTurn = 540.0f;
    private static final Logger Log = LogUtils.getLogger();

    private record Kind(String Name, Supplier<? extends EntityType<?>> Type, boolean Walker) {
    }

    private record Ground(String Name, IntBinaryOperator Height, long Seed) {
    }

    private Scp354SwimGameTests() {
    }

    private static List<Kind> kinds() {
        return List.of(new Kind("lion", Scp354Registry.Lion, true), new Kind("lizard", Scp354Registry.Lizard, true), new Kind("brute", Scp354BeastRegistry.Brute, true),
                new Kind("crawler", Scp354BeastRegistry.Crawler, true), new Kind("leaper", Scp354BeastRegistry.Leaper, true),
                new Kind("charger", Scp354BeastRegistry.Charger, true), new Kind("wingbeast", Scp354BeastRegistry.Wingbeast, false));
    }

    private static List<Ground> grounds() {
        return List.of(new Ground("flat", (X, Z) -> 6, 354L), new Ground("slope", (X, Z) -> 2 + (int) Math.floor(X * 0.45 + Z * 0.12), 3541L));
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> Tests = new ArrayList<>();
        for (Kind Breed : kinds()) {
            for (Ground Land : grounds()) {
                String Name = "scp354swim_" + Breed.Name() + "_escape_" + Land.Name();
                Tests.add(new TestFunction("scp354_swim_escape", Name, Arena, EscapeLimit + ChaseTicks + 120, 0L, true, Helper -> escape(Helper, Breed, Land, Name)));
            }
            if (Breed.Walker()) {
                String Name = "scp354swim_" + Breed.Name() + "_thrown";
                Tests.add(new TestFunction("scp354_swim_thrown", Name, Arena, ThrownLimit + 60, 0L, true, Helper -> thrown(Helper, Breed)));
            }
        }
        Tests.add(new TestFunction("scp354_swim_thrown", "scp354swim_tentacle_anchored", Arena, 120, 0L, true, Scp354SwimGameTests::anchored));
        for (Kind Breed : kinds()) {
            if (Breed.Walker()) {
                String Name = "scp354swim_" + Breed.Name() + "_circles";
                Tests.add(new TestFunction("scp354_swim_circles", Name, Arena, CircleTicks + 40, 0L, true, Helper -> circles(Helper, Breed)));
            }
        }
        return Tests;
    }

    private static void terrain(GameTestHelper Helper, IntBinaryOperator Height) {
        ServerLevel World = Helper.getLevel();
        for (int X = 0; X < Size; X++) {
            for (int Z = 0; Z < Size; Z++) {
                int Top = Height.applyAsInt(X, Z);
                for (int Y = 1; Y <= Top; Y++) {
                    BlockState State = Y == Top ? Blocks.GRASS_BLOCK.defaultBlockState() : Y > Top - 3 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState();
                    World.setBlock(Helper.absolutePos(new BlockPos(X, Y, Z)), State, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static Scp354Pools.Pool watched(GameTestHelper Helper, Scp354Basin.Result Built) {
        ServerLevel World = Helper.getLevel();
        return Scp354Pools.get(World.getServer()).add(new Scp354Pools.Pool(World.dimension(), Built.Shape().centre(), Built.Shape().radius(), Built.Shape().deepest(),
                Scp354GameTests.Quiet, World.getGameTime(), 0));
    }

    private static Scp354Basin.Result build(GameTestHelper Helper, Ground Land) {
        terrain(Helper, Land.Height());
        int Surface = Land.Height().applyAsInt(Middle, Middle);
        return Scp354Basin.build(Helper.getLevel(), Helper.absolutePos(new BlockPos(Middle, Surface, Middle)), RandomSource.create(Land.Seed()));
    }

    private static boolean standing(Entity Creature) {
        return Creature.onGround() && !Scp354Swim.swimming(Creature);
    }

    private static Vec3 relative(GameTestHelper Helper, Vec3 Absolute) {
        return Absolute.subtract(Vec3.atLowerCornerOf(Helper.absolutePos(BlockPos.ZERO)));
    }

    private static Vec3 groundAt(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, 0.0, Z));
        for (int Y = 21; Y >= 1; Y--) {
            Cursor.set(Spot.x, Helper.absolutePos(BlockPos.ZERO).getY() + Y, Spot.z);
            BlockState There = World.getBlockState(Cursor);
            if (!There.isAir() && !Scp354Fluid.isRed(There.getFluidState())) {
                return new Vec3(X, Y + 1.0, Z);
            }
        }
        return new Vec3(X, 7.0, Z);
    }

    private static String where(GameTestHelper Helper, Entity Creature) {
        Vec3 Here = relative(Helper, Creature.position());
        String Text = String.format(" at (%.2f %.2f %.2f) ground %s wet %s", Here.x, Here.y, Here.z, Creature.onGround(), Scp354Swim.swimming(Creature));
        if (Creature instanceof Scp354Swimmer Swimmer && Swimmer.course().Exit != null) {
            Vec3 Exit = relative(Helper, Swimmer.course().Exit);
            Text += String.format(" exit (%.2f %.2f %.2f)", Exit.x, Exit.y, Exit.z);
        }
        return Text;
    }

    private static void escape(GameTestHelper Helper, Kind Breed, Ground Land, String Name) {
        ServerLevel World = Helper.getLevel();
        Scp354Basin.Result Built = build(Helper, Land);
        Scp354Pools.Pool Pond = watched(Helper, Built);
        Optional<Entity> Risen = Scp354Emergence.emerge(World, Pond, Breed.Type().get());
        Helper.assertTrue(Risen.isPresent() && Risen.get() instanceof Mob, "nothing emerged");
        Mob Creature = (Mob) Risen.get();
        double Surface = Built.Shape().Surface() + Scp354Fluid.SurfaceHeight;
        Vec3 Centre = Vec3.atBottomCenterOf(Built.Shape().centre());
        List<double[]> Track = new ArrayList<>();
        int[] Ticks = {0, -1, -1};
        boolean[] Checked = {false};
        Villager[] Bait = {null};
        double[] Closest = {Double.MAX_VALUE, 0.0, 0.0};
        Helper.onEachTick(() -> {
            int Now = Ticks[0]++;
            if (!Creature.isAlive()) {
                Helper.fail(Breed.Name() + " died in the pool" + where(Helper, Creature));
            }
            Vec3 Here = relative(Helper, Creature.position());
            Track.add(new double[]{Now, Here.x, Here.y, Here.z, Scp354Swim.swimming(Creature) ? 1.0 : 0.0});
            boolean Emerging = Creature instanceof Scp354Emergent Emergent && Emergent.isEmerging();
            if (!Checked[0] && !Emerging) {
                Checked[0] = true;
                if (Breed.Walker() && Creature instanceof Scp354Swimmer Swimmer && Scp354Swim.swimming(Creature)) {
                    double Floor = Surface - Swimmer.draft() - SurfaceSlack;
                    Helper.assertTrue(Creature.getY() >= Floor, Breed.Name() + " finished emerging " + String.format("%.2f", Surface - Creature.getY())
                            + " below the surface" + where(Helper, Creature));
                }
            }
            if (Ticks[1] < 0) {
                boolean Out = Breed.Walker() ? standing(Creature) : !Scp354Swim.swimming(Creature) && Creature.getY() > Surface + 0.5;
                if (Out) {
                    Ticks[1] = Now;
                    Log.info("SCP-354 swim: {} on {} left the pool after {} ticks{}", Breed.Name(), Land.Name(), Now, where(Helper, Creature));
                    Vec3 Away = new Vec3(Centre.x - Creature.getX(), 0.0, Centre.z - Creature.getZ());
                    if (Away.lengthSqr() < 1.0e-4) {
                        Away = new Vec3(0.0, 0.0, 1.0);
                    }
                    Vec3 Far = relative(Helper, Centre.add(Away.normalize().scale(Built.Shape().radius() + BaitReach)));
                    Vec3 Spot = groundAt(Helper, Math.floor(Far.x) + 0.5, Math.floor(Far.z) + 0.5);
                    Villager Made = Helper.spawn(EntityType.VILLAGER, Spot);
                    Made.setNoAi(true);
                    Made.getAttribute(Attributes.MAX_HEALTH).setBaseValue(BaitHealth);
                    Made.setHealth((float) BaitHealth);
                    Bait[0] = Made;
                    Creature.setTarget(Made);
                    Closest[1] = Creature.distanceTo(Made);
                    Closest[0] = Closest[1];
                } else {
                    Helper.assertTrue(Now <= EscapeLimit, Breed.Name() + " was still in the pool after " + EscapeLimit + " ticks" + where(Helper, Creature));
                }
                return;
            }
            if (Scp354Swim.swimming(Creature)) {
                dump(Helper, Name, Built, Track, Ticks[1]);
                Helper.fail(Breed.Name() + " went back into the pool " + (Now - Ticks[1]) + " ticks after leaving it" + where(Helper, Creature));
            }
            Villager Made = Bait[0];
            if (Track.size() > 1) {
                double[] Last = Track.get(Track.size() - 2);
                Closest[2] += Math.sqrt((Here.x - Last[1]) * (Here.x - Last[1]) + (Here.z - Last[3]) * (Here.z - Last[3]));
            }
            if (Made != null && Made.isAlive()) {
                Closest[0] = Math.min(Closest[0], Creature.distanceTo(Made));
                if (Creature.getTarget() == null) {
                    Creature.setTarget(Made);
                }
            }
            if (Now - Ticks[1] >= ChaseTicks) {
                boolean Hurt = Made != null && (!Made.isAlive() || Made.getHealth() < Made.getMaxHealth());
                Helper.assertTrue(Hurt || Closest[0] <= Closest[1] - Progress || Closest[2] >= Roam, Breed.Name() + " did not chase around the pool, closest "
                        + String.format("%.1f of %.1f after %.1f blocks", Closest[0], Closest[1], Closest[2]) + where(Helper, Creature));
                Log.info("SCP-354 swim: {} on {} chased {} blocks without going back in, closest {} of {}", Breed.Name(), Land.Name(), String.format("%.1f", Closest[2]),
                        String.format("%.1f", Closest[0]), String.format("%.1f", Closest[1]));
                dump(Helper, Name, Built, Track, Ticks[1]);
                Creature.discard();
                if (Made != null) {
                    Made.discard();
                }
                Scp354Pools.get(World.getServer()).remove(Pond);
                Helper.succeed();
            }
        });
    }

    private static void thrown(GameTestHelper Helper, Kind Breed) {
        ServerLevel World = Helper.getLevel();
        Scp354Basin.Result Built = build(Helper, grounds().get(0));
        Scp354Pools.Pool Pond = watched(Helper, Built);
        Entity Made = Helper.spawn(Breed.Type().get(), new Vec3(Middle + 0.5, 8.5, Middle + 0.5));
        Helper.assertTrue(Made instanceof Mob, "could not spawn the creature");
        Mob Creature = (Mob) Made;
        Creature.setDeltaMovement(0.15, -0.3, 0.1);
        int[] Ticks = {0, -1};
        Helper.onEachTick(() -> {
            int Now = Ticks[0]++;
            Helper.assertFalse(Scp354Grab.isSeized(Creature), Breed.Name() + " was seized by the pool" + where(Helper, Creature));
            if (Ticks[1] < 0) {
                if (Scp354Swim.swimming(Creature)) {
                    Ticks[1] = Now;
                }
                Helper.assertTrue(Now <= 30, Breed.Name() + " never landed in the pool" + where(Helper, Creature));
                return;
            }
            if (standing(Creature)) {
                Log.info("SCP-354 swim: thrown {} climbed out after {} ticks{}", Breed.Name(), Now - Ticks[1], where(Helper, Creature));
                Creature.discard();
                Scp354Pools.get(World.getServer()).remove(Pond);
                Helper.succeed();
                return;
            }
            Helper.assertTrue(Now - Ticks[1] <= ThrownLimit, Breed.Name() + " was still in the pool " + ThrownLimit + " ticks after falling in" + where(Helper, Creature));
        });
    }

    private static void circles(GameTestHelper Helper, Kind Breed) {
        ServerLevel World = Helper.getLevel();
        BlockState Red = Scp354Registry.RedLiquid.get().defaultBlockState();
        for (int X = 0; X < Size; X++) {
            for (int Z = 0; Z < Size; Z++) {
                boolean Pond = X >= 6 && X < 34 && Z >= 6 && Z < 34;
                for (int Y = 1; Y <= 14; Y++) {
                    BlockState State = Pond ? (Y == 1 ? Blocks.STONE.defaultBlockState() : Y <= 5 ? Red : Blocks.AIR.defaultBlockState())
                            : Blocks.STONE.defaultBlockState();
                    World.setBlock(Helper.absolutePos(new BlockPos(X, Y, Z)), State, Block.UPDATE_CLIENTS);
                }
            }
        }
        Entity Made = Helper.spawn(Breed.Type().get(), new Vec3(Middle + 0.5, 3.5, Middle + 0.5));
        Helper.assertTrue(Made instanceof Mob, "could not spawn the creature");
        Mob Creature = (Mob) Made;
        Villager Bait = Helper.spawn(EntityType.VILLAGER, new Vec3(Middle + 0.5 + CircleRadius, 5.5, Middle + 0.5));
        Bait.setNoAi(true);
        Bait.getAttribute(Attributes.MAX_HEALTH).setBaseValue(BaitHealth);
        Bait.setHealth((float) BaitHealth);
        float[] Track = {Float.NaN, 0.0f};
        int[] Ticks = {0};
        Helper.onEachTick(() -> {
            int Now = Ticks[0]++;
            double Angle = Now * CircleRate;
            Vec3 Spot = Helper.absoluteVec(new Vec3(Middle + 0.5 + Math.cos(Angle) * CircleRadius, 5.5, Middle + 0.5 + Math.sin(Angle) * CircleRadius));
            Bait.teleportTo(Spot.x, Spot.y, Spot.z);
            Bait.setHealth((float) BaitHealth);
            Creature.setTarget(Bait);
            if (Now < 20) {
                return;
            }
            float Yaw = Creature.getYRot();
            float Body = Creature.yBodyRot;
            Helper.assertTrue(Yaw >= -180.0f && Yaw <= 180.0f, Breed.Name() + " yaw ran past the wrap to " + Yaw + " after " + Now + " ticks");
            float Head = Mth.wrapDegrees(Creature.getYHeadRot() - Body);
            Helper.assertTrue(Math.abs(Head) <= HeadSlack, Breed.Name() + " head turned " + Head + " degrees from its body");
            if (!Float.isNaN(Track[0])) {
                float Step = Mth.wrapDegrees(Body - Track[0]);
                Helper.assertTrue(Math.abs(Step) <= BodyStep, Breed.Name() + " body snapped " + Step + " degrees in one tick");
                Track[1] += Math.abs(Step);
            }
            Track[0] = Body;
            if (Now >= CircleTicks) {
                Helper.assertTrue(Scp354Swim.swimming(Creature), Breed.Name() + " left the walled pond" + where(Helper, Creature));
                Helper.assertTrue(Track[1] >= CircleTurn, Breed.Name() + " only turned " + Track[1] + " degrees while circling");
                Log.info("SCP-354 swim: {} circled {} degrees, yaw {}, head offset {}", Breed.Name(), String.format("%.0f", Track[1]), String.format("%.1f", Yaw),
                        String.format("%.1f", Head));
                Creature.discard();
                Bait.discard();
                Helper.succeed();
            }
        });
    }

    private static void anchored(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp354Basin.Result Built = build(Helper, grounds().get(0));
        Scp354Pools.Pool Pond = watched(Helper, Built);
        Optional<Entity> Risen = Scp354Emergence.emerge(World, Pond, Scp354Registry.Tentacle.get());
        Helper.assertTrue(Risen.isPresent() && Risen.get() instanceof Scp354Tentacle, "no tentacle emerged");
        Scp354Tentacle Tentacle = (Scp354Tentacle) Risen.get();
        Helper.runAfterDelay(Scp354Tentacle.EmergeTicks + 20, () -> {
            double Anchor = Built.Shape().Surface() + Scp354Fluid.SurfaceHeight - Scp354Tentacle.AnchorDepth;
            Helper.assertTrue(Math.abs(Tentacle.getY() - Anchor) < 0.2, "the tentacle is not rooted at the surface" + where(Helper, Tentacle));
            Tentacle.discard();
            Scp354Pools.get(World.getServer()).remove(Pond);
            Helper.succeed();
        });
    }

    private static void dump(GameTestHelper Helper, String Name, Scp354Basin.Result Built, List<double[]> Track, int Left) {
        String Folder = System.getenv(Scp354GameTests.DumpVariable);
        if (Folder == null || Folder.isEmpty()) {
            return;
        }
        ServerLevel World = Helper.getLevel();
        BlockPos Origin = Helper.absolutePos(BlockPos.ZERO);
        StringBuilder Text = new StringBuilder("{\"name\": \"").append(Name).append("\", \"left\": ").append(Left).append(", \"surface\": ")
                .append(Built.Shape().Surface() - Origin.getY()).append(", \"liquid\": [");
        boolean First = true;
        for (BlockPos Cell : Built.Liquid()) {
            if (Scp354Fluid.isRed(World.getFluidState(Cell.above()))) {
                continue;
            }
            Text.append(First ? "" : ", ").append("[").append(Cell.getX() - Origin.getX()).append(", ").append(Cell.getZ() - Origin.getZ()).append("]");
            First = false;
        }
        Text.append("], \"heights\": [");
        First = true;
        for (int X = 0; X < Size; X++) {
            for (int Z = 0; Z < Size; Z++) {
                Vec3 Top = groundAt(Helper, X + 0.5, Z + 0.5);
                Text.append(First ? "" : ", ").append((int) Top.y);
                First = false;
            }
        }
        Text.append("], \"track\": [");
        First = true;
        for (double[] Point : Track) {
            Text.append(First ? "" : ", ").append(String.format("[%d, %.3f, %.3f, %.3f, %d]", (int) Point[0], Point[1], Point[2], Point[3], (int) Point[4]));
            First = false;
        }
        Text.append("]}\n");
        try {
            Path Target = Path.of(Folder, Name + ".json");
            Files.createDirectories(Target.getParent());
            Files.writeString(Target, Text.toString(), StandardCharsets.UTF_8);
        } catch (IOException Problem) {
            throw new IllegalStateException(Problem);
        }
    }
}
