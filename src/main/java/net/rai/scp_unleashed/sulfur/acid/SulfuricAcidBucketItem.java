package net.rai.scp_unleashed.sulfur.acid;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluid;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class SulfuricAcidBucketItem extends BucketItem {
    public static final String Key = "item.scp_unleashed.sulfuric_acid_bucket.";

    public SulfuricAcidBucketItem(Fluid Content, Properties Settings) {
        super(Content, Settings);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "hazard").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "items").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "flow").withStyle(ChatFormatting.GOLD));
        }
    }
}
