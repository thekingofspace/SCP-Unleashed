package net.rai.scp_unleashed.scp457;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class Scp457Arrival {
    public static final int Wide = 7;
    public static final int Deep = 6;
    public static final int Tall = 4;
    public static final int RoofedRows = 4;
    public static final int Unevenness = 2;
    public static final int NearbyRadius = 12;
    public static final int NearbyTries = 8;
    public static final int Flags = Block.UPDATE_CLIENTS;
    public static final float WallDecay = 0.35f;
    private static final Block[] Walls = {Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS,
            Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS};
    private static final Block[] Floors = {Blocks.COARSE_DIRT, Blocks.SOUL_SOIL, Blocks.GRAVEL, Blocks.BLACKSTONE, Blocks.COARSE_DIRT};

    private Scp457Arrival() {
    }

    public static boolean spawn(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        BlockPos Surface = Found.Pos();
        RandomSource Random = World.getRandom();
        Vec3 Toward = Found.Investigator().position();
        if (shackFits(World, Surface)) {
            buildShack(World, Surface, Random);
            Vec3 Inside = new Vec3(Surface.getX() + 3.5, Surface.getY(), Surface.getZ() + 1.6);
            if (place(World, Inside, facing(Inside, Toward))) {
                return true;
            }
        }
        if (roomy(World, Surface) && place(World, Vec3.atBottomCenterOf(Surface), facing(Vec3.atBottomCenterOf(Surface), Toward))) {
            return true;
        }
        for (int Attempt = 0; Attempt < NearbyTries; Attempt++) {
            Optional<BlockPos> Near = LeadPlanner.standableNear(World, Surface, 2, NearbyRadius, 6, Random);
            if (Near.isPresent() && roomy(World, Near.get())) {
                Vec3 Spot = Vec3.atBottomCenterOf(Near.get());
                if (place(World, Spot, facing(Spot, Toward))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static float facing(Vec3 From, Vec3 Toward) {
        return (float) Math.toDegrees(Math.atan2(Toward.z - From.z, Toward.x - From.x)) - 90.0f;
    }

    public static boolean roomy(ServerLevel World, BlockPos Pos) {
        AABB Box = Scp457Registry.Burning.get().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(Pos));
        return World.noCollision(Box) && !World.containsAnyLiquid(Box.inflate(1.0, 0.0, 1.0)) && World.getBlockState(Pos.below()).isFaceSturdy(World, Pos.below(), Direction.UP);
    }

    public static boolean place(ServerLevel World, Vec3 Spot, float Yaw) {
        Scp457 Burning = Scp457Registry.Burning.get().create(World);
        if (Burning == null) {
            return false;
        }
        Burning.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Burning.setYBodyRot(Yaw);
        Burning.setYHeadRot(Yaw);
        if (!World.noCollision(Burning) || Scp457Water.touching(Burning)) {
            return false;
        }
        Burning.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.EVENT, null);
        Burning.setPersistenceRequired();
        return World.addFreshEntity(Burning);
    }

    public static boolean shackFits(ServerLevel World, BlockPos Surface) {
        return shackProblem(World, Surface) == null;
    }

    @Nullable
    public static String shackProblem(ServerLevel World, BlockPos Surface) {
        int Ground = Surface.getY();
        if (Ground - 1 <= World.getMinBuildHeight() + 1 || Ground + Tall + 1 >= World.getMaxBuildHeight()) {
            return "outside the build height";
        }
        for (int Dx = 0; Dx < Wide; Dx++) {
            for (int Dz = 0; Dz < Deep; Dz++) {
                BlockPos Column = Surface.offset(Dx, 0, Dz);
                if (!World.isLoaded(Column)) {
                    return "unloaded at " + Column.toShortString();
                }
                int Height = World.getHeight(Heightmap.Types.OCEAN_FLOOR, Column.getX(), Column.getZ());
                if (Math.abs(Height - Ground) > Unevenness) {
                    return "ground at " + Height + " near " + Column.toShortString() + " is too far from " + Ground;
                }
                if (!World.getFluidState(new BlockPos(Column.getX(), Height, Column.getZ())).isEmpty()) {
                    return "liquid at " + Column.toShortString();
                }
                for (int Y = Ground - 1; Y <= Ground + Tall; Y++) {
                    BlockPos Pos = new BlockPos(Column.getX(), Y, Column.getZ());
                    BlockState State = World.getBlockState(Pos);
                    if (State.getDestroySpeed(World, Pos) < 0.0f || State.hasBlockEntity() || !State.getFluidState().isEmpty()) {
                        return "blocked by " + State + " at " + Pos.toShortString();
                    }
                }
            }
        }
        return null;
    }

    public static void buildShack(ServerLevel World, BlockPos Surface, RandomSource Random) {
        int Ground = Surface.getY();
        BlockState Post = Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState Beam = Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState Slab = Blocks.BLACKSTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int Dx = 0; Dx < Wide; Dx++) {
            for (int Dz = 0; Dz < Deep; Dz++) {
                int X = Surface.getX() + Dx;
                int Z = Surface.getZ() + Dz;
                boolean Edge = Dx == 0 || Dx == Wide - 1 || Dz == 0 || Dz == Deep - 1;
                boolean Corner = (Dx == 0 || Dx == Wide - 1) && (Dz == 0 || Dz == Deep - 1);
                boolean Door = Dz == Deep - 1 && Dx == Wide / 2;
                boolean Window = Dx == 0 && Dz == Deep / 2;
                for (int Y = Ground - 3; Y < Ground - 1; Y++) {
                    BlockPos Pos = new BlockPos(X, Y, Z);
                    if (World.getBlockState(Pos).canBeReplaced()) {
                        World.setBlock(Pos, Blocks.BLACKSTONE.defaultBlockState(), Flags);
                    }
                }
                World.setBlock(new BlockPos(X, Ground - 1, Z), Floors[Random.nextInt(Floors.length)].defaultBlockState(), Flags);
                for (int Row = 0; Row < Tall; Row++) {
                    BlockPos Pos = new BlockPos(X, Ground + Row, Z);
                    BlockState State = Blocks.AIR.defaultBlockState();
                    if (Corner) {
                        State = Row < Tall - (Dz == Deep - 1 ? 1 : 0) ? Post : Blocks.AIR.defaultBlockState();
                    } else if (Edge && !(Door && Row < 3) && !(Window && Row == 1)) {
                        float Survives = Row < 2 ? 0.92f : Dz >= RoofedRows ? 0.3f : 1.0f - WallDecay * (Row - 1);
                        if (Random.nextFloat() < Survives) {
                            State = Walls[Random.nextInt(Walls.length)].defaultBlockState();
                        }
                    }
                    World.setBlock(Pos, State, Flags);
                }
                BlockPos Roof = new BlockPos(X, Ground + Tall, Z);
                if (Dz < RoofedRows) {
                    World.setBlock(Roof, Dz == RoofedRows - 1 && Dx > 0 && Dx < Wide - 1 ? Beam : Slab, Flags);
                } else if (Dz == RoofedRows && Random.nextFloat() < 0.35f) {
                    World.setBlock(Roof, Beam, Flags);
                }
            }
        }
        World.setBlock(new BlockPos(Surface.getX() + Wide - 2, Ground - 1, Surface.getZ() + 1), Blocks.NETHERRACK.defaultBlockState(), Flags);
        World.setBlock(new BlockPos(Surface.getX() + Wide - 2, Ground, Surface.getZ() + 1), Blocks.FIRE.defaultBlockState(), Flags);
        World.setBlock(new BlockPos(Surface.getX() + 1, Ground - 1, Surface.getZ() + Deep - 2), Blocks.HAY_BLOCK.defaultBlockState(), Flags);
        World.setBlock(new BlockPos(Surface.getX() + 1, Ground, Surface.getZ() + Deep - 2), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true), Flags);
        World.setBlock(new BlockPos(Surface.getX() + 1, Ground, Surface.getZ() + 1), Blocks.CAULDRON.defaultBlockState(), Flags);
    }
}
