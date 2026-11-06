package net.rai.scp_unleashed.scp2845;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.rai.scp_unleashed.scp002.Scp002Impact;

import java.util.Arrays;

public final class Scp2845Crater {
    public static final float FlatRadius = 3.2f;
    public static final float RimWidth = 3.0f;
    public static final int ScatterReach = 7;

    private Scp2845Crater() {
    }

    public record Result(BlockPos Stone, BlockPos Floor, float Radius) {
    }

    private static boolean mayChange(ServerLevel World, BlockPos Pos, boolean Griefing) {
        BlockState State = World.getBlockState(Pos);
        if (!State.getFluidState().isEmpty() || Scp002Impact.isProtected(World, Pos, State)) {
            return false;
        }
        return Griefing || Scp002Impact.isNatural(State);
    }

    private static boolean isOpen(BlockState State) {
        return State.isAir() || State.canBeReplaced() && State.getFluidState().isEmpty();
    }

    private static boolean isGround(BlockState State) {
        return !State.isAir() && !State.canBeReplaced() && State.getFluidState().isEmpty();
    }

    public static int groundY(ServerLevel World, int X, int Z) {
        int Top = World.getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z) - 1;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(X, Top, Z);
        for (int Step = 0; Step < 64 && Cursor.getY() > World.getMinBuildHeight(); Step++) {
            BlockState State = World.getBlockState(Cursor);
            if (isGround(State) && !State.is(Blocks.BARRIER) && !State.is(BlockTags.LEAVES) && !State.is(BlockTags.LOGS)) {
                return Cursor.getY();
            }
            Cursor.move(Direction.DOWN);
        }
        return Top;
    }

    private static int baseLevel(ServerLevel World, int X, int Z) {
        int[] Heights = new int[25];
        int Count = 0;
        for (int Dx = -2; Dx <= 2; Dx++) {
            for (int Dz = -2; Dz <= 2; Dz++) {
                Heights[Count++] = groundY(World, X + Dx, Z + Dz);
            }
        }
        Arrays.sort(Heights);
        return Heights[Count / 2];
    }

    public static BlockState lining(RandomSource Random, boolean Inner) {
        float Roll = Random.nextFloat();
        if (Inner) {
            if (Roll < 0.3f) {
                return Blocks.TUFF.defaultBlockState();
            }
            if (Roll < 0.5f) {
                return Blocks.CALCITE.defaultBlockState();
            }
            if (Roll < 0.68f) {
                return Blocks.COARSE_DIRT.defaultBlockState();
            }
            if (Roll < 0.8f) {
                return Blocks.SMOOTH_BASALT.defaultBlockState();
            }
            if (Roll < 0.9f) {
                return Blocks.GRAVEL.defaultBlockState();
            }
            return Blocks.MOSS_BLOCK.defaultBlockState();
        }
        if (Roll < 0.4f) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (Roll < 0.55f) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (Roll < 0.7f) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        if (Roll < 0.85f) {
            return Blocks.TUFF.defaultBlockState();
        }
        if (Roll < 0.95f) {
            return Blocks.ROOTED_DIRT.defaultBlockState();
        }
        return Blocks.MOSS_BLOCK.defaultBlockState();
    }

    public static Result strike(ServerLevel World, BlockPos Ground, RandomSource Random) {
        boolean Griefing = World.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        int Cx = Ground.getX();
        int Cz = Ground.getZ();
        float Radius = 6.8f + Random.nextFloat() * 1.2f;
        int Reach = (int) Math.ceil(Radius + RimWidth) + ScatterReach;
        for (int ChunkX = (Cx - Reach) >> 4; ChunkX <= (Cx + Reach) >> 4; ChunkX++) {
            for (int ChunkZ = (Cz - Reach) >> 4; ChunkZ <= (Cz + Reach) >> 4; ChunkZ++) {
                World.getChunk(ChunkX, ChunkZ);
            }
        }
        int Base = baseLevel(World, Cx, Cz);
        float Depth = 3.2f + Random.nextFloat() * 0.8f;
        int Floor0 = Math.round(Base - Depth);
        double PhaseA = Random.nextDouble() * Math.PI * 2.0;
        double PhaseB = Random.nextDouble() * Math.PI * 2.0;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
                if (Distance > Reach) {
                    continue;
                }
                double Theta = Math.atan2(Dz, Dx);
                double Local = Distance / (1.0 + 0.06 * Math.sin(3.0 * Theta + PhaseA) + 0.04 * Math.sin(5.0 * Theta + PhaseB));
                int X = Cx + Dx;
                int Z = Cz + Dz;
                int Top = World.getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z) - 1;
                int GroundTop = groundY(World, X, Z);
                if (Local < Radius) {
                    carve(World, Cursor, X, Z, Local, Radius, Floor0, Base, Top, GroundTop, Griefing, Random);
                } else if (Local < Radius + RimWidth) {
                    rim(World, Cursor, X, Z, (Local - Radius) / RimWidth, GroundTop, Griefing, Random);
                } else {
                    scatter(World, Cursor, X, Z, (Local - Radius - RimWidth) / ScatterReach, GroundTop, Griefing, Random);
                }
            }
        }
        BlockPos Floor = new BlockPos(Cx, Floor0, Cz);
        if (mayChange(World, Floor, true) || World.getBlockState(Floor).isAir()) {
            World.setBlock(Floor, Blocks.CALCITE.defaultBlockState(), Block.UPDATE_ALL);
        }
        BlockPos Stone = Floor.above();
        for (int Lift = 0; Lift < 4; Lift++) {
            BlockPos Above = Stone.above(Lift);
            if (!World.getBlockState(Above).isAir() && !Scp002Impact.isProtected(World, Above, World.getBlockState(Above))) {
                World.setBlock(Above, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        World.setBlock(Stone, Scp2845Registry.Stone.get().defaultBlockState().setValue(Scp2845StoneBlock.Sealed, true), Block.UPDATE_ALL);
        return new Result(Stone, Floor, Radius);
    }

    private static void carve(ServerLevel World, BlockPos.MutableBlockPos Cursor, int X, int Z, double Local, float Radius, int Floor0, int Base, int Top, int GroundTop,
                              boolean Griefing, RandomSource Random) {
        int Floor = Local <= FlatRadius ? Floor0 : (int) Math.round(Floor0 + (Base - Floor0) * Math.pow((Local - FlatRadius) / (Radius - FlatRadius), 1.6));
        int Ceiling = Math.max(Top, Base + 4);
        for (int Y = Floor + 1; Y <= Ceiling; Y++) {
            Cursor.set(X, Y, Z);
            if (!World.getBlockState(Cursor).isAir() && mayChange(World, Cursor, Griefing)) {
                World.setBlock(Cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        boolean Inner = Local < FlatRadius * 1.4;
        if (GroundTop < Floor && Floor - GroundTop <= 6) {
            for (int Y = GroundTop + 1; Y <= Floor; Y++) {
                Cursor.set(X, Y, Z);
                if (isOpen(World.getBlockState(Cursor)) && mayChange(World, Cursor, Griefing)) {
                    World.setBlock(Cursor, lining(Random, Inner), Block.UPDATE_ALL);
                }
            }
        }
        for (int Y = Floor - 1; Y <= Floor; Y++) {
            Cursor.set(X, Y, Z);
            BlockState Here = World.getBlockState(Cursor);
            if (isGround(Here) && mayChange(World, Cursor, Griefing)) {
                World.setBlock(Cursor, lining(Random, Inner), Block.UPDATE_ALL);
            }
        }
    }

    private static void rim(ServerLevel World, BlockPos.MutableBlockPos Cursor, int X, int Z, double Along, int GroundTop, boolean Griefing, RandomSource Random) {
        int Height = (int) Math.round(1.6 * Math.pow(1.0 - Along, 1.3) + (Random.nextFloat() - 0.5f) * 0.8f);
        for (int Step = 1; Step <= Height; Step++) {
            Cursor.set(X, GroundTop + Step, Z);
            if (isOpen(World.getBlockState(Cursor)) && mayChange(World, Cursor, Griefing)) {
                World.setBlock(Cursor, lining(Random, false), Block.UPDATE_ALL);
            }
        }
        if (Height <= 0) {
            scorch(World, Cursor, X, GroundTop, Z, Griefing, Random);
        }
    }

    private static void scatter(ServerLevel World, BlockPos.MutableBlockPos Cursor, int X, int Z, double Along, int GroundTop, boolean Griefing, RandomSource Random) {
        double Fade = Math.max(0.0, 1.0 - Along);
        if (Random.nextFloat() < 0.12 * Fade * Fade) {
            Cursor.set(X, GroundTop + 1, Z);
            if (isOpen(World.getBlockState(Cursor)) && mayChange(World, Cursor, Griefing)) {
                World.setBlock(Cursor, lining(Random, false), Block.UPDATE_ALL);
                return;
            }
        }
        if (Random.nextFloat() < 0.4 * Fade) {
            scorch(World, Cursor, X, GroundTop, Z, Griefing, Random);
        }
    }

    private static void scorch(ServerLevel World, BlockPos.MutableBlockPos Cursor, int X, int GroundTop, int Z, boolean Griefing, RandomSource Random) {
        Cursor.set(X, GroundTop, Z);
        BlockState Here = World.getBlockState(Cursor);
        if ((Here.is(Blocks.GRASS_BLOCK) || Here.is(Blocks.PODZOL) || Here.is(Blocks.MYCELIUM) || Here.is(Blocks.SNOW_BLOCK)) && mayChange(World, Cursor, Griefing)) {
            World.setBlock(Cursor, Random.nextFloat() < 0.7f ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.MOSS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            Cursor.move(Direction.UP);
            BlockState Above = World.getBlockState(Cursor);
            if (!Above.isAir() && Above.canBeReplaced() && Above.getFluidState().isEmpty()) {
                World.setBlock(Cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }
}
