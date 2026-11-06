package net.rai.scp_unleashed.furnishing;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public final class FurnishingTooltips {
    private FurnishingTooltips() {
    }

    public static void add(Block Owner, List<Component> Lines, String... Details) {
        String Key = Owner.getDescriptionId();
        Lines.add(Component.translatable(Key + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (Details.length > 0 && ItemTooltips.showDetails(Lines)) {
            for (String Detail : Details) {
                Lines.add(Component.translatable(Key + "." + Detail).withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
