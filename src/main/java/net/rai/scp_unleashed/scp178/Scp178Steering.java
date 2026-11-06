package net.rai.scp_unleashed.scp178;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class Scp178Steering {
    public static final double StepUp = 1.1;
    public static final double DropReach = 48.0;
    public static final double Gravity = 0.08;
    public static final double Terminal = 2.5;
    public static final double Settle = 0.05;
    public static final double SpawnUp = 12.0;
    public static final double SpawnDown = 24.0;
    public static final int SpawnTries = 24;
    public static final double LostBelow = 32.0;
    public static final int LostTicks = 40;
    public static final double DriftClimb = 0.15;
    public static final double Agility = 0.3;
    public static final double DashAgility = 0.6;
    public static final double CalmSpeed = 0.07;
    public static final double FollowSpeed = 0.13;
    public static final double FastFollowSpeed = 0.22;
    public static final double FollowSlack = 6.0;
    public static final double FastFollowBeyond = 16.0;
    public static final double Sector = 0.8;
    public static final int HesitateMin = 20;
    public static final int HesitateMax = 60;
    public static final int ArrivalPauseMin = 10;
    public static final int ArrivalPauseMax = 40;
    public static final int PauseMin = 40;
    public static final int PauseMax = 140;
    public static final int BlockedPause = 10;
    public static final int StuckTicks = 40;
    public static final int WanderTries = 6;
    public static final double WanderRise = 6.0;
    public static final float PauseChance = 0.7f;
    public static final double SwingMin = 0.25;
    public static final double SwingMax = 0.7;
    public static final double Arrive = 0.6;
    public static final double SyncGain = 0.05;
    public static final double CatchUp = 0.15;
    public static final double MaxRun = 0.4;
    public static final double Personal = 1.2;
    public static final double RelocateError = 24.0;
    public static final double DashSpeed = 0.7;
    public static final double DashStop = 1.05;
    public static final double BlinkBeyond = 6.0;
    public static final double BlinkTo = 2.6;
    public static final double Moving = 0.02;

    public interface Ground {
        double surface(double X, double Y, double Z);

        boolean loaded(double X, double Z);

        boolean solid(double X, double Y, double Z);

        boolean wet(double X, double Y, double Z);
    }

    private double X;
    private double Y;
    private double Z;
    private double Vx;
    private double Vy;
    private double Vz;
    private double Travelled;
    private boolean Hostile;
    private boolean Drifting;
    private boolean Airborne;
    private boolean Dashing;
    private boolean Holding;
    private int Pause;
    private int Hesitate = -1;
    private int Stuck;
    private int Groundless;
    private boolean Following;
    private double Anchor = Double.NaN;
    @Nullable
    private Vec3 Wander;

    public Scp178Steering(double X, double Y, double Z) {
        this.X = X;
        this.Y = Y;
        this.Z = Z;
    }

    public static Ground groundOf(LevelReader Level) {
        return new Ground() {
            @Override
            public double surface(double PointX, double PointY, double PointZ) {
                int Top = Mth.floor(PointY + StepUp);
                int Bottom = Math.max(Level.getMinBuildHeight(), Mth.floor(PointY - DropReach));
                for (int Height = Top; Height >= Bottom; Height--) {
                    BlockPos Feet = BlockPos.containing(PointX, Height, PointZ);
                    BlockPos Below = Feet.below();
                    VoxelShape Floor = Level.getBlockState(Below).getCollisionShape(Level, Below);
                    if (Floor.isEmpty() || !clear(Feet) || !clear(Feet.above())) {
                        continue;
                    }
                    double Top0 = Below.getY() + Floor.max(Direction.Axis.Y);
                    if (Top0 <= PointY + StepUp) {
                        return Top0;
                    }
                }
                return Double.NaN;
            }

            private boolean clear(BlockPos Pos) {
                return Level.getBlockState(Pos).getCollisionShape(Level, Pos).isEmpty();
            }

            @Override
            public boolean loaded(double PointX, double PointZ) {
                return Level.hasChunk(SectionPos.blockToSectionCoord(Mth.floor(PointX)), SectionPos.blockToSectionCoord(Mth.floor(PointZ)));
            }

            @Override
            public boolean solid(double PointX, double PointY, double PointZ) {
                return !clear(BlockPos.containing(PointX, PointY, PointZ));
            }

            @Override
            public boolean wet(double PointX, double PointY, double PointZ) {
                return !Level.getFluidState(BlockPos.containing(PointX, PointY, PointZ)).isEmpty();
            }
        };
    }

    @Nullable
    public static Vec3 findSpot(Vec3 Player, double Distance, double Bearing, Ground Floor, RandomSource Random) {
        for (int Attempt = 0; Attempt < SpawnTries; Attempt++) {
            double Angle = Attempt == 0 ? Bearing : Bearing + (Random.nextDouble() - 0.5) * Math.min(Mth.TWO_PI, Sector * (1 + Attempt / 4));
            double Reach = Attempt == 0 ? Distance : Math.max(4.0, Distance + (Random.nextDouble() - 0.5) * 8.0);
            double PointX = Player.x + Math.cos(Angle) * Reach;
            double PointZ = Player.z + Math.sin(Angle) * Reach;
            Vec3 Found = standable(PointX, Player.y, PointZ, Floor);
            if (Found != null) {
                return Found;
            }
        }
        return null;
    }

    @Nullable
    public static Vec3 standable(double PointX, double Near, double PointZ, Ground Floor) {
        if (!Floor.loaded(PointX, PointZ)) {
            return null;
        }
        double Surface = Floor.surface(PointX, Near + SpawnUp, PointZ);
        if (Double.isNaN(Surface) || Surface < Near - SpawnDown || Floor.wet(PointX, Surface + 0.2, PointZ)) {
            return null;
        }
        return new Vec3(PointX, Surface, PointZ);
    }

    public Vec3 position() {
        return new Vec3(X, Y, Z);
    }

    public double travelled() {
        return Travelled;
    }

    public boolean drifting() {
        return Drifting;
    }

    public boolean airborne() {
        return Airborne;
    }

    public boolean hostile() {
        return Hostile;
    }

    public boolean moving() {
        return Travelled > Moving;
    }

    public boolean wandering() {
        return !Hostile && (Wander != null || Pause > 0 || Hesitate >= 0);
    }

    public double headingYaw() {
        return Mth.atan2(Vz, Vx) * Mth.RAD_TO_DEG - 90.0;
    }

    public void setHostile(boolean Value) {
        if (Hostile == Value) {
            return;
        }
        Hostile = Value;
        Wander = null;
        Hesitate = -1;
        Following = false;
        Pause = Value ? 0 : ArrivalPauseMin;
    }

    public void setAnchor(double Bearing) {
        Anchor = Bearing;
    }

    public void hold(boolean Value) {
        Holding = Value;
    }

    public double distanceTo(Vec3 Player) {
        return Math.hypot(Player.x - X, Player.z - Z);
    }

    public boolean needsRelocation(Vec3 Player, double ServerDistance) {
        return Hostile && !Dashing && Math.abs(distanceTo(Player) - ServerDistance) > RelocateError;
    }

    public boolean lost(Vec3 Player, Ground Floor) {
        return !Floor.loaded(X, Z) || Groundless > LostTicks || Y < Player.y - LostBelow;
    }

    public void placeAt(Vec3 Spot, RandomSource Random) {
        X = Spot.x;
        Y = Spot.y;
        Z = Spot.z;
        Vx = 0.0;
        Vy = 0.0;
        Vz = 0.0;
        Wander = null;
        Following = false;
        Hesitate = -1;
        Airborne = false;
        Drifting = false;
        Groundless = 0;
        Stuck = 0;
        Travelled = 0.0;
        Pause = ArrivalPauseMin + Random.nextInt(ArrivalPauseMax - ArrivalPauseMin + 1);
    }

    public boolean place(Vec3 Player, double Distance, double Bearing, Ground Floor, RandomSource Random) {
        Vec3 Spot = findSpot(Player, Distance, Bearing, Floor, Random);
        if (Spot == null) {
            return false;
        }
        placeAt(Spot, Random);
        return true;
    }

    public boolean relocate(Vec3 Player, double Distance, Ground Floor, RandomSource Random) {
        return place(Player, Distance, Math.atan2(Z - Player.z, X - Player.x), Floor, Random);
    }

    public boolean lunge(Vec3 Player, double ServerDistance, Ground Floor, RandomSource Random) {
        Dashing = true;
        Holding = false;
        if (distanceTo(Player) > BlinkBeyond && ServerDistance <= Scp178Watchers.StrikeReach) {
            return place(Player, BlinkTo, Math.atan2(Z - Player.z, X - Player.x), Floor, Random);
        }
        return false;
    }

    public void endLunge() {
        Dashing = false;
    }

    public void tick(Vec3 Player, double ServerDistance, Ground Floor, RandomSource Random) {
        double Dx = Player.x - X;
        double Dz = Player.z - Z;
        double Distance = Math.hypot(Dx, Dz);
        double Speed = 0.0;
        double HeadX = Distance > 1.0e-4 ? Dx / Distance : 0.0;
        double HeadZ = Distance > 1.0e-4 ? Dz / Distance : 0.0;
        double Grip = Agility;
        if (Dashing) {
            Speed = Distance > DashStop ? Math.min(DashSpeed, Distance - DashStop) : 0.0;
            Grip = DashAgility;
        } else if (Holding) {
            Speed = 0.0;
        } else if (Hostile) {
            double Rate = Scp178Watchers.HuntSpeed;
            Speed = Mth.clamp(Rate + Mth.clamp((Distance - ServerDistance) * SyncGain, -Rate, CatchUp), 0.0, MaxRun);
            if (Distance < Personal) {
                Speed = 0.0;
            }
        } else {
            Vec3 Target = calm(Player, Distance, ServerDistance, Floor, Random);
            if (Target != null) {
                double Tx = Target.x - X;
                double Tz = Target.z - Z;
                double Gap = Math.hypot(Tx, Tz);
                if (Gap < Arrive) {
                    Wander = null;
                    Following = false;
                    Stuck = 0;
                    Pause = Random.nextFloat() < PauseChance ? PauseMin + Random.nextInt(PauseMax - PauseMin + 1) : BlockedPause;
                } else {
                    HeadX = Tx / Gap;
                    HeadZ = Tz / Gap;
                    Speed = Following ? (Distance > ServerDistance + FastFollowBeyond ? FastFollowSpeed : FollowSpeed) : CalmSpeed;
                    Speed = Math.min(Speed, Gap);
                }
            }
        }
        Vx += (HeadX * Speed - Vx) * Grip;
        Vz += (HeadZ * Speed - Vz) * Grip;
        if (!Hostile && !Dashing && Distance > 1.0e-4) {
            double Outward = -(Vx * Dx + Vz * Dz) / Distance;
            if (Outward > 0.0) {
                Vx += Dx / Distance * Outward;
                Vz += Dz / Distance * Outward;
            }
        }
        double BeforeX = X;
        double BeforeZ = Z;
        stepAcross(Floor);
        fall(Player, Floor);
        Travelled = Math.hypot(X - BeforeX, Z - BeforeZ);
        if (!Hostile && Wander != null) {
            Stuck = Travelled < 0.01 ? Stuck + 1 : 0;
            if (Stuck > StuckTicks) {
                Wander = null;
                Following = false;
                Stuck = 0;
                Pause = BlockedPause;
            }
        }
    }

    private void stepAcross(Ground Floor) {
        if (Vx * Vx + Vz * Vz < 1.0e-8) {
            return;
        }
        double Nx = X + Vx;
        double Nz = Z + Vz;
        boolean Wall = Floor.solid(Nx, Y + 0.5, Nz) || Floor.solid(Nx, Y + 1.5, Nz);
        if (Wall) {
            double Up = Floor.surface(Nx, Y, Nz);
            boolean Step = !Double.isNaN(Up) && Up > Y + 1.0e-3 && Up <= Y + StepUp;
            if (Step && !Airborne) {
                X = Nx;
                Z = Nz;
                Y = Up;
                return;
            }
            if (Hostile || Dashing) {
                X = Nx;
                Z = Nz;
                return;
            }
            Vx = 0.0;
            Vz = 0.0;
            Wander = null;
            Following = false;
            Pause = BlockedPause;
            return;
        }
        if (!Hostile && !Dashing && !Airborne) {
            double Ahead = Floor.surface(Nx, Y, Nz);
            if (!Double.isNaN(Ahead) && Floor.wet(Nx, Ahead + 0.2, Nz)) {
                Vx = 0.0;
                Vz = 0.0;
                Wander = null;
                Following = false;
                Pause = BlockedPause;
                return;
            }
        }
        X = Nx;
        Z = Nz;
    }

    private void fall(Vec3 Player, Ground Floor) {
        boolean Inside = Floor.solid(X, Y + 0.5, Z) || Floor.solid(X, Y + 1.5, Z);
        if (Inside) {
            Drifting = true;
            Airborne = false;
            Vy = 0.0;
            Groundless = 0;
            Y += Mth.clamp(Player.y - Y, -DriftClimb, DriftClimb);
            return;
        }
        Drifting = false;
        double Surface = Floor.surface(X, Y, Z);
        if (Double.isNaN(Surface)) {
            Groundless++;
            Airborne = true;
            Vy = Math.max(Vy - Gravity, -Terminal);
            Y += Vy;
            return;
        }
        Groundless = 0;
        if (Surface >= Y - Settle) {
            Y = Math.max(Y, Surface);
            Vy = 0.0;
            Airborne = false;
            return;
        }
        Airborne = true;
        Vy = Math.max(Vy - Gravity, -Terminal);
        Y += Vy;
        if (Y <= Surface) {
            Y = Surface;
            Vy = 0.0;
            Airborne = false;
        }
    }

    @Nullable
    private Vec3 calm(Vec3 Player, double Distance, double Goal, Ground Floor, RandomSource Random) {
        if (Wander != null) {
            return Wander;
        }
        if (Pause > 0) {
            Pause--;
            return null;
        }
        if (Airborne) {
            return null;
        }
        double Bearing = Math.atan2(Z - Player.z, X - Player.x);
        if (Distance > Goal + FollowSlack) {
            if (Hesitate < 0) {
                Hesitate = HesitateMin + Random.nextInt(HesitateMax - HesitateMin + 1);
            }
            if (--Hesitate > 0) {
                return null;
            }
            Hesitate = -1;
            for (int Attempt = 0; Attempt < WanderTries; Attempt++) {
                double Angle = Bearing + (Random.nextDouble() - 0.5) * 0.6;
                Vec3 Spot = standable(Player.x + Math.cos(Angle) * Goal, Player.y, Player.z + Math.sin(Angle) * Goal, Floor);
                if (Spot != null) {
                    Following = true;
                    Wander = Spot;
                    return Wander;
                }
            }
            Following = true;
            Wander = new Vec3(Player.x + Math.cos(Bearing) * Goal, Player.y, Player.z + Math.sin(Bearing) * Goal);
            return Wander;
        }
        double Radius = Math.min(Goal, Distance);
        for (int Attempt = 0; Attempt < WanderTries; Attempt++) {
            double Swing = (SwingMin + Random.nextDouble() * (SwingMax - SwingMin)) * (Random.nextBoolean() ? 1.0 : -1.0);
            double Angle = Bearing + Swing;
            if (!Double.isNaN(Anchor) && Math.abs(Mth.wrapDegrees((Angle - Anchor) * Mth.RAD_TO_DEG)) > Sector * Mth.RAD_TO_DEG) {
                Angle = Bearing - Swing;
            }
            Vec3 Spot = standable(Player.x + Math.cos(Angle) * Radius, Y, Player.z + Math.sin(Angle) * Radius, Floor);
            if (Spot != null && Math.abs(Spot.y - Y) <= WanderRise) {
                Following = false;
                Wander = Spot;
                return Wander;
            }
        }
        Pause = BlockedPause;
        return null;
    }
}
