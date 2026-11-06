package net.rai.scp_unleashed.materials;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public final class MaterialTooltips {
    private MaterialTooltips() {
    }

    public static void append(String DescriptionId, List<Component> Lines) {
        Lines.add(Component.translatable(DescriptionId + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.material.source", Component.translatable(DescriptionId + ".source")).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("tooltip.scp_unleashed.material.uses", Component.translatable(DescriptionId + ".uses")).withStyle(ChatFormatting.GOLD));
        }
    }
}
