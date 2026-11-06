package net.rai.scp_unleashed.scp354;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class Scp354Sites {
    public static final int SearchAttempts = 40;
    public static final int Strict = 0;
    public static final int Lenient = 1;
    public static final int Forced = 2;
    public static final int StrictSlope = 2;
    public static final int LenientSlope = 6;
    public static final int GrowthDepth = 40;
    public static final int Footprint = 5;
    public static final int ProbeStep = 2;
    public static final int VisiblePenalty = 6;
    public static final int AheadPenalty = 3;

    private Scp354Sites() {
    }

    public static Optional<BlockPos> findSite(ServerLevel World, @Nullable ServerPlayer Reader, Vec3 Origin, int MinDistance, int MaxDistance, int Mode, RandomSource Random) {
        int Low = Math.min(MinDistance, MaxDistance);
        int High = Math.max(MinDistance, MaxDistance);
        BlockPos Best = null;
        int BestScore = Integer.MAX_VALUE;
        for (int Attempt = 0; Attempt < SearchAttempts; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Radius = Low + Random.nextDouble() * (High - Low);
            int Cx = Mth.floor(Origin.x + Math.cos(Angle) * Radius);
            int Cz = Mth.floor(Origin.z + Math.sin(Angle) * Radius);
            Optional<int[]> Survey = survey(World, Cx, Cz, Mode);
            if (Survey.isEmpty()) {
                continue;
            }
            int Ground = Survey.get()[0];
            int Slope = Survey.get()[1];
            BlockPos Site = new BlockPos(Cx, Ground, Cz);
            if (crowded(World, Site)) {
                continue;
            }
            int Score = Slope * 2;
            if (Reader != null) {
                Vec3 Eye = Reader.getEyePosition();
                Vec3 Top = Vec3.atCenterOf(Site.above(2));
                if (World.clip(new ClipContext(Eye, Top, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Reader)).getType() == HitResult.Type.MISS) {
                    Score += VisiblePenalty;
                }
                if (Top.subtract(Eye).normalize().dot(Reader.getLookAngle()) > 0.5) {
                    Score += AheadPenalty;
                }
            }
            if (Score < BestScore) {
                BestScore = Score;
                Best = Site;
            }
        }
        return Optional.ofNullable(Best);
    }

    public static Optional<int[]> survey(ServerLevel World, int Cx, int Cz, int Mode) {
        int Reach = Footprint + Scp354Basin.GravityRing;
        for (int[] Corner : new int[][]{{-Reach, -Reach}, {Reach, -Reach}, {-Reach, Reach}, {Reach, Reach}, {0, 0}}) {
            if (!World.isLoaded(new BlockPos(Cx + Corner[0], World.getSeaLevel(), Cz + Corner[1]))) {
                return Optional.empty();
            }
        }
        List<Integer> Heights = new ArrayList<>();
        int Wet = 0;
        for (int Dx = -Footprint; Dx <= Footprint; Dx += ProbeStep) {
            for (int Dz = -Footprint; Dz <= Footprint; Dz += ProbeStep) {
                int X = Cx + Dx;
                int Z = Cz + Dz;
                int Top = ground(World, X, Z);
                BlockPos Surface = new BlockPos(X, Top, Z);
                if (!World.getFluidState(Surface).isEmpty() || !World.getFluidState(Surface.above()).isEmpty()) {
                    Wet++;
                }
                Heights.add(Top);
            }
        }
        if (Wet > wetLimit(Mode, Heights.size())) {
            return Optional.empty();
        }
        Integer[] Sorted = Heights.toArray(new Integer[0]);
        Arrays.sort(Sorted);
        int Median = Sorted[Sorted.length / 2];
        int Slope = Sorted[Sorted.length - 1] - Sorted[0];
        if (Slope > slopeLimit(Mode)) {
            return Optional.empty();
        }
        if (Median <= World.getMinBuildHeight() + Scp354Basin.MaxDepth + 2 || Median + Scp354Basin.CanopyClear >= World.getMaxBuildHeight()) {
            return Optional.empty();
        }
        return Optional.of(new int[]{Median, Slope});
    }

    public static int ground(Level World, int X, int Z) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(X, World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z) - 1, Z);
        int Floor = Math.max(World.getMinBuildHeight(), Cursor.getY() - GrowthDepth);
        while (Cursor.getY() > Floor && Scp354Basin.isGrowth(World.getBlockState(Cursor))) {
            Cursor.move(Direction.DOWN);
        }
        return Cursor.getY();
    }

    private static int slopeLimit(int Mode) {
        return Mode == Strict ? StrictSlope : Mode == Lenient ? LenientSlope : Integer.MAX_VALUE;
    }

    private static int wetLimit(int Mode, int Probes) {
        return Mode == Strict ? 0 : Mode == Lenient ? Probes / 3 : Probes * 2 / 3;
    }

    private static boolean crowded(ServerLevel World, BlockPos Site) {
        int Reach = Footprint + 1;
        int MinX = Site.getX() - Reach;
        int MaxX = Site.getX() + Reach;
        int MinZ = Site.getZ() - Reach;
        int MaxZ = Site.getZ() + Reach;
        for (int ChunkX = MinX >> 4; ChunkX <= MaxX >> 4; ChunkX++) {
            for (int ChunkZ = MinZ >> 4; ChunkZ <= MaxZ >> 4; ChunkZ++) {
                LevelChunk Chunk = World.getChunkSource().getChunkNow(ChunkX, ChunkZ);
                if (Chunk == null) {
                    return true;
                }
                for (BlockEntity Fixture : Chunk.getBlockEntities().values()) {
                    BlockPos Pos = Fixture.getBlockPos();
                    if (Pos.getX() >= MinX && Pos.getX() <= MaxX && Pos.getZ() >= MinZ && Pos.getZ() <= MaxZ && Pos.getY() >= Site.getY() - Scp354Basin.MaxDepth - 2
                            && Pos.getY() <= Site.getY() + Scp354Basin.ClearHeight) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
