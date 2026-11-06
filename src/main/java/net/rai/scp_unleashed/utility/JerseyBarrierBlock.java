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

public class JerseyBarrierBlock extends FacingStructureBlock {
    public static final MapCodec<JerseyBarrierBlock> BlockCodec = simpleCodec(JerseyBarrierBlock::new);
    private static final Map<Direction, VoxelShape> JerseyShapes = UtilityShapes.horizontal(
            new double[]{0, 0, 1, 16, 2, 15},
            new double[]{0, 2, 2, 16, 4, 14},
            new double[]{0, 4, 3, 16, 6, 13},
            new double[]{0, 6, 4, 16, 10, 12},
            new double[]{0, 10, 5, 16, 14, 11});

    public JerseyBarrierBlock(Properties Properties) {
        super(Properties, JerseyShapes);
    }

    @Override
    protected MapCodec<? extends JerseyBarrierBlock> codec() {
        return BlockCodec;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        UtilityTooltips.describe(this, Lines, "block.scp_unleashed.utility.faces_you");
    }
}
