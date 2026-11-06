package net.rai.scp_unleashed.scp4845.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class Scp4845FridgeDoorBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp4845FridgeDoorBlock> DoorCodec = simpleCodec(Scp4845FridgeDoorBlock::new);
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final int Lines = 3;

    public Scp4845FridgeDoorBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends Scp4845FridgeDoorBlock> codec() {
        return DoorCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Half);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        Level World = Context.getLevel();
        if (Pos.getY() >= World.getMaxBuildHeight() - 1 || !World.getBlockState(Pos.above()).canBeReplaced(Context)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        World.setBlock(Pos.above(), State.setValue(Half, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        DoubleBlockHalf Part = State.getValue(Half);
        if (Side.getAxis() == Direction.Axis.Y && (Part == DoubleBlockHalf.LOWER) == (Side == Direction.UP)) {
            return Neighbour.is(this) && Neighbour.getValue(Half) != Part ? State : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos);
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        if (State.getValue(Half) == DoubleBlockHalf.UPPER) {
            BlockState Below = World.getBlockState(Pos.below());
            return Below.is(this) && Below.getValue(Half) == DoubleBlockHalf.LOWER;
        }
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            int Line = World.getRandom().nextInt(Lines);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4845.fridge." + Line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }
}
