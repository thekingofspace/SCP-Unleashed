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

public class BollardBlock extends FacingStructureBlock {
    public static final MapCodec<BollardBlock> BlockCodec = simpleCodec(BollardBlock::new);
    private static final Map<Direction, VoxelShape> BollardShapes = UtilityShapes.horizontal(new double[]{4, 0, 4, 12, 1, 12}, new double[]{5, 1, 5, 11, 14, 11});

    public BollardBlock(Properties Properties) {
        super(Properties, BollardShapes);
    }

    @Override
    protected MapCodec<? extends BollardBlock> codec() {
        return BlockCodec;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        UtilityTooltips.describe(this, Lines);
    }
}
