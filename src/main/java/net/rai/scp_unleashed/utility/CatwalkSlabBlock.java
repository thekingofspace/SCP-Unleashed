package net.rai.scp_unleashed.utility;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.SlabBlock;

import java.util.List;

public class CatwalkSlabBlock extends SlabBlock {
    public static final MapCodec<CatwalkSlabBlock> BlockCodec = simpleCodec(CatwalkSlabBlock::new);

    public CatwalkSlabBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    public MapCodec<? extends SlabBlock> codec() {
        return BlockCodec;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        UtilityTooltips.describe(this, Lines, "placement", "block.scp_unleashed.utility.see_through");
    }
}
