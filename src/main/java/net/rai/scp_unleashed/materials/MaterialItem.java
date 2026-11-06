package net.rai.scp_unleashed.materials;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MaterialItem extends Item {
    public MaterialItem(Properties Properties) {
        super(Properties);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        MaterialTooltips.append(getDescriptionId(), Lines);
    }
}
