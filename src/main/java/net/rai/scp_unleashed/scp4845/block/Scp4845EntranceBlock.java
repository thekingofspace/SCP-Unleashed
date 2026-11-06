package net.rai.scp_unleashed.scp4845.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Scp4845EntranceBlock extends Scp4845TiledBlock {
    public static final MapCodec<Scp4845EntranceBlock> EntranceCodec = simpleCodec(Scp4845EntranceBlock::new);
    public static final int Columns = 2;
    public static final int Rows = 3;
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, Columns * Rows - 1);
    public static final double[][] Transom = {{0, 0, 0, 16, 16, 16}};
    public static final double[][] LeftDoor = {{0, 0, 0, 2, 16, 16}, {2, 0, 4, 9, 16, 11}};
    public static final double[][] RightDoor = {{14, 0, 0, 16, 16, 16}, {7, 0, 4, 14, 16, 11}};
    private static final List<Map<Direction, VoxelShape>> Shapes = build();

    public Scp4845EntranceBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Part, 0));
    }

    private static List<Map<Direction, VoxelShape>> build() {
        List<Map<Direction, VoxelShape>> Made = new ArrayList<>();
        for (int Index = 0; Index < Columns * Rows; Index++) {
            int Row = Index / Columns;
            Made.add(Scp4845Shapes.facings(Row == 0 ? Transom : Index % Columns == 0 ? LeftDoor : RightDoor));
        }
        return Made;
    }

    @Override
    protected MapCodec<? extends Scp4845EntranceBlock> codec() {
        return EntranceCodec;
    }

    @Override
    public int columns() {
        return Columns;
    }

    @Override
    public int rows() {
        return Rows;
    }

    @Override
    public IntegerProperty tile() {
        return Part;
    }

    @Override
    protected Map<Direction, VoxelShape> shapeOf(int Index) {
        return Shapes.get(Index);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return getShape(State, World, Pos, Context);
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return State.getValue(Part) >= Columns && Kind == PathComputationType.LAND;
    }
}
