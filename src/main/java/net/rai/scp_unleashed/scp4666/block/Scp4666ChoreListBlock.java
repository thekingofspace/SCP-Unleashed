package net.rai.scp_unleashed.scp4666.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Map;

public class Scp4666ChoreListBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp4666ChoreListBlock> BlockCodec = simpleCodec(Scp4666ChoreListBlock::new);
    private static final Map<Direction, VoxelShape> Shapes = Scp4666Shapes.table(new double[][]{{2, 2, 14, 14, 15, 16}});

    public Scp4666ChoreListBlock(Properties Properties) {
        super(Properties);
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
        Direction Face = Context.getClickedFace();
        if (Face.getAxis().isVertical()) {
            Face = Context.getHorizontalDirection().getOpposite();
        }
        BlockState State = defaultBlockState().setValue(FACING, Face);
        return State.canSurvive(Context.getLevel(), Context.getClickedPos()) ? State : null;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        BlockPos Wall = Pos.relative(State.getValue(FACING).getOpposite());
        return World.getBlockState(Wall).isFaceSturdy(World, Wall, State.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Other, LevelAccessor World, BlockPos Pos, BlockPos OtherPos) {
        return Side == State.getValue(FACING).getOpposite() && !State.canSurvive(World, Pos) ? Blocks.AIR.defaultBlockState() : State;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.get(State.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        return World.isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp4666_chore_list.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
