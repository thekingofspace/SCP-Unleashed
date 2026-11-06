package net.rai.scp_unleashed.furnishing;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import static net.rai.scp_unleashed.furnishing.FurnishingShapes.box;

public class PaperStackBlock extends FurnishingBlock {
    public static final MapCodec<PaperStackBlock> PaperCodec = simpleCodec(PaperStackBlock::new);
    public static final int Variants = 6;
    public static final IntegerProperty Variant = IntegerProperty.create("variant", 0, Variants - 1);
    private static final VoxelShape[] VariantShapes = {
            FurnishingShapes.of(box(4, 0, 3, 13, 2, 13)),
            FurnishingShapes.of(box(5, 0, 4, 11, 3, 13), box(6, 3, 5, 12, 4, 14)),
            FurnishingShapes.of(box(2, 0, 2, 14, 3, 15)),
            FurnishingShapes.of(box(4, 0, 3, 13, 3, 14), box(5, 3, 2, 12, 6, 12)),
            FurnishingShapes.of(box(4, 0, 2, 12, 1, 14), box(5, 1, 2, 11, 3, 13), box(12, 0, 4, 13, 1, 11)),
            FurnishingShapes.of(box(4, 0, 3, 12, 3, 13))
    };

    private final ImmutableMap<BlockState, VoxelShape> Outlines;

    public PaperStackBlock(Properties Properties) {
        super(FurnishingKind.PaperStack, Properties);
        registerDefaultState(defaultBlockState().setValue(Variant, 0));
        this.Outlines = getShapeForEachState(State -> FurnishingShapes.turn(VariantShapes[State.getValue(Variant)], State.getValue(FACING)));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return PaperCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Variant);
    }

    @Override
    protected String[] details() {
        return new String[]{"cycle"};
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        int Step = User.isSecondaryUseActive() ? Variants - 1 : 1;
        World.setBlock(Pos, State.setValue(Variant, (State.getValue(Variant) + Step) % Variants), Block.UPDATE_ALL);
        World.playSound(null, Pos, FurnishingSounds.PaperShuffle.get(), SoundSource.BLOCKS, 0.9f, 0.9f + World.random.nextFloat() * 0.2f);
        World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
        return InteractionResult.CONSUME;
    }
}
