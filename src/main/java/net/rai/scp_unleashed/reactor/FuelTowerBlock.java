package net.rai.scp_unleashed.reactor;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.fission.ReactorFuel;
import net.rai.scp_unleashed.gas.GasHandler;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.pressure.GasPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.reactor.core.ReactorTuning;
import net.rai.scp_unleashed.reactor.core.TowerType;
import org.jetbrains.annotations.Nullable;


public class FuelTowerBlock extends TowerSegmentBlock implements GasPort {
    public static final MapCodec<FuelTowerBlock> BlockCodec = simpleCodec(FuelTowerBlock::new);
    public static final IntegerProperty Glow = IntegerProperty.create("glow", 0, 3);
    public static final int[] GlowLight = {0, 7, 10, 13};

    public FuelTowerBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Section, TowerSection.Single).setValue(Glow, 0));
    }

    public static int lightOf(BlockState State) {
        return GlowLight[State.getValue(Glow)];
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Glow);
    }

    @Override
    public TowerType getTowerType() {
        return TowerType.Fuel;
    }

    @Override
    public BlockState withVisual(BlockState State, int Visual) {
        return State.setValue(Glow, Math.max(0, Math.min(3, Visual)));
    }

    @Override
    protected int detailLines() {
        return 5;
    }

    @Override
    protected Object[] detailArguments(int Line) {
        return switch (Line) {
            case 2 -> new Object[]{Math.round(TowerFuel.heatOf(ReactorFuel.Thorium)), Math.round(TowerFuel.heatOf(ReactorFuel.Uranium))};
            case 5 -> new Object[]{Math.round(ReactorTuning.HotLevel), Math.round(ReactorTuning.CriticalLevel)};
            default -> new Object[0];
        };
    }

    @Nullable
    public static IItemHandler itemHandlerAt(Level World, BlockPos Pos) {
        if (!(World.getBlockState(Pos).getBlock() instanceof FuelTowerBlock)) {
            return null;
        }
        GridTower Tower = towerFor(World, Pos);
        if (Tower != null) {
            ReactorGrid Grid = gridFor(World, Pos);
            return new TowerItemHandler(Tower.Entities, () -> {
                if (Grid != null && !Grid.isDiscarded()) {
                    Grid.fuelChanged(Tower);
                }
            }, () -> {
                if (Grid != null && !Grid.isDiscarded()) {
                    Grid.writeBack();
                }
            });
        }
        return new TowerItemHandler(segments(World, Pos), () -> {
        }, () -> {
        });
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!TowerFuel.isFuel(Stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        IItemHandler Handler = itemHandlerAt(World, Pos);
        if (Handler == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        for (int Slot = 0; Slot < Handler.getSlots(); Slot++) {
            ItemStack Left = Handler.insertItem(Slot, Stack.copyWithCount(1), false);
            if (Left.isEmpty()) {
                if (!User.getAbilities().instabuild) {
                    Stack.shrink(1);
                }
                World.playSound(null, Pos, FissionRegistry.ReactorClunk.get(), SoundSource.BLOCKS, 0.8f, 1.15f);
                return ItemInteractionResult.CONSUME;
            }
        }
        User.displayClientMessage(Component.translatable("message.scp_unleashed.fuel_tower.full").withStyle(ChatFormatting.GOLD), true);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        IItemHandler Handler = itemHandlerAt(World, Pos);
        if (Handler instanceof TowerItemHandler Tower) {
            ItemStack Taken = Tower.takeForHand(User.isShiftKeyDown());
            if (!Taken.isEmpty()) {
                if (!User.getInventory().add(Taken)) {
                    User.drop(Taken, false);
                }
                World.playSound(null, Pos, FissionRegistry.ReactorClunk.get(), SoundSource.BLOCKS, 0.8f, 0.95f);
                return InteractionResult.CONSUME;
            }
        }
        User.displayClientMessage(TowerStatus.line(World, Pos), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void stepOn(Level World, BlockPos Pos, BlockState State, Entity Walker) {
        if (State.getValue(Glow) >= 2 && !Walker.isSteppingCarefully() && Walker instanceof LivingEntity) {
            Walker.hurt(World.damageSources().hotFloor(), 1.0f);
        }
        super.stepOn(World, Pos, State, Walker);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        int Heat = State.getValue(Glow);
        if (Heat <= 0) {
            return;
        }
        if (Heat == 1 && Random.nextInt(6) == 0) {
            Direction Side = Direction.Plane.HORIZONTAL.getRandomDirection(Random);
            double X = Pos.getX() + 0.5 + Side.getStepX() * 0.52 + (Side.getStepX() == 0 ? (Random.nextDouble() - 0.5) * 0.6 : 0.0);
            double Z = Pos.getZ() + 0.5 + Side.getStepZ() * 0.52 + (Side.getStepZ() == 0 ? (Random.nextDouble() - 0.5) * 0.6 : 0.0);
            World.addParticle(ParticleTypes.GLOW, X, Pos.getY() + Random.nextDouble(), Z, 0.0, 0.01, 0.0);
        }
        if (Heat >= 2 && Random.nextInt(Heat == 2 ? 5 : 2) == 0) {
            World.addParticle(ParticleTypes.SMOKE, Pos.getX() + Random.nextDouble(), Pos.getY() + Random.nextDouble(), Pos.getZ() + Random.nextDouble(), 0.0, 0.03, 0.0);
        }
        if (Heat >= 3 && Random.nextInt(4) == 0) {
            World.addParticle(ParticleTypes.LARGE_SMOKE, Pos.getX() + Random.nextDouble(), Pos.getY() + 1.0, Pos.getZ() + Random.nextDouble(), 0.0, 0.05, 0.0);
            if (Random.nextInt(3) == 0) {
                World.addParticle(ParticleTypes.LAVA, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, 0.0, 0.0, 0.0);
            }
        }
        if (State.getValue(Section).isHighest() && Random.nextInt(Heat >= 3 ? 20 : 80) == 0) {
            World.playLocalSound(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, FissionRegistry.ReactorHum.get(), SoundSource.BLOCKS, 0.35f, Heat >= 3 ? 0.7f : 0.9f, false);
        }
        if (Heat >= 3 && Random.nextInt(40) == 0) {
            World.playLocalSound(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.6f, 0.7f, false);
        }
    }

    @Override
    protected void released(ServerLevel World, BlockPos Pos, TowerSegmentBlockEntity Segment) {
        int Gas = Math.round(Segment.getGas());
        if (Gas > 0) {
            GasRegistry.ToxicGas.get().release(World, Pos.getCenter(), Gas);
        }
    }

    @Nullable
    @Override
    public PortRole gasRole(BlockState State, Direction Side) {
        return Side == Direction.DOWN && State.getValue(Section).isLowest() ? PortRole.Output : null;
    }

    @Nullable
    @Override
    public GasHandler gasHandler(Level World, BlockPos Pos, BlockState State) {
        GridTower Tower = towerFor(World, Pos);
        return Tower == null ? null : Tower.Outlet;
    }

    @Override
    public BlockPos gasOwner(Level World, BlockPos Pos, BlockState State) {
        return Pos;
    }
}
