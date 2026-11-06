package net.rai.scp_unleashed.scp3008.furniture.living;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class Scp3008FurnitureAPairBlock extends Scp3008FurnitureABlock {
    public static final MapCodec<Scp3008FurnitureAPairBlock> PairCodec = simpleCodec(Properties -> new Scp3008FurnitureAPairBlock(Properties, Scp3008FurnitureAPiece.Sofa));

    public Scp3008FurnitureAPairBlock(Properties Properties, Scp3008FurnitureAPiece Piece) {
        super(Properties, Piece);
    }

    public static void placeWhole(Level World, BlockPos Pos, BlockState State) {
        Scp3008FurnitureAPairs.placeWhole(World, Pos, State);
    }

    @Override
    protected BlockState startState(BlockState State) {
        return State.setValue(Scp3008FurnitureAPairs.Part, Scp3008FurnitureAPart.Main);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return PairCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Scp3008FurnitureAPairs.Part);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState State = super.getStateForPlacement(Context);
        return State != null && Scp3008FurnitureAPairs.fits(Context, Piece, State) ? State : null;
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        if (Scp3008FurnitureAPairs.broken(Piece, State, Side, Neighbour)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos);
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(State.getBlock())) {
            Scp3008FurnitureAPairs.complete(World, Pos, State, Piece);
        }
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        Scp3008FurnitureAPairs.complete(World, Pos, State, Piece);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        Scp3008FurnitureAPairs.creativeBreak(World, Pos, State, User, Piece);
        return super.playerWillDestroy(World, Pos, State, User);
    }
}
