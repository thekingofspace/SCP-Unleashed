package net.rai.scp_unleashed.scp407;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class Scp407PlantBlock extends Scp407GrowthBlock {
    protected final Traits Kind;

    public Scp407PlantBlock(Properties Settings, Traits Kind) {
        super(Settings);
        this.Kind = Kind;
        registerDefaultState(stateDefinition.any().setValue(Wither, 0).setValue(Carpet, true));
    }

    public record Traits(VoxelShape Outline, @Nullable VoxelShape Solid, float Motes, float Glints, float Spores, float Puffs) {
        public static Traits of(VoxelShape Outline, @Nullable VoxelShape Solid, float Motes, float Glints, float Spores, float Puffs) {
            return new Traits(Outline, Solid, Motes, Glints, Spores, Puffs);
        }
    }

    public Traits traits() {
        return Kind;
    }

    public boolean tall() {
        return Kind.Solid() != null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Carpet);
    }

    @Override
    public boolean soft(BlockState State) {
        return true;
    }

    protected VoxelShape outline(BlockState State) {
        return Kind.Outline();
    }

    @Nullable
    protected VoxelShape solid(BlockState State) {
        return Kind.Solid();
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return outline(State);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        VoxelShape Found = solid(State);
        return Found == null ? Shapes.empty() : Found;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        Direction Base = base(State);
        return Base != null && supported(World, Pos, Base);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        if (Side == base(State) && !canSurvive(State, World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(refreshCarpet(State, Side, World, Pos), Side, Neighbour, World, Pos, NeighbourPos);
    }

    @Override
    protected float moteChance(BlockState State) {
        return Kind.Motes();
    }

    @Override
    protected float glintChance(BlockState State) {
        return Kind.Glints();
    }

    @Override
    protected float sporeChance(BlockState State) {
        return Kind.Spores();
    }

    @Override
    protected float puffChance(BlockState State) {
        return Kind.Puffs();
    }
}
