package net.rai.scp_unleashed.scp420j;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class Scp420jLeafItem extends Item {
    public Scp420jLeafItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.scp420j_leaf.tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
