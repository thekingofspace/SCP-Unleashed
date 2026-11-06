package net.rai.scp_unleashed.scp3008.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.rai.scp_unleashed.scp3008.Scp3008Transit;

import java.util.Map;

public class Scp3008EntranceBlock extends Scp3008SealedBlock {
    public static final MapCodec<Scp3008EntranceBlock> EntranceCodec = simpleCodec(Scp3008EntranceBlock::new);
    public static final int Width = 4;
    public static final int Height = 3;
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, Width * Height - 1);
    public static final Map<Direction, VoxelShape> Outline = Scp3008Shapes.facings(new double[][]{{0, 0, 10, 16, 16, 16}});

    public Scp3008EntranceBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, 0));
    }

    @Override
    protected MapCodec<? extends Scp3008EntranceBlock> codec() {
        return EntranceCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Part);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outline.get(State.getValue(Facing));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState State, Level World, BlockPos Pos, Entity Visitor) {
        if (!World.isClientSide && Visitor instanceof ServerPlayer User && !(User instanceof FakePlayer) && !User.isSpectator() && !User.isPassenger()) {
            Scp3008Transit.enter(User, Pos, State.getValue(Facing));
        }
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return false;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState State, BlockGetter World, BlockPos Pos) {
        return 1.0f;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }
}
