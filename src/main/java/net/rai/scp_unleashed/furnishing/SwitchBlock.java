package net.rai.scp_unleashed.furnishing;

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

public class SwitchBlock extends FurnishingBlock {
    public static final MapCodec<SwitchBlock> SwitchCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            FurnishingKind.KindCodec.fieldOf("kind").forGetter(SwitchBlock::getKind), propertiesCodec()).apply(Instance, SwitchBlock::new));
    public static final BooleanProperty Lit = BlockStateProperties.LIT;

    public SwitchBlock(FurnishingKind Kind, Properties Properties) {
        super(Kind, Properties);
        registerDefaultState(defaultBlockState().setValue(Lit, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return SwitchCodec;
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

    protected void playSwitch(Level World, BlockPos Pos, boolean On) {
        World.playSound(null, Pos, FurnishingSounds.LampClick.get(), SoundSource.BLOCKS, 1.0f, On ? 1.05f : 0.9f);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        boolean On = !State.getValue(Lit);
        World.setBlock(Pos, State.setValue(Lit, On), Block.UPDATE_ALL);
        playSwitch(World, Pos, On);
        World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
        return InteractionResult.CONSUME;
    }
}
