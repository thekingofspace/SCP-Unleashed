package net.rai.scp_unleashed.containmentzone.shutter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.containmentzone.CellShutterBlock;
import net.rai.scp_unleashed.decor.PillarPart;
import net.rai.scp_unleashed.props.PropShapes;

import java.util.ArrayList;
import java.util.List;

public final class ShutterShapes {
    public static final int MaxHeight = 16;
    public static final int SingleHousing = 11;
    public static final int StackHousing = 6;
    public static final int BarHeight = 3;
    public static final int RailWidth = 2;
    public static final int SlabFront = 5;
    public static final int SlabBack = 11;
    public static final int HousingFront = 4;
    public static final int HousingBack = 12;
    public static final int MaxSlatBands = 4;
    public static final float BandEpsilon = 1.0E-4f;
    private static final VoxelShape[] Cache = new VoxelShape[4 * 4 * 2 * 2 * 17 * 17];

    private ShutterShapes() {
    }

    public record Column(int Row, int Height) {
    }

    public record Band(float Low, float High, float VTop, float VBottom) {
    }

    public static Column column(BlockGetter World, BlockPos Pos, BlockState State) {
        PillarPart Part = State.getValue(CellShutterBlock.Part);
        if (Part == PillarPart.Single) {
            return new Column(0, 1);
        }
        int Below = 0;
        if (Part != PillarPart.Bottom) {
            BlockPos Cursor = Pos.below();
            while (Below < MaxHeight && joins(State, World.getBlockState(Cursor))) {
                Below++;
                Cursor = Cursor.below();
            }
        }
        int Above = 0;
        if (Part != PillarPart.Top) {
            BlockPos Cursor = Pos.above();
            while (Above < MaxHeight && joins(State, World.getBlockState(Cursor))) {
                Above++;
                Cursor = Cursor.above();
            }
        }
        return new Column(Below, Below + Above + 1);
    }

    public static boolean joins(BlockState State, BlockState Other) {
        return Other.getBlock() == State.getBlock() && Other.getValue(CellShutterBlock.FACING) == State.getValue(CellShutterBlock.FACING);
    }

    public static int curtainTop(int Height) {
        return Height <= 1 ? SingleHousing : Height * 16 - StackHousing;
    }

    public static float barBottom(int Height, float Progress) {
        return Mth.clamp(Progress, 0.0f, 1.0f) * curtainTop(Height);
    }

    public static List<Band> slats(Column Place, float Progress) {
        float Bar = barBottom(Place.Height(), Progress);
        float Ceiling = curtainTop(Place.Height());
        float Floor = 16.0f * Place.Row();
        float Roof = Floor + 16.0f;
        if (!(Bar < Ceiling) || Roof <= Bar || Floor >= Ceiling) {
            return List.of();
        }
        return slats(Bar, Floor, Math.max(Floor, Bar + BarHeight), Math.min(Roof, Ceiling));
    }

    public static List<Band> slats(float Bar, float Floor, float SlatLow, float SlatHigh) {
        List<Band> Bands = new ArrayList<>(2);
        if (!(SlatHigh - SlatLow > BandEpsilon)) {
            return Bands;
        }
        int First = Mth.floor((SlatLow - Bar) / 16.0f);
        float Cursor = SlatLow;
        for (int Index = First; Index < First + MaxSlatBands && Cursor < SlatHigh - BandEpsilon; Index++) {
            float Offset = Bar + 16.0f * Index;
            float Next = Math.min(SlatHigh, Offset + 16.0f);
            if (Next <= Cursor + BandEpsilon) {
                continue;
            }
            float VTop = Mth.clamp(16.0f - (Next - Offset), 0.0f, 16.0f);
            float VBottom = Mth.clamp(16.0f - (Cursor - Offset), 0.0f, 16.0f);
            Bands.add(new Band(Cursor - Floor, Next - Floor, VTop, VBottom));
            Cursor = Next;
        }
        return Bands;
    }

    public static int localBottom(Column Place, float Progress) {
        return Mth.clamp(Math.round(barBottom(Place.Height(), Progress)) - 16 * Place.Row(), 0, 16);
    }

    public static int localTop(Column Place) {
        return Mth.clamp(curtainTop(Place.Height()) - 16 * Place.Row(), 0, 16);
    }

    public static int housingBottom(PillarPart Part) {
        return Part == PillarPart.Single ? SingleHousing : Part == PillarPart.Top ? 16 - StackHousing : 16;
    }

    public static VoxelShape shape(BlockState State, int Bottom, int Top) {
        Direction Facing = State.getValue(CellShutterBlock.FACING);
        PillarPart Part = State.getValue(CellShutterBlock.Part);
        boolean Left = State.getValue(CellShutterBlock.Left);
        boolean Right = State.getValue(CellShutterBlock.Right);
        int Index = ((((PropShapes.quarterTurns(Facing) * 4 + Part.ordinal()) * 2 + (Left ? 1 : 0)) * 2 + (Right ? 1 : 0)) * 17 + Bottom) * 17 + Top;
        VoxelShape Found = Cache[Index];
        if (Found == null) {
            Found = PropShapes.turn(build(Part, Left, Right, Bottom, Top), Facing);
            Cache[Index] = Found;
        }
        return Found;
    }

    private static VoxelShape build(PillarPart Part, boolean Left, boolean Right, int Bottom, int Top) {
        int Housing = housingBottom(Part);
        VoxelShape Result = Shapes.empty();
        if (Top > Bottom) {
            Result = Shapes.or(Result, PropShapes.box(0, Bottom, SlabFront, 16, Top, SlabBack));
        }
        if (Housing < 16) {
            Result = Shapes.or(Result, PropShapes.box(0, Housing, HousingFront, 16, 16, HousingBack));
        }
        if (!Left) {
            Result = Shapes.or(Result, PropShapes.box(16 - RailWidth, 0, SlabFront, 16, Housing, SlabBack));
        }
        if (!Right) {
            Result = Shapes.or(Result, PropShapes.box(0, 0, SlabFront, RailWidth, Housing, SlabBack));
        }
        return Result.optimize();
    }

    public static AABB opening(BlockPos Pos, Direction Facing, int Bottom, double Reach) {
        double Height = Math.min(16, Bottom + Reach) / 16.0;
        AABB Local = new AABB(0.0, 0.0, (SlabFront - 0.5) / 16.0, 1.0, Height, (SlabBack + 0.5) / 16.0);
        AABB Turned = PropShapes.turn(Shapes.create(Local), Facing).bounds();
        return Turned.move(Pos);
    }
}
