package net.rai.scp_unleashed.scp4845.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class Scp4845TiledBlock extends HorizontalDirectionalBlock {
    protected Scp4845TiledBlock(Properties Settings) {
        super(Settings);
    }

    public abstract int columns();

    public abstract int rows();

    public abstract IntegerProperty tile();

    protected abstract Map<Direction, VoxelShape> shapeOf(int Tile);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, tile());
    }

    public int bottomLeft() {
        return (rows() - 1) * columns();
    }

    public BlockPos cell(BlockPos BottomLeft, Direction Facing, int Tile) {
        int Row = Tile / columns();
        int Column = Tile % columns();
        return BottomLeft.relative(Facing.getClockWise(), Column).above(rows() - 1 - Row);
    }

    public BlockPos bottomLeftOf(BlockPos Pos, BlockState State) {
        int Tile = State.getValue(tile());
        int Row = Tile / columns();
        int Column = Tile % columns();
        return Pos.relative(State.getValue(FACING).getCounterClockWise(), Column).below(rows() - 1 - Row);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Facing = Context.getHorizontalDirection().getOpposite();
        Level World = Context.getLevel();
        BlockPos Origin = Context.getClickedPos();
        for (int Tile = 0; Tile < columns() * rows(); Tile++) {
            BlockPos Spot = cell(Origin, Facing, Tile);
            if (!Spot.equals(Origin) && (World.isOutsideBuildHeight(Spot) || !World.getBlockState(Spot).canBeReplaced(Context))) {
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, Facing).setValue(tile(), bottomLeft());
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        Direction Facing = State.getValue(FACING);
        for (int Tile = 0; Tile < columns() * rows(); Tile++) {
            BlockPos Spot = cell(Pos, Facing, Tile);
            if (!Spot.equals(Pos)) {
                World.setBlock(Spot, State.setValue(tile(), Tile), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide) {
            BlockPos Origin = bottomLeftOf(Pos, State);
            Direction Facing = State.getValue(FACING);
            for (int Tile = 0; Tile < columns() * rows(); Tile++) {
                BlockPos Spot = cell(Origin, Facing, Tile);
                BlockState There = World.getBlockState(Spot);
                if (!Spot.equals(Pos) && There.is(this) && There.getValue(FACING) == Facing && There.getValue(tile()) == Tile) {
                    World.setBlock(Spot, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return shapeOf(State.getValue(tile())).get(State.getValue(FACING));
    }
}
