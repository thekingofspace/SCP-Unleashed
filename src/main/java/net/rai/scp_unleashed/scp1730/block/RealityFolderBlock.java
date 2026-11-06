package net.rai.scp_unleashed.scp1730.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.wire.WireHostBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RealityFolderBlock extends BaseEntityBlock implements LargeBlock, WireHostBlock {
    public static final MapCodec<RealityFolderBlock> FolderCodec = simpleCodec(RealityFolderBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, RealityFolderLayout.CellCount - 1);
    public static final BooleanProperty Powered = BlockStateProperties.POWERED;
    private static boolean Dismantling;

    public RealityFolderBlock(Properties Props) {
        super(Props);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, RealityFolderLayout.MasterPart).setValue(Powered, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return FolderCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Part, Powered);
    }

    public static boolean master(BlockState State) {
        return State.getValue(Part) == RealityFolderLayout.MasterPart;
    }

    public static BlockPos masterPos(BlockState State, BlockPos Pos) {
        return RealityFolderLayout.masterOf(Pos, State.getValue(Part));
    }

    @Override
    public BlockPos devicePos(BlockState State, BlockPos Pos) {
        return masterPos(State, Pos);
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        return RealityFolderLayout.cells(Context.getClickedPos());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Part, RealityFolderLayout.MasterPart);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        if (World.isClientSide) {
            return;
        }
        for (int Index = 0; Index < RealityFolderLayout.CellCount; Index++) {
            if (Index != RealityFolderLayout.MasterPart) {
                World.setBlock(RealityFolderLayout.cellPos(Pos, Index), State.setValue(Part, Index), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide) {
            BlockPos Master = masterPos(State, Pos);
            if (!User.isCreative()) {
                popResource(World, Master.above(), new ItemStack(Scp1730Registry.FolderItem.get()));
            }
            dismantle(World, Master, Pos);
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replacement, boolean Moving) {
        if (!State.is(Replacement.getBlock()) && !World.isClientSide && !Dismantling) {
            dismantle(World, masterPos(State, Pos), Pos);
        }
        super.onRemove(State, World, Pos, Replacement, Moving);
    }

    private void dismantle(Level World, BlockPos Master, BlockPos Skip) {
        if (Dismantling) {
            return;
        }
        Dismantling = true;
        try {
            for (int Index = 0; Index < RealityFolderLayout.CellCount; Index++) {
                BlockPos Cell = RealityFolderLayout.cellPos(Master, Index);
                if (!Cell.equals(Skip) && World.getBlockState(Cell).is(this) && World.getBlockState(Cell).getValue(Part) == Index) {
                    World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        } finally {
            Dismantling = false;
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        if (master(State)) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }
        return RealityFolderLayout.portSide(State.getValue(Part), State.getValue(Facing)) != null ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return RealityFolderLayout.shape(State.getValue(Part), State.getValue(Facing));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return master(State) ? new RealityFolderBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (!master(State)) {
            return null;
        }
        return createTickerHelper(Type, Scp1730Registry.FolderEntity.get(), World.isClientSide ? RealityFolderBlockEntity::clientTick : RealityFolderBlockEntity::serverTick);
    }

    @Override
    public void appendHoverText(ItemStack Stack, net.minecraft.world.item.Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.reality_folder.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.reality_folder.detail", RealityFolderBlockEntity.Demand).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.reality_folder.wiring").withStyle(ChatFormatting.GRAY));
        }
    }
}
