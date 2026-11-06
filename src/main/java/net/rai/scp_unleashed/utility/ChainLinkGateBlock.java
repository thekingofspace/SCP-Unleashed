package net.rai.scp_unleashed.utility;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Map;

public class ChainLinkGateBlock extends FenceGateBlock {
    public static final MapCodec<FenceGateBlock> BlockCodec = simpleCodec(ChainLinkGateBlock::new);
    private static final Map<Direction, VoxelShape> ClosedShapes = UtilityShapes.horizontal(new double[]{0, 0, 6, 16, 16, 10});
    private static final Map<Direction, VoxelShape> OpenShapes = UtilityShapes.horizontal(
            new double[]{0, 0, 6, 2, 16, 10},
            new double[]{14, 0, 6, 16, 16, 10},
            new double[]{2, 0, 2, 4, 16, 8},
            new double[]{12, 0, 2, 14, 16, 8});

    public ChainLinkGateBlock(Properties Properties) {
        super(Properties, SoundEvents.IRON_TRAPDOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE);
    }

    @Override
    public MapCodec<FenceGateBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(OPEN) ? OpenShapes : ClosedShapes).get(State.getValue(FACING));
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        UtilityTooltips.describe(this, Lines, "use", "block.scp_unleashed.utility.unjumpable");
    }
}
