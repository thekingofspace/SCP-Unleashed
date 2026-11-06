package net.rai.scp_unleashed.scp1730.puzzle.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class Scp1730ValveBlock extends Scp1730DeviceBlock {
    public static final MapCodec<Scp1730ValveBlock> Codec = simpleCodec(Scp1730ValveBlock::new);
    public static final EnumProperty<Role> Kind = EnumProperty.create("role", Role.class);
    public static final BooleanProperty Open = BooleanProperty.create("open");
    public static final BooleanProperty High = BooleanProperty.create("high");

    public enum Role implements StringRepresentable {
        Inflow("inflow"),
        Bypass("bypass"),
        Drain("drain");

        private final String Name;

        Role(String Name) {
            this.Name = Name;
        }

        @Override
        public String getSerializedName() {
            return Name;
        }
    }

    public Scp1730ValveBlock(Properties Props) {
        super(Props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Kind, Role.Inflow).setValue(Open, false).setValue(High, true));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return Codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Kind, Open, High);
    }
}
