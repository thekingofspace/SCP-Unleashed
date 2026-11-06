package net.rai.scp_unleashed.scp420j;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class Scp420jSeedsItem extends ItemNameBlockItem {
    public Scp420jSeedsItem(Block Planted, Properties Settings) {
        super(Planted, Settings);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.scp420j_seeds.tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
