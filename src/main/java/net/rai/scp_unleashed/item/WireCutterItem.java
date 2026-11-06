package net.rai.scp_unleashed.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.Tags;

import java.util.List;

public class WireCutterItem extends Item {
    public static final int Durability = 238;

    public WireCutterItem(Properties Properties) {
        super(Properties);
    }

    @Override
    public boolean isValidRepairItem(ItemStack Stack, ItemStack Repair) {
        return Repair.is(Tags.Items.INGOTS_IRON) || super.isValidRepairItem(Stack, Repair);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.wire_cutter.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.wire_cutter.snip").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.wire_cutter.refund").withStyle(ChatFormatting.GOLD));
        }
    }
}
