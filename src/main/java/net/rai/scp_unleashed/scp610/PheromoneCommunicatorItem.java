package net.rai.scp_unleashed.scp610;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class PheromoneCommunicatorItem extends BlockItem {
    public PheromoneCommunicatorItem(Block Placed, Properties Settings) {
        super(Placed, Settings);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("tooltip.scp_unleashed.pheromone_communicator").withStyle(ChatFormatting.GRAY));
    }
}
