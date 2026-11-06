package net.rai.scp_unleashed.materials;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class MaterialBlock extends Block {
    public static final MapCodec<MaterialBlock> BlockCodec = simpleCodec(MaterialBlock::new);

    public MaterialBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        MaterialTooltips.append(getDescriptionId(), Lines);
    }
}
