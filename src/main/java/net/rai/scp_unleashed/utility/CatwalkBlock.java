package net.rai.scp_unleashed.utility;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class CatwalkBlock extends UtilityWaterloggedBlock {
    public static final MapCodec<CatwalkBlock> BlockCodec = simpleCodec(CatwalkBlock::new);
    public static final VoxelShape Plate = Block.box(0, 0, 0, 16, 2, 16);

    public CatwalkBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends CatwalkBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Plate;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState Existing = Context.getLevel().getBlockState(Context.getClickedPos());
        if (Existing.getBlock() instanceof SafetyRailingBlock && !Existing.getValue(SafetyRailingBlock.Catwalk)) {
            return Existing.setValue(SafetyRailingBlock.Catwalk, true);
        }
        return watered(defaultBlockState(), Context);
    }

    @Override
    protected boolean canBeReplaced(BlockState State, BlockPlaceContext Context) {
        if (Context.getItemInHand().getItem() instanceof BlockItem Held && Held.getBlock() instanceof SafetyRailingBlock) {
            return true;
        }
        return super.canBeReplaced(State, Context);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        UtilityTooltips.describe(this, Lines, "railings", "block.scp_unleashed.utility.see_through");
    }
}
