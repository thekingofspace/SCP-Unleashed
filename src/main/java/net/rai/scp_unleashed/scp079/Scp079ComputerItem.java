package net.rai.scp_unleashed.scp079;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp079ComputerItem extends BlockItem {
    public Scp079ComputerItem(Block Placed, Properties Properties) {
        super(Placed, Properties);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp079_computer.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.scp079_computer.size").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.scp079_computer.draw", Scp079BlockEntity.BaseDraw).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.scp079_computer.wiring").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.scp079_computer.terminal").withStyle(ChatFormatting.GOLD));
        }
    }
}
