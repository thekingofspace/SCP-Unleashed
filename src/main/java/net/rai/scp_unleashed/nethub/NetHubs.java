package net.rai.scp_unleashed.nethub;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.rai.scp_unleashed.computer.net.ComputerGrid;
import net.rai.scp_unleashed.mining.MiningGrid;
import net.rai.scp_unleashed.wire.WireMessage;
import net.rai.scp_unleashed.wire.WireNetwork;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NetHubs {
    public static final int RelayBand = 1 << 26;
    public static final int RelayBands = 15;
    public static final int LiveTicks = 3;
    public static final int MaxFarEnds = 16;
    private static final Map<HubKey, NetworkHubBlockEntity> Live = new HashMap<>();
    private static long Revision;
    private static long SightingTick = Long.MIN_VALUE;
    private static Set<String> SeenNow = new HashSet<>();
    private static Set<String> SeenBefore = new HashSet<>();

    private NetHubs() {
    }

    public static HubKey keyOf(Level World, BlockPos Pos) {
        return new HubKey(World.dimension(), Pos.immutable());
    }

    public static long revision() {
        return Revision;
    }

    static void loaded(ServerLevel World, NetworkHubBlockEntity Hub) {
        if (Live.put(keyOf(World, Hub.getBlockPos()), Hub) != Hub) {
            changed(World.getServer());
        }
    }

    static void unloaded(ServerLevel World, NetworkHubBlockEntity Hub) {
        if (Live.remove(keyOf(World, Hub.getBlockPos()), Hub)) {
            changed(World.getServer());
        }
    }

    static void removed(ServerLevel World, BlockPos Origin) {
        HubKey Key = keyOf(World, Origin);
        Live.remove(Key);
        NetHubLedger.get(World.getServer()).remove(Key);
        changed(World.getServer());
    }

    static void changed(@Nullable MinecraftServer Server) {
        Revision++;
        if (Server == null) {
            return;
        }
        for (ServerLevel Each : Server.getAllLevels()) {
            ComputerGrid.invalidate(Each);
            MiningGrid.forget(Each);
        }
    }

    static void forgetAll() {
        Live.clear();
        SeenNow = new HashSet<>();
        SeenBefore = new HashSet<>();
        SightingTick = Long.MIN_VALUE;
        Revision++;
    }

    static void forgetLevel(ServerLevel World) {
        if (Live.keySet().removeIf(Key -> Key.Dimension() == World.dimension())) {
            Revision++;
        }
    }

    public static boolean isLive(@Nullable NetworkHubBlockEntity Hub) {
        return Hub != null && !Hub.isRemoved() && Hub.getLevel() instanceof ServerLevel World && World.getGameTime() - Hub.getLastTick() <= LiveTicks;
    }

    public static List<Member> members(MinecraftServer Server, int Port, HubKey Self) {
        List<Member> Found = new ArrayList<>();
        NetHubLedger Ledger = NetHubLedger.get(Server);
        for (Map.Entry<HubKey, NetHubLedger.Entry> Each : Ledger.onPort(Port)) {
            if (Each.getKey().equals(Self)) {
                continue;
            }
            NetworkHubBlockEntity Hub = Live.get(Each.getKey());
            if (Hub == null && isStale(Server, Each.getKey())) {
                Ledger.remove(Each.getKey());
                continue;
            }
            if (Hub != null && Hub.getPort() != Port) {
                continue;
            }
            Found.add(new Member(Each.getKey(), isLive(Hub) ? Hub : null, Each.getValue()));
        }
        Found.sort(Comparator.comparing((Member Each) -> Each.Key().Dimension().location().toString()).thenComparingLong(Each -> Each.Key().Pos().asLong()));
        return Found;
    }

    private static boolean isStale(MinecraftServer Server, HubKey Key) {
        ServerLevel World = Server.getLevel(Key.Dimension());
        LevelChunk Chunk = World == null ? null : World.getChunkSource().getChunkNow(Key.Pos().getX() >> 4, Key.Pos().getZ() >> 4);
        if (Chunk == null) {
            return false;
        }
        BlockState Found = Chunk.getBlockState(Key.Pos());
        return !(Found.getBlock() instanceof NetworkHubBlock) || Found.getValue(NetworkHubBlock.Part) != 0;
    }

    public static List<FarEnd> farEnds(ServerLevel World, List<BlockPos> Hubs) {
        if (Hubs.isEmpty()) {
            return List.of();
        }
        List<FarEnd> Found = new ArrayList<>();
        Set<HubKey> Seen = new HashSet<>();
        for (BlockPos Pos : Hubs) {
            Seen.add(keyOf(World, Pos));
        }
        for (BlockPos Pos : Hubs) {
            if (!(World.getBlockEntity(Pos) instanceof NetworkHubBlockEntity Hub) || !Hub.isBridgeReady()) {
                continue;
            }
            for (Member Far : members(World.getServer(), Hub.getPort(), Hub.key())) {
                if (Found.size() >= MaxFarEnds) {
                    return Found;
                }
                if (!Far.bridging() || !Seen.add(Far.Key())) {
                    continue;
                }
                Found.add(Far.isLive() ? new FarEnd(Far.Key(), (ServerLevel) Far.Hub().getLevel(), Far.Hub().hasArchive())
                        : new FarEnd(Far.Key(), null, Far.Saved().Archive()));
            }
        }
        return Found;
    }

    static void relay(ServerLevel World, NetworkHubBlockEntity From, WireMessage Message) {
        if (!From.isBridgeReady() || isRelayed(Message.Source())) {
            return;
        }
        String Mark = From.getPort() + "|" + World.dimension().location() + "|" + Message.Channel() + "|" + Message.Source().asLong() + "|" + Message.Payload().hashCode();
        if (!firstSighting(Mark, World.getGameTime())) {
            return;
        }
        Set<WireNetwork> Covered = new HashSet<>(WireNetworkManager.get(World).dataDomain(World, From.getBlockPos()));
        BlockPos Source = relaySource(World.dimension(), Message.Source());
        for (Member Far : members(World.getServer(), From.getPort(), From.key())) {
            if (!Far.bridging()) {
                continue;
            }
            if (!Far.isLive()) {
                From.noteMissed(Far.Key());
                continue;
            }
            NetworkHubBlockEntity Target = Far.Hub();
            if (!(Target.getLevel() instanceof ServerLevel FarWorld)) {
                continue;
            }
            WireNetworkManager Manager = WireNetworkManager.get(FarWorld);
            Set<WireNetwork> Domain = Manager.dataDomain(FarWorld, Target.getBlockPos());
            if (Domain.isEmpty() || !Collections.disjoint(Covered, Domain)) {
                continue;
            }
            Covered.addAll(Domain);
            BlockPos Exit = Target.exitOutlet(FarWorld);
            if (Exit != null && Manager.send(Exit, new WireMessage(Message.Channel(), Message.Payload().copy(), Source, false))) {
                From.noteRelayed();
                Target.noteReceived();
            }
        }
    }

    private static boolean firstSighting(String Mark, long Now) {
        if (Now != SightingTick) {
            SeenBefore = Now == SightingTick + 1 ? SeenNow : new HashSet<>();
            SeenNow = new HashSet<>();
            SightingTick = Now;
        }
        if (SeenBefore.contains(Mark) || SeenNow.contains(Mark)) {
            return false;
        }
        SeenNow.add(Mark);
        return true;
    }

    public static BlockPos relaySource(ResourceKey<Level> Dimension, BlockPos Source) {
        int Band = Math.floorMod(Dimension.location().hashCode(), RelayBands) + 1;
        return new BlockPos(Source.getX(), Math.floorMod(Source.getY(), RelayBand) + Band * RelayBand, Source.getZ());
    }

    public static boolean isRelayed(BlockPos Source) {
        return Source.getY() >= RelayBand;
    }

    public record HubKey(ResourceKey<Level> Dimension, BlockPos Pos) {
    }

    public record Member(HubKey Key, @Nullable NetworkHubBlockEntity Hub, NetHubLedger.Entry Saved) {
        public boolean isLive() {
            return Hub != null;
        }

        public boolean bridging() {
            return Hub != null ? Hub.isBridgeReady() : Saved.Powered();
        }

        public boolean powered() {
            return Hub != null ? Hub.isPowered() : Saved.Powered();
        }

        public String name() {
            return Hub != null ? Hub.getNetworkName() : Saved.Name();
        }

        public int devices() {
            return Hub != null ? Hub.getDeviceCount() : Saved.Devices();
        }

        public List<String> subscriptions() {
            return Hub != null ? Hub.localSubscriptions() : Saved.Subscriptions();
        }
    }

    public record FarEnd(HubKey Key, @Nullable ServerLevel World, boolean Archive) {
        public boolean isLive() {
            return World != null;
        }

        public BlockPos Hub() {
            return Key.Pos();
        }
    }
}
