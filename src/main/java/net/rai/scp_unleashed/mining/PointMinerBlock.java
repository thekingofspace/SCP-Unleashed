package net.rai.scp_unleashed.mining;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import net.rai.scp_unleashed.pressure.RestrictedFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class PointMinerBlock extends BaseEntityBlock implements FluidPort {
    public static final MapCodec<PointMinerBlock> BlockCodec = simpleCodec(PointMinerBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<MinerStatus> Status = EnumProperty.create("status", MinerStatus.class);
    public static final String Prefix = "block.scp_unleashed.point_miner.";
    private static final VoxelShape PowerPlate = Block.box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0);

    public PointMinerBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Status, MinerStatus.Off));
    }

    public static Direction backOf(BlockState State) {
        return State.getValue(Facing).getOpposite();
    }

    @Nullable
    public static IFluidHandler capabilityAt(Level World, BlockPos Pos, BlockState State, @Nullable Direction Side) {
        if (!(World.getBlockEntity(Pos) instanceof PointMinerBlockEntity Miner)) {
            return null;
        }
        if (Side == null) {
            return new RestrictedFluidHandler(Miner.getTank(), PortRole.Input);
        }
        return Side == backOf(State) ? new RestrictedFluidHandler(Miner.getTank(), PortRole.Input) : null;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Status);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return PowerPlate;
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
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && World.getBlockEntity(Pos) instanceof PointMinerBlockEntity Miner) {
            MinerStatus Shown = State.getValue(Status);
            Component Line = Component.translatable("message.scp_unleashed.point_miner.status", Component.translatable(Shown.getTranslationKey()),
                    Miner.getStackIndex(), Miner.getStackSize(), String.format(Locale.ROOT, "%.1f", MinerRules.pointsPerMinute(Miner.getCore().isMining() ? Miner.getCore().getShare() : 0.0)),
                    Miner.getCore().getBuffer(), MinerRules.BufferSize);
            User.displayClientMessage(Line.copy().withStyle(Shown.getColour()), true);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Nullable
    @Override
    public PortRole portRole(BlockState State, Direction Side) {
        return Side == backOf(State) ? PortRole.Input : null;
    }

    @Nullable
    @Override
    public IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State) {
        return World.getBlockEntity(Pos) instanceof PointMinerBlockEntity Miner ? Miner.getTank() : null;
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return Pos;
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (State.getValue(Status) == MinerStatus.Running) {
            ServerAmbience.idle(World, Pos, Random, ServerAmbience.BusyLoad);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "stack").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "power", MinerRules.DrawPerMiner).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "water", MinerRules.waterPerMinute()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "output", String.format(Locale.ROOT, "%.0f", MinerRules.pointsPerMinute(1.0))).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "bank", MinerRules.BufferSize).withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new PointMinerBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, MiningRegistry.PointMinerEntity.get(), PointMinerBlockEntity::serverTick);
    }
}
