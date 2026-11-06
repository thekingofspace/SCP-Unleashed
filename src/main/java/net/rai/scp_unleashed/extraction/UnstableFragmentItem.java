package net.rai.scp_unleashed.extraction;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.anomaly.RiftFragment;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class UnstableFragmentItem extends Item implements RiftFragment {
    public static final int StackSize = 16;
    private static final String Pip = "◆";
    private final FragmentRarity Grade;

    public UnstableFragmentItem(FragmentRarity Grade, Properties Properties) {
        super(Properties);
        this.Grade = Grade;
    }

    @Override
    public FragmentRarity getRarity() {
        return Grade;
    }

    public int getRgb() {
        return Grade.getColour() & 0xFFFFFF;
    }

    @Override
    public Component getName(ItemStack Stack) {
        return Component.translatable(getDescriptionId(Stack)).withStyle(Style.EMPTY.withColor(getRgb()));
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.unstable_fragment.tooltip").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("item.scp_unleashed.unstable_fragment.instability", instability()).withStyle(ChatFormatting.DARK_GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.unstable_fragment.archive").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.unstable_fragment.danger").withStyle(ChatFormatting.GRAY));
        }
    }

    private Component instability() {
        int Count = FragmentRarity.values().length;
        MutableComponent Filled = Component.literal(Pip.repeat(Grade.getTier())).withStyle(Style.EMPTY.withColor(getRgb()));
        MutableComponent Empty = Component.literal(Pip.repeat(Count - Grade.getTier())).withStyle(ChatFormatting.DARK_GRAY);
        return Filled.append(Empty);
    }
}
