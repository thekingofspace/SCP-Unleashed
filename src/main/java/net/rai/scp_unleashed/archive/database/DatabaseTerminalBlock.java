package net.rai.scp_unleashed.archive.database;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;
import java.util.Map;

public class DatabaseTerminalBlock extends Block {
    public static final MapCodec<DatabaseTerminalBlock> BlockCodec = simpleCodec(DatabaseTerminalBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    private static final double[][] Boxes = {
            {2, 0, 2, 14, 1, 14},
            {4, 1, 2, 12, 2, 5},
            {6, 1, 7, 10, 4, 10},
            {1, 4, 5, 15, 15, 12},
            {3, 5, 12, 13, 13, 14}
    };
    private static final Map<Direction, VoxelShape> Shapes = DatabaseShapes.turnedShapes(Boxes);

    public DatabaseTerminalBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.get(State.getValue(Facing));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && User instanceof ServerPlayer Reader) {
            DatabaseActions.open(Reader, Pos, false);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
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
        Lines.add(Component.translatable("block.scp_unleashed.archive_database_terminal.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.archive_database_terminal.browse").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.archive_database_terminal.read_only").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.archive_database_terminal.return").withStyle(ChatFormatting.GOLD));
        }
    }
}
