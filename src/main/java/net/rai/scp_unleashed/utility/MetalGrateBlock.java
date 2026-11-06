package net.rai.scp_unleashed.utility;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.WaterloggedTransparentBlock;

import java.util.List;

public class MetalGrateBlock extends WaterloggedTransparentBlock {
    public static final MapCodec<MetalGrateBlock> BlockCodec = simpleCodec(MetalGrateBlock::new);

    public MetalGrateBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends WaterloggedTransparentBlock> codec() {
        return BlockCodec;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        UtilityTooltips.describe(this, Lines, "block.scp_unleashed.utility.see_through");
    }
}
