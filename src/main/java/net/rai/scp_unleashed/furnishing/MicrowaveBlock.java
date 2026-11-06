package net.rai.scp_unleashed.furnishing;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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

public class MicrowaveBlock extends FurnishingBlock {
    public static final MapCodec<MicrowaveBlock> MicrowaveCodec = simpleCodec(MicrowaveBlock::new);
    public static final BooleanProperty Lit = BlockStateProperties.LIT;
    public static final int RunTicks = 100;

    public MicrowaveBlock(Properties Properties) {
        super(FurnishingKind.Microwave, Properties);
        registerDefaultState(defaultBlockState().setValue(Lit, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return MicrowaveCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Lit);
    }

    @Override
    protected String[] details() {
        return new String[]{"run"};
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!State.getValue(Lit)) {
            World.setBlock(Pos, State.setValue(Lit, true), Block.UPDATE_ALL);
            World.scheduleTick(Pos, this, RunTicks);
            World.playSound(null, Pos, FurnishingSounds.LampClick.get(), SoundSource.BLOCKS, 1.0f, 0.95f);
            World.playSound(null, Pos, FurnishingSounds.MicrowaveStart.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
            World.playSound(null, Pos, FurnishingSounds.MicrowaveHum.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            World.gameEvent(User, GameEvent.BLOCK_ACTIVATE, Pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (State.getValue(Lit)) {
            World.setBlock(Pos, State.setValue(Lit, false), Block.UPDATE_ALL);
            World.playSound(null, Pos, FurnishingSounds.MicrowaveDing.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
            World.gameEvent(null, GameEvent.BLOCK_DEACTIVATE, Pos);
        }
    }
}
