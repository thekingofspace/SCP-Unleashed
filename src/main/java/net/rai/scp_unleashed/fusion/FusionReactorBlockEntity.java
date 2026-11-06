package net.rai.scp_unleashed.fusion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.RestrictedFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class FusionReactorBlockEntity extends BlockEntity implements MenuProvider {
    public static final TagKey<Item> FuelTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "fusion_fuel"));
    public static final int MaxOutput = 24000;
    public static final int StartupEnergy = 5000000;
    public static final int MaxChargeRate = 16000;
    public static final int FuelTicks = 96000;
    public static final long FuelEnergy = (long) MaxOutput * FuelTicks;
    public static final int SustainOutput = MaxOutput / 20;
    public static final int WaterCapacity = 16 * FluidType.BUCKET_VOLUME;
    public static final int WaterPerTick = 2;
    public static final int IgnitionWater = FluidType.BUCKET_VOLUME;
    public static final int LowWater = 2 * FluidType.BUCKET_VOLUME;
    public static final int IgnitionTicks = 120;
    public static final float IgnitionStrike = 2.0f / 3.0f;
    public static final int StrikeTick = Math.round(IgnitionTicks * IgnitionStrike);
    public static final float IgniteVolume = 2.0f;
    public static final float StrikeVolume = 3.0f;
    public static final float ShutdownVolume = 1.5f;
    public static final float PreIonised = 0.25f;
    public static final int QuenchTicks = 60;
    public static final int BufferCapacity = MaxOutput;
    public static final int BusRefreshTicks = 10;
    public static final int SyncTicks = 10;
    public static final int StabilityScale = 1000;
    public static final int FuelScale = 1000;
    public static final int LoadSteps = 20;
    public static final int ChargeSteps = 50;
    public static final int StabilitySteps = 50;
    public static final int ValuePhase = 0;
    public static final int ValueNotice = 1;
    public static final int ValueStability = 2;
    public static final int ValueFuel = 3;
    public static final int ValueWater = 4;
    public static final int ValueCharge = 5;
    public static final int ValueDelivered = 6;
    public static final int ValueIntake = 7;
    public static final int ValueInputBus = 8;
    public static final int ValueOutputBus = 9;
    public static final int ValueEnabled = 10;
    public static final int ValuePhaseTicks = 11;
    public static final int ValueCount = 12;
    public static final int DataCount = ValueCount * 2;

    private static Consumer<FusionReactorBlockEntity> ClientEffects = Reactor -> {
    };

    private final ItemStackHandler Fuel = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return isFuel(Stack);
        }

        @Override
        protected void onContentsChanged(int Slot) {
            setChanged();
        }
    };
    private final FluidTank Water = new FluidTank(WaterCapacity, Stack -> Stack.is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler Inlet = new RestrictedFluidHandler(Water, PortRole.Input);
    private FusionPhase Phase = FusionPhase.Offline;
    private FusionNotice Notice = FusionNotice.None;
    private boolean Enabled;
    private int PhaseTicks;
    private int Charge;
    private long FuelLeft;
    private int Buffer;
    private double WaterDebt;
    private int Stability;
    private int QuenchFrom;
    private int Received;
    private int LastIntake;
    private int Delivered;
    private int LastDelivered;
    private int LastProduced;
    private FusionBus.Rating InputBus = FusionBus.Rating.Empty;
    private FusionBus.Rating OutputBus = FusionBus.Rating.Empty;
    private boolean BusDirty = true;
    private boolean GlowDirty = true;
    private FusionGlow ShownGlow = FusionGlow.Off;
    private boolean NeedsSync;
    private boolean UrgentSync;
    private long LastSync = Long.MIN_VALUE / 2;
    private int SentStability = -1;
    private int SentLoad = -1;
    private int SentCharge = -1;
    private float ClientLoad;
    private float ClientCharge;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            int Value = value(Index / 2);
            return Index % 2 == 0 ? Value & 0xFFFF : Value >>> 16;
        }

        @Override
        public void set(int Index, int Value) {
        }

        @Override
        public int getCount() {
            return DataCount;
        }
    };

    public FusionReactorBlockEntity(BlockPos Pos, BlockState State) {
        super(FusionRegistry.ReactorEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<FusionReactorBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public static boolean isFuel(ItemStack Stack) {
        return !Stack.isEmpty() && Stack.is(FuelTag);
    }

    public static int joinValue(ContainerData Source, int Value) {
        return (Source.get(Value * 2 + 1) << 16) | (Source.get(Value * 2) & 0xFFFF);
    }

    private int value(int Index) {
        return switch (Index) {
            case ValuePhase -> Phase.ordinal();
            case ValueNotice -> Notice.ordinal();
            case ValueStability -> Stability;
            case ValueFuel -> getFuelPermille();
            case ValueWater -> Water.getFluidAmount();
            case ValueCharge -> Charge;
            case ValueDelivered -> LastDelivered;
            case ValueIntake -> LastIntake;
            case ValueInputBus -> InputBus.Capacity();
            case ValueOutputBus -> OutputBus.Capacity();
            case ValueEnabled -> Enabled ? 1 : 0;
            case ValuePhaseTicks -> PhaseTicks;
            default -> 0;
        };
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, FusionReactorBlockEntity Reactor) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof FusionReactorBlock) {
            Reactor.tickServer(ServerWorld, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, FusionReactorBlockEntity Reactor) {
        Reactor.PhaseTicks++;
        ClientEffects.accept(Reactor);
    }

    private void tickServer(ServerLevel World, BlockState State) {
        long Now = World.getGameTime();
        LastDelivered = Delivered;
        Delivered = 0;
        LastIntake = Received;
        Received = 0;
        LastProduced = 0;
        if (Phase == FusionPhase.Charging && LastIntake > 0) {
            Charge = Math.min(StartupEnergy, Charge + LastIntake);
        }
        if (BusDirty || Math.floorMod(Now + worldPosition.asLong(), BusRefreshTicks) == 0) {
            BusDirty = false;
            refreshBuses(World, State);
        }
        FusionPhase Before = Phase;
        switch (Phase) {
            case Offline -> tickOffline();
            case Charging -> tickCharging(World, State);
            case Igniting -> tickIgniting(World, State);
            case Running -> tickRunning(World, State, Now);
            case Quenching -> tickQuenching();
        }
        if (Phase == Before) {
            PhaseTicks++;
        }
        if (Phase.isActive() || LastDelivered > 0 || LastIntake > 0) {
            setChanged();
        }
        updateGlow(World, State);
        updateSync(World, Now);
    }

    private void tickOffline() {
        Stability = 0;
        if (Enabled) {
            Notice = FusionNotice.None;
            enter(FusionPhase.Charging);
        }
    }

    private void tickCharging(ServerLevel World, BlockState State) {
        Stability = 0;
        if (!Enabled) {
            enter(FusionPhase.Offline);
            return;
        }
        if (Charge < StartupEnergy || !hasFuel() || Water.getFluidAmount() < IgnitionWater) {
            return;
        }
        if (FuelLeft <= 0 && !loadFuel()) {
            return;
        }
        enter(FusionPhase.Igniting);
        play(World, State, FusionRegistry.Ignite.get(), IgniteVolume);
    }

    private void tickIgniting(ServerLevel World, BlockState State) {
        if (!Enabled) {
            quench(World, State, FusionNotice.Stopped);
            return;
        }
        if (Water.getFluidAmount() <= 0) {
            quench(World, State, FusionNotice.NoWater);
            return;
        }
        if (PhaseTicks + 1 == StrikeTick) {
            play(World, State, FusionRegistry.IgniteStrike.get(), StrikeVolume);
        }
        float Progress = Math.min(1.0f, (PhaseTicks + 1) / (float) IgnitionTicks);
        if (Progress < IgnitionStrike) {
            float Build = Progress / IgnitionStrike;
            Stability = Math.round(StabilityScale * PreIonised * Build * Build);
        } else {
            float Surge = (Progress - IgnitionStrike) / (1.0f - IgnitionStrike);
            Stability = Math.round(StabilityScale * (PreIonised + (1.0f - PreIonised) * Surge * Surge * (3.0f - 2.0f * Surge)));
        }
        Charge = Math.max(0, Math.round(StartupEnergy * (1.0f - Progress)));
        if (PhaseTicks + 1 >= IgnitionTicks) {
            Charge = 0;
            Stability = StabilityScale;
            Notice = FusionNotice.None;
            enter(FusionPhase.Running);
        }
    }

    private void tickRunning(ServerLevel World, BlockState State, long Now) {
        if (!Enabled) {
            quench(World, State, FusionNotice.Stopped);
            return;
        }
        int Produce = Math.min(MaxOutput, BufferCapacity - Buffer);
        long Burn = Math.max(Produce, SustainOutput);
        if (FuelLeft < Burn && !loadFuel()) {
            int Last = (int) Math.min(Produce, FuelLeft);
            FuelLeft = 0;
            Buffer += Last;
            LastProduced = Last;
            quench(World, State, FusionNotice.NoFuel);
            return;
        }
        FuelLeft -= Burn;
        WaterDebt += (double) Burn * WaterPerTick / MaxOutput;
        int Need = (int) WaterDebt;
        if (Need > 0) {
            FluidStack Drained = Water.drain(Need, IFluidHandler.FluidAction.EXECUTE);
            WaterDebt -= Drained.getAmount();
            if (Drained.getAmount() < Need) {
                WaterDebt = 0.0;
                Buffer += Produce;
                LastProduced = Produce;
                quench(World, State, FusionNotice.NoWater);
                return;
            }
        }
        Buffer += Produce;
        LastProduced = Produce;
        Stability = runningStability(Now);
    }

    private void tickQuenching() {
        float Progress = Math.min(1.0f, (PhaseTicks + 1) / (float) QuenchTicks);
        Stability = Math.max(0, Math.round(QuenchFrom * (1.0f - Progress) * (1.0f - Progress)));
        if (PhaseTicks + 1 >= QuenchTicks) {
            Stability = 0;
            enter(FusionPhase.Offline);
        }
    }

    private int runningStability(long Now) {
        double Wobble = 14.0 + 8.0 * Math.sin(Now * 0.047) + 5.0 * Math.sin(Now * 0.131 + 1.7);
        double Penalty = 0.0;
        int Stored = Water.getFluidAmount();
        if (Stored < LowWater) {
            Penalty += 320.0 * (LowWater - Stored) / LowWater;
        }
        if (!isFuel(Fuel.getStackInSlot(0)) && FuelLeft < FuelEnergy / 50) {
            Penalty += 320.0 * (1.0 - FuelLeft / (double) (FuelEnergy / 50));
        }
        return Mth.clamp((int) Math.round(StabilityScale - Wobble - Penalty), 0, StabilityScale);
    }

    private void quench(ServerLevel World, BlockState State, FusionNotice Reason) {
        Notice = Reason;
        Enabled = false;
        Charge = 0;
        QuenchFrom = Stability;
        enter(FusionPhase.Quenching);
        play(World, State, FusionRegistry.Shutdown.get(), ShutdownVolume);
        play(World, State, FusionRegistry.ShutdownLow.get(), ShutdownVolume);
    }

    private void enter(FusionPhase Next) {
        Phase = Next;
        PhaseTicks = 0;
        NeedsSync = true;
        UrgentSync = true;
        setChanged();
    }

    private boolean hasFuel() {
        return FuelLeft > 0 || isFuel(Fuel.getStackInSlot(0));
    }

    private boolean loadFuel() {
        if (!isFuel(Fuel.getStackInSlot(0))) {
            return false;
        }
        Fuel.extractItem(0, 1, false);
        FuelLeft += FuelEnergy;
        return true;
    }

    private void refreshBuses(ServerLevel World, BlockState State) {
        BlockPos Origin = FusionLayout.originOf(worldPosition, State);
        Direction Front = State.getValue(FusionReactorBlock.Facing);
        InputBus = FusionBus.rate(World, FusionLayout.cellPos(Origin, Front, FusionLayout.InputPart));
        OutputBus = FusionBus.rate(World, FusionLayout.cellPos(Origin, Front, FusionLayout.OutputPart));
    }

    private void updateGlow(ServerLevel World, BlockState State) {
        FusionGlow Wanted = Phase.getGlow();
        if (!GlowDirty && Wanted == ShownGlow) {
            return;
        }
        GlowDirty = false;
        ShownGlow = Wanted;
        BlockPos Origin = FusionLayout.originOf(worldPosition, State);
        Direction Front = State.getValue(FusionReactorBlock.Facing);
        for (int Part = 0; Part < FusionLayout.CellCount; Part++) {
            if (!FusionLayout.isGlowCell(Part)) {
                continue;
            }
            BlockPos Cell = FusionLayout.cellPos(Origin, Front, Part);
            if (!World.isLoaded(Cell)) {
                GlowDirty = true;
                continue;
            }
            BlockState CellState = World.getBlockState(Cell);
            if (CellState.getBlock() instanceof FusionReactorBlock && CellState.getValue(FusionReactorBlock.Part) == Part
                    && CellState.getValue(FusionReactorBlock.Facing) == Front && CellState.getValue(FusionReactorBlock.Glow) != Wanted) {
                World.setBlock(Cell, CellState.setValue(FusionReactorBlock.Glow, Wanted), Block.UPDATE_CLIENTS);
            }
        }
    }

    private void updateSync(ServerLevel World, long Now) {
        int StabilityLevel = Stability * StabilitySteps / StabilityScale;
        int LoadLevel = Math.min(LoadSteps, LastDelivered * LoadSteps / MaxOutput);
        int ChargeLevel = (int) ((long) Charge * ChargeSteps / StartupEnergy);
        if (StabilityLevel != SentStability || LoadLevel != SentLoad || ChargeLevel != SentCharge) {
            NeedsSync = true;
        }
        if (NeedsSync && (UrgentSync || Now - LastSync >= SyncTicks)) {
            NeedsSync = false;
            UrgentSync = false;
            LastSync = Now;
            SentStability = StabilityLevel;
            SentLoad = LoadLevel;
            SentCharge = ChargeLevel;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void play(ServerLevel World, BlockState State, SoundEvent Sound, float Volume) {
        Vec3 Middle = centre(State);
        World.playSound(null, Middle.x, Middle.y, Middle.z, Sound, SoundSource.BLOCKS, Volume, 1.0f);
    }

    public Vec3 centre(BlockState State) {
        return FusionLayout.toWorld(FusionLayout.originOf(worldPosition, State), State.getValue(FusionReactorBlock.Facing), 40.0, 24.0, 40.0);
    }

    public void requestStart() {
        if (!Enabled) {
            Enabled = true;
            Notice = FusionNotice.None;
            NeedsSync = true;
            setChanged();
        }
    }

    public void requestStop() {
        if (Enabled) {
            Enabled = false;
            NeedsSync = true;
            setChanged();
        }
    }

    public int offerOutput() {
        return Math.max(0, Math.min(Buffer, Math.min(MaxOutput, OutputBus.Capacity())));
    }

    public void drawOutput(int Amount) {
        int Taken = Math.min(Math.max(0, Amount), Buffer);
        Buffer -= Taken;
        Delivered += Taken;
    }

    public int chargeDemand() {
        if (Phase != FusionPhase.Charging || !Enabled) {
            return 0;
        }
        return Math.max(0, Math.min(Math.min(MaxChargeRate, StartupEnergy - Charge - Received), InputBus.Capacity()));
    }

    public void receiveCharge(int Amount) {
        if (Amount > 0) {
            Received += Amount;
        }
    }

    public void markBusesDirty() {
        BusDirty = true;
    }

    public int getGenerationRate() {
        return Phase == FusionPhase.Running ? Math.max(LastProduced, LastDelivered) : 0;
    }

    public FusionPhase getPhase() {
        return Phase;
    }

    public FusionNotice getNotice() {
        return Notice;
    }

    public boolean isEnabled() {
        return Enabled;
    }

    public int getPhaseTicks() {
        return PhaseTicks;
    }

    public int getStability() {
        return Stability;
    }

    public int getCharge() {
        return Charge;
    }

    public int getWaterAmount() {
        return Water.getFluidAmount();
    }

    public int getFuelUnits() {
        ItemStack Stack = Fuel.getStackInSlot(0);
        return isFuel(Stack) ? Stack.getCount() : 0;
    }

    public int getFuelPermille() {
        return (int) Math.min(FuelScale, Math.max(0L, FuelLeft * FuelScale / FuelEnergy));
    }

    public long getFuelTicksLeft() {
        return (FuelLeft + (long) getFuelUnits() * FuelEnergy) / MaxOutput;
    }

    public int getLastDelivered() {
        return LastDelivered;
    }

    public int getLastIntake() {
        return LastIntake;
    }

    public FusionBus.Rating getInputBus() {
        return InputBus;
    }

    public FusionBus.Rating getOutputBus() {
        return OutputBus;
    }

    public float getClientLoad() {
        return ClientLoad;
    }

    public float getClientCharge() {
        return ClientCharge;
    }

    public Direction getFront() {
        BlockState State = getBlockState();
        return State.hasProperty(FusionReactorBlock.Facing) ? State.getValue(FusionReactorBlock.Facing) : Direction.NORTH;
    }

    public BlockPos getOrigin() {
        BlockState State = getBlockState();
        return State.getBlock() instanceof FusionReactorBlock ? FusionLayout.originOf(worldPosition, State) : worldPosition;
    }

    public ItemStackHandler getFuel() {
        return Fuel;
    }

    public FluidTank getWater() {
        return Water;
    }

    public IFluidHandler getInlet() {
        return Inlet;
    }

    public void dropContents(Level World, BlockPos Pos) {
        Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Fuel.getStackInSlot(0).copy());
        Fuel.setStackInSlot(0, ItemStack.EMPTY);
    }

    public boolean isUsableBy(Player User) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        Vec3 Middle = centre(getBlockState());
        return User.distanceToSqr(Middle) <= 144.0;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        GlowDirty = true;
        BusDirty = true;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Fuel", Fuel.serializeNBT(Registries));
        Tag.put("Water", Water.writeToNBT(Registries, new CompoundTag()));
        Tag.putLong("FuelLeft", FuelLeft);
        Tag.putInt("Charge", Charge);
        Tag.putInt("Buffer", Buffer);
        Tag.putByte("Phase", (byte) Phase.ordinal());
        Tag.putInt("PhaseTicks", PhaseTicks);
        Tag.putBoolean("Enabled", Enabled);
        Tag.putByte("Notice", (byte) Notice.ordinal());
        Tag.putDouble("WaterDebt", WaterDebt);
        Tag.putShort("Stability", (short) Stability);
        Tag.putShort("QuenchFrom", (short) QuenchFrom);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Fuel")) {
            Fuel.deserializeNBT(Registries, Tag.getCompound("Fuel"));
        }
        if (Tag.contains("Water")) {
            Water.readFromNBT(Registries, Tag.getCompound("Water"));
        }
        if (Tag.contains("FuelLeft")) {
            FuelLeft = Math.max(0L, Tag.getLong("FuelLeft"));
        }
        if (Tag.contains("Charge")) {
            Charge = Mth.clamp(Tag.getInt("Charge"), 0, StartupEnergy);
        }
        if (Tag.contains("Buffer")) {
            Buffer = Mth.clamp(Tag.getInt("Buffer"), 0, BufferCapacity);
        }
        if (Tag.contains("Phase")) {
            Phase = FusionPhase.byIndex(Tag.getByte("Phase"));
        }
        if (Tag.contains("PhaseTicks")) {
            PhaseTicks = Math.max(0, Tag.getInt("PhaseTicks"));
        }
        if (Tag.contains("Enabled")) {
            Enabled = Tag.getBoolean("Enabled");
        }
        if (Tag.contains("Notice")) {
            Notice = FusionNotice.byIndex(Tag.getByte("Notice"));
        }
        if (Tag.contains("WaterDebt")) {
            WaterDebt = Math.max(0.0, Tag.getDouble("WaterDebt"));
        }
        if (Tag.contains("Stability")) {
            Stability = Mth.clamp(Tag.getShort("Stability"), 0, StabilityScale);
        }
        if (Tag.contains("QuenchFrom")) {
            QuenchFrom = Mth.clamp(Tag.getShort("QuenchFrom"), 0, StabilityScale);
        }
        if (Tag.contains("Load")) {
            ClientLoad = Mth.clamp(Tag.getByte("Load") / (float) LoadSteps, 0.0f, 1.0f);
        }
        if (Tag.contains("Charged")) {
            ClientCharge = Mth.clamp(Tag.getByte("Charged") / (float) ChargeSteps, 0.0f, 1.0f);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Phase", (byte) Phase.ordinal());
        Tag.putInt("PhaseTicks", PhaseTicks);
        Tag.putShort("Stability", (short) Stability);
        Tag.putByte("Load", (byte) Math.min(LoadSteps, LastDelivered * LoadSteps / MaxOutput));
        Tag.putByte("Charged", (byte) (int) ((long) Charge * ChargeSteps / StartupEnergy));
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.fusion_reactor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new FusionMenu(ContainerId, PlayerInventory, this, Data);
    }
}
