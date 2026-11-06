package net.rai.scp_unleashed.props;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Map;

public class DecorationWorkbenchBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<DecorationWorkbenchBlock> BlockCodec = simpleCodec(DecorationWorkbenchBlock::new);
    public static final Component Title = Component.translatable("container.scp_unleashed.decoration_workbench");
    private static final Map<Direction, VoxelShape> Outlines = PropShapes.horizontal(PropShapes.of(box(0, 0, 1, 16, 14, 15), box(0, 14, 13, 16, 22, 15),
            box(1, 14, 0, 7, 17, 5), box(9, 14, 3, 15, 17, 13)));

    public DecorationWorkbenchBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        User.openMenu(State.getMenuProvider(World, Pos));
        return InteractionResult.CONSUME;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState State, Level World, BlockPos Pos) {
        return new SimpleMenuProvider((ContainerId, PlayerInventory, User) -> new DecorationWorkbenchMenu(ContainerId, PlayerInventory, ContainerLevelAccess.create(World, Pos)), Title);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        PropTooltips.add(this, Lines, "use");
    }
}
