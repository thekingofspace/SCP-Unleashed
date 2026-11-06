package net.rai.scp_unleashed.utility;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public final class UtilityTooltips {
    private UtilityTooltips() {
    }

    public static void describe(Block Described, List<Component> Lines, String... Details) {
        String Key = Described.getDescriptionId();
        Lines.add(Component.translatable(Key + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (Details.length > 0 && ItemTooltips.showDetails(Lines)) {
            for (String Detail : Details) {
                Lines.add(Component.translatable(Detail.startsWith("block.") ? Detail : Key + "." + Detail).withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
