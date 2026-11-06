package net.rai.scp_unleashed.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.rai.scp_unleashed.block.entity.RenderableBlockEntity;
import org.jetbrains.annotations.Nullable;

public class DecoratableBlock extends RenderableBlock {
    public static final MapCodec<DecoratableBlock> DecoratableCodec = simpleCodec(DecoratableBlock::new);
    public static final BooleanProperty Decorated = BooleanProperty.create("decorated");

    public DecoratableBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Decorated, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return DecoratableCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Decorated);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Decorated) ? super.newBlockEntity(Pos, State) : null;
    }

    public static boolean addDecal(Level World, BlockPos Pos, Direction Face, FaceLayer Layer) {
        BlockState State = World.getBlockState(Pos);
        if (World.isClientSide || !(State.getBlock() instanceof DecoratableBlock)) {
            return false;
        }
        if (!State.getValue(Decorated)) {
            World.setBlock(Pos, State.setValue(Decorated, true), Block.UPDATE_ALL);
        }
        if (!(World.getBlockEntity(Pos) instanceof RenderableBlockEntity Entity)) {
            return false;
        }
        Entity.addLayer(Face, Layer);
        return true;
    }

    public static boolean clearDecals(Level World, BlockPos Pos, Direction Face) {
        if (World.isClientSide || !(World.getBlockEntity(Pos) instanceof RenderableBlockEntity Entity) || Entity.getLayers(Face).isEmpty()) {
            return false;
        }
        Entity.clearLayers(Face);
        return true;
    }

    public static boolean clearAllDecals(Level World, BlockPos Pos) {
        if (World.isClientSide || !(World.getBlockEntity(Pos) instanceof RenderableBlockEntity Entity) || !Entity.hasLayers()) {
            return false;
        }
        Entity.clearLayers();
        return true;
    }
}
