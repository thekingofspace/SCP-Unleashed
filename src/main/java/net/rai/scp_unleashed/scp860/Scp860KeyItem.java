package net.rai.scp_unleashed.scp860;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class Scp860KeyItem extends Item {
    public Scp860KeyItem(Properties Settings) {
        super(Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp860KeyItem;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.scp860_key.tooltip").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("item.scp_unleashed.scp860_key.detail").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
