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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.rai.scp_unleashed.block.FluidHands;
import net.rai.scp_unleashed.gas.GasHandler;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.GasPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.RestrictedFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CoolingTowerBlock extends SteamMachineBlock implements FluidPort, GasPort {
    public static final MapCodec<CoolingTowerBlock> BlockCodec = simpleCodec(CoolingTowerBlock::new);
    public static final MachineLayout Layout = MachineLayout.of(3, 8, 3, 1, 0, 1);
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, Layout.count() - 1);
    public static final int[][] Bands = {{18, 22, 23}, {22, 26, 22}, {26, 30, 21}, {30, 34, 20}, {34, 42, 19}, {42, 46, 18}, {46, 54, 17}, {54, 58, 16},
            {58, 66, 15}, {66, 78, 14}, {78, 106, 13}, {106, 118, 14}, {118, 126, 15}, {126, 128, 17}};
    public static final int Shell = 2;
    public static final int Span = 48;
    public static final List<MachineLayout.Port> SteamPorts = List.of(
            new MachineLayout.Port(Layout.index(0, 0, 0), Direction.WEST), new MachineLayout.Port(Layout.index(0, 0, 1), Direction.WEST),
            new MachineLayout.Port(Layout.index(0, 0, 2), Direction.WEST), new MachineLayout.Port(Layout.index(2, 0, 0), Direction.EAST),
            new MachineLayout.Port(Layout.index(2, 0, 1), Direction.EAST), new MachineLayout.Port(Layout.index(2, 0, 2), Direction.EAST));
    public static final List<MachineLayout.Port> WaterPorts = List.of(new MachineLayout.Port(Layout.index(1, 0, 0), Direction.NORTH),
            new MachineLayout.Port(Layout.index(1, 0, 2), Direction.SOUTH));
    private static final VoxelShape[][] CellShapes = Layout.shapes(boxes());

    public CoolingTowerBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, Layout.origin()).setValue(Light, MachineLight.Off));
    }

    public static int chamfer(int Apothem) {
        return Math.round(Apothem * 0.586f);
    }

    private static List<AABB> boxes() {
        List<AABB> Found = new ArrayList<>();
        Found.add(MachineLayout.pixels(0, 0, 0, 48, 3, 48));
        Found.add(MachineLayout.pixels(0, 3, 0, 48, 5, 3));
        Found.add(MachineLayout.pixels(0, 3, 45, 48, 5, 48));
        Found.add(MachineLayout.pixels(0, 3, 3, 3, 5, 45));
        Found.add(MachineLayout.pixels(45, 3, 3, 48, 5, 45));
        Found.add(MachineLayout.pixels(19, 2, 0, 29, 12, 6));
        Found.add(MachineLayout.pixels(19, 2, 42, 29, 12, 48));
        for (int Row = 0; Row < 3; Row++) {
            Found.add(MachineLayout.pixels(0, 4, Row * 16 + 3, 6, 14, Row * 16 + 13));
            Found.add(MachineLayout.pixels(42, 4, Row * 16 + 3, 48, 14, Row * 16 + 13));
        }
        for (int[] Leg : new int[][]{{8, 8}, {37, 8}, {8, 37}, {37, 37}}) {
            Found.add(MachineLayout.pixels(Leg[0], 5, Leg[1], Leg[0] + 3, 18, Leg[1] + 3));
        }
        Found.add(MachineLayout.pixels(3, 19, 3, 45, 21, 45));
        double Middle = Span / 2.0;
        for (int[] Band : Bands) {
            double Outer = Band[2];
            double Flat = Outer - chamfer(Band[2]);
            double Cut = chamfer(Band[2]);
            Found.add(MachineLayout.pixels(Middle - Flat, Band[0], Middle - Outer, Middle + Flat, Band[1], Middle - Outer + Shell));
            Found.add(MachineLayout.pixels(Middle - Flat, Band[0], Middle + Outer - Shell, Middle + Flat, Band[1], Middle + Outer));
            Found.add(MachineLayout.pixels(Middle - Outer, Band[0], Middle - Flat, Middle - Outer + Shell, Band[1], Middle + Flat));
            Found.add(MachineLayout.pixels(Middle + Outer - Shell, Band[0], Middle - Flat, Middle + Outer, Band[1], Middle + Flat));
            double Half = Cut / 6.0 + 1.0;
            for (int Step = 0; Step < 3; Step++) {
                double Share = (Step + 0.5) / 3.0;
                double Along = Flat + Share * Cut;
                double Across = Outer - Share * Cut;
                for (int SignX = -1; SignX <= 1; SignX += 2) {
                    for (int SignZ = -1; SignZ <= 1; SignZ += 2) {
                        double X = Middle + SignX * Along;
                        double Z = Middle + SignZ * Across;
                        Found.add(MachineLayout.pixels(X - Half, Band[0], Z - Half, X + Half, Band[1], Z + Half));
                    }
                }
            }
        }
        return Found;
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
    protected boolean isPortCell(int Index) {
        for (MachineLayout.Port Each : SteamPorts) {
            if (Each.Part() == Index) {
                return true;
            }
        }
        for (MachineLayout.Port Each : WaterPorts) {
            if (Each.Part() == Index) {
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

    public boolean isWaterFace(BlockState State, Direction Side) {
        for (MachineLayout.Port Each : WaterPorts) {
            if (isPort(State, Side, Each.Part(), Each.Local())) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static CoolingTowerBlockEntity machineAt(BlockGetter World, BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof CoolingTowerBlock Tower)) {
            return null;
        }
        return World.getBlockEntity(Tower.masterPos(State, Pos)) instanceof CoolingTowerBlockEntity Machine ? Machine : null;
    }

    @Nullable
    public static IFluidHandler capabilityAt(Level World, BlockPos Pos, BlockState State, @Nullable Direction Side) {
        if (!(State.getBlock() instanceof CoolingTowerBlock Tower) || Side == null || !Tower.isWaterFace(State, Side)) {
            return null;
        }
        CoolingTowerBlockEntity Machine = machineAt(World, Pos, State);
        return Machine == null ? null : new RestrictedFluidHandler(Machine.getWaterTank(), PortRole.Output);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void dismantled(ServerLevel World, BlockPos MasterPos, BlockState MasterState) {
        if (World.getBlockEntity(MasterPos) instanceof CoolingTowerBlockEntity Machine) {
            GasStack Held = Machine.getSteamTank().getGas();
            if (!Held.isEmpty()) {
                Held.getGas().release(World, MasterPos.above().getCenter(), Held.getAmount());
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            CoolingTowerBlockEntity Machine = machineAt(World, Pos, State);
            if (Machine != null) {
                User.displayClientMessage(Machine.describe(), true);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (FluidUtil.getFluidHandler(Stack.copyWithCount(1)).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        CoolingTowerBlockEntity Machine = machineAt(World, Pos, State);
        if (Machine != null && FluidHands.interact(User, Hand, new RestrictedFluidHandler(Machine.getWaterTank(), PortRole.Output))) {
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    @Override
    public PortRole portRole(BlockState State, Direction Side) {
        return isWaterFace(State, Side) ? PortRole.Output : null;
    }

    @Nullable
    @Override
    public IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State) {
        CoolingTowerBlockEntity Machine = machineAt(World, Pos, State);
        return Machine == null ? null : new RestrictedFluidHandler(Machine.getWaterTank(), PortRole.Output);
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return masterPos(State, Pos);
    }

    @Nullable
    @Override
    public PortRole gasRole(BlockState State, Direction Side) {
        return isSteamFace(State, Side) ? PortRole.Input : null;
    }

    @Nullable
    @Override
    public GasHandler gasHandler(Level World, BlockPos Pos, BlockState State) {
        CoolingTowerBlockEntity Machine = machineAt(World, Pos, State);
        return Machine == null ? null : GasHandler.fillOnly(Machine.getSteamTank());
    }

    @Override
    public BlockPos gasOwner(Level World, BlockPos Pos, BlockState State) {
        return masterPos(State, Pos);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.cooling_tower.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.cooling_tower.size").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.cooling_tower.rate", CoolingTowerBlockEntity.CondenseRate * 20).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.cooling_tower.returns", Math.round(CoolingTowerBlockEntity.MinReturn * 100), Math.round(CoolingTowerBlockEntity.MaxReturn * 100))
                    .withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.cooling_tower.ports").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.cooling_tower.full").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Part) == Layout.Master() ? new CoolingTowerBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Part) != Layout.Master()) {
            return null;
        }
        return createTickerHelper(Type, SteamRegistry.CoolingTowerEntity.get(), World.isClientSide ? CoolingTowerBlockEntity::clientTick : CoolingTowerBlockEntity::serverTick);
    }
}
