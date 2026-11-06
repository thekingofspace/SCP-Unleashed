package net.rai.scp_unleashed.scp3008.gear.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

public class Scp3008TallShelfBlock extends Scp3008ShelfBlock {
    public static final MapCodec<Scp3008TallShelfBlock> TallCodec = simpleCodec(Scp3008TallShelfBlock::new);
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final int TallRows = 2;

    public Scp3008TallShelfBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return TallCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Half);
    }

    @Override
    public int rows() {
        return TallRows;
    }

    @Override
    public BlockPos storagePos(BlockState State, BlockPos Pos) {
        return State.getValue(Half) == DoubleBlockHalf.UPPER ? Pos.below() : Pos;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        Level World = Context.getLevel();
        if (Pos.getY() >= World.getMaxBuildHeight() - 1 || !World.getBlockState(Pos.above()).canBeReplaced(Context)) {
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Half, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        placeWhole(World, Pos, State);
    }

    public static void placeWhole(Level World, BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof Scp3008TallShelfBlock) || State.getValue(Half) != DoubleBlockHalf.LOWER) {
            return;
        }
        World.setBlock(Pos.above(), State.setValue(Half, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Other, LevelAccessor World, BlockPos Pos, BlockPos OtherPos) {
        DoubleBlockHalf Part = State.getValue(Half);
        if (Side.getAxis() == Direction.Axis.Y && (Part == DoubleBlockHalf.LOWER) == (Side == Direction.UP)) {
            return Other.is(this) && Other.getValue(Half) != Part ? State : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, Other, World, Pos, OtherPos);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && User.isCreative() && State.getValue(Half) == DoubleBlockHalf.UPPER) {
            BlockPos Below = Pos.below();
            BlockState Base = World.getBlockState(Below);
            if (Base.is(this) && Base.getValue(Half) == DoubleBlockHalf.LOWER) {
                World.setBlock(Below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                World.levelEvent(User, 2001, Below, Block.getId(Base));
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Half) == DoubleBlockHalf.LOWER ? new Scp3008ShelfBlockEntity(Pos, State) : null;
    }
}
