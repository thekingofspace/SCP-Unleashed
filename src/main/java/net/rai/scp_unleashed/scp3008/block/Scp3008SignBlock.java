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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp3008.Scp3008Department;

public class Scp3008SignBlock extends Scp3008SealedBlock {
    public static final MapCodec<Scp3008SignBlock> SignCodec = simpleCodec(Scp3008SignBlock::new);
    public static final EnumProperty<Scp3008Department> Department = EnumProperty.create("department", Scp3008Department.class);
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, 7);
    public static final EnumProperty<Direction.Axis> Axis = BlockStateProperties.HORIZONTAL_AXIS;
    public static final VoxelShape AlongX = Block.box(0, 0, 7, 16, 16, 9);
    public static final VoxelShape AlongZ = Block.box(7, 0, 0, 9, 16, 16);

    public Scp3008SignBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Department, Scp3008Department.Living).setValue(Part, 0).setValue(Axis, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Scp3008SignBlock> codec() {
        return SignCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Department, Part, Axis);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Axis, Context.getHorizontalDirection().getClockWise().getAxis());
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
