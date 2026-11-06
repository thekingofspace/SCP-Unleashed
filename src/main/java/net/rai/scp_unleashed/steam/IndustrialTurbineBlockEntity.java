package net.rai.scp_unleashed.steam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.gas.GasTank;
import net.rai.scp_unleashed.wire.WireDevice;

import java.util.function.Consumer;

public class IndustrialTurbineBlockEntity extends BlockEntity implements WireDevice {
    public static final int InletCount = 3;
    public static final int InletCapacity = 8000;
    public static final int ExhaustCapacity = 24000;
    public static final int InletFlow = 100;
    public static final int MaxFlow = InletFlow * InletCount;
    public static final int PowerPerSteam = 7;
    public static final int MaxOutput = MaxFlow * PowerPerSteam;
    public static final int Capacity = 250000;
    public static final float FullFlowShare = 0.5f;
    public static final float Breakaway = 0.1f;
    public static final int SpinUpTicks = 300;
    public static final int SpinDownTicks = 400;
    public static final float SpinUpStep = 1.0f / SpinUpTicks;
    public static final float SpinDownStep = 1.0f / SpinDownTicks;
    public static final int SpinSteps = 32;
    public static final int SyncTicks = 10;
    public static final int LightCheckTicks = 40;
    public static final float MaxSpinDegrees = 13.0f;
    public static final float SpinEase = 0.06f;
    public static final float SpinFloor = 0.0008f;
    public static final int FillSteps = 32;
    public static final float FillEase = 0.04f;
    public static final float FillFloor = 0.002f;

    private static Consumer<IndustrialTurbineBlockEntity> ClientEffects = Machine -> {
    };

    private final GasTank[] Inlets = new GasTank[InletCount];
    private final GasTank Exhaust = new GasTank(ExhaustCapacity, Kind -> Kind == GasRegistry.LowPressureSteam.get(), this::setChanged);

    private int Stored;
    private float FlowCarry;
    private int LastFlow;
    private int LastGenerated;
    private float Momentum;
    private TurbineStatus Status = TurbineStatus.Idle;
    private MachineLight LastLight = MachineLight.Off;
    private int Rate;
    private boolean NeedsSync;
    private boolean UrgentSync;
    private long LastSync;
    private boolean Primed;
    private float Spin;
    private float PreviousSpin;
    private float Angle;
    private float PreviousAngle;
    private final int[] Fill = new int[InletCount];
    private final float[] ShownFill = new float[InletCount];
    private final float[] PreviousFill = new float[InletCount];
    private boolean FillPrimed;

    public IndustrialTurbineBlockEntity(BlockPos Pos, BlockState State) {
        super(SteamRegistry.IndustrialTurbineEntity.get(), Pos, State);
        for (int Index = 0; Index < InletCount; Index++) {
            Inlets[Index] = new GasTank(InletCapacity, Kind -> Kind == GasRegistry.Steam.get(), this::setChanged);
        }
    }

    public static void setClientEffects(Consumer<IndustrialTurbineBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public GasTank getInletTank(int Index) {
        return Inlets[Index];
    }

    public GasTank getExhaustTank() {
        return Exhaust;
    }

    public TurbineStatus getStatus() {
        return Status;
    }

    public int getLastFlow() {
        return LastFlow;
    }

    public int getLastGenerated() {
        return LastGenerated;
    }

    public float getMomentum() {
        return Momentum;
    }

    public int getSpeedPercent() {
        return Math.round(Momentum * 100.0f);
    }

    public int getPressurePercent(int Index) {
        return Math.round(Inlets[Index].getAmount() * 100.0f / Inlets[Index].getCapacity());
    }

    public int getInletAmount() {
        int Total = 0;
        for (GasTank Inlet : Inlets) {
            Total += Inlet.getAmount();
        }
        return Total;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, IndustrialTurbineBlockEntity Machine) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof IndustrialTurbineBlock) {
            Machine.tickServer(ServerWorld);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, IndustrialTurbineBlockEntity Machine) {
        Machine.tickClient();
        ClientEffects.accept(Machine);
    }

    public static float flowFor(int Held, int Capacity) {
        if (Held <= 0) {
            return 0.0f;
        }
        float Share = Math.min(1.0f, Held / (Capacity * FullFlowShare));
        return Math.max(1.0f, InletFlow * Share);
    }

    private void tickServer(ServerLevel World) {
        float[] Offers = new float[InletCount];
        float Wanted = 0.0f;
        for (int Index = 0; Index < InletCount; Index++) {
            Offers[Index] = flowFor(Inlets[Index].getAmount(), Inlets[Index].getCapacity());
            Wanted += Offers[Index];
        }
        int Held = getInletAmount();
        int RoomForPower = (Capacity - Stored) / PowerPerSteam;
        int Potential = Math.min((int) Wanted, Math.min(Held, Math.min(Exhaust.getSpace(), RoomForPower)));
        float Allowed = Math.min(Wanted, MaxFlow * Math.max(Breakaway, Momentum)) + FlowCarry;
        int Whole = (int) Allowed;
        int Flow = Math.min(Whole, Math.min(Held, Math.min(Exhaust.getSpace(), RoomForPower)));
        FlowCarry = Flow == Whole ? Allowed - Whole : 0.0f;
        if (Flow > 0) {
            drainInlets(Flow, Offers, Wanted);
            Exhaust.fill(new GasStack(GasRegistry.LowPressureSteam.get(), Flow), false);
            Stored += Flow * PowerPerSteam;
            setChanged();
        }
        LastFlow = Flow;
        LastGenerated = Flow * PowerPerSteam;
        float Target = Mth.clamp((float) Potential / MaxFlow, 0.0f, 1.0f);
        if (Momentum < Target) {
            Momentum = Math.min(Target, Momentum + SpinUpStep);
        } else if (Momentum > Target) {
            Momentum = Math.max(Target, Momentum - SpinDownStep);
        }
        if (Held <= 0 && Flow == 0) {
            Status = TurbineStatus.Idle;
        } else if (Flow < Whole && Exhaust.getSpace() <= Flow) {
            Status = Flow == 0 ? TurbineStatus.Backed : TurbineStatus.Throttled;
        } else if (Flow < Whole && RoomForPower <= Flow) {
            Status = TurbineStatus.NoLoad;
        } else {
            Status = TurbineStatus.Spinning;
        }
        MachineLight Shown = switch (Status) {
            case Spinning -> MachineLight.Working;
            case Throttled, NoLoad -> MachineLight.Waiting;
            case Backed -> MachineLight.Alert;
            default -> MachineLight.Off;
        };
        if (Shown != LastLight || World.getGameTime() % LightCheckTicks == 0) {
            LastLight = Shown;
            if (getBlockState().getBlock() instanceof IndustrialTurbineBlock Turbine) {
                Turbine.showLight(World, worldPosition, getBlockState(), Shown);
            }
        }
        updateSync(World, Mth.clamp(Math.round(Momentum * SpinSteps), 0, SpinSteps));
    }

    public static int fillStep(int Amount, int Capacity) {
        return Mth.clamp(Math.round(Amount * (float) FillSteps / Capacity), 0, FillSteps);
    }

    private void drainInlets(int Flow, float[] Offers, float Wanted) {
        int Left = Flow;
        for (int Index = 0; Index < InletCount && Left > 0; Index++) {
            int Share = Wanted <= 0.0f ? Left : Math.min(Left, (int) Math.floor(Flow * Offers[Index] / Wanted));
            Left -= Inlets[Index].drain(Share, false).getAmount();
        }
        for (int Index = 0; Index < InletCount && Left > 0; Index++) {
            Left -= Inlets[Index].drain(Left, false).getAmount();
        }
    }

    private void updateSync(ServerLevel World, int Wanted) {
        if (Wanted != Rate) {
            UrgentSync |= Wanted == 0 || Rate == 0;
            Rate = Wanted;
            NeedsSync = true;
        }
        for (int Index = 0; Index < InletCount; Index++) {
            int Step = fillStep(Inlets[Index].getAmount(), Inlets[Index].getCapacity());
            if (Step != Fill[Index]) {
                Fill[Index] = Step;
                NeedsSync = true;
            }
        }
        long Now = World.getGameTime();
        if (NeedsSync && (UrgentSync || Now - LastSync >= SyncTicks)) {
            NeedsSync = false;
            UrgentSync = false;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void tickClient() {
        float Target = (float) Rate / SpinSteps;
        if (!Primed) {
            Primed = true;
            Spin = Target;
        }
        PreviousSpin = Spin;
        if (Spin < Target) {
            Spin = Math.min(Target, Spin + (Target - Spin) * SpinEase + SpinFloor);
        } else if (Spin > Target) {
            Spin = Math.max(Target, Spin - (Spin - Target) * SpinEase - SpinFloor);
        }
        for (int Index = 0; Index < InletCount; Index++) {
            float Wanted = getFillTarget(Index);
            PreviousFill[Index] = FillPrimed ? ShownFill[Index] : Wanted;
            float Shown = FillPrimed ? ShownFill[Index] : Wanted;
            if (Shown < Wanted) {
                Shown = Math.min(Wanted, Shown + (Wanted - Shown) * FillEase + FillFloor);
            } else if (Shown > Wanted) {
                Shown = Math.max(Wanted, Shown - (Shown - Wanted) * FillEase - FillFloor);
            }
            ShownFill[Index] = Shown;
        }
        FillPrimed = true;
        PreviousAngle = Angle;
        Angle += Spin * MaxSpinDegrees;
        if (Angle >= 360.0f) {
            Angle -= 360.0f;
            PreviousAngle -= 360.0f;
        }
    }

    public float getFillTarget(int Index) {
        return (float) Fill[Index] / FillSteps;
    }

    public float getRenderFill(int Index, float PartialTick) {
        return Mth.lerp(PartialTick, PreviousFill[Index], ShownFill[Index]);
    }

    public float getSpin() {
        return Spin;
    }

    public float getRenderSpin(float PartialTick) {
        return Mth.lerp(PartialTick, PreviousSpin, Spin);
    }

    public float getRenderAngle(float PartialTick) {
        return Mth.lerp(PartialTick, PreviousAngle, Angle);
    }

    public Vec3 getSoundCentre() {
        BlockState State = getBlockState();
        if (State.getBlock() instanceof IndustrialTurbineBlock Turbine) {
            MachineLayout Layout = Turbine.layout();
            return Layout.point(worldPosition, Turbine.partOf(State), State.getValue(SteamMachineBlock.Facing), 24.0, 16.0, 48.0);
        }
        return Vec3.atCenterOf(worldPosition);
    }

    public AABB getRenderBounds() {
        BlockState State = getBlockState();
        if (State.getBlock() instanceof IndustrialTurbineBlock Turbine) {
            MachineLayout Layout = Turbine.layout();
            BlockPos Origin = Layout.originOf(worldPosition, Turbine.partOf(State), State.getValue(SteamMachineBlock.Facing));
            BlockPos Far = Layout.cellPos(Origin, State.getValue(SteamMachineBlock.Facing), Layout.count() - 1);
            return new AABB(Origin).minmax(new AABB(Far)).inflate(1.0);
        }
        return new AABB(worldPosition).inflate(6.0);
    }

    public Component describe() {
        return Component.translatable("message.scp_unleashed.industrial_turbine.status", Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()),
                LastGenerated, getSpeedPercent(), getPressurePercent(0), getPressurePercent(1), getPressurePercent(2));
    }

    @Override
    public int getPowerOutput() {
        return Math.min(Stored, MaxOutput);
    }

    @Override
    public int getReserveOutput() {
        return WireDevice.bufferedReserve(this);
    }

    @Override
    public boolean isGenerator() {
        return true;
    }

    @Override
    public int getGenerationRate() {
        return LastGenerated;
    }

    @Override
    public void extractPower(int Amount) {
        Stored = Math.max(0, Stored - Amount);
        setChanged();
    }

    @Override
    public int getStoredPower() {
        return Stored;
    }

    @Override
    public int getPowerCapacity() {
        return Capacity;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        for (int Index = 0; Index < InletCount; Index++) {
            Inlets[Index].save(Tag, "Inlet" + Index);
        }
        Exhaust.save(Tag, "Exhaust");
        Tag.putInt("Stored", Stored);
        Tag.putFloat("Momentum", Momentum);
        Tag.putFloat("FlowCarry", FlowCarry);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Rate")) {
            Rate = Mth.clamp(Tag.getByte("Rate"), 0, SpinSteps);
            byte[] Synced = Tag.getByteArray("Fill");
            for (int Index = 0; Index < InletCount && Index < Synced.length; Index++) {
                Fill[Index] = Mth.clamp(Synced[Index], 0, FillSteps);
            }
            return;
        }
        for (int Index = 0; Index < InletCount; Index++) {
            Inlets[Index].load(Tag, "Inlet" + Index);
        }
        Exhaust.load(Tag, "Exhaust");
        Stored = Mth.clamp(Tag.getInt("Stored"), 0, Capacity);
        Momentum = Mth.clamp(Tag.getFloat("Momentum"), 0.0f, 1.0f);
        FlowCarry = Mth.clamp(Tag.getFloat("FlowCarry"), 0.0f, 1.0f);
        Rate = Mth.clamp(Math.round(Momentum * SpinSteps), 0, SpinSteps);
        for (int Index = 0; Index < InletCount; Index++) {
            Fill[Index] = fillStep(Inlets[Index].getAmount(), Inlets[Index].getCapacity());
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Rate", (byte) Rate);
        byte[] Synced = new byte[InletCount];
        for (int Index = 0; Index < InletCount; Index++) {
            Synced[Index] = (byte) Fill[Index];
        }
        Tag.putByteArray("Fill", Synced);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
