package net.rai.scp_unleashed.daevite.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DaeviteLanternBlock extends DaeviteDecorBlock {
    public static final MapCodec<DaeviteLanternBlock> LanternCodec = simpleCodec(DaeviteLanternBlock::new);
    public static final BooleanProperty Hanging = BlockStateProperties.HANGING;
    public static final VoxelShape Standing = Shapes.or(Block.box(5, 0, 5, 11, 9, 11), Block.box(6.5, 9, 6.5, 9.5, 11, 9.5));
    public static final VoxelShape Hung = Shapes.or(Block.box(5, 1, 5, 11, 10, 11), Block.box(7, 10, 7, 9, 16, 9));

    public DaeviteLanternBlock(Properties Settings) {
        super(Settings, DaeviteDecor.Lantern);
        registerDefaultState(defaultBlockState().setValue(Hanging, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return LanternCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Hanging);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState Base = super.getStateForPlacement(Context);
        for (Direction Side : Context.getNearestLookingDirections()) {
            if (Side.getAxis() != Direction.Axis.Y) {
                continue;
            }
            BlockState Candidate = Base.setValue(Hanging, Side == Direction.UP);
            if (Candidate.canSurvive(Context.getLevel(), Context.getClickedPos())) {
                return Candidate;
            }
        }
        return null;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(Hanging) ? Hung : Standing;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return getShape(State, World, Pos, Context);
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        Direction Toward = State.getValue(Hanging) ? Direction.UP : Direction.DOWN;
        return Block.canSupportCenter(World, Pos.relative(Toward), Toward.getOpposite());
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Other, LevelAccessor World, BlockPos Pos, BlockPos OtherPos) {
        Direction Toward = State.getValue(Hanging) ? Direction.UP : Direction.DOWN;
        if (Side == Toward && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, Other, World, Pos, OtherPos);
    }

    @Override
    public void ambient(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (Random.nextInt(6) == 0) {
            double Lift = State.getValue(Hanging) ? 0.35 : 0.3;
            World.addParticle(ParticleTypes.SMALL_FLAME, Pos.getX() + 0.5, Pos.getY() + Lift, Pos.getZ() + 0.5, 0.0, 0.003, 0.0);
        }
    }
}
