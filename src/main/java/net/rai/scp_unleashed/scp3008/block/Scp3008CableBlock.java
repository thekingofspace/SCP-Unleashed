package net.rai.scp_unleashed.scp3008.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Scp3008CableBlock extends Scp3008SealedBlock {
    public static final MapCodec<Scp3008CableBlock> CableCodec = simpleCodec(Scp3008CableBlock::new);
    public static final VoxelShape Shape = Block.box(7, 0, 7, 9, 16, 9);
    public static final VoxelShape ClampShape = Shapes.or(Shape, Block.box(6, 0, 6, 10, 3, 10));
    public static final VoxelShape CanopyShape = Shapes.or(Shape, Block.box(5, 14, 5, 11, 16, 11));
    public static final EnumProperty<Fitting> Fit = EnumProperty.create("fitting", Fitting.class);

    public enum Fitting implements StringRepresentable {
        Span("span"), Clamp("clamp"), Canopy("canopy");

        private final String Name;

        Fitting(String Name) {
            this.Name = Name;
        }

        @Override
        public String getSerializedName() {
            return Name;
        }

        public static Fitting of(int Index) {
            Fitting[] All = values();
            return All[Math.max(0, Math.min(All.length - 1, Index))];
        }
    }

    public Scp3008CableBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Fit, Fitting.Span));
    }

    @Override
    protected MapCodec<? extends Scp3008CableBlock> codec() {
        return CableCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Fit);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return switch (State.getValue(Fit)) {
            case Clamp -> ClampShape;
            case Canopy -> CanopyShape;
            default -> Shape;
        };
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return true;
    }
}
