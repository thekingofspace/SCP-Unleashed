package net.rai.scp_unleashed.sulfur.tank;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.rai.scp_unleashed.block.FluidHands;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import net.rai.scp_unleashed.pressure.RestrictedFluidHandler;
import net.rai.scp_unleashed.sulfur.SulfurRegistry;
import net.rai.scp_unleashed.sulfur.acid.AcidEffects;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class IndustrialTankBlock extends BaseEntityBlock implements FluidPort {
    public static final MapCodec<IndustrialTankBlock> BlockCodec = simpleCodec(IndustrialTankBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Active = BooleanProperty.create("active");
    public static final IntegerProperty Fill = IntegerProperty.create("fill", 0, IndustrialTankBlockEntity.FillSteps);
    public static final String Key = "block.scp_unleashed." + SulfurRegistry.TankId + ".";
    public static final VoxelShape Shape = Shapes.or(Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0), Block.box(1.0, 2.0, 1.0, 15.0, 14.5, 15.0),
            Block.box(5.5, 14.5, 5.5, 10.5, 16.0, 10.5));
    public static final int VentChance = 4;

    public IndustrialTankBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Active, false).setValue(Fill, 0));
    }

    public static boolean isInlet(BlockState State, Direction Side) {
        Direction Front = State.getValue(Facing);
        return Side == Front.getClockWise() || Side == Front.getCounterClockWise();
    }

    public static boolean isOutlet(BlockState State, Direction Side) {
        return Side == State.getValue(Facing).getOpposite() || Side == Direction.DOWN;
    }

    @Nullable
    public static IFluidHandler fluidHandlerAt(Level World, BlockPos Pos, BlockState State, @Nullable Direction Side) {
        if (!(World.getBlockEntity(Pos) instanceof IndustrialTankBlockEntity Machine)) {
            return null;
        }
        if (Side == null) {
            return Machine.getValves();
        }
        PortRole Role = isInlet(State, Side) ? PortRole.Input : isOutlet(State, Side) ? PortRole.Output : null;
        return Role == null ? null : new RestrictedFluidHandler(Machine.getValves(), Role);
    }

    @Nullable
    public static IItemHandler itemHandlerAt(Level World, BlockPos Pos, BlockState State, @Nullable Direction Side) {
        return World.getBlockEntity(Pos) instanceof IndustrialTankBlockEntity Machine ? Machine.getAutomation() : null;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Active, Fill);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return Shapes.block();
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(this) && World instanceof ServerLevel ServerWorld) {
            WireNetworkManager.get(ServerWorld).markDirty();
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock())) {
            if (World.getBlockEntity(Pos) instanceof IndustrialTankBlockEntity Machine) {
                Machine.dropContents(World, Pos);
            }
            if (World instanceof ServerLevel ServerWorld) {
                WireNetworkManager.get(ServerWorld).markDirty();
                PressureNetworkManager.blockChanged(ServerWorld, Pos);
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && World.getBlockEntity(Pos) instanceof IndustrialTankBlockEntity Machine) {
            User.openMenu(Machine, Pos);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        boolean Liquid = FluidUtil.getFluidHandler(Stack).isPresent();
        boolean Sulfur = IndustrialTankBlockEntity.accepts(Stack);
        if (!Liquid && !Sulfur) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(World.getBlockEntity(Pos) instanceof IndustrialTankBlockEntity Machine)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (Liquid) {
            return FluidHands.interact(User, Hand, Machine.getValves()) ? ItemInteractionResult.CONSUME : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ItemStack Left = Machine.getStorage().insertItem(IndustrialTankBlockEntity.SulfurSlot, Stack.copy(), false);
        if (Left.getCount() == Stack.getCount()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!User.hasInfiniteMaterials()) {
            User.setItemInHand(Hand, Left);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public PortRole portRole(BlockState State, Direction Side) {
        return isInlet(State, Side) ? PortRole.Input : isOutlet(State, Side) ? PortRole.Output : null;
    }

    @Nullable
    @Override
    public IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State) {
        return World.getBlockEntity(Pos) instanceof IndustrialTankBlockEntity Machine ? Machine.getValves() : null;
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return Pos;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        return World.getBlockEntity(Pos) instanceof IndustrialTankBlockEntity Machine ? Machine.comparatorLevel() : 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new IndustrialTankBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, SulfurRegistry.TankEntity.get(), IndustrialTankBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Active) || Random.nextInt(VentChance) != 0) {
            return;
        }
        BlockPos Above = Pos.above();
        if (World.getBlockState(Above).isSolidRender(World, Above)) {
            return;
        }
        double X = Pos.getX() + 0.5 + (Random.nextDouble() - 0.5) * 0.25;
        double Z = Pos.getZ() + 0.5 + (Random.nextDouble() - 0.5) * 0.25;
        World.addParticle(ParticleTypes.WHITE_SMOKE, X, Pos.getY() + 1.02, Z, 0.0, 0.02, 0.0);
        if (Random.nextBoolean()) {
            World.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, AcidEffects.WispColour), X, Pos.getY() + 1.05, Z, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "recipe", IndustrialTankBlockEntity.BatchSulfur).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "draw", IndustrialTankBlockEntity.WorkDraw).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "ports").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "buckets").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "wiring").withStyle(ChatFormatting.GOLD));
        }
    }
}
