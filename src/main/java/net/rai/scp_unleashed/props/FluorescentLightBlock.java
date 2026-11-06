package net.rai.scp_unleashed.props;

import com.google.common.collect.ImmutableMap;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FluorescentLightBlock extends FaceAttachedHorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<FluorescentLightBlock> BlockCodec = simpleCodec(FluorescentLightBlock::new);
    public static final BooleanProperty Lit = BlockStateProperties.LIT;
    public static final int Brightness = 15;
    private static final VoxelShape Floor = PropShapes.of(box(1, 0, 3, 15, 4, 13));

    private final ImmutableMap<BlockState, VoxelShape> Outlines;

    public FluorescentLightBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACE, AttachFace.CEILING).setValue(FACING, Direction.NORTH).setValue(Lit, true)
                .setValue(PropWater.Waterlogged, false));
        this.Outlines = getShapeForEachState(State -> PropShapes.attached(Floor, State.getValue(FACE), State.getValue(FACING)));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACE, FACING, Lit, PropWater.Waterlogged);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState State = super.getStateForPlacement(Context);
        if (State == null) {
            return null;
        }
        return PropWater.placed(State.setValue(Lit, !Context.getLevel().hasNeighborSignal(Context.getClickedPos())), Context);
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block Neighbor, BlockPos NeighborPos, boolean MovedByPiston) {
        super.neighborChanged(State, World, Pos, Neighbor, NeighborPos, MovedByPiston);
        if (World.isClientSide) {
            return;
        }
        boolean Powered = World.hasNeighborSignal(Pos);
        if (Powered && State.getValue(Lit)) {
            World.setBlock(Pos, State.setValue(Lit, false), Block.UPDATE_CLIENTS);
        } else if (!Powered && !State.getValue(Lit) && !World.getBlockTicks().hasScheduledTick(Pos, this)) {
            World.scheduleTick(Pos, this, 2 + World.random.nextInt(3));
        }
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (World.hasNeighborSignal(Pos)) {
            if (State.getValue(Lit)) {
                World.setBlock(Pos, State.setValue(Lit, false), Block.UPDATE_CLIENTS);
            }
            return;
        }
        if (!State.getValue(Lit)) {
            World.setBlock(Pos, State.setValue(Lit, true), Block.UPDATE_CLIENTS);
            if (Random.nextFloat() < 0.45f) {
                World.scheduleTick(Pos, this, 2 + Random.nextInt(4));
            }
        } else {
            World.setBlock(Pos, State.setValue(Lit, false), Block.UPDATE_CLIENTS);
            World.scheduleTick(Pos, this, 1 + Random.nextInt(3));
        }
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        PropWater.update(State, World, Pos);
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState State) {
        return PropWater.fluid(State);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        PropTooltips.add(this, Lines, "redstone", "mount");
    }
}
