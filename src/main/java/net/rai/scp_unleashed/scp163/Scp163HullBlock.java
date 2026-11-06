package net.rai.scp_unleashed.scp163;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp163HullBlock extends Block {
    public static final MapCodec<Scp163HullBlock> BlockCodec = simpleCodec(Scp163HullBlock::new);
    public static final float Hardness = 4.5f;
    public static final float Resistance = 12.0f;
    public static final int LightLevel = 10;

    public Scp163HullBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends Scp163HullBlock> codec() {
        return BlockCodec;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp163_hull.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.scp163_hull.details").withStyle(ChatFormatting.GOLD));
        }
    }
}
