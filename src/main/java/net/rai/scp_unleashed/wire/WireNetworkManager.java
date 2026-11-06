package net.rai.scp_unleashed.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.block.BatteryBlock;
import net.rai.scp_unleashed.block.OutletBlock;
import net.rai.scp_unleashed.block.entity.BreakerBlockEntity;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.creativeoutlet.CreativeOutletBlock;
import net.rai.scp_unleashed.registry.ModSounds;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;

public class WireNetworkManager {
    private static final Map<ServerLevel, WireNetworkManager> Managers = new WeakHashMap<>();
    private static final int VirtualOffset = 1 << 20;

    private final Set<BlockPos> Outlets = new LinkedHashSet<>();
    private final Map<BlockPos, WireNetwork> NetworkByOutlet = new HashMap<>();
    private final List<WireNetwork> Networks = new ArrayList<>();
    private final ArrayDeque<PendingMessage> Messages = new ArrayDeque<>();
    private final Map<WirePair, WireSpan> Spans = new HashMap<>();
    private final Map<Long, Set<WirePair>> SpanIndex = new HashMap<>();
    private final Set<WirePair> PendingValidation = new LinkedHashSet<>();
    private final Set<BlockPos> ChangedCells = new LinkedHashSet<>();
    private final Map<WirePair, Integer> WireStrain = new HashMap<>();
    private final Map<BlockPos, Integer> OutletStrain = new HashMap<>();
    private final Map<BlockPos, Integer> SwitchStrain = new HashMap<>();
    private final Map<BlockPos, ChargingRedstone> Charging = new HashMap<>();
    private final Set<BlockPos> SeenHosts = new HashSet<>();
    private final Map<BlockPos, BlockPos> HostOf = new HashMap<>();
    private final Map<BlockPos, List<BlockPos>> OutletsOnHost = new HashMap<>();
    private final Set<BlockPos> Routers = new HashSet<>();
    private final Map<WireNetwork, List<WireNetwork>> DataDomains = new HashMap<>();
    private final Map<WireNetwork, BlockPos> NetworkExits = new HashMap<>();
    private final Map<BlockPos, BatteryBank> BankByMember = new HashMap<>();
    private final Map<BlockPos, SwitchState> Switches = new HashMap<>();
    private final Map<BlockPos, SwitchState> SwitchByKey = new HashMap<>();
    private final Map<BlockPos, double[]> SwitchLoads = new HashMap<>();
    private boolean Dirty;
    private boolean Ticking;

    public static WireNetworkManager get(ServerLevel World) {
        return Managers.computeIfAbsent(World, Key -> new WireNetworkManager());
    }

    public static void routerUnloaded(ServerLevel World, BlockPos Pos) {
        WireNetworkManager Manager = Managers.get(World);
        if (Manager != null && Manager.Routers.remove(Pos)) {
            Manager.Dirty = true;
        }
    }

    public void routerLoaded(BlockPos Pos) {
        if (Routers.add(Pos.immutable())) {
            Dirty = true;
        }
    }

    public int dataNetworkCount(ServerLevel World, BlockPos RouterPos) {
        if (Dirty && !Ticking) {
            rebuild(World);
        }
        Set<WireNetwork> Reachable = new HashSet<>();
        for (BlockPos OutletPos : OutletsOnHost.getOrDefault(RouterPos, List.of())) {
            WireNetwork Network = NetworkByOutlet.get(OutletPos);
            if (Network != null) {
                Reachable.add(Network);
                Reachable.addAll(DataDomains.getOrDefault(Network, List.of()));
            }
        }
        return Reachable.size();
    }

    public static void outletUnloaded(ServerLevel World, OutletBlockEntity Outlet) {
        WireNetworkManager Manager = Managers.get(World);
        if (Manager != null) {
            Manager.unloadOutlet(Outlet);
        }
    }

    public static void blockChanged(ServerLevel World, BlockPos Pos) {
        WireNetworkManager Manager = Managers.get(World);
        if (Manager != null && Manager.SpanIndex.containsKey(Pos.asLong())) {
            Manager.ChangedCells.add(Pos.immutable());
        }
    }

    public static void tickLevel(ServerLevel World) {
        WireNetworkManager Manager = Managers.get(World);
        if (Manager != null) {
            Manager.tick(World);
        }
    }

    public static void forget(ServerLevel World) {
        Managers.remove(World);
    }

    public void outletLoaded(OutletBlockEntity Outlet) {
        BlockPos Pos = Outlet.getBlockPos().immutable();
        Outlets.add(Pos);
        for (WireLink Link : Outlet.getLinks()) {
            WirePair Pair = WirePair.of(Pos, Link.Target());
            indexWire(Pair, Outlet.attachFor(Link), Link.targetAttach(), Link.Tier());
            PendingValidation.add(Pair);
        }
        Dirty = true;
    }

    private void unloadOutlet(OutletBlockEntity Outlet) {
        BlockPos Pos = Outlet.getBlockPos();
        if (!Outlets.remove(Pos)) {
            return;
        }
        for (WireLink Link : Outlet.getLinks()) {
            if (!Outlets.contains(Link.Target())) {
                unindexWire(WirePair.of(Pos, Link.Target()));
            }
        }
        Dirty = true;
    }

    public void wireAdded(BlockPos First, Vec3 FirstAttach, BlockPos Second, Vec3 SecondAttach, WireTier Tier) {
        indexWire(WirePair.of(First, Second), FirstAttach, SecondAttach, Tier);
        Dirty = true;
    }

    public void wireRemoved(BlockPos First, BlockPos Second) {
        unindexWire(WirePair.of(First, Second));
        Dirty = true;
    }

    public BlockPos canonicalHost(ServerLevel World, BlockPos HostPos) {
        if (!World.isLoaded(HostPos)) {
            return HostPos;
        }
        BlockState State = World.getBlockState(HostPos);
        if (State.getBlock() instanceof BatteryBlock) {
            return bankAt(World, HostPos).getCanonical();
        }
        return State.getBlock() instanceof WireHostBlock Host ? Host.devicePos(State, HostPos).immutable() : HostPos.immutable();
    }

    public BatteryBank bankAt(ServerLevel World, BlockPos CellPos) {
        BatteryBank Bank = BankByMember.get(CellPos);
        if (Bank == null) {
            Bank = BatteryBank.scan(World, CellPos);
            for (BlockPos Member : Bank.getMembers()) {
                BankByMember.put(Member, Bank);
            }
            BankByMember.putIfAbsent(CellPos.immutable(), Bank);
        }
        return Bank;
    }

    @Nullable
    public WireDevice deviceAt(ServerLevel World, BlockPos HostPos) {
        if (World.getBlockState(HostPos).getBlock() instanceof BatteryBlock) {
            return bankAt(World, HostPos);
        }
        if (World.getBlockEntity(HostPos) instanceof WireDevice Device) {
            return Device;
        }
        ChargingRedstone.Charge Match = ChargingRedstone.chargeFor(World.getBlockState(HostPos));
        if (Match != null) {
            SeenHosts.add(HostPos);
            return Charging.compute(HostPos.immutable(), (Key, Existing) -> Existing != null && Existing.getCharge() == Match ? Existing : new ChargingRedstone(Match));
        }
        return null;
    }

    @Nullable
    public ChargingRedstone chargeAt(BlockPos HostPos) {
        return Charging.get(HostPos);
    }

    @Nullable
    public double[] switchLoad(BlockPos SwitchPos) {
        return SwitchLoads.get(SwitchPos);
    }

    public void markDirty() {
        Dirty = true;
    }

    @Nullable
    public double[] openSwitchEstimate(ServerLevel World, BlockPos SwitchPos) {
        if (Dirty) {
            return null;
        }
        SwitchState Info = Switches.get(SwitchPos);
        if (Info == null || Info.Closed()) {
            return null;
        }
        List<WireNetwork> Groups = new ArrayList<>();
        for (BlockPos OutletPos : switchOutlets(SwitchPos)) {
            WireNetwork Network = NetworkByOutlet.get(OutletPos);
            if (Network != null && !Groups.contains(Network)) {
                Groups.add(Network);
            }
        }
        return new double[]{crossingLoad(Groups), switchLimit(World, Info)};
    }

    private static double crossingLoad(List<WireNetwork> Groups) {
        if (Groups.size() < 2) {
            return 0.0;
        }
        int Count = Groups.size();
        long[] Offers = new long[Count];
        long[] Reserves = new long[Count];
        long[] Demands = new long[Count];
        long[] StorageOffers = new long[Count];
        long[] StorageDemands = new long[Count];
        long[] Sinks = new long[Count];
        for (int Index = 0; Index < Count; Index++) {
            WireNetwork Group = Groups.get(Index);
            Offers[Index] = Group.getLastSupply();
            Reserves[Index] = Group.getLastBackup();
            Demands[Index] = Group.getLastDemand();
            StorageDemands[Index] = Group.getLastStorageIn();
            Sinks[Index] = Group.getLastSinkWeight();
        }
        PowerSplit Joined = PowerSplit.solve(Offers, Reserves, Demands, StorageOffers, StorageDemands, Sinks, 0, 0);
        double Worst = 0.0;
        for (int Index = 0; Index < Count; Index++) {
            Worst = Math.max(Worst, Math.abs(Joined.Outputs()[Index] - Joined.Inputs()[Index]));
        }
        return Worst;
    }

    @Nullable
    public WireNetwork networkAt(ServerLevel World, BlockPos OutletPos) {
        if (Dirty && !Ticking) {
            rebuild(World);
        }
        return NetworkByOutlet.get(OutletPos);
    }

    public boolean send(BlockPos OutletPos, WireMessage Message) {
        if (Messages.size() >= Wires.MaxQueuedMessages || !WireChannels.isValidChannel(Message.Channel())) {
            return false;
        }
        Messages.add(new PendingMessage(OutletPos.immutable(), Message));
        return true;
    }

    public int sendFromDevice(ServerLevel World, BlockPos DevicePos, String Channel, CompoundTag Payload) {
        if (Dirty && !Ticking) {
            rebuild(World);
        }
        if (!WireChannels.isValidChannel(Channel)) {
            return 0;
        }
        BlockPos Host = canonicalHost(World, DevicePos);
        Set<WireNetwork> Reached = new HashSet<>();
        int Sent = 0;
        for (BlockPos OutletPos : outletsServing(Host)) {
            WireNetwork Network = NetworkByOutlet.get(OutletPos);
            if (Network != null && Reached.add(Network) && send(OutletPos, new WireMessage(Channel, Payload.copy(), Host))) {
                Sent++;
            }
        }
        return Sent;
    }

    private List<BlockPos> outletsServing(BlockPos Host) {
        return Switches.containsKey(Host) ? switchOutlets(Host) : OutletsOnHost.getOrDefault(Host, List.of());
    }

    private List<BlockPos> switchOutlets(BlockPos SwitchPos) {
        List<BlockPos> Found = new ArrayList<>();
        for (Direction Face : Direction.values()) {
            Found.addAll(OutletsOnHost.getOrDefault(faceKey(SwitchPos, Face), List.of()));
        }
        return Found;
    }

    @Nullable
    public DeviceFlow deviceFlow(ServerLevel World, BlockPos DevicePos) {
        if (Dirty && !Ticking) {
            rebuild(World);
        }
        BlockPos Host = canonicalHost(World, DevicePos);
        for (BlockPos OutletPos : OutletsOnHost.getOrDefault(Host, List.of())) {
            WireNetwork Network = NetworkByOutlet.get(OutletPos);
            if (Network != null) {
                return new DeviceFlow(Network.getLastOutput(Host), Network.getLastInput(Host));
            }
        }
        return null;
    }

    private void tick(ServerLevel World) {
        SeenHosts.clear();
        checkChangedCells(World);
        validatePending(World);
        if (Dirty) {
            rebuild(World);
        }
        Set<WirePair> HotWires = new HashSet<>();
        Set<BlockPos> HotOutlets = new HashSet<>();
        Set<BlockPos> Overloaded = new HashSet<>();
        SwitchLoads.clear();
        List<WireNetwork> Current = List.copyOf(Networks);
        Ticking = true;
        try {
            for (WireNetwork Network : Current) {
                Network.refresh(World);
            }
            deliverMessages();
            for (WireNetwork Network : Current) {
                Network.tickPower();
                Network.getWireLoads().forEach((Pair, Load) -> {
                    if (Load > Network.getWireLimit(Pair) + 1.0E-6) {
                        HotWires.add(Pair);
                    }
                });
                Network.getOutletLoads().forEach((Outlet, Load) -> {
                    if (Load > ConnectorTier.Basic.getMaxDemand() + 1.0E-6 && Load > outletLimit(World, Outlet) + 1.0E-6) {
                        HotOutlets.add(Outlet);
                    }
                });
                for (WireNetwork.Bridge Link : Network.getBridges()) {
                    double Load = Network.getBridgeLoad(Link.Switch());
                    SwitchLoads.put(Link.Switch(), new double[]{Load, Link.Limit()});
                    if (Load > Link.Limit() + 1.0E-6) {
                        Overloaded.add(Link.Switch());
                    }
                }
            }
        } finally {
            Ticking = false;
        }
        finishCharging(World);
        for (BlockPos SwitchPos : strain(SwitchStrain, Overloaded, Wires.TripTicks)) {
            if (World.isLoaded(SwitchPos) && World.getBlockEntity(SwitchPos) instanceof BreakerBlockEntity Breaker) {
                Breaker.overloadTrip(World);
            }
        }
        for (WirePair Pair : strain(WireStrain, HotWires, Wires.BurnTicks)) {
            burnWire(World, Pair);
        }
        for (BlockPos Outlet : strain(OutletStrain, HotOutlets, Wires.BurnTicks)) {
            burnOutlet(World, Outlet);
        }
    }

    private static int outletLimit(ServerLevel World, BlockPos Outlet) {
        if (!World.isLoaded(Outlet)) {
            return ConnectorTier.Basic.getMaxDemand();
        }
        if (World.getBlockState(Outlet).getBlock() instanceof CreativeOutletBlock) {
            return Integer.MAX_VALUE;
        }
        if (World.getBlockEntity(Outlet) instanceof OutletBlockEntity Entity && Entity.getLinks().stream().anyMatch(Link -> Link.Tier() == WireTier.Creative)) {
            return Integer.MAX_VALUE;
        }
        if (World.getBlockState(Outlet).getBlock() instanceof WireConnector Connector) {
            return Connector.getConnectorTier().getMaxDemand();
        }
        return ConnectorTier.Basic.getMaxDemand();
    }

    private static <T> List<T> strain(Map<T, Integer> Strain, Set<T> Hot, int Limit) {
        Strain.keySet().retainAll(Hot);
        List<T> Burnt = new ArrayList<>();
        for (T Key : Hot) {
            if (Strain.merge(Key, 1, Integer::sum) >= Limit) {
                Burnt.add(Key);
            }
        }
        Burnt.forEach(Strain::remove);
        return Burnt;
    }

    private void burnWire(ServerLevel World, WirePair Pair) {
        WireSpan Span = Spans.get(Pair);
        unindexWire(Pair);
        detach(World, Pair.Low(), Pair.High());
        detach(World, Pair.High(), Pair.Low());
        Dirty = true;
        if (Span == null) {
            return;
        }
        Vec3 Run = Span.To().subtract(Span.From());
        int Puffs = Math.max(2, (int) Math.ceil(Run.length() * 2.0));
        for (int Puff = 0; Puff <= Puffs; Puff++) {
            Vec3 Point = Span.From().add(Run.scale((double) Puff / Puffs));
            World.sendParticles(ParticleTypes.SMOKE, Point.x, Point.y, Point.z, 2, 0.05, 0.05, 0.05, 0.01);
        }
        Vec3 Middle = Span.From().add(Run.scale(0.5));
        World.playSound(null, Middle.x, Middle.y, Middle.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.4f);
    }

    private void finishCharging(ServerLevel World) {
        Charging.keySet().retainAll(SeenHosts);
        Iterator<Map.Entry<BlockPos, ChargingRedstone>> Entries = Charging.entrySet().iterator();
        while (Entries.hasNext()) {
            Map.Entry<BlockPos, ChargingRedstone> Entry = Entries.next();
            if (!Entry.getValue().isCharged()) {
                continue;
            }
            Entries.remove();
            ChargingRedstone.convert(World, Entry.getKey());
        }
    }

    private void burnOutlet(ServerLevel World, BlockPos Outlet) {
        if (!World.isLoaded(Outlet) || !(World.getBlockState(Outlet).getBlock() instanceof WireConnector)) {
            return;
        }
        Vec3 Centre = Vec3.atCenterOf(Outlet);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Centre.x, Centre.y, Centre.z, 8, 0.15, 0.15, 0.15, 0.02);
        World.playSound(null, Centre.x, Centre.y, Centre.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 0.9f);
        World.destroyBlock(Outlet, false);
        Dirty = true;
    }

    private void checkChangedCells(ServerLevel World) {
        if (ChangedCells.isEmpty()) {
            return;
        }
        List<BlockPos> Cells = List.copyOf(ChangedCells);
        ChangedCells.clear();
        for (BlockPos Cell : Cells) {
            Set<WirePair> Pairs = SpanIndex.get(Cell.asLong());
            if (Pairs == null || !World.isLoaded(Cell)) {
                continue;
            }
            for (WirePair Pair : List.copyOf(Pairs)) {
                WireSpan Span = Spans.get(Pair);
                if (Span != null && WirePath.obstructs(World, Cell, Span.From(), Span.To())) {
                    snap(World, Span, Cell);
                }
            }
        }
    }

    private void validatePending(ServerLevel World) {
        int Budget = Wires.ValidationsPerTick;
        Iterator<WirePair> Pending = PendingValidation.iterator();
        while (Pending.hasNext() && Budget-- > 0) {
            WirePair Pair = Pending.next();
            Pending.remove();
            WireSpan Span = Spans.get(Pair);
            if (Span == null) {
                continue;
            }
            BlockPos Blocked = WirePath.firstObstruction(World, Span.From(), Span.To(), Pair.Low(), Pair.High());
            if (Blocked != null) {
                snap(World, Span, Blocked);
                Pending = PendingValidation.iterator();
            }
        }
    }

    private void snap(ServerLevel World, WireSpan Span, BlockPos At) {
        WirePair Pair = Span.Pair();
        unindexWire(Pair);
        detach(World, Pair.Low(), Pair.High());
        detach(World, Pair.High(), Pair.Low());
        Dirty = true;
        Wires.dropCoils(World, At, Span.Tier(), Wires.coilCost(Span.From().distanceTo(Span.To())));
        Wires.playSound(World, At, ModSounds.WireSnap.get());
    }

    private static void detach(ServerLevel World, BlockPos OutletPos, BlockPos Target) {
        if (World.isLoaded(OutletPos) && World.getBlockEntity(OutletPos) instanceof OutletBlockEntity Outlet) {
            Outlet.removeLink(Target);
        }
    }

    private void indexWire(WirePair Pair, Vec3 FirstAttach, Vec3 SecondAttach, WireTier Tier) {
        if (Spans.containsKey(Pair)) {
            return;
        }
        WireSpan Span = new WireSpan(Pair, FirstAttach, SecondAttach, WirePath.cells(FirstAttach, SecondAttach, Pair.Low(), Pair.High()), Tier);
        Spans.put(Pair, Span);
        for (BlockPos Cell : Span.Cells()) {
            SpanIndex.computeIfAbsent(Cell.asLong(), Key -> new HashSet<>()).add(Pair);
        }
    }

    private void unindexWire(WirePair Pair) {
        WireSpan Span = Spans.remove(Pair);
        PendingValidation.remove(Pair);
        if (Span == null) {
            return;
        }
        for (BlockPos Cell : Span.Cells()) {
            Set<WirePair> Pairs = SpanIndex.get(Cell.asLong());
            if (Pairs != null && Pairs.remove(Pair) && Pairs.isEmpty()) {
                SpanIndex.remove(Cell.asLong());
            }
        }
    }

    private void deliverMessages() {
        int Budget = Wires.MessagesPerTick;
        while (!Messages.isEmpty() && Budget-- > 0) {
            PendingMessage Pending = Messages.poll();
            WireNetwork Network = NetworkByOutlet.get(Pending.OutletPos());
            if (Network == null) {
                continue;
            }
            WireMessage Original = Pending.Message();
            Network.deliver(Original);
            if (!Original.Routed()) {
                for (WireNetwork Other : DataDomains.getOrDefault(Network, List.of())) {
                    if (Other != Network && Other.hasListeners(Original.Channel())) {
                        send(NetworkExits.get(Other), new WireMessage(Original.Channel(), Original.Payload().copy(), Original.Source(), true));
                    }
                }
            }
        }
    }

    private static BlockPos faceKey(BlockPos SwitchPos, Direction Face) {
        return new BlockPos(SwitchPos.getX(), SwitchPos.getY() + VirtualOffset * (Face.ordinal() + 1), SwitchPos.getZ());
    }

    private void rebuild(ServerLevel World) {
        Dirty = false;
        Networks.clear();
        NetworkByOutlet.clear();
        HostOf.clear();
        BankByMember.clear();
        OutletsOnHost.clear();
        Switches.clear();
        SwitchByKey.clear();
        for (BlockPos OutletPos : Outlets) {
            BlockState State = World.getBlockState(OutletPos);
            if (!(State.getBlock() instanceof WireConnector Connector)) {
                continue;
            }
            BlockPos Host = canonicalHost(World, Connector.hostPos(State, OutletPos));
            BlockState HostState = World.isLoaded(Host) ? World.getBlockState(Host) : null;
            if (HostState != null && HostState.getBlock() instanceof PowerSwitch Switch && State.hasProperty(OutletBlock.Facing)) {
                SwitchState Info = Switches.computeIfAbsent(Host, Key -> new SwitchState(Key, Switch.isClosed(HostState)));
                Host = faceKey(Info.Switch(), State.getValue(OutletBlock.Facing));
                SwitchByKey.put(Host, Info);
            }
            HostOf.put(OutletPos, Host);
            OutletsOnHost.computeIfAbsent(Host, Key -> new ArrayList<>()).add(OutletPos);
        }
        for (BlockPos Start : Outlets) {
            if (NetworkByOutlet.containsKey(Start)) {
                continue;
            }
            WireNetwork Network = new WireNetwork();
            ArrayDeque<BlockPos> Queue = new ArrayDeque<>();
            Queue.add(Start);
            NetworkByOutlet.put(Start, Network);
            while (!Queue.isEmpty()) {
                BlockPos Current = Queue.poll();
                Network.addOutlet(Current, HostOf.getOrDefault(Current, Current));
                for (BlockPos Next : neighbours(World, Current, Network)) {
                    if (Outlets.contains(Next) && !NetworkByOutlet.containsKey(Next)) {
                        NetworkByOutlet.put(Next, Network);
                        Queue.add(Next);
                    }
                }
            }
            Networks.add(Network);
        }
        buildDataDomains();
    }

    private int switchLimit(ServerLevel World, SwitchState Info) {
        int Limit = Integer.MAX_VALUE;
        for (BlockPos OutletPos : switchOutlets(Info.Switch())) {
            Limit = Math.min(Limit, outletLimit(World, OutletPos));
            if (World.getBlockEntity(OutletPos) instanceof OutletBlockEntity Outlet) {
                for (WireLink Link : Outlet.getLinks()) {
                    Limit = Math.min(Limit, Link.Tier().getMaxDemand());
                }
            }
        }
        return Limit;
    }

    private void buildDataDomains() {
        DataDomains.clear();
        NetworkExits.clear();
        Map<WireNetwork, WireNetwork> Parent = new HashMap<>();
        for (WireNetwork Network : Networks) {
            NetworkExits.put(Network, Network.getOutlets().iterator().next());
            Parent.put(Network, Network);
        }
        List<List<BlockPos>> Junctions = new ArrayList<>();
        for (BlockPos Router : Routers) {
            Junctions.add(OutletsOnHost.getOrDefault(Router, List.of()));
        }
        for (SwitchState Info : Switches.values()) {
            if (!Info.Closed()) {
                Junctions.add(switchOutlets(Info.Switch()));
            }
        }
        for (List<BlockPos> Junction : Junctions) {
            WireNetwork First = null;
            for (BlockPos OutletPos : Junction) {
                WireNetwork Network = NetworkByOutlet.get(OutletPos);
                if (Network == null) {
                    continue;
                }
                if (First == null) {
                    First = Network;
                } else {
                    Parent.put(findRoot(Parent, Network), findRoot(Parent, First));
                }
            }
        }
        Map<WireNetwork, List<WireNetwork>> Groups = new HashMap<>();
        for (WireNetwork Network : Networks) {
            Groups.computeIfAbsent(findRoot(Parent, Network), Key -> new ArrayList<>()).add(Network);
        }
        for (List<WireNetwork> Group : Groups.values()) {
            if (Group.size() > 1) {
                for (WireNetwork Network : Group) {
                    DataDomains.put(Network, Group);
                }
            }
        }
    }

    private static WireNetwork findRoot(Map<WireNetwork, WireNetwork> Parent, WireNetwork Network) {
        WireNetwork Root = Network;
        while (Parent.get(Root) != Root) {
            Root = Parent.get(Root);
        }
        WireNetwork Step = Network;
        while (Parent.get(Step) != Root) {
            WireNetwork Next = Parent.get(Step);
            Parent.put(Step, Root);
            Step = Next;
        }
        return Root;
    }

    private List<BlockPos> neighbours(ServerLevel World, BlockPos Current, WireNetwork Network) {
        List<BlockPos> Found = new ArrayList<>();
        BlockState State = World.getBlockState(Current);
        if (!(State.getBlock() instanceof WireConnector) || !(World.getBlockEntity(Current) instanceof OutletBlockEntity Outlet)) {
            return Found;
        }
        for (WireLink Link : List.copyOf(Outlet.getLinks())) {
            BlockPos Target = Link.Target();
            if (!World.isLoaded(Target)) {
                continue;
            }
            if (isDangling(World, Current, Target)) {
                Outlet.removeLink(Target);
                continue;
            }
            Found.add(Target);
            if (Outlets.contains(Target) && Current.asLong() < Target.asLong()) {
                Network.addEdge(new WireNetwork.Edge(WirePair.of(Current, Target), Current, HostOf.get(Current), Target, HostOf.getOrDefault(Target, Target), Link.Tier()));
            }
        }
        BlockPos Host = HostOf.get(Current);
        if (Host == null) {
            return Found;
        }
        if (!Routers.contains(Host)) {
            for (BlockPos Sibling : OutletsOnHost.getOrDefault(Host, List.of())) {
                if (!Sibling.equals(Current)) {
                    Found.add(Sibling);
                }
            }
        }
        SwitchState Info = SwitchByKey.get(Host);
        if (Info != null && Info.Closed()) {
            for (BlockPos Other : switchOutlets(Info.Switch())) {
                if (!Other.equals(Current)) {
                    Found.add(Other);
                }
            }
            if (!Network.hasBridge(Info.Switch())) {
                int Limit = switchLimit(World, Info);
                for (Direction Face : Direction.values()) {
                    BlockPos Key = faceKey(Info.Switch(), Face);
                    if (OutletsOnHost.containsKey(Key)) {
                        Network.addBridge(new WireNetwork.Bridge(Info.Switch(), Info.Switch(), Key, Limit));
                    }
                }
            }
        }
        return Found;
    }

    private static boolean isDangling(ServerLevel World, BlockPos From, BlockPos Target) {
        if (!(World.getBlockState(Target).getBlock() instanceof WireConnector)) {
            return true;
        }
        return World.getBlockEntity(Target) instanceof OutletBlockEntity Other && !Other.isLinkedTo(From);
    }

    public Set<WireNetwork> dataDomain(ServerLevel World, BlockPos DevicePos) {
        if (Dirty && !Ticking) {
            rebuild(World);
        }
        Set<WireNetwork> Reached = new LinkedHashSet<>();
        for (BlockPos OutletPos : outletsServing(canonicalHost(World, DevicePos))) {
            WireNetwork Network = NetworkByOutlet.get(OutletPos);
            if (Network != null && Reached.add(Network)) {
                Reached.addAll(DataDomains.getOrDefault(Network, List.of()));
            }
        }
        return Reached;
    }

    public SortedSet<Integer> listeningPorts(ServerLevel World, BlockPos DevicePos, String Type, int Min, int Max) {
        if (Dirty && !Ticking) {
            rebuild(World);
        }
        BlockPos Host = canonicalHost(World, DevicePos);
        Set<WireNetwork> Reached = new HashSet<>();
        for (BlockPos OutletPos : outletsServing(Host)) {
            WireNetwork Network = NetworkByOutlet.get(OutletPos);
            if (Network != null && Reached.add(Network)) {
                Reached.addAll(DataDomains.getOrDefault(Network, List.of()));
            }
        }
        TreeSet<Integer> Ports = new TreeSet<>();
        for (WireNetwork Network : Reached) {
            Network.collectListeningPorts(Type, Min, Max, Ports);
        }
        return Ports;
    }

    public record DeviceFlow(long Output, long Input) {
    }

    private record PendingMessage(BlockPos OutletPos, WireMessage Message) {
    }

    private record WireSpan(WirePair Pair, Vec3 From, Vec3 To, List<BlockPos> Cells, WireTier Tier) {
    }

    private record SwitchState(BlockPos Switch, boolean Closed) {
    }
}
