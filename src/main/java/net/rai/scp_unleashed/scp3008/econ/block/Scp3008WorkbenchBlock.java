package net.rai.scp_unleashed.scp3008.econ.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Scp3008WorkbenchBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp3008WorkbenchBlock> BlockCodec = simpleCodec(Scp3008WorkbenchBlock::new);
    private static final VoxelShape Outline = Shapes.or(Block.box(0.0, 13.0, 0.0, 16.0, 16.0, 16.0), Block.box(1.0, 0.0, 1.0, 3.0, 13.0, 3.0),
            Block.box(13.0, 0.0, 1.0, 15.0, 13.0, 3.0), Block.box(1.0, 0.0, 13.0, 3.0, 13.0, 15.0), Block.box(13.0, 0.0, 13.0, 15.0, 13.0, 15.0),
            Block.box(1.5, 3.0, 1.5, 14.5, 4.0, 14.5));

    public Scp3008WorkbenchBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outline;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        User.openMenu(State.getMenuProvider(World, Pos));
        User.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
        return InteractionResult.CONSUME;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState State, Level World, BlockPos Pos) {
        return new SimpleMenuProvider((Id, Holder, User) -> new Scp3008WorkbenchMenu(Id, Holder, ContainerLevelAccess.create(World, Pos), this), getName());
    }
}
