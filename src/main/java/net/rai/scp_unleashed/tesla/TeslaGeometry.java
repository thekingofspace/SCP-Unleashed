package net.rai.scp_unleashed.tesla;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TeslaGeometry {
    public static final double PixelsWide = 32.0;
    public static final double PixelsHigh = 48.0;
    public static final double PixelsDeep = 16.0;
    public static final double OriginCentreX = 24.0;
    public static final double OriginCentreZ = 8.0;
    public static final double PostWidth = 4.0;
    public static final double LintelBottom = 40.0;
    public static final double ArcInset = 0.5;
    public static final double StrikeTolerance = 0.001;
    public static final double[] PostRows = {8.0, 21.0, 34.0};
    public static final double[][] PostSegments = {{1.0, 7.0}, {9.0, 15.0}};
    public static final double[] UnderRails = {4.0, 12.0};
    public static final double[][] UnderSegments = {{5.0, 11.0}, {13.0, 19.0}, {21.0, 27.0}};
    public static final double LampHeight = 44.0;
    public static final double[] LampColumns = {8.0, 24.0};
    public static final AABB Cells = new AABB(0.0, 0.0, 0.0, PixelsWide, PixelsHigh, PixelsDeep);
    public static final AABB LeftPost = new AABB(0.0, 0.0, 0.0, PostWidth, LintelBottom, PixelsDeep);
    public static final AABB RightPost = new AABB(PixelsWide - PostWidth, 0.0, 0.0, PixelsWide, LintelBottom, PixelsDeep);
    public static final AABB Lintel = new AABB(0.0, LintelBottom, 0.0, PixelsWide, PixelsHigh, PixelsDeep);
    public static final AABB Opening = new AABB(PostWidth, 0.0, 0.0, PixelsWide - PostWidth, LintelBottom, PixelsDeep);
    public static final List<AABB> Frame = List.of(LeftPost, RightPost, Lintel);
    public static final Vec3 Centre = new Vec3(PixelsWide / 2, PixelsHigh / 2, PixelsDeep / 2);
    public static final List<Emitter> Emitters = emitters();
    public static final List<Emitter> BarPoints = barPoints();
    public static final List<Emitter> Lamps = lamps();
    public static final List<Vec3> Glows = glows();

    private TeslaGeometry() {
    }

    public record Emitter(Vec3 Start, Vec3 End, TeslaSurface Surface) {
        public Vec3 at(double Fraction) {
            return Start.add(End.subtract(Start).scale(Fraction));
        }
    }

    private static List<Emitter> emitters() {
        List<Emitter> Bars = new ArrayList<>();
        for (TeslaSurface Post : new TeslaSurface[]{TeslaSurface.LeftPost, TeslaSurface.RightPost}) {
            double Across = Post.low(0);
            for (double Height : PostRows) {
                for (double[] Segment : PostSegments) {
                    Bars.add(new Emitter(new Vec3(Across, Height, Segment[0]), new Vec3(Across, Height, Segment[1]), Post));
                }
            }
        }
        for (double Rail : UnderRails) {
            for (double[] Segment : UnderSegments) {
                Bars.add(new Emitter(new Vec3(Segment[0], LintelBottom, Rail), new Vec3(Segment[1], LintelBottom, Rail), TeslaSurface.Underside));
            }
        }
        return Collections.unmodifiableList(Bars);
    }

    private static List<Emitter> barPoints() {
        List<Emitter> Points = new ArrayList<>();
        for (Emitter Bar : emitters()) {
            for (double Step = 0.25; Step < 1.0; Step += 0.25) {
                Vec3 Point = Bar.at(Step);
                Points.add(new Emitter(Point, Point, Bar.Surface()));
            }
        }
        return Collections.unmodifiableList(Points);
    }

    private static List<Emitter> lamps() {
        List<Emitter> Points = new ArrayList<>();
        for (TeslaSurface Face : new TeslaSurface[]{TeslaSurface.LintelFront, TeslaSurface.LintelBack}) {
            for (double Across : LampColumns) {
                Vec3 Point = new Vec3(Across, LampHeight, Face.low(2));
                Points.add(new Emitter(Point, Point, Face));
            }
        }
        return Collections.unmodifiableList(Points);
    }

    private static List<Vec3> glows() {
        List<Vec3> Points = new ArrayList<>();
        for (double Height = 7.0; Height < LintelBottom; Height += 13.0) {
            Points.add(new Vec3(PixelsWide / 2, Height, PixelsDeep / 2));
        }
        return Collections.unmodifiableList(Points);
    }

    public static Direction right(Direction Front) {
        return Front.getClockWise();
    }

    public static Direction away(Direction Front) {
        return Front.getOpposite();
    }

    public static float yaw(Direction Front) {
        return 180.0f - Front.toYRot();
    }

    public static double pivotAcross() {
        return (PixelsWide / 2 - OriginCentreX) / 16.0;
    }

    public static double pivotAlong() {
        return (PixelsDeep / 2 - OriginCentreZ) / 16.0;
    }

    public static Vec3 pivot(BlockPos Origin, Direction Front) {
        Direction Right = right(Front);
        Direction Away = away(Front);
        return new Vec3(
                Origin.getX() + 0.5 + Right.getStepX() * pivotAcross() + Away.getStepX() * pivotAlong(),
                Origin.getY(),
                Origin.getZ() + 0.5 + Right.getStepZ() * pivotAcross() + Away.getStepZ() * pivotAlong());
    }

    public static Vec3 toWorld(BlockPos Origin, Direction Front, Vec3 Local) {
        Vec3 Pivot = pivot(Origin, Front);
        Direction Right = right(Front);
        Direction Away = away(Front);
        double Across = (Local.x - PixelsWide / 2) / 16.0;
        double Along = (Local.z - PixelsDeep / 2) / 16.0;
        return new Vec3(
                Pivot.x + Right.getStepX() * Across + Away.getStepX() * Along,
                Pivot.y + Local.y / 16.0,
                Pivot.z + Right.getStepZ() * Across + Away.getStepZ() * Along);
    }

    public static Vec3 toLocal(BlockPos Origin, Direction Front, Vec3 World) {
        Vec3 Relative = World.subtract(pivot(Origin, Front));
        Direction Right = right(Front);
        Direction Away = away(Front);
        return new Vec3(
                (Relative.x * Right.getStepX() + Relative.z * Right.getStepZ()) * 16.0 + PixelsWide / 2,
                Relative.y * 16.0,
                (Relative.x * Away.getStepX() + Relative.z * Away.getStepZ()) * 16.0 + PixelsDeep / 2);
    }

    public static AABB toWorld(BlockPos Origin, Direction Front, AABB Local) {
        return new AABB(toWorld(Origin, Front, new Vec3(Local.minX, Local.minY, Local.minZ)), toWorld(Origin, Front, new Vec3(Local.maxX, Local.maxY, Local.maxZ)));
    }

    public static AABB strikeZone(BlockPos Origin, Direction Front) {
        return toWorld(Origin, Front, Opening).deflate(StrikeTolerance);
    }

    public static AABB corridorZone(BlockPos Origin, Direction Front, double Margin, double Reach) {
        AABB Volume = toWorld(Origin, Front, Cells);
        double Along = Reach - PixelsDeep / 32.0;
        return Front.getAxis() == Direction.Axis.Z ? Volume.inflate(Margin, Margin, Along) : Volume.inflate(Along, Margin, Margin);
    }

    public static boolean struck(AABB Zone, AABB Body) {
        return Body.intersects(Zone);
    }
}
