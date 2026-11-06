package net.rai.scp_unleashed.scp3008.furniture.living;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropSeatEntity;
import net.rai.scp_unleashed.props.SeatBlock;

import java.util.List;

public class Scp3008FurnitureABlock extends HorizontalDirectionalBlock implements SeatBlock {
    public static final MapCodec<Scp3008FurnitureABlock> BlockCodec = simpleCodec(Properties -> new Scp3008FurnitureABlock(Properties, Scp3008FurnitureAPiece.Armchair));
    protected final Scp3008FurnitureAPiece Piece;

    public Scp3008FurnitureABlock(Properties Properties, Scp3008FurnitureAPiece Piece) {
        super(Properties);
        this.Piece = Piece;
        registerDefaultState(startState(stateDefinition.any().setValue(FACING, Direction.NORTH)));
    }

    protected BlockState startState(BlockState State) {
        return State;
    }

    public Scp3008FurnitureAPiece piece() {
        return Piece;
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
        Direction Facing = Context.getHorizontalDirection().getOpposite();
        if (Piece.Wall && Context.getClickedFace().getAxis().isHorizontal()) {
            Facing = Context.getClickedFace();
        }
        return defaultBlockState().setValue(FACING, Facing);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Piece.shape(State);
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    public boolean seats(BlockState State) {
        return Piece.Seat;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!Piece.Seat || User.isSecondaryUseActive() || User.isPassenger() || PropSeatEntity.occupied(World, Pos)) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (PropSeatEntity.sit(World, Pos, State, User)) {
            boolean Soft = Piece.Finish == Scp3008FurnitureAPiece.Stuff.Fabric;
            World.playSound(null, Pos, Soft ? Scp3008FurnitureARegistry.Cushion.get() : Scp3008FurnitureARegistry.SeatWood.get(), SoundSource.BLOCKS, 1.0f,
                    0.9f + World.random.nextFloat() * 0.2f);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (Piece.Seat && !World.isClientSide && !State.is(NewState.getBlock())) {
            PropSeatEntity.release(World, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
}
