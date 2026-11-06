package net.rai.scp_unleashed.daeviteruins.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class DaeviteRuinFacingBlock extends HorizontalDirectionalBlock {
    public static final int Full = 0;
    public static final int Head = 1;
    public static final int Hand = 2;
    public static final int Rack = 3;
    public static final int UrnShape = 4;
    private static final double[][] Boxes = {
            {0, 0, 0, 16, 16, 16},
            {2, 0, 2, 14, 15, 14},
            {2, 0, 0, 16, 7, 15},
            {1, 0, 0, 15, 15, 7},
            {3, 0, 3, 13, 14, 13}
    };
    private final int Kind;
    private final Map<Direction, VoxelShape> Shaped = new EnumMap<>(Direction.class);

    public DaeviteRuinFacingBlock(Properties Settings, int Kind) {
        super(Settings);
        this.Kind = Kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Shaped.put(Side, Kind == Full ? Shapes.block() : rotated(Boxes[Kind], Side));
        }
    }

    public static VoxelShape rotated(double[] Box, Direction Side) {
        double X1 = Box[0];
        double Z1 = Box[2];
        double X2 = Box[3];
        double Z2 = Box[5];
        return switch (Side) {
            case WEST -> Block.box(16 - Z2, Box[1], X1, 16 - Z1, Box[4], X2);
            case NORTH -> Block.box(16 - X2, Box[1], 16 - Z2, 16 - X1, Box[4], 16 - Z1);
            case EAST -> Block.box(Z1, Box[1], 16 - X2, Z2, Box[4], 16 - X1);
            default -> Block.box(X1, Box[1], Z1, X2, Box[4], Z2);
        };
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(Settings -> new DaeviteRuinFacingBlock(Settings, Kind));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shaped.get(State.getValue(FACING));
    }
}
