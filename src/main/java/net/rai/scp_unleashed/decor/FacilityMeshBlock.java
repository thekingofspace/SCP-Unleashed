package net.rai.scp_unleashed.decor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.DecoratableBlock;

public class FacilityMeshBlock extends DecoratableBlock {
    public static final MapCodec<FacilityMeshBlock> MeshCodec = simpleCodec(FacilityMeshBlock::new);

    public FacilityMeshBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MeshCodec;
    }

    @Override
    protected VoxelShape getVisualShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    protected float getShadeBrightness(BlockState State, BlockGetter World, BlockPos Pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return true;
    }

    @Override
    protected boolean skipRendering(BlockState State, BlockState Adjacent, Direction Side) {
        return Adjacent.is(this) || super.skipRendering(State, Adjacent, Side);
    }
}
