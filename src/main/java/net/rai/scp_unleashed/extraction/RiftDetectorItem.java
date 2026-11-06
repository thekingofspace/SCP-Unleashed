package net.rai.scp_unleashed.extraction;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.goggles.Goggles;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class RiftDetectorItem extends Item {
    public static final double BaseRange = 48.0;
    public static final double RangePerTier = 16.0;

    public RiftDetectorItem(Properties Properties) {
        super(Properties);
    }

    public static double rangeFor(FragmentRarity Grade) {
        return BaseRange + RangePerTier * Grade.getTier();
    }

    public static double maxRange() {
        return rangeFor(FragmentRarity.Abyssal);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.rift_detector.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.rift_detector.range", (int) rangeFor(FragmentRarity.Faint), (int) rangeFor(FragmentRarity.Abyssal)).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.rift_detector.clicks").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.rift_detector.hands").withStyle(ChatFormatting.GRAY));
            Goggles.addSlotLine(Lines, Goggles.Kind.RiftDetector);
        }
    }
}
