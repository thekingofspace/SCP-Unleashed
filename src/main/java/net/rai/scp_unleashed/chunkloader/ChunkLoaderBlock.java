package net.rai.scp_unleashed.chunkloader;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.phys.BlockHitResult;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChunkLoaderBlock extends BaseEntityBlock {
    public static final MapCodec<ChunkLoaderBlock> BlockCodec = simpleCodec(ChunkLoaderBlock::new);
    public static final BooleanProperty Active = BooleanProperty.create("active");
    public static final int ActiveLight = 6;
    public static final int[] ExampleRadii = {0, 2, 8};

    public ChunkLoaderBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Active, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Active);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World instanceof ServerLevel ServerWorld && World.getBlockEntity(Pos) instanceof ChunkLoaderBlockEntity Loader) {
            Loader.removed(ServerWorld);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && User instanceof ServerPlayer Server && World.getBlockEntity(Pos) instanceof ChunkLoaderBlockEntity Loader) {
            Server.openMenu(Loader, Buffer -> Buffer.writeBlockPos(Pos));
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.chunk_loader.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            int Max = ChunkLoaderConfig.maxRadius();
            int Side = ChunkLoaderArea.side(Max);
            Lines.add(Component.translatable("block.scp_unleashed.chunk_loader.size", Max, Side, Side).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.chunk_loader.draw", ChunkLoaderConfig.basePower(), ChunkLoaderConfig.powerPerChunk()).withStyle(ChatFormatting.GOLD));
            Object[] Examples = new Object[ExampleRadii.length * 2];
            for (int Index = 0; Index < ExampleRadii.length; Index++) {
                int Shown = ChunkLoaderArea.side(ExampleRadii[Index]);
                Examples[Index * 2] = Shown + "x" + Shown;
                Examples[Index * 2 + 1] = ChunkLoaderConfig.demand(ExampleRadii[Index]);
            }
            Lines.add(Component.translatable("block.scp_unleashed.chunk_loader.examples", Examples).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.chunk_loader.grace").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.chunk_loader.wiring").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.chunk_loader.outline").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new ChunkLoaderBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return createTickerHelper(Type, ChunkLoaderRegistry.ChunkLoaderEntity.get(), World.isClientSide ? ChunkLoaderBlockEntity::clientTick : ChunkLoaderBlockEntity::serverTick);
    }
}
