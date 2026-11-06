package net.rai.scp_unleashed.scp409;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp409CaseItem extends BlockItem {
    public static final String Prefix = "block.scp_unleashed.scp409_case";

    public Scp409CaseItem(Block Placed, Properties Settings) {
        super(Placed, Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp409CaseItem;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + ".detail.0").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + ".detail.1").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(Prefix + ".detail.2").withStyle(ChatFormatting.GRAY));
        }
    }
}
