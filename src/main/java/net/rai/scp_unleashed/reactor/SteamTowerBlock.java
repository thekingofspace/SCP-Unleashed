package net.rai.scp_unleashed.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.rai.scp_unleashed.block.FluidHands;
import net.rai.scp_unleashed.gas.GasHandler;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.GasPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.reactor.core.ReactorTuning;
import org.jetbrains.annotations.Nullable;

public abstract class SteamTowerBlock extends TowerSegmentBlock implements FluidPort, GasPort {
    public static final BooleanProperty Active = BooleanProperty.create("active");

    protected SteamTowerBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Section, TowerSection.Single).setValue(Active, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Active);
    }

    @Override
    public BlockState withVisual(BlockState State, int Visual) {
        return State.setValue(Active, Visual > 0);
    }

    public static boolean isWaterFace(BlockState State, Direction Side) {
        return Side.getAxis().isHorizontal() || Side == Direction.DOWN && State.getValue(Section).isLowest();
    }

    @Nullable
    public static IFluidHandler waterHandlerAt(Level World, BlockPos Pos, BlockState State, @Nullable Direction Side) {
        if (!(State.getBlock() instanceof SteamTowerBlock) || Side != null && !isWaterFace(State, Side)) {
            return null;
        }
        GridTower Tower = towerFor(World, Pos);
        if (Tower != null) {
            return Tower.Inlet;
        }
        if (World.getBlockEntity(Pos) instanceof TowerSegmentBlockEntity Segment) {
            return new TowerWaterInlet(Segment::getWater, Segment::setWater, ReactorTuning.WaterPerSegment);
        }
        return null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (FluidUtil.getFluidContained(Stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        IFluidHandler Handler = waterHandlerAt(World, Pos, State, null);
        if (Handler != null && FluidHands.interact(User, Hand, Handler)) {
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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
        if (State.getValue(Section).isHighest() && Random.nextInt(2) == 0) {
            World.addParticle(ParticleTypes.CLOUD, Pos.getX() + 0.3 + Random.nextDouble() * 0.4, Pos.getY() + 1.05, Pos.getZ() + 0.3 + Random.nextDouble() * 0.4,
                    (Random.nextDouble() - 0.5) * 0.02, 0.06 + Random.nextDouble() * 0.04, (Random.nextDouble() - 0.5) * 0.02);
        }
        if (Random.nextInt(5) == 0) {
            Direction Side = Direction.Plane.HORIZONTAL.getRandomDirection(Random);
            World.addParticle(ParticleTypes.BUBBLE_POP, Pos.getX() + 0.5 + Side.getStepX() * 0.53, Pos.getY() + 0.2 + Random.nextDouble() * 0.6,
                    Pos.getZ() + 0.5 + Side.getStepZ() * 0.53, 0.0, 0.02, 0.0);
        }
        if (State.getValue(Section).isHighest() && Random.nextInt(60) == 0) {
            World.playLocalSound(Pos.getX() + 0.5, Pos.getY() + 1.0, Pos.getZ() + 0.5, ReactorRegistry.TowerBoil.get(), SoundSource.BLOCKS, 0.4f, 0.9f + Random.nextFloat() * 0.2f, false);
        }
    }

    @Override
    protected void released(ServerLevel World, BlockPos Pos, TowerSegmentBlockEntity Segment) {
        int Steam = Math.round(Segment.getSteam());
        if (Steam > 0) {
            GasRegistry.Steam.get().release(World, Pos.getCenter(), Steam);
        }
    }

    @Nullable
    @Override
    public PortRole portRole(BlockState State, Direction Side) {
        return isWaterFace(State, Side) ? PortRole.Input : null;
    }

    @Nullable
    @Override
    public IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State) {
        return waterHandlerAt(World, Pos, State, null);
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return Pos;
    }

    @Nullable
    @Override
    public PortRole gasRole(BlockState State, Direction Side) {
        return Side != Direction.DOWN && State.getValue(Section).isHighest() ? PortRole.Output : null;
    }

    @Nullable
    @Override
    public GasHandler gasHandler(Level World, BlockPos Pos, BlockState State) {
        GridTower Tower = towerFor(World, Pos);
        return Tower == null ? null : Tower.Outlet;
    }

    @Override
    public boolean exhausts(BlockState State) {
        return true;
    }
}
