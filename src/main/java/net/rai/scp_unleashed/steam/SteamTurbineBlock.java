package net.rai.scp_unleashed.steam;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.gas.GasHandler;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.GasPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.wire.WireHostBlock;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SteamTurbineBlock extends SteamMachineBlock implements WireHostBlock, GasPort {
    public static final MapCodec<SteamTurbineBlock> BlockCodec = simpleCodec(SteamTurbineBlock::new);
    public static final MachineLayout Layout = MachineLayout.of(3, 2, 3, 1, 0, 0);
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, Layout.count() - 1);
    public static final List<MachineLayout.Port> InletPorts = List.of(new MachineLayout.Port(Layout.index(1, 1, 0), Direction.UP),
            new MachineLayout.Port(Layout.index(1, 1, 1), Direction.UP));
    public static final List<MachineLayout.Port> ExhaustPorts = List.of(new MachineLayout.Port(Layout.index(1, 0, 2), Direction.SOUTH),
            new MachineLayout.Port(Layout.index(1, 1, 2), Direction.SOUTH));
    public static final List<AABB> Boxes = List.of(
            MachineLayout.pixels(0, 0, 0, 48, 3, 48),
            MachineLayout.pixels(2, 3, 0, 46, 24, 47),
            MachineLayout.pixels(4, 24, 0, 44, 30, 47),
            MachineLayout.pixels(18, 30, 3, 30, 32, 13),
            MachineLayout.pixels(18, 30, 19, 30, 32, 29),
            MachineLayout.pixels(18, 4, 47, 30, 14, 48),
            MachineLayout.pixels(18, 18, 47, 30, 28, 48),
            MachineLayout.pixels(0, 3, 19, 2, 13, 29),
            MachineLayout.pixels(46, 3, 19, 48, 13, 29));
    private static final VoxelShape[][] CellShapes = Layout.shapes(Boxes);
    private static final VoxelShape[][] CellSupports = Layout.supports(Boxes);

    public SteamTurbineBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, Layout.origin()).setValue(Light, MachineLight.Off));
    }

    @Override
    public int lampPart() {
        return Layout.index(2, 0, 0);
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
    protected VoxelShape supportShape(int Turn, int Index) {
        return CellSupports[Turn][Index];
    }

    @Override
    protected boolean isPortCell(int Index) {
        for (MachineLayout.Port Each : InletPorts) {
            if (Each.Part() == Index) {
                return true;
            }
        }
        for (MachineLayout.Port Each : ExhaustPorts) {
            if (Each.Part() == Index) {
                return true;
            }
        }
        return false;
    }

    public boolean isInletFace(BlockState State, Direction Side) {
        for (MachineLayout.Port Each : InletPorts) {
            if (isPort(State, Side, Each.Part(), Each.Local())) {
                return true;
            }
        }
        return false;
    }

    public boolean isExhaustFace(BlockState State, Direction Side) {
        for (MachineLayout.Port Each : ExhaustPorts) {
            if (isPort(State, Side, Each.Part(), Each.Local())) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static SteamTurbineBlockEntity machineAt(BlockGetter World, BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof SteamTurbineBlock Turbine)) {
            return null;
        }
        return World.getBlockEntity(Turbine.masterPos(State, Pos)) instanceof SteamTurbineBlockEntity Machine ? Machine : null;
    }

    @Override
    public BlockPos devicePos(BlockState State, BlockPos Pos) {
        return masterPos(State, Pos);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void placedOn(ServerLevel World, BlockPos Pos, BlockState State) {
        super.placedOn(World, Pos, State);
        if (State.getValue(Part) == Layout.Master()) {
            WireNetworkManager.get(World).markDirty();
        }
    }

    @Override
    protected void removedFrom(ServerLevel World, BlockPos Pos, BlockState State) {
        super.removedFrom(World, Pos, State);
        if (State.getValue(Part) == Layout.Master()) {
            WireNetworkManager.get(World).markDirty();
        }
    }

    @Override
    protected void dismantled(ServerLevel World, BlockPos MasterPos, BlockState MasterState) {
        if (World.getBlockEntity(MasterPos) instanceof SteamTurbineBlockEntity Machine) {
            release(World, MasterPos, Machine.getInletTank().getGas());
            release(World, MasterPos, Machine.getExhaustTank().getGas());
        }
    }

    private static void release(ServerLevel World, BlockPos Pos, GasStack Held) {
        if (!Held.isEmpty()) {
            Held.getGas().release(World, Pos.above().getCenter(), Held.getAmount());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            SteamTurbineBlockEntity Machine = machineAt(World, Pos, State);
            if (Machine != null) {
                User.displayClientMessage(Machine.describe(), true);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Nullable
    @Override
    public PortRole gasRole(BlockState State, Direction Side) {
        if (isInletFace(State, Side)) {
            return PortRole.Input;
        }
        return isExhaustFace(State, Side) ? PortRole.Output : null;
    }

    @Nullable
    @Override
    public GasHandler gasHandler(Level World, BlockPos Pos, BlockState State) {
        SteamTurbineBlockEntity Machine = machineAt(World, Pos, State);
        if (Machine == null) {
            return null;
        }
        int Index = State.getValue(Part);
        for (MachineLayout.Port Each : InletPorts) {
            if (Each.Part() == Index) {
                return GasHandler.fillOnly(Machine.getInletTank());
            }
        }
        return GasHandler.drainOnly(Machine.getExhaustTank());
    }

    @Override
    public BlockPos gasOwner(Level World, BlockPos Pos, BlockState State) {
        return masterPos(State, Pos);
    }

    @Override
    public boolean exhausts(BlockState State) {
        int Index = State.getValue(Part);
        for (MachineLayout.Port Each : ExhaustPorts) {
            if (Each.Part() == Index) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.steam_turbine.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.steam_turbine.size").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.steam_turbine.output", SteamTurbineBlockEntity.MaxOutput, SteamTurbineBlockEntity.MaxFlow * 20).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.steam_turbine.pressure").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.steam_turbine.ports").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.steam_turbine.wiring").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.steam_turbine.backup").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.steam_turbine.buffer", SteamTurbineBlockEntity.Capacity).withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Part) == Layout.Master() ? new SteamTurbineBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Part) != Layout.Master()) {
            return null;
        }
        return createTickerHelper(Type, SteamRegistry.SteamTurbineEntity.get(), World.isClientSide ? SteamTurbineBlockEntity::clientTick : SteamTurbineBlockEntity::serverTick);
    }
}
