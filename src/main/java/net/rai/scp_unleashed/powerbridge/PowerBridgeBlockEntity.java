package net.rai.scp_unleashed.powerbridge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.creativeoutlet.CreativeOutletBlock;
import net.rai.scp_unleashed.wire.WireConnector;
import net.rai.scp_unleashed.wire.WireDevice;
import net.rai.scp_unleashed.wire.WireLink;
import net.rai.scp_unleashed.wire.WireTier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PowerBridgeBlockEntity extends BlockEntity implements WireDevice {
    public static final int BufferCapacity = 100_000;
    public static final int CeilingUnits = 50_000;
    public static final int CeilingFe = (int) PowerBridgeConversion.feForUnits(CeilingUnits);
    public static final int MinTarget = 1_000;
    public static final int TargetTicks = 2;
    public static final int LineRefreshTicks = 10;
    public static final int ActiveHold = 20;
    public static final int RateWindow = 20;
    public static final int HumMin = 96;
    public static final int HumMax = 112;
    public static final int HumStart = 6;
    public static final double HumReach = 12.0;
    public static final float HumVolume = 1.0f;
    public static final float SwitchVolume = 0.5f;
    public static final long CapacityMilli = PowerBridgeConversion.milli(BufferCapacity);

    private final PowerBridgeKind Kind;
    private final Port Energy = new Port();
    private long BufferMilli;
    private int LineCapacity;
    private boolean Linked;
    private boolean HasNeighbour;
    private boolean LineDirty = true;
    private int LineTimer;
    private long TargetMilli = PowerBridgeConversion.milli(MinTarget);
    private long FeThisTick;
    private long UnitsThisTick;
    private long ReceivedThisTick;
    private long OutflowThisTick;
    private long FeWindow;
    private long UnitsWindow;
    private int WindowTicks;
    private int ShownFe;
    private int ShownUnits;
    private int ActiveTimer;
    private int HumTimer;
    @Nullable
    private BlockCapabilityCache<IEnergyStorage, Direction> FrontCache;
    @Nullable
    private Direction CachedFront;

    public PowerBridgeBlockEntity(PowerBridgeKind Kind, BlockPos Pos, BlockState State) {
        super(PowerBridgeRegistry.entityType(Kind), Pos, State);
        this.Kind = Kind;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, PowerBridgeBlockEntity Bridge) {
        if (World instanceof ServerLevel Server) {
            Bridge.tickServer(Server, Pos, State);
        }
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        if (LineDirty || --LineTimer <= 0) {
            refreshLine(World, Pos);
        }
        IEnergyStorage Neighbour = neighbour(World, Pos, State);
        HasNeighbour = Neighbour != null;
        if (Neighbour != null) {
            if (Kind.takesFe()) {
                pullFrom(Neighbour);
            } else {
                pushTo(Neighbour);
            }
        }
        if (Kind.givesFe()) {
            long Wanted = Math.max(MinTarget, OutflowThisTick * TargetTicks);
            TargetMilli = Math.min(CapacityMilli, PowerBridgeConversion.milli(Wanted));
        }
        OutflowThisTick = 0;
        ReceivedThisTick = 0;
        boolean Moved = FeThisTick > 0 || UnitsThisTick > 0;
        FeWindow += FeThisTick;
        UnitsWindow += UnitsThisTick;
        FeThisTick = 0;
        UnitsThisTick = 0;
        if (++WindowTicks >= RateWindow) {
            ShownFe = (int) Math.min(Integer.MAX_VALUE, Math.round((double) FeWindow / WindowTicks));
            ShownUnits = (int) Math.min(Integer.MAX_VALUE, Math.round((double) UnitsWindow / WindowTicks));
            FeWindow = 0;
            UnitsWindow = 0;
            WindowTicks = 0;
        }
        ActiveTimer = Moved ? ActiveHold : Math.max(0, ActiveTimer - 1);
        boolean Active = ActiveTimer > 0;
        if (State.getValue(PowerBridgeBlock.Active) != Active) {
            World.setBlock(Pos, State.setValue(PowerBridgeBlock.Active, Active), Block.UPDATE_CLIENTS);
            World.playSound(null, Pos, PowerBridgeRegistry.Switch.get(), SoundSource.BLOCKS, SwitchVolume, Active ? 1.1f : 0.8f);
            HumTimer = HumStart;
        }
        if (Active && --HumTimer <= 0) {
            HumTimer = Mth.nextInt(World.getRandom(), HumMin, HumMax);
            if (World.hasNearbyAlivePlayer(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, HumReach)) {
                play(World, Pos, PowerBridgeRegistry.humLayers(Kind));
            }
        }
    }

    private static void play(ServerLevel World, BlockPos Pos, List<DeferredHolder<SoundEvent, SoundEvent>> Layers) {
        float Pitch = 0.96f + World.getRandom().nextFloat() * 0.08f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Pos, Layer.get(), SoundSource.BLOCKS, HumVolume, Pitch);
        }
    }

    @Nullable
    private IEnergyStorage neighbour(ServerLevel World, BlockPos Pos, BlockState State) {
        Direction Front = State.getValue(PowerBridgeBlock.Facing);
        if (FrontCache == null || CachedFront != Front) {
            CachedFront = Front;
            FrontCache = BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, World, Pos.relative(Front), Front.getOpposite());
        }
        return FrontCache.getCapability();
    }

    public void lineChanged() {
        LineDirty = true;
    }

    public void refreshLine(ServerLevel World, BlockPos Pos) {
        LineDirty = false;
        LineTimer = LineRefreshTicks;
        long Total = 0;
        boolean Wired = false;
        for (Direction Side : Direction.values()) {
            BlockPos Next = Pos.relative(Side);
            if (!World.isLoaded(Next)) {
                continue;
            }
            BlockState Other = World.getBlockState(Next);
            if (!(Other.getBlock() instanceof WireConnector Connector) || !Connector.hostPos(Other, Next).equals(Pos)) {
                continue;
            }
            if (Other.getBlock() instanceof CreativeOutletBlock) {
                Wired = true;
                Total += CeilingUnits;
                continue;
            }
            if (!(World.getBlockEntity(Next) instanceof OutletBlockEntity Outlet) || Outlet.getLinks().isEmpty()) {
                continue;
            }
            long Carried = 0;
            for (WireLink Link : Outlet.getLinks()) {
                Carried += Link.Tier() == WireTier.Creative ? CeilingUnits : Link.Tier().getMaxDemand();
            }
            boolean Creative = Outlet.getLinks().stream().anyMatch(Link -> Link.Tier() == WireTier.Creative);
            Wired = true;
            Total += Creative ? Carried : Math.min(Carried, Connector.getConnectorTier().getMaxDemand());
        }
        Linked = Wired;
        LineCapacity = (int) Math.min(CeilingUnits, Total);
    }

    public int pullFrom(IEnergyStorage Source) {
        if (!Kind.takesFe() || !Linked || !Source.canExtract()) {
            return 0;
        }
        long Want = Math.min(roomFe(), CeilingFe - ReceivedThisTick);
        if (Want <= 0) {
            return 0;
        }
        int Got = Source.extractEnergy((int) Math.min(Integer.MAX_VALUE, Want), false);
        if (Got > 0) {
            accept(Got);
        }
        return Math.max(0, Got);
    }

    public int pushTo(IEnergyStorage Target) {
        if (!Kind.givesFe() || !Target.canReceive()) {
            return 0;
        }
        long Offer = Math.min(PowerBridgeConversion.wholeFe(BufferMilli), CeilingFe);
        if (Offer <= 0) {
            return 0;
        }
        int Sent = Target.receiveEnergy((int) Offer, false);
        if (Sent > 0) {
            release(Math.min(Sent, Offer));
        }
        return Math.max(0, Sent);
    }

    private long roomFe() {
        return Math.max(0L, (CapacityMilli - BufferMilli) / PowerBridgeConversion.Milli);
    }

    private void accept(long Fe) {
        BufferMilli = Math.min(CapacityMilli, BufferMilli + PowerBridgeConversion.milli(Fe));
        ReceivedThisTick += Fe;
        FeThisTick += Fe;
        setChanged();
    }

    private void release(long Fe) {
        BufferMilli = Math.max(0L, BufferMilli - PowerBridgeConversion.milli(Fe));
        OutflowThisTick += Fe;
        FeThisTick += Fe;
        setChanged();
    }

    @Nullable
    public IEnergyStorage energyFor(@Nullable Direction Side) {
        return Side != null && Side == getBlockState().getValue(PowerBridgeBlock.Facing) ? Energy : null;
    }

    public IEnergyStorage port() {
        return Energy;
    }

    public PowerBridgeKind getKind() {
        return Kind;
    }

    public long getBufferMilli() {
        return BufferMilli;
    }

    public int getBufferFe() {
        return (int) Math.min(Integer.MAX_VALUE, PowerBridgeConversion.wholeFe(BufferMilli));
    }

    public void setBufferFe(int Fe) {
        BufferMilli = Math.max(0L, Math.min(CapacityMilli, PowerBridgeConversion.milli(Fe)));
        setChanged();
    }

    public int getLineCapacity() {
        return LineCapacity;
    }

    public boolean isLinked() {
        return Linked;
    }

    public boolean hasNeighbour() {
        return HasNeighbour;
    }

    public int getShownFe() {
        return ShownFe;
    }

    public int getShownUnits() {
        return ShownUnits;
    }

    public boolean isActive() {
        BlockState State = getBlockState();
        return State.hasProperty(PowerBridgeBlock.Active) && State.getValue(PowerBridgeBlock.Active);
    }

    @Override
    public boolean isGenerator() {
        return Kind.takesFe();
    }

    @Override
    public int getPowerOutput() {
        if (!Kind.takesFe() || !Linked) {
            return 0;
        }
        return (int) Math.min(LineCapacity, PowerBridgeConversion.unitsAvailable(BufferMilli));
    }

    @Override
    public void extractPower(int Amount) {
        if (!Kind.takesFe() || Amount <= 0) {
            return;
        }
        BufferMilli = Math.max(0L, BufferMilli - PowerBridgeConversion.costMilli(Amount));
        UnitsThisTick += Amount;
        setChanged();
    }

    @Override
    public int getGenerationRate() {
        return Kind.takesFe() ? ShownUnits : 0;
    }

    @Override
    public int getStoredPower() {
        return Kind.takesFe() ? (int) Math.min(Integer.MAX_VALUE, PowerBridgeConversion.unitsAvailable(BufferMilli)) : 0;
    }

    @Override
    public int getPowerCapacity() {
        return Kind.takesFe() ? (int) Math.min(Integer.MAX_VALUE, PowerBridgeConversion.unitsAvailable(CapacityMilli)) : 0;
    }

    @Override
    public int getPowerDemand() {
        if (!Kind.givesFe() || !Linked || !HasNeighbour) {
            return 0;
        }
        return (int) Math.min(LineCapacity, PowerBridgeConversion.unitsToFill(TargetMilli - BufferMilli));
    }

    @Override
    public void insertPower(int Amount) {
        if (!Kind.givesFe() || Amount <= 0) {
            return;
        }
        BufferMilli = Math.min(CapacityMilli, BufferMilli + PowerBridgeConversion.gainMilli(Amount));
        UnitsThisTick += Amount;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putLong("Buffer", BufferMilli);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        BufferMilli = Math.max(0L, Math.min(CapacityMilli, Tag.getLong("Buffer")));
        LineDirty = true;
    }

    private final class Port implements IEnergyStorage {
        @Override
        public int receiveEnergy(int ToReceive, boolean Simulate) {
            if (!canReceive() || ToReceive <= 0) {
                return 0;
            }
            long Allowed = Math.min(roomFe(), CeilingFe - ReceivedThisTick);
            int Accepted = (int) Math.max(0L, Math.min(ToReceive, Allowed));
            if (!Simulate && Accepted > 0) {
                accept(Accepted);
            }
            return Accepted;
        }

        @Override
        public int extractEnergy(int ToExtract, boolean Simulate) {
            if (!canExtract() || ToExtract <= 0) {
                return 0;
            }
            int Given = (int) Math.max(0L, Math.min(ToExtract, Math.min(PowerBridgeConversion.wholeFe(BufferMilli), CeilingFe)));
            if (!Simulate && Given > 0) {
                release(Given);
            }
            return Given;
        }

        @Override
        public int getEnergyStored() {
            return getBufferFe();
        }

        @Override
        public int getMaxEnergyStored() {
            return BufferCapacity;
        }

        @Override
        public boolean canExtract() {
            return Kind.givesFe();
        }

        @Override
        public boolean canReceive() {
            return Kind.takesFe() && Linked;
        }
    }
}
