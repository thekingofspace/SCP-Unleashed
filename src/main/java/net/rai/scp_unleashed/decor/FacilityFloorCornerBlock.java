package net.rai.scp_unleashed.decor;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class FacilityFloorCornerBlock extends FacilityFloorMarkBlock {
    public static final MapCodec<FacilityFloorCornerBlock> CornerCodec = simpleCodec(FacilityFloorCornerBlock::new);

    public FacilityFloorCornerBlock(Properties Settings) {
        super(Settings);
    }

    public static Direction cornerFacing(Direction Ahead, float Yaw) {
        Direction Right = Ahead.getClockWise();
        float Radians = Yaw * Mth.DEG_TO_RAD;
        float Lean = -Mth.sin(Radians) * Right.getStepX() + Mth.cos(Radians) * Right.getStepZ();
        return Lean >= 0.0f ? Ahead : Ahead.getCounterClockWise();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CornerCodec;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, cornerFacing(Context.getHorizontalDirection(), Context.getRotation()));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        if (Flip == Mirror.NONE) {
            return State;
        }
        return State.setValue(Facing, Flip.mirror(State.getValue(Facing).getClockWise()));
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.facility_floor_corner.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
