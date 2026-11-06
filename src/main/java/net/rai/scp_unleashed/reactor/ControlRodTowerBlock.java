package net.rai.scp_unleashed.reactor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.rai.scp_unleashed.reactor.core.TowerType;

public class ControlRodTowerBlock extends TowerSegmentBlock {
    public static final MapCodec<ControlRodTowerBlock> BlockCodec = simpleCodec(ControlRodTowerBlock::new);
    public static final BooleanProperty Active = BooleanProperty.create("active");
    public static final float HandStep = 10.0f;

    public ControlRodTowerBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Section, TowerSection.Single).setValue(Active, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Active);
    }

    @Override
    public TowerType getTowerType() {
        return TowerType.Rods;
    }

    @Override
    public BlockState withVisual(BlockState State, int Visual) {
        return State.setValue(Active, Visual > 0);
    }

    @Override
    protected int detailLines() {
        return 4;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        float Step = User.isShiftKeyDown() ? HandStep : -HandStep;
        ReactorGrid Grid = gridFor(World, Pos);
        GridTower Tower = Grid == null ? null : Grid.towerAt(Pos);
        if (Grid != null && Tower != null) {
            Grid.setTarget(Tower, Tower.Core.Target + Step);
            Grid.writeBack();
        } else {
            for (TowerSegmentBlockEntity Segment : segments(World, Pos)) {
                Segment.setTarget(Segment.getTarget() + Step);
            }
        }
        World.playSound(null, Pos, ReactorRegistry.RodDrive.get(), SoundSource.BLOCKS, 0.7f, Step > 0 ? 0.9f : 1.1f);
        User.displayClientMessage(TowerStatus.line(World, Pos), true);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block Neighbour, BlockPos NeighbourPos, boolean MovedByPiston) {
        super.neighborChanged(State, World, Pos, Neighbour, NeighbourPos, MovedByPiston);
        if (World instanceof ServerLevel ServerWorld) {
            ReactorGrid Grid = ReactorGridManager.get(ServerWorld).gridAt(Pos);
            if (Grid != null) {
                Grid.markRedstone();
            }
        }
    }
}
