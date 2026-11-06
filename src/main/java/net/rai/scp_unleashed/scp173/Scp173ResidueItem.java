package net.rai.scp_unleashed.scp173;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp173ResidueItem extends BlockItem {
    public Scp173ResidueItem(Block Placed, Properties Properties) {
        super(Placed, Properties);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp173_residue.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.scp173_residue.clean").withStyle(ChatFormatting.YELLOW));
            Lines.add(Component.translatable("block.scp_unleashed.scp173_residue.warning").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
