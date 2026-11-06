package net.rai.scp_unleashed.advmaterials;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class MagnetizedBlock extends Block {
    public static final MapCodec<MagnetizedBlock> BlockCodec = simpleCodec(MagnetizedBlock::new);

    public MagnetizedBlock(BlockBehaviour.Properties Properties) {
        super(Properties);
    }

    public static boolean isMagnetized(BlockState State) {
        return State.is(AdvancedMaterialsRegistry.Magnetized);
    }

    public static boolean holds(BlockGetter World, BlockState State) {
        if (State.getBlock() instanceof MagnetizedDoorBlock) {
            return !State.getValue(MagnetizedDoorBlock.Open);
        }
        return isMagnetized(State);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.magnetized_block.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.magnetized_block.detail").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.magnetized_block.made").withStyle(ChatFormatting.GOLD));
        }
    }
}
