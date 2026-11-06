package net.rai.scp_unleashed.scp4845;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.space.SpaceRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class Scp4845Restaurant {
    public static final int HalfSpan = 18;
    public static final int ProbeStep = 6;
    public static final int SearchAttempts = 40;
    public static final int MaxSlope = 3;
    public static final int LenientSlope = 7;
    public static final int Strict = 0;
    public static final int Lenient = 1;
    public static final int Forced = 2;
    public static final int ClearAbove = 16;
    public static final int VisiblePenalty = 6;
    public static final int AheadPenalty = 3;
    @Nullable
    private static Scp4845Site Cached;
    @Nullable
    private static Scp4845State CachedState;
    private static long CachedVersion = -1;
    @Nullable
    private static ResourceKey<Level> StandIn;

    private Scp4845Restaurant() {
    }

    @Nullable
    public static Scp4845Site current(@Nullable MinecraftServer Server) {
        if (Server == null) {
            return null;
        }
        Scp4845State Data = Scp4845State.get(Server);
        if (Data != CachedState || Data.version() != CachedVersion) {
            CachedState = Data;
            CachedVersion = Data.version();
            Cached = Data.built() ? new Scp4845Site(Data.dimension(), Data.origin(), Data.turn(), Scp4845Layout.get(Server)) : null;
        }
        return Cached;
    }

    @Nullable
    public static Scp4845Site in(Level World) {
        if (World.isClientSide) {
            return null;
        }
        Scp4845Site Found = current(World.getServer());
        return Found != null && Found.in(World) ? Found : null;
    }

    public static void forget() {
        Cached = null;
        CachedState = null;
        CachedVersion = -1;
        Scp4845Layout.forget();
    }

    public static void standIn(@Nullable ResourceKey<Level> Dimension) {
        StandIn = Dimension;
    }

    public static boolean standingIn() {
        return StandIn != null;
    }

    public static ResourceKey<Level> moonKey() {
        return StandIn != null ? StandIn : SpaceRegistry.MoonLevel;
    }

    public static boolean isMoon(Level World) {
        return World.dimension() == moonKey();
    }

    public static boolean spawn(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        if (Scp4845State.get(World.getServer()).built()) {
            return true;
        }
        if (!isMoon(World)) {
            return false;
        }
        place(World, Found.Pos(), facing(Found.Pos(), Found.Investigator().position()));
        return true;
    }

    public static Direction facing(BlockPos Site, Vec3 Toward) {
        return Direction.getNearest(Toward.x - (Site.getX() + 0.5), 0.0, Toward.z - (Site.getZ() + 0.5));
    }

    public static Scp4845Site place(ServerLevel World, BlockPos Centre, Direction Front) {
        MinecraftServer Server = World.getServer();
        Scp4845Layout Layout = Scp4845Layout.get(Server);
        StructureTemplate Template = World.getStructureManager().get(Scp4845Layout.Template)
                .orElseThrow(() -> new IllegalStateException("Missing SCP-4845 restaurant template " + Scp4845Layout.Template));
        Scp4845Site Planned = new Scp4845Site(World.dimension(), Scp4845Site.originFor(Centre, Scp4845Site.rotationFor(Front), Layout), Scp4845Site.rotationFor(Front),
                Layout);
        BoundingBox Box = Template.getBoundingBox(Planned.Settings, Planned.Origin);
        prepare(World, Box, Planned.Origin.getY());
        Template.placeInWorld(World, Planned.Origin, Planned.Origin, Planned.Settings, World.getRandom(), Block.UPDATE_CLIENTS);
        Scp4845State.get(Server).record(World.dimension(), Planned.Origin, Planned.Turn);
        Scp4845Site Placed = current(Server);
        if (Placed != null && Scp4845Staffing.restaff(World, Placed) > 0) {
            Scp4845State.get(Server).markStaffed();
        }
        return Placed != null ? Placed : Planned;
    }

    private static void prepare(ServerLevel World, BoundingBox Box, int Floor) {
        BlockState Empty = Blocks.AIR.defaultBlockState();
        BlockState Fill = SpaceRegistry.LunarStone.get().defaultBlockState();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int X = Box.minX(); X <= Box.maxX(); X++) {
            for (int Z = Box.minZ(); Z <= Box.maxZ(); Z++) {
                for (int Y = Floor + 1; Y <= Box.maxY() + ClearAbove; Y++) {
                    Cursor.set(X, Y, Z);
                    if (World.isOutsideBuildHeight(Cursor)) {
                        break;
                    }
                    if (!World.getBlockState(Cursor).isAir()) {
                        World.setBlock(Cursor, Empty, Block.UPDATE_CLIENTS);
                    }
                }
                for (int Down = 1; Down <= Scp4845Site.FoundationDepth; Down++) {
                    Cursor.set(X, Floor - Down, Z);
                    if (World.isOutsideBuildHeight(Cursor)) {
                        break;
                    }
                    BlockState There = World.getBlockState(Cursor);
                    if (There.isSolidRender(World, Cursor) && !There.is(SpaceRegistry.LunarDust.get())) {
                        break;
                    }
                    World.setBlock(Cursor, Fill, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    public static int ground(Level World, int X, int Z) {
        return World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z) - 1;
    }

    private static int slopeLimit(int Mode) {
        return Mode == Strict ? MaxSlope : Mode == Lenient ? LenientSlope : Integer.MAX_VALUE;
    }

    private static int[] heights(ServerLevel World, int Cx, int Cz) {
        for (int[] Corner : new int[][]{{-HalfSpan, -HalfSpan}, {HalfSpan, -HalfSpan}, {-HalfSpan, HalfSpan}, {HalfSpan, HalfSpan}, {0, 0}}) {
            if (!World.isLoaded(new BlockPos(Cx + Corner[0], World.getSeaLevel(), Cz + Corner[1]))) {
                return null;
            }
        }
        List<Integer> Found = new ArrayList<>();
        for (int Dx = -HalfSpan; Dx <= HalfSpan; Dx += ProbeStep) {
            for (int Dz = -HalfSpan; Dz <= HalfSpan; Dz += ProbeStep) {
                Found.add(ground(World, Cx + Dx, Cz + Dz));
            }
        }
        int[] Made = new int[Found.size()];
        for (int Index = 0; Index < Made.length; Index++) {
            Made[Index] = Found.get(Index);
        }
        Arrays.sort(Made);
        return Made;
    }

    private static boolean crowded(ServerLevel World, BlockPos Site) {
        int Reach = HalfSpan + 8;
        for (int ChunkX = (Site.getX() - Reach) >> 4; ChunkX <= (Site.getX() + Reach) >> 4; ChunkX++) {
            for (int ChunkZ = (Site.getZ() - Reach) >> 4; ChunkZ <= (Site.getZ() + Reach) >> 4; ChunkZ++) {
                LevelChunk Chunk = World.getChunkSource().getChunkNow(ChunkX, ChunkZ);
                if (Chunk == null) {
                    return true;
                }
                for (BlockEntity Entity : Chunk.getBlockEntities().values()) {
                    BlockPos Pos = Entity.getBlockPos();
                    if (Math.abs(Pos.getX() - Site.getX()) <= Reach && Math.abs(Pos.getZ() - Site.getZ()) <= Reach && Pos.getY() >= Site.getY() - 4
                            && Pos.getY() <= Site.getY() + 16) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static Optional<BlockPos> findSite(ServerLevel World, ServerPlayer Reader, Vec3 Origin, int MinDistance, int MaxDistance, int Mode, RandomSource Random) {
        int Low = Math.min(MinDistance, MaxDistance);
        int High = Math.max(MinDistance, MaxDistance);
        Vec3 Eye = Reader.getEyePosition();
        Vec3 Look = Reader.getLookAngle();
        BlockPos Best = null;
        int BestScore = Integer.MAX_VALUE;
        for (int Attempt = 0; Attempt < SearchAttempts; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Radius = Low + Random.nextDouble() * (High - Low);
            int Cx = Mth.floor(Origin.x + Math.cos(Angle) * Radius);
            int Cz = Mth.floor(Origin.z + Math.sin(Angle) * Radius);
            int[] Heights = heights(World, Cx, Cz);
            if (Heights == null) {
                continue;
            }
            int Median = Heights[Heights.length / 2];
            int Spread = Heights[Heights.length - 1] - Heights[0];
            if (Median <= World.getMinBuildHeight() + Scp4845Site.FoundationDepth || Median + ClearAbove >= World.getMaxBuildHeight() || Spread > slopeLimit(Mode)) {
                continue;
            }
            BlockPos Site = new BlockPos(Cx, Median, Cz);
            if (Mode != Forced && crowded(World, Site)) {
                continue;
            }
            int Score = Spread * 2;
            Vec3 Top = Vec3.atCenterOf(Site.above(6));
            if (World.clip(new ClipContext(Eye, Top, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Reader)).getType() == HitResult.Type.MISS) {
                Score += VisiblePenalty;
            }
            if (Top.subtract(Eye).normalize().dot(Look) > 0.5) {
                Score += AheadPenalty;
            }
            if (Score < BestScore) {
                BestScore = Score;
                Best = Site;
            }
        }
        return Optional.ofNullable(Best);
    }
}
