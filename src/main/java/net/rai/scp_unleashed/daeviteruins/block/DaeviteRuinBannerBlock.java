package net.rai.scp_unleashed.daeviteruins.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class DaeviteRuinBannerBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<DaeviteRuinBannerBlock> BlockCodec = simpleCodec(DaeviteRuinBannerBlock::new);
    private static final double[] Box = {1, 0, 0, 15, 16, 3};
    private final Map<Direction, VoxelShape> Shaped = new EnumMap<>(Direction.class);

    public DaeviteRuinBannerBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Shaped.put(Side, DaeviteRuinFacingBlock.rotated(Box, Side));
        }
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shaped.get(State.getValue(FACING));
    }

    public static boolean holds(LevelReader World, BlockPos Pos, Direction Facing) {
        BlockPos Behind = Pos.relative(Facing.getOpposite());
        BlockState Support = World.getBlockState(Behind);
        return Support.isFaceSturdy(World, Behind, Facing) || Support.is(BlockTags.FENCES) || Support.is(BlockTags.WALLS) || Support.getBlock() instanceof IronBarsBlock
                || Support.getBlock() instanceof ChainBlock || Support.is(BlockTags.LOGS);
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return holds(World, Pos, State.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        if (Side == State.getValue(FACING).getOpposite() && !canSurvive(State, World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        for (Direction Looking : Context.getNearestLookingDirections()) {
            if (!Looking.getAxis().isHorizontal()) {
                continue;
            }
            BlockState Candidate = defaultBlockState().setValue(FACING, Looking.getOpposite());
            if (Candidate.canSurvive(Context.getLevel(), Context.getClickedPos())) {
                return Candidate;
            }
        }
        return null;
    }
}
