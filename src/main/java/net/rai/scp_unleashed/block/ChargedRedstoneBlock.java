package net.rai.scp_unleashed.block;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.wire.ChargingRedstone;

import java.util.List;

public class ChargedRedstoneBlock extends Block {
    public ChargedRedstoneBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.charged_redstone.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.charged_redstone.cost", ChargingRedstone.ChargeCost).withStyle(ChatFormatting.GOLD));
        }
    }
}
