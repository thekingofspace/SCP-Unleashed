package net.rai.scp_unleashed.scp002;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class MeteoricShellBlock extends Block {
    public static final MapCodec<MeteoricShellBlock> BlockCodec = simpleCodec(MeteoricShellBlock::new);

    public MeteoricShellBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends MeteoricShellBlock> codec() {
        return BlockCodec;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp002_shell.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.scp002_shell.details").withStyle(ChatFormatting.GOLD));
        }
    }
}
