package net.rai.scp_unleashed.scp3008.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class Scp3008FixtureBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp3008FixtureBlock> FixtureCodec = simpleCodec(Settings -> new Scp3008FixtureBlock(Settings, new double[][]{{0, 0, 0, 16, 16, 16}}, true));
    public static final IntegerProperty Variant = IntegerProperty.create("variant", 0, 7);
    private final Map<Direction, VoxelShape> Outline;
    private final boolean Solid;

    public Scp3008FixtureBlock(Properties Settings, double[][] North, boolean Solid) {
        super(Settings);
        this.Outline = Scp3008Shapes.facings(North);
        this.Solid = Solid;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Variant, 0));
    }

    @Override
    protected MapCodec<? extends Scp3008FixtureBlock> codec() {
        return FixtureCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Variant);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outline.get(State.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Solid ? Outline.get(State.getValue(FACING)) : Shapes.empty();
    }
}
