package net.rai.scp_unleashed.utility;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.LadderBlock;

import java.util.List;

public class CagedLadderTopBlock extends LadderBlock {
    public static final MapCodec<LadderBlock> TopCodec = simpleCodec(CagedLadderTopBlock::new);

    public CagedLadderTopBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    public MapCodec<LadderBlock> codec() {
        return TopCodec;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        UtilityTooltips.describe(this, Lines, "exit", "block.scp_unleashed.caged_ladder.cage", "block.scp_unleashed.utility.ladder_mount");
    }
}
