package net.rai.scp_unleashed.scp372.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp372.Scp372Glimpse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Scp372Spots {
    public static final double MinDistance = 4.0;
    public static final double CloseDistance = 2.0;
    public static final double MaxDistance = 16.0;
    public static final double MinAngle = 60.0;
    public static final double MaxAngle = 85.0;
    public static final double EdgeInner = 0.8;
    public static final double EdgeOuter = 0.97;
    public static final double CornerInner = 0.72;
    public static final double CornerOuter = 0.94;
    public static final double EdgeLow = -0.8;
    public static final double EdgeHigh = 0.55;
    public static final double CornerLow = 0.62;
    public static final double CornerHigh = 0.9;
    public static final int Rays = 14;
    public static final double BodyHalf = 0.84;
    public static final double OutShift = 0.48;
    public static final double GainReach = 0.5;
    public static final double GainScale = 10.0;
    public static final double ProfileWeight = 0.3;
    public static final double DistanceWeight = 1.5;
    public static final double Jitter = 0.08;

    public enum Side {
        Left(-1, false),
        Right(1, false),
        TopLeft(-1, true),
        TopRight(1, true);

        public final int Horizontal;
        public final boolean Corner;

        Side(int Horizontal, boolean Corner) {
            this.Horizontal = Horizontal;
            this.Corner = Corner;
        }
    }

    public record Spot(Vec3 Point, Vec3 Normal, Vec3 Heading, double Angle, double Distance, Side Edge) {
    }

    private Scp372Spots() {
    }

    public static List<Side> order(RandomSource Random) {
        List<Side> Sides = new ArrayList<>(List.of(Side.Left, Side.Right, Side.TopLeft, Side.TopRight));
        for (int Index = Sides.size() - 1; Index > 0; Index--) {
            int Other = Random.nextInt(Index + 1);
            Side Held = Sides.get(Index);
            Sides.set(Index, Sides.get(Other));
            Sides.set(Other, Held);
        }
        if (Sides.get(0).Corner && Random.nextInt(3) != 0) {
            for (int Index = 1; Index < Sides.size(); Index++) {
                if (!Sides.get(Index).Corner) {
                    Side Held = Sides.get(0);
                    Sides.set(0, Sides.get(Index));
                    Sides.set(Index, Held);
                    break;
                }
            }
        }
        return Sides;
    }

    public static Optional<Spot> find(Level World, Player Viewer, RandomSource Random, double FovDegrees, double Aspect) {
        Vec3 Eye = Viewer.getEyePosition();
        Vec3 Forward = Viewer.getViewVector(1.0f).normalize();
        Vec3 Right = Forward.cross(new Vec3(0.0, 1.0, 0.0));
        if (Right.lengthSqr() < 1.0E-6) {
            float Yaw = Viewer.getYRot() * Mth.DEG_TO_RAD;
            Right = new Vec3(-Mth.cos(Yaw), 0.0, -Mth.sin(Yaw));
        }
        Right = Right.normalize();
        Vec3 Up = Right.cross(Forward).normalize();
        double TanV = Math.tan(Math.toRadians(Mth.clamp(FovDegrees, 30.0, 150.0)) / 2.0);
        double TanH = TanV * Mth.clamp(Aspect, 0.5, 4.0);
        double Wanted = Math.tan(Math.toRadians(MinAngle));
        double EdgeFloor = Mth.clamp(Wanted / TanH, EdgeInner, EdgeOuter);
        double CornerFloor = Mth.clamp(Wanted / Math.sqrt(TanH * TanH + TanV * TanV * CornerLow * CornerLow), CornerInner, CornerOuter);
        for (double Nearest : new double[]{MinDistance, CloseDistance}) {
            for (Side Edge : order(Random)) {
                for (int Index = 0; Index < Rays; Index++) {
                    Vec3 Ray = sample(Edge, Random, Forward, Right, Up, TanH, TanV, EdgeFloor, CornerFloor);
                    Optional<Spot> Found = probe(World, Viewer, Eye, Forward, Ray, Edge, Random, Nearest);
                    if (Found.isPresent()) {
                        return Found;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Vec3 sample(Side Edge, RandomSource Random, Vec3 Forward, Vec3 Right, Vec3 Up, double TanH, double TanV, double EdgeFloor, double CornerFloor) {
        double Across;
        double Height;
        if (Edge.Corner) {
            Across = Edge.Horizontal * Mth.lerp(Random.nextDouble(), CornerFloor, CornerOuter);
            Height = Mth.lerp(Random.nextDouble(), CornerLow, CornerHigh);
        } else {
            Across = Edge.Horizontal * Mth.lerp(Random.nextDouble(), EdgeFloor, EdgeOuter);
            Height = Mth.lerp(Random.nextDouble(), EdgeLow, EdgeHigh);
        }
        double OffX = Across * TanH;
        double OffY = Height * TanV;
        double Spread = Math.sqrt(OffX * OffX + OffY * OffY);
        double Limit = Math.tan(Math.toRadians(MaxAngle));
        if (Spread > Limit) {
            OffX *= Limit / Spread;
            OffY *= Limit / Spread;
        }
        return Forward.add(Right.scale(OffX)).add(Up.scale(OffY)).normalize();
    }

    private static Optional<Spot> probe(Level World, Player Viewer, Vec3 Eye, Vec3 Forward, Vec3 Ray, Side Edge, RandomSource Random, double Nearest) {
        BlockHitResult Hit = World.clip(new ClipContext(Eye, Eye.add(Ray.scale(MaxDistance)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Viewer));
        if (Hit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        double Distance = Hit.getLocation().distanceTo(Eye);
        if (Distance < Nearest || Distance > MaxDistance) {
            return Optional.empty();
        }
        Direction Face = Hit.getDirection();
        BlockPos Behind = Hit.getBlockPos();
        if (!supports(World, Behind, Face)) {
            return Optional.empty();
        }
        Vec3 Normal = Vec3.atLowerCornerOf(Face.getNormal());
        Vec3 Point = Hit.getLocation();
        Optional<Vec3> Heading = heading(World, Point, Face, Normal, Ray, Eye, Forward, Random);
        if (Heading.isEmpty()) {
            return Optional.empty();
        }
        Vec3 Placed = Point.add(Heading.get().scale(OutShift));
        double Angle = angle(Placed.add(Normal.scale(Scp372Glimpse.CentreLift)), Eye, Forward);
        return Optional.of(new Spot(Placed, Normal, Heading.get(), Angle, Placed.distanceTo(Eye), Edge));
    }

    public static double angle(Vec3 Point, Vec3 Eye, Vec3 Forward) {
        Vec3 Toward = Point.subtract(Eye).normalize();
        return Math.toDegrees(Math.acos(Mth.clamp(Toward.dot(Forward), -1.0, 1.0)));
    }

    public static boolean supports(Level World, BlockPos Behind, Direction Face) {
        BlockPos Front = Behind.relative(Face);
        return World.getBlockState(Behind).isFaceSturdy(World, Behind, Face) && World.getBlockState(Front).getCollisionShape(World, Front).isEmpty();
    }

    private static Optional<Vec3> heading(Level World, Vec3 Point, Direction Face, Vec3 Normal, Vec3 Ray, Vec3 Eye, Vec3 Forward, RandomSource Random) {
        Vec3 First = Face.getAxis() == Direction.Axis.Y ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 Second = Normal.cross(First).normalize();
        double Base = angle(Point, Eye, Forward);
        double Far = Point.distanceTo(Eye);
        Vec3 Best = null;
        double Score = -Double.MAX_VALUE;
        for (int Step = 0; Step < 8; Step++) {
            double Turn = Step * Math.PI / 4.0;
            Vec3 Option = First.scale(Math.cos(Turn)).add(Second.scale(Math.sin(Turn))).normalize();
            if (!clings(World, Point.add(Option.scale(OutShift)), Normal, Option)) {
                continue;
            }
            Vec3 Ahead = Point.add(Option.scale(GainReach));
            double Gain = (angle(Ahead, Eye, Forward) - Base) / GainScale;
            double Away = (Ahead.distanceTo(Eye) - Far) / GainReach;
            double Value = Gain + DistanceWeight * Math.min(0.0, Away) + ProfileWeight * (1.0 - Math.abs(Option.dot(Ray))) + Jitter * Random.nextDouble();
            if (Value > Score) {
                Score = Value;
                Best = Option;
            }
        }
        return Optional.ofNullable(Best);
    }

    public static boolean clings(Level World, Vec3 Point, Vec3 Normal, Vec3 Heading) {
        for (double Along = -BodyHalf; Along <= BodyHalf + 1.0E-6; Along += BodyHalf / 2.0) {
            Vec3 Sample = Point.add(Heading.scale(Along));
            BlockPos Behind = BlockPos.containing(Sample.subtract(Normal.scale(0.5)));
            Direction Face = Direction.getNearest(Normal.x, Normal.y, Normal.z);
            if (!supports(World, Behind, Face)) {
                return false;
            }
        }
        return true;
    }
}
