package net.rai.scp_unleashed.scp163;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp002.Scp002Impact;
import net.rai.scp_unleashed.scp163.network.Scp163ImpactPayload;

import java.util.Arrays;

public final class Scp163Impact {
    public static final double SkidCentre = -4.0;
    public static final double SkidLength = 15.0;
    public static final double SkidWidth = 10.0;
    public static final double FlatShare = 0.32;
    public static final double RimShare = 1.45;
    public static final double ScatterShare = 2.1;
    public static final int SinkDepth = 3;
    public static final double AnnounceRange = 256.0;
    public static final int Fragments = 4;

    private Scp163Impact() {
    }

    public record Result(BlockPos Origin, Direction Front, BlockPos Hatch, float Radius) {
    }

    private static void loadArea(ServerLevel World, int X, int Z, int Reach) {
        for (int ChunkX = (X - Reach) >> 4; ChunkX <= (X + Reach) >> 4; ChunkX++) {
            for (int ChunkZ = (Z - Reach) >> 4; ChunkZ <= (Z + Reach) >> 4; ChunkZ++) {
                World.getChunk(ChunkX, ChunkZ);
            }
        }
    }

    private static int baseLevel(ServerLevel World, int X, int Z) {
        int[] Heights = new int[25];
        int Count = 0;
        for (int Dx = -2; Dx <= 2; Dx++) {
            for (int Dz = -2; Dz <= 2; Dz++) {
                Heights[Count++] = Scp002Impact.groundY(World, X + Dx * 2, Z + Dz * 2);
            }
        }
        Arrays.sort(Heights);
        return Heights[Count / 2];
    }

    private static boolean mayChange(ServerLevel World, BlockPos Pos, boolean Griefing) {
        BlockState State = World.getBlockState(Pos);
        if (!State.getFluidState().isEmpty() || Scp002Impact.isProtected(World, Pos, State)) {
            return false;
        }
        return Griefing || Scp002Impact.isNatural(State);
    }

    private static BlockState lining(RandomSource Random, boolean Deep) {
        float Roll = Random.nextFloat();
        if (Roll < 0.38f) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (Roll < 0.58f) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        if (Roll < 0.72f) {
            return Deep ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.DIRT.defaultBlockState();
        }
        if (Roll < 0.86f) {
            return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        }
        if (Roll < 0.95f) {
            return Blocks.TUFF.defaultBlockState();
        }
        return Deep ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.ROOTED_DIRT.defaultBlockState();
    }

    private static BlockState rim(RandomSource Random) {
        float Roll = Random.nextFloat();
        if (Roll < 0.45f) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (Roll < 0.7f) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (Roll < 0.86f) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        return Blocks.COBBLESTONE.defaultBlockState();
    }

    public static Vec3 along(Direction Front) {
        BlockPos Step = BlockPos.ZERO.east().rotate(Scp163Ship.turnFor(Front));
        return new Vec3(Step.getX(), 0.0, Step.getZ());
    }

    public static Result crash(ServerLevel World, BlockPos Ground, Direction Front, RandomSource Random) {
        GameRules Rules = World.getGameRules();
        boolean Griefing = Rules.getBoolean(GameRules.RULE_MOBGRIEFING);
        int Cx = Ground.getX();
        int Cz = Ground.getZ();
        int Reach = (int) Math.ceil((Math.abs(SkidCentre) + SkidLength) * ScatterShare * 0.75);
        loadArea(World, Cx, Cz, Reach + 2);
        int Base = baseLevel(World, Cx, Cz);
        BlockPos Origin = new BlockPos(Cx, Base - SinkDepth, Cz);
        Vec3 Along = along(Front);
        Vec3 Across = new Vec3(-Along.z, 0.0, Along.x);
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                double U = Dx * Along.x + Dz * Along.z;
                double V = Dx * Across.x + Dz * Across.z;
                double Shape = Math.pow((U - SkidCentre) / SkidLength, 2.0) + Math.pow(V / SkidWidth, 2.0);
                if (Shape > ScatterShare) {
                    continue;
                }
                int X = Cx + Dx;
                int Z = Cz + Dz;
                int Top = World.getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z) - 1;
                int GroundTop = Scp002Impact.groundY(World, X, Z);
                if (Shape < 1.0) {
                    double Rise = Mth.clamp((Shape - FlatShare) / (1.0 - FlatShare), 0.0, 1.0);
                    double Tail = Mth.clamp((-U - 8.0) / 14.0, 0.0, 1.0);
                    int Floor = (int) Math.round(Origin.getY() + (Base - Origin.getY()) * Math.max(Rise * Rise * (3.0 - 2.0 * Rise), Tail * 0.7));
                    carve(World, Cursor, X, Z, Floor, Base, Top, GroundTop, Griefing, Random, Shape < 0.6);
                } else if (Shape < RimShare) {
                    double Fade = 1.0 - (Shape - 1.0) / (RimShare - 1.0);
                    int Height = (int) Math.round(2.2 * Fade * (U > SkidCentre ? 1.0 : 0.5) + (Random.nextFloat() - 0.5f) * 0.8f);
                    for (int Step = 1; Step <= Height; Step++) {
                        Cursor.set(X, GroundTop + Step, Z);
                        BlockState Here = World.getBlockState(Cursor);
                        if ((Here.isAir() || Here.canBeReplaced() && Here.getFluidState().isEmpty()) && mayChange(World, Cursor, Griefing)) {
                            World.setBlock(Cursor, rim(Random), Block.UPDATE_ALL);
                        }
                    }
                } else if (Random.nextFloat() < 0.18f * (ScatterShare - Shape)) {
                    Cursor.set(X, GroundTop, Z);
                    BlockState Here = World.getBlockState(Cursor);
                    if ((Here.is(Blocks.GRASS_BLOCK) || Here.is(Blocks.DIRT) || Here.is(Blocks.PODZOL) || Here.is(Blocks.SNOW_BLOCK)) && mayChange(World, Cursor, Griefing)) {
                        World.setBlock(Cursor, Blocks.COARSE_DIRT.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
        Scp163Ship.build(World, Origin, Front, Pos -> !Scp002Impact.isProtected(World, Pos, World.getBlockState(Pos)));
        clearPorch(World, Origin, Front, Random);
        scatterFragments(World, Origin, Front, Random);
        spawnCastaway(World, Origin, Front);
        return new Result(Origin, Front, Scp163Ship.toWorld(Origin, Front, Scp163Ship.HatchLower), (float) SkidLength);
    }

    private static void carve(ServerLevel World, BlockPos.MutableBlockPos Cursor, int X, int Z, int Floor, int Base, int Top, int GroundTop, boolean Griefing,
                              RandomSource Random, boolean Deep) {
        int Ceiling = Math.max(Top, Base + 4);
        for (int Y = Floor + 1; Y <= Ceiling; Y++) {
            Cursor.set(X, Y, Z);
            if (!World.getBlockState(Cursor).isAir() && mayChange(World, Cursor, Griefing)) {
                World.setBlock(Cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        if (GroundTop < Floor && Floor - GroundTop <= 5) {
            for (int Y = GroundTop + 1; Y <= Floor; Y++) {
                Cursor.set(X, Y, Z);
                BlockState Here = World.getBlockState(Cursor);
                if ((Here.isAir() || Here.canBeReplaced() && Here.getFluidState().isEmpty()) && mayChange(World, Cursor, Griefing)) {
                    World.setBlock(Cursor, lining(Random, Deep), Block.UPDATE_ALL);
                }
            }
        }
        for (int Y = Floor - 1; Y <= Floor; Y++) {
            Cursor.set(X, Y, Z);
            BlockState Here = World.getBlockState(Cursor);
            if (!Here.isAir() && Here.getFluidState().isEmpty() && !Here.canBeReplaced() && mayChange(World, Cursor, Griefing)) {
                World.setBlock(Cursor, lining(Random, Deep), Block.UPDATE_ALL);
            }
        }
    }

    public static void clearPorch(ServerLevel World, BlockPos Origin, Direction Front, RandomSource Random) {
        for (int X = -1; X <= 1; X++) {
            for (int Z = Scp163Ship.HatchLower.getZ() + 1; Z <= Scp163Ship.HatchLower.getZ() + 3; Z++) {
                BlockPos Floor = Scp163Ship.toWorld(Origin, Front, X, 0, Z);
                BlockState Ground = World.getBlockState(Floor);
                if (!Scp002Impact.isProtected(World, Floor, Ground) && (Ground.isAir() || Ground.canBeReplaced() || Ground.is(Blocks.MAGMA_BLOCK))) {
                    World.setBlock(Floor, Random.nextFloat() < 0.7f ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_ALL);
                }
                for (int Y = 1; Y <= 3; Y++) {
                    BlockPos Above = Scp163Ship.toWorld(Origin, Front, X, Y, Z);
                    BlockState State = World.getBlockState(Above);
                    if (!State.isAir() && !Scp002Impact.isProtected(World, Above, State) && State.getFluidState().isEmpty()) {
                        World.setBlock(Above, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    private static void scatterFragments(ServerLevel World, BlockPos Origin, Direction Front, RandomSource Random) {
        for (int Index = 0; Index < Fragments; Index++) {
            int X = -11 - Random.nextInt(9);
            int Z = Random.nextInt(7) - 3;
            BlockPos Column = Scp163Ship.toWorld(Origin, Front, X, 0, Z);
            int Y = Scp002Impact.groundY(World, Column.getX(), Column.getZ()) + 1;
            BlockPos Spot = new BlockPos(Column.getX(), Y, Column.getZ());
            BlockState Here = World.getBlockState(Spot);
            if ((Here.isAir() || Here.canBeReplaced() && Here.getFluidState().isEmpty()) && !Scp002Impact.isProtected(World, Spot, Here)) {
                World.setBlock(Spot, (Random.nextBoolean() ? Scp163Registry.Hull : Scp163Registry.HullSeam).get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    public static void spawnCastaway(ServerLevel World, BlockPos Origin, Direction Front) {
        AABB Inside = Scp163Ship.interior(Origin, Front);
        if (!World.getEntitiesOfClass(Scp163Castaway.class, Inside.inflate(4.0)).isEmpty()) {
            return;
        }
        BlockPos Cell = Scp163Ship.toWorld(Origin, Front, Scp163Ship.SpawnCell);
        Scp163Castaway Creature = Scp163Registry.Castaway.get().create(World);
        if (Creature == null) {
            return;
        }
        float Yaw = Front.toYRot();
        Creature.moveTo(Cell.getX() + 0.5, Cell.getY(), Cell.getZ() + 0.5, Yaw, 0.0f);
        Creature.setYHeadRot(Yaw);
        Creature.yBodyRot = Yaw;
        Creature.finalizeSpawn(World, World.getCurrentDifficultyAt(Cell), MobSpawnType.EVENT, null);
        World.addFreshEntity(Creature);
    }

    public static void announce(ServerLevel World, BlockPos At, float Radius) {
        Scp163ImpactPayload Payload = new Scp163ImpactPayload(At, Radius);
        for (ServerPlayer Viewer : World.players()) {
            if (Viewer.distanceToSqr(Vec3.atCenterOf(At)) < AnnounceRange * AnnounceRange) {
                PacketDistributor.sendToPlayer(Viewer, Payload);
            }
        }
    }
}
