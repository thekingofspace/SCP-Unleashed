package net.rai.scp_unleashed.scp017;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class Scp017Corners {
    public static final int Radius = 8;
    public static final int Rise = 3;
    public static final int PathChecks = 6;
    public static final double DeepInset = -0.2;
    public static final double ShallowInset = 0.0;
    public static final double FullHeightBonus = 4.0;
    public static final double DarkPenalty = 6.0;
    private static final Direction[] Clockwise = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public record Corner(BlockPos Pos, Direction First, Direction Second, boolean FullHeight, double Inset) {
        public Vec3 seat() {
            return new Vec3(Pos.getX() + 0.5 + (First.getStepX() + Second.getStepX()) * Inset, Pos.getY(),
                    Pos.getZ() + 0.5 + (First.getStepZ() + Second.getStepZ()) * Inset);
        }

        public float yaw() {
            double Dx = First.getStepX() + Second.getStepX();
            double Dz = First.getStepZ() + Second.getStepZ();
            return Mth.wrapDegrees((float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f);
        }
    }

    private record Candidate(Corner Found, double Score) {
    }

    private Scp017Corners() {
    }

    public static boolean standable(Level World, BlockPos Pos) {
        BlockPos Floor = Pos.below();
        BlockState Below = World.getBlockState(Floor);
        if (!Below.isFaceSturdy(World, Floor, Direction.UP)) {
            return false;
        }
        for (BlockPos Cell : new BlockPos[]{Pos, Pos.above()}) {
            BlockState State = World.getBlockState(Cell);
            if (!State.getCollisionShape(World, Cell).isEmpty() || !State.getFluidState().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean wall(Level World, BlockPos Pos, Direction Side) {
        BlockPos Beside = Pos.relative(Side);
        return World.getBlockState(Beside).isFaceSturdy(World, Beside, Side.getOpposite());
    }

    private static boolean open(Level World, BlockPos Pos) {
        for (BlockPos Cell : new BlockPos[]{Pos, Pos.above()}) {
            if (!World.getBlockState(Cell).getCollisionShape(World, Cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static Optional<Corner> at(Level World, BlockPos Pos) {
        if (!World.isLoaded(Pos) || !standable(World, Pos)) {
            return Optional.empty();
        }
        Corner Best = null;
        for (int Index = 0; Index < Clockwise.length; Index++) {
            Direction First = Clockwise[Index];
            Direction Second = Clockwise[(Index + 1) % Clockwise.length];
            if (!wall(World, Pos, First) || !wall(World, Pos, Second)) {
                continue;
            }
            boolean Full = wall(World, Pos.above(), First) && wall(World, Pos.above(), Second);
            if (Best == null || Full && !Best.FullHeight()) {
                boolean Roomy = open(World, Pos.relative(First.getOpposite())) && open(World, Pos.relative(Second.getOpposite()));
                Best = new Corner(Pos.immutable(), First, Second, Full, Roomy ? DeepInset : ShallowInset);
            }
        }
        return Optional.ofNullable(Best);
    }

    public static boolean stillValid(Level World, Corner Seat) {
        return World.isLoaded(Seat.Pos()) && standable(World, Seat.Pos()) && wall(World, Seat.Pos(), Seat.First()) && wall(World, Seat.Pos(), Seat.Second());
    }

    public static Optional<Corner> find(Mob Seeker) {
        Level World = Seeker.level();
        BlockPos Centre = Seeker.blockPosition();
        List<Candidate> Found = new ArrayList<>();
        for (BlockPos Cell : BlockPos.betweenClosed(Centre.offset(-Radius, -Rise, -Radius), Centre.offset(Radius, Rise, Radius))) {
            Optional<Corner> Here = at(World, Cell);
            if (Here.isEmpty()) {
                continue;
            }
            double Score = Math.sqrt(Cell.distSqr(Centre));
            if (Here.get().FullHeight()) {
                Score -= FullHeightBonus;
            }
            if (!Scp017Light.isLit(World, Cell)) {
                Score += DarkPenalty;
            }
            Found.add(new Candidate(Here.get(), Score));
        }
        Found.sort(Comparator.comparingDouble(Candidate::Score));
        int Checked = 0;
        for (Candidate Option : Found) {
            if (Checked++ >= PathChecks) {
                break;
            }
            BlockPos Target = Option.Found().Pos();
            if (Target.equals(Centre)) {
                return Optional.of(Option.Found());
            }
            Path Route = Seeker.getNavigation().createPath(Target, 0);
            if (Route != null && Route.canReach()) {
                return Optional.of(Option.Found());
            }
        }
        return Optional.empty();
    }
}
