package net.rai.scp_unleashed.medical;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.rai.scp_unleashed.furnishing.FurnishingSounds;

public class MedicalLightBlock extends MedicalPropBlock {
    public static final MapCodec<MedicalLightBlock> LightCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            MedicalPropKind.KindCodec.fieldOf("kind").forGetter(MedicalLightBlock::getKind), propertiesCodec()).apply(Instance, MedicalLightBlock::new));
    public static final BooleanProperty Lit = BlockStateProperties.LIT;

    public MedicalLightBlock(MedicalPropKind Kind, Properties Properties) {
        super(Kind, Properties);
        registerDefaultState(defaultBlockState().setValue(Lit, true));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return LightCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Lit);
    }

    @Override
    protected String[] details() {
        return new String[]{"toggle"};
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        boolean On = !State.getValue(Lit);
        World.setBlock(Pos, State.setValue(Lit, On), Block.UPDATE_ALL);
        World.playSound(null, Pos, FurnishingSounds.LampClick.get(), SoundSource.BLOCKS, 1.0f, On ? 1.05f : 0.9f);
        World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
        return InteractionResult.CONSUME;
    }
}
