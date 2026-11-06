package net.rai.scp_unleashed.scp553;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
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

public class Scp553StalactiteBlock extends BaseEntityBlock {
    public static final MapCodec<Scp553StalactiteBlock> BlockCodec = simpleCodec(Scp553StalactiteBlock::new);
    public static final int MaxStage = 3;
    public static final IntegerProperty Stage = IntegerProperty.create("stage", 0, MaxStage);
    public static final float Hardness = 1.5f;
    public static final float Resistance = 1.5f;
    public static final int GlintChance = 6;
    private static final VoxelShape[] Shapes = {
            box(5.0, 12.0, 5.0, 11.0, 16.0, 11.0),
            box(4.5, 8.0, 4.5, 11.5, 16.0, 11.5),
            box(4.0, 4.0, 4.0, 12.0, 16.0, 12.0),
            box(3.5, 1.0, 3.5, 12.5, 16.0, 12.5)
    };

    public Scp553StalactiteBlock(Properties Properties) {
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
        return RenderShape.MODEL;
    }

    public static VoxelShape shapeFor(int Value) {
        return Shapes[Math.max(0, Math.min(MaxStage, Value))];
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return shapeFor(State.getValue(Stage));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        BlockPos Above = Pos.above();
        return World.getBlockState(Above).isFaceSturdy(World, Above, Direction.DOWN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState Fresh = defaultBlockState();
        return Fresh.canSurvive(Context.getLevel(), Context.getClickedPos()) ? Fresh : null;
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == Direction.UP && !canSurvive(State, World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        int Grown = State.getValue(Stage);
        if (Random.nextInt(GlintChance) > Grown) {
            return;
        }
        double Low = 1.0 - (Grown + 1) * 0.22;
        World.addParticle(Scp553Registry.Glint.get(), Pos.getX() + 0.3 + Random.nextDouble() * 0.4, Pos.getY() + Low + Random.nextDouble() * (1.0 - Low),
                Pos.getZ() + 0.3 + Random.nextDouble() * 0.4, 0.0, -0.01, 0.0);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp553StalactiteBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, Scp553Registry.StalactiteEntity.get(), Scp553StalactiteBlockEntity::serverTick);
    }
}
