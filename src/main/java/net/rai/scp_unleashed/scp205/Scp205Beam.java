package net.rai.scp_unleashed.scp205;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Scp205Beam {
    public static final double PivotRise = 1.0 / 16.0;
    public static final double PivotForward = 3.0 / 16.0;
    public static final double LensForward = 3.5 / 16.0;
    public static final double MaxReach = 10.0;
    public static final double WallReach = 6.5;
    public static final double HalfAngle = Math.toRadians(15.0);
    public static final double Spread = Math.tan(HalfAngle);
    public static final double LensRadius = 3.0 / 16.0;
    public static final double CellStep = 0.5;
    public static final double PoolMinimum = 3.0;
    public static final int NearLevel = 15;
    public static final int MiddleLevel = 14;
    public static final int FarLevel = 13;
    public static final int EdgeLevel = 12;
    public static final int PoolLevel = 13;

    private Scp205Beam() {
    }

    public static BlockPos lowerOf(BlockState State, BlockPos Pos) {
        return State.hasProperty(Scp205LampBlock.Half) && State.getValue(Scp205LampBlock.Half) == DoubleBlockHalf.UPPER ? Pos.below() : Pos;
    }

    public static Direction facing(BlockState State) {
        Direction Side = State.hasProperty(Scp205LampBlock.Facing) ? State.getValue(Scp205LampBlock.Facing) : Direction.NORTH;
        return Side.getAxis().isHorizontal() ? Side : Direction.NORTH;
    }

    public static Scp205Tilt tilt(BlockState State) {
        return State.hasProperty(Scp205LampBlock.Tilt) ? State.getValue(Scp205LampBlock.Tilt) : Scp205Tilt.Level;
    }

    public static Vec3 forward(Direction Side) {
        return new Vec3(Side.getStepX(), 0.0, Side.getStepZ());
    }

    public static Vec3 axis(Direction Side, Scp205Tilt Tilt) {
        double Angle = Math.toRadians(Tilt.degrees());
        return forward(Side).scale(Math.cos(Angle)).add(0.0, Math.sin(Angle), 0.0);
    }

    public static Vec3 pivot(BlockPos LowerPos, Direction Side) {
        return Vec3.atCenterOf(LowerPos.above()).add(forward(Side).scale(PivotForward)).add(0.0, PivotRise, 0.0);
    }

    public static Vec3 origin(BlockPos LowerPos, Direction Side, Scp205Tilt Tilt) {
        return pivot(LowerPos, Side).add(axis(Side, Tilt).scale(LensForward));
    }

    public static double radiusAt(double Distance) {
        return LensRadius + Distance * Spread;
    }

    public static Trace trace(BlockGetter World, BlockPos LowerPos, BlockState State) {
        Direction Side = facing(State);
        Scp205Tilt Tilt = tilt(State);
        Vec3 Origin = origin(LowerPos, Side, Tilt);
        Vec3 Axis = axis(Side, Tilt);
        Vec3 Far = Origin.add(Axis.scale(MaxReach));
        BlockPos UpperPos = LowerPos.above();
        BlockHitResult Hit = BlockGetter.traverseBlocks(Origin, Far, World, (Context, Cursor) -> {
            if (Cursor.equals(LowerPos) || Cursor.equals(UpperPos)) {
                return null;
            }
            if (Context instanceof LevelReader Reader && !Reader.hasChunkAt(Cursor)) {
                return BlockHitResult.miss(Vec3.atCenterOf(Cursor), Direction.UP, Cursor.immutable());
            }
            BlockState Found = Context.getBlockState(Cursor);
            if (Found.isAir() || passesLight(Context, Cursor, Found)) {
                return null;
            }
            VoxelShape Shape = Found.getCollisionShape(Context, Cursor, CollisionContext.empty());
            if (Shape.isEmpty()) {
                return null;
            }
            return Shape.clip(Origin, Far, Cursor);
        }, Context -> null);
        if (Hit == null) {
            return new Trace(Origin, Axis, MaxReach, null, null, Far, false);
        }
        if (Hit.getType() == HitResult.Type.MISS) {
            double Cut = Math.max(0.0, Hit.getLocation().distanceTo(Origin) - 0.5);
            return new Trace(Origin, Axis, Cut, null, null, Origin.add(Axis.scale(Cut)), false);
        }
        BlockPos HitPos = Hit.getBlockPos().immutable();
        Direction Face = Hit.getDirection();
        double Length = Hit.getLocation().distanceTo(Origin);
        boolean Wall = Face.getAxis().isHorizontal() && Length <= WallReach && World.getBlockState(HitPos).isFaceSturdy(World, HitPos, Face);
        return new Trace(Origin, Axis, Length, HitPos, Face, Hit.getLocation(), Wall);
    }

    public static boolean passesLight(BlockGetter World, BlockPos Pos, BlockState State) {
        return State.getBlock() instanceof Scp205GlowBlock || State.propagatesSkylightDown(World, Pos);
    }

    public static int levelAt(double Distance) {
        if (Distance < 3.0) {
            return NearLevel;
        }
        if (Distance < 6.0) {
            return MiddleLevel;
        }
        if (Distance < 9.0) {
            return FarLevel;
        }
        return EdgeLevel;
    }

    public static Map<BlockPos, Integer> cells(Trace Beam, BlockPos LowerPos) {
        Map<BlockPos, Integer> Cells = new LinkedHashMap<>();
        BlockPos UpperPos = LowerPos.above();
        double Limit = Beam.Length() - 0.2;
        for (double Distance = CellStep; Distance < Limit; Distance += CellStep) {
            BlockPos Cell = BlockPos.containing(Beam.Origin().add(Beam.Axis().scale(Distance)));
            if (!Cell.equals(LowerPos) && !Cell.equals(UpperPos)) {
                Cells.merge(Cell.immutable(), levelAt(Distance), Math::max);
            }
        }
        if (Beam.Hit() != null && Beam.Face() != null) {
            BlockPos End = Beam.Hit().relative(Beam.Face());
            if (!End.equals(LowerPos) && !End.equals(UpperPos)) {
                Cells.merge(End, Math.max(PoolLevel, levelAt(Beam.Length())), Math::max);
                if (Beam.Length() >= PoolMinimum && Beam.Face().getAxis().isHorizontal()) {
                    Direction Across = Beam.Face().getClockWise();
                    for (BlockPos Side : List.of(End.relative(Across), End.relative(Across.getOpposite()))) {
                        if (!Side.equals(LowerPos) && !Side.equals(UpperPos)) {
                            Cells.merge(Side, PoolLevel, Math::max);
                        }
                    }
                }
            }
        }
        return Cells;
    }

    public static AABB litArea(Trace Beam) {
        return new AABB(Beam.Origin(), Beam.End()).inflate(3.0, 2.0, 3.0);
    }

    @Nullable
    public static Vec3 home(Level World, Trace Beam, BlockPos LowerPos) {
        if (Beam.Hit() == null || Beam.Face() == null) {
            return null;
        }
        Vec3 Flat = new Vec3(Beam.Axis().x, 0.0, Beam.Axis().z);
        Flat = Flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : Flat.normalize();
        List<Vec3> Tries = new ArrayList<>();
        for (int Step = 0; Step < 4; Step++) {
            Tries.add(Beam.End().subtract(Flat.scale(0.65 + Step)));
        }
        for (Vec3 Try : Tries) {
            BlockPos Start = BlockPos.containing(Try);
            for (int Drop = 0; Drop <= 4; Drop++) {
                BlockPos Feet = Start.below(Drop);
                if (Feet.equals(LowerPos) || Feet.equals(LowerPos.above())) {
                    continue;
                }
                if (standable(World, Feet) && Scp205Light.bright(World, Feet)) {
                    return new Vec3(Try.x, Feet.getY(), Try.z);
                }
            }
        }
        return null;
    }

    public static boolean standable(Level World, BlockPos Feet) {
        return World.getBlockState(Feet).getCollisionShape(World, Feet).isEmpty() && World.getBlockState(Feet.above()).getCollisionShape(World, Feet.above()).isEmpty()
                && World.getBlockState(Feet.below()).isFaceSturdy(World, Feet.below(), Direction.UP);
    }

    public record Trace(Vec3 Origin, Vec3 Axis, double Length, @Nullable BlockPos Hit, @Nullable Direction Face, Vec3 End, boolean Wall) {
    }
}
