package net.rai.scp_unleashed.scp999;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.props.PropTooltips;

import java.util.List;

public class PinkDogBowlItem extends BlockItem {
    public PinkDogBowlItem(Block Owner, Properties Settings) {
        super(Owner, Settings);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        PropTooltips.add(getBlock(), Lines, "fill", "take");
    }
}
