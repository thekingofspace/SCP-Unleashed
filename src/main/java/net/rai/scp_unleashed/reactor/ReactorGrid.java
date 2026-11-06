package net.rai.scp_unleashed.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.block.DuctBlock;
import net.rai.scp_unleashed.block.PressurizerBlock;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.fission.ToxicGasCloud;
import net.rai.scp_unleashed.gas.Gas;
import net.rai.scp_unleashed.gas.GasHandler;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.pressure.DuctKind;
import net.rai.scp_unleashed.pressure.GasPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.reactor.core.CoreTower;
import net.rai.scp_unleashed.reactor.core.ReactorCore;
import net.rai.scp_unleashed.reactor.core.ReactorTuning;
import net.rai.scp_unleashed.reactor.core.TowerType;
import net.rai.scp_unleashed.discord.DiscordNotices;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ReactorGrid {
    public static final int VentTicks = 10;
    public static final int ScorchTicks = 20;
    public static final int SaveTicks = 40;
    public static final int RodSoundTicks = 6;
    public static final int DirectPush = 80;
    public static final float LeakStart = 0.85f;
    public static final float LeakShare = 0.6f;
    public static final float BlowStart = 0.9f;
    public static final float BlowTo = 0.75f;
    public static final int ScramHold = 40;
    public static final int HoldTicks = 30;
    public static final float BlastBase = 2.5f;
    public static final float BlastPerRod = 0.6f;
    public static final float BlastPerSegment = 0.25f;
    public static final float BlastMax = 9.0f;
    public static final int FalloutRods = 4;
    public static final int GasPerRod = 200;
    public static final int HotAlarmTicks = 40;
    public static final int CriticalAlarmTicks = 20;
    public static final int BreachAlarmTicks = 10;
    public static final int CreakMin = 30;
    public static final int CreakSpread = 50;
    public static final float ScorchDamage = 2.0f;
    public static final float ScorchSeconds = 3.0f;
    public static final double ScorchReach = 1.25;
    public static final int HardCap = 4096;

    private final Set<BlockPos> Members;
    private final List<GridTower> Towers;
    private final Map<BlockPos, GridTower> TowerByPos = new HashMap<>();
    private final ReactorCore Core;
    private final BlockPos Anchor;
    private final GridState State;
    private final int Phase;
    private boolean Discarded;
    private boolean RedstoneDirty = true;
    private long ScramUntil = Long.MIN_VALUE / 2;
    private long NextAlarm;
    private int AlarmStep;

    private ReactorGrid(Set<BlockPos> Members, List<GridTower> Towers, GridState State) {
        this.Members = Collections.unmodifiableSet(Members);
        this.Towers = List.copyOf(Towers);
        this.State = State;
        List<CoreTower> CoreTowers = new ArrayList<>();
        for (GridTower Tower : this.Towers) {
            CoreTowers.add(Tower.Core);
            for (BlockPos Pos : Tower.Segments) {
                TowerByPos.put(Pos, Tower);
            }
        }
        Core = new ReactorCore(CoreTowers);
        BlockPos Lowest = null;
        for (BlockPos Pos : Members) {
            if (Lowest == null || Pos.getY() < Lowest.getY() || Pos.getY() == Lowest.getY() && (Pos.getX() < Lowest.getX() || Pos.getX() == Lowest.getX() && Pos.getZ() < Lowest.getZ())) {
                Lowest = Pos;
            }
        }
        Anchor = Lowest == null ? BlockPos.ZERO : Lowest;
        Phase = Math.floorMod(Anchor.hashCode(), 40);
    }

    public static ReactorGrid build(ServerLevel World, BlockPos Seed) {
        Set<BlockPos> Members = new HashSet<>();
        ArrayDeque<BlockPos> Queue = new ArrayDeque<>();
        BlockPos Start = Seed.immutable();
        Members.add(Start);
        Queue.add(Start);
        boolean Incomplete = false;
        while (!Queue.isEmpty()) {
            BlockPos Current = Queue.poll();
            for (Direction Side : Direction.values()) {
                BlockPos Next = Current.relative(Side);
                if (Members.contains(Next)) {
                    continue;
                }
                if (!World.isLoaded(Next)) {
                    Incomplete = true;
                    continue;
                }
                if (!(World.getBlockState(Next).getBlock() instanceof TowerSegmentBlock)) {
                    continue;
                }
                if (Members.size() >= HardCap) {
                    continue;
                }
                Members.add(Next);
                Queue.add(Next);
            }
        }
        if (Members.size() > ReactorTuning.MaxSegments) {
            return new ReactorGrid(Members, List.of(), GridState.Oversized);
        }
        Map<Long, List<BlockPos>> Columns = new HashMap<>();
        for (BlockPos Pos : Members) {
            Columns.computeIfAbsent(((long) Pos.getX() << 32) ^ (Pos.getZ() & 0xFFFFFFFFL), Key -> new ArrayList<>()).add(Pos);
        }
        List<GridTower> Towers = new ArrayList<>();
        for (List<BlockPos> Column : Columns.values()) {
            Column.sort(Comparator.comparingInt(BlockPos::getY));
            List<BlockPos> Run = new ArrayList<>();
            TowerType RunType = null;
            for (BlockPos Pos : Column) {
                if (!(World.getBlockState(Pos).getBlock() instanceof TowerSegmentBlock Segment)) {
                    continue;
                }
                TowerType Type = Segment.getTowerType();
                boolean Continues = !Run.isEmpty() && RunType == Type && Run.getLast().getY() + 1 == Pos.getY() && Run.size() < ReactorTuning.MaxHeight;
                if (!Continues && !Run.isEmpty()) {
                    GridTower Made = makeTower(World, RunType, Run);
                    if (Made == null) {
                        Incomplete = true;
                    } else {
                        Towers.add(Made);
                    }
                    Run = new ArrayList<>();
                }
                Run.add(Pos);
                RunType = Type;
            }
            if (!Run.isEmpty()) {
                GridTower Made = makeTower(World, RunType, Run);
                if (Made == null) {
                    Incomplete = true;
                } else {
                    Towers.add(Made);
                }
            }
        }
        Towers.sort(Comparator.<GridTower>comparingInt(Tower -> Tower.Core.X).thenComparingInt(Tower -> Tower.Core.Z).thenComparingInt(Tower -> Tower.Core.Bottom));
        GridState State = Towers.size() > ReactorTuning.MaxTowers ? GridState.Oversized : Incomplete ? GridState.Incomplete : GridState.Ready;
        return new ReactorGrid(Members, Towers, State);
    }

    @Nullable
    private static GridTower makeTower(ServerLevel World, TowerType Type, List<BlockPos> Run) {
        BlockPos Low = Run.getFirst();
        CoreTower Tower = new CoreTower(Type, Low.getX(), Low.getZ(), Low.getY(), Run.size());
        List<TowerSegmentBlockEntity> Entities = new ArrayList<>();
        float TemperatureSum = 0.0f;
        float Integrity = ReactorTuning.FullIntegrity;
        float Water = 0.0f;
        float Steam = 0.0f;
        float Gas = 0.0f;
        float Insertion = 0.0f;
        float Target = 0.0f;
        for (int Tier = 0; Tier < Run.size(); Tier++) {
            if (!(World.getBlockEntity(Run.get(Tier)) instanceof TowerSegmentBlockEntity Piece)) {
                return null;
            }
            Entities.add(Piece);
            TemperatureSum += Piece.getTemperature();
            Integrity = Math.min(Integrity, Piece.getIntegrity());
            Water += Piece.getWater();
            Steam += Piece.getSteam();
            Gas += Piece.getGas();
            Insertion = Math.max(Insertion, Piece.getInsertion());
            Target = Math.max(Target, Piece.getTarget());
            if (Type == TowerType.Fuel && Piece.hasFreshRod()) {
                Tower.setFuel(Tier, TowerFuel.heatOf(Piece.getRod()), Piece.burnLeft());
            }
        }
        Tower.Temperature = Math.max(ReactorTuning.Ambient, TemperatureSum / Run.size());
        Tower.Integrity = Integrity;
        Tower.Water = Math.min(Water, Tower.waterCapacity());
        Tower.Steam = Math.min(Steam, Tower.steamCapacity());
        Tower.Gas = Math.min(Gas, Tower.gasCapacity());
        Tower.Insertion = Insertion;
        Tower.Target = Target;
        return new GridTower(Tower, Run, Entities);
    }

    public Set<BlockPos> getMembers() {
        return Members;
    }

    public List<GridTower> getTowers() {
        return Towers;
    }

    public ReactorCore getCore() {
        return Core;
    }

    public GridState getState() {
        return State;
    }

    public BlockPos getAnchor() {
        return Anchor;
    }

    public boolean isDiscarded() {
        return Discarded;
    }

    void markDiscarded() {
        Discarded = true;
    }

    @Nullable
    public GridTower towerAt(BlockPos Pos) {
        return TowerByPos.get(Pos);
    }

    public boolean isScramHeld(long Now) {
        return Now < ScramUntil;
    }

    public void markRedstone() {
        RedstoneDirty = true;
    }

    public void holdScram(long Now) {
        ScramUntil = Math.max(ScramUntil, Now + HoldTicks);
    }

    public void setTarget(GridTower Tower, float Value) {
        if (Tower.getType() == TowerType.Rods) {
            Tower.Core.Target = Math.max(0.0f, Math.min(100.0f, Value));
        }
    }

    public void setAllTargets(float Value) {
        for (GridTower Tower : Towers) {
            setTarget(Tower, Value);
        }
    }

    public void scram(ServerLevel World) {
        setAllTargets(100.0f);
        ScramUntil = World.getGameTime() + ScramHold;
        for (GridTower Tower : Towers) {
            if (Tower.getType() == TowerType.Rods) {
                Vec3 Centre = Tower.centre();
                World.playSound(null, Centre.x, Centre.y, Centre.z, FissionRegistry.ReactorScram.get(), SoundSource.BLOCKS, 1.4f, 1.0f);
                World.playSound(null, Centre.x, Centre.y, Centre.z, FissionRegistry.ReactorClunk.get(), SoundSource.BLOCKS, 1.2f, 0.8f);
            }
        }
    }

    public void fuelChanged(GridTower Tower) {
        syncBurn(Tower);
        for (int Tier = 0; Tier < Tower.Entities.size() && Tier < Tower.Core.RodHeat.length; Tier++) {
            TowerSegmentBlockEntity Piece = Tower.Entities.get(Tier);
            if (Piece.hasFreshRod()) {
                Tower.Core.setFuel(Tier, TowerFuel.heatOf(Piece.getRod()), Piece.burnLeft());
            } else {
                Tower.Core.setFuel(Tier, 0.0f, 0.0f);
            }
        }
    }

    private static void syncBurn(GridTower Tower) {
        for (int Tier = 0; Tier < Tower.Entities.size() && Tier < Tower.Core.RodHeat.length; Tier++) {
            if (Tower.Core.RodHeat[Tier] > 0.0f) {
                Tower.Entities.get(Tier).setBurnLeft(Tower.Core.BurnLeft[Tier]);
            }
        }
    }

    public void writeBack() {
        for (GridTower Tower : Towers) {
            CoreTower Data = Tower.Core;
            float Share = 1.0f / Data.Height;
            syncBurn(Tower);
            for (TowerSegmentBlockEntity Piece : Tower.Entities) {
                if (!Piece.isRemoved()) {
                    Piece.store(Data.Temperature, Data.Integrity, Data.Water * Share, Data.Steam * Share, Data.Gas * Share, Data.Insertion, Data.Target);
                }
            }
        }
    }

    public void tick(ServerLevel World) {
        if (Discarded || State != GridState.Ready || !World.shouldTickBlocksAt(Anchor)) {
            return;
        }
        long Now = World.getGameTime();
        if (RedstoneDirty) {
            RedstoneDirty = false;
            refreshRedstone(World);
        }
        Core.setScram(Now < ScramUntil);
        Core.step();
        convertSpent(World);
        for (GridTower Tower : Towers) {
            if (Tower.Core.Failed) {
                fail(World, Tower);
                return;
            }
        }
        long Beat = Now + Phase;
        if (Beat % VentTicks == 0) {
            vent(World);
            visuals(World);
            alarms(World, Now);
        }
        if (Beat % ScorchTicks == 0) {
            scorch(World);
        }
        if (Beat % RodSoundTicks == 0) {
            rodSounds(World);
        }
        if (Beat % SaveTicks == 0) {
            writeBack();
        }
    }

    private void refreshRedstone(ServerLevel World) {
        for (GridTower Tower : Towers) {
            if (Tower.getType() != TowerType.Rods) {
                continue;
            }
            boolean Powered = false;
            for (BlockPos Pos : Tower.Segments) {
                if (World.hasNeighborSignal(Pos)) {
                    Powered = true;
                    break;
                }
            }
            Tower.Core.Forced = Powered;
        }
    }

    private void convertSpent(ServerLevel World) {
        for (GridTower Tower : Towers) {
            if (Tower.getType() != TowerType.Fuel) {
                continue;
            }
            for (int Tier = 0; Tier < Tower.Core.Spent.length; Tier++) {
                if (!Tower.Core.Spent[Tier]) {
                    continue;
                }
                Tower.Core.setFuel(Tier, 0.0f, 0.0f);
                if (Tier < Tower.Entities.size()) {
                    ItemStack Depleted = TowerFuel.depleted();
                    Tower.Entities.get(Tier).setRod(Depleted, 0.0f);
                    BlockPos Pos = Tower.Segments.get(Tier);
                    World.playSound(null, Pos, FissionRegistry.ReactorClunk.get(), SoundSource.BLOCKS, 0.6f, 1.25f);
                }
            }
        }
    }

    private void vent(ServerLevel World) {
        Gas Toxic = GasRegistry.ToxicGas.get();
        Gas Vapour = GasRegistry.Steam.get();
        RandomSource Random = World.getRandom();
        for (GridTower Tower : Towers) {
            CoreTower Data = Tower.Core;
            if (Data.Type == TowerType.Fuel) {
                Tower.Captured = pushDirect(World, Tower.bottom(), Direction.DOWN, Tower.Outlet, Toxic);
                Tower.Vented = hasDuct(World, Tower.bottom(), Direction.DOWN);
                float Leak = Data.GasOverflow;
                Data.GasOverflow = 0.0f;
                float Start = Data.gasCapacity() * LeakStart;
                if (Data.Gas > Start) {
                    float Extra = (Data.Gas - Start) * LeakShare;
                    Data.Gas -= Extra;
                    Leak += Extra;
                }
                Tower.Leaking = Leak >= 0.5f;
                if (Tower.Leaking) {
                    leak(World, Tower, Leak, Random);
                }
            } else if (Data.Type.makesSteam()) {
                Tower.Captured = pushDirect(World, Tower.top(), Direction.UP, Tower.Outlet, Vapour);
                Tower.Vented = false;
                for (Direction Side : Direction.values()) {
                    if (Side != Direction.DOWN && hasDuct(World, Tower.top(), Side)) {
                        Tower.Vented = true;
                    }
                }
                float Blow = Data.Overflow;
                Data.Overflow = 0.0f;
                float Capacity = Data.steamCapacity();
                if (Data.Steam > Capacity * BlowStart) {
                    float Extra = Data.Steam - Capacity * BlowTo;
                    Data.Steam -= Extra;
                    Blow += Extra;
                }
                Tower.Blowing = Blow >= 0.5f;
                if (Tower.Blowing) {
                    BlockPos Top = Tower.top();
                    Vec3 Mouth = new Vec3(Top.getX() + 0.5, Top.getY() + 1.15, Top.getZ() + 0.5);
                    Vapour.release(World, Mouth, Math.max(1, Math.round(Blow)));
                    if (Random.nextInt(4) == 0) {
                        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, ReactorRegistry.TowerVent.get(), SoundSource.BLOCKS, 0.7f, 0.85f + Random.nextFloat() * 0.3f);
                    }
                }
            }
        }
    }

    private static boolean pushDirect(ServerLevel World, BlockPos Pos, Direction Side, GasHandler Source, Gas Kind) {
        BlockPos Next = Pos.relative(Side);
        if (!World.isLoaded(Next)) {
            return false;
        }
        BlockState Neighbour = World.getBlockState(Next);
        if (Neighbour.getBlock() instanceof TowerSegmentBlock || !(Neighbour.getBlock() instanceof GasPort Port)) {
            return false;
        }
        PortRole Role = Port.gasRole(Neighbour, Side.getOpposite());
        if (Role == null || !Role.canFill()) {
            return false;
        }
        GasHandler Sink = Port.gasHandler(World, Next, Neighbour);
        if (Sink == null) {
            return false;
        }
        GasStack Offer = Source.drain(DirectPush, true);
        if (!Offer.isEmpty() && Offer.is(Kind)) {
            int Filled = Sink.fill(Offer, false);
            if (Filled > 0) {
                Source.drain(Filled, false);
            }
        }
        return true;
    }

    public static boolean hasDuct(Level World, BlockPos Pos, Direction Side) {
        BlockPos Next = Pos.relative(Side);
        if (!World.isLoaded(Next)) {
            return false;
        }
        BlockState Neighbour = World.getBlockState(Next);
        if (Neighbour.getBlock() instanceof DuctBlock Duct && Duct.getKind() == DuctKind.Vent) {
            return Neighbour.getValue(DuctBlock.Connections.get(Side.getOpposite()));
        }
        return Neighbour.getBlock() instanceof PressurizerBlock Pump && Pump.getKind() == DuctKind.Vent;
    }

    private static void leak(ServerLevel World, GridTower Tower, float Amount, RandomSource Random) {
        BlockPos Low = Tower.bottom();
        List<Vec3> Openings = new ArrayList<>();
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            BlockPos Spot = Low.relative(Side);
            if (World.isLoaded(Spot) && !World.getBlockState(Spot).isCollisionShapeFullBlock(World, Spot)) {
                Openings.add(new Vec3(Spot.getX() + 0.5, Spot.getY() + 0.3, Spot.getZ() + 0.5));
            }
        }
        BlockPos Under = Low.below();
        if (Openings.isEmpty() && World.isLoaded(Under) && !World.getBlockState(Under).isCollisionShapeFullBlock(World, Under)) {
            Openings.add(Vec3.atCenterOf(Under));
        }
        if (Openings.isEmpty()) {
            BlockPos High = Tower.top().above();
            Openings.add(new Vec3(High.getX() + 0.5, High.getY() + 0.3, High.getZ() + 0.5));
        }
        float Share = Amount / Openings.size();
        for (Vec3 Point : Openings) {
            ToxicGasCloud.feed(World, Point, Share);
        }
        if (Random.nextInt(4) == 0) {
            Vec3 Spot = Openings.get(Random.nextInt(Openings.size()));
            World.playSound(null, Spot.x, Spot.y, Spot.z, FissionRegistry.GasHiss.get(), SoundSource.BLOCKS, 0.5f, 0.9f + Random.nextFloat() * 0.2f);
        }
    }

    private void visuals(ServerLevel World) {
        for (GridTower Tower : Towers) {
            CoreTower Data = Tower.Core;
            int Wanted = switch (Data.Type) {
                case Fuel -> Data.Temperature >= ReactorTuning.CriticalLevel ? 3 : Data.Temperature >= ReactorTuning.HotLevel ? 2 : Data.isReacting() ? 1 : 0;
                case Rods -> Data.Insertion < 99.5f ? 1 : 0;
                case Booster -> Data.BoostPassed > 0.001f ? 1 : 0;
                default -> Data.SteamMade > 0.05f ? 1 : 0;
            };
            if (Wanted == Tower.Visual) {
                continue;
            }
            Tower.Visual = Wanted;
            for (BlockPos Pos : Tower.Segments) {
                BlockState Current = World.getBlockState(Pos);
                if (Current.getBlock() instanceof TowerSegmentBlock Segment) {
                    BlockState Next = Segment.withVisual(Current, Wanted);
                    if (Next != Current) {
                        World.setBlock(Pos, Next, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private void alarms(ServerLevel World, long Now) {
        GridTower Worst = null;
        TowerHeat Band = TowerHeat.Stable;
        RandomSource Random = World.getRandom();
        for (GridTower Tower : Towers) {
            if (Tower.getType() != TowerType.Fuel) {
                continue;
            }
            TowerHeat Heat = TowerHeat.of(Tower.Core.Temperature, Tower.Core.Integrity);
            if (Worst == null || Heat.ordinal() > Band.ordinal() || Heat == Band && Tower.Core.Temperature > Worst.Core.Temperature) {
                Worst = Tower;
                Band = Heat;
            }
            if (Tower.Core.Temperature >= ReactorTuning.CriticalLevel && Now >= Tower.NextCreak) {
                Tower.NextCreak = Now + CreakMin + Random.nextInt(CreakSpread);
                BlockPos Spot = Tower.Segments.get(Random.nextInt(Tower.Segments.size()));
                World.playSound(null, Spot, ReactorRegistry.TowerCreak.get(), SoundSource.BLOCKS, 1.0f, 0.7f + Random.nextFloat() * 0.3f);
                World.sendParticles(ParticleTypes.LARGE_SMOKE, Spot.getX() + 0.5, Spot.getY() + 0.5, Spot.getZ() + 0.5, 4, 0.45, 0.4, 0.45, 0.01);
            }
            if (Heat == TowerHeat.Breach) {
                Vec3 Centre = Tower.centre();
                World.sendParticles(ParticleTypes.LARGE_SMOKE, Centre.x, Tower.top().getY() + 1.0, Centre.z, 6, 0.3, 0.6, 0.3, 0.03);
                World.sendParticles(ParticleTypes.LAVA, Centre.x, Centre.y, Centre.z, 2, 0.5, Tower.Segments.size() * 0.3, 0.5, 0.0);
            }
        }
        if (Worst == null || Band == TowerHeat.Stable || Now < NextAlarm) {
            return;
        }
        Vec3 Centre = Worst.centre();
        AlarmStep++;
        switch (Band) {
            case Hot -> {
                World.playSound(null, Centre.x, Centre.y, Centre.z, FissionRegistry.ReactorAlarm.get(), SoundSource.BLOCKS, 1.2f, AlarmStep % 2 == 0 ? 1.0f : 0.75f);
                NextAlarm = Now + HotAlarmTicks;
            }
            case Critical -> {
                World.playSound(null, Centre.x, Centre.y, Centre.z, FissionRegistry.ReactorKlaxon.get(), SoundSource.BLOCKS, 1.8f, 1.0f);
                World.playSound(null, Centre.x, Centre.y, Centre.z, FissionRegistry.ReactorAlarm.get(), SoundSource.BLOCKS, 1.4f, AlarmStep % 2 == 0 ? 0.8f : 0.6f);
                NextAlarm = Now + CriticalAlarmTicks;
            }
            default -> {
                World.playSound(null, Centre.x, Centre.y, Centre.z, FissionRegistry.ReactorKlaxon.get(), SoundSource.BLOCKS, 2.2f, AlarmStep % 2 == 0 ? 1.15f : 0.95f);
                World.playSound(null, Centre.x, Centre.y, Centre.z, FissionRegistry.ReactorBell.get(), SoundSource.BLOCKS, 1.6f, AlarmStep % 2 == 0 ? 1.0f : 0.8f);
                NextAlarm = Now + BreachAlarmTicks;
            }
        }
    }

    private void scorch(ServerLevel World) {
        for (GridTower Tower : Towers) {
            if (Tower.getType() != TowerType.Fuel || Tower.Core.Temperature < ReactorTuning.CriticalLevel) {
                continue;
            }
            for (LivingEntity Target : World.getEntitiesOfClass(LivingEntity.class, Tower.bounds().inflate(ScorchReach))) {
                Target.hurt(World.damageSources().hotFloor(), ScorchDamage);
                Target.igniteForSeconds(ScorchSeconds);
            }
        }
    }

    private void rodSounds(ServerLevel World) {
        for (GridTower Tower : Towers) {
            if (Tower.getType() == TowerType.Rods && Tower.Core.Moving) {
                Vec3 Centre = Tower.centre();
                World.playSound(null, Centre.x, Centre.y, Centre.z, ReactorRegistry.RodDrive.get(), SoundSource.BLOCKS, 0.6f, 0.85f + Tower.Core.Insertion / 400.0f);
            }
        }
    }

    private void fail(ServerLevel World, GridTower Tower) {
        Core.shock(Tower.Core);
        writeBack();
        Vec3 Centre = Tower.centre();
        DiscordNotices.meltdown(World, BlockPos.containing(Centre), true);
        int Rods = Tower.Core.loadedLevels();
        float Power = Math.min(BlastMax, BlastBase + BlastPerRod * Rods + BlastPerSegment * Tower.Core.Height);
        int Released = Math.round(Tower.Core.Gas) + GasPerRod * Math.max(1, Rods);
        ReactorGridManager.get(World).discard(this);
        for (TowerSegmentBlockEntity Piece : Tower.Entities) {
            Piece.clearContents();
        }
        for (BlockPos Pos : Tower.Segments) {
            if (World.getBlockState(Pos).getBlock() instanceof TowerSegmentBlock) {
                World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        DamageSource Source = new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(FissionRegistry.MeltdownDamage));
        World.explode(null, Source, null, Centre.x, Centre.y, Centre.z, Power, true, Level.ExplosionInteraction.BLOCK,
                ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, FissionRegistry.Meltdown);
        World.playSound(null, Centre.x, Centre.y, Centre.z, FissionRegistry.MeltdownRumble.get(), SoundSource.BLOCKS, 4.0f, 1.0f);
        if (Rods >= FalloutRods) {
            ToxicGasCloud.fallout(World, Centre);
        } else {
            GasRegistry.ToxicGas.get().release(World, Centre, Released);
        }
    }
}
