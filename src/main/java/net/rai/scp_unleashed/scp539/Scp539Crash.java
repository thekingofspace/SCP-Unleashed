package net.rai.scp_unleashed.scp539;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Scp539Crash {
    public static final double Reach = 36.0;
    public static final double ShortestReach = 4.0;
    public static final double ReachStep = 2.0;
    public static final double DiveRise = 18.0;
    public static final double DiveBend = 0.35;
    public static final double WallRise = 0.5;
    public static final double WallStandoff = 1.5;
    public static final double WallHeight = 1.0;
    public static final double WallBlastOut = 0.4;
    public static final double FloorBlastUp = 0.5;
    public static final int GroundSearch = 4;
    public static final int ApproachRoom = 2;

    public record Route(boolean Wall, Vec3 Start, Vec3 Control, Vec3 End, Vec3 Blast, Vec3 Normal) {
    }

    private Scp539Crash() {
    }

    public static boolean solid(BlockGetter World, BlockPos Pos) {
        return !World.getBlockState(Pos).getCollisionShape(World, Pos).isEmpty();
    }

    public static boolean wallAt(BlockGetter World, BlockPos Cell, Direction Side) {
        BlockPos Face = Cell.relative(Side);
        if (!solid(World, Face) || !solid(World, Face.above())) {
            return false;
        }
        for (int Step = 0; Step <= ApproachRoom; Step++) {
            BlockPos Open = Cell.relative(Side.getOpposite(), Step);
            if (solid(World, Open) || solid(World, Open.above())) {
                return false;
            }
        }
        return true;
    }

    public static double groundBelow(BlockGetter World, Vec3 Point) {
        BlockPos Cell = BlockPos.containing(Point);
        for (int Down = 0; Down <= GroundSearch; Down++) {
            BlockPos Below = Cell.below(Down + 1);
            if (solid(World, Below)) {
                return Below.getY() + 1.0;
            }
        }
        return Point.y;
    }

    public static List<Direction> sidesFacing(Vec3 Heading) {
        List<Direction> Sides = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        Sides.sort(Comparator.comparingDouble(Side -> -(Side.getStepX() * Heading.x + Side.getStepZ() * Heading.z)));
        return Sides;
    }

    public static Route plan(ServerLevel World, Vec3 Landing, @Nullable Direction Face, @Nullable BlockPos Struck, Vec3 Heading) {
        Vec3 Flat = Scp539Stone.flat(Heading);
        if (Face != null && Face.getAxis().isHorizontal() && Struck != null && solid(World, Struck)) {
            Vec3 Normal = new Vec3(Face.getStepX(), 0.0, Face.getStepZ());
            double Height = Math.max(Landing.y, groundBelow(World, Landing) + WallHeight);
            Vec3 Contact = new Vec3(Landing.x, Height, Landing.z);
            return wall(World, Contact, Normal);
        }
        BlockPos Cell = BlockPos.containing(Landing);
        for (Direction Side : sidesFacing(Flat)) {
            if (wallAt(World, Cell, Side)) {
                Vec3 Normal = new Vec3(-Side.getStepX(), 0.0, -Side.getStepZ());
                Vec3 Contact = new Vec3(Cell.getX() + 0.5 + Side.getStepX() * 0.5, Cell.getY() + WallHeight, Cell.getZ() + 0.5 + Side.getStepZ() * 0.5);
                return wall(World, Contact, Normal);
            }
        }
        return floor(World, Landing, Flat);
    }

    public static Route wall(ServerLevel World, Vec3 Contact, Vec3 Normal) {
        Vec3 End = Contact.add(Normal.scale(WallStandoff)).add(0.0, -WallHeight * 0.5, 0.0);
        Vec3 Blast = Contact.add(Normal.scale(WallBlastOut));
        double Distance = reach(World, Distance0 -> End.add(Normal.scale(Distance0)).add(0.0, WallRise, 0.0));
        Vec3 Start = clampHeight(World, End.add(Normal.scale(Distance)).add(0.0, WallRise, 0.0));
        Vec3 Control = Start.add(End).scale(0.5);
        return new Route(true, Start, Control, End, Blast, Normal);
    }

    public static Route floor(ServerLevel World, Vec3 Landing, Vec3 Heading) {
        Vec3 End = Landing;
        Vec3 Blast = Landing.add(0.0, FloorBlastUp, 0.0);
        double Distance = reach(World, Distance0 -> End.add(Heading.scale(Distance0)).add(0.0, DiveRise, 0.0));
        Vec3 Start = clampHeight(World, End.add(Heading.scale(Distance)).add(0.0, DiveRise, 0.0));
        Vec3 Control = clampHeight(World, End.add(Heading.scale(Distance * DiveBend)).add(0.0, DiveRise, 0.0));
        return new Route(false, Start, Control, End, Blast, Heading.scale(-1.0));
    }

    private interface Spot {
        Vec3 at(double Distance);
    }

    private static double reach(ServerLevel World, Spot Where) {
        for (double Distance = Reach; Distance >= ShortestReach; Distance -= ReachStep) {
            if (World.isPositionEntityTicking(BlockPos.containing(Where.at(Distance)))) {
                return Distance;
            }
        }
        return ShortestReach;
    }

    private static Vec3 clampHeight(ServerLevel World, Vec3 Point) {
        return new Vec3(Point.x, Math.min(Point.y, World.getMaxBuildHeight() - 2.0), Point.z);
    }
}
