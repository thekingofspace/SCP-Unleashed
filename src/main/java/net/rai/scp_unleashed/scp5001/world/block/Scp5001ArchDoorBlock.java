package net.rai.scp_unleashed.scp5001.world.block;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.rai.scp_unleashed.scp5001.world.Scp5001WorldRegistry;

import java.util.List;

public class Scp5001ArchDoorBlock extends Scp5001PanelDoorBlock {
    public static final int Columns = 2;
    public static final int Rows = 3;
    public static final IntegerProperty Column = IntegerProperty.create("column", 0, Columns - 1);
    public static final IntegerProperty Row = IntegerProperty.create("row", 0, Rows - 1);

    public Scp5001ArchDoorBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, net.minecraft.core.Direction.NORTH).setValue(Open, false).setValue(Column, 0).setValue(Row, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Open, Column, Row);
    }

    @Override
    public IntegerProperty column() {
        return Column;
    }

    @Override
    public IntegerProperty row() {
        return Row;
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
    public double thickness() {
        return 4.0;
    }

    @Override
    protected SoundEvent openSound() {
        return Scp5001WorldRegistry.DoorOpen.get();
    }

    @Override
    protected SoundEvent closeSound() {
        return Scp5001WorldRegistry.DoorClose.get();
    }

    @Override
    protected List<SoundEvent> extraSounds(boolean Opening) {
        return List.of(Scp5001WorldRegistry.DoorChime.get());
    }
}
