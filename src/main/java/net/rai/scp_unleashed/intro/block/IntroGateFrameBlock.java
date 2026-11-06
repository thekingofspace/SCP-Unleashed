package net.rai.scp_unleashed.intro.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class IntroGateFrameBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<IntroGateFrameBlock> FrameCodec = simpleCodec(IntroGateFrameBlock::new);
    public static final EnumProperty<Piece> Part = EnumProperty.create("part", Piece.class);

    public enum Piece implements StringRepresentable {
        Base("base", 0),
        Post("post", 0),
        Coil("coil", 9),
        Corner("corner", 0),
        Lintel("lintel", 0),
        Crown("crown", 11);

        private final String Name;
        private final int Glow;

        Piece(String Name, int Glow) {
            this.Name = Name;
            this.Glow = Glow;
        }

        @Override
        public String getSerializedName() {
            return Name;
        }

        public int glow() {
            return Glow;
        }
    }

    public IntroGateFrameBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Part, Piece.Post));
    }

    public static int lightOf(BlockState State) {
        return State.hasProperty(Part) ? State.getValue(Part).glow() : 0;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return FrameCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Part);
    }
}
