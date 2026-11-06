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

public class Scp1730SwitchBlock extends Scp1730DeviceBlock {
    public static final MapCodec<Scp1730SwitchBlock> Codec = simpleCodec(Scp1730SwitchBlock::new);
    public static final EnumProperty<Role> Kind = EnumProperty.create("role", Role.class);
    public static final BooleanProperty On = BooleanProperty.create("on");

    public enum Role implements StringRepresentable {
        Interlock("interlock"),
        Fan("fan"),
        RelayRed("relay_red"),
        RelayYellow("relay_yellow"),
        RelayBlue("relay_blue"),
        CellLock("cell_lock"),
        Controller("controller"),
        Key("key");

        private final String Name;

        Role(String Name) {
            this.Name = Name;
        }

        @Override
        public String getSerializedName() {
            return Name;
        }
    }

    public Scp1730SwitchBlock(Properties Props) {
        super(Props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Kind, Role.Interlock).setValue(On, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return Codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Kind, On);
    }
}
