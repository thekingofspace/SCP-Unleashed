package net.rai.scp_unleashed.rifttech;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.rai.scp_unleashed.anomaly.Extractable;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.extraction.ExtractionRegistry;
import net.rai.scp_unleashed.extraction.ExtractionTargeting;
import net.rai.scp_unleashed.upgrade.MachineUpgrades;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import net.rai.scp_unleashed.upgrade.Upgradeable;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class RiftCollectorBlockEntity extends BlockEntity implements WireDevice, MenuProvider, Upgradeable {
    public static final int SyncTicks = 10;
    public static final int ProgressScale = 1000;
    public static final int DataStatus = 0;
    public static final int DataReceived = 1;
    public static final int DataDemand = 2;
    public static final int DataRange = 3;
    public static final int DataProgress = 4;
    public static final int DataKind = 5;
    public static final int DataDistance = 6;
    public static final int DataCollected = 7;
    public static final int DataUpgrades = 8;
    public static final int DataOffsetX = 9;
    public static final int DataOffsetZ = 10;
    public static final int DataCount = 11;
    public static final int KindApocalyptic = FragmentRarity.values().length + 1;
    public static final float MinStep = 0.0025f;

    private static Consumer<RiftCollectorBlockEntity> ClientEffects = Collector -> {
    };

    private final ItemStackHandler Buffer = new ItemStackHandler(CollectorTuning.BufferSlots) {
        @Override
        protected void onContentsChanged(int Slot) {
            setChanged();
        }
    };
    private final IItemHandler Output = new DrainHandler();
    private final MachineUpgrades Upgrades = new MachineUpgrades(this::upgradesChanged, UpgradeType.Range);

    private int Received;
    private int LastReceived;
    private int Demand = CollectorTuning.IdleDraw;
    private int PreviousDemand;
    private final float[] Window = new float[CollectorTuning.WindowTicks];
    private int WindowFilled;
    private int WindowCursor;
    private float Flow;
    private float TickRatio;
    private float WorkFlow = 1.0f;

    private int TargetId = -1;
    private int ScanCooldown;
    private int SightCooldown;
    private boolean SawBlocked;
    private float AimYaw = Float.NaN;
    private float AimPitch;
    private boolean Locked;
    private CollectorStatus Status = CollectorStatus.Unpowered;
    private int Collected;
    private float Progress;
    private float Carry;
    private int Kind;
    private float Distance;
    private int OffsetX;
    private int OffsetZ;

    private int SyncedTarget = -1;
    private CollectorStatus SyncedStatus = CollectorStatus.Unpowered;
    private boolean NeedsSync;
    private boolean UrgentSync;
    private long LastSync = Long.MIN_VALUE / 2;

    private int ClientTarget = -1;
    private CollectorStatus ClientStatus = CollectorStatus.Unpowered;
    private float ClientYaw = Float.NaN;
    private float ClientYawOld;
    private float ClientPitch = CollectorTuning.RestPitch;
    private float ClientPitchOld = CollectorTuning.RestPitch;
    private float TurnSpeed;
    private int ClientTicks;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case DataStatus -> Status.ordinal();
                case DataReceived -> Math.min(Short.MAX_VALUE, LastReceived);
                case DataDemand -> Math.min(Short.MAX_VALUE, Demand);
                case DataRange -> getRange();
                case DataProgress -> Math.round(Progress * ProgressScale);
                case DataKind -> Kind;
                case DataDistance -> Math.min(Short.MAX_VALUE, Math.round(Distance * 10.0f));
                case DataCollected -> Math.min(Short.MAX_VALUE, Collected);
                case DataUpgrades -> Upgrades.count(UpgradeType.Range);
                case DataOffsetX -> OffsetX;
                case DataOffsetZ -> OffsetZ;
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

    public RiftCollectorBlockEntity(BlockPos Pos, BlockState State) {
        super(RiftTechRegistry.RiftCollectorEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<RiftCollectorBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, RiftCollectorBlockEntity Collector) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof RiftCollectorBlock) {
            Collector.tickServer(ServerWorld, Pos, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, RiftCollectorBlockEntity Collector) {
        Collector.tickClient(World);
        ClientEffects.accept(Collector);
    }

    public static float restYaw(Direction Front) {
        return switch (Front) {
            case EAST -> -90.0f;
            case SOUTH -> 180.0f;
            case WEST -> 90.0f;
            default -> 0.0f;
        };
    }

    public static float yawTowards(Vec3 Offset) {
        return (float) Math.toDegrees(Math.atan2(-Offset.x, -Offset.z));
    }

    public static float pitchTowards(Vec3 Offset) {
        return (float) Math.toDegrees(Math.atan2(Offset.y, Math.sqrt(Offset.x * Offset.x + Offset.z * Offset.z)));
    }

    public static Vec3 heading(float Yaw, float Pitch) {
        double YawRadians = Math.toRadians(Yaw);
        double PitchRadians = Math.toRadians(Pitch);
        double Across = Math.cos(PitchRadians);
        return new Vec3(-Math.sin(YawRadians) * Across, Math.sin(PitchRadians), -Math.cos(YawRadians) * Across);
    }

    public Vec3 getPivot() {
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + CollectorTuning.PivotHeight, worldPosition.getZ() + 0.5);
    }

    public Vec3 getEmitter(float PartialTick) {
        return getPivot().add(heading(getClientYaw(PartialTick), getClientPitch(PartialTick)).scale(CollectorTuning.BarrelLength));
    }

    private Direction front() {
        BlockState State = getBlockState();
        return State.hasProperty(RiftCollectorBlock.Facing) ? State.getValue(RiftCollectorBlock.Facing) : Direction.NORTH;
    }

    public int getRange() {
        return CollectorTuning.rangeFor(Upgrades.count(UpgradeType.Range));
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        if (Float.isNaN(AimYaw)) {
            AimYaw = restYaw(front());
            AimPitch = CollectorTuning.RestPitch;
        }
        int Delivered = Received;
        Received = 0;
        LastReceived = Delivered;
        TickRatio = PreviousDemand > 0 ? Math.min(1.0f, (float) Delivered / PreviousDemand) : 0.0f;
        Window[WindowCursor] = Math.min(1.0f, (float) Delivered / CollectorTuning.IdleDraw);
        WindowCursor = (WindowCursor + 1) % Window.length;
        WindowFilled = Math.min(Window.length, WindowFilled + 1);
        Flow = rollingFlow();
        Entity Target = TargetId >= 0 ? World.getEntity(TargetId) : null;
        if (Flow < CollectorTuning.PoweredFlow) {
            release(World, Target);
            Status = CollectorStatus.Unpowered;
            Demand = CollectorTuning.IdleDraw;
            AimPitch = Mth.approach(AimPitch, CollectorTuning.RestPitch, CollectorTuning.PitchSpeed * 0.25f);
        } else {
            work(World, Target);
        }
        PreviousDemand = Demand;
        RiftCollectorBlock.setMode(World, Pos, State, Status.getMode());
        updateSync(World, Now);
    }

    private float rollingFlow() {
        if (WindowFilled == 0) {
            return 0.0f;
        }
        float Sum = 0.0f;
        for (int Index = 0; Index < WindowFilled; Index++) {
            Sum += Window[Index];
        }
        return Sum / WindowFilled;
    }

    private void work(ServerLevel World, @Nullable Entity Target) {
        if (Target != null && !stillValid(World, Target)) {
            release(World, Target);
            Target = null;
        }
        if (Target == null) {
            if (--ScanCooldown <= 0) {
                ScanCooldown = CollectorTuning.ScanTicks;
                Target = findTarget(World);
                if (Target != null) {
                    TargetId = Target.getId();
                    Carry = 0.0f;
                    Locked = false;
                    SightCooldown = CollectorTuning.SightTicks;
                }
            }
        }
        if (!(Target instanceof Extractable Rift)) {
            TargetId = -1;
            Progress = 0.0f;
            Kind = 0;
            Distance = 0.0f;
            Locked = false;
            Status = SawBlocked ? CollectorStatus.Blocked : CollectorStatus.Scanning;
            Demand = CollectorTuning.IdleDraw;
            return;
        }
        boolean Apocalyptic = Target instanceof ApocalypticRiftEntity;
        Kind = Apocalyptic ? KindApocalyptic : Rift.getRarity().getTier();
        Vec3 Centre = Target.getBoundingBox().getCenter();
        Vec3 Offset = Centre.subtract(getPivot());
        Distance = (float) Offset.length();
        OffsetX = Mth.clamp((int) Math.round(Offset.x), -127, 127);
        OffsetZ = Mth.clamp((int) Math.round(Offset.z), -127, 127);
        Progress = Rift.getExtraction();
        if (!canStore(Rift)) {
            Status = CollectorStatus.Full;
            Demand = CollectorTuning.IdleDraw;
            Locked = false;
            return;
        }
        int WorkDraw = CollectorTuning.drawFor(Rift.getRarity(), Apocalyptic);
        float WantYaw = yawTowards(Offset);
        float WantPitch = Mth.clamp(pitchTowards(Offset), CollectorTuning.MinPitch, CollectorTuning.MaxPitch);
        AimYaw = Mth.approachDegrees(AimYaw, WantYaw, CollectorTuning.YawSpeed);
        AimPitch = Mth.approach(AimPitch, WantPitch, CollectorTuning.PitchSpeed);
        boolean Aligned = Mth.degreesDifferenceAbs(AimYaw, WantYaw) <= CollectorTuning.LockTolerance && Math.abs(AimPitch - WantPitch) <= CollectorTuning.LockTolerance;
        if (Aligned && !Locked) {
            Locked = true;
            Vec3 Pivot = getPivot();
            World.playSound(null, Pivot.x, Pivot.y, Pivot.z, RiftTechRegistry.CollectorLock.get(), SoundSource.BLOCKS, 0.7f, 1.0f + 0.04f * Kind);
        }
        if (!Locked) {
            Status = CollectorStatus.Tracking;
            Demand = CollectorTuning.IdleDraw;
            return;
        }
        if (Target instanceof ApocalypticRiftEntity Golden) {
            Golden.notePuller(worldPosition);
        }
        Demand = WorkDraw;
        boolean Funded = PreviousDemand >= WorkDraw;
        float Share = Funded ? TickRatio : 0.0f;
        if (Funded) {
            WorkFlow += (TickRatio - WorkFlow) * 0.2f;
        }
        Status = WorkFlow < CollectorTuning.WorkingFlow ? CollectorStatus.LowPower : CollectorStatus.Collecting;
        if (Share <= 0.0f) {
            return;
        }
        float Current = Rift.getExtraction();
        float Add = ExtractionTargeting.stepFor(Rift.getRarity()) * Share + Carry;
        float Next = Math.min(1.0f, Current + Add);
        if (Next < 1.0f && Add < MinStep) {
            Carry = Add;
            Rift.setExtraction(Current);
            return;
        }
        Carry = 0.0f;
        Rift.setExtraction(Next);
        Progress = Rift.getExtraction();
        if (Next >= 1.0f) {
            complete(World, Target, Rift);
        }
    }

    private boolean stillValid(ServerLevel World, Entity Target) {
        if (!Target.isAlive() || !(Target instanceof Extractable Rift) || !Rift.canBeExtracted()) {
            return false;
        }
        Vec3 Centre = Target.getBoundingBox().getCenter();
        if (Centre.distanceTo(getPivot()) > getRange()) {
            return false;
        }
        if (--SightCooldown <= 0) {
            SightCooldown = CollectorTuning.SightTicks;
            return hasSight(World, Centre);
        }
        return true;
    }

    @Nullable
    private Entity findTarget(ServerLevel World) {
        Vec3 Pivot = getPivot();
        double Range = getRange();
        SawBlocked = false;
        List<Entity> Candidates = World.getEntities((Entity) null, new AABB(Pivot, Pivot).inflate(Range + 2.0),
                Candidate -> Candidate instanceof Extractable Rift && Candidate.isAlive() && Rift.canBeExtracted());
        Candidates.sort(Comparator.comparingDouble(Candidate -> Candidate.getBoundingBox().getCenter().distanceToSqr(Pivot)));
        for (Entity Candidate : Candidates) {
            Vec3 Centre = Candidate.getBoundingBox().getCenter();
            Vec3 Offset = Centre.subtract(Pivot);
            if (Offset.length() > Range) {
                continue;
            }
            float Pitch = pitchTowards(Offset);
            if (Pitch < CollectorTuning.MinPitch - CollectorTuning.LockTolerance || Pitch > CollectorTuning.MaxPitch + CollectorTuning.LockTolerance) {
                SawBlocked = true;
                continue;
            }
            if (!hasSight(World, Centre)) {
                SawBlocked = true;
                continue;
            }
            return Candidate;
        }
        return null;
    }

    private boolean hasSight(ServerLevel World, Vec3 Centre) {
        Vec3 Pivot = getPivot();
        Vec3 Offset = Centre.subtract(Pivot);
        double Length = Offset.length();
        if (Length <= CollectorTuning.SightClearance + 0.05) {
            return true;
        }
        Vec3 Start = Pivot.add(Offset.scale(CollectorTuning.SightClearance / Length));
        return World.clip(new ClipContext(Start, Centre, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.MISS;
    }

    private ItemStack fragmentOf(Extractable Rift) {
        ItemStack Fragment = Rift.createFragment();
        return Fragment.isEmpty() ? new ItemStack(ExtractionRegistry.fragmentFor(Rift.getRarity())) : Fragment;
    }

    private boolean canStore(Extractable Rift) {
        return ItemHandlerHelper.insertItem(Buffer, fragmentOf(Rift), true).isEmpty();
    }

    private void complete(ServerLevel World, Entity Target, Extractable Rift) {
        FragmentRarity Grade = Rift.getRarity();
        Vec3 Centre = Target.getBoundingBox().getCenter();
        ItemStack Fragment = fragmentOf(Rift);
        Rift.collapse();
        ItemStack Left = ItemHandlerHelper.insertItem(Buffer, Fragment, false);
        if (!Left.isEmpty()) {
            Containers.dropItemStack(World, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, Left);
        }
        Collected++;
        TargetId = -1;
        Locked = false;
        Progress = 0.0f;
        ScanCooldown = CollectorTuning.ScanTicks;
        World.playSound(null, Centre.x, Centre.y, Centre.z, ExtractionRegistry.ExtractionSuccess.get(), SoundSource.BLOCKS, 1.0f, 1.12f - 0.07f * (Grade.getTier() - 1));
        setChanged();
        UrgentSync = true;
        NeedsSync = true;
    }

    private void release(ServerLevel World, @Nullable Entity Target) {
        if (TargetId < 0) {
            return;
        }
        if (Locked && Target instanceof Extractable Rift && Rift.getExtraction() > 0.0f) {
            Vec3 Emitter = getPivot().add(heading(AimYaw, AimPitch).scale(CollectorTuning.BarrelLength));
            World.playSound(null, Emitter.x, Emitter.y, Emitter.z, ExtractionRegistry.ExtractionFizzle.get(), SoundSource.BLOCKS, 0.7f, 0.85f + World.random.nextFloat() * 0.2f);
        }
        TargetId = -1;
        Locked = false;
        Progress = 0.0f;
        Carry = 0.0f;
        Kind = 0;
        ScanCooldown = 0;
    }

    private void updateSync(ServerLevel World, long Now) {
        if (Status != SyncedStatus) {
            UrgentSync |= Status.getMode() != SyncedStatus.getMode() || Status.hasTarget() != SyncedStatus.hasTarget();
            NeedsSync = true;
            SyncedStatus = Status;
        }
        if (TargetId != SyncedTarget) {
            SyncedTarget = TargetId;
            NeedsSync = true;
            UrgentSync = true;
        }
        if (NeedsSync && (UrgentSync || Now - LastSync >= SyncTicks)) {
            NeedsSync = false;
            UrgentSync = false;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void upgradesChanged() {
        setChanged();
        ScanCooldown = 0;
    }

    private void tickClient(Level World) {
        ClientTicks++;
        float Rest = restYaw(front());
        if (Float.isNaN(ClientYaw)) {
            ClientYaw = Rest;
            ClientYawOld = Rest;
        }
        ClientYawOld = ClientYaw;
        ClientPitchOld = ClientPitch;
        Entity Target = ClientTarget >= 0 ? World.getEntity(ClientTarget) : null;
        float WantYaw;
        float WantPitch;
        float YawStep = CollectorTuning.YawSpeed;
        float PitchStep = CollectorTuning.PitchSpeed;
        if (ClientStatus.hasTarget() && Target != null) {
            Vec3 Offset = Target.getBoundingBox().getCenter().subtract(getPivot());
            WantYaw = yawTowards(Offset);
            WantPitch = Mth.clamp(pitchTowards(Offset), CollectorTuning.MinPitch, CollectorTuning.MaxPitch);
        } else if (ClientStatus.isPowered()) {
            WantYaw = Rest + 55.0f * Mth.sin(ClientTicks * 0.021f + worldPosition.hashCode() * 0.37f);
            WantPitch = -4.0f + 6.0f * Mth.sin(ClientTicks * 0.013f);
            YawStep = 1.2f;
            PitchStep = 0.6f;
        } else {
            WantYaw = ClientYaw;
            WantPitch = CollectorTuning.RestPitch;
            PitchStep = CollectorTuning.PitchSpeed * 0.25f;
        }
        ClientYaw = Mth.approachDegrees(ClientYaw, WantYaw, YawStep);
        ClientPitch = Mth.approach(ClientPitch, WantPitch, PitchStep);
        TurnSpeed = Math.abs(Mth.degreesDifference(ClientYawOld, ClientYaw)) + Math.abs(ClientPitch - ClientPitchOld);
    }

    public float getClientYaw(float PartialTick) {
        if (Float.isNaN(ClientYaw)) {
            return restYaw(front());
        }
        return Mth.rotLerp(PartialTick, ClientYawOld, ClientYaw);
    }

    public float getClientPitch(float PartialTick) {
        return Mth.lerp(PartialTick, ClientPitchOld, ClientPitch);
    }

    public float getTurnSpeed() {
        return TurnSpeed;
    }

    public int getClientTicks() {
        return ClientTicks;
    }

    public int getClientTarget() {
        return ClientTarget;
    }

    public CollectorStatus getClientStatus() {
        return ClientStatus;
    }

    public boolean isBeaming() {
        return ClientStatus == CollectorStatus.Collecting || ClientStatus == CollectorStatus.LowPower;
    }

    public CollectorStatus getStatus() {
        return Status;
    }

    public int getTargetId() {
        return TargetId;
    }

    public float getProgress() {
        return Progress;
    }

    public int getKind() {
        return Kind;
    }

    public float getDistance() {
        return Distance;
    }

    public int getCollected() {
        return Collected;
    }

    public int getLastReceived() {
        return LastReceived;
    }

    public float getFlow() {
        return Flow;
    }

    public ItemStackHandler getBuffer() {
        return Buffer;
    }

    public IItemHandler getOutput() {
        return Output;
    }

    public int countStored() {
        int Total = 0;
        for (int Slot = 0; Slot < Buffer.getSlots(); Slot++) {
            Total += Buffer.getStackInSlot(Slot).getCount();
        }
        return Total;
    }

    public void dropContents(Level World, BlockPos Pos) {
        for (int Slot = 0; Slot < Buffer.getSlots(); Slot++) {
            Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Buffer.getStackInSlot(Slot).copy());
            Buffer.setStackInSlot(Slot, ItemStack.EMPTY);
        }
        Upgrades.drop(World, Pos);
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
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Buffer", Buffer.serializeNBT(Registries));
        Upgrades.save(Tag);
        Tag.putInt("Collected", Collected);
        if (!Float.isNaN(AimYaw)) {
            Tag.putFloat("AimYaw", AimYaw);
            Tag.putFloat("AimPitch", AimPitch);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Buffer")) {
            Buffer.deserializeNBT(Registries, Tag.getCompound("Buffer"));
        }
        Upgrades.load(Tag);
        Collected = Math.max(0, Tag.getInt("Collected"));
        if (Tag.contains("AimYaw")) {
            AimYaw = Tag.getFloat("AimYaw");
            AimPitch = Tag.getFloat("AimPitch");
        }
        if (Tag.contains("Status")) {
            ClientStatus = CollectorStatus.byIndex(Tag.getByte("Status"));
        }
        if (Tag.contains("Target")) {
            ClientTarget = Tag.getInt("Target");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Status", (byte) Status.ordinal());
        Tag.putInt("Target", TargetId);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.rift_collector");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new RiftCollectorMenu(ContainerId, PlayerInventory, this, Data);
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5) <= 64.0;
    }

    private class DrainHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return Buffer.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int Slot) {
            return Buffer.getStackInSlot(Slot);
        }

        @Override
        public ItemStack insertItem(int Slot, ItemStack Stack, boolean Simulate) {
            return Stack;
        }

        @Override
        public ItemStack extractItem(int Slot, int Amount, boolean Simulate) {
            return Buffer.extractItem(Slot, Amount, Simulate);
        }

        @Override
        public int getSlotLimit(int Slot) {
            return Buffer.getSlotLimit(Slot);
        }

        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return false;
        }
    }
}
