package net.rai.scp_unleashed.mtf;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class MtfLandingZone {
    public static final int DefaultHover = 16;
    public static final int MaxHover = 22;
    public static final int ColumnRadius = 1;
    public static final int ClearRadius = 6;
    public static final int ClearMargin = 3;
    public static final double AnchorLateral = 25.0 / 16.0;
    public static final double AnchorUp = 44.0 / 16.0;
    public static final double AnchorForward = 13.0 / 16.0;
    public static final double StandOff = 0.3;
    public static final double MaxRope = 28.0;
    public static final double FarEntry = 144.0;
    public static final double NearEntry = 32.0;
    public static final double EntryStep = 16.0;
    public static final double PathSample = 8.0;
    public static final int Headings = 12;
    public static final double CruiseLift = 18.0;
    public static final double TerrainClearance = 12.0;
    public static final double OverheadLift = 44.0;

    public record Zone(BlockPos Ground, double HoverY, float Yaw) {
        public Vec3 hover() {
            return new Vec3(Ground.getX() + 0.5, HoverY, Ground.getZ() + 0.5);
        }
    }

    private MtfLandingZone() {
    }

    public static int surface(ServerLevel World, double X, double Z) {
        return World.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(X), Mth.floor(Z));
    }

    public static boolean hasSky(ServerLevel World, BlockPos Ground) {
        if (World.dimensionType().hasCeiling()) {
            return false;
        }
        for (int Dx = -ColumnRadius; Dx <= ColumnRadius; Dx++) {
            for (int Dz = -ColumnRadius; Dz <= ColumnRadius; Dz++) {
                if (World.getHeight(Heightmap.Types.MOTION_BLOCKING, Ground.getX() + Dx, Ground.getZ() + Dz) > Ground.getY()) {
                    return false;
                }
            }
        }
        return true;
    }

    public static Optional<Zone> find(ServerLevel World, Vec3 Feet, float PreferredYaw) {
        BlockPos Ground = BlockPos.containing(Feet);
        if (!hasSky(World, Ground)) {
            return Optional.empty();
        }
        int Highest = Ground.getY();
        for (int Dx = -ClearRadius; Dx <= ClearRadius; Dx++) {
            for (int Dz = -ClearRadius; Dz <= ClearRadius; Dz++) {
                Highest = Math.max(Highest, World.getHeight(Heightmap.Types.MOTION_BLOCKING, Ground.getX() + Dx, Ground.getZ() + Dz));
            }
        }
        double HoverY = Math.max(Ground.getY() + DefaultHover, Highest + ClearMargin);
        if (HoverY > Ground.getY() + MaxHover) {
            return Optional.empty();
        }
        Vec3 Centre = new Vec3(Ground.getX() + 0.5, HoverY, Ground.getZ() + 0.5);
        float Yaw = PreferredYaw;
        for (int Step = 0; Step < 8; Step++) {
            float Candidate = PreferredYaw + Step * 45.0f;
            if (ropesLand(World, Centre, Candidate, Ground.getY())) {
                Yaw = Candidate;
                break;
            }
        }
        return Optional.of(new Zone(Ground, HoverY, Mth.wrapDegrees(Yaw)));
    }

    private static boolean ropesLand(ServerLevel World, Vec3 Centre, float Yaw, int GroundY) {
        for (int Side = 0; Side < 2; Side++) {
            Vec3 Rope = anchor(Centre, Yaw, Side);
            int Top = surface(World, Rope.x, Rope.z);
            if (Top > GroundY + 1 || Top < GroundY - 6) {
                return false;
            }
        }
        return true;
    }

    public static Vec3 left(float Yaw) {
        float Radians = Yaw * Mth.DEG_TO_RAD;
        return new Vec3(Mth.cos(Radians), 0.0, Mth.sin(Radians));
    }

    public static Vec3 forward(float Yaw) {
        float Radians = Yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(Radians), 0.0, Mth.cos(Radians));
    }

    public static double sideSign(int Side) {
        return Side == 0 ? 1.0 : -1.0;
    }

    public static Vec3 anchor(Vec3 Centre, float Yaw, int Side) {
        Vec3 Lateral = left(Yaw).scale(AnchorLateral * sideSign(Side));
        Vec3 Ahead = forward(Yaw).scale(AnchorForward);
        return Centre.add(Lateral).add(Ahead).add(0.0, AnchorUp, 0.0);
    }

    public static Vec3 standPoint(Vec3 Centre, float Yaw, int Side) {
        Vec3 Rope = anchor(Centre, Yaw, Side);
        Vec3 Out = left(Yaw).scale(StandOff * sideSign(Side));
        return new Vec3(Rope.x + Out.x, Rope.y, Rope.z + Out.z);
    }

    public static float inwardYaw(float Yaw, int Side) {
        return Mth.wrapDegrees(Yaw + (Side == 0 ? 90.0f : -90.0f));
    }

    public static boolean pathTicking(ServerLevel World, Vec3 From, Vec3 To) {
        double Length = From.distanceTo(To);
        int Samples = Math.max(1, Mth.ceil(Length / PathSample));
        for (int Index = 0; Index <= Samples; Index++) {
            Vec3 Point = From.lerp(To, Index / (double) Samples);
            if (!World.isPositionEntityTicking(BlockPos.containing(Point.x, From.y, Point.z))) {
                return false;
            }
        }
        return true;
    }

    public static int terrainAlong(ServerLevel World, Vec3 From, Vec3 To) {
        double Length = From.distanceTo(To);
        int Samples = Math.max(1, Mth.ceil(Length / PathSample));
        int Highest = World.getMinBuildHeight();
        for (int Index = 0; Index <= Samples; Index++) {
            Vec3 Point = From.lerp(To, Index / (double) Samples);
            if (World.hasChunkAt(BlockPos.containing(Point.x, From.y, Point.z))) {
                Highest = Math.max(Highest, surface(World, Point.x, Point.z));
            }
        }
        return Highest;
    }

    public static Vec3 entry(ServerLevel World, Zone Target, RandomSource Random) {
        Vec3 Hover = Target.hover();
        float Start = Random.nextFloat() * 360.0f;
        for (double Distance = FarEntry; Distance >= NearEntry; Distance -= EntryStep) {
            for (int Index = 0; Index < Headings; Index++) {
                float Heading = Start + Index * (360.0f / Headings);
                Vec3 Out = forward(Heading).scale(Distance);
                Vec3 Point = new Vec3(Hover.x + Out.x, Hover.y, Hover.z + Out.z);
                if (pathTicking(World, Hover, Point)) {
                    double Cruise = Math.max(Hover.y + CruiseLift, terrainAlong(World, Hover, Point) + TerrainClearance);
                    return new Vec3(Point.x, Math.min(Cruise, World.getMaxBuildHeight() + 32.0), Point.z);
                }
            }
        }
        return new Vec3(Hover.x, Hover.y + OverheadLift, Hover.z);
    }
}
