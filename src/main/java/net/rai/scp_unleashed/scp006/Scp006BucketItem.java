package net.rai.scp_unleashed.scp006;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

public class Scp006BucketItem extends BucketItem {
    public static final String Key = "item.scp_unleashed.scp006_bucket.";

    public Scp006BucketItem(Fluid Content, Properties Settings) {
        super(Content, Settings);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
