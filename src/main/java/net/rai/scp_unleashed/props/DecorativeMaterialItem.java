package net.rai.scp_unleashed.props;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class DecorativeMaterialItem extends Item {
    public DecorativeMaterialItem(Properties Properties) {
        super(Properties);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        PropTooltips.add(getDescriptionId(), Lines);
    }
}
