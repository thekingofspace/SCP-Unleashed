package net.rai.scp_unleashed.creativeoutlet;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.block.OutletBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.wire.ConnectorTier;
import net.rai.scp_unleashed.wire.Wires;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CreativeOutletBlock extends OutletBlock {
    public static final MapCodec<CreativeOutletBlock> CreativeCodec = simpleCodec(CreativeOutletBlock::new);
    public static final String Key = "block.scp_unleashed." + CreativeOutletRegistry.OutletId + ".";

    public CreativeOutletBlock(Properties Settings) {
        super(ConnectorTier.Tether, Settings);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CreativeCodec;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new CreativeOutletBlockEntity(Pos, State);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "supply").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "wires").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.outlet.max_connections", Wires.MaxLinksPerOutlet).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "creative").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
