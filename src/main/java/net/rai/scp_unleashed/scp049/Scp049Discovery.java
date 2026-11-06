package net.rai.scp_unleashed.scp049;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;

public final class Scp049Discovery {
    public static final int BellSearch = 64;
    public static final int MinBellGap = 3;
    public static final int MaxBellGap = 9;
    public static final int SiteAttempts = 48;
    public static final int Vertical = 6;

    private Scp049Discovery() {
    }

    public static boolean canHoldTray(ServerLevel World, BlockPos Cell) {
        if (!World.isLoaded(Cell) || World.isOutsideBuildHeight(Cell)) {
            return false;
        }
        BlockState At = World.getBlockState(Cell);
        BlockState Below = World.getBlockState(Cell.below());
        return At.canBeReplaced() && At.getFluidState().isEmpty() && Below.isFaceSturdy(World, Cell.below(), Direction.UP) && !Below.is(Scp049Registry.Tray.get());
    }

    public static boolean canStage(ServerLevel World, BlockPos Cell) {
        if (!canHoldTray(World, Cell)) {
            return false;
        }
        int Open = 0;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            if (LeadPlanner.isDryStandable(World, Cell.relative(Side))) {
                Open++;
            }
        }
        return Open >= 2 && World.getBlockState(Cell.above()).canBeReplaced();
    }

    public static Optional<BlockPos> villageSite(ServerLevel World, BlockPos Centre, RandomSource Random) {
        PoiManager Points = World.getPoiManager();
        Optional<BlockPos> Bell = Points.getInRange(Holder -> Holder.is(PoiTypes.MEETING), Centre, BellSearch, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos).min(Comparator.comparingDouble(Pos -> Pos.distSqr(Centre)));
        BlockPos Anchor = Bell.orElse(LeadPlanner.surface(World, Centre.getX(), Centre.getZ(), Centre.getY()));
        for (int Attempt = 0; Attempt < SiteAttempts; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Radius = MinBellGap + Random.nextDouble() * (MaxBellGap - MinBellGap) * (1.0 + Attempt / 16.0);
            int X = Anchor.getX() + Mth.floor(Math.cos(Angle) * Radius);
            int Z = Anchor.getZ() + Mth.floor(Math.sin(Angle) * Radius);
            BlockPos Column = new BlockPos(X, World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z), Z);
            for (int Dy = 0; Dy >= -Vertical; Dy--) {
                BlockPos Cell = Column.above(Dy);
                if (Math.abs(Cell.getY() - Anchor.getY()) <= Vertical && canStage(World, Cell)) {
                    return Optional.of(Cell);
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<BlockPos> openSite(ServerLevel World, BlockPos Centre, RandomSource Random) {
        if (canStage(World, Centre)) {
            return Optional.of(Centre);
        }
        for (int Attempt = 0; Attempt < SiteAttempts; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Radius = 1.0 + Random.nextDouble() * (2.0 + Attempt / 4.0);
            BlockPos Column = Centre.offset(Mth.floor(Math.cos(Angle) * Radius), 0, Mth.floor(Math.sin(Angle) * Radius));
            for (int Dy = Vertical; Dy >= -Vertical; Dy--) {
                BlockPos Cell = Column.above(Dy);
                if (canStage(World, Cell)) {
                    return Optional.of(Cell);
                }
            }
        }
        return Optional.empty();
    }

    @Nullable
    public static Scp049Doctor stage(ServerLevel World, BlockPos TrayCell, @Nullable Vec3 Facing) {
        Direction Front = Direction.NORTH;
        BlockPos WorkCell = null;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            if (LeadPlanner.isDryStandable(World, TrayCell.relative(Side)) || LeadPlanner.isDryStandable(World, TrayCell.relative(Side).below())) {
                if (WorkCell == null || Facing != null && Vec3.atCenterOf(TrayCell.relative(Side)).distanceToSqr(Facing) < Vec3.atCenterOf(WorkCell).distanceToSqr(Facing)) {
                    WorkCell = LeadPlanner.isDryStandable(World, TrayCell.relative(Side)) ? TrayCell.relative(Side) : TrayCell.relative(Side).below();
                    Front = Side;
                }
            }
        }
        if (WorkCell == null) {
            return null;
        }
        World.setBlock(TrayCell, Scp049Registry.Tray.get().defaultBlockState().setValue(Scp049TrayBlock.Facing, Front), Block.UPDATE_ALL);
        Scp049Doctor Doctor = Scp049Registry.Doctor.get().create(World);
        if (Doctor == null) {
            World.removeBlock(TrayCell, false);
            return null;
        }
        Vec3 Spot = Vec3.atBottomCenterOf(WorkCell);
        float Yaw = (float) (Mth.atan2(TrayCell.getZ() + 0.5 - Spot.z, TrayCell.getX() + 0.5 - Spot.x) * Mth.RAD_TO_DEG) - 90.0f;
        Doctor.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Doctor.setYHeadRot(Yaw);
        Doctor.setYBodyRot(Yaw);
        Doctor.finalizeSpawn(World, World.getCurrentDifficultyAt(WorkCell), MobSpawnType.EVENT, null);
        Doctor.setPersistenceRequired();
        if (!World.addFreshEntity(Doctor)) {
            World.removeBlock(TrayCell, false);
            return null;
        }
        Doctor.bindTray(World, TrayCell);
        World.playSound(null, TrayCell, Scp049Registry.Tools.get(), SoundSource.HOSTILE, 1.0f, 0.85f);
        World.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, Spot.x, Spot.y + 0.2, Spot.z, 6, 0.3, 0.1, 0.3, 0.005);
        return Doctor;
    }

    public static boolean spawn(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        BlockPos Site = Found.Pos();
        if (!canStage(World, Site)) {
            Optional<BlockPos> Near = openSite(World, Site, World.random);
            if (Near.isEmpty()) {
                return false;
            }
            Site = Near.get();
        }
        return stage(World, Site, Found.Investigator().position()) != null;
    }

    public static boolean anyDoctorNear(ServerLevel World, Vec3 Centre, double Range) {
        return !World.getEntitiesOfClass(Scp049Doctor.class, new AABB(Centre, Centre).inflate(Range)).isEmpty();
    }
}
