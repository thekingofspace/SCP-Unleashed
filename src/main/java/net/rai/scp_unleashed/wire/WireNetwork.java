package net.rai.scp_unleashed.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class WireNetwork {
    private final Set<BlockPos> Outlets = new LinkedHashSet<>();
    private final Map<BlockPos, BlockPos> OutletHosts = new HashMap<>();
    private final List<Edge> Edges = new ArrayList<>();
    private final List<Bridge> Bridges = new ArrayList<>();
    private final Map<WirePair, Integer> WireLimits = new HashMap<>();
    private final Map<BlockPos, Double> BridgeLoads = new HashMap<>();
    private final Map<WirePair, Double> WireLoads = new HashMap<>();
    private final Map<BlockPos, Double> OutletLoads = new HashMap<>();
    private final Map<String, List<BlockPos>> Listeners = new HashMap<>();
    private final TreeMap<Integer, Set<String>> PortTypes = new TreeMap<>();
    private Map<BlockPos, Double> SolvedInjections = Map.of();
    private Map<BlockPos, WireDevice> Snapshot = Map.of();
    private List<BlockPos> LastHosts = List.of();
    private long[] LastOutputs = new long[0];
    private long[] LastInputs = new long[0];
    private int LastDeviceCount;
    private long LastSupply;
    private long LastDemand;
    private long LastStorageIn;
    private long LastBackup;
    private long LastSinkWeight;
    private long LastFromBackup;
    private long LastToStorage;
    private long LastSunk;
    private long LastShortfall;
    private long LastTransferred;
    private long LastStored;
    private long LastCapacity;

    void addOutlet(BlockPos Pos) {
        Outlets.add(Pos);
    }

    void addOutlet(BlockPos Pos, BlockPos Host) {
        Outlets.add(Pos);
        OutletHosts.put(Pos, Host);
    }

    void addEdge(Edge Wire) {
        Edges.add(Wire);
        WireLimits.merge(Wire.Pair(), Wire.Tier().getMaxDemand(), Math::min);
    }

    void addBridge(Bridge Link) {
        Bridges.add(Link);
    }

    boolean hasBridge(BlockPos SwitchPos) {
        for (Bridge Link : Bridges) {
            if (Link.Switch().equals(SwitchPos)) {
                return true;
            }
        }
        return false;
    }

    public List<Bridge> getBridges() {
        return Collections.unmodifiableList(Bridges);
    }

    public double getBridgeLoad(BlockPos SwitchPos) {
        return BridgeLoads.getOrDefault(SwitchPos, 0.0);
    }

    public int getWireLimit(WirePair Pair) {
        return WireLimits.getOrDefault(Pair, WireTier.Coated.getMaxDemand());
    }

    public Set<BlockPos> getOutlets() {
        return Collections.unmodifiableSet(Outlets);
    }

    public Map<BlockPos, WireDevice> findDevices(ServerLevel World) {
        Map<BlockPos, WireDevice> Devices = new LinkedHashMap<>();
        for (BlockPos OutletPos : Outlets) {
            BlockState State = World.getBlockState(OutletPos);
            if (!(State.getBlock() instanceof WireConnector Connector)) {
                continue;
            }
            WireNetworkManager Manager = WireNetworkManager.get(World);
            BlockPos HostPos = Manager.canonicalHost(World, Connector.hostPos(State, OutletPos));
            if (!HostPos.equals(OutletPos) && World.getBlockEntity(OutletPos) instanceof OutletDevice Own) {
                Devices.putIfAbsent(OutletPos.immutable(), Own);
            }
            if (Devices.containsKey(HostPos) || !World.isLoaded(HostPos)) {
                continue;
            }
            WireDevice Device = Manager.deviceAt(World, HostPos);
            if (Device != null) {
                Devices.put(HostPos, Device);
            }
        }
        return Devices;
    }

    void refresh(ServerLevel World) {
        Snapshot = findDevices(World);
        Listeners.clear();
        PortTypes.clear();
        Snapshot.forEach((Host, Device) -> {
            for (String Subscription : Device.getSubscriptions()) {
                if (!WireChannels.isValidSubscription(Subscription)) {
                    continue;
                }
                List<BlockPos> Hosts = Listeners.computeIfAbsent(Subscription, Key -> new ArrayList<>());
                if (!Hosts.contains(Host)) {
                    Hosts.add(Host);
                }
                int SubscribedPort = WireChannels.portOf(Subscription);
                if (SubscribedPort >= 0) {
                    PortTypes.computeIfAbsent(SubscribedPort, Key -> new HashSet<>()).add(WireChannels.typeOf(Subscription));
                }
            }
        });
    }

    public void collectListeningPorts(String Type, int Min, int Max, Set<Integer> Into) {
        for (Map.Entry<Integer, Set<String>> Entry : PortTypes.subMap(Min, true, Max, true).entrySet()) {
            if (Entry.getValue().contains(Type) || Entry.getValue().contains(WireChannels.Everything)) {
                Into.add(Entry.getKey());
            }
        }
    }

    public boolean hasListeners(String Channel) {
        for (String Subscription : WireChannels.subscriptionsMatching(Channel)) {
            if (Listeners.containsKey(Subscription)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getSubscriptions() {
        return Collections.unmodifiableSet(Listeners.keySet());
    }

    int deliver(WireMessage Message) {
        Set<BlockPos> Reached = new HashSet<>();
        for (String Subscription : WireChannels.subscriptionsMatching(Message.Channel())) {
            for (BlockPos Host : Listeners.getOrDefault(Subscription, List.of())) {
                WireDevice Device = Snapshot.get(Host);
                if (Device != null && !Host.equals(Message.Source()) && Reached.add(Host)) {
                    Device.receiveMessage(new WireMessage(Message.Channel(), Message.Payload().copy(), Message.Source(), Message.Routed()));
                }
            }
        }
        return Reached.size();
    }

    void tickPower() {
        List<BlockPos> Hosts = new ArrayList<>(Snapshot.keySet());
        List<WireDevice> Devices = new ArrayList<>(Snapshot.values());
        PowerSplit Split = PowerSplit.run(Devices);
        long[] Outputs = Split.Outputs();
        long[] Inputs = Split.Inputs();
        LastHosts = Hosts;
        LastOutputs = Outputs;
        LastInputs = Inputs;
        Map<BlockPos, Double> Injections = new HashMap<>();
        for (int Index = 0; Index < Hosts.size(); Index++) {
            long Net = Outputs[Index] - Inputs[Index];
            if (Net != 0) {
                Injections.merge(Hosts.get(Index), (double) Net, Double::sum);
            }
        }
        computeLoads(Injections);
        LastDeviceCount = Devices.size();
        LastSupply = Split.Supply();
        LastDemand = Split.Demand();
        LastStorageIn = Split.StorageIn();
        LastBackup = Split.Backup();
        LastSinkWeight = Split.SinkWeight();
        LastFromBackup = Split.FromBackup();
        LastToStorage = Split.ToStorage();
        LastSunk = Split.Sunk();
        LastShortfall = Split.getShortfall();
        LastTransferred = Split.getDelivered();
        LastStored = Split.Stored();
        LastCapacity = Split.Capacity();
    }

    private void computeLoads(Map<BlockPos, Double> Injections) {
        if (Injections.equals(SolvedInjections)) {
            return;
        }
        SolvedInjections = Injections;
        WireLoads.clear();
        OutletLoads.clear();
        BridgeLoads.clear();
        if (Injections.isEmpty() || (Edges.isEmpty() && Bridges.isEmpty())) {
            return;
        }
        Map<BlockPos, Integer> NodeIndex = new HashMap<>();
        for (Edge Wire : Edges) {
            NodeIndex.putIfAbsent(Wire.HostA(), NodeIndex.size());
            NodeIndex.putIfAbsent(Wire.HostB(), NodeIndex.size());
        }
        for (Bridge Link : Bridges) {
            NodeIndex.putIfAbsent(Link.SideA(), NodeIndex.size());
            NodeIndex.putIfAbsent(Link.SideB(), NodeIndex.size());
        }
        double[] Injection = new double[NodeIndex.size()];
        Injections.forEach((Host, Amount) -> {
            Integer Node = NodeIndex.containsKey(Host) ? NodeIndex.get(Host) : NodeIndex.get(OutletHosts.getOrDefault(Host, Host));
            if (Node != null) {
                Injection[Node] += Amount;
            }
        });
        int WireCount = Edges.size();
        int[] From = new int[WireCount + Bridges.size()];
        int[] To = new int[WireCount + Bridges.size()];
        double[] Conductance = new double[WireCount + Bridges.size()];
        for (int Index = 0; Index < WireCount; Index++) {
            From[Index] = NodeIndex.get(Edges.get(Index).HostA());
            To[Index] = NodeIndex.get(Edges.get(Index).HostB());
            Conductance[Index] = WireFlow.conductance(Edges.get(Index).Tier().getMaxDemand());
        }
        for (int Index = 0; Index < Bridges.size(); Index++) {
            From[WireCount + Index] = NodeIndex.get(Bridges.get(Index).SideA());
            To[WireCount + Index] = NodeIndex.get(Bridges.get(Index).SideB());
            Conductance[WireCount + Index] = WireFlow.conductance(Bridges.get(Index).Limit());
        }
        double[] Flows = WireFlow.solve(NodeIndex.size(), From, To, Conductance, Injection);
        Map<BlockPos, double[]> Through = new HashMap<>();
        for (int Index = 0; Index < Edges.size(); Index++) {
            Edge Wire = Edges.get(Index);
            WireLoads.merge(Wire.Pair(), Math.abs(Flows[Index]), Double::sum);
            passThrough(Through, Wire.OutletA(), Flows[Index]);
            passThrough(Through, Wire.OutletB(), -Flows[Index]);
        }
        Through.forEach((Outlet, Totals) -> OutletLoads.put(Outlet, Math.max(Totals[0], Totals[1])));
        for (int Index = 0; Index < Bridges.size(); Index++) {
            BridgeLoads.merge(Bridges.get(Index).Switch(), Math.abs(Flows[WireCount + Index]), Math::max);
        }
    }

    private static void passThrough(Map<BlockPos, double[]> Through, BlockPos Outlet, double Outgoing) {
        double[] Totals = Through.computeIfAbsent(Outlet, Key -> new double[2]);
        if (Outgoing > 0.0) {
            Totals[1] += Outgoing;
        } else {
            Totals[0] -= Outgoing;
        }
    }

    public Map<WirePair, Double> getWireLoads() {
        return Collections.unmodifiableMap(WireLoads);
    }

    public Map<BlockPos, Double> getOutletLoads() {
        return Collections.unmodifiableMap(OutletLoads);
    }

    public double getOutletLoad(BlockPos Outlet) {
        return OutletLoads.getOrDefault(Outlet, 0.0);
    }

    public long getLastOutput(BlockPos Host) {
        int Index = LastHosts.indexOf(Host);
        return Index < 0 ? 0 : LastOutputs[Index];
    }

    public long getLastInput(BlockPos Host) {
        int Index = LastHosts.indexOf(Host);
        return Index < 0 ? 0 : LastInputs[Index];
    }

    public int getLastDeviceCount() {
        return LastDeviceCount;
    }

    public long getLastSupply() {
        return LastSupply;
    }

    public long getLastDemand() {
        return LastDemand;
    }

    public long getLastStorageIn() {
        return LastStorageIn;
    }

    public long getLastBackup() {
        return LastBackup;
    }

    public long getLastSinkWeight() {
        return LastSinkWeight;
    }

    public long getLastFromBackup() {
        return LastFromBackup;
    }

    public long getLastToStorage() {
        return LastToStorage;
    }

    public long getLastSunk() {
        return LastSunk;
    }

    public long getLastShortfall() {
        return LastShortfall;
    }

    public long getLastTransferred() {
        return LastTransferred;
    }

    public long getLastStored() {
        return LastStored;
    }

    public long getLastCapacity() {
        return LastCapacity;
    }

    public record Edge(WirePair Pair, BlockPos OutletA, BlockPos HostA, BlockPos OutletB, BlockPos HostB, WireTier Tier) {
    }

    public record Bridge(BlockPos Switch, BlockPos SideA, BlockPos SideB, int Limit) {
    }
}
