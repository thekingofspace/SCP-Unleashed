package net.rai.scp_unleashed.labware;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

public class LabAnalyzerBlockEntity extends BlockEntity implements WireDevice, MenuProvider {
    public static final int InputSlot = 0;
    public static final int OutputSlot = 1;
    public static final int SlotCount = 2;
    public static final int WorkDraw = 12;
    public static final int WorkTicks = 120;
    public static final int ProgressScale = 1000;
    public static final int ActiveHold = 10;
    public static final double ReadoutRange = 8.0;
    public static final int DataProgress = 0;
    public static final int DataReceived = 1;
    public static final int DataDemand = 2;
    public static final int DataStatus = 3;
    public static final int DataCount = 4;

    private final ItemStackHandler Storage = new ItemStackHandler(SlotCount) {
        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return Slot == InputSlot && LabVials.isUsedSwab(Stack);
        }

        @Override
        protected void onContentsChanged(int Slot) {
            if (Slot == InputSlot) {
                Progress = 0.0f;
            }
            contentsChanged();
        }
    };

    private int Received;
    private int LastReceived;
    private int Demand;
    private int LastDemand;
    private float Progress;
    private LabAnalyzerStatus Status = LabAnalyzerStatus.Idle;
    private long LastWorked = Long.MIN_VALUE / 2;
    private boolean Primed;
    private int Assays;
    @Nullable
    private AssayRecord LastResult;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case DataProgress -> Math.min(ProgressScale, Math.round(Progress * ProgressScale / WorkTicks));
                case DataReceived -> Math.min(Short.MAX_VALUE, LastReceived);
                case DataDemand -> Math.min(Short.MAX_VALUE, Demand);
                case DataStatus -> Status.ordinal();
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

    public LabAnalyzerBlockEntity(BlockPos Pos, BlockState State) {
        super(LabwareRegistry.AnalyzerEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, LabAnalyzerBlockEntity Machine) {
        if (World instanceof ServerLevel Server && State.getBlock() instanceof LabAnalyzerBlock) {
            Machine.tickServer(Server, Pos, State);
        }
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        if (!Primed) {
            Primed = true;
            if (State.getValue(LabAnalyzerBlock.Running)) {
                LastWorked = Now;
            }
        }
        LastReceived = Received;
        LastDemand = Demand;
        Received = 0;
        ItemStack Input = Storage.getStackInSlot(InputSlot);
        LabSwabRecord Record = LabVials.isUsedSwab(Input) ? LabVials.recordOf(Input) : null;
        if (Record == null) {
            Progress = 0.0f;
            Status = LabAnalyzerStatus.Idle;
        } else if (!Storage.getStackInSlot(OutputSlot).isEmpty()) {
            Status = LabAnalyzerStatus.Blocked;
        } else if (LastReceived <= 0) {
            Status = LastDemand > 0 ? LabAnalyzerStatus.Unpowered : LabAnalyzerStatus.Working;
        } else {
            Status = LastReceived >= LastDemand ? LabAnalyzerStatus.Working : LabAnalyzerStatus.Underpowered;
            if (Progress <= 0.0f) {
                LabwareSounds.play(World, Pos, LabwareSounds.Cycling, LabwareSounds.pitch(World));
            }
            Progress += Math.min(1.0f, LastReceived / (float) WorkDraw);
            LastWorked = Now;
            if (Progress >= WorkTicks) {
                complete(World, Pos, Input, Record);
            }
            setChanged();
        }
        Demand = Record != null && Status != LabAnalyzerStatus.Blocked ? WorkDraw : 0;
        updateRunning(World, Pos, Now - LastWorked <= ActiveHold);
    }

    private void complete(ServerLevel World, BlockPos Pos, ItemStack Input, LabSwabRecord Record) {
        AssayRecord Result = AssayRecord.of(Record, World.getDayTime());
        Storage.setStackInSlot(OutputSlot, LabResultSlipItem.of(Result));
        Storage.setStackInSlot(InputSlot, Input.getCount() > 1 ? Input.copyWithCount(Input.getCount() - 1) : ItemStack.EMPTY);
        Progress = 0.0f;
        LastResult = Result;
        Assays++;
        LabwareSounds.play(World, Pos, Result.isPositive() ? LabwareSounds.ReadPositive : LabwareSounds.ReadClean, 1.0f);
        announce(World, Pos, Result);
        BlockState Current = getBlockState();
        World.sendBlockUpdated(Pos, Current, Current, Block.UPDATE_CLIENTS);
    }

    public static Component summary(AssayRecord Result) {
        Component Subject = Result.subjectName();
        if (!Result.isPositive()) {
            return Component.translatable("message.scp_unleashed.labware.assay.clean", Subject).withStyle(ChatFormatting.GREEN);
        }
        Component First = LabOrganisms.styledNameOf(Result.Findings().get(0));
        int Others = Result.Findings().size() - 1;
        return (Others <= 0 ? Component.translatable("message.scp_unleashed.labware.assay.positive", Subject, First)
                : Component.translatable("message.scp_unleashed.labware.assay.positive_more", Subject, First, Others)).withStyle(ChatFormatting.RED);
    }

    private void announce(ServerLevel World, BlockPos Pos, AssayRecord Result) {
        Component Line = summary(Result);
        Vec3 Centre = Vec3.atCenterOf(Pos);
        double Reach = ReadoutRange * ReadoutRange;
        for (ServerPlayer Nearby : World.players()) {
            if (Nearby.distanceToSqr(Centre) <= Reach) {
                Nearby.sendSystemMessage(Line);
            }
        }
    }

    private void updateRunning(ServerLevel World, BlockPos Pos, boolean Working) {
        BlockState Current = getBlockState();
        if (Current.hasProperty(LabAnalyzerBlock.Running) && Current.getValue(LabAnalyzerBlock.Running) != Working) {
            World.setBlock(Pos, Current.setValue(LabAnalyzerBlock.Running, Working), Block.UPDATE_CLIENTS);
        }
    }

    private void contentsChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    public void slotTouched() {
        contentsChanged();
    }

    public ItemStackHandler getStorage() {
        return Storage;
    }

    public ContainerData getData() {
        return Data;
    }

    public LabAnalyzerStatus getStatus() {
        return Status;
    }

    public float getProgressFraction() {
        return Mth.clamp(Progress / WorkTicks, 0.0f, 1.0f);
    }

    @Nullable
    public AssayRecord getLastResult() {
        return LastResult;
    }

    public int getAssays() {
        return Assays;
    }

    public int getSignal() {
        return Storage.getStackInSlot(OutputSlot).isEmpty() ? (Storage.getStackInSlot(InputSlot).isEmpty() ? 0 : 7) : 15;
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    public void dropContents(Level World, BlockPos Pos) {
        for (int Slot = 0; Slot < SlotCount; Slot++) {
            ItemStack Stack = Storage.getStackInSlot(Slot);
            if (!Stack.isEmpty()) {
                Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Stack.copy());
                Storage.setStackInSlot(Slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.lab_analyzer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new LabAnalyzerMenu(ContainerId, PlayerInventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu Menu, RegistryFriendlyByteBuf Buffer) {
        Buffer.writeBlockPos(worldPosition);
    }

    @Override
    public int getPowerDemand() {
        return Demand;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Amount;
    }

    private void writeResult(CompoundTag Saved, HolderLookup.Provider Registries) {
        Saved.putInt("Assays", Assays);
        if (LastResult != null) {
            AssayRecord.AssayCodec.encodeStart(Registries.createSerializationContext(NbtOps.INSTANCE), LastResult).result()
                    .ifPresent(Encoded -> Saved.put("Result", Encoded));
        }
    }

    private void readResult(CompoundTag Saved, HolderLookup.Provider Registries) {
        Assays = Math.max(0, Saved.getInt("Assays"));
        LastResult = Saved.contains("Result")
                ? AssayRecord.AssayCodec.parse(Registries.createSerializationContext(NbtOps.INSTANCE), Saved.get("Result")).result().orElse(null) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Items", Storage.serializeNBT(Registries));
        Tag.putFloat("Progress", Progress);
        writeResult(Tag, Registries);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        readResult(Tag, Registries);
        if (Tag.contains("Sync")) {
            return;
        }
        if (Tag.contains("Items")) {
            CompoundTag Saved = Tag.getCompound("Items");
            Saved.putInt("Size", SlotCount);
            Storage.deserializeNBT(Registries, Saved);
        }
        Progress = Mth.clamp(Tag.getFloat("Progress"), 0.0f, WorkTicks);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Sync", true);
        writeResult(Tag, Registries);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
