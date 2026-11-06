package net.rai.scp_unleashed.props;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public final class PropTooltips {
    private PropTooltips() {
    }

    public static void add(Block Owner, List<Component> Lines, String... Details) {
        add(Owner.getDescriptionId(), Lines, Details);
    }

    public static void add(String Key, List<Component> Lines, String... Details) {
        Lines.add(Component.translatable(Key + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (Details.length > 0 && ItemTooltips.showDetails(Lines)) {
            for (String Detail : Details) {
                Lines.add(Component.translatable(Key + "." + Detail).withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
