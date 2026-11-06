package net.rai.scp_unleashed.rifttech;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.rift.RiftEntity;
import net.rai.scp_unleashed.rift.RiftFormation;
import net.rai.scp_unleashed.rift.RiftRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class GridField {
    public static final double Margin = 0.08;
    public static final double Spacing = 0.5;
    public static final int Settle = 3;

    private GridField() {
    }

    public static List<RiftEntity> riftsInside(ServerLevel World, PylonGrid.Layout Grid) {
        AABB Volume = Grid.volume();
        return World.getEntitiesOfClass(RiftEntity.class, Volume.inflate(1.0), Rift -> Rift.isAlive() && Volume.contains(Rift.getBoundingBox().getCenter()));
    }

    public static boolean contains(PylonGrid.Layout Grid, AABB Box) {
        AABB Volume = Grid.volume();
        return Box.minX >= Volume.minX - 1.0E-6 && Box.maxX <= Volume.maxX + 1.0E-6 && Box.minY >= Volume.minY - 1.0E-6
                && Box.maxY <= Volume.maxY + 1.0E-6 && Box.minZ >= Volume.minZ - 1.0E-6 && Box.maxZ <= Volume.maxZ + 1.0E-6;
    }

    public static AABB boxAt(Vec3 Bottom, double Height) {
        double Half = Height * RiftEntity.WidthRatio * 0.5;
        return new AABB(Bottom.x - Half, Bottom.y, Bottom.z - Half, Bottom.x + Half, Bottom.y + Height, Bottom.z + Half);
    }

    @Nullable
    public static Vec3 findSpot(ServerLevel World, PylonGrid.Layout Grid, double Height, boolean Centred, RandomSource Random) {
        AABB Volume = Grid.volume();
        double Half = Height * RiftEntity.WidthRatio * 0.5 + Margin;
        double SpanX = Volume.getXsize() - 2.0 * Half;
        double SpanZ = Volume.getZsize() - 2.0 * Half;
        double SpanY = Volume.getYsize() - Height - 2.0 * Margin;
        if (SpanX < 0.0 || SpanZ < 0.0 || SpanY < 0.0) {
            return null;
        }
        List<RiftEntity> Present = riftsInside(World, Grid);
        for (int Attempt = 0; Attempt < GridTuning.Attempts; Attempt++) {
            boolean Middle = Centred && Attempt < GridTuning.Attempts / 3;
            double Spread = Middle ? 0.15 + 0.1 * Attempt : 1.0;
            double X = Volume.minX + Half + SpanX * Mth.clamp(0.5 + (Random.nextDouble() - 0.5) * (Middle ? Spread : 1.0), 0.0, 1.0);
            double Z = Volume.minZ + Half + SpanZ * Mth.clamp(0.5 + (Random.nextDouble() - 0.5) * (Middle ? Spread : 1.0), 0.0, 1.0);
            double Start = Volume.minY + Margin + Random.nextDouble() * SpanY;
            int Column = Mth.floor(X);
            int Row = Mth.floor(Z);
            int Floor = Mth.floor(Start);
            int Lowest = Math.max(Mth.floor(Volume.minY), Floor - Settle);
            while (Floor > Lowest && World.getBlockState(new BlockPos(Column, Floor - 1, Row)).getCollisionShape(World, new BlockPos(Column, Floor - 1, Row)).isEmpty()) {
                Floor--;
            }
            double Lift = 0.25 + Random.nextDouble() * 0.5;
            double Bottom = Math.max(Volume.minY + Margin, Math.min(Floor + Lift, Volume.maxY - Height - Margin));
            Vec3 Spot = new Vec3(X, Bottom, Z);
            AABB Box = boxAt(Spot, Height);
            if (!contains(Grid, Box) || !RiftFormation.isOpen(World, Box.inflate(0.05, 0.02, 0.05))) {
                continue;
            }
            if (crowded(Present, Box)) {
                continue;
            }
            return Spot;
        }
        return null;
    }

    private static boolean crowded(List<RiftEntity> Present, AABB Box) {
        AABB Wide = Box.inflate(Spacing);
        for (RiftEntity Rift : Present) {
            if (Rift.getBoundingBox().intersects(Wide)) {
                return true;
            }
        }
        return false;
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

    @Nullable
    public static ApocalypticRiftEntity openApocalyptic(ServerLevel World, Vec3 Spot, RandomSource Random) {
        ApocalypticRiftEntity Rift = RiftTechRegistry.ApocalypticRift.get().create(World);
        if (Rift == null) {
            return null;
        }
        Rift.moveTo(Spot.x, Spot.y, Spot.z, Random.nextFloat() * 360.0f, 0.0f);
        return World.addFreshEntity(Rift) ? Rift : null;
    }
}
