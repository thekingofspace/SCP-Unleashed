package net.rai.scp_unleashed.scp3008.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class Scp3008PillarBlock extends Scp3008SealedBlock {
    public static final MapCodec<Scp3008PillarBlock> PillarCodec = simpleCodec(Scp3008PillarBlock::new);
    public static final EnumProperty<Part> Section = EnumProperty.create("part", Part.class);

    public enum Part implements StringRepresentable {
        Base("base"),
        Shaft("shaft"),
        Band("band");

        private static final Part[] Ordered = values();
        public final String Name;

        Part(String Name) {
            this.Name = Name;
        }

        public static Part of(int Index) {
            return Ordered[Math.max(0, Math.min(Ordered.length - 1, Index))];
        }

        @Override
        public String getSerializedName() {
            return Name;
        }
    }

    public Scp3008PillarBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Section, Part.Shaft));
    }

    @Override
    protected MapCodec<? extends Scp3008PillarBlock> codec() {
        return PillarCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Section);
    }
}
