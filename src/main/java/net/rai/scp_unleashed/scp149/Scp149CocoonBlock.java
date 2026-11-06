package net.rai.scp_unleashed.scp149;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class Scp149CocoonBlock extends BaseEntityBlock {
    public static final MapCodec<Scp149CocoonBlock> BlockCodec = simpleCodec(Scp149CocoonBlock::new);
    public static final IntegerProperty Stage = IntegerProperty.create("stage", 0, 3);
    public static final int MaxStage = 3;
    public static final float Hardness = 0.6f;
    public static final float Resistance = 0.6f;
    private static final VoxelShape[] Shapes = {
            box(5.5, 0.0, 5.5, 10.5, 7.0, 10.5),
            box(4.75, 0.0, 4.75, 11.25, 10.0, 11.25),
            box(4.25, 0.0, 4.25, 11.75, 12.0, 11.75),
            box(4.0, 0.0, 4.0, 12.0, 14.0, 12.0)
    };

    public Scp149CocoonBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Stage, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Stage);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes[State.getValue(Stage)];
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        BlockPos Below = Pos.below();
        return World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState Fresh = defaultBlockState();
        return Fresh.canSurvive(Context.getLevel(), Context.getClickedPos()) ? Fresh : null;
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == Direction.DOWN && !canSurvive(State, World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replaced, boolean Moved) {
        if (!Replaced.is(this) && World instanceof ServerLevel Server && World.getBlockEntity(Pos) instanceof Scp149CocoonBlockEntity Cocoon && !Cocoon.burst()) {
            Scp149Brood.tear(Server, Pos, State);
        }
        super.onRemove(State, World, Pos, Replaced, Moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp149CocoonBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? createTickerHelper(Type, Scp149Registry.CocoonEntity.get(), Scp149CocoonBlockEntity::clientTick)
                : createTickerHelper(Type, Scp149Registry.CocoonEntity.get(), Scp149CocoonBlockEntity::serverTick);
    }
}
