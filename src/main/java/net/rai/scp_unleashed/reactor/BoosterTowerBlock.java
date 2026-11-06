package net.rai.scp_unleashed.reactor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.rai.scp_unleashed.reactor.core.ReactorTuning;
import net.rai.scp_unleashed.reactor.core.TowerType;

public class BoosterTowerBlock extends TowerSegmentBlock {
    public static final MapCodec<BoosterTowerBlock> BlockCodec = simpleCodec(BoosterTowerBlock::new);
    public static final BooleanProperty Active = BooleanProperty.create("active");
    public static final int ActiveLight = 6;

    public BoosterTowerBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Section, TowerSection.Single).setValue(Active, false));
    }

    public static int lightOf(BlockState State) {
        return State.getValue(Active) ? ActiveLight : 0;
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
        return TowerType.Booster;
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
    protected Object[] detailArguments(int Line) {
        return switch (Line) {
            case 2 -> new Object[]{Math.round(ReactorTuning.BoostPerSegment * 100.0f), Math.round(ReactorTuning.MaxBoost * 100.0f)};
            default -> new Object[0];
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            User.displayClientMessage(TowerStatus.line(World, Pos), true);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Active)) {
            return;
        }
        if (Random.nextInt(3) == 0) {
            Direction Side = Direction.Plane.HORIZONTAL.getRandomDirection(Random);
            double X = Pos.getX() + 0.5 + Side.getStepX() * 0.53 + (Side.getStepX() == 0 ? (Random.nextDouble() - 0.5) * 0.4 : 0.0);
            double Z = Pos.getZ() + 0.5 + Side.getStepZ() * 0.53 + (Side.getStepZ() == 0 ? (Random.nextDouble() - 0.5) * 0.4 : 0.0);
            World.addParticle(ParticleTypes.ELECTRIC_SPARK, X, Pos.getY() + 0.2 + Random.nextDouble() * 0.6, Z, 0.0, 0.0, 0.0);
        }
        if (State.getValue(Section).isHighest() && Random.nextInt(50) == 0) {
            World.playLocalSound(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, ReactorRegistry.BoosterHum.get(), SoundSource.BLOCKS, 0.45f, 0.9f + Random.nextFloat() * 0.2f, false);
        }
    }
}
