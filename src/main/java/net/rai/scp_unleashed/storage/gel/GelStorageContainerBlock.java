package net.rai.scp_unleashed.storage.gel;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import net.rai.scp_unleashed.pressure.RestrictedFluidHandler;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GelStorageContainerBlock extends StorageContainerBlock implements FluidPort {
    public static final MapCodec<GelStorageContainerBlock> GelCodec = simpleCodec(GelStorageContainerBlock::new);
    public static final EnumProperty<GelStatus> Status = EnumProperty.create("gel", GelStatus.class);
    public static final String Prefix = "block.scp_unleashed.storage_container_gel";
    private static final Map<Direction, VoxelShape> Sockets = sockets();

    public GelStorageContainerBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(defaultBlockState().setValue(Status, GelStatus.Idle));
    }

    private static VoxelShape plate(Direction Side) {
        return switch (Side) {
            case EAST -> Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);
            case WEST -> Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);
            case SOUTH -> Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
            default -> Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
        };
    }

    private static Map<Direction, VoxelShape> sockets() {
        Map<Direction, VoxelShape> Found = new EnumMap<>(Direction.class);
        for (Direction Front : Direction.Plane.HORIZONTAL) {
            Found.put(Front, Shapes.or(plate(Front.getClockWise()), plate(Front.getCounterClockWise())));
        }
        return Found;
    }

    public static Direction backOf(BlockState State) {
        return State.getValue(Facing).getOpposite();
    }

    public static List<Direction> powerFaces(BlockState State) {
        Direction Front = State.getValue(Facing);
        return List.of(Front.getClockWise(), Front.getCounterClockWise());
    }

    @Nullable
    public static IFluidHandler capabilityAt(Level World, BlockPos Pos, BlockState State, @Nullable Direction Side) {
        if (!(World.getBlockEntity(Pos) instanceof GelStorageContainerBlockEntity Locker)) {
            return null;
        }
        if (Side == null || Side == backOf(State)) {
            return new RestrictedFluidHandler(Locker.getTank(), PortRole.Input);
        }
        return null;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return GelCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Status);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return Sockets.get(State.getValue(Facing));
    }

    @Nullable
    @Override
    public PortRole portRole(BlockState State, Direction Side) {
        return Side == backOf(State) ? PortRole.Input : null;
    }

    @Nullable
    @Override
    public IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State) {
        return World.getBlockEntity(Pos) instanceof GelStorageContainerBlockEntity Locker ? new RestrictedFluidHandler(Locker.getTank(), PortRole.Input) : null;
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return Pos;
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(this) && World instanceof ServerLevel Server) {
            PressureNetworkManager.blockChanged(Server, Pos);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World instanceof ServerLevel Server) {
            PressureNetworkManager.blockChanged(Server, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && User.isCreative() && World.getBlockEntity(Pos) instanceof GelStorageContainerBlockEntity Locker && Locker.hasContents()) {
            ItemStack Packed = new ItemStack(GelStorageRegistry.GelContainerItem.get());
            Packed.applyComponents(Locker.collectComponents());
            ItemEntity Dropped = new ItemEntity(World, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Packed);
            Dropped.setDefaultPickUpDelay();
            World.addFreshEntity(Dropped);
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    public InteractionResult screwdrive(Level World, BlockPos Pos, BlockState State, @Nullable Player User, boolean Sneaking) {
        if (User != null) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.storage_gel.no_fittings"), true);
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.storage_container.toggle").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.storage_container.store").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + ".power").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + ".water").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + ".gel").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + ".carry").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new GelStorageContainerBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? createTickerHelper(Type, GelStorageRegistry.GelContainerEntity.get(), GelStorageContainerBlockEntity::clientTick)
                : createTickerHelper(Type, GelStorageRegistry.GelContainerEntity.get(), GelStorageContainerBlockEntity::serverTick);
    }
}
