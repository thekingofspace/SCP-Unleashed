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
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.gas.GasTank;
import net.rai.scp_unleashed.wire.WireDevice;

import java.util.function.Consumer;

public class SteamTurbineBlockEntity extends BlockEntity implements WireDevice {
    public static final int InletCapacity = 8000;
    public static final int ExhaustCapacity = 8000;
    public static final int MaxFlow = 100;
    public static final int PowerPerSteam = 5;
    public static final int MaxOutput = MaxFlow * PowerPerSteam;
    public static final int Capacity = 50000;
    public static final float FullFlowShare = 0.5f;
    public static final float LoadEase = 0.08f;
    public static final int SpinSteps = 16;
    public static final int SyncTicks = 10;
    public static final int LightCheckTicks = 40;
    public static final float MaxSpinDegrees = 18.0f;
    public static final float SpinUpRate = 0.04f;
    public static final float SpinUpFloor = 0.002f;
    public static final float SpinDownRate = 0.02f;
    public static final float SpinDownFloor = 0.0015f;

    private static Consumer<SteamTurbineBlockEntity> ClientEffects = Machine -> {
    };

    private final GasTank Inlet = new GasTank(InletCapacity, Kind -> Kind == GasRegistry.Steam.get(), this::setChanged);
    private final GasTank Exhaust = new GasTank(ExhaustCapacity, Kind -> Kind == GasRegistry.LowPressureSteam.get(), this::setChanged);

    private int Stored;
    private float FlowCarry;
    private int LastFlow;
    private int LastGenerated;
    private float Load;
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

    public SteamTurbineBlockEntity(BlockPos Pos, BlockState State) {
        super(SteamRegistry.SteamTurbineEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<SteamTurbineBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public GasTank getInletTank() {
        return Inlet;
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

    public float getLoad() {
        return Load;
    }

    public int getPressurePercent() {
        return Math.round(Inlet.getAmount() * 100.0f / Inlet.getCapacity());
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, SteamTurbineBlockEntity Machine) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof SteamTurbineBlock) {
            Machine.tickServer(ServerWorld);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, SteamTurbineBlockEntity Machine) {
        Machine.tickClient();
        ClientEffects.accept(Machine);
    }

    public static float flowFor(int Held, int Capacity) {
        if (Held <= 0) {
            return 0.0f;
        }
        float Share = Math.min(1.0f, Held / (Capacity * FullFlowShare));
        return Math.max(1.0f, MaxFlow * Share);
    }

    private void tickServer(ServerLevel World) {
        float Wanted = flowFor(Inlet.getAmount(), Inlet.getCapacity()) + FlowCarry;
        int Whole = (int) Wanted;
        int RoomForPower = (Capacity - Stored) / PowerPerSteam;
        int Flow = Math.min(Whole, Math.min(Inlet.getAmount(), Math.min(Exhaust.getSpace(), RoomForPower)));
        FlowCarry = Flow == Whole ? Wanted - Whole : 0.0f;
        if (Flow > 0) {
            Inlet.drain(Flow, false);
            Exhaust.fill(new GasStack(GasRegistry.LowPressureSteam.get(), Flow), false);
            Stored += Flow * PowerPerSteam;
            setChanged();
        }
        LastFlow = Flow;
        LastGenerated = Flow * PowerPerSteam;
        if (Inlet.isEmpty() && Flow == 0) {
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
            if (getBlockState().getBlock() instanceof SteamTurbineBlock Turbine) {
                Turbine.showLight(World, worldPosition, getBlockState(), Shown);
            }
        }
        float Target = (float) Flow / MaxFlow;
        Load += (Target - Load) * LoadEase;
        if (Math.abs(Target - Load) < 0.002f) {
            Load = Target;
        }
        updateRate(World, Mth.clamp(Math.round(Load * SpinSteps), 0, SpinSteps));
    }

    private void updateRate(ServerLevel World, int Wanted) {
        if (Wanted != Rate) {
            UrgentSync |= Wanted == 0 || Rate == 0;
            Rate = Wanted;
            NeedsSync = true;
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
            Spin = Math.min(Target, Spin + (Target - Spin) * SpinUpRate + SpinUpFloor);
        } else if (Spin > Target) {
            Spin = Math.max(Target, Spin - (Spin - Target) * SpinDownRate - SpinDownFloor);
        }
        PreviousAngle = Angle;
        Angle += Spin * MaxSpinDegrees;
        if (Angle >= 360.0f) {
            Angle -= 360.0f;
            PreviousAngle -= 360.0f;
        }
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

    public AABB getRenderBounds() {
        return new AABB(worldPosition).inflate(1.5);
    }

    public Component describe() {
        return Component.translatable("message.scp_unleashed.steam_turbine.status", Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()),
                LastGenerated, getPressurePercent());
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
        Inlet.save(Tag, "Inlet");
        Exhaust.save(Tag, "Exhaust");
        Tag.putInt("Stored", Stored);
        Tag.putFloat("Load", Load);
        Tag.putFloat("FlowCarry", FlowCarry);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Rate")) {
            Rate = Mth.clamp(Tag.getByte("Rate"), 0, SpinSteps);
            return;
        }
        Inlet.load(Tag, "Inlet");
        Exhaust.load(Tag, "Exhaust");
        Stored = Mth.clamp(Tag.getInt("Stored"), 0, Capacity);
        Load = Mth.clamp(Tag.getFloat("Load"), 0.0f, 1.0f);
        FlowCarry = Mth.clamp(Tag.getFloat("FlowCarry"), 0.0f, 1.0f);
        Rate = Mth.clamp(Math.round(Load * SpinSteps), 0, SpinSteps);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Rate", (byte) Rate);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
