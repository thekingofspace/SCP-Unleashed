package net.rai.scp_unleashed.decor;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.rai.scp_unleashed.block.DecoratableBlock;

import java.util.List;

public class FacilityWallPieceBlock extends DecoratableBlock {
    public static final MapCodec<FacilityWallPieceBlock> PieceCodec = simpleCodec(FacilityWallPieceBlock::new);
    public static final EnumProperty<Direction.Axis> WallAxis = BlockStateProperties.HORIZONTAL_AXIS;

    public FacilityWallPieceBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(defaultBlockState().setValue(WallAxis, Direction.Axis.Z));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return PieceCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(WallAxis);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(WallAxis, Context.getHorizontalDirection().getAxis());
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        if (Turn == Rotation.CLOCKWISE_90 || Turn == Rotation.COUNTERCLOCKWISE_90) {
            return State.setValue(WallAxis, State.getValue(WallAxis) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        }
        return State;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.facility_wall_piece.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
