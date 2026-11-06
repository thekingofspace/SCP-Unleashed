package net.rai.scp_unleashed.scp343;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

public final class Scp343Walls {
    public static final int Attempts = 64;
    public static final int Vertical = 6;

    private Scp343Walls() {
    }

    public record Emergence(BlockPos Wall, BlockPos Stand) {
        public boolean hasWall() {
            return !Wall.equals(Stand);
        }

        public float yaw() {
            if (!hasWall()) {
                return 0.0f;
            }
            return (float) (Mth.atan2(Stand.getZ() - Wall.getZ(), Stand.getX() - Wall.getX()) * Mth.RAD_TO_DEG) - 90.0f;
        }

        public Vec3 from() {
            return Vec3.atBottomCenterOf(Wall);
        }

        public Vec3 to() {
            return Vec3.atBottomCenterOf(Stand);
        }
    }

    public static boolean solid(BlockGetter World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return !State.getCollisionShape(World, Pos).isEmpty() && State.isCollisionShapeFullBlock(World, Pos);
    }

    public static boolean isWall(Level World, BlockPos Pos) {
        return World.isLoaded(Pos) && solid(World, Pos) && solid(World, Pos.above());
    }

    public static boolean isStand(Level World, BlockPos Pos) {
        if (!World.isLoaded(Pos) || World.isOutsideBuildHeight(Pos) || World.isOutsideBuildHeight(Pos.above())) {
            return false;
        }
        BlockState At = World.getBlockState(Pos);
        BlockState Above = World.getBlockState(Pos.above());
        if (!At.getCollisionShape(World, Pos).isEmpty() || !Above.getCollisionShape(World, Pos.above()).isEmpty()) {
            return false;
        }
        if (!At.getFluidState().isEmpty() || !Above.getFluidState().isEmpty()) {
            return false;
        }
        BlockPos Below = Pos.below();
        return World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP)
                && World.noCollision(Scp343Registry.God.get().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(Pos)).deflate(0.02));
    }

    @Nullable
    public static BlockPos wallBeside(Level World, BlockPos Stand, RandomSource Random) {
        int Start = Random.nextInt(4);
        for (int Step = 0; Step < 4; Step++) {
            Direction Side = Direction.from2DDataValue((Start + Step) % 4);
            BlockPos Candidate = Stand.relative(Side);
            if (isWall(World, Candidate)) {
                return Candidate;
            }
        }
        return null;
    }

    public static final Predicate<BlockPos> Anywhere = Pos -> true;

    public static boolean ticking(ServerLevel World, BlockPos Pos) {
        return World.isLoaded(Pos) && World.isPositionEntityTicking(Pos);
    }

    public static boolean usable(ServerLevel World, BlockPos Pos, Predicate<BlockPos> Allowed) {
        return ticking(World, Pos) && Allowed.test(Pos);
    }

    public static boolean usable(ServerLevel World, Emergence Spot, Predicate<BlockPos> Allowed) {
        return usable(World, Spot.Stand(), Allowed) && usable(World, Spot.Wall(), Allowed);
    }

    public static Optional<Emergence> near(ServerLevel World, BlockPos Centre, int Min, int Max, RandomSource Random, boolean WallsOnly, @Nullable Vec3 Avoid, double AvoidDistance) {
        return near(World, Centre, Min, Max, Random, WallsOnly, Avoid, AvoidDistance, Anywhere);
    }

    public static Optional<Emergence> near(ServerLevel World, BlockPos Centre, int Min, int Max, RandomSource Random, boolean WallsOnly, @Nullable Vec3 Avoid, double AvoidDistance,
                                           Predicate<BlockPos> Allowed) {
        Emergence Open = null;
        Emergence Close = null;
        Emergence Plain = null;
        int Low = Math.max(0, Math.min(Min, Max));
        int High = Math.max(Low + 1, Math.max(Min, Max));
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Radius = Low + Random.nextDouble() * (High - Low);
            BlockPos Column = Centre.offset(Mth.floor(Math.cos(Angle) * Radius), 0, Mth.floor(Math.sin(Angle) * Radius));
            if (!usable(World, Column, Allowed)) {
                continue;
            }
            for (int Step = 0; Step <= Vertical * 2; Step++) {
                int Dy = Step % 2 == 0 ? Step / 2 : -(Step + 1) / 2;
                BlockPos Stand = Column.above(Dy);
                if (!usable(World, Stand, Allowed) || !isStand(World, Stand)) {
                    continue;
                }
                BlockPos Wall = wallBeside(World, Stand, Random);
                if (Wall != null && !usable(World, Wall, Allowed)) {
                    Wall = null;
                }
                boolean Far = Avoid == null || Vec3.atBottomCenterOf(Stand).distanceTo(Avoid) >= AvoidDistance;
                if (Wall != null) {
                    Emergence Found = new Emergence(Wall, Stand);
                    if (Far) {
                        return Optional.of(Found);
                    }
                    if (Close == null) {
                        Close = Found;
                    }
                } else if (Open == null && Far) {
                    Open = new Emergence(Stand, Stand);
                } else if (Plain == null) {
                    Plain = new Emergence(Stand, Stand);
                }
                break;
            }
        }
        if (Close != null) {
            return Optional.of(Close);
        }
        if (WallsOnly) {
            return Optional.empty();
        }
        return Optional.ofNullable(Open != null ? Open : Plain);
    }

    public static Optional<Emergence> open(ServerLevel World, BlockPos Centre, int Min, int Max, RandomSource Random, @Nullable Vec3 Avoid, double AvoidDistance, int Tries) {
        return open(World, Centre, Min, Max, Random, Avoid, AvoidDistance, Tries, Anywhere);
    }

    public static Optional<Emergence> open(ServerLevel World, BlockPos Centre, int Min, int Max, RandomSource Random, @Nullable Vec3 Avoid, double AvoidDistance, int Tries,
                                           Predicate<BlockPos> Allowed) {
        Emergence Fallback = null;
        for (int Attempt = 0; Attempt < Tries; Attempt++) {
            Optional<Emergence> Spot = near(World, Centre, Min, Max, Random, false, Avoid, AvoidDistance, Allowed);
            if (Spot.isEmpty()) {
                continue;
            }
            if (Fallback == null) {
                Fallback = Spot.get();
            }
            if (!Scp343Enclosure.survey(World, Spot.get().Stand()).Enclosed()) {
                return Spot;
            }
        }
        return Optional.ofNullable(Fallback);
    }

    public static Optional<Emergence> around(ServerLevel World, BlockPos Stand, RandomSource Random) {
        return around(World, Stand, Random, Anywhere);
    }

    public static Optional<Emergence> around(ServerLevel World, BlockPos Stand, RandomSource Random, Predicate<BlockPos> Allowed) {
        if (!usable(World, Stand, Allowed) || !isStand(World, Stand)) {
            return Optional.empty();
        }
        BlockPos Wall = wallBeside(World, Stand, Random);
        if (Wall != null && !usable(World, Wall, Allowed)) {
            Wall = null;
        }
        return Optional.of(new Emergence(Wall == null ? Stand : Wall, Stand));
    }

    public static boolean roomFor(Level World, Vec3 Spot) {
        AABB Box = Scp343Registry.God.get().getDimensions().makeBoundingBox(Spot).deflate(0.02);
        return World.noCollision(Box);
    }
}
