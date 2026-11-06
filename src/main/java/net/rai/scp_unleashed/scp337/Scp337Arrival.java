package net.rai.scp_unleashed.scp337;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;

import java.util.Optional;

public final class Scp337Arrival {
    public static final int Depth = 6;
    public static final int Wide = 5;
    public static final int Long = 5;
    public static final int Tall = 3;
    public static final int Unevenness = 3;
    public static final int NearbyRadius = 12;
    public static final int NearbyTries = 6;
    public static final int Flags = Block.UPDATE_CLIENTS;
    private static final Block[] Walls = {Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE, Blocks.MOSSY_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.MOSSY_COBBLESTONE};
    private static final Block[] Floors = {Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.SPRUCE_PLANKS, Blocks.PACKED_MUD, Blocks.COARSE_DIRT};
    private static final Block[] Ceilings = {Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_PLANKS};

    private Scp337Arrival() {
    }

    public static boolean spawn(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        BlockPos Surface = Found.Pos();
        RandomSource Random = World.getRandom();
        Vec3 Toward = Found.Investigator().position();
        if (cellarFits(World, Surface)) {
            BlockPos Floor = buildCellar(World, Surface, Random);
            if (place(World, new Vec3(Floor.getX() + 2.5, Floor.getY(), Floor.getZ() + 0.5), 90.0f)) {
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
        Vec3 Last = Vec3.atBottomCenterOf(Surface);
        return place(World, Last, facing(Last, Toward));
    }

    public static float facing(Vec3 From, Vec3 Toward) {
        return (float) Math.toDegrees(Math.atan2(Toward.z - From.z, Toward.x - From.x)) - 90.0f;
    }

    public static boolean roomy(ServerLevel World, BlockPos Pos) {
        AABB Box = Scp337Registry.Mass.get().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(Pos));
        return World.noCollision(Box) && !World.containsAnyLiquid(Box) && World.getBlockState(Pos.below()).isFaceSturdy(World, Pos.below(), Direction.UP);
    }

    public static boolean place(ServerLevel World, Vec3 Spot, float Yaw) {
        Scp337Mass Mass = Scp337Registry.Mass.get().create(World);
        if (Mass == null) {
            return false;
        }
        Mass.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Mass.setYBodyRot(Yaw);
        Mass.setYHeadRot(Yaw);
        if (!World.noCollision(Mass)) {
            return false;
        }
        Mass.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.EVENT, null);
        Mass.setPersistenceRequired();
        return World.addFreshEntity(Mass);
    }

    public static boolean cellarFits(ServerLevel World, BlockPos Surface) {
        if (World.dimensionType().hasCeiling()) {
            return false;
        }
        int X = Surface.getX();
        int Z = Surface.getZ();
        int Top = Surface.getY();
        int Floor = Top - Depth;
        if (Floor - 1 <= World.getMinBuildHeight() + 1) {
            return false;
        }
        BlockPos Ground = Surface.below();
        BlockState GroundState = World.getBlockState(Ground);
        if (GroundState.is(BlockTags.LOGS) || GroundState.is(BlockTags.LEAVES) || !GroundState.getFluidState().isEmpty() || !World.getFluidState(Surface).isEmpty()) {
            return false;
        }
        for (int Dx = -1; Dx <= Wide; Dx++) {
            for (int Dz = -3; Dz <= 3; Dz++) {
                BlockPos Column = new BlockPos(X + Dx, Top, Z + Dz);
                if (!World.isLoaded(Column)) {
                    return false;
                }
                int Height = World.getHeight(Heightmap.Types.OCEAN_FLOOR, Column.getX(), Column.getZ());
                if (Math.abs(Height - Top) > Unevenness || !World.getFluidState(new BlockPos(Column.getX(), Height, Column.getZ())).isEmpty()) {
                    return false;
                }
                for (int Y = Floor - 1; Y <= Floor + Tall; Y++) {
                    BlockPos Pos = new BlockPos(Column.getX(), Y, Column.getZ());
                    BlockState State = World.getBlockState(Pos);
                    if (State.getDestroySpeed(World, Pos) < 0.0f || State.hasBlockEntity()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static BlockPos buildCellar(ServerLevel World, BlockPos Surface, RandomSource Random) {
        int X = Surface.getX();
        int Z = Surface.getZ();
        int Top = Surface.getY();
        int Floor = Top - Depth;
        for (int Dx = -1; Dx <= Wide; Dx++) {
            for (int Dz = -(Long / 2) - 1; Dz <= Long / 2 + 1; Dz++) {
                for (int Y = Floor - 1; Y <= Floor + Tall; Y++) {
                    BlockPos Pos = new BlockPos(X + Dx, Y, Z + Dz);
                    boolean Shell = Dx == -1 || Dx == Wide || Math.abs(Dz) == Long / 2 + 1;
                    BlockState State;
                    if (Y == Floor - 1) {
                        State = pick(Floors, Random).defaultBlockState();
                    } else if (Y == Floor + Tall) {
                        State = pick(Ceilings, Random).defaultBlockState();
                    } else if (Shell) {
                        State = pick(Walls, Random).defaultBlockState();
                    } else {
                        State = Blocks.AIR.defaultBlockState();
                    }
                    World.setBlock(Pos, State, Flags);
                }
            }
        }
        for (int Y = Floor + Tall; Y < Top; Y++) {
            BlockPos Shaft = new BlockPos(X, Y, Z);
            if (Y > Floor + Tall && Y < Top - 1) {
                for (Direction Side : Direction.Plane.HORIZONTAL) {
                    BlockPos Lining = Shaft.relative(Side);
                    BlockState Existing = World.getBlockState(Lining);
                    if (Side == Direction.WEST || !Existing.isSolidRender(World, Lining) || !Existing.getFluidState().isEmpty()) {
                        World.setBlock(Lining, pick(Walls, Random).defaultBlockState(), Flags);
                    }
                }
            }
        }
        BlockState Ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST);
        for (int Y = Floor; Y < Top - 1; Y++) {
            World.setBlock(new BlockPos(X, Y, Z), Ladder, Flags);
        }
        World.setBlock(new BlockPos(X, Top - 1, Z), Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.EAST)
                .setValue(TrapDoorBlock.OPEN, true), Flags);
        World.setBlock(new BlockPos(X + Wide - 1, Floor, Z - Long / 2), Blocks.BARREL.defaultBlockState(), Flags);
        World.setBlock(new BlockPos(X + Wide - 1, Floor + Tall - 1, Z + Long / 2), Blocks.COBWEB.defaultBlockState(), Flags);
        World.setBlock(new BlockPos(X + 1, Floor + Tall - 1, Z - Long / 2), Blocks.COBWEB.defaultBlockState(), Flags);
        World.setBlock(new BlockPos(X + Wide - 1, Floor, Z + Long / 2), Blocks.MOSS_CARPET.defaultBlockState(), Flags);
        World.setBlock(new BlockPos(X + 2, Floor, Z + Long / 2), Blocks.MOSS_CARPET.defaultBlockState(), Flags);
        return new BlockPos(X, Floor, Z);
    }

    private static Block pick(Block[] Options, RandomSource Random) {
        return Options[Random.nextInt(Options.length)];
    }
}
