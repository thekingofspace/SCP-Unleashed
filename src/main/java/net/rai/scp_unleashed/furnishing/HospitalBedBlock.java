package net.rai.scp_unleashed.furnishing;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static net.rai.scp_unleashed.furnishing.FurnishingShapes.box;

public class HospitalBedBlock extends BedBlock implements LargeBlock {
    public static final MapCodec<BedBlock> BedCodec = simpleCodec(HospitalBedBlock::new);
    private static final Map<Direction, VoxelShape> HeadOutlines = FurnishingShapes.horizontal(FurnishingShapes.of(
            box(1, 0, 2, 15, 10, 16), box(0, 0, 0, 16, 15, 2), box(0, 7, 4, 1, 13, 13), box(15, 7, 4, 16, 13, 13), box(3, 10, 3, 13, 12, 8)));
    private static final Map<Direction, VoxelShape> FootOutlines = FurnishingShapes.horizontal(FurnishingShapes.of(
            box(1, 0, 0, 15, 11, 14), box(0, 0, 14, 16, 13, 16), box(0, 7, 4, 1, 11, 12), box(15, 7, 4, 16, 11, 12)));

    public HospitalBedBlock(Properties Properties) {
        super(DyeColor.WHITE, Properties);
    }

    @Override
    public MapCodec<BedBlock> codec() {
        return BedCodec;
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return null;
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        return List.of(Pos, Pos.relative(Context.getHorizontalDirection()));
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(PART) == BedPart.HEAD ? HeadOutlines : FootOutlines).get(State.getValue(FACING));
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        FurnishingTooltips.add(this, Lines, "sleep");
    }
}
