package net.rai.scp_unleashed.scp3199;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class Scp3199EggBlock extends BaseEntityBlock {
    public static final MapCodec<Scp3199EggBlock> Codec = simpleCodec(Scp3199EggBlock::new);
    public static final BooleanProperty Waterlogged = BlockStateProperties.WATERLOGGED;

    public Scp3199EggBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Waterlogged, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return Codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Waterlogged);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return !State.getValue(Waterlogged);
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return true;
    }

    @Override
    protected boolean canBeReplaced(BlockState State, BlockPlaceContext Context) {
        return false;
    }

    @Override
    protected boolean canBeReplaced(BlockState State, Fluid Liquid) {
        return false;
    }

    @Override
    protected FluidState getFluidState(BlockState State) {
        return State.getValue(Waterlogged) ? Fluids.WATER.getSource(false) : super.getFluidState(State);
    }

    public static BlockState leftover(BlockState State) {
        return State.getValue(Waterlogged) ? Fluids.WATER.defaultFluidState().createLegacyBlock() : Blocks.AIR.defaultBlockState();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp3199EggBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, Scp3199Registry.NestEntity.get(), Scp3199EggBlockEntity::serverTick);
    }
}
