package net.rai.scp_unleashed.rifttech;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.matrix.MatrixRegistry;
import net.rai.scp_unleashed.rift.RiftEntity;
import net.rai.scp_unleashed.rift.RiftFormation;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class InstabilityPylonBlockEntity extends BlockEntity implements WireDevice, MenuProvider {
    public static final int SurgeEvent = 1;
    public static final int ApocalypseEvent = 2;
    public static final int DataStatus = 0;
    public static final int DataIssue = 1;
    public static final int DataIssueX = 2;
    public static final int DataIssueY = 3;
    public static final int DataIssueZ = 4;
    public static final int DataTier = 5;
    public static final int DataEnabled = 6;
    public static final int DataApocalyptic = 7;
    public static final int DataCharge = 8;
    public static final int DataApocalypticCharge = 9;
    public static final int DataIntake = 10;
    public static final int DataGridDemand = 11;
    public static final int DataSizeX = 12;
    public static final int DataSizeZ = 13;
    public static final int DataRise = 14;
    public static final int DataCorners = 15;
    public static final int DataRifts = 16;
    public static final int DataCap = 17;
    public static final int DataEta = 18;
    public static final int DataOwnReceived = 19;
    public static final int DataOwnDemand = 20;
    public static final int DataMask = 21;
    public static final int DataCornerIndex = 22;
    public static final int DataFormed = 23;
    public static final int DataCount = 24;

    private static Consumer<InstabilityPylonBlockEntity> ClientEffects = Pylon -> {
    };

    private boolean Enabled;
    private FragmentRarity Tier = FragmentRarity.Faint;
    private boolean ApocalypticMode;

    private int Received;
    private int LastReceived;
    private int Demand = GridTuning.StandbyDraw;
    private int PreviousDemand;
    private final float[] Window = new float[GridTuning.WindowTicks];
    private int WindowFilled;
    private int WindowCursor;
    private float Probe;
    private float WorkRatio;
    private int Assigned;
    private long AssignedAt = Long.MIN_VALUE / 2;

    private PylonGrid.Scan Scan = PylonGrid.Scan.failed(PylonGrid.Problem.NoPartnerX, null);
    private boolean Scanned;

    private GridStatus Status = GridStatus.Unpowered;
    private PylonGrid.Problem Issue = PylonGrid.Problem.NoPartnerX;
    @Nullable
    private BlockPos IssueAt;
    @Nullable
    private PylonGrid.Layout Grid;
    private long Charge;
    private long ApocalypticCharge;
    private int Intake;
    private int GridDemand;
    private int RiftsInside;
    private boolean ApocalypticPresent;
    private int FailedFormations;
    private long LastAttempt = Long.MIN_VALUE / 2;
    private int Formed;
    private int PoweredMask;
    private boolean WasFormed;
    private boolean CensusDue = true;

    private boolean SyncedMaster;
    @Nullable
    private PylonGrid.Layout SyncedLayout;
    private GridStatus SyncedStatus = GridStatus.Unpowered;
    private int SyncedCharge = -1;
    private int SyncedApocalyptic = -1;
    private FragmentRarity SyncedTier = FragmentRarity.Faint;
    private boolean NeedsSync;
    private boolean UrgentSync;
    private long LastSync = Long.MIN_VALUE / 2;

    private boolean ClientMaster;
    @Nullable
    private PylonGrid.Layout ClientLayout;
    private GridStatus ClientStatus = GridStatus.Unpowered;
    private FragmentRarity ClientTier = FragmentRarity.Faint;
    private float ClientCharge;
    private float ClientApocalyptic;
    private float Glow;
    private float GlowOld;
    private float Flash;
    private float FlashOld;
    private int ClientTicks;
    private int PendingSurge = -1;
    private boolean PendingApocalypse;
    @Nullable
    private Vec3 SurgeSpot;
    private boolean SurgeGolden;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            InstabilityPylonBlockEntity Control = controller();
            PylonGrid.Layout Shown = Control.Grid != null ? Control.Grid : Scan.Found();
            return switch (Index) {
                case DataStatus -> viewStatus().ordinal();
                case DataIssue -> viewIssue().ordinal();
                case DataIssueX -> issueOffset(0);
                case DataIssueY -> issueOffset(1);
                case DataIssueZ -> issueOffset(2);
                case DataTier -> Control.Tier.getTier();
                case DataEnabled -> Control.Enabled ? 1 : 0;
                case DataApocalyptic -> Control.ApocalypticMode ? 1 : 0;
                case DataCharge -> Control.chargeScaled();
                case DataApocalypticCharge -> Control.apocalypticScaled();
                case DataIntake -> Math.min(Short.MAX_VALUE, Control.Grid != null ? Control.Intake : 0);
                case DataGridDemand -> Math.min(Short.MAX_VALUE, Control.Grid != null ? Control.GridDemand : 0);
                case DataSizeX -> Shown == null ? 0 : Shown.SizeX();
                case DataSizeZ -> Shown == null ? 0 : Shown.SizeZ();
                case DataRise -> Shown == null ? 0 : Shown.Rise();
                case DataCorners -> Shown == null ? 0 : Shown.corners().size();
                case DataRifts -> Control.Grid != null ? Control.RiftsInside : 0;
                case DataCap -> Shown == null ? 0 : Shown.cap();
                case DataEta -> Control.etaSeconds();
                case DataOwnReceived -> Math.min(Short.MAX_VALUE, LastReceived);
                case DataOwnDemand -> Math.min(Short.MAX_VALUE, Demand);
                case DataMask -> Control.PoweredMask;
                case DataCornerIndex -> Shown == null ? -1 : Shown.cornerIndex(worldPosition);
                case DataFormed -> Math.min(Short.MAX_VALUE, Control.Formed);
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

    public InstabilityPylonBlockEntity(BlockPos Pos, BlockState State) {
        super(RiftTechRegistry.InstabilityPylonEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<InstabilityPylonBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, InstabilityPylonBlockEntity Pylon) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof InstabilityPylonBlock) {
            Pylon.tickServer(ServerWorld);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, InstabilityPylonBlockEntity Pylon) {
        Pylon.tickClient();
        ClientEffects.accept(Pylon);
    }

    public Vec3 getEmitter() {
        return emitterOf(worldPosition);
    }

    public static Vec3 emitterOf(BlockPos Base) {
        return new Vec3(Base.getX() + 0.5, Base.getY() + 2.0 + 8.0 / 16.0, Base.getZ() + 0.5);
    }

    private int scanPhase() {
        return Math.floorMod(worldPosition.hashCode(), GridTuning.ScanTicks);
    }

    private void tickServer(ServerLevel World) {
        long Now = World.getGameTime();
        int Delivered = Received;
        Received = 0;
        LastReceived = Delivered;
        WorkRatio = PreviousDemand > 0 ? Math.min(1.0f, (float) Delivered / PreviousDemand) : 0.0f;
        Window[WindowCursor] = Math.min(1.0f, (float) Delivered / GridTuning.StandbyDraw);
        WindowCursor = (WindowCursor + 1) % Window.length;
        WindowFilled = Math.min(Window.length, WindowFilled + 1);
        Probe = rollingProbe();
        if (!Scanned || (Now + scanPhase()) % GridTuning.ScanTicks == 0) {
            Scanned = true;
            Scan = PylonGrid.scan(World, worldPosition);
        }
        if (!isPowered()) {
            if (Grid != null) {
                breakGrid(World);
            }
            Status = GridStatus.Unpowered;
            Issue = PylonGrid.Problem.None;
            IssueAt = null;
        } else if (Scan.Found() == null) {
            if (Grid != null) {
                breakGrid(World);
            }
            Status = GridStatus.Searching;
            Issue = Scan.Issue();
            IssueAt = Scan.Where();
        } else if (Scan.Found().Anchor().equals(worldPosition)) {
            tickMaster(World, Now, Scan.Found());
        } else {
            if (Grid != null) {
                breakGrid(World);
            }
            Status = GridStatus.Searching;
            Issue = PylonGrid.Problem.None;
            IssueAt = null;
        }
        Demand = Now - AssignedAt <= GridTuning.AssignHold ? Math.max(1, Assigned) : GridTuning.StandbyDraw;
        PreviousDemand = Demand;
        GridStatus Shown = viewStatus();
        InstabilityPylonBlock.setMode(World, worldPosition, Shown == GridStatus.Unpowered ? PylonMode.Off : Shown.getMode());
        updateSync(World, Now);
    }

    private float rollingProbe() {
        if (WindowFilled == 0) {
            return 0.0f;
        }
        float Sum = 0.0f;
        for (int Index = 0; Index < WindowFilled; Index++) {
            Sum += Window[Index];
        }
        return Sum / WindowFilled;
    }

    public boolean isPowered() {
        return Probe >= GridTuning.PoweredRatio;
    }

    @Nullable
    public PylonGrid.Layout getScannedLayout() {
        return Scan.Found();
    }

    @Nullable
    public PylonGrid.Layout getGrid() {
        return Grid;
    }

    void assign(int Share, long Now) {
        Assigned = Share;
        AssignedAt = Now;
    }

    private void breakGrid(ServerLevel World) {
        if (WasFormed) {
            Vec3 Emitter = getEmitter();
            World.playSound(null, Emitter.x, Emitter.y, Emitter.z, RiftTechRegistry.PylonUnlink.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        WasFormed = false;
        Grid = null;
        PoweredMask = 0;
        Intake = 0;
        GridDemand = 0;
        CensusDue = true;
        UrgentSync = true;
        NeedsSync = true;
    }

    private void tickMaster(ServerLevel World, long Now, PylonGrid.Layout Layout) {
        List<BlockPos> Corners = Layout.corners();
        InstabilityPylonBlockEntity[] Members = new InstabilityPylonBlockEntity[Corners.size()];
        PylonGrid.Problem Found = PylonGrid.Problem.None;
        BlockPos Where = null;
        int Mask = 0;
        for (int Index = 0; Index < Corners.size(); Index++) {
            BlockPos Corner = Corners.get(Index);
            InstabilityPylonBlockEntity Member = Corner.equals(worldPosition) ? this : World.isLoaded(Corner) && World.getBlockEntity(Corner) instanceof InstabilityPylonBlockEntity Other ? Other : null;
            if (Member == null) {
                if (Found == PylonGrid.Problem.None) {
                    Found = World.isLoaded(Corner) ? PylonGrid.Problem.CornerMissing : PylonGrid.Problem.Unloaded;
                    Where = Corner;
                }
                continue;
            }
            Members[Index] = Member;
            if (Member.isPowered()) {
                Mask |= 1 << Index;
            }
            if (!Layout.equals(Member.getScannedLayout())) {
                if (Found == PylonGrid.Problem.None || Found == PylonGrid.Problem.CornerUnpowered) {
                    Found = PylonGrid.Problem.Conflict;
                    Where = Corner;
                }
            } else if (!Member.isPowered() && Found == PylonGrid.Problem.None) {
                Found = PylonGrid.Problem.CornerUnpowered;
                Where = Corner;
            }
        }
        PoweredMask = Mask;
        if (Found != PylonGrid.Problem.None) {
            if (Grid != null) {
                breakGrid(World);
            }
            PoweredMask = Mask;
            Status = Found == PylonGrid.Problem.CornerUnpowered ? GridStatus.CornerUnpowered : Found == PylonGrid.Problem.Conflict ? GridStatus.Conflict : GridStatus.Searching;
            Issue = Found;
            IssueAt = Where;
            return;
        }
        Issue = PylonGrid.Problem.None;
        IssueAt = null;
        if (Grid == null || !Grid.equals(Layout)) {
            Grid = Layout;
            CensusDue = true;
            UrgentSync = true;
            NeedsSync = true;
            if (!WasFormed) {
                WasFormed = true;
                Vec3 Centre = Layout.centre();
                World.playSound(null, Centre.x, Centre.y, Centre.z, RiftTechRegistry.PylonLink.get(), SoundSource.BLOCKS, 1.4f, 1.0f);
            }
            for (InstabilityPylonBlockEntity Member : Members) {
                Member.adopt(Enabled, Tier, ApocalypticMode);
            }
        }
        if (CensusDue || (Now + scanPhase()) % GridTuning.CensusTicks == 0) {
            census(World);
        }
        boolean Box = Layout.isBox();
        int Wanted;
        if (!Enabled) {
            Status = GridStatus.Offline;
            Wanted = 0;
        } else if (!RiftFormation.allowedIn(World)) {
            Status = GridStatus.Dormant;
            Wanted = 0;
        } else if (ApocalypticMode) {
            boolean Full = ApocalypticCharge >= GridTuning.ApocalypticCost;
            Wanted = ApocalypticPresent || Full ? 0 : GridTuning.ApocalypticDraw;
            if (ApocalypticPresent) {
                Status = GridStatus.Holding;
            } else if (Full) {
                Status = FailedFormations > 0 ? GridStatus.Obstructed : GridStatus.Apocalyptic;
                if (Now - LastAttempt >= (FailedFormations > 0 ? GridTuning.CensusTicks : 1)) {
                    LastAttempt = Now;
                    formApocalyptic(World, Layout);
                }
            }
        } else if (RiftsInside >= Layout.cap()) {
            Status = GridStatus.Saturated;
            Wanted = 0;
        } else if (FailedFormations >= GridTuning.ObstructedAfter && Charge >= GridTuning.costFor(Tier, Box)) {
            Status = GridStatus.Obstructed;
            Wanted = 0;
        } else {
            Wanted = GridTuning.drawFor(Tier, Box);
        }
        int Share = Wanted > 0 ? Math.max(GridTuning.StandbyDraw, (Wanted + Members.length - 1) / Members.length) : GridTuning.StandbyDraw;
        int Total = 0;
        float Weakest = 1.0f;
        for (InstabilityPylonBlockEntity Member : Members) {
            Member.assign(Share, Now);
            Total += Member.LastReceived;
            Weakest = Math.min(Weakest, Member.WorkRatio);
        }
        Intake = Total;
        GridDemand = Share * Members.length;
        if (Wanted <= 0) {
            if (!ApocalypticMode && Status == GridStatus.Obstructed && Now - LastAttempt >= GridTuning.CensusTicks) {
                LastAttempt = Now;
                formRift(World, Layout);
            }
            return;
        }
        if (ApocalypticMode) {
            if (Total >= GridTuning.ApocalypticMinIntake && Weakest >= GridTuning.CornerMinRatio) {
                ApocalypticCharge = Math.min(GridTuning.ApocalypticCost, ApocalypticCharge + Math.min(Total, GridTuning.ApocalypticDraw));
                Status = GridStatus.Apocalyptic;
            } else {
                Status = GridStatus.Starved;
            }
            return;
        }
        long Cost = GridTuning.costFor(Tier, Box);
        if (Weakest >= GridTuning.CornerMinRatio) {
            Charge = Math.min(Cost, Charge + Math.min(Total, Wanted));
            Status = Weakest < GridTuning.FullRatio || Total < Wanted * GridTuning.FullRatio ? GridStatus.LowPower : GridStatus.Charging;
        } else {
            Status = GridStatus.LowPower;
        }
        if (Charge >= Cost && Now - LastAttempt >= 1) {
            LastAttempt = Now;
            formRift(World, Layout);
        }
    }

    private void census(ServerLevel World) {
        CensusDue = false;
        if (Grid == null) {
            return;
        }
        List<RiftEntity> Inside = GridField.riftsInside(World, Grid);
        RiftsInside = Inside.size();
        ApocalypticPresent = Inside.stream().anyMatch(Rift -> Rift instanceof ApocalypticRiftEntity);
    }

    private void formRift(ServerLevel World, PylonGrid.Layout Layout) {
        long Cost = GridTuning.costFor(Tier, Layout.isBox());
        Vec3 Spot = GridField.findSpot(World, Layout, RiftEntity.heightOf(Tier), false, World.random);
        RiftEntity Rift = Spot == null ? null : GridField.open(World, Spot, Tier, World.random);
        if (Rift == null) {
            FailedFormations = Math.min(FailedFormations + 1, 99);
            return;
        }
        FailedFormations = 0;
        Charge = Math.max(0L, Charge - Cost);
        Formed++;
        RiftsInside++;
        setChanged();
        int Grade = Tier.getTier();
        float Deep = 1.08f - 0.06f * Grade;
        double Middle = Spot.y + RiftEntity.heightOf(Tier) * 0.5;
        World.playSound(null, Spot.x, Middle, Spot.z, MatrixRegistry.RiftTear.get(), SoundSource.AMBIENT, 2.0f, Deep);
        World.playSound(null, Spot.x, Middle, Spot.z, MatrixRegistry.RiftTearBoom.get(), SoundSource.AMBIENT, 2.0f, Deep + 0.04f);
        World.blockEvent(worldPosition, getBlockState().getBlock(), SurgeEvent, packOffset(Spot.x, Middle, Spot.z) | (Grade << 21));
    }

    private void formApocalyptic(ServerLevel World, PylonGrid.Layout Layout) {
        Vec3 Spot = GridField.findSpot(World, Layout, ApocalypticRiftEntity.Height, true, World.random);
        ApocalypticRiftEntity Rift = Spot == null ? null : GridField.openApocalyptic(World, Spot, World.random);
        if (Rift == null) {
            FailedFormations = Math.min(FailedFormations + 1, 99);
            Status = GridStatus.Obstructed;
            return;
        }
        FailedFormations = 0;
        ApocalypticCharge = 0L;
        ApocalypticPresent = true;
        Formed++;
        RiftsInside++;
        setChanged();
        double Middle = Spot.y + ApocalypticRiftEntity.Height * 0.5;
        World.playSound(null, Spot.x, Middle, Spot.z, RiftTechRegistry.ApocalypticBoom.get(), SoundSource.AMBIENT, 4.0f, 1.0f);
        World.playSound(null, Spot.x, Middle, Spot.z, RiftTechRegistry.ApocalypticBoomTail.get(), SoundSource.AMBIENT, 4.0f, 1.0f);
        World.playSound(null, Spot.x, Middle, Spot.z, RiftTechRegistry.ApocalypticBoomPortal.get(), SoundSource.AMBIENT, 4.0f, 1.0f);
        World.blockEvent(worldPosition, getBlockState().getBlock(), ApocalypseEvent, packOffset(Spot.x, Middle, Spot.z));
        NeoForge.EVENT_BUS.post(new ApocalypticRiftEvent.Formed(World, new Vec3(Spot.x, Middle, Spot.z), Rift, worldPosition));
    }

    private int packOffset(double X, double Y, double Z) {
        int DeltaX = Mth.clamp((int) Math.round((X - worldPosition.getX()) * 2.0), -63, 63) + 63;
        int DeltaY = Mth.clamp((int) Math.round((Y - worldPosition.getY()) * 2.0), -63, 63) + 63;
        int DeltaZ = Mth.clamp((int) Math.round((Z - worldPosition.getZ()) * 2.0), -63, 63) + 63;
        return DeltaX | (DeltaY << 7) | (DeltaZ << 14);
    }

    public static Vec3 unpackOffset(int Param) {
        return new Vec3(((Param & 127) - 63) * 0.5, (((Param >> 7) & 127) - 63) * 0.5, (((Param >> 14) & 127) - 63) * 0.5);
    }

    public static FragmentRarity surgeTier(int Param) {
        return FragmentRarity.byTier((Param >> 21) & 7);
    }

    private void adopt(boolean NextEnabled, FragmentRarity NextTier, boolean NextApocalyptic) {
        if (Enabled != NextEnabled || Tier != NextTier || ApocalypticMode != NextApocalyptic) {
            Enabled = NextEnabled;
            Tier = NextTier;
            ApocalypticMode = NextApocalyptic;
            setChanged();
        }
    }

    public InstabilityPylonBlockEntity controller() {
        PylonGrid.Layout Found = Scan.Found();
        if (Found == null || level == null || Found.Anchor().equals(worldPosition)) {
            return this;
        }
        if (level.isLoaded(Found.Anchor()) && level.getBlockEntity(Found.Anchor()) instanceof InstabilityPylonBlockEntity Master && Found.equals(Master.getScannedLayout())) {
            return Master;
        }
        return this;
    }

    public GridStatus viewStatus() {
        if (!isPowered()) {
            return GridStatus.Unpowered;
        }
        if (Scan.Found() == null) {
            return GridStatus.Searching;
        }
        InstabilityPylonBlockEntity Control = controller();
        if (Control == this) {
            return Scan.Found().Anchor().equals(worldPosition) ? Status : GridStatus.Conflict;
        }
        return Control.Status == GridStatus.Unpowered ? GridStatus.CornerUnpowered : Control.Status;
    }

    public PylonGrid.Problem viewIssue() {
        if (!isPowered()) {
            return PylonGrid.Problem.None;
        }
        if (Scan.Found() == null) {
            return Scan.Issue();
        }
        InstabilityPylonBlockEntity Control = controller();
        if (Control == this && !Scan.Found().Anchor().equals(worldPosition)) {
            return PylonGrid.Problem.Conflict;
        }
        if (Control != this && Control.Status == GridStatus.Unpowered) {
            return PylonGrid.Problem.CornerUnpowered;
        }
        return Control.Issue;
    }

    @Nullable
    public BlockPos viewIssueAt() {
        if (!isPowered()) {
            return null;
        }
        if (Scan.Found() == null) {
            return Scan.Where();
        }
        InstabilityPylonBlockEntity Control = controller();
        if (Control == this && !Scan.Found().Anchor().equals(worldPosition)) {
            return Scan.Found().Anchor();
        }
        if (Control != this && Control.Status == GridStatus.Unpowered) {
            return Control.getBlockPos();
        }
        return Control.IssueAt;
    }

    private int issueOffset(int Axis) {
        BlockPos Where = viewIssueAt();
        if (Where == null) {
            return Short.MIN_VALUE;
        }
        int Delta = Axis == 0 ? Where.getX() - worldPosition.getX() : Axis == 1 ? Where.getY() - worldPosition.getY() : Where.getZ() - worldPosition.getZ();
        return Mth.clamp(Delta, -9999, 9999);
    }

    public int chargeScaled() {
        long Cost = GridTuning.costFor(Tier, Grid != null && Grid.isBox());
        return (int) Math.min(GridTuning.Scale, Charge * GridTuning.Scale / Math.max(1L, Cost));
    }

    public int apocalypticScaled() {
        return (int) Math.min(GridTuning.Scale, ApocalypticCharge * GridTuning.Scale / GridTuning.ApocalypticCost);
    }

    public int etaSeconds() {
        if (Grid == null || !Status.isCharging()) {
            return -1;
        }
        int Rate = Math.max(0, ApocalypticMode ? Math.min(Intake, GridTuning.ApocalypticDraw) : Math.min(Intake, GridDemand));
        long Remaining = ApocalypticMode ? GridTuning.ApocalypticCost - ApocalypticCharge : GridTuning.costFor(Tier, Grid.isBox()) - Charge;
        return GridTuning.etaSeconds(Math.max(0L, Remaining), Rate);
    }

    public void selectTier(FragmentRarity Next) {
        InstabilityPylonBlockEntity Control = controller();
        if (Control != this) {
            Control.selectTier(Next);
            return;
        }
        boolean Changed = Tier != Next || ApocalypticMode;
        Tier = Next;
        ApocalypticMode = false;
        if (Changed) {
            FailedFormations = 0;
            settingsChanged();
            int Grade = Next.getTier();
            play(MatrixRegistry.TierClick.get(), 0.5f, 1.3f - 0.07f * Grade);
            play(MatrixRegistry.TierChime.get(), 1.0f, 1.34f - 0.11f * Grade);
        }
    }

    public void toggleEnabled() {
        InstabilityPylonBlockEntity Control = controller();
        if (Control != this) {
            Control.toggleEnabled();
            return;
        }
        Enabled = !Enabled;
        settingsChanged();
        play(Enabled ? MatrixRegistry.Start.get() : MatrixRegistry.Stop.get(), 0.8f, 0.9f);
    }

    public void toggleApocalyptic() {
        InstabilityPylonBlockEntity Control = controller();
        if (Control != this) {
            Control.toggleApocalyptic();
            return;
        }
        ApocalypticMode = !ApocalypticMode;
        FailedFormations = 0;
        settingsChanged();
        play(ApocalypticMode ? RiftTechRegistry.PylonLink.get() : RiftTechRegistry.PylonUnlink.get(), 1.0f, ApocalypticMode ? 0.7f : 1.1f);
    }

    private void settingsChanged() {
        setChanged();
        CensusDue = true;
        UrgentSync = true;
        NeedsSync = true;
        if (Grid != null && level != null) {
            for (BlockPos Corner : Grid.corners()) {
                if (!Corner.equals(worldPosition) && level.isLoaded(Corner) && level.getBlockEntity(Corner) instanceof InstabilityPylonBlockEntity Member) {
                    Member.adopt(Enabled, Tier, ApocalypticMode);
                }
            }
        }
    }

    private void play(SoundEvent Sound, float Volume, float Pitch) {
        if (level != null) {
            Vec3 Emitter = getEmitter();
            level.playSound(null, Emitter.x, Emitter.y, Emitter.z, Sound, SoundSource.BLOCKS, Volume, Pitch);
        }
    }

    public void setCharge(long Value) {
        Charge = Math.max(0L, Value);
        setChanged();
    }

    public void setApocalypticCharge(long Value) {
        ApocalypticCharge = Mth.clamp(Value, 0L, GridTuning.ApocalypticCost);
        setChanged();
    }

    private void updateSync(ServerLevel World, long Now) {
        boolean Master = Grid != null;
        if (Master != SyncedMaster || !Objects.equals(Grid, SyncedLayout)) {
            SyncedMaster = Master;
            SyncedLayout = Grid;
            NeedsSync = true;
            UrgentSync = true;
        }
        if (Status != SyncedStatus) {
            UrgentSync |= Status.getMode() != SyncedStatus.getMode();
            SyncedStatus = Status;
            NeedsSync = true;
        }
        if (Tier != SyncedTier) {
            SyncedTier = Tier;
            NeedsSync = true;
        }
        int ChargeLevel = chargeScaled() / 100;
        if (ChargeLevel != SyncedCharge) {
            SyncedCharge = ChargeLevel;
            NeedsSync = true;
        }
        int ApocalypticLevel = apocalypticScaled() / 50;
        if (ApocalypticLevel != SyncedApocalyptic) {
            SyncedApocalyptic = ApocalypticLevel;
            NeedsSync = true;
        }
        if (NeedsSync && (UrgentSync || Now - LastSync >= GridTuning.SyncTicks)) {
            NeedsSync = false;
            UrgentSync = false;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void tickClient() {
        ClientTicks++;
        GlowOld = Glow;
        FlashOld = Flash;
        float Target = !ClientMaster ? 0.0f : ClientStatus.isCharging() ? 0.55f + 0.45f * (ClientStatus == GridStatus.Apocalyptic ? ClientApocalyptic : ClientCharge)
                : ClientStatus.isFormed() ? 0.18f : 0.0f;
        Glow += (Target - Glow) * (Target > Glow ? 0.06f : 0.03f);
        Flash *= 0.86f;
        if (PendingApocalypse || PendingSurge >= 0) {
            Flash = 1.0f;
        }
    }

    @Override
    public boolean triggerEvent(int Id, int Param) {
        if (Id == SurgeEvent || Id == ApocalypseEvent) {
            if (level != null && level.isClientSide) {
                if (Id == ApocalypseEvent) {
                    PendingApocalypse = true;
                }
                PendingSurge = Param;
                SurgeSpot = Vec3.atLowerCornerOf(worldPosition).add(unpackOffset(Param));
                SurgeGolden = Id == ApocalypseEvent;
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

    public boolean takePendingApocalypse() {
        boolean Taken = PendingApocalypse;
        PendingApocalypse = false;
        return Taken;
    }

    @Nullable
    public Vec3 getSurgeSpot() {
        return SurgeSpot;
    }

    public boolean isSurgeGolden() {
        return SurgeGolden;
    }

    public boolean isClientMaster() {
        return ClientMaster && ClientLayout != null;
    }

    @Nullable
    public PylonGrid.Layout getClientLayout() {
        return ClientLayout;
    }

    public GridStatus getClientStatus() {
        return ClientStatus;
    }

    public FragmentRarity getClientTier() {
        return ClientTier;
    }

    public float getClientCharge() {
        return ClientCharge;
    }

    public float getClientApocalyptic() {
        return ClientApocalyptic;
    }

    public float getGlow(float PartialTick) {
        return Mth.lerp(PartialTick, GlowOld, Glow);
    }

    public float getFlash(float PartialTick) {
        return Mth.lerp(PartialTick, FlashOld, Flash);
    }

    public int getClientTicks() {
        return ClientTicks;
    }

    public AABB getRenderBounds() {
        return ClientLayout == null ? new AABB(worldPosition).expandTowards(0.0, 2.0, 0.0) : ClientLayout.bounds().inflate(1.0);
    }

    public GridStatus getStatus() {
        return Status;
    }

    public FragmentRarity getTier() {
        return Tier;
    }

    public boolean isEnabled() {
        return Enabled;
    }

    public boolean isApocalypticMode() {
        return ApocalypticMode;
    }

    public long getCharge() {
        return Charge;
    }

    public long getApocalypticCharge() {
        return ApocalypticCharge;
    }

    public int getIntake() {
        return Intake;
    }

    public int getGridDemand() {
        return GridDemand;
    }

    public int getRiftsInside() {
        return RiftsInside;
    }

    public int getFormed() {
        return Formed;
    }

    public int getLastReceived() {
        return LastReceived;
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
        Tag.putBoolean("Enabled", Enabled);
        Tag.putString("Tier", Tier.getSerializedName());
        Tag.putBoolean("ApocalypticMode", ApocalypticMode);
        Tag.putLong("Charge", Charge);
        Tag.putLong("ApocalypticCharge", ApocalypticCharge);
        Tag.putInt("Formed", Formed);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Enabled")) {
            Enabled = Tag.getBoolean("Enabled");
        }
        if (Tag.contains("Tier")) {
            String Name = Tag.getString("Tier");
            for (FragmentRarity Rarity : FragmentRarity.values()) {
                if (Rarity.getSerializedName().equals(Name)) {
                    Tier = Rarity;
                }
            }
        }
        if (Tag.contains("ApocalypticMode")) {
            ApocalypticMode = Tag.getBoolean("ApocalypticMode");
        }
        if (Tag.contains("Charge")) {
            Charge = Math.max(0L, Tag.getLong("Charge"));
        }
        if (Tag.contains("ApocalypticCharge")) {
            ApocalypticCharge = Mth.clamp(Tag.getLong("ApocalypticCharge"), 0L, GridTuning.ApocalypticCost);
        }
        if (Tag.contains("Formed")) {
            Formed = Math.max(0, Tag.getInt("Formed"));
        }
        if (Tag.contains("Status")) {
            ClientStatus = GridStatus.byIndex(Tag.getByte("Status"));
            ClientMaster = Tag.getBoolean("Master");
            ClientLayout = Tag.contains("Layout") ? PylonGrid.Layout.load(Tag.getCompound("Layout")) : null;
            ClientTier = Tier;
            ClientCharge = Tag.getShort("ChargeLevel") / (float) GridTuning.Scale;
            ClientApocalyptic = Tag.getShort("ApocalypticLevel") / (float) GridTuning.Scale;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Status", (byte) Status.ordinal());
        Tag.putBoolean("Master", Grid != null);
        if (Grid != null) {
            Tag.put("Layout", Grid.save());
        }
        Tag.putString("Tier", Tier.getSerializedName());
        Tag.putBoolean("ApocalypticMode", ApocalypticMode);
        Tag.putShort("ChargeLevel", (short) chargeScaled());
        Tag.putShort("ApocalypticLevel", (short) apocalypticScaled());
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.instability_pylon");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new PylonMenu(ContainerId, PlayerInventory, this, Data);
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5) <= 64.0;
    }
}
