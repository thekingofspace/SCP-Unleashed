package net.rai.scp_unleashed.matrix;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.rift.RiftEntity;
import net.rai.scp_unleashed.rift.RiftFormation;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class InstabilityMatrixBlockEntity extends BlockEntity implements WireDevice, MenuProvider {
    public static final int SurgeEvent = 1;
    public static final int BlipCount = 4;
    public static final int Scale = 1000;
    public static final int ResonanceSteps = 100;
    public static final int FlowSteps = 16;
    public static final int SyncTicks = 10;
    public static final int WindowTicks = 20;
    public static final int ObstructedAfter = 2;
    public static final int Never = -1;
    public static final int DataTier = 0;
    public static final int DataEnabled = 1;
    public static final int DataStatus = 2;
    public static final int DataReceived = 3;
    public static final int DataDemand = 4;
    public static final int DataResonance = 5;
    public static final int DataFlow = 6;
    public static final int DataNearby = 7;
    public static final int DataSince = 8;
    public static final int DataFormed = 9;
    public static final int DataBlips = 10;
    public static final int DataCount = DataBlips + BlipCount * 2;
    public static final double CoreHeight = 1.46875;
    public static final float LowFlow = 0.95f;
    public static final float FullFlow = 0.99f;
    public static final float NoFlow = 0.01f;
    public static final float SurgeVolume = 1.5f;
    public static final float LatticeWrap = 3600.0f;
    public static final float RingWrap = 1440.0f;
    public static final float TearVolume = 2.0f;

    private static Consumer<InstabilityMatrixBlockEntity> ClientEffects = Matrix -> {
    };

    private FragmentRarity Tier = FragmentRarity.Faint;
    private boolean Enabled;
    private MatrixStatus Status = MatrixStatus.Offline;
    private float Resonance;
    private int Received;
    private int LastReceived;
    private int Demand;
    private int PreviousDemand;
    private final float[] Window = new float[WindowTicks];
    private int WindowFilled;
    private int WindowCursor;
    private float Flow;
    private boolean Saturated;
    private boolean Pending;
    private int FailedPulses;
    private int Nearby;
    private int Formed;
    private long LastFormed = Long.MIN_VALUE;
    private int[] Blips = new int[BlipCount * 2];
    private boolean CensusTaken;
    private int LastSignal;
    private int SyncedResonance;
    private int SyncedFlow;
    private MatrixStatus SyncedStatus = MatrixStatus.Offline;
    private boolean NeedsSync;
    private boolean UrgentSync;
    private long LastSync = Long.MIN_VALUE / 2;

    private boolean Primed;
    private float ShownResonance;
    private float ShownResonanceOld;
    private float Glow;
    private float GlowOld;
    private float Spin;
    private float LatticeAngle;
    private float LatticeAngleOld;
    private float RingAngle;
    private float RingAngleOld;
    private float Flash;
    private float FlashOld;
    private float TintRed = 1.0f;
    private float TintGreen = 1.0f;
    private float TintBlue = 1.0f;
    private int ClientTicks;
    private int PendingSurge = -1;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            if (Index >= DataBlips && Index < DataCount) {
                return Blips[Index - DataBlips];
            }
            return switch (Index) {
                case DataTier -> Tier.getTier();
                case DataEnabled -> Enabled ? 1 : 0;
                case DataStatus -> Status.ordinal();
                case DataReceived -> Math.min(Short.MAX_VALUE, LastReceived);
                case DataDemand -> Math.min(Short.MAX_VALUE, Demand);
                case DataResonance -> Math.round(Resonance * Scale);
                case DataFlow -> Math.round(Flow * Scale);
                case DataNearby -> Nearby;
                case DataSince -> secondsSinceFormed();
                case DataFormed -> Math.min(Short.MAX_VALUE, Formed);
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

    public InstabilityMatrixBlockEntity(BlockPos Pos, BlockState State) {
        super(MatrixRegistry.MatrixEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<InstabilityMatrixBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, InstabilityMatrixBlockEntity Matrix) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof InstabilityMatrixBlock) {
            Matrix.tickServer(ServerWorld, Pos, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, InstabilityMatrixBlockEntity Matrix) {
        Matrix.tickClient();
        ClientEffects.accept(Matrix);
    }

    public Vec3 getCoreCentre() {
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + CoreHeight, worldPosition.getZ() + 0.5);
    }

    private int pulseOffset() {
        return Math.floorMod(worldPosition.hashCode(), MatrixTuning.PulseTicks);
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        int Delivered = Received;
        Received = 0;
        LastReceived = Delivered;
        int TierDraw = MatrixTuning.drawFor(Tier);
        boolean WasDemanding = PreviousDemand == TierDraw && Enabled;
        float TickRatio = WasDemanding ? Math.min(1.0f, (float) Delivered / TierDraw) : 0.0f;
        if (WasDemanding) {
            Window[WindowCursor] = TickRatio;
            WindowCursor = (WindowCursor + 1) % WindowTicks;
            WindowFilled = Math.min(WindowTicks, WindowFilled + 1);
        }
        boolean Holding = Enabled && (Saturated || isObstructed());
        if (TickRatio > 0.0f) {
            Resonance = Math.min(1.0f, Resonance + TickRatio / MatrixTuning.WarmupTicks);
        } else if (!Holding && Resonance > 0.0f) {
            Resonance = Math.max(0.0f, Resonance - 1.0f / MatrixTuning.DecayTicks);
        }
        long Phase = Now + pulseOffset();
        if (!CensusTaken || Phase % MatrixTuning.CensusTicks == 0) {
            takeCensus(World);
        }
        if (Phase % MatrixTuning.PulseTicks == 0) {
            Flow = rollingFlow();
            pulse(World, Now);
            setChanged();
        }
        boolean Dormant = !RiftFormation.allowedIn(World);
        Status = Enabled && Dormant ? MatrixStatus.Dormant : decideStatus();
        Demand = Enabled && !Dormant && !Saturated && !isObstructed() ? TierDraw : 0;
        PreviousDemand = Demand;
        InstabilityMatrixBlock.setMode(World, Pos, State, Status.getMode());
        updateSignal(World, Pos);
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

    private void takeCensus(ServerLevel World) {
        CensusTaken = true;
        MatrixField.Census Result = MatrixField.census(World, getCoreCentre());
        Nearby = Result.Nearby();
        Saturated = Result.Saturated();
        Blips = Result.Blips();
    }

    private void pulse(ServerLevel World, long Now) {
        if (!Enabled || Saturated || !RiftFormation.allowedIn(World)) {
            return;
        }
        if (!Pending && Flow > 0.0f && World.random.nextDouble() < MatrixTuning.chancePerPulse(Tier, Flow, Resonance)) {
            Pending = true;
            FailedPulses = 0;
        }
        if (!Pending) {
            return;
        }
        Vec3 Centre = getCoreCentre();
        Vec3 Spot = MatrixField.findSpot(World, Centre, Tier, World.random);
        RiftEntity Rift = Spot == null ? null : MatrixField.open(World, Spot, Tier, World.random);
        if (Rift == null) {
            FailedPulses++;
            return;
        }
        Pending = false;
        FailedPulses = 0;
        Resonance = 0.0f;
        Formed++;
        LastFormed = Now;
        Nearby++;
        Saturated = Nearby >= RiftFormation.CapCount;
        surge(World, Centre, Spot);
    }

    private void surge(ServerLevel World, Vec3 Centre, Vec3 Spot) {
        int Grade = Tier.getTier();
        float Deep = 1.08f - 0.06f * Grade;
        World.playSound(null, Centre.x, Centre.y, Centre.z, MatrixRegistry.Surge.get(), SoundSource.BLOCKS, SurgeVolume, Deep + World.random.nextFloat() * 0.06f);
        World.playSound(null, Centre.x, Centre.y, Centre.z, MatrixRegistry.SurgeCrack.get(), SoundSource.BLOCKS, SurgeVolume, 1.0f + World.random.nextFloat() * 0.1f);
        double Middle = Spot.y + RiftEntity.heightOf(Tier) * 0.5;
        World.playSound(null, Spot.x, Middle, Spot.z, MatrixRegistry.RiftTear.get(), SoundSource.AMBIENT, TearVolume, Deep);
        World.playSound(null, Spot.x, Middle, Spot.z, MatrixRegistry.RiftTearBoom.get(), SoundSource.AMBIENT, TearVolume, Deep + 0.04f);
        int DeltaX = Mth.clamp((int) Math.round(Spot.x - Centre.x), -63, 63) + 63;
        int DeltaY = Mth.clamp((int) Math.round(Middle - Centre.y), -63, 63) + 63;
        int DeltaZ = Mth.clamp((int) Math.round(Spot.z - Centre.z), -63, 63) + 63;
        World.blockEvent(worldPosition, getBlockState().getBlock(), SurgeEvent, DeltaX | (DeltaY << 7) | (DeltaZ << 14) | (Grade << 21));
    }

    public static Vec3 surgeOffset(int Param) {
        return new Vec3((Param & 127) - 63, ((Param >> 7) & 127) - 63, ((Param >> 14) & 127) - 63);
    }

    public static FragmentRarity surgeTier(int Param) {
        return FragmentRarity.byTier((Param >> 21) & 7);
    }

    private boolean isObstructed() {
        return Pending && FailedPulses >= ObstructedAfter;
    }

    private MatrixStatus decideStatus() {
        if (!Enabled) {
            return MatrixStatus.Offline;
        }
        if (Saturated) {
            return MatrixStatus.Saturated;
        }
        if (isObstructed()) {
            return MatrixStatus.Obstructed;
        }
        if (WindowFilled == 0) {
            return Status == MatrixStatus.Offline || Status.isHolding() ? MatrixStatus.Unpowered : Status;
        }
        float Recent = rollingFlow();
        if (Recent <= NoFlow) {
            return MatrixStatus.Unpowered;
        }
        if (Status == MatrixStatus.Resonating) {
            return Recent < LowFlow ? MatrixStatus.LowPower : MatrixStatus.Resonating;
        }
        return Recent >= FullFlow ? MatrixStatus.Resonating : MatrixStatus.LowPower;
    }

    private void updateSignal(ServerLevel World, BlockPos Pos) {
        int Signal = getComparatorSignal();
        if (Signal != LastSignal) {
            LastSignal = Signal;
            Block Owner = getBlockState().getBlock();
            World.updateNeighbourForOutputSignal(Pos, Owner);
            World.updateNeighbourForOutputSignal(Pos.above(), Owner);
        }
    }

    private void updateSync(ServerLevel World, long Now) {
        if (Status != SyncedStatus) {
            UrgentSync |= Status.getMode() != SyncedStatus.getMode();
            NeedsSync = true;
            SyncedStatus = Status;
        }
        int ShownLevel = Math.round(Resonance * ResonanceSteps);
        if (ShownLevel != SyncedResonance) {
            SyncedResonance = ShownLevel;
            NeedsSync = true;
        }
        int FlowLevel = Status.isRunning() ? Math.round(rollingFlow() * FlowSteps) : 0;
        if (FlowLevel != SyncedFlow) {
            UrgentSync |= FlowLevel == 0 || SyncedFlow == 0;
            SyncedFlow = FlowLevel;
            NeedsSync = true;
        }
        if (NeedsSync && (UrgentSync || Now - LastSync >= SyncTicks)) {
            NeedsSync = false;
            UrgentSync = false;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void resetWindow() {
        WindowFilled = 0;
        WindowCursor = 0;
    }

    private void markUrgent() {
        NeedsSync = true;
        UrgentSync = true;
    }

    public int getComparatorSignal() {
        return Enabled ? Mth.clamp(Math.round(Resonance * 15.0f), 0, 15) : 0;
    }

    public void selectTier(FragmentRarity Next) {
        if (Next == Tier || level == null) {
            return;
        }
        Tier = Next;
        Resonance = 0.0f;
        Pending = false;
        FailedPulses = 0;
        resetWindow();
        setChanged();
        markUrgent();
        int Grade = Next.getTier();
        play(MatrixRegistry.TierClick.get(), 0.5f, 1.3f - 0.07f * Grade);
        play(MatrixRegistry.TierChime.get(), 1.0f, 1.34f - 0.11f * Grade);
    }

    public void toggle() {
        if (level == null) {
            return;
        }
        Enabled = !Enabled;
        Pending = false;
        FailedPulses = 0;
        resetWindow();
        setChanged();
        markUrgent();
        play(Enabled ? MatrixRegistry.Start.get() : MatrixRegistry.Stop.get(), 0.8f, 1.0f);
    }

    private void play(SoundEvent Sound, float Volume, float Pitch) {
        Vec3 Centre = getCoreCentre();
        if (level != null) {
            level.playSound(null, Centre.x, Centre.y, Centre.z, Sound, SoundSource.BLOCKS, Volume, Pitch);
        }
    }

    private int secondsSinceFormed() {
        if (level == null || LastFormed == Long.MIN_VALUE) {
            return Never;
        }
        return (int) Math.min(Short.MAX_VALUE, Math.max(0L, (level.getGameTime() - LastFormed) / 20L));
    }

    private void tickClient() {
        ClientTicks++;
        float TargetResonance = (float) SyncedResonance / ResonanceSteps;
        float FlowShare = (float) SyncedFlow / FlowSteps;
        float TargetGlow = Status.isRunning() ? 0.45f + 0.55f * FlowShare : Status.isHolding() ? 0.3f : 0.0f;
        float TargetSpin = Status.isRunning() ? 0.35f + 0.65f * FlowShare : Status.isHolding() ? 0.12f : 0.0f;
        int Colour = Tier.getColour();
        float Red = ((Colour >> 16) & 255) / 255.0f;
        float Green = ((Colour >> 8) & 255) / 255.0f;
        float Blue = (Colour & 255) / 255.0f;
        if (!Primed) {
            Primed = true;
            ShownResonance = TargetResonance;
            Glow = TargetGlow;
            Spin = TargetSpin;
            TintRed = Red;
            TintGreen = Green;
            TintBlue = Blue;
        }
        ShownResonanceOld = ShownResonance;
        ShownResonance += (TargetResonance - ShownResonance) * 0.12f;
        GlowOld = Glow;
        Glow += (TargetGlow - Glow) * (TargetGlow > Glow ? 0.08f : 0.035f);
        Spin += (TargetSpin - Spin) * (TargetSpin > Spin ? 0.04f : 0.02f);
        TintRed += (Red - TintRed) * 0.1f;
        TintGreen += (Green - TintGreen) * 0.1f;
        TintBlue += (Blue - TintBlue) * 0.1f;
        LatticeAngleOld = LatticeAngle;
        LatticeAngle += Spin * (1.2f + 3.2f * ShownResonance);
        RingAngleOld = RingAngle;
        RingAngle += Spin * (5.0f + 9.0f * ShownResonance);
        if (LatticeAngle > LatticeWrap) {
            LatticeAngle -= LatticeWrap;
            LatticeAngleOld -= LatticeWrap;
        }
        if (RingAngle > RingWrap) {
            RingAngle -= RingWrap;
            RingAngleOld -= RingWrap;
        }
        FlashOld = Flash;
        Flash *= 0.88f;
        if (PendingSurge >= 0 && Flash < 0.5f) {
            Flash = 1.0f;
        }
    }

    @Override
    public boolean triggerEvent(int Id, int Param) {
        if (Id == SurgeEvent) {
            if (level != null && level.isClientSide) {
                PendingSurge = Param;
            }
            return true;
        }
        return super.triggerEvent(Id, Param);
    }

    public int takePendingSurge() {
        int Taken = PendingSurge;
        PendingSurge = -1;
        return Taken;
    }

    public FragmentRarity getTier() {
        return Tier;
    }

    public boolean isEnabled() {
        return Enabled;
    }

    public MatrixStatus getStatus() {
        return Status;
    }

    public float getResonance() {
        return Resonance;
    }

    public float getFlow() {
        return Flow;
    }

    public int getNearby() {
        return Nearby;
    }

    public int getDemand() {
        return Demand;
    }

    public int getLastReceived() {
        return LastReceived;
    }

    public float getShownResonance(float PartialTick) {
        return Mth.lerp(PartialTick, ShownResonanceOld, ShownResonance);
    }

    public float getGlow(float PartialTick) {
        return Mth.lerp(PartialTick, GlowOld, Glow);
    }

    public float getSpin() {
        return Spin;
    }

    public float getLatticeAngle(float PartialTick) {
        return Mth.lerp(PartialTick, LatticeAngleOld, LatticeAngle);
    }

    public float getRingAngle(float PartialTick) {
        return Mth.lerp(PartialTick, RingAngleOld, RingAngle);
    }

    public float getFlash(float PartialTick) {
        return Mth.lerp(PartialTick, FlashOld, Flash);
    }

    public float getTintRed() {
        return TintRed;
    }

    public float getTintGreen() {
        return TintGreen;
    }

    public float getTintBlue() {
        return TintBlue;
    }

    public int getClientTicks() {
        return ClientTicks;
    }

    public float getClientFlow() {
        return (float) SyncedFlow / FlowSteps;
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
        Tag.putString("Tier", Tier.getSerializedName());
        Tag.putBoolean("Enabled", Enabled);
        Tag.putFloat("Charge", Resonance);
        Tag.putBoolean("Pending", Pending);
        Tag.putInt("Formed", Formed);
        if (LastFormed != Long.MIN_VALUE) {
            Tag.putLong("LastFormed", LastFormed);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Tier")) {
            Tier = rarityNamed(Tag.getString("Tier"));
        }
        if (Tag.contains("Enabled")) {
            Enabled = Tag.getBoolean("Enabled");
        }
        if (Tag.contains("Charge")) {
            Resonance = Mth.clamp(Tag.getFloat("Charge"), 0.0f, 1.0f);
        }
        if (Tag.contains("Pending")) {
            Pending = Tag.getBoolean("Pending");
        }
        if (Tag.contains("Formed")) {
            Formed = Math.max(0, Tag.getInt("Formed"));
        }
        if (Tag.contains("LastFormed")) {
            LastFormed = Tag.getLong("LastFormed");
        }
        if (Tag.contains("Status")) {
            Status = MatrixStatus.byIndex(Tag.getByte("Status"));
        }
        if (Tag.contains("Shown")) {
            SyncedResonance = Mth.clamp(Tag.getByte("Shown"), 0, ResonanceSteps);
        }
        if (Tag.contains("Flow")) {
            SyncedFlow = Mth.clamp(Tag.getByte("Flow"), 0, FlowSteps);
        }
    }

    private static FragmentRarity rarityNamed(String Name) {
        for (FragmentRarity Rarity : FragmentRarity.values()) {
            if (Rarity.getSerializedName().equals(Name)) {
                return Rarity;
            }
        }
        return FragmentRarity.Faint;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putString("Tier", Tier.getSerializedName());
        Tag.putBoolean("Enabled", Enabled);
        Tag.putByte("Status", (byte) Status.ordinal());
        Tag.putByte("Shown", (byte) SyncedResonance);
        Tag.putByte("Flow", (byte) SyncedFlow);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.instability_matrix");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new MatrixMenu(ContainerId, PlayerInventory, this, Data);
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5) <= 64.0;
    }
}
