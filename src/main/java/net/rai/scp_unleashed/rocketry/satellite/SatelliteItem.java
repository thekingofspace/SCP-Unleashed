package net.rai.scp_unleashed.rocketry.satellite;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class SatelliteItem extends Item {
    public SatelliteItem(Properties Settings) {
        super(Settings);
    }

    public static boolean isPayload(ItemStack Stack) {
        return Stack.getItem() instanceof SatelliteItem;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.satellite.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.satellite.load").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.satellite.launch").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.satellite.name").withStyle(ChatFormatting.GOLD));
        }
    }
}
