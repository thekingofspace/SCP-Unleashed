package net.rai.scp_unleashed.decor;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.rai.scp_unleashed.block.DecoratableBlock;

import java.util.List;

public class FacilityWallBlock extends DecoratableBlock {
    public static final MapCodec<FacilityWallBlock> WallCodec = simpleCodec(FacilityWallBlock::new);

    public FacilityWallBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return WallCodec;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.facility_wall.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
