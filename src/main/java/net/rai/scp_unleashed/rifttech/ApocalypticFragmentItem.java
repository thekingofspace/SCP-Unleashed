package net.rai.scp_unleashed.rifttech;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class ApocalypticFragmentItem extends Item {
    public static final int StackSize = 1;
    public static final int Gold = ApocalypticRiftEntity.Colour & 0xFFFFFF;
    private static final String Pip = "◆";

    public ApocalypticFragmentItem(Properties Properties) {
        super(Properties);
    }

    @Override
    public Component getName(ItemStack Stack) {
        return Component.translatable(getDescriptionId(Stack)).withStyle(Style.EMPTY.withColor(Gold));
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.apocalyptic_rift_fragment.tooltip").withStyle(ChatFormatting.GRAY));
        MutableComponent Pips = Component.literal(Pip.repeat(FragmentRarity.values().length + 1)).withStyle(Style.EMPTY.withColor(Gold));
        Lines.add(Component.translatable("item.scp_unleashed.unstable_fragment.instability", Pips).withStyle(ChatFormatting.DARK_GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.apocalyptic_rift_fragment.key").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.apocalyptic_rift_fragment.waiting").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.apocalyptic_rift_fragment.keep").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
