package net.rai.scp_unleashed.intro.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class IntroPropBlock extends HorizontalDirectionalBlock {
    public static final int NameplateShape = 0;
    public static final int FolderShape = 1;
    public static final int EmblemShape = 2;
    public static final int ClockShape = 3;
    public static final double[][] Boxes = {
            {3.0, 0.0, 6.0, 13.0, 4.0, 10.0},
            {1.0, 0.0, 2.0, 15.0, 2.0, 14.0},
            {1.0, 1.0, 14.0, 15.0, 15.0, 16.0},
            {2.0, 2.0, 14.0, 14.0, 14.0, 16.0}
    };
    public static final MapCodec<IntroPropBlock> PropCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            propertiesCodec(),
            Codec.INT.fieldOf("shape").forGetter(IntroPropBlock::shapeIndex)
    ).apply(Instance, IntroPropBlock::new));
    private final int Shape;
    private final VoxelShape[] Turned = new VoxelShape[4];

    public IntroPropBlock(Properties Properties, int Shape) {
        super(Properties);
        this.Shape = Shape;
        double[] Box = Boxes[Math.max(0, Math.min(Boxes.length - 1, Shape))];
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Turned[Side.get2DDataValue()] = turn(Box, Side);
        }
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public static VoxelShape turn(double[] Box, Direction Side) {
        double X1 = Box[0];
        double Z1 = Box[2];
        double X2 = Box[3];
        double Z2 = Box[5];
        int Steps = switch (Side) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        for (int Step = 0; Step < Steps; Step++) {
            double NewX1 = 16.0 - Z2;
            double NewX2 = 16.0 - Z1;
            Z1 = X1;
            Z2 = X2;
            X1 = NewX1;
            X2 = NewX2;
        }
        return Block.box(X1, Box[1], Z1, X2, Box[4], Z2);
    }

    public int shapeIndex() {
        return Shape;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return PropCodec;
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
        return Turned[State.getValue(FACING).get2DDataValue()];
    }
}
