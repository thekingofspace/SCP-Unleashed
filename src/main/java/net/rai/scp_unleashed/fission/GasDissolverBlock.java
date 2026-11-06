package net.rai.scp_unleashed.fission;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.rai.scp_unleashed.block.DuctConnectable;
import net.rai.scp_unleashed.block.FluidHands;
import net.rai.scp_unleashed.gas.GasHandler;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.DuctKind;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.GasPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import net.rai.scp_unleashed.pressure.RestrictedFluidHandler;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GasDissolverBlock extends BaseEntityBlock implements FluidPort, DuctConnectable, GasPort {
    public static final MapCodec<GasDissolverBlock> BlockCodec = simpleCodec(GasDissolverBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DissolverMode> Mode = EnumProperty.create("mode", DissolverMode.class);

    public GasDissolverBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Mode, DissolverMode.Empty));
    }

    public static boolean isGasFace(Direction Side) {
        return Side.getAxis() == Direction.Axis.Y;
    }

    public static boolean isWaterFace(BlockState State, Direction Side) {
        return Side.getAxis().isHorizontal() && Side != State.getValue(Facing);
    }

    @Nullable
    public static IFluidHandler capabilityAt(Level World, BlockPos Pos, BlockState State, @Nullable Direction Side) {
        if (Side != null && !isWaterFace(State, Side) || !(World.getBlockEntity(Pos) instanceof GasDissolverBlockEntity Dissolver)) {
            return null;
        }
        return new RestrictedFluidHandler(Dissolver.getTank(), PortRole.Input);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Mode);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

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
        return State.setValue(Facing, Flip.mirror(State.getValue(Facing)));
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(this) && World instanceof ServerLevel ServerWorld) {
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World instanceof ServerLevel ServerWorld) {
            if (World.getBlockEntity(Pos) instanceof GasDissolverBlockEntity Dissolver) {
                Dissolver.dropUpgrades(World, Pos);
            }
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && World.getBlockEntity(Pos) instanceof GasDissolverBlockEntity Dissolver) {
            User.openMenu(Dissolver);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        boolean Water = FluidUtil.getFluidContained(Stack).map(Contained -> Contained.is(FluidTags.WATER)).orElse(false);
        if (!Water) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (World.getBlockEntity(Pos) instanceof GasDissolverBlockEntity Dissolver
                && FluidHands.interact(User, Hand, new RestrictedFluidHandler(Dissolver.getTank(), PortRole.Input))) {
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    @Override
    public PortRole portRole(BlockState State, Direction Side) {
        return isWaterFace(State, Side) ? PortRole.Input : null;
    }

    @Nullable
    @Override
    public IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State) {
        return World.getBlockEntity(Pos) instanceof GasDissolverBlockEntity Dissolver ? new RestrictedFluidHandler(Dissolver.getTank(), PortRole.Input) : null;
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return Pos;
    }

    @Override
    public boolean acceptsDuct(BlockState State, Direction Side, DuctKind Kind) {
        return Kind == DuctKind.Vent ? isGasFace(Side) : isWaterFace(State, Side);
    }

    @Nullable
    @Override
    public PortRole gasRole(BlockState State, Direction Side) {
        return isGasFace(Side) ? PortRole.Input : null;
    }

    @Nullable
    @Override
    public GasHandler gasHandler(Level World, BlockPos Pos, BlockState State) {
        return World.getBlockEntity(Pos) instanceof GasDissolverBlockEntity Dissolver ? Dissolver.getIntake() : null;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.gas_dissolver.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.gas_dissolver.vents").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.gas_dissolver.water", GasDissolverBlockEntity.TankCapacity).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.gas_dissolver.rate", GasDissolverBlockEntity.perSecond(GasDissolverBlockEntity.DissolveRate),
                    GasDissolverBlockEntity.formatAmount(GasDissolverBlockEntity.WaterPerGas)).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.gas_dissolver.area", (int) GasDissolverBlockEntity.ScrubRadius).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.gas_dissolver.power").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.gas_dissolver.upgrades", UpgradeType.Speed.getLimit()).withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new GasDissolverBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return createTickerHelper(Type, FissionRegistry.GasDissolverEntity.get(), World.isClientSide ? GasDissolverBlockEntity::clientTick : GasDissolverBlockEntity::serverTick);
    }
}
