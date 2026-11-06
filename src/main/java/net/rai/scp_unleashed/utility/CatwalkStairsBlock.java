package net.rai.scp_unleashed.utility;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.List;

public class CatwalkStairsBlock extends StairBlock {
    public static final MapCodec<CatwalkStairsBlock> BlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            BlockState.CODEC.fieldOf("base_state").forGetter(Stairs -> Stairs.baseState),
            propertiesCodec()
    ).apply(Instance, CatwalkStairsBlock::new));

    public CatwalkStairsBlock(BlockState BaseState, Properties Properties) {
        super(BaseState, Properties);
    }

    @Override
    public MapCodec<? extends StairBlock> codec() {
        return BlockCodec;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState Placed = super.getStateForPlacement(Context);
        if (Placed == null || Placed.getValue(HALF) == Half.BOTTOM) {
            return Placed;
        }
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        BlockPos Beside = Pos.relative(Direction.NORTH);
        return updateShape(Placed.setValue(HALF, Half.BOTTOM), Direction.NORTH, World.getBlockState(Beside), World, Pos, Beside);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        UtilityTooltips.describe(this, Lines, "corners", "block.scp_unleashed.utility.see_through");
    }
}
