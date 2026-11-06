package net.rai.scp_unleashed.scp3008.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Scp3008ShapedBlock extends Block {
    public static final MapCodec<Scp3008ShapedBlock> ShapedCodec = simpleCodec(Settings -> new Scp3008ShapedBlock(Settings, new double[][]{{0, 0, 0, 16, 16, 16}}));
    public static final IntegerProperty Variant = IntegerProperty.create("variant", 0, 3);
    private final VoxelShape Outline;

    public Scp3008ShapedBlock(Properties Settings, double[][] Boxes) {
        super(Settings);
        this.Outline = Scp3008Shapes.boxes(Boxes);
        registerDefaultState(stateDefinition.any().setValue(Variant, 0));
    }

    @Override
    protected MapCodec<? extends Scp3008ShapedBlock> codec() {
        return ShapedCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Variant);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outline;
    }
}
