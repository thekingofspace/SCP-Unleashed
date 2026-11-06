package net.rai.scp_unleashed.matrix;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.rift.RiftEntity;
import net.rai.scp_unleashed.rift.RiftFormation;
import net.rai.scp_unleashed.rift.RiftRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MatrixField {
    public static final float SurfaceShareOpen = 0.6f;
    public static final float SurfaceShareCovered = 0.12f;
    public static final int DepthBelow = 6;
    public static final int DepthAbove = 10;

    private MatrixField() {
    }

    public static Census census(ServerLevel World, Vec3 Centre) {
        double CapReach = RiftFormation.CapRadius;
        double ScopeReach = MatrixTuning.ScopeReach;
        List<? extends RiftEntity> Rifts = World.getEntities(RiftRegistry.Rift.get(), new AABB(Centre, Centre).inflate(CapReach), Rift -> Rift.distanceToSqr(Centre) <= CapReach * CapReach);
        int Counted = Rifts.size();
        List<RiftEntity> Shown = new ArrayList<>();
        for (RiftEntity Rift : Rifts) {
            double DeltaX = Rift.getX() - Centre.x;
            double DeltaZ = Rift.getZ() - Centre.z;
            if (!Rift.isCollapsing() && DeltaX * DeltaX + DeltaZ * DeltaZ <= ScopeReach * ScopeReach && Math.abs(Rift.getY() - Centre.y) <= ScopeReach) {
                Shown.add(Rift);
            }
        }
        Shown.sort(Comparator.comparingDouble(Rift -> Rift.distanceToSqr(Centre)));
        int[] Blips = new int[InstabilityMatrixBlockEntity.BlipCount * 2];
        for (int Index = 0; Index < Math.min(Shown.size(), InstabilityMatrixBlockEntity.BlipCount); Index++) {
            RiftEntity Rift = Shown.get(Index);
            Blips[Index * 2] = packOffset(Rift.getX() - Centre.x, Rift.getZ() - Centre.z);
            Blips[Index * 2 + 1] = Rift.getRarity().getTier();
        }
        return new Census(Counted, Counted >= RiftFormation.CapCount, Blips);
    }

    public static int packOffset(double DeltaX, double DeltaZ) {
        int X = Mth.clamp((int) Math.round(DeltaX), -63, 63) + 63;
        int Z = Mth.clamp((int) Math.round(DeltaZ), -63, 63) + 63;
        return X | (Z << 7);
    }

    public static int unpackX(int Packed) {
        return (Packed & 127) - 63;
    }

    public static int unpackZ(int Packed) {
        return ((Packed >> 7) & 127) - 63;
    }

    @Nullable
    public static Vec3 findSpot(ServerLevel World, Vec3 Centre, FragmentRarity Tier, RandomSource Random) {
        BlockPos Origin = BlockPos.containing(Centre);
        boolean Open = World.canSeeSky(Origin.above(2));
        float SurfaceShare = Open ? SurfaceShareOpen : SurfaceShareCovered;
        double Inner = MatrixTuning.MinRange;
        double Outer = MatrixTuning.FieldRadius;
        for (int Attempt = 0; Attempt < MatrixTuning.Attempts; Attempt++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Distance = Math.sqrt(Inner * Inner + Random.nextDouble() * (Outer * Outer - Inner * Inner));
            int X = Mth.floor(Centre.x + Math.cos(Angle) * Distance);
            int Z = Mth.floor(Centre.z + Math.sin(Angle) * Distance);
            if (!World.isPositionEntityTicking(new BlockPos(X, Origin.getY(), Z))) {
                continue;
            }
            int Y;
            if (Random.nextFloat() < SurfaceShare) {
                Y = World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z);
                if (Math.abs(Y - Origin.getY()) > MatrixTuning.VerticalReach) {
                    Y = Origin.getY() + Random.nextInt(DepthBelow + DepthAbove + 1) - DepthBelow;
                }
            } else {
                Y = Origin.getY() + Random.nextInt(DepthBelow + DepthAbove + 1) - DepthBelow;
            }
            Vec3 Spot = RiftFormation.findSpot(World, X, Y, Z, Tier, Random);
            if (Spot == null || !acceptable(World, Centre, Spot)) {
                continue;
            }
            return Spot;
        }
        return null;
    }

    private static boolean acceptable(ServerLevel World, Vec3 Centre, Vec3 Spot) {
        double DeltaX = Spot.x - Centre.x;
        double DeltaZ = Spot.z - Centre.z;
        double Flat = DeltaX * DeltaX + DeltaZ * DeltaZ;
        if (Flat < MatrixTuning.MinRange * MatrixTuning.MinRange || Flat > MatrixTuning.FieldRadius * MatrixTuning.FieldRadius) {
            return false;
        }
        if (Math.abs(Spot.y - Centre.y) > MatrixTuning.VerticalReach) {
            return false;
        }
        double Clearance = MatrixTuning.PlayerClearance * MatrixTuning.PlayerClearance;
        for (ServerPlayer User : World.players()) {
            if (!User.isSpectator() && User.position().distanceToSqr(Spot) < Clearance) {
                return false;
            }
        }
        return RiftFormation.countNearby(World, Spot, RiftFormation.CapRadius) < RiftFormation.CapCount;
    }

    @Nullable
    public static RiftEntity open(ServerLevel World, Vec3 Spot, FragmentRarity Tier, RandomSource Random) {
        RiftEntity Rift = RiftRegistry.Rift.get().create(World);
        if (Rift == null) {
            return null;
        }
        Rift.setRarity(Tier);
        Rift.moveTo(Spot.x, Spot.y, Spot.z, Random.nextFloat() * 360.0f, 0.0f);
        return World.addFreshEntity(Rift) ? Rift : null;
    }

    public record Census(int Nearby, boolean Saturated, int[] Blips) {
    }
}
