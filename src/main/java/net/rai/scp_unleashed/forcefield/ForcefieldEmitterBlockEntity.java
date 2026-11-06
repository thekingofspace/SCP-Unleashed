package net.rai.scp_unleashed.forcefield;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.forcefield.network.ForcefieldHitPayload;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ForcefieldEmitterBlockEntity extends BlockEntity implements WireDevice {
    public static final int MaxGap = 8;
    public static final int MaxColumns = 24;
    public static final int BootTicks = 200;
    public static final int BootSteps = 5;
    public static final int CollapseTicks = 14;
    public static final float IntegrityPerCell = 25.0f;
    public static final int RechargeDelay = 100;
    public static final float RechargePerCell = 0.025f;
    public static final int BaseDraw = 4;
    public static final int CellDraw = 4;
    public static final int FaultDraw = 1;
    public static final int PowerGrace = 3;
    public static final int LoadGrace = 40;
    public static final int ScanTicks = 20;
    public static final int CheckTicks = 10;
    public static final int SyncTicks = 4;
    public static final int HumMin = 70;
    public static final int HumMax = 100;
    public static final double HumReach = 16.0;
    public static final double EffectRange = 48.0;
    public static final int ZapGap = 3;
    public static final float CollapseMargin = 0.5f;
    public static final double PushMargin = 0.02;

    private ForcefieldPhase Phase = ForcefieldPhase.Offline;
    private int PhaseTicks;
    private boolean Faulted;
    private float Integrity;
    private int Height;
    private boolean RedstoneHigh;

    @Nullable
    private BlockPos LeaderPos;
    private final List<BlockPos> Members = new ArrayList<>();
    private boolean ScanDirty = true;
    private long ScanDue = Long.MIN_VALUE / 2;
    private int Gap;
    @Nullable
    private BlockPos PartnerPos;
    private boolean PairDirty = true;
    private long PairDue = Long.MIN_VALUE / 2;
    private int Received;
    private long ReceivedTick = Long.MIN_VALUE / 2;
    private long Observed = Long.MIN_VALUE / 2;
    private int Share;
    private long FedAt = Long.MIN_VALUE / 2;
    private long LastFed = Long.MIN_VALUE / 2;
    private int WallDemand;
    private int WallReceived;
    private long LastHit = Long.MIN_VALUE / 2;
    private long LastZap = Long.MIN_VALUE / 2;
    private int HumTimer = HumMin;
    private boolean Loaded;
    private boolean NeedsSync;
    private boolean UrgentSync;
    private long LastSync = Long.MIN_VALUE / 2;
    private float SyncedFraction = -1.0f;
    private float ClientFraction = 1.0f;

    public ForcefieldEmitterBlockEntity(BlockPos Pos, BlockState State) {
        super(ForcefieldRegistry.EmitterEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, ForcefieldEmitterBlockEntity Emitter) {
        if (World instanceof ServerLevel Server && ForcefieldEmitterBlock.isBottom(State)) {
            Emitter.tickServer(Server);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, ForcefieldEmitterBlockEntity Emitter) {
        Emitter.PhaseTicks++;
    }

    public static int columnDraw(int Cells) {
        return Cells <= 0 ? 0 : BaseDraw + CellDraw * Cells;
    }

    public static float columnIntegrity(int Cells) {
        return IntegrityPerCell * Cells;
    }

    public Direction.Axis axis() {
        BlockState State = getBlockState();
        return State.hasProperty(ForcefieldEmitterBlock.Axis) ? State.getValue(ForcefieldEmitterBlock.Axis) : Direction.Axis.X;
    }

    private Direction forward() {
        return axis() == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
    }

    private void tickServer(ServerLevel World) {
        long Now = World.getGameTime();
        if (!Loaded) {
            Loaded = true;
            if (Phase != ForcefieldPhase.Offline) {
                FedAt = Now + LoadGrace;
                LastFed = Now + LoadGrace;
            }
            RedstoneHigh = World.hasNeighborSignal(worldPosition);
        }
        if (ScanDirty || Now >= ScanDue) {
            ScanDirty = false;
            ScanDue = Now + ScanTicks;
            rescanLeader(World);
            if (isLeader()) {
                rescanMembers(World);
            }
        }
        if (isLeader()) {
            tickWall(World, Now);
        }
        if (PairDirty || Now >= PairDue) {
            refreshPair(World, Now);
        }
        tickColumn(World, Now);
        updateGlow(World);
        float Fraction = fraction();
        if (Math.abs(Fraction - SyncedFraction) >= 0.01f) {
            NeedsSync = true;
        }
        if (NeedsSync && (UrgentSync || Now - LastSync >= SyncTicks)) {
            NeedsSync = false;
            UrgentSync = false;
            LastSync = Now;
            SyncedFraction = Fraction;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private boolean lineAt(Level World, BlockPos Pos) {
        if (!World.isLoaded(Pos)) {
            return false;
        }
        BlockState State = World.getBlockState(Pos);
        return ForcefieldEmitterBlock.isBottom(State) && State.getValue(ForcefieldEmitterBlock.Axis) == axis();
    }

    private void rescanLeader(Level World) {
        Direction Back = forward().getOpposite();
        BlockPos Cursor = worldPosition;
        for (int Step = 1; Step < MaxColumns; Step++) {
            BlockPos Next = Cursor.relative(Back);
            if (!lineAt(World, Next)) {
                LeaderPos = Cursor.immutable();
                return;
            }
            Cursor = Next;
        }
        LeaderPos = lineAt(World, Cursor.relative(Back)) ? null : Cursor.immutable();
    }

    private void rescanMembers(Level World) {
        Members.clear();
        BlockPos Cursor = worldPosition;
        for (int Index = 0; Index < MaxColumns; Index++) {
            if (!lineAt(World, Cursor) || !(World.getBlockEntity(Cursor) instanceof ForcefieldEmitterBlockEntity Column)) {
                break;
            }
            Members.add(Cursor.immutable());
            Column.LeaderPos = worldPosition;
            Cursor = Cursor.relative(forward());
        }
    }

    public boolean isLeader() {
        return worldPosition.equals(LeaderPos);
    }

    @Nullable
    public ForcefieldEmitterBlockEntity leader() {
        if (LeaderPos == null || level == null) {
            return null;
        }
        if (isLeader()) {
            return this;
        }
        if (level.isLoaded(LeaderPos) && level.getBlockEntity(LeaderPos) instanceof ForcefieldEmitterBlockEntity Head && Head.isLeader()) {
            return Head;
        }
        return null;
    }

    public List<ForcefieldEmitterBlockEntity> wall() {
        ForcefieldEmitterBlockEntity Head = leader();
        if (Head == null || level == null) {
            return List.of(this);
        }
        List<ForcefieldEmitterBlockEntity> Found = new ArrayList<>(Head.Members.size());
        for (BlockPos Pos : Head.Members) {
            if (level.isLoaded(Pos) && level.getBlockEntity(Pos) instanceof ForcefieldEmitterBlockEntity Column && !Column.isRemoved()) {
                Found.add(Column);
            }
        }
        if (!Found.contains(this)) {
            Found.add(this);
        }
        return Found;
    }

    private void tickWall(ServerLevel World, long Now) {
        List<ForcefieldEmitterBlockEntity> Wall = wall();
        int Want = 0;
        int Got = 0;
        int Linked = 0;
        boolean AnyOnline = false;
        Vec3 Middle = Vec3.ZERO;
        for (ForcefieldEmitterBlockEntity Column : Wall) {
            Want += Column.need();
            Got += Column.ReceivedTick >= Now - 1 ? Column.Received : 0;
            if (Column.Observed >= Now - 1) {
                Linked++;
            }
            if (Column.Phase == ForcefieldPhase.Online) {
                AnyOnline = true;
            }
            Middle = Middle.add(Column.fieldCentre());
        }
        if (Want > 0 && Got >= Want) {
            LastFed = Now;
        }
        boolean Powered = Want > 0 && Now - LastFed <= PowerGrace;
        int Each = Linked > 0 ? Mth.positiveCeilDiv(Want, Linked) : 0;
        for (ForcefieldEmitterBlockEntity Column : Wall) {
            Column.Share = Column.Observed >= Now - 1 ? Each : 0;
            if (Powered) {
                Column.FedAt = Math.max(Column.FedAt, Now);
            }
        }
        WallDemand = Want;
        WallReceived = Got;
        if (AnyOnline && --HumTimer <= 0) {
            HumTimer = Mth.nextInt(World.getRandom(), HumMin, HumMax);
            Vec3 Centre = Middle.scale(1.0 / Wall.size());
            if (World.getNearestPlayer(Centre.x, Centre.y, Centre.z, HumReach, false) != null) {
                ForcefieldSounds.play(World, Centre, ForcefieldRegistry.HumLayers, 0.7f, 1.0f);
            }
        }
    }

    private int need() {
        if (Gap <= 0 || LeaderPos == null) {
            return 0;
        }
        if (Phase == ForcefieldPhase.Offline && Faulted) {
            return FaultDraw;
        }
        return columnDraw(Phase.raised() ? Math.max(Height, Gap) : Gap);
    }

    private static boolean passable(BlockState State) {
        return State.isAir() || State.is(ForcefieldRegistry.Barrier.get()) || (State.canBeReplaced() && State.getFluidState().isEmpty());
    }

    private void refreshPair(ServerLevel World, long Now) {
        PairDirty = false;
        PairDue = Now + ScanTicks;
        int Found = 0;
        BlockPos Partner = null;
        for (int Step = 1; Step <= MaxGap + 1; Step++) {
            BlockPos Cell = worldPosition.above(Step);
            BlockState State = World.getBlockState(Cell);
            if (State.getBlock() instanceof ForcefieldEmitterBlock) {
                if (State.getValue(ForcefieldEmitterBlock.HalfPart) == Half.TOP && State.getValue(ForcefieldEmitterBlock.Axis) == axis() && Step > 1) {
                    Found = Step - 1;
                    Partner = Cell.immutable();
                }
                break;
            }
            if (!passable(State)) {
                break;
            }
        }
        if (Partner == null ? PartnerPos != null : !Partner.equals(PartnerPos)) {
            dimPartner(World);
            PartnerPos = Partner;
            NeedsSync = true;
        }
        if (Found != Gap) {
            Gap = Found;
            NeedsSync = true;
        }
    }

    private void dimPartner(ServerLevel World) {
        if (PartnerPos == null || !World.isLoaded(PartnerPos)) {
            return;
        }
        BlockState Top = World.getBlockState(PartnerPos);
        if (Top.getBlock() instanceof ForcefieldEmitterBlock && Top.getValue(ForcefieldEmitterBlock.Glow) != ForcefieldEmitterBlock.GlowOff) {
            World.setBlock(PartnerPos, Top.setValue(ForcefieldEmitterBlock.Glow, ForcefieldEmitterBlock.GlowOff), Block.UPDATE_CLIENTS);
        }
    }

    private void tickColumn(ServerLevel World, long Now) {
        boolean Powered = FedAt >= Now - 1;
        boolean Ready = Gap > 0 && LeaderPos != null;
        ForcefieldPhase Before = Phase;
        switch (Phase) {
            case Online -> {
                if (!Powered || !Ready || Gap != Height) {
                    shutdown(World, true);
                } else {
                    if (PhaseTicks % CheckTicks == 0 && !verifyCells(World)) {
                        shutdown(World, true);
                    } else {
                        recharge(Now);
                    }
                }
            }
            case Collapsing -> {
                if (PhaseTicks % 3 == 0) {
                    sparks(World, 3);
                }
                if (PhaseTicks >= CollapseTicks) {
                    sparks(World, 8);
                    clearCells(World);
                    Faulted = true;
                    enter(ForcefieldPhase.Offline);
                }
            }
            case Booting -> {
                if (!Powered || !Ready) {
                    enter(ForcefieldPhase.Offline);
                    ForcefieldSounds.play(World, fieldCentre(), ForcefieldRegistry.OfflineLayers, 0.6f, 1.2f);
                } else if (PhaseTicks >= BootTicks) {
                    raise(World);
                } else if (PhaseTicks > 0 && PhaseTicks % (BootTicks / BootSteps) == 0) {
                    int Step = PhaseTicks / (BootTicks / BootSteps);
                    ForcefieldSounds.play(World, fieldCentre(), ForcefieldRegistry.BootStepLayers, 0.55f, 0.8f + 0.12f * Step);
                }
            }
            case Offline -> {
                if (!Faulted && Powered && Ready) {
                    enter(ForcefieldPhase.Booting);
                    ForcefieldSounds.play(World, fieldCentre(), ForcefieldRegistry.BootStartLayers, 0.8f, 1.0f);
                }
            }
        }
        if (Phase == Before) {
            PhaseTicks++;
        }
    }

    private void enter(ForcefieldPhase Next) {
        Phase = Next;
        PhaseTicks = 0;
        index();
        NeedsSync = true;
        UrgentSync = true;
        setChanged();
    }

    private void raise(ServerLevel World) {
        int Cells = Gap;
        for (int Step = 1; Step <= Cells; Step++) {
            if (!passable(World.getBlockState(worldPosition.above(Step)))) {
                enter(ForcefieldPhase.Offline);
                PairDirty = true;
                return;
            }
        }
        BlockState Cell = ForcefieldRegistry.Barrier.get().defaultBlockState().setValue(ForcefieldBarrierBlock.Axis, axis());
        for (int Step = 1; Step <= Cells; Step++) {
            BlockPos Pos = worldPosition.above(Step);
            if (!World.getBlockState(Pos).is(ForcefieldRegistry.Barrier.get())) {
                World.setBlock(Pos, Cell, Block.UPDATE_ALL);
            }
        }
        Height = Cells;
        pushOut(World);
        Integrity = columnIntegrity(Cells);
        Faulted = false;
        LastHit = Long.MIN_VALUE / 2;
        enter(ForcefieldPhase.Online);
        ForcefieldSounds.play(World, fieldCentre(), ForcefieldRegistry.OnlineLayers, 0.9f, 1.0f);
    }

    private void pushOut(ServerLevel World) {
        AABB Slab = sheetBox();
        boolean AlongX = axis() == Direction.Axis.X;
        double Plane = AlongX ? worldPosition.getZ() + 0.5 : worldPosition.getX() + 0.5;
        double Half = ForcefieldBarrierBlock.Thickness / 32.0;
        for (Entity Target : World.getEntities((Entity) null, Slab, Candidate -> !Candidate.isSpectator() && !Candidate.noPhysics && !Candidate.isPassenger())) {
            AABB Box = Target.getBoundingBox();
            double Centre = AlongX ? Box.getCenter().z : Box.getCenter().x;
            double Reach = (AlongX ? Box.getZsize() : Box.getXsize()) / 2.0 + Half + PushMargin;
            double Moved = Centre < Plane ? Plane - Reach : Plane + Reach;
            double Shift = Moved - Centre;
            if (AlongX) {
                Target.teleportTo(Target.getX(), Target.getY(), Target.getZ() + Shift);
            } else {
                Target.teleportTo(Target.getX() + Shift, Target.getY(), Target.getZ());
            }
        }
    }

    private AABB sheetBox() {
        double Half = ForcefieldBarrierBlock.Thickness / 32.0;
        double Top = worldPosition.getY() + 1.0 + Math.max(Height, Gap);
        if (axis() == Direction.Axis.X) {
            return new AABB(worldPosition.getX(), worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5 - Half, worldPosition.getX() + 1.0, Top, worldPosition.getZ() + 0.5 + Half);
        }
        return new AABB(worldPosition.getX() + 0.5 - Half, worldPosition.getY() + 1.0, worldPosition.getZ(), worldPosition.getX() + 0.5 + Half, Top, worldPosition.getZ() + 1.0);
    }

    private boolean verifyCells(ServerLevel World) {
        BlockState Cell = ForcefieldRegistry.Barrier.get().defaultBlockState().setValue(ForcefieldBarrierBlock.Axis, axis());
        for (int Step = 1; Step <= Height; Step++) {
            BlockPos Pos = worldPosition.above(Step);
            BlockState State = World.getBlockState(Pos);
            if (State.is(ForcefieldRegistry.Barrier.get())) {
                continue;
            }
            if (!passable(State)) {
                return false;
            }
            World.setBlock(Pos, Cell, Block.UPDATE_ALL);
        }
        return true;
    }

    public void clearCells(Level World) {
        for (int Step = 1; Step <= MaxGap; Step++) {
            BlockPos Pos = worldPosition.above(Step);
            if (!World.getBlockState(Pos).is(ForcefieldRegistry.Barrier.get())) {
                break;
            }
            World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        Height = 0;
        NeedsSync = true;
    }

    private void shutdown(ServerLevel World, boolean Audible) {
        clearCells(World);
        Integrity = 0.0f;
        enter(ForcefieldPhase.Offline);
        if (Audible) {
            ForcefieldSounds.play(World, fieldCentre(), ForcefieldRegistry.OfflineLayers, 0.8f, 1.0f);
        }
    }

    private void recharge(long Now) {
        float Full = columnIntegrity(Height);
        if (Integrity < Full && Now - LastHit >= RechargeDelay) {
            Integrity = Math.min(Full, Integrity + RechargePerCell * Height);
            setChanged();
        }
    }

    private void sparks(ServerLevel World, int PerCell) {
        AABB Box = sheetBox();
        Vec3 Centre = Box.getCenter();
        World.sendParticles(ParticleTypes.ELECTRIC_SPARK, Centre.x, Centre.y, Centre.z, PerCell * Math.max(1, Height),
                Box.getXsize() * 0.35, Box.getYsize() * 0.35, Box.getZsize() * 0.35, 0.15);
    }

    private void updateGlow(ServerLevel World) {
        int Wanted = switch (Phase) {
            case Online, Collapsing -> ForcefieldEmitterBlock.GlowOnline;
            case Booting -> ForcefieldEmitterBlock.GlowBooting;
            default -> Faulted && Gap > 0 ? ForcefieldEmitterBlock.GlowFault : ForcefieldEmitterBlock.GlowOff;
        };
        BlockState Current = getBlockState();
        if (Current.getBlock() instanceof ForcefieldEmitterBlock && Current.getValue(ForcefieldEmitterBlock.Glow) != Wanted) {
            World.setBlock(worldPosition, Current.setValue(ForcefieldEmitterBlock.Glow, Wanted), Block.UPDATE_CLIENTS);
        }
        if (PartnerPos != null && World.isLoaded(PartnerPos)) {
            BlockState Top = World.getBlockState(PartnerPos);
            if (Top.getBlock() instanceof ForcefieldEmitterBlock && Top.getValue(ForcefieldEmitterBlock.Glow) != Wanted) {
                World.setBlock(PartnerPos, Top.setValue(ForcefieldEmitterBlock.Glow, Wanted), Block.UPDATE_CLIENTS);
            }
        }
    }

    private void index() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (Phase.raised() && !isRemoved()) {
            ForcefieldIndex.add(level, worldPosition);
        } else {
            ForcefieldIndex.remove(level, worldPosition);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        index();
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            ForcefieldIndex.remove(level, worldPosition);
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && !level.isClientSide) {
            ForcefieldIndex.remove(level, worldPosition);
        }
        super.onChunkUnloaded();
    }

    public Vec3 fieldCentre() {
        int Cells = Math.max(1, Math.max(Height, Gap));
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0 + Cells / 2.0, worldPosition.getZ() + 0.5);
    }

    public void damage(ServerLevel World, float Amount, Vec3 Point) {
        if (Amount <= 0.0f || Phase != ForcefieldPhase.Online) {
            return;
        }
        List<ForcefieldEmitterBlockEntity> Live = new ArrayList<>();
        float Total = 0.0f;
        for (ForcefieldEmitterBlockEntity Column : wall()) {
            if (Column.Phase == ForcefieldPhase.Online) {
                Live.add(Column);
                Total += Column.Integrity;
            }
        }
        if (Live.isEmpty()) {
            return;
        }
        long Now = World.getGameTime();
        boolean Breaks = Total - Amount <= CollapseMargin;
        for (ForcefieldEmitterBlockEntity Column : Live) {
            Column.LastHit = Now;
            if (Breaks) {
                Column.Integrity = 0.0f;
                Column.enter(ForcefieldPhase.Collapsing);
            } else {
                Column.Integrity = Math.max(0.0f, Column.Integrity - Amount * (Total > 0.0f ? Column.Integrity / Total : 1.0f / Live.size()));
                Column.NeedsSync = true;
                Column.setChanged();
            }
        }
        float Strength = Breaks ? 1.0f : Mth.clamp(Amount / 20.0f, 0.15f, 1.0f);
        PacketDistributor.sendToPlayersNear(World, null, Point.x, Point.y, Point.z, EffectRange, new ForcefieldHitPayload(Point.x, Point.y, Point.z, axis() == Direction.Axis.X, Strength));
        ForcefieldEmitterBlockEntity Head = leader();
        ForcefieldEmitterBlockEntity Voice = Head != null ? Head : this;
        if (Breaks) {
            ForcefieldSounds.play(World, Point, ForcefieldRegistry.CollapseLayers, 1.2f, 1.0f);
        } else if (Now - Voice.LastZap >= ZapGap) {
            Voice.LastZap = Now;
            ForcefieldSounds.play(World, Point, ForcefieldRegistry.HitLayers, 0.5f + 0.5f * Strength, 1.15f - 0.25f * Strength);
        }
    }

    public Reboot reboot(ServerLevel World) {
        boolean Paired = false;
        boolean Started = false;
        boolean Booting = false;
        boolean Waiting = false;
        for (ForcefieldEmitterBlockEntity Column : wall()) {
            if (Column.Gap <= 0) {
                continue;
            }
            Paired = true;
            if (Column.Phase == ForcefieldPhase.Offline && Column.Faulted) {
                Column.Faulted = false;
                Column.NeedsSync = true;
                Column.setChanged();
                Started = true;
            } else if (Column.Phase == ForcefieldPhase.Booting) {
                Booting = true;
            } else if (Column.Phase == ForcefieldPhase.Offline) {
                Waiting = true;
            }
        }
        if (!Paired) {
            return Reboot.Unpaired;
        }
        boolean Powered = isPowered(World.getGameTime());
        if (Started) {
            ForcefieldSounds.play(World, fieldCentre(), ForcefieldRegistry.BootStartLayers, 0.5f, 0.9f);
            return Powered ? Reboot.Started : Reboot.Waiting;
        }
        if (Booting) {
            return Reboot.InProgress;
        }
        return Waiting ? Reboot.Waiting : Reboot.AlreadyOnline;
    }

    public void redstone(ServerLevel World, boolean High) {
        if (High && !RedstoneHigh) {
            RedstoneHigh = true;
            reboot(World);
        } else if (!High) {
            RedstoneHigh = false;
        }
    }

    public boolean isPowered(long Now) {
        return FedAt >= Now - 1;
    }

    public void markScanDirty() {
        ScanDirty = true;
        PairDirty = true;
    }

    public void markPairDirty() {
        PairDirty = true;
    }

    public void dismantle(Level World) {
        clearCells(World);
        if (World instanceof ServerLevel Server) {
            dimPartner(Server);
        }
    }

    public float fraction() {
        float Full = columnIntegrity(Math.max(Height, Gap));
        return Phase == ForcefieldPhase.Online && Full > 0.0f ? Mth.clamp(Integrity / Full, 0.0f, 1.0f) : 0.0f;
    }

    public WallReport report() {
        List<ForcefieldEmitterBlockEntity> Wall = wall();
        float Sum = 0.0f;
        float Full = 0.0f;
        int Online = 0;
        int Paired = 0;
        int Draw = 0;
        for (ForcefieldEmitterBlockEntity Column : Wall) {
            Draw += Column.need();
            if (Column.Gap > 0) {
                Paired++;
            }
            if (Column.Phase == ForcefieldPhase.Online) {
                Online++;
                Sum += Column.Integrity;
                Full += columnIntegrity(Column.Height);
            }
        }
        ForcefieldEmitterBlockEntity Head = leader();
        long Now = level == null ? 0L : level.getGameTime();
        return new WallReport(Phase, Faulted, Gap, LeaderPos == null, Wall.size(), Paired, Online, Full > 0.0f ? Sum / Full : 0.0f,
                Head == null ? Draw : Head.WallDemand, Head == null ? 0 : Head.WallReceived, isPowered(Now), Phase == ForcefieldPhase.Booting ? (float) PhaseTicks / BootTicks : 0.0f);
    }

    public ForcefieldPhase getPhase() {
        return Phase;
    }

    public int getPhaseTicks() {
        return PhaseTicks;
    }

    public int getHeight() {
        return Height;
    }

    public int getGap() {
        return Gap;
    }

    public boolean isFaulted() {
        return Faulted;
    }

    public float getClientFraction() {
        return ClientFraction;
    }

    public AABB renderBox() {
        return new AABB(worldPosition).expandTowards(0.0, MaxGap + 1.0, 0.0).inflate(0.5, 0.0, 0.5);
    }

    @Override
    public int getPowerDemand() {
        return Share;
    }

    @Override
    public void insertPower(int Amount) {
        long Now = level == null ? 0L : level.getGameTime();
        if (ReceivedTick != Now) {
            ReceivedTick = Now;
            Received = 0;
        }
        Received += Amount;
    }

    @Override
    public void observeNetwork(long Available) {
        if (level != null) {
            Observed = level.getGameTime();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putByte("Phase", (byte) Phase.ordinal());
        Tag.putInt("PhaseTicks", PhaseTicks);
        Tag.putBoolean("Faulted", Faulted);
        Tag.putFloat("Integrity", Integrity);
        Tag.putInt("Height", Height);
        Tag.putBoolean("Redstone", RedstoneHigh);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Phase = ForcefieldPhase.byIndex(Tag.getByte("Phase"));
        PhaseTicks = Tag.getInt("PhaseTicks");
        Faulted = Tag.getBoolean("Faulted");
        Height = Tag.getInt("Height");
        if (Tag.contains("Integrity")) {
            Integrity = Tag.getFloat("Integrity");
            RedstoneHigh = Tag.getBoolean("Redstone");
        }
        if (Tag.contains("Fraction")) {
            ClientFraction = Tag.getFloat("Fraction");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Phase", (byte) Phase.ordinal());
        Tag.putInt("PhaseTicks", PhaseTicks);
        Tag.putBoolean("Faulted", Faulted);
        Tag.putInt("Height", Phase.raised() ? Height : Gap);
        Tag.putFloat("Fraction", fraction());
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public enum Reboot {
        Started,
        Waiting,
        AlreadyOnline,
        InProgress,
        Unpaired
    }

    public record WallReport(ForcefieldPhase Phase, boolean Faulted, int Gap, boolean Overlong, int Columns, int Paired, int Online, float Integrity,
                             int Draw, int Received, boolean Powered, float Boot) {
    }
}
