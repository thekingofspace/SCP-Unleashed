package net.rai.scp_unleashed.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.rai.scp_unleashed.upgrade.MachineUpgrades;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import net.rai.scp_unleashed.upgrade.Upgradeable;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class ProcessingMachineBlockEntity extends BlockEntity implements WireDevice, MenuProvider, Upgradeable {
    public static final int InputSlot = 0;
    public static final int OutputSlot = 1;
    public static final int SlotCount = 2;
    public static final int ActiveHold = 10;
    public static final int SyncTicks = 10;
    public static final int DriftTicks = 40;
    public static final int DataCount = 6;
    public static final int ProgressScale = 1000;
    public static final int EventCompleted = 1;
    public static final float CatchUp = 0.25f;
    public static final float SnapDistance = 0.2f;

    private static Consumer<ProcessingMachineBlockEntity> ClientTicker = Machine -> {
    };
    private static BiConsumer<ProcessingMachineBlockEntity, ItemStack> ClientCompletion = (Machine, Result) -> {
    };

    private final ItemStackHandler Storage = new ItemStackHandler(SlotCount) {
        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return Slot == InputSlot && (level == null || accepts(level, Stack));
        }

        @Override
        protected void onContentsChanged(int Slot) {
            contentsChanged(Slot);
        }
    };
    private final IItemHandler InputSide = new MachineItemHandler(Storage, true, false, InputSlot);
    private final IItemHandler OutputSide = new MachineItemHandler(Storage, false, true, OutputSlot);
    private final IItemHandler Automation = new MachineItemHandler(Storage, true, true, InputSlot, OutputSlot);
    private final MachineUpgrades Upgrades = new MachineUpgrades(this::setChanged, UpgradeType.Speed);

    private int Received;
    private int LastReceived;
    private int LastDemand;
    private int Demand;
    private float Progress;
    private int Total;
    @Nullable
    private ResourceLocation JobId;
    private MachineStatus Status = MachineStatus.Idle;
    private long LastWorked = Long.MIN_VALUE / 2;
    private float Rate;
    private float SyncedRate;
    private boolean NeedsSync = true;
    private long LastSync = Long.MIN_VALUE / 2;
    private ItemStack Workpiece = ItemStack.EMPTY;

    private float Shown;
    private float PreviousShown;
    private float ShownRate;
    private float Lag;
    private ItemStack ShownWorkpiece = ItemStack.EMPTY;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case 0 -> Total > 0 ? Math.min(ProgressScale, Math.round(Progress * ProgressScale / Total)) : 0;
                case 1 -> Math.min(Short.MAX_VALUE, Total);
                case 2 -> Math.min(Short.MAX_VALUE, LastReceived);
                case 3 -> Math.min(Short.MAX_VALUE, LastDemand);
                case 4 -> Status.ordinal();
                default -> getMode();
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

    protected ProcessingMachineBlockEntity(BlockEntityType<?> Type, BlockPos Pos, BlockState State) {
        super(Type, Pos, State);
    }

    public static void setClientHooks(Consumer<ProcessingMachineBlockEntity> Ticker, BiConsumer<ProcessingMachineBlockEntity, ItemStack> Completion) {
        ClientTicker = Ticker;
        ClientCompletion = Completion;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, ProcessingMachineBlockEntity Machine) {
        if (World instanceof ServerLevel ServerWorld) {
            Machine.tickServer(ServerWorld, Pos, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, ProcessingMachineBlockEntity Machine) {
        Machine.tickClient();
        ClientTicker.accept(Machine);
    }

    public abstract int getWorkDraw();

    @Nullable
    protected abstract MachineJob findJob(Level World, ItemStack Input);

    protected abstract boolean accepts(Level World, ItemStack Stack);

    protected void onCompleted(ServerLevel World, BlockPos Pos, MachineJob Job) {
    }

    protected void onClientCompleted(ItemStack Result) {
    }

    protected void tickAnimation() {
    }

    protected int getMode() {
        return 0;
    }

    protected void requestSync() {
        NeedsSync = true;
    }

    protected void writeSync(CompoundTag Tag, HolderLookup.Provider Registries) {
    }


    public void awardExperience(ServerPlayer User) {
    }

    public void onBroken(ServerLevel World, BlockPos Pos) {
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        LastReceived = Received;
        LastDemand = Demand;
        Received = 0;
        ItemStack Input = Storage.getStackInSlot(InputSlot);
        MachineJob Job = Input.isEmpty() ? null : findJob(World, Input);
        ResourceLocation Id = Job == null ? null : Job.id();
        if (!Objects.equals(Id, JobId)) {
            JobId = Id;
            Progress = 0.0f;
            NeedsSync = true;
        }
        Total = Job == null ? 0 : Math.max(1, Job.Ticks());
        float Speed = 0.0f;
        if (Job == null) {
            Status = MachineStatus.Idle;
        } else if (!fits(Job.Result())) {
            Status = MachineStatus.Blocked;
        } else {
            float Fraction = Math.min(1.0f, (float) LastReceived / Upgrades.draw(Job.Draw()));
            if (LastDemand <= 0) {
                Status = MachineStatus.Idle;
            } else {
                Status = LastReceived <= 0 ? MachineStatus.Unpowered : LastReceived >= LastDemand ? MachineStatus.Running : MachineStatus.Underpowered;
            }
            if (Fraction > 0.0f) {
                LastWorked = Now;
                float Step = Fraction * Upgrades.speedFactor();
                Speed = Step / Total;
                Progress += Step;
                if (Progress >= Total) {
                    Progress = 0.0f;
                    complete(World, Pos, State, Job);
                }
                setChanged();
            }
        }
        Demand = Job != null && Status != MachineStatus.Blocked ? Upgrades.draw(Job.Draw()) : 0;
        Rate = Speed;
        if (Math.abs(Rate - SyncedRate) > 1.0e-4f) {
            NeedsSync = true;
        }
        updateWorking(World, Pos, Now);
        if ((NeedsSync && Now - LastSync >= SyncTicks) || (Rate > 0.0f && Now - LastSync >= DriftTicks)) {
            NeedsSync = false;
            LastSync = Now;
            SyncedRate = Rate;
            World.sendBlockUpdated(Pos, State, State, Block.UPDATE_CLIENTS);
        }
    }

    private void complete(ServerLevel World, BlockPos Pos, BlockState State, MachineJob Job) {
        ItemStack Result = Job.Result().copy();
        ItemStack Output = Storage.getStackInSlot(OutputSlot);
        Storage.setStackInSlot(OutputSlot, Output.isEmpty() ? Result.copy() : Output.copyWithCount(Output.getCount() + Result.getCount()));
        ItemStack Input = Storage.getStackInSlot(InputSlot);
        Storage.setStackInSlot(InputSlot, Input.getCount() > Job.Consumes() ? Input.copyWithCount(Input.getCount() - Job.Consumes()) : ItemStack.EMPTY);
        onCompleted(World, Pos, Job);
        World.blockEvent(Pos, State.getBlock(), EventCompleted, BuiltInRegistries.ITEM.getId(Result.getItem()));
    }

    private boolean fits(ItemStack Result) {
        ItemStack Output = Storage.getStackInSlot(OutputSlot);
        if (Output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(Output, Result)
                && Output.getCount() + Result.getCount() <= Math.min(Output.getMaxStackSize(), Storage.getSlotLimit(OutputSlot));
    }

    private void updateWorking(ServerLevel World, BlockPos Pos, long Now) {
        BlockState Current = getBlockState();
        boolean Working = Now - LastWorked <= ActiveHold;
        if (Current.hasProperty(ProcessingMachineBlock.Active) && Current.getValue(ProcessingMachineBlock.Active) != Working) {
            World.setBlock(Pos, Current.setValue(ProcessingMachineBlock.Active, Working), Block.UPDATE_CLIENTS);
        }
    }

    private void contentsChanged(int Slot) {
        setChanged();
        if (Slot == InputSlot) {
            ItemStack Input = Storage.getStackInSlot(InputSlot);
            if (!ItemStack.isSameItemSameComponents(Input, Workpiece)) {
                Workpiece = Input.copyWithCount(1);
                NeedsSync = true;
            }
        }
    }

    private void tickClient() {
        PreviousShown = Shown;
        float Correction = Lag * CatchUp;
        Lag -= Correction;
        Shown = Math.max(0.0f, Math.min(1.0f, Shown + ShownRate + Correction));
        tickAnimation();
    }

    @Override
    public boolean triggerEvent(int Id, int Param) {
        if (Id != EventCompleted) {
            return super.triggerEvent(Id, Param);
        }
        if (level != null && level.isClientSide) {
            ItemStack Result = new ItemStack(BuiltInRegistries.ITEM.byId(Param));
            Shown = 0.0f;
            PreviousShown = 0.0f;
            Lag = 0.0f;
            onClientCompleted(Result);
            ClientCompletion.accept(this, Result);
        }
        return true;
    }

    public ItemStackHandler getStorage() {
        return Storage;
    }

    public IItemHandler getItemHandler(@Nullable Direction Side) {
        if (Side == null) {
            return Automation;
        }
        return Side == Direction.DOWN ? OutputSide : InputSide;
    }

    public ContainerData getData() {
        return Data;
    }

    public MachineStatus getStatus() {
        return Status;
    }

    public float getProgressFraction() {
        return Total > 0 ? Math.min(1.0f, Progress / Total) : 0.0f;
    }


    public float getShownProgress(float PartialTick) {
        return PreviousShown + (Shown - PreviousShown) * PartialTick;
    }

    public float getShownRate() {
        return ShownRate;
    }

    public ItemStack getShownWorkpiece() {
        return ShownWorkpiece;
    }

    public boolean isWorking() {
        BlockState State = getBlockState();
        return State.hasProperty(ProcessingMachineBlock.Active) && State.getValue(ProcessingMachineBlock.Active);
    }

    public void dropContents(Level World, BlockPos Pos) {
        for (int Slot = 0; Slot < SlotCount; Slot++) {
            Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Storage.getStackInSlot(Slot).copy());
            Storage.setStackInSlot(Slot, ItemStack.EMPTY);
        }
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

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
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
        Tag.put("Items", Storage.serializeNBT(Registries));
        Upgrades.save(Tag);
        Tag.putFloat("Progress", Progress);
        if (JobId != null) {
            Tag.putString("Job", JobId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Items")) {
            Storage.deserializeNBT(Registries, Tag.getCompound("Items"));
        }
        Upgrades.load(Tag);
        if (Tag.contains("Progress")) {
            Progress = Math.max(0.0f, Tag.getFloat("Progress"));
        }
        if (Tag.contains("Job")) {
            JobId = ResourceLocation.tryParse(Tag.getString("Job"));
        }
        if (Tag.contains("Shown")) {
            float Synced = Tag.getFloat("Shown");
            float Difference = Synced - Shown;
            if (Math.abs(Difference) > SnapDistance) {
                Shown = Synced;
                PreviousShown = Synced;
                Lag = 0.0f;
            } else {
                Lag = Difference;
            }
            ShownRate = Tag.getFloat("Rate");
            ShownWorkpiece = Tag.contains("Workpiece") ? ItemStack.parseOptional(Registries, Tag.getCompound("Workpiece")) : ItemStack.EMPTY;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putFloat("Shown", getProgressFraction());
        Tag.putFloat("Rate", Rate);
        ItemStack Input = Storage.getStackInSlot(InputSlot);
        if (!Input.isEmpty()) {
            Tag.put("Workpiece", Input.copyWithCount(1).save(Registries));
        }
        writeSync(Tag, Registries);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
