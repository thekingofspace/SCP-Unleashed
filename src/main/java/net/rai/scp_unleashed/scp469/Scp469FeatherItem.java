package net.rai.scp_unleashed.scp469;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp469FeatherItem extends Item {
    public static final String Key = "item.scp_unleashed.scp469_feather.";

    public Scp469FeatherItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "detail.0").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "detail.1").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
