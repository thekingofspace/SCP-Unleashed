package net.rai.scp_unleashed.scp5001.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class Scp5001GlyphBlock extends Block {
    public static final IntegerProperty Glyph = IntegerProperty.create("glyph", 0, 2);

    public Scp5001GlyphBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Glyph, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Glyph);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Glyph, Math.floorMod(Context.getClickedPos().hashCode(), 3));
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return Scp5001WallFaces.occlusion(State);
    }

    @Override
    protected int getLightBlock(BlockState State, BlockGetter World, BlockPos Pos) {
        return World.getMaxLightLevel();
    }
}
