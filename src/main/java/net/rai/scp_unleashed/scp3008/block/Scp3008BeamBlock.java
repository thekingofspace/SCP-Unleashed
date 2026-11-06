package net.rai.scp_unleashed.scp3008.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Scp3008BeamBlock extends Block {
    public static final MapCodec<Scp3008BeamBlock> BeamCodec = simpleCodec(Settings -> new Scp3008BeamBlock(Settings, new double[]{0, 0, 0, 16, 16, 16}));
    public static final EnumProperty<Direction.Axis> Axis = BlockStateProperties.HORIZONTAL_AXIS;
    private final VoxelShape AlongX;
    private final VoxelShape AlongZ;

    public Scp3008BeamBlock(Properties Settings, double[] Box) {
        super(Settings);
        AlongX = Block.box(Box[0], Box[1], Box[2], Box[3], Box[4], Box[5]);
        AlongZ = Block.box(Box[2], Box[1], Box[0], Box[5], Box[4], Box[3]);
        registerDefaultState(stateDefinition.any().setValue(Axis, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Scp3008BeamBlock> codec() {
        return BeamCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Axis);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Axis, Context.getHorizontalDirection().getAxis());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(Axis) == Direction.Axis.X ? AlongX : AlongZ;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        if (Turn == Rotation.CLOCKWISE_90 || Turn == Rotation.COUNTERCLOCKWISE_90) {
            return State.setValue(Axis, State.getValue(Axis) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        }
        return State;
    }
}
