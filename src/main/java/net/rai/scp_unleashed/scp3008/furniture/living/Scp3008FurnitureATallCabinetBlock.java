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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class Scp3008FurnitureATallCabinetBlock extends Scp3008FurnitureACabinetBlock {
    public static final MapCodec<Scp3008FurnitureATallCabinetBlock> TallCodec = simpleCodec(Properties -> new Scp3008FurnitureATallCabinetBlock(Properties, Scp3008FurnitureAPiece.Fridge));

    public Scp3008FurnitureATallCabinetBlock(Properties Properties, Scp3008FurnitureAPiece Piece) {
        super(Properties, Piece);
    }

    public static void placeWhole(Level World, BlockPos Pos, BlockState State) {
        Scp3008FurnitureAPairs.placeWhole(World, Pos, State);
    }

    @Override
    protected BlockState startState(BlockState State) {
        return super.startState(State).setValue(Scp3008FurnitureAPairs.Part, Scp3008FurnitureAPart.Main);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return TallCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Scp3008FurnitureAPairs.Part);
    }

    @Override
    public BlockPos containerPos(BlockState State, BlockPos Pos) {
        return Scp3008FurnitureAPairs.main(Piece, State, Pos);
    }

    @Override
    public Vec3 soundAt(BlockState State, BlockPos Pos) {
        return Vec3.atCenterOf(containerPos(State, Pos)).add(0.0, 0.5, 0.0);
    }

    @Override
    public void setOpen(Level World, BlockPos Pos, BlockState State, boolean Opened) {
        super.setOpen(World, Pos, State, Opened);
        BlockPos Other = Scp3008FurnitureAPairs.partner(Piece, State, Pos);
        BlockState There = World.getBlockState(Other);
        if (Scp3008FurnitureAPairs.matches(State, There) && There.getValue(Open) != Opened) {
            World.setBlock(Other, There.setValue(Open, Opened), Block.UPDATE_ALL);
        }
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Scp3008FurnitureAPairs.Part) == Scp3008FurnitureAPart.Main ? super.newBlockEntity(Pos, State) : null;
    }
}
