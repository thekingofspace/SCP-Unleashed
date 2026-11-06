package net.rai.scp_unleashed.scp3008.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class Scp3008FacadeBlock extends Scp3008SealedBlock {
    public static final MapCodec<Scp3008FacadeBlock> FacadeCodec = simpleCodec(Scp3008FacadeBlock::new);
    public static final EnumProperty<Finish> Surface = EnumProperty.create("finish", Finish.class);

    public enum Finish implements StringRepresentable {
        Blue("blue"),
        Yellow("yellow"),
        Plinth("plinth"),
        Roof("roof"),
        Paving("paving"),
        Grey("grey");

        public final String Name;

        Finish(String Name) {
            this.Name = Name;
        }

        @Override
        public String getSerializedName() {
            return Name;
        }
    }

    public Scp3008FacadeBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Surface, Finish.Blue));
    }

    @Override
    protected MapCodec<? extends Scp3008FacadeBlock> codec() {
        return FacadeCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Surface);
    }
}
