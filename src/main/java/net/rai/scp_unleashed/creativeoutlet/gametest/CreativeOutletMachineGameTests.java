package net.rai.scp_unleashed.creativeoutlet.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.OutletBlock;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.cooler.CoolerBlock;
import net.rai.scp_unleashed.cooler.CoolerBlockEntity;
import net.rai.scp_unleashed.cooler.CoolerRegistry;
import net.rai.scp_unleashed.creativeoutlet.CreativeOutletBlockEntity;
import net.rai.scp_unleashed.creativeoutlet.CreativeOutletRegistry;
import net.rai.scp_unleashed.gateway.ContainedGatewayBlock;
import net.rai.scp_unleashed.gateway.ContainedGatewayBlockEntity;
import net.rai.scp_unleashed.gateway.GatewayGeometry;
import net.rai.scp_unleashed.registry.ModBlocks;
import net.rai.scp_unleashed.scp939.Scp939;
import net.rai.scp_unleashed.scp939.Scp939Registry;
import net.rai.scp_unleashed.scp939.Scp939Rooms;
import net.rai.scp_unleashed.scp939.compressor.CompressorStatus;
import net.rai.scp_unleashed.scp939.compressor.GasCompressorBlockEntity;
import net.rai.scp_unleashed.semanticblocker.SemanticBlockerBlockEntity;
import net.rai.scp_unleashed.semanticblocker.SemanticBlockerRegistry;
import net.rai.scp_unleashed.sulfur.SulfurRegistry;
import net.rai.scp_unleashed.sulfur.tank.IndustrialTankBlockEntity;
import net.rai.scp_unleashed.sulfur.tank.TankStatus;
import net.rai.scp_unleashed.wire.WireLink;
import net.rai.scp_unleashed.wire.WireNetwork;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import net.rai.scp_unleashed.wire.WireTier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class CreativeOutletMachineGameTests {
    public static final String Arena = "scp939_arena";
    public static final int Wait = 300;
    private static final BlockPos CoolerSpot = new BlockPos(4, 2, 4);
    private static final BlockPos TankSpot = new BlockPos(9, 2, 4);
    private static final BlockPos BlockerSpot = new BlockPos(14, 2, 4);
    private static final BlockPos GatewaySpot = new BlockPos(24, 2, 6);
    private static final BlockPos SourceSpot = new BlockPos(12, 7, 12);
    private static final int PillarBase = 2;
    private static final BlockPos RoomLow = new BlockPos(4, 2, 22);
    private static final int RoomSize = 7;
    private static final int RoomHeight = 3;
    private static final Direction[] MountOrder = {Direction.UP, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.NORTH};

    private CreativeOutletMachineGameTests() {
    }

    private record Machine(String Name, BlockPos Rel, BooleanSupplier Running, BooleanSupplier Stopped) {
    }

    private static BlockState creative() {
        return CreativeOutletRegistry.Outlet.get().defaultBlockState();
    }

    private static BlockState basic() {
        return ModBlocks.Outlet.get().defaultBlockState();
    }

    private static BlockPos mount(GameTestHelper Helper, BlockPos MachineRel, BlockState Outlet) {
        ServerLevel World = Helper.getLevel();
        for (Direction Side : MountOrder) {
            BlockPos Rel = MachineRel.relative(Side);
            BlockPos Abs = Helper.absolutePos(Rel);
            BlockState Candidate = Outlet.setValue(OutletBlock.Facing, Side);
            if (World.getBlockState(Abs).isAir() && Candidate.canSurvive(World, Abs)) {
                Helper.setBlock(Rel, Candidate);
                return Rel;
            }
        }
        Helper.fail("no face of " + Helper.getBlockState(MachineRel) + " takes an outlet");
        return MachineRel;
    }

    private static void link(OutletBlockEntity First, OutletBlockEntity Second, WireTier Tier) {
        int Slot = WireLink.freeSlot(First.getLinks(), Second.getLinks());
        First.addLink(new WireLink(Second.getBlockPos(), Second.getFacing(), Slot, Tier, false, null));
        Second.addLink(new WireLink(First.getBlockPos(), First.getFacing(), Slot, Tier, false, null));
    }

    private static <T extends BlockEntity> T entity(GameTestHelper Helper, BlockPos Rel, Class<T> Kind) {
        BlockEntity Found = Helper.getLevel().getBlockEntity(Helper.absolutePos(Rel));
        if (!Kind.isInstance(Found)) {
            Helper.fail("expected a " + Kind.getSimpleName() + " at " + Rel + " but found " + Found);
        }
        return Kind.cast(Found);
    }

    private static Machine cooler(GameTestHelper Helper) {
        Helper.setBlock(CoolerSpot, CoolerRegistry.Cooler.get().defaultBlockState().setValue(CoolerBlock.Facing, Direction.SOUTH));
        CoolerBlockEntity Device = entity(Helper, CoolerSpot, CoolerBlockEntity.class);
        return new Machine("cooler", CoolerSpot, Device::isActive, () -> !Device.isActive());
    }

    private static Machine tank(GameTestHelper Helper) {
        Helper.setBlock(TankSpot, SulfurRegistry.Tank.get().defaultBlockState());
        IndustrialTankBlockEntity Device = entity(Helper, TankSpot, IndustrialTankBlockEntity.class);
        Device.getStorage().setStackInSlot(IndustrialTankBlockEntity.SulfurSlot, new ItemStack(SulfurRegistry.Sulfur.get(), 64));
        Device.getWaterTank().fill(new FluidStack(Fluids.WATER, IndustrialTankBlockEntity.WaterCapacity), IFluidHandler.FluidAction.EXECUTE);
        return new Machine("industrial tank", TankSpot, () -> Device.status() == TankStatus.Running, () -> Device.status() == TankStatus.Unpowered);
    }

    private static Machine blocker(GameTestHelper Helper) {
        Helper.setBlock(BlockerSpot, SemanticBlockerRegistry.Blocker.get().defaultBlockState());
        SemanticBlockerBlockEntity Device = entity(Helper, BlockerSpot, SemanticBlockerBlockEntity.class);
        return new Machine("semantic blocker", BlockerSpot, Device::isActive, () -> !Device.isActive());
    }

    private static Machine compressor(GameTestHelper Helper) {
        for (int X = RoomLow.getX() - 1; X <= RoomLow.getX() + RoomSize; X++) {
            for (int Z = RoomLow.getZ() - 1; Z <= RoomLow.getZ() + RoomSize; Z++) {
                boolean Edge = X < RoomLow.getX() || X >= RoomLow.getX() + RoomSize || Z < RoomLow.getZ() || Z >= RoomLow.getZ() + RoomSize;
                for (int Y = RoomLow.getY(); Y < RoomLow.getY() + RoomHeight; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Edge ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
                Helper.setBlock(new BlockPos(X, RoomLow.getY() + RoomHeight, Z), Blocks.STONE_BRICKS);
                Helper.setBlock(new BlockPos(X, RoomLow.getY() - 1, Z), Blocks.SMOOTH_STONE);
            }
        }
        Scp939Rooms.forgetAll();
        for (BlockPos Spot : List.of(RoomLow.offset(1, 0, 1), RoomLow.offset(RoomSize - 2, 0, RoomSize - 2))) {
            Scp939 Made = Scp939Registry.Scp939Type.get().create(Helper.getLevel());
            Vec3 Where = Helper.absoluteVec(Vec3.atBottomCenterOf(Spot));
            Made.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
            Made.setPersistenceRequired();
            Made.setNoAi(true);
            Helper.getLevel().addFreshEntity(Made);
        }
        BlockPos Rel = RoomLow.offset(RoomSize / 2, 0, RoomSize / 2);
        Helper.setBlock(Rel, Scp939Registry.Compressor.get().defaultBlockState());
        GasCompressorBlockEntity Device = entity(Helper, Rel, GasCompressorBlockEntity.class);
        Device.getStorage().setStackInSlot(GasCompressorBlockEntity.InputSlot, new ItemStack(Scp939Registry.EmptyCanister.get(), 16));
        Device.recheck();
        return new Machine("gas compressor", Rel, () -> Device.status() == CompressorStatus.Running, () -> Device.status() == CompressorStatus.Unpowered);
    }

    private static Machine gateway(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Origin = Helper.absolutePos(GatewaySpot);
        ContainedGatewayBlock.assemble(World, Origin, Direction.NORTH);
        BlockPos Master = ContainedGatewayBlock.cellPos(Origin, Direction.NORTH, ContainedGatewayBlock.MasterPart);
        ContainedGatewayBlockEntity Gate = (ContainedGatewayBlockEntity) World.getBlockEntity(Master);
        BlockPos PortCell = ContainedGatewayBlock.cellPos(Origin, Direction.NORTH, ContainedGatewayBlock.PortPart);
        BlockPos PortRel = PortCell.subtract(Helper.absolutePos(BlockPos.ZERO));
        return new Machine("contained gateway", PortRel, () -> Gate.status() == ContainedGatewayBlockEntity.StatusStandby,
                () -> Gate.status() == ContainedGatewayBlockEntity.StatusOff);
    }

    private static CreativeOutletBlockEntity floorSource(GameTestHelper Helper) {
        for (int Y = PillarBase; Y < SourceSpot.getY(); Y++) {
            Helper.setBlock(new BlockPos(SourceSpot.getX(), Y, SourceSpot.getZ()), Blocks.SMOOTH_STONE);
        }
        Helper.setBlock(SourceSpot, creative().setValue(OutletBlock.Facing, Direction.UP));
        return entity(Helper, SourceSpot, CreativeOutletBlockEntity.class);
    }

    private static CreativeOutletBlockEntity roomSource(GameTestHelper Helper) {
        BlockPos Rel = RoomLow.offset(RoomSize - 2, 0, 1);
        Helper.setBlock(Rel, creative().setValue(OutletBlock.Facing, Direction.UP));
        return entity(Helper, Rel, CreativeOutletBlockEntity.class);
    }

    private static CreativeOutletBlockEntity gatewaySource(GameTestHelper Helper, BlockPos PortOutletRel) {
        Direction Side = GatewayGeometry.portSide(Direction.NORTH);
        BlockPos Rel = PortOutletRel.relative(Side, 3);
        StringBuilder Seen = new StringBuilder();
        for (int Step = 1; Step <= 3; Step++) {
            for (int Up = 0; Up <= 1; Up++) {
                BlockPos Cell = PortOutletRel.relative(Side, Step).above(Up);
                if (!Helper.getBlockState(Cell).isAir()) {
                    Seen.append(Cell).append('=').append(Helper.getBlockState(Cell).getBlock()).append(' ');
                }
            }
        }
        Helper.assertTrue(Seen.isEmpty(), "the gateway wire path is blocked: " + Seen);
        Helper.setBlock(Rel.relative(Side), Blocks.SMOOTH_STONE);
        Helper.setBlock(Rel, creative().setValue(OutletBlock.Facing, Side.getOpposite()));
        return entity(Helper, Rel, CreativeOutletBlockEntity.class);
    }

    private static BlockPos mountOnGateway(GameTestHelper Helper, BlockState Outlet) {
        Direction Side = GatewayGeometry.portSide(Direction.NORTH);
        BlockPos PortCell = ContainedGatewayBlock.cellPos(Helper.absolutePos(GatewaySpot), Direction.NORTH, ContainedGatewayBlock.PortPart);
        BlockPos Rel = PortCell.relative(Side).subtract(Helper.absolutePos(BlockPos.ZERO));
        Helper.assertTrue(Outlet.setValue(OutletBlock.Facing, Side).canSurvive(Helper.getLevel(), Helper.absolutePos(Rel)), "the outlet does not fit the gateway's power port");
        Helper.setBlock(Rel, Outlet.setValue(OutletBlock.Facing, Side));
        return Rel;
    }

    private static List<Machine> machines(GameTestHelper Helper) {
        return List.of(cooler(Helper), tank(Helper), blocker(Helper), compressor(Helper), gateway(Helper));
    }

    private static BlockPos mountOn(GameTestHelper Helper, Machine Target, BlockState Outlet) {
        return Target.Name().equals("contained gateway") ? mountOnGateway(Helper, Outlet) : mount(Helper, Target.Rel(), Outlet);
    }

    private static String describe(GameTestHelper Helper, BlockPos OutletRel) {
        WireNetwork Network = WireNetworkManager.get(Helper.getLevel()).networkAt(Helper.getLevel(), Helper.absolutePos(OutletRel));
        WireNetwork SourceNetwork = WireNetworkManager.get(Helper.getLevel()).networkAt(Helper.getLevel(), Helper.absolutePos(SourceSpot));
        String Links = Helper.getLevel().getBlockEntity(Helper.absolutePos(SourceSpot)) instanceof OutletBlockEntity Found ? " links " + Found.getLinks().size() : " no source entity";
        String Source = " (source " + Helper.getBlockState(SourceSpot).getBlock() + Links + ", same network " + (SourceNetwork == Network) + ", source devices "
                + (SourceNetwork == null ? -1 : SourceNetwork.getLastDeviceCount()) + ", outlets " + (Network == null ? -1 : Network.getOutlets().size()) + ")";
        return Network == null ? " (no network)" + Source : Source + " (network devices " + Network.getLastDeviceCount() + ", supply " + Network.getLastSupply() + ", demand " + Network.getLastDemand()
                + ", delivered " + Network.getLastTransferred() + ")";
    }

    private static void allRunning(GameTestHelper Helper, List<Machine> Targets, List<BlockPos> Outlets, String How) {
        for (int Index = 0; Index < Targets.size(); Index++) {
            Machine Target = Targets.get(Index);
            Helper.assertTrue(Target.Running().getAsBoolean(), "the " + Target.Name() + " " + How + " is not running" + describe(Helper, Outlets.get(Index)));
        }
    }

    private static void reload(GameTestHelper Helper, BlockPos Rel) {
        ServerLevel World = Helper.getLevel();
        BlockPos Abs = Helper.absolutePos(Rel);
        BlockEntity Old = World.getBlockEntity(Abs);
        CompoundTag Saved = Old.saveWithFullMetadata(World.registryAccess());
        BlockState State = World.getBlockState(Abs);
        Old.onChunkUnloaded();
        World.removeBlockEntity(Abs);
        BlockEntity Fresh = BlockEntity.loadStatic(Abs, State, Saved, World.registryAccess());
        Helper.assertTrue(Fresh != null, "the outlet at " + Rel + " did not load back from its save");
        World.setBlockEntity(Fresh);
        Fresh.onLoad();
    }

    @GameTest(template = Arena, timeoutTicks = Wait, batch = "outlet_creative_mounted")
    public static void mountedOnMachinesPowersThem(GameTestHelper Helper) {
        List<Machine> Targets = machines(Helper);
        List<BlockPos> Outlets = new ArrayList<>();
        for (Machine Target : Targets) {
            Outlets.add(mountOn(Helper, Target, creative()));
        }
        Helper.startSequence().thenWaitUntil(() -> allRunning(Helper, Targets, Outlets, "with a Creative Outlet mounted on it")).thenSucceed();
    }

    private static void wired(GameTestHelper Helper, WireTier Tier) {
        List<Machine> Targets = machines(Helper);
        CreativeOutletBlockEntity Source = floorSource(Helper);
        CreativeOutletBlockEntity Inside = roomSource(Helper);
        List<BlockPos> Outlets = new ArrayList<>();
        List<CreativeOutletBlockEntity> Feeders = new ArrayList<>();
        for (Machine Target : Targets) {
            BlockPos Rel = mountOn(Helper, Target, basic());
            Outlets.add(Rel);
            CreativeOutletBlockEntity Feeder = switch (Target.Name()) {
                case "gas compressor" -> Inside;
                case "contained gateway" -> gatewaySource(Helper, Rel);
                default -> Source;
            };
            Feeders.add(Feeder);
            link(Feeder, entity(Helper, Rel, OutletBlockEntity.class), Tier);
        }
        Helper.startSequence().thenWaitUntil(() -> allRunning(Helper, Targets, Outlets, "wired to a Creative Outlet over " + Tier.getSerializedName() + " wire")).thenExecute(() -> {
            for (int Index = 0; Index < Outlets.size(); Index++) {
                Helper.assertTrue(Feeders.get(Index).isLinkedTo(Helper.absolutePos(Outlets.get(Index))), "a " + Tier.getSerializedName() + " wire burnt out");
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = Wait, batch = "outlet_creative_wired_coated")
    public static void wiredOverCoatedWire(GameTestHelper Helper) {
        wired(Helper, WireTier.Coated);
    }

    @GameTest(template = Arena, timeoutTicks = Wait, batch = "outlet_creative_wired_advanced")
    public static void wiredOverAdvancedWire(GameTestHelper Helper) {
        wired(Helper, WireTier.Advanced);
    }

    @GameTest(template = Arena, timeoutTicks = Wait, batch = "outlet_creative_both")
    public static void mountedAndWiredAtOnce(GameTestHelper Helper) {
        Machine Host = cooler(Helper);
        Machine Far = blocker(Helper);
        BlockPos SourceRel = mount(Helper, Host.Rel(), creative());
        BlockPos FarRel = mount(Helper, Far.Rel(), basic());
        link(entity(Helper, SourceRel, CreativeOutletBlockEntity.class), entity(Helper, FarRel, OutletBlockEntity.class), WireTier.Coated);
        Helper.startSequence().thenWaitUntil(() -> allRunning(Helper, List.of(Host, Far), List.of(SourceRel, FarRel), "on a Creative Outlet that is both mounted and wired")).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "outlet_creative_replace")
    public static void breakAndReplace(GameTestHelper Helper) {
        Machine Mounted = cooler(Helper);
        Machine Wired = blocker(Helper);
        BlockPos MountedRel = mount(Helper, Mounted.Rel(), creative());
        BlockPos FarRel = mount(Helper, Wired.Rel(), basic());
        link(floorSource(Helper), entity(Helper, FarRel, OutletBlockEntity.class), WireTier.Coated);
        BlockState MountedState = Helper.getBlockState(MountedRel);
        List<Machine> Both = List.of(Mounted, Wired);
        List<BlockPos> Outlets = List.of(MountedRel, FarRel);
        Helper.startSequence().thenWaitUntil(() -> allRunning(Helper, Both, Outlets, "before anything is broken")).thenExecute(() -> {
            Helper.getLevel().destroyBlock(Helper.absolutePos(MountedRel), false);
            Helper.getLevel().destroyBlock(Helper.absolutePos(SourceSpot), false);
        }).thenWaitUntil(() -> {
            Helper.assertTrue(Mounted.Stopped().getAsBoolean(), "the cooler kept running after its Creative Outlet was broken");
            Helper.assertTrue(Wired.Stopped().getAsBoolean(), "the blocker kept running after the Creative Outlet feeding it was broken");
        }).thenExecute(() -> {
            Helper.setBlock(MountedRel, MountedState);
            link(floorSource(Helper), entity(Helper, FarRel, OutletBlockEntity.class), WireTier.Coated);
        }).thenWaitUntil(() -> allRunning(Helper, Both, Outlets, "after the Creative Outlets were replaced")).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "outlet_creative_reload")
    public static void survivesReload(GameTestHelper Helper) {
        Machine Mounted = cooler(Helper);
        Machine Wired = tank(Helper);
        BlockPos MountedRel = mount(Helper, Mounted.Rel(), creative());
        BlockPos FarRel = mount(Helper, Wired.Rel(), basic());
        link(floorSource(Helper), entity(Helper, FarRel, OutletBlockEntity.class), WireTier.Advanced);
        List<Machine> Both = List.of(Mounted, Wired);
        List<BlockPos> Outlets = List.of(MountedRel, FarRel);
        Helper.startSequence().thenWaitUntil(() -> allRunning(Helper, Both, Outlets, "before the reload")).thenExecute(() -> {
            for (BlockPos Rel : List.of(MountedRel, SourceSpot, FarRel)) {
                reload(Helper, Rel);
            }
            Helper.assertTrue(entity(Helper, SourceSpot, CreativeOutletBlockEntity.class).isLinkedTo(Helper.absolutePos(FarRel)), "the wire was lost in the save");
        }).thenIdle(CoolerBlockEntity.PowerGrace + 20).thenExecute(() -> allRunning(Helper, Both, Outlets, "after the outlets were reloaded")).thenSucceed();
    }
}
