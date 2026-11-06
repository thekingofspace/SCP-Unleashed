package net.rai.scp_unleashed.door;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class KeycardItem extends Item {
    public static final int OmniClearance = 6;

    private final int Clearance;

    public KeycardItem(int Clearance, Properties Properties) {
        super(Properties);
        this.Clearance = Clearance;
    }

    public int getClearance() {
        return Clearance;
    }

    public boolean isOmni() {
        return Clearance >= OmniClearance;
    }

    public boolean opens(int Required) {
        return isOmni() || Clearance >= Required;
    }

    public static ChatFormatting colourOf(int Clearance) {
        return switch (Clearance) {
            case 1 -> ChatFormatting.WHITE;
            case 2 -> ChatFormatting.YELLOW;
            case 3 -> ChatFormatting.GOLD;
            case 4 -> ChatFormatting.RED;
            case 5 -> ChatFormatting.GRAY;
            default -> ChatFormatting.LIGHT_PURPLE;
        };
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Component Rank = isOmni() ? Component.translatable("item.scp_unleashed.keycard.omni_clearance") : Component.translatable("item.scp_unleashed.keycard.clearance", Clearance);
        Lines.add(Rank.copy().withStyle(colourOf(Clearance)));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.keycard.swipe").withStyle(ChatFormatting.GOLD));
            Lines.add((isOmni() ? Component.translatable("item.scp_unleashed.keycard.opens_all") : Component.translatable("item.scp_unleashed.keycard.opens", Clearance)).withStyle(ChatFormatting.GOLD));
        }
    }
}
