package net.rai.scp_unleashed.scp465;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class Scp465PartyBlockItem extends BlockItem {
    public static final String TooltipKey = "block.scp_unleashed.scp465_party.tooltip";

    public Scp465PartyBlockItem(Block Placed, Properties Settings) {
        super(Placed, Settings);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(TooltipKey).withStyle(ChatFormatting.GRAY));
    }
}
