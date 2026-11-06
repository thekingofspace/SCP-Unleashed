package net.rai.scp_unleashed.processing;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemPressBlock extends ProcessingMachineBlock {
    public static final MapCodec<ItemPressBlock> BlockCodec = simpleCodec(ItemPressBlock::new);

    public ItemPressBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected String getMachineName() {
        return "item_press";
    }

    @Override
    protected int getWorkDraw() {
        return ItemPressBlockEntity.WorkDraw;
    }

    @Override
    protected void appendDetails(List<Component> Lines) {
        Lines.add(Component.translatable("block.scp_unleashed.item_press.products").withStyle(ChatFormatting.GOLD));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new ItemPressBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (World.isClientSide) {
            return createTickerHelper(Type, ProcessingRegistry.ItemPressEntity.get(), ProcessingMachineBlockEntity::clientTick);
        }
        return createTickerHelper(Type, ProcessingRegistry.ItemPressEntity.get(), ProcessingMachineBlockEntity::serverTick);
    }
}
