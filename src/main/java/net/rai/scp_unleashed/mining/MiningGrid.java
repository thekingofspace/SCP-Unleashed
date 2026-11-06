package net.rai.scp_unleashed.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.block.entity.NetworkRouterBlockEntity;
import net.rai.scp_unleashed.nethub.NetHubs;
import net.rai.scp_unleashed.nethub.NetworkHubBlockEntity;
import net.rai.scp_unleashed.wire.PowerSwitch;
import net.rai.scp_unleashed.wire.WireConnector;
import net.rai.scp_unleashed.wire.WireNetwork;
import net.rai.scp_unleashed.wire.WireNetworkManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class MiningGrid {
    public static final int MaxNetworks = 16;
    public static final int MaxDevices = 512;
    public static final int CacheTicks = 10;
    public static final int MaxCached = 256;
    private static final Map<ServerLevel, Map<BlockPos, Cached>> Cache = new WeakHashMap<>();

    private MiningGrid() {
    }

    public static List<PointBankBlockEntity> banks(ServerLevel World, BlockPos Origin) {
        long Now = World.getGameTime();
        Map<BlockPos, Cached> Known = Cache.computeIfAbsent(World, Key -> new HashMap<>());
        Cached Found = Known.get(Origin);
        if (Found != null && Now - Found.At() < CacheTicks && Now >= Found.At()) {
            return Found.Banks();
        }
        List<PointBankBlockEntity> Fresh = scan(World, Origin);
        if (Known.size() >= MaxCached) {
            Known.clear();
        }
        Known.put(Origin.immutable(), new Cached(Fresh, Now));
        return Fresh;
    }

    public static List<PointBankBlockEntity> runningBanks(ServerLevel World, BlockPos Origin) {
        List<PointBankBlockEntity> Running = new ArrayList<>();
        for (PointBankBlockEntity Bank : banks(World, Origin)) {
            if (!Bank.isRemoved() && Bank.isRunning()) {
                Running.add(Bank);
            }
        }
        return Running;
    }

    public static long total(List<PointBankBlockEntity> Banks) {
        long Sum = 0L;
        for (PointBankBlockEntity Bank : Banks) {
            Sum += Bank.getBalance();
        }
        return Sum;
    }

    public static void forget(ServerLevel World) {
        Cache.remove(World);
    }

    private static List<PointBankBlockEntity> scan(ServerLevel World, BlockPos Origin) {
        return scan(World, Origin, true);
    }

    private static List<PointBankBlockEntity> scan(ServerLevel World, BlockPos Origin, boolean Bridged) {
        WireNetworkManager Manager = WireNetworkManager.get(World);
        Set<WireNetwork> Networks = new LinkedHashSet<>();
        Set<BlockPos> Seen = new HashSet<>();
        Set<BlockPos> Visited = new HashSet<>();
        ArrayDeque<BlockPos> Hosts = new ArrayDeque<>();
        List<PointBankBlockEntity> Banks = new ArrayList<>();
        List<BlockPos> Hubs = new ArrayList<>();
        Hosts.add(Origin);
        Visited.add(Origin);
        while (!Hosts.isEmpty() && Networks.size() < MaxNetworks) {
            BlockPos Host = Hosts.poll();
            for (BlockPos OutletPos : outletsOn(World, Manager, Host)) {
                WireNetwork Network = Manager.networkAt(World, OutletPos);
                if (Network == null || !Networks.add(Network)) {
                    continue;
                }
                for (BlockPos Each : Network.getOutlets()) {
                    if (Seen.size() >= MaxDevices) {
                        break;
                    }
                    if (!World.isLoaded(Each)) {
                        continue;
                    }
                    BlockState State = World.getBlockState(Each);
                    if (!(State.getBlock() instanceof WireConnector Connector)) {
                        continue;
                    }
                    BlockPos Device = Manager.canonicalHost(World, Connector.hostPos(State, Each));
                    if (!World.isLoaded(Device) || !Seen.add(Device)) {
                        continue;
                    }
                    BlockEntity Found = World.getBlockEntity(Device);
                    if (Found instanceof PointBankBlockEntity Bank) {
                        Banks.add(Bank);
                    } else if (Bridged && Found instanceof NetworkHubBlockEntity) {
                        Hubs.add(Device);
                    }
                    if ((Found instanceof NetworkRouterBlockEntity || World.getBlockState(Device).getBlock() instanceof PowerSwitch) && Visited.add(Device)) {
                        Hosts.add(Device);
                    }
                }
            }
        }
        for (NetHubs.FarEnd Far : NetHubs.farEnds(World, Hubs)) {
            if (Far.isLive()) {
                for (PointBankBlockEntity Bank : scan(Far.World(), Far.Hub(), false)) {
                    if (!Banks.contains(Bank)) {
                        Banks.add(Bank);
                    }
                }
            }
        }
        Banks.sort(Comparator.comparingLong(Bank -> Bank.getBlockPos().asLong()));
        return List.copyOf(Banks);
    }

    private static List<BlockPos> outletsOn(ServerLevel World, WireNetworkManager Manager, BlockPos Host) {
        List<BlockPos> Found = new ArrayList<>();
        for (Direction Side : Direction.values()) {
            BlockPos Next = Host.relative(Side);
            if (!World.isLoaded(Next)) {
                continue;
            }
            BlockState State = World.getBlockState(Next);
            if (State.getBlock() instanceof WireConnector Connector && Manager.canonicalHost(World, Connector.hostPos(State, Next)).equals(Host)) {
                Found.add(Next.immutable());
            }
        }
        return Found;
    }

    private record Cached(List<PointBankBlockEntity> Banks, long At) {
    }
}
