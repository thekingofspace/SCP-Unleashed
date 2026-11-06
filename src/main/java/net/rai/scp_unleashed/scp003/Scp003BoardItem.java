package net.rai.scp_unleashed.scp003;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp003BoardItem extends BlockItem {
    public Scp003BoardItem(Block Placed, Properties Properties) {
        super(Placed, Properties);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.scp003_board.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.scp003_board.safe").withStyle(ChatFormatting.AQUA));
            Lines.add(Component.translatable("item.scp_unleashed.scp003_board.active").withStyle(ChatFormatting.RED));
            Lines.add(Component.translatable("item.scp_unleashed.scp003.together").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
    }
}
