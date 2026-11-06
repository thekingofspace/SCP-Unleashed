package net.rai.scp_unleashed.computer.net;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.archive.database.DatabaseBlock;
import net.rai.scp_unleashed.block.entity.NetworkRouterBlockEntity;
import net.rai.scp_unleashed.computer.ChatServerBlockEntity;
import net.rai.scp_unleashed.computer.ComputerBlockEntity;
import net.rai.scp_unleashed.computer.MessageDatabaseBlockEntity;
import net.rai.scp_unleashed.computer.UploadPortBlockEntity;
import net.rai.scp_unleashed.nethub.NetHubs;
import net.rai.scp_unleashed.nethub.NetworkHubBlockEntity;
import net.rai.scp_unleashed.wire.PowerSwitch;
import net.rai.scp_unleashed.wire.WireConnector;
import net.rai.scp_unleashed.wire.WireNetwork;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

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

public final class ComputerGrid {
    public static final int MaxNetworks = 16;
    public static final int MaxDevices = 512;
    public static final int CacheTicks = 10;
    public static final int MaxCached = 256;
    private static final Map<ServerLevel, Map<BlockPos, Cached>> Cache = new WeakHashMap<>();

    private ComputerGrid() {
    }

    public static Reach reach(ServerLevel World, BlockPos Origin) {
        long Now = World.getGameTime();
        Map<BlockPos, Cached> Known = Cache.computeIfAbsent(World, Key -> new HashMap<>());
        Cached Found = Known.get(Origin);
        if (Found != null && Now - Found.At() < CacheTicks && Now >= Found.At()) {
            return Found.Result();
        }
        Reach Fresh = scan(World, Origin);
        if (Known.size() >= MaxCached) {
            Known.clear();
        }
        Known.put(Origin.immutable(), new Cached(Fresh, Now));
        return Fresh;
    }

    public static void invalidate(ServerLevel World) {
        Map<BlockPos, Cached> Known = Cache.get(World);
        if (Known != null) {
            Known.clear();
        }
    }

    public static void forget(ServerLevel World) {
        Cache.remove(World);
    }

    public static Reach local(ServerLevel World, BlockPos Origin) {
        return scan(World, Origin, false);
    }

    private static Reach scan(ServerLevel World, BlockPos Origin) {
        return scan(World, Origin, true);
    }

    private static Reach scan(ServerLevel World, BlockPos Origin, boolean Bridged) {
        WireNetworkManager Manager = WireNetworkManager.get(World);
        Set<WireNetwork> Networks = new LinkedHashSet<>();
        Set<BlockPos> Seen = new HashSet<>();
        Set<BlockPos> Visited = new HashSet<>();
        ArrayDeque<BlockPos> Hosts = new ArrayDeque<>();
        List<ComputerBlockEntity> Computers = new ArrayList<>();
        List<ChatServerBlockEntity> Chats = new ArrayList<>();
        List<MessageDatabaseBlockEntity> Databases = new ArrayList<>();
        List<UploadPortBlockEntity> Ports = new ArrayList<>();
        List<BlockPos> Hubs = new ArrayList<>();
        BlockPos Documents = null;
        boolean DocumentsOnline = false;
        Hosts.add(Origin);
        Visited.add(Origin);
        while (!Hosts.isEmpty() && Networks.size() < MaxNetworks) {
            BlockPos Host = Hosts.poll();
            for (BlockPos OutletPos : outletsOn(World, Manager, Host)) {
                WireNetwork Network = Manager.networkAt(World, OutletPos);
                if (Network == null || !Networks.add(Network)) {
                    continue;
                }
                boolean Live = Network.getLastSupply() + Network.getLastBackup() > 0;
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
                    if (!World.isLoaded(Device)) {
                        continue;
                    }
                    BlockState DeviceState = World.getBlockState(Device);
                    if (DeviceState.getBlock() instanceof DatabaseBlock && (Documents == null || !DocumentsOnline && Live)) {
                        Documents = DatabaseBlock.lowerPos(DeviceState, Device).immutable();
                        DocumentsOnline = Live;
                    }
                    if (!Seen.add(Device)) {
                        continue;
                    }
                    BlockEntity Entity = World.getBlockEntity(Device);
                    if (Entity instanceof ComputerBlockEntity Computer) {
                        Computers.add(Computer);
                    } else if (Entity instanceof ChatServerBlockEntity Chat) {
                        Chats.add(Chat);
                    } else if (Entity instanceof MessageDatabaseBlockEntity Database) {
                        Databases.add(Database);
                    } else if (Entity instanceof UploadPortBlockEntity Port) {
                        Ports.add(Port);
                    } else if (Bridged && Entity instanceof NetworkHubBlockEntity) {
                        Hubs.add(Device);
                    }
                    if ((Entity instanceof NetworkRouterBlockEntity || DeviceState.getBlock() instanceof PowerSwitch) && Visited.add(Device)) {
                        Hosts.add(Device);
                    }
                }
            }
        }
        for (NetHubs.FarEnd Far : NetHubs.farEnds(World, Hubs)) {
            Reach Remote = Far.isLive() ? scan(Far.World(), Far.Hub(), false) : new Reach(List.of(), List.of(), List.of(), List.of(), Far.Archive() ? Far.Hub() : null, Far.Archive());
            join(Computers, Remote.Computers());
            join(Chats, Remote.ChatServers());
            join(Databases, Remote.Databases());
            join(Ports, Remote.Ports());
            if (Remote.Documents() != null && (Documents == null || !DocumentsOnline && Remote.DocumentsOnline())) {
                Documents = Remote.Documents();
                DocumentsOnline = Remote.DocumentsOnline();
            }
        }
        Comparator<BlockEntity> Order = Comparator.comparingLong(Entity -> Entity.getBlockPos().asLong());
        Computers.sort(Order);
        Chats.sort(Order);
        Databases.sort(Order);
        Ports.sort(Order);
        return new Reach(List.copyOf(Computers), List.copyOf(Chats), List.copyOf(Databases), List.copyOf(Ports), Documents, DocumentsOnline);
    }

    private static <T> void join(List<T> Into, List<T> From) {
        for (T Each : From) {
            if (!Into.contains(Each)) {
                Into.add(Each);
            }
        }
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

    public record Reach(List<ComputerBlockEntity> Computers, List<ChatServerBlockEntity> ChatServers, List<MessageDatabaseBlockEntity> Databases,
                        List<UploadPortBlockEntity> Ports, @Nullable BlockPos Documents, boolean DocumentsOnline) {
        @Nullable
        public ChatServerBlockEntity chatServer() {
            for (ChatServerBlockEntity Server : ChatServers) {
                if (!Server.isRemoved() && Server.isRunning()) {
                    return Server;
                }
            }
            return null;
        }

        public List<ChatServerBlockEntity> runningChatServers() {
            List<ChatServerBlockEntity> Found = new ArrayList<>();
            for (ChatServerBlockEntity Server : ChatServers) {
                if (!Server.isRemoved() && Server.isRunning()) {
                    Found.add(Server);
                }
            }
            return Found;
        }

        public List<MessageDatabaseBlockEntity> runningDatabases() {
            List<MessageDatabaseBlockEntity> Found = new ArrayList<>();
            Set<String> Keys = new HashSet<>();
            for (MessageDatabaseBlockEntity Database : Databases) {
                if (!Database.isRemoved() && Database.isRunning() && Keys.add(Database.getKey())) {
                    Found.add(Database);
                }
            }
            return Found;
        }

        @Nullable
        public MessageDatabaseBlockEntity database(String Key) {
            for (MessageDatabaseBlockEntity Database : runningDatabases()) {
                if (Database.getKey().equals(Key)) {
                    return Database;
                }
            }
            return null;
        }

        public List<UploadPortBlockEntity> runningPorts() {
            List<UploadPortBlockEntity> Found = new ArrayList<>();
            for (UploadPortBlockEntity Port : Ports) {
                if (!Port.isRemoved() && Port.isRunning()) {
                    Found.add(Port);
                }
            }
            return Found;
        }

        @Nullable
        public UploadPortBlockEntity port(BlockPos Pos) {
            for (UploadPortBlockEntity Port : runningPorts()) {
                if (Port.getBlockPos().equals(Pos)) {
                    return Port;
                }
            }
            return null;
        }

        public int runningComputers() {
            int Count = 0;
            for (ComputerBlockEntity Computer : Computers) {
                if (!Computer.isRemoved() && Computer.isRunning()) {
                    Count++;
                }
            }
            return Count;
        }

        public boolean reaches(BlockPos Pos) {
            for (ComputerBlockEntity Computer : Computers) {
                if (Computer.getBlockPos().equals(Pos)) {
                    return true;
                }
            }
            return false;
        }

        public boolean reaches(ResourceKey<Level> Dimension, BlockPos Pos) {
            for (ComputerBlockEntity Computer : Computers) {
                if (Computer.getBlockPos().equals(Pos) && Computer.getLevel() != null && Computer.getLevel().dimension() == Dimension) {
                    return true;
                }
            }
            return false;
        }

        public boolean hasDocuments() {
            return Documents != null && DocumentsOnline;
        }
    }

    private record Cached(Reach Result, long At) {
    }
}
