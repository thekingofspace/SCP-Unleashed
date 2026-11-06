package net.rai.scp_unleashed.scp002;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class FleshBedBlock extends FleshFixtureBlock {
    public static final MapCodec<FleshBedBlock> BedCodec = simpleCodec(FleshBedBlock::new);
    public static final EnumProperty<BedPart> Part = BlockStateProperties.BED_PART;

    public FleshBedBlock(Properties Properties) {
        super(Properties, Fixture.Bed);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, BedPart.FOOT));
    }

    @Override
    protected MapCodec<? extends FleshBedBlock> codec() {
        return BedCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Part);
    }
}
