package net.rai.scp_unleashed.trinitite;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class TrinititeBlock extends Block {
    public static final MapCodec<TrinititeBlock> BlockCodec = simpleCodec(TrinititeBlock::new);

    public TrinititeBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        String Key = getDescriptionId();
        Lines.add(Component.translatable(Key + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.material.source", Component.translatable(Key + ".source")).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("tooltip.scp_unleashed.material.uses", Component.translatable(Key + ".uses")).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + ".cost", TrinititeRegistry.ChargeCost).withStyle(ChatFormatting.GOLD));
        }
    }
}
