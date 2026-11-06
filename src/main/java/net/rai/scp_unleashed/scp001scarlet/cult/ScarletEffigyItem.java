package net.rai.scp_unleashed.scp001scarlet.cult;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ScarletEffigyItem extends Item {
    public ScarletEffigyItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.scp001_scarlet_effigy.lore").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }
}
