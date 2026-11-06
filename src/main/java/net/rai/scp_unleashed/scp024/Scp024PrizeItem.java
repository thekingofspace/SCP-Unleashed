package net.rai.scp_unleashed.scp024;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp024PrizeItem extends Item {
    private final String Key;
    private final int Details;

    public Scp024PrizeItem(Properties Settings, String Key, int Details) {
        super(Settings);
        this.Key = Key;
        this.Details = Details;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + ".tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            for (int Index = 0; Index < Details; Index++) {
                Lines.add(Component.translatable(Key + ".detail." + Index).withStyle(Index == Details - 1 ? ChatFormatting.DARK_GRAY : ChatFormatting.GOLD));
            }
        }
    }
}
