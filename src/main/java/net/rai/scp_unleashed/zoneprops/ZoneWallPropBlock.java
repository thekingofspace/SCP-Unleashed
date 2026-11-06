package net.rai.scp_unleashed.zoneprops;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.props.PropMounts;
import net.rai.scp_unleashed.props.PropWater;
import org.jetbrains.annotations.Nullable;

public class ZoneWallPropBlock extends ZonePropBlock {
    public static final MapCodec<ZoneWallPropBlock> WallCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            ZonePropKind.KindCodec.fieldOf("kind").forGetter(ZonePropBlock::getKind), propertiesCodec()).apply(Instance, ZoneWallPropBlock::new));

    public ZoneWallPropBlock(ZonePropKind Kind, Properties Properties) {
        super(Kind, Properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return WallCodec;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        for (Direction Side : Context.getNearestLookingDirections()) {
            if (!Side.getAxis().isHorizontal()) {
                continue;
            }
            BlockState Candidate = defaultBlockState().setValue(FACING, Side.getOpposite());
            if (Candidate.canSurvive(Context.getLevel(), Context.getClickedPos())) {
                return PropWater.placed(Candidate, Context);
            }
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return PropMounts.onWall(World, Pos, State.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == State.getValue(FACING).getOpposite() && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }
}
