package net.rai.scp_unleashed.daeviteruins.block;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class DaeviteRuinBlockItem extends BlockItem {
    public DaeviteRuinBlockItem(Block Placed, Properties Settings) {
        super(Placed, Settings);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.daevite_ruin.found").withStyle(ChatFormatting.GOLD));
        }
    }
}
