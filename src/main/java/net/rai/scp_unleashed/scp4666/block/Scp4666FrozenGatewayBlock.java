package net.rai.scp_unleashed.scp4666.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.gateway.ContainedGatewayBlock;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp4666FrozenGatewayBlock extends Block implements LargeBlock {
    public static final MapCodec<Scp4666FrozenGatewayBlock> BlockCodec = simpleCodec(Scp4666FrozenGatewayBlock::new);
    private static boolean Dismantling;

    public Scp4666FrozenGatewayBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(ContainedGatewayBlock.Facing, Direction.NORTH).setValue(ContainedGatewayBlock.Part, ContainedGatewayBlock.OriginPart));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(ContainedGatewayBlock.Facing, ContainedGatewayBlock.Part);
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        return ContainedGatewayBlock.footprint(Context.getClickedPos(), Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return ContainedGatewayBlock.shapeOf(State.getValue(ContainedGatewayBlock.Facing), State.getValue(ContainedGatewayBlock.Part));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return defaultBlockState().setValue(ContainedGatewayBlock.Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        Direction Front = State.getValue(ContainedGatewayBlock.Facing);
        for (int Index = 0; Index < ContainedGatewayBlock.CellCount; Index++) {
            if (Index != ContainedGatewayBlock.OriginPart) {
                World.setBlock(ContainedGatewayBlock.cellPos(Pos, Front, Index), State.setValue(ContainedGatewayBlock.Part, Index), Block.UPDATE_ALL);
            }
        }
    }

    public static void assemble(Level World, BlockPos Origin, Direction Front) {
        BlockState Base = Scp4666Registry.FrozenGateway.get().defaultBlockState().setValue(ContainedGatewayBlock.Facing, Front);
        for (int Index = 0; Index < ContainedGatewayBlock.CellCount; Index++) {
            World.setBlock(ContainedGatewayBlock.cellPos(Origin, Front, Index), Base.setValue(ContainedGatewayBlock.Part, Index), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && !World.isClientSide && !Dismantling) {
            Dismantling = true;
            try {
                BlockPos Origin = ContainedGatewayBlock.originOf(Pos, State);
                Direction Front = State.getValue(ContainedGatewayBlock.Facing);
                for (int Index = 0; Index < ContainedGatewayBlock.CellCount; Index++) {
                    BlockPos Cell = ContainedGatewayBlock.cellPos(Origin, Front, Index);
                    if (Cell.equals(Pos)) {
                        continue;
                    }
                    BlockState CellState = World.getBlockState(Cell);
                    if (CellState.is(this) && CellState.getValue(ContainedGatewayBlock.Part) == Index) {
                        World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            } finally {
                Dismantling = false;
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (Random.nextInt(10) == 0) {
            World.addParticle(ParticleTypes.SNOWFLAKE, Pos.getX() + Random.nextDouble(), Pos.getY() + Random.nextDouble(), Pos.getZ() + Random.nextDouble(), 0.0, -0.02, 0.0);
        }
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(ContainedGatewayBlock.Facing, Turn.rotate(State.getValue(ContainedGatewayBlock.Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp4666_frozen_gateway.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
