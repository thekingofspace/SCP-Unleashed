package net.rai.scp_unleashed.fission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.rai.scp_unleashed.upgrade.MachineUpgrades;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import net.rai.scp_unleashed.upgrade.Upgradeable;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;

public class SonicVibratorBlockEntity extends BlockEntity implements WireDevice, MenuProvider, Upgradeable {
    public static final int TankCapacity = 4000;
    public static final int WorkDraw = 40;
    public static final int CleanRate = 10;
    public static final int ActiveHold = 10;
    public static final int SyncTicks = 5;
    public static final int RateSteps = 16;
    public static final float RiseRate = 0.06f;
    public static final float FallRate = 0.035f;
    public static final int DataInput = 0;
    public static final int DataOutput = 1;
    public static final int DataReceived = 2;
    public static final int DataDemand = 3;
    public static final int DataStatus = 4;
    public static final int DataUpgrades = 5;
    public static final int DataCount = 6;

    private static Consumer<SonicVibratorBlockEntity> ClientEffects = Machine -> {
    };

    private final FluidTank Input = new FluidTank(TankCapacity, Stack -> Stack.is(FissionTags.WasteWater)) {
        @Override
        protected void onContentsChanged() {
            contentsChanged();
        }
    };
    private final FluidTank Output = new FluidTank(TankCapacity, Stack -> Stack.is(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            contentsChanged();
        }
    };
    private final IFluidHandler Combined = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int Tank) {
            return Tank == 0 ? Input.getFluid() : Output.getFluid();
        }

        @Override
        public int getTankCapacity(int Tank) {
            return TankCapacity;
        }

        @Override
        public boolean isFluidValid(int Tank, FluidStack Stack) {
            return Tank == 0 && Input.isFluidValid(Stack);
        }

        @Override
        public int fill(FluidStack Resource, FluidAction Action) {
            return Input.fill(Resource, Action);
        }

        @Override
        public FluidStack drain(FluidStack Resource, FluidAction Action) {
            return Output.drain(Resource, Action);
        }

        @Override
        public FluidStack drain(int MaxDrain, FluidAction Action) {
            return Output.drain(MaxDrain, Action);
        }
    };

    private final MachineUpgrades Upgrades = new MachineUpgrades(this::setChanged, UpgradeType.Speed);
    private int Received;
    private int LastReceived;
    private int Demand;
    private float Carry;
    private VibratorStatus Status = VibratorStatus.Idle;
    private long LastWorked = Long.MIN_VALUE / 2;
    private int Rate;
    private int InputShown;
    private int OutputShown;
    private boolean NeedsSync;
    private boolean UrgentSync;
    private long LastSync = Long.MIN_VALUE / 2;
    private boolean Primed;
    private float Intensity;
    private float PreviousIntensity;
    private float Phase;
    private float PreviousPhase;
    private float ShownFill;
    private float PreviousShownFill;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case DataInput -> Input.getFluidAmount();
                case DataOutput -> Output.getFluidAmount();
                case DataReceived -> LastReceived;
                case DataDemand -> Demand;
                case DataStatus -> Status.ordinal();
                case DataUpgrades -> Upgrades.count(UpgradeType.Speed);
                default -> 0;
            };
        }

        @Override
        public void set(int Index, int Value) {
        }

        @Override
        public int getCount() {
            return DataCount;
        }
    };

    public SonicVibratorBlockEntity(BlockPos Pos, BlockState State) {
        super(FissionRegistry.SonicVibratorEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<SonicVibratorBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public FluidTank getInputTank() {
        return Input;
    }

    public FluidTank getOutputTank() {
        return Output;
    }

    public IFluidHandler getCombined() {
        return Combined;
    }

    public VibratorStatus getStatus() {
        return Status;
    }

    public int getDemand() {
        return Demand;
    }

    public int getLastReceived() {
        return LastReceived;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, SonicVibratorBlockEntity Machine) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof SonicVibratorBlock) {
            Machine.tickServer(ServerWorld, Pos, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, SonicVibratorBlockEntity Machine) {
        Machine.tickClient();
        ClientEffects.accept(Machine);
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        int Delivered = Received;
        Received = 0;
        LastReceived = Delivered;
        boolean WasDemanding = Demand > 0;
        boolean HasWaste = Input.getFluidAmount() > 0;
        boolean HasRoom = Output.getSpace() > 0;
        int Draw = Upgrades.draw(WorkDraw);
        int Used = 0;
        if (!HasWaste) {
            Status = VibratorStatus.Idle;
        } else if (!HasRoom) {
            Status = VibratorStatus.Blocked;
        } else if (WasDemanding) {
            Used = Math.min(Delivered, Draw);
            Status = Used >= Draw ? VibratorStatus.Working : Used > 0 ? VibratorStatus.LowPower : VibratorStatus.Unpowered;
            if (Used > 0) {
                float Amount = CleanRate * Upgrades.speedFactor() * Used / (float) Draw + Carry;
                int Whole = (int) Amount;
                Carry = Amount - Whole;
                Whole = Math.min(Whole, Math.min(Input.getFluidAmount(), Output.getSpace()));
                if (Whole > 0) {
                    Input.drain(Whole, IFluidHandler.FluidAction.EXECUTE);
                    Output.fill(new FluidStack(Fluids.WATER, Whole), IFluidHandler.FluidAction.EXECUTE);
                }
                LastWorked = Now;
            }
        }
        Demand = HasWaste && HasRoom ? Draw : 0;
        boolean Active = Now - LastWorked <= ActiveHold;
        if (State.getValue(SonicVibratorBlock.Active) != Active) {
            World.setBlock(Pos, State.setValue(SonicVibratorBlock.Active, Active), Block.UPDATE_CLIENTS);
        }
        updateSync(World, Now, Used, Draw);
    }

    private void contentsChanged() {
        setChanged();
        NeedsSync = true;
    }

    private void updateSync(ServerLevel World, long Now, int Used, int Draw) {
        int Wanted = Mth.clamp(Math.round((float) Used * RateSteps / Draw), 0, RateSteps);
        int InputNow = Math.round(Input.getFluidAmount() * 100.0f / TankCapacity);
        int OutputNow = Math.round(Output.getFluidAmount() * 100.0f / TankCapacity);
        if (Wanted != Rate || InputNow != InputShown || OutputNow != OutputShown) {
            UrgentSync |= (Wanted == 0) != (Rate == 0);
            Rate = Wanted;
            InputShown = InputNow;
            OutputShown = OutputNow;
            NeedsSync = true;
        }
        if (NeedsSync && (UrgentSync || Now - LastSync >= SyncTicks)) {
            NeedsSync = false;
            UrgentSync = false;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void tickClient() {
        float Target = (float) Rate / RateSteps;
        float Fill = InputShown / 100.0f;
        if (!Primed) {
            Primed = true;
            Intensity = Target;
            ShownFill = Fill;
        }
        PreviousIntensity = Intensity;
        PreviousPhase = Phase;
        PreviousShownFill = ShownFill;
        Intensity += (Target - Intensity) * (Target > Intensity ? RiseRate : FallRate);
        if (Math.abs(Target - Intensity) < 0.002f) {
            Intensity = Target;
        }
        Phase += 0.15f + Intensity * 1.35f;
        ShownFill += (Fill - ShownFill) * 0.15f;
    }

    public float getIntensity() {
        return Intensity;
    }

    public float getRenderIntensity(float PartialTick) {
        return Mth.lerp(PartialTick, PreviousIntensity, Intensity);
    }

    public float getRenderPhase(float PartialTick) {
        return Mth.lerp(PartialTick, PreviousPhase, Phase);
    }

    public float getRenderLevel(float PartialTick) {
        return Mth.lerp(PartialTick, PreviousShownFill, ShownFill);
    }

    public float getOutputLevel() {
        return OutputShown / 100.0f;
    }

    public int getSpeedUpgrades() {
        return Upgrades.count(UpgradeType.Speed);
    }

    public static int drawFor(int Upgrades) {
        return Math.max(1, Math.round(WorkDraw * (1.0f + MachineUpgrades.PowerPerUpgrade * Upgrades)));
    }

    public static int rateFor(int Upgrades) {
        return Math.round(CleanRate * 20 * (1.0f + MachineUpgrades.SpeedPerUpgrade * Upgrades));
    }

    public void dropUpgrades(Level World, BlockPos Pos) {
        Upgrades.drop(World, Pos);
    }

    @Override
    public boolean acceptsUpgrade(UpgradeType Type) {
        return Upgrades.accepts(Type);
    }

    @Override
    public boolean hasUpgrade(UpgradeType Type) {
        return Upgrades.count(Type) > 0;
    }

    @Override
    public int countUpgrade(UpgradeType Type) {
        return Upgrades.count(Type);
    }

    @Override
    public void installUpgrade(UpgradeType Type) {
        Upgrades.install(Type);
    }

    @Override
    public Collection<UpgradeType> getUpgrades() {
        return Upgrades.list();
    }

    @Override
    public int getPowerDemand() {
        return Demand;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Amount;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Input", Input.writeToNBT(Registries, new CompoundTag()));
        Tag.put("Output", Output.writeToNBT(Registries, new CompoundTag()));
        Upgrades.save(Tag);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Sync")) {
            Rate = Mth.clamp(Tag.getByte("Rate"), 0, RateSteps);
            InputShown = Mth.clamp(Tag.getByte("InputFill"), 0, 100);
            OutputShown = Mth.clamp(Tag.getByte("OutputFill"), 0, 100);
            return;
        }
        if (Tag.contains("Input")) {
            Input.readFromNBT(Registries, Tag.getCompound("Input"));
        }
        if (Tag.contains("Output")) {
            Output.readFromNBT(Registries, Tag.getCompound("Output"));
        }
        Upgrades.load(Tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Sync", true);
        Tag.putByte("Rate", (byte) Rate);
        Tag.putByte("InputFill", (byte) Math.round(Input.getFluidAmount() * 100.0f / TankCapacity));
        Tag.putByte("OutputFill", (byte) Math.round(Output.getFluidAmount() * 100.0f / TankCapacity));
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.sonic_vibrator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new SonicVibratorMenu(ContainerId, PlayerInventory, this, Data);
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }
}
