package net.rai.scp_unleashed.scp3008.furniture.living;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public final class Scp3008FurnitureAPairs {
    public static final EnumProperty<Scp3008FurnitureAPart> Part = EnumProperty.create("part", Scp3008FurnitureAPart.class);

    private Scp3008FurnitureAPairs() {
    }

    public static Direction outward(Scp3008FurnitureAPiece Piece, Direction Facing) {
        return Piece.Reach == Scp3008FurnitureAPiece.Span.Up ? Direction.UP : Facing.getClockWise();
    }

    public static Direction toward(Scp3008FurnitureAPiece Piece, BlockState State) {
        Direction Out = outward(Piece, State.getValue(HorizontalDirectionalBlock.FACING));
        return State.getValue(Part) == Scp3008FurnitureAPart.Main ? Out : Out.getOpposite();
    }

    public static BlockPos partner(Scp3008FurnitureAPiece Piece, BlockState State, BlockPos Pos) {
        return Pos.relative(toward(Piece, State));
    }

    public static BlockPos main(Scp3008FurnitureAPiece Piece, BlockState State, BlockPos Pos) {
        return State.getValue(Part) == Scp3008FurnitureAPart.Main ? Pos : partner(Piece, State, Pos);
    }

    public static boolean matches(BlockState State, BlockState Other) {
        return Other.is(State.getBlock()) && Other.getValue(Part) == State.getValue(Part).other()
                && Other.getValue(HorizontalDirectionalBlock.FACING) == State.getValue(HorizontalDirectionalBlock.FACING);
    }

    public static boolean fits(BlockPlaceContext Context, Scp3008FurnitureAPiece Piece, BlockState State) {
        BlockPos Other = partner(Piece, State, Context.getClickedPos());
        Level World = Context.getLevel();
        return World.isInWorldBounds(Other) && World.getBlockState(Other).canBeReplaced(Context);
    }

    public static boolean broken(Scp3008FurnitureAPiece Piece, BlockState State, Direction Side, BlockState Neighbour) {
        return Side == toward(Piece, State) && !matches(State, Neighbour);
    }

    public static boolean complete(Level World, BlockPos Pos, BlockState State, Scp3008FurnitureAPiece Piece) {
        if (World.isClientSide || State.getValue(Part) != Scp3008FurnitureAPart.Main) {
            return true;
        }
        BlockPos Other = partner(Piece, State, Pos);
        BlockState There = World.getBlockState(Other);
        if (matches(State, There)) {
            return true;
        }
        if (!World.isInWorldBounds(Other) || !There.canBeReplaced()) {
            return false;
        }
        World.setBlock(Other, State.setValue(Part, Scp3008FurnitureAPart.Extra), Block.UPDATE_ALL);
        return true;
    }

    public static void creativeBreak(Level World, BlockPos Pos, BlockState State, Player User, Scp3008FurnitureAPiece Piece) {
        if (World.isClientSide || !User.isCreative() || State.getValue(Part) != Scp3008FurnitureAPart.Extra) {
            return;
        }
        BlockPos Main = partner(Piece, State, Pos);
        BlockState There = World.getBlockState(Main);
        if (matches(State, There)) {
            World.setBlock(Main, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            World.levelEvent(User, 2001, Main, Block.getId(There));
        }
    }

    public static void placeWhole(Level World, BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof Scp3008FurnitureABlock Furniture) || !State.hasProperty(Part)) {
            World.setBlock(Pos, State, Block.UPDATE_ALL);
            return;
        }
        BlockState Main = State.setValue(Part, Scp3008FurnitureAPart.Main);
        BlockPos Other = partner(Furniture.piece(), Main, Pos);
        BlockState There = World.getBlockState(Other);
        if (!matches(Main, There) && (!World.isInWorldBounds(Other) || !There.canBeReplaced())) {
            if (World.getBlockState(Pos).is(State.getBlock())) {
                World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
            return;
        }
        if (!World.getBlockState(Pos).equals(Main)) {
            World.setBlock(Pos, Main, Block.UPDATE_ALL);
        }
        complete(World, Pos, Main, Furniture.piece());
    }
}
