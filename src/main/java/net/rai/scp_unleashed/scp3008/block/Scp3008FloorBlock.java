package net.rai.scp_unleashed.scp3008.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class Scp3008FloorBlock extends Scp3008SealedBlock {
    public static final MapCodec<Scp3008FloorBlock> FloorCodec = simpleCodec(Scp3008FloorBlock::new);
    public static final EnumProperty<Style> Finish = EnumProperty.create("style", Style.class);

    public enum Style implements StringRepresentable {
        Concrete("concrete"),
        Living("living"),
        Dining("dining"),
        Kitchen("kitchen"),
        Bedroom("bedroom"),
        Bathroom("bathroom"),
        Office("office"),
        Children("children"),
        Lighting("lighting"),
        Textiles("textiles"),
        Outdoor("outdoor"),
        Warehouse("warehouse"),
        Electronics("electronics"),
        FoodCourt("food_court"),
        RugGrey("rug_grey"),
        RugGreyEdge("rug_grey_edge"),
        RugBeige("rug_beige"),
        RugBeigeEdge("rug_beige_edge"),
        RugCream("rug_cream"),
        RugCreamEdge("rug_cream_edge"),
        RugBlue("rug_blue"),
        RugBlueEdge("rug_blue_edge"),
        RugGreen("rug_green"),
        RugGreenEdge("rug_green_edge"),
        RugRust("rug_rust"),
        RugRustEdge("rug_rust_edge"),
        RugCharcoal("rug_charcoal"),
        RugCharcoalEdge("rug_charcoal_edge"),
        RugYellow("rug_yellow"),
        RugYellowEdge("rug_yellow_edge"),
        RugPink("rug_pink"),
        RugPinkEdge("rug_pink_edge"),
        RugTeal("rug_teal"),
        RugTealEdge("rug_teal_edge");

        private static final Style[] Ordered = values();
        public final String Name;

        Style(String Name) {
            this.Name = Name;
        }

        public static Style of(int Index) {
            return Ordered[Math.max(0, Math.min(Ordered.length - 1, Index))];
        }

        @Override
        public String getSerializedName() {
            return Name;
        }
    }

    public Scp3008FloorBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Finish, Style.Concrete));
    }

    @Override
    protected MapCodec<? extends Scp3008FloorBlock> codec() {
        return FloorCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Finish);
    }
}
