package net.rai.scp_unleashed.facilitysign;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class FacilitySignItem extends BlockItem {
    public FacilitySignItem(Block Placed, Properties Settings) {
        super(Placed, Settings);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        boolean Large = getBlock() instanceof FacilitySignBlock;
        boolean Header = getBlock() instanceof FacilityHeaderBlock;
        Lines.add(Component.translatable(FacilitySignSupport.Key + (Header ? "header_tooltip" : Large ? "tooltip" : "small_tooltip")).withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            if (Header) {
                Lines.add(Component.translatable(FacilitySignSupport.Key + "header_centre").withStyle(ChatFormatting.GOLD));
                Lines.add(Component.translatable(FacilitySignSupport.Key + "hang").withStyle(ChatFormatting.GOLD));
                Lines.add(Component.translatable(FacilitySignSupport.Key + "header_room").withStyle(ChatFormatting.DARK_GRAY));
            } else if (Large) {
                Lines.add(Component.translatable(FacilitySignSupport.Key + "arrow").withStyle(ChatFormatting.GOLD));
                Lines.add(Component.translatable(FacilitySignSupport.Key + "hang").withStyle(ChatFormatting.GOLD));
                Lines.add(Component.translatable(FacilitySignSupport.Key + "room").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                Lines.add(Component.translatable(FacilitySignSupport.Key + "wall").withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
