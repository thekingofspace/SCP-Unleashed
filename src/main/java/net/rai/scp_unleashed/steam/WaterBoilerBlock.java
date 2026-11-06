package net.rai.scp_unleashed.steam;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.rai.scp_unleashed.block.FluidHands;
import net.rai.scp_unleashed.gas.GasHandler;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.GasPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.RestrictedFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WaterBoilerBlock extends SteamMachineBlock implements FluidPort, GasPort {
    public static final MapCodec<WaterBoilerBlock> BlockCodec = simpleCodec(WaterBoilerBlock::new);
    public static final MachineLayout Layout = MachineLayout.of(3, 4, 3, 1, 0, 1);
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, Layout.count() - 1);
    public static final int FireLight = 13;
    public static final int EmberLight = 6;
    public static final int DoorPart = Layout.index(1, 0, 0);
    public static final List<MachineLayout.Port> WaterPorts = List.of(new MachineLayout.Port(Layout.index(0, 0, 1), Direction.WEST),
            new MachineLayout.Port(Layout.index(2, 0, 1), Direction.EAST), new MachineLayout.Port(Layout.index(1, 0, 2), Direction.SOUTH));
    public static final List<MachineLayout.Port> SteamPorts = List.of(new MachineLayout.Port(Layout.index(1, 3, 1), Direction.UP),
            new MachineLayout.Port(Layout.index(1, 3, 2), Direction.SOUTH));
    private static final VoxelShape[][] CellShapes = Layout.shapes(List.of(
            MachineLayout.pixels(0, 0, 0, 48, 16, 48),
            MachineLayout.pixels(3, 16, 12, 45, 58, 36),
            MachineLayout.pixels(12, 16, 3, 36, 58, 45),
            MachineLayout.pixels(6, 16, 6, 42, 58, 42),
            MachineLayout.pixels(9, 58, 9, 39, 61, 39),
            MachineLayout.pixels(16, 61, 16, 32, 64, 32),
            MachineLayout.pixels(18, 50, 42, 30, 60, 48),
            MachineLayout.pixels(11, 61, 12, 14, 64, 15)));

    public WaterBoilerBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, Layout.origin()).setValue(Light, MachineLight.Off));
    }

    @Override
    public MachineLayout layout() {
        return Layout;
    }

    @Override
    public IntegerProperty partProperty() {
        return Part;
    }

    @Override
    protected VoxelShape[][] shapes() {
        return CellShapes;
    }

    @Override
    protected boolean isPortCell(int Index) {
        for (MachineLayout.Port Each : WaterPorts) {
            if (Each.Part() == Index) {
                return true;
            }
        }
        for (MachineLayout.Port Each : SteamPorts) {
            if (Each.Part() == Index) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int lampPart() {
        return DoorPart;
    }

    public static int lightOf(BlockState State) {
        if (State.getValue(Part) != DoorPart) {
            return 0;
        }
        return switch (State.getValue(Light)) {
            case Working -> FireLight;
            case Waiting -> EmberLight;
            default -> 0;
        };
    }

    public boolean isWaterFace(BlockState State, Direction Side) {
        for (MachineLayout.Port Each : WaterPorts) {
            if (isPort(State, Side, Each.Part(), Each.Local())) {
                return true;
            }
        }
        return false;
    }

    public boolean isSteamFace(BlockState State, Direction Side) {
        for (MachineLayout.Port Each : SteamPorts) {
            if (isPort(State, Side, Each.Part(), Each.Local())) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static WaterBoilerBlockEntity machineAt(BlockGetter World, BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof WaterBoilerBlock Boiler)) {
            return null;
        }
        return World.getBlockEntity(Boiler.masterPos(State, Pos)) instanceof WaterBoilerBlockEntity Machine ? Machine : null;
    }

    @Nullable
    public static IFluidHandler capabilityAt(Level World, BlockPos Pos, BlockState State, @Nullable Direction Side) {
        if (!(State.getBlock() instanceof WaterBoilerBlock Boiler) || Side == null || !Boiler.isWaterFace(State, Side)) {
            return null;
        }
        WaterBoilerBlockEntity Machine = machineAt(World, Pos, State);
        return Machine == null ? null : new RestrictedFluidHandler(Machine.getWaterTank(), PortRole.Input);
    }

    @Nullable
    public static IItemHandler itemHandlerAt(Level World, BlockPos Pos, BlockState State) {
        WaterBoilerBlockEntity Machine = machineAt(World, Pos, State);
        return Machine == null ? null : Machine.getAutomation();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void dismantled(ServerLevel World, BlockPos MasterPos, BlockState MasterState) {
        if (World.getBlockEntity(MasterPos) instanceof WaterBoilerBlockEntity Machine) {
            Machine.dropContents(World, MasterPos);
            GasStack Held = Machine.getSteamTank().getGas();
            if (!Held.isEmpty()) {
                Held.getGas().release(World, MasterPos.above(2).getCenter(), Held.getAmount());
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            WaterBoilerBlockEntity Machine = machineAt(World, Pos, State);
            if (Machine != null) {
                User.openMenu(Machine);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (FluidUtil.getFluidContained(Stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        WaterBoilerBlockEntity Machine = machineAt(World, Pos, State);
        if (Machine != null && FluidHands.interact(User, Hand, new RestrictedFluidHandler(Machine.getWaterTank(), PortRole.Input))) {
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
        WaterBoilerBlockEntity Machine = machineAt(World, Pos, State);
        return Machine == null ? null : new RestrictedFluidHandler(Machine.getWaterTank(), PortRole.Input);
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return masterPos(State, Pos);
    }

    @Nullable
    @Override
    public PortRole gasRole(BlockState State, Direction Side) {
        return isSteamFace(State, Side) ? PortRole.Output : null;
    }

    @Nullable
    @Override
    public GasHandler gasHandler(Level World, BlockPos Pos, BlockState State) {
        WaterBoilerBlockEntity Machine = machineAt(World, Pos, State);
        return Machine == null ? null : GasHandler.drainOnly(Machine.getSteamTank());
    }

    @Override
    public BlockPos gasOwner(Level World, BlockPos Pos, BlockState State) {
        return masterPos(State, Pos);
    }

    @Override
    public boolean exhausts(BlockState State) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.water_boiler.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.water_boiler.size").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.water_boiler.fuel", WaterBoilerBlockEntity.BurnSpeed).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.water_boiler.rate", WaterBoilerBlockEntity.BoilRate * 20).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.water_boiler.water").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.water_boiler.steam").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.water_boiler.stops").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Part) == Layout.Master() ? new WaterBoilerBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Part) != Layout.Master()) {
            return null;
        }
        return createTickerHelper(Type, SteamRegistry.WaterBoilerEntity.get(), World.isClientSide ? WaterBoilerBlockEntity::clientTick : WaterBoilerBlockEntity::serverTick);
    }
}
