package net.rai.scp_unleashed.scp939;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Scp939Cave {
    public static final int GroupMin = 2;
    public static final int GroupMax = 4;
    public static final int SearchRadius = 24;
    public static final int ColumnStep = 3;
    public static final int MinCover = 6;
    public static final int CeilingReach = 8;
    public static final int FloorMargin = 4;
    public static final int GrottoDepth = 14;
    public static final double GrottoWide = 4.5;
    public static final double GrottoTall = 2.6;
    public static final int PlaceAttempts = 16;
    public static final int Flags = Block.UPDATE_CLIENTS;

    private Scp939Cave() {
    }

    public static int surface(ServerLevel World, int X, int Z) {
        return World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z);
    }

    public static boolean open(ServerLevel World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return State.getCollisionShape(World, Pos).isEmpty() && State.getFluidState().isEmpty();
    }

    @Nullable
    public static String problem(ServerLevel World, BlockPos Pos) {
        if (!World.isLoaded(Pos) || World.isOutsideBuildHeight(Pos.above(CeilingReach)) || Pos.getY() < World.getMinBuildHeight() + FloorMargin) {
            return "outside the loaded world";
        }
        if (!open(World, Pos)) {
            return "not open";
        }
        if (World.getBrightness(LightLayer.SKY, Pos) > 0) {
            return "open to the sky";
        }
        if (Pos.getY() > surface(World, Pos.getX(), Pos.getZ()) - MinCover) {
            return "too close to the surface";
        }
        BlockPos Under = Pos.below();
        if (!World.getBlockState(Under).isFaceSturdy(World, Under, Direction.UP)) {
            return "no floor";
        }
        for (BlockPos Near : BlockPos.betweenClosed(Pos.offset(-1, 0, -1), Pos.offset(1, 1, 1))) {
            if (!open(World, Near)) {
                return "too cramped";
            }
        }
        if (World.containsAnyLiquid(new AABB(Pos).inflate(2.0))) {
            return "flooded";
        }
        for (int Rise = 2; Rise <= CeilingReach; Rise++) {
            BlockState State = World.getBlockState(Pos.above(Rise));
            if (State.isAir()) {
                continue;
            }
            return State.isSolidRender(World, Pos.above(Rise)) ? null : "no rock ceiling";
        }
        return "no ceiling in reach";
    }

    public static boolean spot(ServerLevel World, BlockPos Pos) {
        return problem(World, Pos) == null;
    }

    public static Optional<BlockPos> find(ServerLevel World, BlockPos Centre, int Radius) {
        for (int Ring = 0; Ring <= Radius; Ring += ColumnStep) {
            for (int Dx = -Ring; Dx <= Ring; Dx += ColumnStep) {
                for (int Dz = -Ring; Dz <= Ring; Dz += ColumnStep) {
                    if (Math.max(Math.abs(Dx), Math.abs(Dz)) != Ring) {
                        continue;
                    }
                    Optional<BlockPos> Found = column(World, Centre.getX() + Dx, Centre.getZ() + Dz);
                    if (Found.isPresent()) {
                        return Found;
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<BlockPos> column(ServerLevel World, int X, int Z) {
        BlockPos Bottom = new BlockPos(X, World.getMinBuildHeight(), Z);
        if (!World.isLoaded(Bottom)) {
            return Optional.empty();
        }
        int High = surface(World, X, Z) - MinCover;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(X, High, Z);
        for (int Y = High; Y >= World.getMinBuildHeight() + FloorMargin; Y--) {
            Cursor.setY(Y);
            if (!World.getBlockState(Cursor).isAir() || World.getBrightness(LightLayer.SKY, Cursor) > 0) {
                continue;
            }
            if (spot(World, Cursor)) {
                return Optional.of(Cursor.immutable());
            }
        }
        return Optional.empty();
    }

    public static BlockPos grottoFor(ServerLevel World, BlockPos Site) {
        int Ground = surface(World, Site.getX(), Site.getZ());
        int Y = Math.max(World.getMinBuildHeight() + FloorMargin + 3, Ground - GrottoDepth);
        return new BlockPos(Site.getX(), Y, Site.getZ());
    }

    public static BlockPos carve(ServerLevel World, BlockPos Floor) {
        int Wide = Mth.ceil(GrottoWide);
        int Tall = Mth.ceil(GrottoTall * 2.0);
        BlockPos Centre = Floor.above(Mth.floor(GrottoTall));
        for (BlockPos Pos : BlockPos.betweenClosed(Floor.offset(-Wide, 0, -Wide), Floor.offset(Wide, Tall, Wide))) {
            double Dx = (Pos.getX() - Centre.getX()) / GrottoWide;
            double Dy = (Pos.getY() + 0.5 - Centre.getY()) / GrottoTall;
            double Dz = (Pos.getZ() - Centre.getZ()) / GrottoWide;
            if (Dx * Dx + Dy * Dy + Dz * Dz > 1.0 || Pos.getY() < Floor.getY()) {
                continue;
            }
            BlockState State = World.getBlockState(Pos);
            if (State.isAir() || State.getDestroySpeed(World, Pos) < 0.0f || State.hasBlockEntity()) {
                continue;
            }
            World.setBlock(Pos, Blocks.CAVE_AIR.defaultBlockState(), Flags);
        }
        for (BlockPos Pos : BlockPos.betweenClosed(Floor.offset(-Wide, -1, -Wide), Floor.offset(Wide, -1, Wide))) {
            if (World.getBlockState(Pos).isAir()) {
                World.setBlock(Pos, Blocks.STONE.defaultBlockState(), Flags);
            }
        }
        return Floor;
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        BlockPos Spot = Found.Pos();
        if (!spot(World, Spot)) {
            Optional<BlockPos> Near = find(World, Spot, SearchRadius / 2);
            Spot = Near.isPresent() ? Near.get() : carve(World, Spot);
        }
        return !arrive(World, Spot, Found.Investigator()).isEmpty();
    }

    public static int groupSize(RandomSource Random) {
        return Mth.nextInt(Random, GroupMin, GroupMax);
    }

    public static List<Scp939> arrive(ServerLevel World, BlockPos Spot, @Nullable ServerPlayer Reader) {
        return arrive(World, Spot, groupSize(World.getRandom()));
    }

    public static List<Scp939> arrive(ServerLevel World, BlockPos Spot, int Wanted) {
        List<Scp939> Made = new ArrayList<>();
        RandomSource Random = World.getRandom();
        Vec3 Centre = Vec3.atBottomCenterOf(Spot);
        int Allowed = Math.max(0, Math.min(Wanted, Scp939.LevelCap - Scp939Population.level(World)));
        for (int Index = 0; Index < Allowed; Index++) {
            Scp939 Hunter = Scp939Registry.Scp939Type.get().create(World);
            if (Hunter == null) {
                break;
            }
            Vec3 Place = Centre;
            for (int Attempt = 0; Attempt < PlaceAttempts; Attempt++) {
                double Angle = Random.nextDouble() * Math.PI * 2.0;
                double Reach = Attempt == 0 && Index == 0 ? 0.0 : 1.0 + Random.nextDouble() * 2.5;
                Vec3 Candidate = Centre.add(Math.cos(Angle) * Reach, 0.0, Math.sin(Angle) * Reach);
                if (World.noCollision(Hunter, Hunter.getDimensions(Hunter.getPose()).makeBoundingBox(Candidate))
                        && !World.getBlockState(BlockPos.containing(Candidate).below()).getCollisionShape(World, BlockPos.containing(Candidate).below()).isEmpty()) {
                    Place = Candidate;
                    break;
                }
            }
            float Yaw = Random.nextFloat() * 360.0f;
            Hunter.moveTo(Place.x, Place.y, Place.z, Yaw, 0.0f);
            Hunter.setYBodyRot(Yaw);
            Hunter.setYHeadRot(Yaw);
            Hunter.finalizeSpawn(World, World.getCurrentDifficultyAt(Spot), MobSpawnType.EVENT, null);
            if (World.addFreshEntity(Hunter)) {
                Made.add(Hunter);
            }
        }
        if (!Made.isEmpty()) {
            Scp939Sounds.play(World, Centre, Scp939Registry.GrowlLayers, 1.0f, 0.85f);
        }
        return Made;
    }
}
