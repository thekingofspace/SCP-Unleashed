package net.rai.scp_unleashed.scp4845.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class Scp4845MarkingBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp4845MarkingBlock> MarkingCodec = simpleCodec(Scp4845MarkingBlock::new);
    public static final EnumProperty<Marking> Paint = EnumProperty.create("marking", Marking.class);

    public enum Marking implements StringRepresentable {
        Line("line"),
        Arrow("arrow");

        public final String Name;

        Marking(String Name) {
            this.Name = Name;
        }

        @Override
        public String getSerializedName() {
            return Name;
        }
    }

    public Scp4845MarkingBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Paint, Marking.Arrow));
    }

    @Override
    protected MapCodec<? extends Scp4845MarkingBlock> codec() {
        return MarkingCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Paint);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection());
    }
}
