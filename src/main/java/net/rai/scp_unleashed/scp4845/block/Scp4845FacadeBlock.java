package net.rai.scp_unleashed.scp4845.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class Scp4845FacadeBlock extends Block {
    public static final MapCodec<Scp4845FacadeBlock> FacadeCodec = simpleCodec(Scp4845FacadeBlock::new);
    public static final EnumProperty<Finish> Surface = EnumProperty.create("finish", Finish.class);
    public static final int LampLight = 15;

    public enum Finish implements StringRepresentable {
        StuccoPurple("stucco_purple"),
        StuccoWhite("stucco_white"),
        TrimMagenta("trim_magenta"),
        Cornice("cornice"),
        Roof("roof"),
        Foundation("foundation"),
        Sidewalk("sidewalk"),
        DiningTile("dining_tile"),
        KitchenTile("kitchen_tile"),
        OfficeCarpet("office_carpet"),
        WallLower("wall_lower"),
        WallUpper("wall_upper"),
        KitchenWall("kitchen_wall"),
        Ceiling("ceiling"),
        CeilingLight("ceiling_light");

        public final String Name;

        Finish(String Name) {
            this.Name = Name;
        }

        @Override
        public String getSerializedName() {
            return Name;
        }
    }

    public Scp4845FacadeBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Surface, Finish.StuccoPurple));
    }

    public static int light(BlockState State) {
        return State.getValue(Surface) == Finish.CeilingLight ? LampLight : 0;
    }

    @Override
    protected MapCodec<? extends Scp4845FacadeBlock> codec() {
        return FacadeCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Surface);
    }
}
