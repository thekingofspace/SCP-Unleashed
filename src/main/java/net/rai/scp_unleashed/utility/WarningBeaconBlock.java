package net.rai.scp_unleashed.utility;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Map;

public class WarningBeaconBlock extends UtilityWaterloggedBlock {
    public static final MapCodec<WarningBeaconBlock> BlockCodec = simpleCodec(WarningBeaconBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.FACING;
    public static final BooleanProperty Lit = BlockStateProperties.LIT;
    public static final int Brightness = 13;
    private static final int SwitchOffDelay = 4;
    private static final Map<Direction, VoxelShape> ShapeByFacing = UtilityShapes.facing(new double[]{4, 0, 4, 12, 11, 12});

    public WarningBeaconBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(defaultBlockState().setValue(Facing, Direction.UP).setValue(Lit, false));
    }

    @Override
    protected MapCodec<? extends WarningBeaconBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Facing, Lit);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        BlockState State = watered(defaultBlockState(), Context).setValue(Facing, Context.getClickedFace()).setValue(Lit, World.hasNeighborSignal(Pos));
        return State.canSurvive(World, Pos) ? State : null;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        Direction Front = State.getValue(Facing);
        return Block.canSupportCenter(World, Pos.relative(Front.getOpposite()), Front);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == State.getValue(Facing).getOpposite() && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block Neighbor, BlockPos NeighborPos, boolean MovedByPiston) {
        super.neighborChanged(State, World, Pos, Neighbor, NeighborPos, MovedByPiston);
        if (World.isClientSide) {
            return;
        }
        boolean Powered = World.hasNeighborSignal(Pos);
        if (Powered == State.getValue(Lit)) {
            return;
        }
        if (Powered) {
            World.setBlock(Pos, State.setValue(Lit, true), Block.UPDATE_CLIENTS);
        } else {
            World.scheduleTick(Pos, this, SwitchOffDelay);
        }
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (State.getValue(Lit) && !World.hasNeighborSignal(Pos)) {
            World.setBlock(Pos, State.setValue(Lit, false), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return ShapeByFacing.get(State.getValue(Facing));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.setValue(Facing, Flip.mirror(State.getValue(Facing)));
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        UtilityTooltips.describe(this, Lines, "power", "mount");
    }
}
