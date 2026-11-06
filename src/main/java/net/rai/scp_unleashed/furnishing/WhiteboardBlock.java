package net.rai.scp_unleashed.furnishing;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

public class WhiteboardBlock extends TallFurnishingBlock {
    public static final MapCodec<WhiteboardBlock> WhiteboardCodec = simpleCodec(WhiteboardBlock::new);
    public static final int Drawings = 4;
    public static final IntegerProperty Drawing = IntegerProperty.create("drawing", 0, Drawings - 1);

    public WhiteboardBlock(Properties Properties) {
        super(TallKind.Whiteboard, Properties);
        registerDefaultState(defaultBlockState().setValue(Drawing, 1));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return WhiteboardCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Drawing);
    }

    @Override
    protected String[] details() {
        return new String[]{"cycle"};
    }

    @Override
    protected BlockState partnered(BlockState State, BlockState Partner) {
        return State.setValue(Drawing, Partner.getValue(Drawing));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        int Step = User.isSecondaryUseActive() ? Drawings - 1 : 1;
        World.setBlock(Pos, State.setValue(Drawing, (State.getValue(Drawing) + Step) % Drawings), Block.UPDATE_ALL);
        World.playSound(null, Pos, FurnishingSounds.MarkerSqueak.get(), SoundSource.BLOCKS, 1.0f, 0.9f + World.random.nextFloat() * 0.2f);
        World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
        return InteractionResult.CONSUME;
    }
}
