package net.rai.scp_unleashed.scp087;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

public class Scp087DoorBlock extends Scp087SealedBlock {
    public static final MapCodec<Scp087DoorBlock> DoorCodec = simpleCodec(Scp087DoorBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public Scp087DoorBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.SOUTH).setValue(Half, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends Scp087DoorBlock> codec() {
        return DoorCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Half);
    }

    public static BlockPos lower(BlockPos Pos, BlockState State) {
        return State.getValue(Half) == DoubleBlockHalf.UPPER ? Pos.below() : Pos;
    }

    public static void place(Level World, BlockPos Lower, Direction Front, int Flags) {
        BlockState Base = Scp087Registry.Door.get().defaultBlockState().setValue(Facing, Front);
        World.setBlock(Lower, Base.setValue(Half, DoubleBlockHalf.LOWER), Flags);
        World.setBlock(Lower.above(), Base.setValue(Half, DoubleBlockHalf.UPPER), Flags);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        Level World = Context.getLevel();
        if (World.isOutsideBuildHeight(Pos.above()) || !World.getBlockState(Pos.above()).canBeReplaced(Context)) {
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Half, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        World.setBlock(Pos.above(), State.setValue(Half, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replaced, boolean Moved) {
        super.onRemove(State, World, Pos, Replaced, Moved);
        if (World.isClientSide || Replaced.is(this)) {
            return;
        }
        BlockPos Other = State.getValue(Half) == DoubleBlockHalf.LOWER ? Pos.above() : Pos.below();
        if (World.getBlockState(Other).is(this)) {
            World.setBlock(Other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (User instanceof ServerPlayer Visitor && !(User instanceof FakePlayer) && !Visitor.isSpectator()) {
            BlockPos Lower = lower(Pos, State);
            if (Scp087Stairwell.inStairwell(World)) {
                Scp087Descent.exitByDoor(Visitor);
            } else {
                Scp087Descent.enterByDoor(Visitor, Lower, State.getValue(Facing));
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return false;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return false;
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
