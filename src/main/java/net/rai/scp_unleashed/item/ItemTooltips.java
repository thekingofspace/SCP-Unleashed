package net.rai.scp_unleashed.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ItemTooltips {
    private ItemTooltips() {
    }

    public static boolean showDetails(List<Component> Lines) {
        if (Screen.hasShiftDown()) {
            return true;
        }
        Lines.add(Component.translatable("tooltip.scp_unleashed.hold_shift", Component.translatable("tooltip.scp_unleashed.shift").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.DARK_GRAY));
        return false;
    }
}
