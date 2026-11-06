package net.rai.scp_unleashed.containment;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.rai.scp_unleashed.block.DecoratableBlock;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class ContainmentBlock extends DecoratableBlock {
    public static final MapCodec<ContainmentBlock> BlockCodec = simpleCodec(ContainmentBlock::new);

    public ContainmentBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.containment.blast_proof").withStyle(ChatFormatting.GOLD));
        }
    }
}
