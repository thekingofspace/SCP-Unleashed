package net.rai.scp_unleashed.scp4845.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class Scp4845SignBlock extends Scp4845TiledBlock {
    public static final MapCodec<Scp4845SignBlock> SignCodec = simpleCodec(Scp4845SignBlock::new);
    public static final int Columns = 6;
    public static final int Rows = 3;
    public static final IntegerProperty Tile = IntegerProperty.create("tile", 0, Columns * Rows - 1);
    private static final Map<Direction, VoxelShape> Shape = Scp4845Shapes.facings(Scp4845Shapes.Panel);

    public Scp4845SignBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Tile, 0));
    }

    @Override
    protected MapCodec<? extends Scp4845SignBlock> codec() {
        return SignCodec;
    }

    @Override
    public int columns() {
        return Columns;
    }

    @Override
    public int rows() {
        return Rows;
    }

    @Override
    public IntegerProperty tile() {
        return Tile;
    }

    @Override
    protected Map<Direction, VoxelShape> shapeOf(int Index) {
        return Shape;
    }
}
