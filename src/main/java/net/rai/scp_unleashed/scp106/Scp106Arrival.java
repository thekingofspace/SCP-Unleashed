package net.rai.scp_unleashed.scp106;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Scp106Arrival {
    public static final int CeilingMin = 3;
    public static final int CeilingMax = 7;
    public static final int WallHeight = 3;
    public static final int CeilingThickness = 3;
    public static final float BothWall = 0.4f;
    public static final float BothCeiling = 0.75f;
    public static final float WallOnly = 0.6f;
    public static final float CeilingOnly = 0.5f;

    public enum Style {
        Floor,
        Wall,
        Ceiling
    }

    public record Plan(Style Kind, Vec3 End, @Nullable Direction Out, double CeilingY) {
    }

    private Scp106Arrival() {
    }

    public static Plan floor(Vec3 Spot) {
        return new Plan(Style.Floor, Spot, null, Double.NaN);
    }

    public static Plan choose(ServerLevel World, Vec3 Spot, RandomSource Random, @Nullable Style Forced) {
        BlockPos Feet = BlockPos.containing(Spot.x, Spot.y + 0.01, Spot.z);
        if (!World.isLoaded(Feet) || !Scp106OldMan.openFor(World, Feet)) {
            return floor(Spot);
        }
        Vec3 End = new Vec3(Feet.getX() + 0.5, Spot.y, Feet.getZ() + 0.5);
        List<Direction> Walls = walls(World, Feet);
        double Ceiling = ceiling(World, Feet);
        boolean HasWall = !Walls.isEmpty();
        boolean HasCeiling = !Double.isNaN(Ceiling);
        Style Kind = Style.Floor;
        if (Forced != null) {
            Kind = Forced == Style.Wall && HasWall || Forced == Style.Ceiling && HasCeiling ? Forced : Style.Floor;
        } else {
            float Roll = Random.nextFloat();
            if (HasWall && HasCeiling) {
                Kind = Roll < BothWall ? Style.Wall : Roll < BothCeiling ? Style.Ceiling : Style.Floor;
            } else if (HasWall) {
                Kind = Roll < WallOnly ? Style.Wall : Style.Floor;
            } else if (HasCeiling) {
                Kind = Roll < CeilingOnly ? Style.Ceiling : Style.Floor;
            }
        }
        return switch (Kind) {
            case Wall -> new Plan(Style.Wall, End, Walls.get(Random.nextInt(Walls.size())), Double.NaN);
            case Ceiling -> new Plan(Style.Ceiling, End, null, Ceiling);
            default -> floor(Spot);
        };
    }

    public static List<Direction> walls(ServerLevel World, BlockPos Feet) {
        List<Direction> Found = new ArrayList<>();
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            BlockPos Base = Feet.relative(Side);
            boolean Usable = true;
            for (int Dy = 0; Dy < WallHeight && Usable; Dy++) {
                BlockPos Cell = Base.above(Dy);
                if (!World.isLoaded(Cell)) {
                    Usable = false;
                    break;
                }
                BlockState State = World.getBlockState(Cell);
                Usable = !State.isAir() && State.isFaceSturdy(World, Cell, Side.getOpposite()) && !Scp106Phasing.holds(World, Cell, State)
                        && State.getFluidState().isEmpty();
            }
            if (Usable) {
                Found.add(Side.getOpposite());
            }
        }
        return Found;
    }

    public static double ceiling(ServerLevel World, BlockPos Feet) {
        if (World.canSeeSky(Feet)) {
            return Double.NaN;
        }
        for (int Dy = WallHeight; Dy <= CeilingMax; Dy++) {
            BlockPos Cell = Feet.above(Dy);
            if (!World.isLoaded(Cell)) {
                return Double.NaN;
            }
            if (!World.getFluidState(Cell).isEmpty()) {
                return Double.NaN;
            }
            if (!Scp106Phasing.solid(World, Cell)) {
                continue;
            }
            if (Dy < CeilingMin || !World.getBlockState(Cell).isFaceSturdy(World, Cell, Direction.DOWN)) {
                return Double.NaN;
            }
            for (int Up = 0; Up < CeilingThickness; Up++) {
                BlockPos Inside = Cell.above(Up);
                if (!World.isLoaded(Inside) || Scp106Phasing.holds(World, Inside, World.getBlockState(Inside))) {
                    return Double.NaN;
                }
            }
            return Cell.getY();
        }
        return Double.NaN;
    }
}
