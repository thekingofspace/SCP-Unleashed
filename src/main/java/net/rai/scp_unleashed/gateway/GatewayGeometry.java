package net.rai.scp_unleashed.gateway;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class GatewayGeometry {
    public static final double PixelsWide = 32.0;
    public static final double PixelsHigh = 48.0;
    public static final double PixelsDeep = 16.0;
    public static final double OriginCentreX = 24.0;
    public static final double OriginCentreZ = 8.0;
    public static final double PostWidth = 6.0;
    public static final double LintelBottom = 38.0;
    public static final double FootHeight = 3.0;
    public static final AABB Cells = new AABB(0.0, 0.0, 0.0, PixelsWide, PixelsHigh, PixelsDeep);
    public static final AABB LeftFoot = new AABB(0.0, 0.0, 0.0, PostWidth, FootHeight, PixelsDeep);
    public static final AABB RightFoot = new AABB(PixelsWide - PostWidth, 0.0, 0.0, PixelsWide, FootHeight, PixelsDeep);
    public static final AABB LeftPost = new AABB(0.0, FootHeight, 2.0, PostWidth, LintelBottom, 14.0);
    public static final AABB RightPost = new AABB(PixelsWide - PostWidth, FootHeight, 2.0, PixelsWide, LintelBottom, 14.0);
    public static final AABB Lintel = new AABB(0.0, LintelBottom, 1.0, PixelsWide, PixelsHigh, 15.0);
    public static final AABB Opening = new AABB(PostWidth, 0.0, 0.0, PixelsWide - PostWidth, LintelBottom, PixelsDeep);
    public static final AABB Threshold = new AABB(PostWidth + 1.0, 0.0, 5.5, PixelsWide - PostWidth - 1.0, LintelBottom - 2.0, 10.5);
    public static final AABB Surface = new AABB(PostWidth, 0.0, 7.5, PixelsWide - PostWidth, LintelBottom, 8.5);
    public static final Vec3 Socket = new Vec3(PixelsWide / 2.0, 43.0, 2.6);
    public static final List<AABB> Frame = List.of(LeftFoot, RightFoot, LeftPost, RightPost, Lintel);
    public static final double[] LampColumns = {4.0, 28.0};
    public static final double LampHeight = 45.0;

    private GatewayGeometry() {
    }

    public static Direction right(Direction Front) {
        return Front.getClockWise();
    }

    public static Direction away(Direction Front) {
        return Front.getOpposite();
    }

    public static Direction portSide(Direction Front) {
        return Front.getCounterClockWise();
    }

    public static Vec3 pivot(BlockPos Origin, Direction Front) {
        Direction Right = right(Front);
        Direction Away = away(Front);
        double Across = (PixelsWide / 2 - OriginCentreX) / 16.0;
        double Along = (PixelsDeep / 2 - OriginCentreZ) / 16.0;
        return new Vec3(Origin.getX() + 0.5 + Right.getStepX() * Across + Away.getStepX() * Along, Origin.getY(),
                Origin.getZ() + 0.5 + Right.getStepZ() * Across + Away.getStepZ() * Along);
    }

    public static Vec3 toWorld(BlockPos Origin, Direction Front, Vec3 Local) {
        Vec3 Pivot = pivot(Origin, Front);
        Direction Right = right(Front);
        Direction Away = away(Front);
        double Across = (Local.x - PixelsWide / 2) / 16.0;
        double Along = (Local.z - PixelsDeep / 2) / 16.0;
        return new Vec3(Pivot.x + Right.getStepX() * Across + Away.getStepX() * Along, Pivot.y + Local.y / 16.0,
                Pivot.z + Right.getStepZ() * Across + Away.getStepZ() * Along);
    }

    public static Vec3 toLocal(BlockPos Origin, Direction Front, Vec3 World) {
        Vec3 Relative = World.subtract(pivot(Origin, Front));
        Direction Right = right(Front);
        Direction Away = away(Front);
        return new Vec3((Relative.x * Right.getStepX() + Relative.z * Right.getStepZ()) * 16.0 + PixelsWide / 2, Relative.y * 16.0,
                (Relative.x * Away.getStepX() + Relative.z * Away.getStepZ()) * 16.0 + PixelsDeep / 2);
    }

    public static AABB toWorld(BlockPos Origin, Direction Front, AABB Local) {
        return new AABB(toWorld(Origin, Front, new Vec3(Local.minX, Local.minY, Local.minZ)), toWorld(Origin, Front, new Vec3(Local.maxX, Local.maxY, Local.maxZ)));
    }

    public static Vec3 centre(BlockPos Origin, Direction Front) {
        return toWorld(Origin, Front, new Vec3(PixelsWide / 2, LintelBottom / 2, PixelsDeep / 2));
    }

    public static Vec3 frontStep(BlockPos Origin, Direction Front, double Blocks) {
        Vec3 Middle = toWorld(Origin, Front, new Vec3(PixelsWide / 2, 0.0, PixelsDeep / 2));
        return Middle.add(Front.getStepX() * Blocks, 0.0, Front.getStepZ() * Blocks);
    }

    public static float frontYaw(Direction Front) {
        return Front.toYRot();
    }
}
