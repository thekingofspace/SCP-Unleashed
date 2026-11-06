package net.rai.scp_unleashed.scp003;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class Scp003Ritual {
    public static final int SiteRadius = 16;
    public static final int SiteSamples = 40;
    public static final int Vertical = 4;
    public static final int Clearance = 3;
    public static final int Spacing = 4;
    public static final int PathChecks = 3;

    private Scp003Ritual() {
    }

    public static List<BlockPos> corners(BlockPos Centre) {
        return List.of(Centre.offset(-1, 0, -1), Centre.offset(1, 0, -1), Centre.offset(-1, 0, 1), Centre.offset(1, 0, 1));
    }

    public static boolean isCorner(BlockPos Centre, BlockPos Pos) {
        return Pos.getY() == Centre.getY() && Math.abs(Pos.getX() - Centre.getX()) == 1 && Math.abs(Pos.getZ() - Centre.getZ()) == 1;
    }

    private static boolean isOurs(BlockState State) {
        return State.getBlock() instanceof Scp003PolypBlock || State.getBlock() instanceof Scp003BoardBlock || State.getBlock() instanceof Scp003PlateBlock;
    }

    private static boolean isFree(Level World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return State.getFluidState().isEmpty() && (State.isAir() || State.canBeReplaced() && State.getCollisionShape(World, Pos).isEmpty());
    }

    public static boolean isOpenSite(Level World, BlockPos Centre, boolean AllowPolyps) {
        if (!World.isLoaded(Centre) || World.isOutsideBuildHeight(Centre.above(Clearance))) {
            return false;
        }
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                BlockPos Cell = Centre.offset(Dx, 0, Dz);
                BlockPos Ground = Cell.below();
                BlockState Floor = World.getBlockState(Ground);
                if (!Floor.isFaceSturdy(World, Ground, Direction.UP) || !Floor.getFluidState().isEmpty() || isOurs(Floor)) {
                    return false;
                }
                BlockState At = World.getBlockState(Cell);
                boolean Corner = Dx != 0 && Dz != 0;
                if (!(Corner && AllowPolyps && At.getBlock() instanceof Scp003PolypBlock) && !isFree(World, Cell)) {
                    return false;
                }
                if (!isFree(World, Cell.above())) {
                    return false;
                }
            }
        }
        for (int Dy = 0; Dy < Clearance; Dy++) {
            if (!isFree(World, Centre.above(Dy))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isCrowded(Level World, BlockPos Centre) {
        for (BlockPos Pos : BlockPos.betweenClosed(Centre.offset(-Spacing, -2, -Spacing), Centre.offset(Spacing, 2, Spacing))) {
            if (isCorner(Centre, Pos)) {
                continue;
            }
            BlockState State = World.getBlockState(Pos);
            if (State.getBlock() instanceof Scp003PolypBlock || State.getBlock() instanceof Scp003BoardBlock) {
                return true;
            }
        }
        return false;
    }

    public static Optional<BlockPos> findSite(ServerLevel World, Mob Seeker, RandomSource Random) {
        BlockPos Origin = Seeker.blockPosition();
        List<BlockPos> Valid = new ArrayList<>();
        for (int Sample = 0; Sample < SiteSamples; Sample++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Distance = 2.0 + Random.nextDouble() * (SiteRadius - 2.0);
            int X = Origin.getX() + Mth.floor(Math.cos(Angle) * Distance);
            int Z = Origin.getZ() + Mth.floor(Math.sin(Angle) * Distance);
            for (int Dy = Vertical; Dy >= -Vertical; Dy--) {
                BlockPos Centre = new BlockPos(X, Origin.getY() + Dy, Z);
                if (World.isLoaded(Centre) && isOpenSite(World, Centre, false) && !isCrowded(World, Centre)) {
                    Valid.add(Centre);
                    break;
                }
            }
        }
        Valid.sort(Comparator.comparingDouble(Centre -> Centre.distSqr(Origin)));
        for (int Index = 0; Index < Math.min(PathChecks, Valid.size()); Index++) {
            Path Route = Seeker.getNavigation().createPath(Valid.get(Index), 1);
            if (Route != null && Route.canReach()) {
                return Optional.of(Valid.get(Index));
            }
        }
        return Optional.empty();
    }

    public static boolean canFormAt(Level World, BlockPos Centre) {
        BlockState Floor = World.getBlockState(Centre.below());
        BlockState At = World.getBlockState(Centre);
        return Floor.isFaceSturdy(World, Centre.below(), Direction.UP) && At.getFluidState().isEmpty() && (At.isAir() || At.canBeReplaced());
    }
}
