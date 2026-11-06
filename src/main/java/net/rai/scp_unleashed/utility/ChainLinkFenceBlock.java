package net.rai.scp_unleashed.utility;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class ChainLinkFenceBlock extends IronBarsBlock {
    public static final MapCodec<ChainLinkFenceBlock> BlockCodec = simpleCodec(ChainLinkFenceBlock::new);
    private final VoxelShape[] Outlines;
    private final VoxelShape[] TallCollisions;

    public ChainLinkFenceBlock(Properties Properties) {
        super(Properties);
        this.Outlines = makeShapes(2.0F, 1.0F, 16.0F, 0.0F, 16.0F);
        this.TallCollisions = makeShapes(2.0F, 1.0F, 24.0F, 0.0F, 24.0F);
    }

    @Override
    public MapCodec<? extends IronBarsBlock> codec() {
        return BlockCodec;
    }

    public static boolean connectsTo(BlockState State, boolean Sturdy, Direction Side) {
        if (State.getBlock() instanceof FenceGateBlock) {
            return FenceGateBlock.connectsToDirection(State, Side);
        }
        return !isExceptionForConnection(State) && Sturdy || State.getBlock() instanceof IronBarsBlock || State.is(BlockTags.WALLS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockGetter World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        BlockState State = defaultBlockState().setValue(WATERLOGGED, Context.getLevel().getFluidState(Pos).getType() == Fluids.WATER);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            BlockPos Beside = Pos.relative(Side);
            BlockState Neighbor = World.getBlockState(Beside);
            State = State.setValue(PROPERTY_BY_DIRECTION.get(Side), connectsTo(Neighbor, Neighbor.isFaceSturdy(World, Beside, Side.getOpposite()), Side));
        }
        return State;
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (State.getValue(WATERLOGGED)) {
            World.scheduleTick(Pos, Fluids.WATER, Fluids.WATER.getTickDelay(World));
        }
        if (Side.getAxis().isHorizontal()) {
            return State.setValue(PROPERTY_BY_DIRECTION.get(Side), connectsTo(NeighborState, NeighborState.isFaceSturdy(World, NeighborPos, Side.getOpposite()), Side));
        }
        return State;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines[getAABBIndex(State)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return TallCollisions[getAABBIndex(State)];
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        UtilityTooltips.describe(this, Lines, "connections", "block.scp_unleashed.utility.unjumpable");
    }
}
