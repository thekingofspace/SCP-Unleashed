package net.rai.scp_unleashed.scp335;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.computer.FloppyDiscItem;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp335DiskItem extends FloppyDiscItem {
    public static final String Key = "item.scp_unleashed.scp335_disk";

    public Scp335DiskItem(Properties Properties) {
        super(Properties);
    }

    @Override
    public boolean isWritable(ItemStack Stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + ".file", Scp335Image.FileName).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + ".locked").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + ".use").withStyle(ChatFormatting.GOLD));
        }
    }
}
