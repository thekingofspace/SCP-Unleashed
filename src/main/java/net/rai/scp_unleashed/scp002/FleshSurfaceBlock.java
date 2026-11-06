package net.rai.scp_unleashed.scp002;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class FleshSurfaceBlock extends FleshBlock {
    public static final MapCodec<FleshSurfaceBlock> SurfaceCodec = simpleCodec(FleshSurfaceBlock::new);
    public static final BooleanProperty Awake = BooleanProperty.create("awake");

    public FleshSurfaceBlock(Properties Properties) {
        super(Properties);
        BlockState Default = stateDefinition.any().setValue(Awake, false);
        for (Direction Side : Direction.values()) {
            Default = Default.setValue(inside(Side), false);
        }
        registerDefaultState(Default);
    }

    public static BooleanProperty inside(Direction Side) {
        return PipeBlock.PROPERTY_BY_DIRECTION.get(Side);
    }

    @Override
    protected MapCodec<? extends FleshSurfaceBlock> codec() {
        return SurfaceCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Awake);
        for (Direction Side : Direction.values()) {
            Builder.add(inside(Side));
        }
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        BlockState Turned = State;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Turned = Turned.setValue(inside(Turn.rotate(Side)), State.getValue(inside(Side)));
        }
        return Turned;
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        BlockState Flipped = State;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Flipped = Flipped.setValue(inside(Flip.mirror(Side)), State.getValue(inside(Side)));
        }
        return Flipped;
    }
}
