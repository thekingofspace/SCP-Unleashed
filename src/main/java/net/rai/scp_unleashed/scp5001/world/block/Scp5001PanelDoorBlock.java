package net.rai.scp_unleashed.scp5001.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public abstract class Scp5001PanelDoorBlock extends Block {
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    public static final int RemoveFlags = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_KNOWN_SHAPE;

    protected Scp5001PanelDoorBlock(Properties Settings) {
        super(Settings);
    }

    public abstract IntegerProperty column();

    public abstract IntegerProperty row();

    public abstract int columns();

    public abstract int rows();

    public abstract double thickness();

    protected abstract SoundEvent openSound();

    protected abstract SoundEvent closeSound();

    protected abstract List<SoundEvent> extraSounds(boolean Opening);

    public BlockPos origin(BlockState State, BlockPos Pos) {
        Direction Right = State.getValue(Facing).getClockWise();
        return Pos.relative(Right, -State.getValue(column())).below(State.getValue(row()));
    }

    public BlockPos part(BlockState State, BlockPos Origin, int Column, int Row) {
        Direction Right = State.getValue(Facing).getClockWise();
        return Origin.relative(Right, Column).above(Row);
    }

    private VoxelShape shape(BlockState State) {
        double Half = thickness() / 2.0;
        double Low = 8.0 - Half;
        double High = 8.0 + Half;
        boolean Shut = !State.getValue(Open);
        int Column = State.getValue(column());
        boolean Edge = Column == 0 || Column == columns() - 1;
        if (!Shut && !Edge) {
            return Shapes.empty();
        }
        double X0 = 0.0;
        double X1 = 16.0;
        if (!Shut) {
            if (Column == 0) {
                X1 = 1.5;
            } else {
                X0 = 14.5;
            }
        }
        return switch (State.getValue(Facing)) {
            case SOUTH -> Block.box(16.0 - X1, 0.0, Low, 16.0 - X0, 16.0, High);
            case EAST -> Block.box(Low, 0.0, X0, High, 16.0, X1);
            case WEST -> Block.box(Low, 0.0, 16.0 - X1, High, 16.0, 16.0 - X0);
            default -> Block.box(X0, 0.0, Low, X1, 16.0, High);
        };
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        VoxelShape Found = shape(State);
        return Found.isEmpty() ? Block.box(0.0, 0.0, 7.0, 0.1, 0.1, 9.0) : Found;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return shape(State);
    }

    @Override
    protected boolean isPathfindable(BlockState State, net.minecraft.world.level.pathfinder.PathComputationType Type) {
        return State.getValue(Open);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Face = Context.getHorizontalDirection();
        BlockPos Origin = Context.getClickedPos();
        Level World = Context.getLevel();
        Direction Right = Face.getClockWise();
        for (int Column = 0; Column < columns(); Column++) {
            for (int Row = 0; Row < rows(); Row++) {
                BlockPos At = Origin.relative(Right, Column).above(Row);
                if (World.isOutsideBuildHeight(At) || !World.getBlockState(At).canBeReplaced(Context)) {
                    return null;
                }
            }
        }
        return defaultBlockState().setValue(Facing, Face).setValue(Open, false).setValue(column(), 0).setValue(row(), 0);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, LivingEntity Placer, ItemStack Stack) {
        if (World.isClientSide) {
            return;
        }
        for (int Column = 0; Column < columns(); Column++) {
            for (int Row = 0; Row < rows(); Row++) {
                if (Column == 0 && Row == 0) {
                    continue;
                }
                World.setBlock(part(State, Pos, Column, Row), State.setValue(column(), Column).setValue(row(), Row), Block.UPDATE_ALL);
            }
        }
    }

    public void setOpen(Level World, BlockState State, BlockPos Pos, boolean Opened) {
        BlockPos Origin = origin(State, Pos);
        for (int Column = 0; Column < columns(); Column++) {
            for (int Row = 0; Row < rows(); Row++) {
                BlockPos At = part(State, Origin, Column, Row);
                BlockState Found = World.getBlockState(At);
                if (Found.is(this) && Found.getValue(Open) != Opened) {
                    World.setBlock(At, Found.setValue(Open, Opened), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }
        BlockPos Middle = part(State, Origin, columns() / 2, rows() / 2);
        World.playSound(null, Middle, Opened ? openSound() : closeSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
        for (SoundEvent Layer : extraSounds(Opened)) {
            World.playSound(null, Middle, Layer, SoundSource.BLOCKS, 0.8f, 1.0f);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            setOpen(World, State, Pos, !State.getValue(Open));
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replaced, boolean Moving) {
        if (!State.is(Replaced.getBlock()) && !World.isClientSide) {
            BlockPos Origin = origin(State, Pos);
            for (int Column = 0; Column < columns(); Column++) {
                for (int Row = 0; Row < rows(); Row++) {
                    BlockPos At = part(State, Origin, Column, Row);
                    if (!At.equals(Pos) && World.getBlockState(At).is(this)) {
                        World.setBlock(At, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), RemoveFlags);
                    }
                }
            }
        }
        super.onRemove(State, World, Pos, Replaced, Moving);
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }
}
