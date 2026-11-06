package net.rai.scp_unleashed.containmentzone;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class CellPlaqueBlock extends CellPropBlock {
    public static final MapCodec<CellPlaqueBlock> PlaqueCodec = simpleCodec(CellPlaqueBlock::new);
    public static final int Highest = 16;
    public static final IntegerProperty Number = IntegerProperty.create("number", 1, Highest);

    public CellPlaqueBlock(Properties Properties) {
        super(CellPropKind.CellPlaque, Properties);
        registerDefaultState(defaultBlockState().setValue(Number, 1));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return PlaqueCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Number);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            int Current = State.getValue(Number);
            int Next = User.isShiftKeyDown() ? (Current == 1 ? Highest : Current - 1) : (Current == Highest ? 1 : Current + 1);
            World.setBlock(Pos, State.setValue(Number, Next), Block.UPDATE_ALL);
            World.playSound(null, Pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.7f, 0.8f + Next * 0.02f);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }
}
