package net.rai.scp_unleashed.scp109;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.block.FluidHands;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp109TapBlock extends HorizontalDirectionalBlock implements EntityBlock, FluidPort {
    public static final MapCodec<Scp109TapBlock> BlockCodec = simpleCodec(Scp109TapBlock::new);
    public static final BooleanProperty Canteen = BooleanProperty.create("canteen");
    public static final BooleanProperty Flowing = BooleanProperty.create("flowing");
    public static final int SeatedLight = 3;
    public static final String Key = "block.scp_unleashed.scp109_canteen_tap.";

    public Scp109TapBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Canteen, false).setValue(Flowing, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Canteen, Flowing);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    public static Scp109TapBlockEntity tapAt(Level World, BlockPos Pos) {
        return World.getBlockEntity(Pos) instanceof Scp109TapBlockEntity Tap ? Tap : null;
    }

    @Nullable
    public static IFluidHandler capabilityAt(Level World, BlockPos Pos, BlockState State, @Nullable Direction Side) {
        if (Side != null && Side == State.getValue(FACING)) {
            return null;
        }
        Scp109TapBlockEntity Tap = tapAt(World, Pos);
        return Tap == null ? null : Tap.outlet();
    }

    public static void seat(Level World, BlockPos Pos, BlockState State, Scp109TapBlockEntity Tap, ItemStack Stack) {
        Tap.insert(Stack);
        World.setBlock(Pos, State.setValue(Canteen, true).setValue(Flowing, false), Block.UPDATE_ALL);
        play(World, Pos, Scp109Registry.SeatLayers, 0.8f, 1.0f);
    }

    public static ItemStack unseat(Level World, BlockPos Pos, BlockState State, Scp109TapBlockEntity Tap) {
        ItemStack Taken = Tap.take();
        World.setBlock(Pos, State.setValue(Canteen, false).setValue(Flowing, false), Block.UPDATE_ALL);
        play(World, Pos, Scp109Registry.ReleaseLayers, 0.8f, 1.0f);
        return Taken;
    }

    private static void play(Level World, BlockPos Pos, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Pos, Layer.get(), SoundSource.BLOCKS, Volume, Pitch);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        boolean Seated = State.getValue(Canteen);
        if (Scp109CanteenItem.is(Stack) && !Seated) {
            if (!World.isClientSide) {
                Scp109TapBlockEntity Tap = tapAt(World, Pos);
                if (Tap == null || Tap.hasCanteen()) {
                    return ItemInteractionResult.FAIL;
                }
                seat(World, Pos, State, Tap, Stack);
                Stack.consume(1, User);
            }
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        if (!Seated) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (Stack.is(Items.GLASS_BOTTLE)) {
            if (!World.isClientSide) {
                Scp109CanteenItem.fillBottle(World, User, Hand, Stack, Vec3.atCenterOf(Pos));
                Scp109TapBlockEntity Tap = tapAt(World, Pos);
                if (Tap != null) {
                    Tap.spout().drain(Scp109CanteenItem.BottleVolume, IFluidHandler.FluidAction.EXECUTE);
                }
            }
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        if (FluidUtil.getFluidHandler(Stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide) {
            Scp109TapBlockEntity Tap = tapAt(World, Pos);
            if (Tap == null || !FluidHands.interact(User, Hand, Tap.spout())) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!State.getValue(Canteen) || !User.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide) {
            Scp109TapBlockEntity Tap = tapAt(World, Pos);
            if (Tap == null || !Tap.hasCanteen()) {
                return InteractionResult.PASS;
            }
            ItemStack Taken = unseat(World, Pos, State, Tap);
            if (!User.getInventory().add(Taken)) {
                User.drop(Taken, false);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(this) && World instanceof ServerLevel ServerWorld) {
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replaced, boolean Moved) {
        if (!State.is(Replaced.getBlock())) {
            Scp109TapBlockEntity Tap = tapAt(World, Pos);
            if (Tap != null && Tap.hasCanteen()) {
                Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Tap.take());
            }
            if (World instanceof ServerLevel ServerWorld) {
                PressureNetworkManager.blockChanged(ServerWorld, Pos);
            }
        }
        super.onRemove(State, World, Pos, Replaced, Moved);
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return false;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        return State.getValue(Flowing) ? 15 : State.getValue(Canteen) ? 7 : 0;
    }

    @Nullable
    @Override
    public PortRole portRole(BlockState State, Direction Side) {
        return Side == State.getValue(FACING) ? null : PortRole.Output;
    }

    @Nullable
    @Override
    public IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State) {
        Scp109TapBlockEntity Tap = tapAt(World, Pos);
        return Tap == null ? null : Tap.outlet();
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return Pos;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "seat").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "pipes", Scp109TapBlockEntity.MaxPerSecond).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "pressure").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "fill").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp109TapBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (Type != Scp109Registry.TapEntity.get()) {
            return null;
        }
        if (World.isClientSide) {
            return (Where, At, Current, Holder) -> Scp109TapBlockEntity.clientTick(Where, At, Current, (Scp109TapBlockEntity) Holder);
        }
        return (Where, At, Current, Holder) -> Scp109TapBlockEntity.serverTick(Where, At, Current, (Scp109TapBlockEntity) Holder);
    }
}
