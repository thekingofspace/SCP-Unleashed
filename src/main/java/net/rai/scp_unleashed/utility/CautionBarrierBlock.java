package net.rai.scp_unleashed.utility;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Map;

public class CautionBarrierBlock extends FacingStructureBlock {
    public static final MapCodec<CautionBarrierBlock> BlockCodec = simpleCodec(CautionBarrierBlock::new);
    private static final Map<Direction, VoxelShape> BarrierShapes = UtilityShapes.horizontal(new double[]{0, 0, 2, 16, 16, 14});

    public CautionBarrierBlock(Properties Properties) {
        super(Properties, BarrierShapes);
    }

    @Override
    protected MapCodec<? extends CautionBarrierBlock> codec() {
        return BlockCodec;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        UtilityTooltips.describe(this, Lines, "block.scp_unleashed.utility.faces_you");
    }
}
