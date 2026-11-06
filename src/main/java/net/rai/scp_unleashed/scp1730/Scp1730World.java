package net.rai.scp_unleashed.scp1730;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.block.DecoratableBlock;
import net.rai.scp_unleashed.block.entity.RenderableBlockEntity;
import net.rai.scp_unleashed.decal.DecalKind;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Fixture;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Plan;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Spot;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp1730World {
    public static final ResourceKey<Level> Key = ResourceKey.create(Registries.DIMENSION, Scp1730Registry.location("scp1730_facility"));
    public static final int RunStride = 2048;
    public static final int StandinBaseX = -3584000;
    public static final int StandinBaseZ = -3584000;
    public static final int PaintFlags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<String, BlockState> Palette = new ConcurrentHashMap<>();
    private static final Set<String> Unknown = ConcurrentHashMap.newKeySet();
    private static volatile ResourceKey<Level> Standin;
    private static volatile long Salt = 1730L;
    private static final Map<Integer, Long> Chosen = new ConcurrentHashMap<>();

    private Scp1730World() {
    }

    public static void standIn(@Nullable ResourceKey<Level> Dimension) {
        Standin = Dimension;
    }

    public static boolean standingIn() {
        return Standin != null;
    }

    public static ResourceKey<Level> key() {
        ResourceKey<Level> Found = Standin;
        return Found != null ? Found : Key;
    }

    public static boolean inside(@Nullable Level World) {
        return World != null && World.dimension() == key();
    }

    public static boolean inside(Entity Target) {
        return inside(Target.level());
    }

    public static boolean strict(@Nullable Level World) {
        return inside(World) && !standingIn();
    }

    @Nullable
    public static ServerLevel level(MinecraftServer Server) {
        return Server.getLevel(key());
    }

    public static void salt(long WorldSeed) {
        Salt = WorldSeed;
        Chosen.clear();
    }

    public static long seedOf(int Run) {
        long Mixed = Salt * 0x9E3779B97F4A7C15L + (Run + 1L) * 0xC2B2AE3D27D4EB4FL + 1730L;
        Mixed ^= Mixed >>> 31;
        Mixed *= 0xBF58476D1CE4E5B9L;
        return Mixed ^ Mixed >>> 29;
    }

    public static int baseX() {
        return Standin != null ? StandinBaseX : 0;
    }

    public static int baseZ() {
        return Standin != null ? StandinBaseZ : 0;
    }

    public static int originX(int Run) {
        return baseX() + Run * RunStride;
    }

    public static int originZ() {
        return baseZ();
    }

    public static int runAt(int X) {
        return Math.floorDiv(X - baseX(), RunStride);
    }

    public static Scp1730Plan plan(int Run) {
        Long Known = Chosen.get(Run);
        if (Known != null) {
            return Scp1730Plan.of(Known);
        }
        long Seed = seedOf(Run);
        for (int Attempt = 0; Attempt < 64; Attempt++) {
            long Tried = Attempt == 0 ? Seed : Seed ^ (0x5DEECE66DL * Attempt + 0x2545F4914F6CDD1DL);
            try {
                Scp1730Plan Built = Scp1730Plan.of(Tried);
                Chosen.put(Run, Tried);
                return Built;
            } catch (RuntimeException Problem) {
                if (Attempt == 0) {
                    Log.warn("SCP-1730 plan seed {} failed to build ({}), trying derived seeds", Seed, Problem.getMessage());
                }
            }
        }
        throw new IllegalStateException("no SCP-1730 plan could be built for run " + Run);
    }

    public static boolean inPlan(int Run, int X, int Z) {
        int Lx = X - originX(Run);
        int Lz = Z - originZ();
        return Run >= 0 && Lx >= 0 && Lx < Scp1730Plan.Width && Lz >= 0 && Lz < Scp1730Plan.Depth;
    }

    public static boolean inPlanRange(int Run, net.minecraft.world.level.ChunkPos Pos) {
        int Ox = originX(Run);
        int Oz = originZ();
        return Pos.getMaxBlockX() >= Ox && Pos.getMinBlockX() < Ox + Scp1730Plan.Width && Pos.getMaxBlockZ() >= Oz && Pos.getMinBlockZ() < Oz + Scp1730Plan.Depth;
    }

    public static Vec3 world(int Run, Scp1730Spot Spot) {
        return new Vec3(originX(Run) + Spot.X(), Spot.Y(), originZ() + Spot.Z());
    }

    public static BlockPos world(int Run, Scp1730Fixture Fixture) {
        return new BlockPos(originX(Run) + Fixture.X(), Fixture.Y(), originZ() + Fixture.Z());
    }

    public static BlockPos world(int Run, int X, int Y, int Z) {
        return new BlockPos(originX(Run) + X, Y, originZ() + Z);
    }

    public static int localX(int Run, double X) {
        return (int) Math.floor(X) - originX(Run);
    }

    public static int localZ(double Z) {
        return (int) Math.floor(Z) - originZ();
    }

    public static int zoneOf(int Run, BlockPos Pos) {
        if (!inPlan(Run, Pos.getX(), Pos.getZ())) {
            return -1;
        }
        return plan(Run).zoneAt(Pos.getX() - originX(Run), Pos.getY(), Pos.getZ() - originZ());
    }

    @Nullable
    public static String planState(int Run, BlockPos Pos) {
        if (!inPlan(Run, Pos.getX(), Pos.getZ())) {
            return null;
        }
        return plan(Run).stateAt(Pos.getX() - originX(Run), Pos.getY(), Pos.getZ() - originZ());
    }

    public static boolean original(int Run, BlockPos Pos, BlockState Current) {
        String Planned = planState(Run, Pos);
        if (Planned == null) {
            return false;
        }
        return state(Planned).getBlock() == Current.getBlock();
    }

    public static BlockState state(String Text) {
        BlockState Found = Palette.get(Text);
        if (Found != null) {
            return Found;
        }
        try {
            Found = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), Text, false).blockState();
        } catch (Exception Problem) {
            if (Unknown.add(Text)) {
                Log.warn("SCP-1730 plan names an unknown block state {}", Text);
            }
            Found = Blocks.AIR.defaultBlockState();
        }
        Palette.put(Text, Found);
        return Found;
    }

    public static void paint(ServerLevel World, int Run, int MinX, int MinZ, int MaxX, int MaxZ) {
        Scp1730Plan Plan = plan(Run);
        int Ox = originX(Run);
        int Oz = originZ();
        int Lx0 = Math.max(0, MinX - Ox);
        int Lz0 = Math.max(0, MinZ - Oz);
        int Lx1 = Math.min(Scp1730Plan.Width - 1, MaxX - Ox);
        int Lz1 = Math.min(Scp1730Plan.Depth - 1, MaxZ - Oz);
        if (Lx0 > Lx1 || Lz0 > Lz1) {
            return;
        }
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        Plan.paint(Lx0, Lz0, Lx1, Lz1, (X, Y, Z, Text) -> {
            if (World.isOutsideBuildHeight(Y)) {
                return;
            }
            World.setBlock(Cursor.set(Ox + X, Y, Oz + Z), state(Text), PaintFlags);
        });
        decorate(World, Run, Lx0 + Ox, Lz0 + Oz, Lx1 + Ox, Lz1 + Oz, World.getRandom());
    }

    public static void paintZone(ServerLevel World, int Run, int Zone) {
        var Box = plan(Run).zone(Zone);
        paint(World, Run, originX(Run) + Box.MinX() - 2, originZ() + Box.MinZ() - 2, originX(Run) + Box.MaxX() + 2, originZ() + Box.MaxZ() + 2);
    }

    public static void decorate(LevelAccessor World, int Run, int MinX, int MinZ, int MaxX, int MaxZ, RandomSource Random) {
        Scp1730Plan Plan = plan(Run);
        for (Scp1730Fixture Fixture : Plan.fixtures()) {
            BlockPos Pos = world(Run, Fixture);
            if (Pos.getX() < MinX || Pos.getX() > MaxX || Pos.getZ() < MinZ || Pos.getZ() > MaxZ) {
                continue;
            }
            switch (Fixture.Type()) {
                case "decal" -> decal(World, Pos, Fixture, Random);
                case "loot" -> loot(World, Pos, Fixture.Data(), Random);
                case "cutoff" -> Scp1730CutoffDoors.settle(World, Pos, Fixture);
                default -> {
                }
            }
        }
    }

    private static void decal(LevelAccessor World, BlockPos Pos, Scp1730Fixture Fixture, RandomSource Random) {
        BlockState Host = World.getBlockState(Pos);
        if (!(Host.getBlock() instanceof DecoratableBlock)) {
            return;
        }
        DecalKind Kind = kindNamed(Scp1730Plan.decalKind(Fixture.Data()));
        Direction Face = Direction.byName(Fixture.Facing());
        if (Kind == null || Face == null) {
            return;
        }
        if (!Host.getValue(DecoratableBlock.Decorated)) {
            World.setBlock(Pos, Host.setValue(DecoratableBlock.Decorated, true), PaintFlags);
        }
        if (World.getBlockEntity(Pos) instanceof RenderableBlockEntity Entity) {
            int Variant = Math.min(Kind.getVariants(), Scp1730Plan.decalVariant(Fixture.Data()));
            Direction Facing = Face.getAxis().isHorizontal() ? Face : Direction.NORTH;
            Entity.addLayer(Face, Kind.layer(Variant, Face, Facing, Random));
        }
    }

    private static void loot(LevelAccessor World, BlockPos Pos, int Tier, RandomSource Random) {
        BlockEntity Found = World.getBlockEntity(Pos);
        if (Found instanceof RandomizableContainer) {
            RandomizableContainer.setBlockEntityLootTable(World, Random, Pos, lootTable(Tier));
        }
    }

    public static ResourceKey<LootTable> lootTable(int Tier) {
        return ResourceKey.create(Registries.LOOT_TABLE, Scp1730Registry.location("chests/scp1730_tier_" + Math.max(0, Math.min(3, Tier))));
    }

    @Nullable
    private static DecalKind kindNamed(String Name) {
        for (DecalKind Kind : DecalKind.values()) {
            if (Kind.getName().equals(Name)) {
                return Kind;
            }
        }
        return null;
    }

    public static ResourceLocation id(String Path) {
        return Scp1730Registry.location(Path);
    }
}
