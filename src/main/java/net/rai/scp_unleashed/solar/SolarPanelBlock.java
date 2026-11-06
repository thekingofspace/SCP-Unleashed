package net.rai.scp_unleashed.solar;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.wire.WireHostBlock;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SolarPanelBlock extends BaseEntityBlock implements WireHostBlock {
    public static final MapCodec<SolarPanelBlock> BlockCodec = simpleCodec(SolarPanelBlock::new);
    public static final BooleanProperty Generating = BooleanProperty.create("generating");
    private static final VoxelShape Support = Shapes.or(box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0), box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0));

    public SolarPanelBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Generating, false));
    }

    @Override
    public BlockPos devicePos(BlockState State, BlockPos Pos) {
        return Pos;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Generating);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return Support;
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(this) && World instanceof ServerLevel ServerWorld) {
            WireNetworkManager.get(ServerWorld).markDirty();
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World instanceof ServerLevel ServerWorld) {
            WireNetworkManager.get(ServerWorld).markDirty();
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.solar_panel.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.solar.peak", SolarTier.Panel.getPeakOutput()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.solar.conditions").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.solar.sky").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.solar.buffer", SolarTier.Panel.getCapacity()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.solar.wiring").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new SolarBlockEntity(SolarTier.Panel, Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return createTickerHelper(Type, SolarRegistry.SolarPanelEntity.get(), World.isClientSide ? SolarBlockEntity::clientTick : SolarBlockEntity::serverTick);
    }
}
