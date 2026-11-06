package net.rai.scp_unleashed.scp093;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp093MirrorItem extends BlockItem {
    public static final String Prefix = "block.scp_unleashed.scp093_mirror.";

    public Scp093MirrorItem(Block Placed, Properties Settings) {
        super(Placed, Settings);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "size").withStyle(ChatFormatting.DARK_GRAY));
            Lines.add(Component.translatable(Prefix + "slot").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "take").withStyle(ChatFormatting.GOLD));
        }
    }
}
