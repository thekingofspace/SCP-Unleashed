package net.rai.scp_unleashed.scp3008.econ.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.scp3008.econ.block.Scp3008LanternBlock;

import java.util.List;

public class Scp3008LanternItem extends BlockItem {
    public Scp3008LanternItem(Block Placing, Properties Settings) {
        super(Placing, Settings);
    }

    public static int charge(ItemStack Stack) {
        BlockItemStateProperties Saved = Stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        Integer Value = Saved.get(Scp3008LanternBlock.Charge);
        return Value == null ? Scp3008LanternBlock.MaxCharge : Value;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        int Percent = charge(Stack) * 100 / Scp3008LanternBlock.MaxCharge;
        Lines.add(Component.translatable(getDescriptionId() + ".charge", Percent).withStyle(Percent > 25 ? ChatFormatting.YELLOW : ChatFormatting.RED));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(getDescriptionId() + ".details").withStyle(ChatFormatting.GOLD));
        }
    }
}
