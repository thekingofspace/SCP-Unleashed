package net.rai.scp_unleashed.scp024;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp024DoorItem extends BlockItem {
    private final String Key;

    public Scp024DoorItem(Block Door, Properties Settings, String Key) {
        super(Door, Settings);
        this.Key = Key;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + ".tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + ".detail.0").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + ".detail.1").withStyle(ChatFormatting.RED));
        }
    }
}
