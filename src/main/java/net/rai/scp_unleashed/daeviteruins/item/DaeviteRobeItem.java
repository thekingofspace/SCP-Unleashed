package net.rai.scp_unleashed.daeviteruins.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.daeviteruins.DaeviteRuinsRegistry;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class DaeviteRobeItem extends ArmorItem {
    public DaeviteRobeItem(Type Slot, Properties Settings) {
        super(DaeviteRuinsRegistry.RobeCloth, Slot, Settings);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.daevite_robe.set").withStyle(ChatFormatting.GOLD));
        }
    }
}
