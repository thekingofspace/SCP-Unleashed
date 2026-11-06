package net.rai.scp_unleashed.scp093;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp093DiscItem extends Item {
    public static final String Prefix = "item.scp_unleashed.scp093_disc.";

    public Scp093DiscItem(Properties Settings) {
        super(Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp093DiscItem;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "seat").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "take").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "store").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
