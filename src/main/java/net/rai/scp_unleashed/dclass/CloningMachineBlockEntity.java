package net.rai.scp_unleashed.dclass;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.labware.LabwareRegistry;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CloningMachineBlockEntity extends BlockEntity implements WireDevice, MenuProvider {
    public static final int CycleTicks = 2400;
    public static final int WorkingDraw = 80;
    public static final int PowerGrace = 10;
    public static final int FillTicks = 120;
    public static final int OpenTicks = 50;
    public static final int DrainTicks = 20;
    public static final int SyncTicks = 10;
    public static final int BubbleChance = 90;
    public static final int InputSlot = 0;
    public static final int VialSlot = 1;
    public static final int SlotCount = 2;
    public static final int ProgressScale = 1000;
    public static final int ProgressData = 0;
    public static final int ReceivedData = 1;
    public static final int DemandData = 2;
    public static final int StatusData = 3;
    public static final int KindData = 4;
    public static final int DataCount = 5;
    public static final String SampleNameKey = "Name";

    public interface Hatch {
        @Nullable
        LivingEntity hatch(ServerLevel World, Vec3 At, float Yaw, int Appearance);

        int appearance(ServerLevel World);

        @Nullable
        default LivingEntity hatch(ServerLevel World, Vec3 At, float Yaw, int Appearance, CompoundTag Sample) {
            return hatch(World, At, Yaw, Appearance);
        }
    }

    private static final Map<CloningKind, Hatch> Hatches = new EnumMap<>(CloningKind.class);

    private final ItemStackHandler Contents = new ItemStackHandler(SlotCount) {
        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return Slot == InputSlot && Stack.getItem() instanceof CloningBloodItem;
        }

        @Override
        protected void onContentsChanged(int Slot) {
            setChanged();
        }
    };
    private final IItemHandler Access = new IItemHandler() {
        @Override
        public int getSlots() {
            return SlotCount;
        }

        @Override
        public ItemStack getStackInSlot(int Slot) {
            return Contents.getStackInSlot(Slot);
        }

        @Override
        public ItemStack insertItem(int Slot, ItemStack Stack, boolean Simulate) {
            return Slot == InputSlot ? Contents.insertItem(Slot, Stack, Simulate) : Stack;
        }

        @Override
        public ItemStack extractItem(int Slot, int Amount, boolean Simulate) {
            return Slot == VialSlot ? Contents.extractItem(Slot, Amount, Simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int Slot) {
            return Contents.getSlotLimit(Slot);
        }

        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return Contents.isItemValid(Slot, Stack);
        }
    };
    private float Progress;
    private int Demand;
    private int Received;
    private int LastReceived;
    private long LastPowered = Long.MIN_VALUE / 2;
    private CloningKind Growing = CloningKind.None;
    private int Appearance;
    private CompoundTag Sample = new CompoundTag();
    private CompoundTag SpecimenSample = new CompoundTag();
    private CloningStatus Status = CloningStatus.Off;
    private int Releasing;
    private boolean NeedsSync;
    private boolean UrgentSync;
    private long LastSync = Long.MIN_VALUE / 2;
    private float Shown;
    private float ShownO;
    private float Rate;
    private int ClientTicks;
    @Nullable
    private DClassLike Specimen;
    private CloningKind SpecimenKind = CloningKind.None;
    private int SpecimenAppearance;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case ProgressData -> Math.round(Math.min(1.0f, Progress / CycleTicks) * ProgressScale);
                case ReceivedData -> LastReceived;
                case DemandData -> Demand;
                case StatusData -> Status.ordinal();
                case KindData -> Growing.ordinal();
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

    public CloningMachineBlockEntity(BlockPos Pos, BlockState State) {
        super(DClassRegistry.CloningMachineEntity.get(), Pos, State);
    }

    public static void registerHatch(CloningKind Kind, Hatch Spawner) {
        Hatches.put(Kind, Spawner);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, CloningMachineBlockEntity Tank) {
        if (World instanceof ServerLevel ServerWorld) {
            Tank.tickServer(ServerWorld);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, CloningMachineBlockEntity Tank) {
        Tank.tickClient();
    }

    private void tickServer(ServerLevel World) {
        long Now = World.getGameTime();
        int Got = Received;
        Received = 0;
        LastReceived = Got;
        if (Got > 0) {
            LastPowered = Now;
        }
        if (Releasing > 0) {
            Releasing--;
            if (Releasing == 0) {
                NeedsSync = true;
                UrgentSync = true;
            }
        }
        boolean Powered = Now - LastPowered <= PowerGrace;
        boolean Blocked = false;
        if (Growing == CloningKind.None) {
            Demand = 0;
            if (Powered && Releasing == 0 && hasBlood() && trayRoom()) {
                begin(World);
            }
        } else if (Progress >= CycleTicks) {
            Demand = 0;
            Blocked = !release(World);
        } else {
            Demand = WorkingDraw;
            float Fraction = Math.min(1.0f, (float) Got / WorkingDraw);
            if (Fraction > 0.0f) {
                Progress = Math.min(CycleTicks, Progress + Fraction);
                setChanged();
            }
            if (Math.abs(Fraction - Rate) > 0.05f) {
                Rate = Fraction;
                NeedsSync = true;
            }
        }
        updateStatus(World, Powered, Blocked);
        if (Growing != CloningKind.None && Now % 40 == 0 && Status == CloningStatus.Growing) {
            NeedsSync = true;
        }
        if (NeedsSync && (UrgentSync || Now - LastSync >= SyncTicks)) {
            NeedsSync = false;
            UrgentSync = false;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private boolean hasBlood() {
        ItemStack Input = Contents.getStackInSlot(InputSlot);
        return Input.getItem() instanceof CloningBloodItem Blood && Hatches.containsKey(Blood.kind());
    }

    private boolean trayRoom() {
        ItemStack Tray = Contents.getStackInSlot(VialSlot);
        return Tray.isEmpty() || Tray.is(LabwareRegistry.Vial.get()) && Tray.getCount() < Tray.getMaxStackSize() && Tray.getComponentsPatch().isEmpty();
    }

    private void begin(ServerLevel World) {
        ItemStack Blood = Contents.extractItem(InputSlot, 1, false);
        if (!(Blood.getItem() instanceof CloningBloodItem Kind)) {
            return;
        }
        Hatch Spawner = Hatches.get(Kind.kind());
        if (Spawner == null) {
            Contents.insertItem(InputSlot, Blood, false);
            return;
        }
        Growing = Kind.kind();
        Appearance = Spawner.appearance(World);
        Sample = Kind.sampleOf(Blood);
        Progress = 0.0f;
        ItemStack Vial = new ItemStack(LabwareRegistry.Vial.get());
        ItemStack Tray = Contents.getStackInSlot(VialSlot);
        if (Tray.isEmpty()) {
            Contents.setStackInSlot(VialSlot, Vial);
        } else {
            Tray.grow(1);
            setChanged();
        }
        play(World, DClassRegistry.TankFillLayers, 1.0f);
        NeedsSync = true;
        UrgentSync = true;
        setChanged();
    }

    private boolean release(ServerLevel World) {
        Direction Front = getFront();
        BlockPos Door = worldPosition.relative(Front);
        if (!World.getBlockState(Door).getCollisionShape(World, Door).isEmpty() || !World.getBlockState(Door.above()).getCollisionShape(World, Door.above()).isEmpty()) {
            return false;
        }
        AABB Space = new AABB(Door).expandTowards(0.0, 1.0, 0.0).deflate(0.05);
        if (!World.getEntitiesOfClass(Entity.class, Space, Found -> Found.isAlive() && !Found.isSpectator() && (Found instanceof LivingEntity || Found.canBeCollidedWith())).isEmpty()) {
            return false;
        }
        Hatch Spawner = Hatches.get(Growing);
        Vec3 At = Vec3.atBottomCenterOf(Door);
        LivingEntity Born = Spawner == null ? null : Spawner.hatch(World, At, Front.toYRot(), Appearance, Sample);
        if (Born == null) {
            return false;
        }
        Growing = CloningKind.None;
        Sample = new CompoundTag();
        Progress = 0.0f;
        Releasing = OpenTicks;
        play(World, DClassRegistry.TankReleaseLayers, 1.0f);
        World.sendParticles(ParticleTypes.CLOUD, At.x, At.y + 1.0, At.z, 10, 0.25, 0.5, 0.25, 0.02);
        World.sendParticles(ParticleTypes.SPLASH, At.x - Front.getStepX() * 0.4, At.y + 0.3, At.z - Front.getStepZ() * 0.4, 30, 0.3, 0.1, 0.3, 0.1);
        NeedsSync = true;
        UrgentSync = true;
        setChanged();
        return true;
    }

    private void updateStatus(ServerLevel World, boolean Powered, boolean Blocked) {
        CloningStatus Next;
        if (Releasing > 0) {
            Next = CloningStatus.Releasing;
        } else if (Blocked) {
            Next = CloningStatus.Blocked;
        } else if (!Powered) {
            Next = CloningStatus.Off;
        } else if (Growing != CloningKind.None) {
            Next = CloningStatus.Growing;
        } else if (!hasBlood()) {
            Next = CloningStatus.Idle;
        } else if (!trayRoom()) {
            Next = CloningStatus.TrayFull;
        } else {
            Next = CloningStatus.Idle;
        }
        if (Next != Status) {
            Status = Next;
            NeedsSync = true;
            UrgentSync = true;
        }
        CloningMachineBlock.setLook(World, worldPosition, Status.lamp(), Releasing > 0);
    }

    private void play(ServerLevel World, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Pitch) {
        float Varied = Pitch * (0.95f + World.getRandom().nextFloat() * 0.1f);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, Layer.get(), SoundSource.BLOCKS, 0.8f, Varied);
        }
    }

    public boolean feed(ItemStack Stack, Player User) {
        ItemStack Rest = Contents.insertItem(InputSlot, Stack.copyWithCount(1), false);
        if (!Rest.isEmpty()) {
            return false;
        }
        Stack.consume(1, User);
        if (level instanceof ServerLevel World) {
            play(World, DClassRegistry.TankFeedLayers, 1.0f);
        }
        return true;
    }

    private void tickClient() {
        ClientTicks++;
        if (Releasing > 0) {
            Releasing--;
        }
        ShownO = Shown;
        float Target = Math.min(1.0f, Progress / CycleTicks);
        if (Growing == CloningKind.None) {
            Shown = 0.0f;
        } else if (Status == CloningStatus.Growing) {
            Shown = Math.min(Target + 0.04f, Math.max(Shown, Target - 0.04f) + Rate / CycleTicks);
        } else {
            Shown = Target;
        }
        if (Specimen != null) {
            Specimen.tickCount++;
            Specimen.tickLook();
        }
        if (level != null && Status == CloningStatus.Growing && level.random.nextInt(BubbleChance) == 0) {
            BlockPos At = getBlockPos();
            level.playLocalSound(At.getX() + 0.5, At.getY() + 1.0, At.getZ() + 0.5, DClassRegistry.TankBubble.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.random.nextFloat() * 0.2f, false);
        }
    }

    public float shownProgress(float Partial) {
        return ShownO + (Shown - ShownO) * Partial;
    }

    public int clientTicks() {
        return ClientTicks;
    }

    public float fill(float Partial) {
        if (Growing == CloningKind.None) {
            return Releasing > 0 ? Math.max(0.0f, (Releasing - Partial - (OpenTicks - DrainTicks)) / DrainTicks) : 0.0f;
        }
        return Math.min(1.0f, shownProgress(Partial) * CycleTicks / FillTicks);
    }

    public boolean isReleasing() {
        return Releasing > 0;
    }

    public CloningKind growing() {
        return Growing;
    }

    public int appearance() {
        return Appearance;
    }

    public CompoundTag sample() {
        return Sample;
    }

    public String sampleName() {
        return Sample.getString(SampleNameKey);
    }

    public CloningStatus status() {
        return Status;
    }

    public float progressFraction() {
        return Math.min(1.0f, Progress / CycleTicks);
    }

    public int lastReceived() {
        return LastReceived;
    }

    @Nullable
    public DClassLike specimen() {
        return Specimen;
    }

    public void setSpecimen(@Nullable DClassLike Shown, CloningKind Kind, int Code) {
        Specimen = Shown;
        SpecimenKind = Kind;
        SpecimenAppearance = Code;
        SpecimenSample = Sample;
    }

    public boolean specimenMatches() {
        return Specimen != null && SpecimenKind == Growing && SpecimenAppearance == Appearance && SpecimenSample.equals(Sample);
    }

    public Direction getFront() {
        BlockState State = getBlockState();
        return State.hasProperty(CloningMachineBlock.Facing) ? State.getValue(CloningMachineBlock.Facing) : Direction.NORTH;
    }

    public AABB getRenderBounds() {
        return new AABB(worldPosition).expandTowards(0.0, 1.0, 0.0);
    }

    public ItemStackHandler getContents() {
        return Contents;
    }

    public IItemHandler access() {
        return Access;
    }

    public void dropContents(Level World, BlockPos Pos) {
        for (int Slot = 0; Slot < SlotCount; Slot++) {
            Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Contents.getStackInSlot(Slot).copy());
            Contents.setStackInSlot(Slot, ItemStack.EMPTY);
        }
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5) <= 64.0;
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
    public void observeNetwork(long Available) {
        if (Available > 0 && level != null) {
            LastPowered = level.getGameTime();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.saveAdditional(Saved, Registries);
        Saved.put("Inventory", Contents.serializeNBT(Registries));
        Saved.putFloat("Progress", Progress);
        Saved.putByte("Growing", (byte) Growing.ordinal());
        Saved.putInt("Appearance", Appearance);
        if (!Sample.isEmpty()) {
            Saved.put("Sample", Sample.copy());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.loadAdditional(Saved, Registries);
        if (Saved.contains("Inventory")) {
            Contents.deserializeNBT(Registries, Saved.getCompound("Inventory"));
        }
        Progress = Saved.getFloat("Progress");
        Growing = CloningKind.byIndex(Saved.getByte("Growing"));
        Appearance = Saved.getInt("Appearance");
        Sample = Saved.getCompound("Sample");
        if (Saved.contains("Status")) {
            Status = CloningStatus.byIndex(Saved.getByte("Status"));
            Releasing = Saved.getInt("Releasing");
            Rate = Saved.getFloat("Rate");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Saved = new CompoundTag();
        Saved.putByte("Status", (byte) Status.ordinal());
        Saved.putByte("Growing", (byte) Growing.ordinal());
        Saved.putInt("Appearance", Appearance);
        Saved.putFloat("Progress", Progress);
        Saved.putInt("Releasing", Releasing);
        Saved.putFloat("Rate", Rate);
        if (!Sample.isEmpty()) {
            Saved.put("Sample", Sample.copy());
        }
        return Saved;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.cloning_machine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new CloningMenu(ContainerId, PlayerInventory, this, Data);
    }
}
