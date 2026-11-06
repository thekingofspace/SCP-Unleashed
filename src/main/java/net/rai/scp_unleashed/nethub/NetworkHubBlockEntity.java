package net.rai.scp_unleashed.nethub;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.computer.net.ComputerGrid;
import net.rai.scp_unleashed.nethub.client.NetHubClientHooks;
import net.rai.scp_unleashed.wire.WireChannels;
import net.rai.scp_unleashed.wire.WireConnector;
import net.rai.scp_unleashed.wire.WireDevice;
import net.rai.scp_unleashed.wire.WireMessage;
import net.rai.scp_unleashed.wire.WireNetwork;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class NetworkHubBlockEntity extends BlockEntity implements WireDevice {
    public static final int Draw = 160;
    public static final int PowerGrace = 2;
    public static final int LoadGrace = 20;
    public static final int RefreshTicks = 10;
    public static final int ScanTicks = 20;
    public static final int ArchiveTicks = 40;
    public static final int SyncTicks = 20;
    public static final int MaxViews = 16;
    public static final String NoNetwork = "----";
    public static final int ViewLinked = 0;
    public static final int ViewUnloaded = 1;
    public static final int ViewNoPower = 2;

    private int Port;
    private int Received;
    private long LastFed = Long.MIN_VALUE / 2;
    private long LastTick = Long.MIN_VALUE / 2;
    private boolean Powered;
    private boolean Restored;
    private boolean Bridging;
    private long SeenRevision = -1;
    private long NextRefresh;
    private long NextScan;
    private long NextArchive;
    private String NetworkName = NoNetwork;
    private int DeviceCount;
    private List<String> LocalSubscriptions = List.of();
    private Set<String> Mirrored = Set.of();
    private boolean Archive;
    @Nullable
    private BlockPos ExitOutlet;
    private long Relayed;
    private long Delivered;
    private final Map<NetHubs.HubKey, Integer> Missed = new HashMap<>();
    private List<FarView> Views = List.of();
    private String DimensionName = "";
    private boolean NeedsSync;
    private long LastSync = Long.MIN_VALUE / 2;

    public NetworkHubBlockEntity(BlockPos Pos, BlockState State) {
        super(NetHubRegistry.NetworkHubEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, NetworkHubBlockEntity Hub) {
        if (World instanceof ServerLevel Server) {
            Hub.tickServer(Server, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, NetworkHubBlockEntity Hub) {
        NetHubClientHooks.tick(Hub, State);
    }

    private void tickServer(ServerLevel World, BlockState State) {
        long Now = World.getGameTime();
        if (Now - LastTick > NetHubs.LiveTicks) {
            if (Restored && Powered) {
                LastFed = Now + LoadGrace;
            }
            Restored = false;
            LastTick = Now;
            NetHubs.loaded(World, this);
            NetHubs.changed(World.getServer());
        }
        LastTick = Now;
        boolean Fed = Received >= Draw;
        Received = 0;
        if (Fed) {
            LastFed = Math.max(LastFed, Now);
        }
        boolean Lit = Now - LastFed <= PowerGrace;
        if (Lit != Powered) {
            Powered = Lit;
            setChanged();
            NeedsSync = true;
            NetHubs.changed(World.getServer());
        }
        if (NetHubs.revision() != SeenRevision || Now >= NextRefresh) {
            refresh(World, Now, State);
        }
        if (NeedsSync && Now - LastSync >= SyncTicks) {
            NeedsSync = false;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, State, State, Block.UPDATE_CLIENTS);
        }
    }

    private void refresh(ServerLevel World, long Now, BlockState State) {
        SeenRevision = NetHubs.revision();
        NextRefresh = Now + RefreshTicks;
        WireNetworkManager Manager = WireNetworkManager.get(World);
        Set<WireNetwork> Domain = Manager.dataDomain(World, worldPosition);
        if (Now >= NextScan) {
            NextScan = Now + ScanTicks;
            scanDomain(World, Domain);
        }
        if (Now >= NextArchive) {
            NextArchive = Now + ArchiveTicks;
            Archive = Powered && !Domain.isEmpty() && ComputerGrid.local(World, worldPosition).hasDocuments();
        }
        ExitOutlet = findOutlet(World, Manager);
        DimensionName = World.dimension().location().toString();
        Set<String> Wanted = new TreeSet<>();
        boolean Linked = false;
        List<FarView> Shown = new ArrayList<>();
        for (NetHubs.Member Far : NetHubs.members(World.getServer(), Port, key())) {
            int Kind = Far.isLive() ? (Far.bridging() ? ViewLinked : ViewNoPower) : (Far.powered() ? ViewUnloaded : ViewNoPower);
            if (Far.bridging()) {
                Wanted.addAll(Far.subscriptions());
                Linked |= Far.isLive();
            }
            if (Shown.size() < MaxViews) {
                Shown.add(new FarView(Far.Key().Dimension().location().toString(), Far.Key().Pos(), Far.name(), Far.devices(), Kind, Missed.getOrDefault(Far.Key(), 0)));
            }
        }
        Missed.keySet().removeIf(Key -> Shown.stream().noneMatch(View -> View.Pos().equals(Key.Pos()) && View.Dimension().equals(Key.Dimension().location().toString())));
        Mirrored = Powered ? Set.copyOf(Wanted) : Set.of();
        boolean NowBridging = Powered && Linked;
        if (!Shown.equals(Views) || NowBridging != Bridging) {
            Views = List.copyOf(Shown);
            NeedsSync = true;
        }
        if (NowBridging != Bridging) {
            Vec3 Centre = getCentre();
            World.playSound(null, Centre.x, Centre.y, Centre.z, (NowBridging ? NetHubRegistry.Link : NetHubRegistry.Unlink).get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        Bridging = NowBridging;
        NetHubLedger.get(World.getServer()).put(key(), new NetHubLedger.Entry(Port, Powered, Archive, NetworkName, DeviceCount, LocalSubscriptions));
        HubStatus NextStatus = !Powered ? HubStatus.Off : Bridging ? HubStatus.Bridging : HubStatus.Standby;
        if (State.getValue(NetworkHubBlock.Status) != NextStatus) {
            NetworkHubBlock.applyStatus(World, worldPosition, State, NextStatus);
        }
    }

    private void scanDomain(ServerLevel World, Set<WireNetwork> Domain) {
        Set<String> Found = new TreeSet<>();
        int Devices = 0;
        long Lowest = Long.MAX_VALUE;
        for (WireNetwork Network : Domain) {
            for (BlockPos Outlet : Network.getOutlets()) {
                Lowest = Math.min(Lowest, Outlet.asLong());
            }
            for (Map.Entry<BlockPos, WireDevice> Entry : Network.findDevices(World).entrySet()) {
                if (Entry.getValue() instanceof NetworkHubBlockEntity) {
                    continue;
                }
                Devices++;
                for (String Subscription : Entry.getValue().getSubscriptions()) {
                    if (Found.size() < NetHubLedger.MaxSubscriptions && WireChannels.isValidSubscription(Subscription)) {
                        Found.add(Subscription);
                    }
                }
            }
        }
        String Name = Domain.isEmpty() ? NoNetwork : label(World.dimension(), Lowest);
        List<String> Subscriptions = List.copyOf(Found);
        if (!Name.equals(NetworkName) || Devices != DeviceCount) {
            NeedsSync = true;
        }
        NetworkName = Name;
        DeviceCount = Devices;
        LocalSubscriptions = Subscriptions;
    }

    public static String label(ResourceKey<Level> Dimension, long Lowest) {
        long Mixed = HashCommon.mix(Lowest ^ ((long) Dimension.location().hashCode() << 17));
        return String.format(Locale.ROOT, "NET-%04X", (int) (Mixed & 0xFFFFL));
    }

    @Nullable
    private BlockPos findOutlet(ServerLevel World, WireNetworkManager Manager) {
        for (Direction Side : Direction.values()) {
            BlockPos Next = worldPosition.relative(Side);
            if (!World.isLoaded(Next)) {
                continue;
            }
            BlockState Found = World.getBlockState(Next);
            if (Found.getBlock() instanceof WireConnector Connector && Manager.canonicalHost(World, Connector.hostPos(Found, Next)).equals(worldPosition)
                    && Manager.networkAt(World, Next) != null) {
                return Next.immutable();
            }
        }
        return null;
    }

    @Nullable
    BlockPos exitOutlet(ServerLevel World) {
        if (ExitOutlet == null) {
            ExitOutlet = findOutlet(World, WireNetworkManager.get(World));
        }
        return ExitOutlet;
    }

    public NetHubs.HubKey key() {
        return new NetHubs.HubKey(level == null ? Level.OVERWORLD : level.dimension(), worldPosition);
    }

    public int getPort() {
        return Port;
    }

    public void setPort(int Requested) {
        int Clamped = WireChannels.clampPort(Requested);
        if (Clamped == Port) {
            return;
        }
        Port = Clamped;
        Missed.clear();
        Relayed = 0;
        Delivered = 0;
        setChanged();
        NeedsSync = true;
        LastSync = Long.MIN_VALUE / 2;
        if (level instanceof ServerLevel World) {
            NetHubLedger.get(World.getServer()).put(key(), new NetHubLedger.Entry(Port, Powered, Archive, NetworkName, DeviceCount, LocalSubscriptions));
            NetHubs.changed(World.getServer());
        }
    }

    public boolean isPowered() {
        return Powered;
    }

    public boolean isBridging() {
        return Bridging;
    }

    public boolean isBridgeReady() {
        return Powered && NetHubs.isLive(this);
    }

    public long getLastTick() {
        return LastTick;
    }

    public boolean hasArchive() {
        return Archive;
    }

    public String getNetworkName() {
        return NetworkName;
    }

    public int getDeviceCount() {
        return DeviceCount;
    }

    public List<String> localSubscriptions() {
        return LocalSubscriptions;
    }

    public List<FarView> getViews() {
        return Views;
    }

    public long getRelayed() {
        return Relayed;
    }

    public long getDelivered() {
        return Delivered;
    }

    public String getDimensionName() {
        return DimensionName;
    }

    public int linkedCount() {
        int Count = 0;
        for (FarView View : Views) {
            if (View.Kind() == ViewLinked) {
                Count++;
            }
        }
        return Count;
    }

    public Vec3 getCentre() {
        Direction Front = getBlockState().hasProperty(NetworkHubBlock.Facing) ? getBlockState().getValue(NetworkHubBlock.Facing) : Direction.NORTH;
        return NetworkHubBlock.Layout.centre(worldPosition, Front);
    }

    void noteMissed(NetHubs.HubKey Far) {
        Missed.merge(Far, 1, Integer::sum);
        NeedsSync = true;
    }

    void noteRelayed() {
        Relayed++;
        NeedsSync = true;
    }

    void noteReceived() {
        Delivered++;
        NeedsSync = true;
    }

    @Override
    public int getPowerDemand() {
        return Draw;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Amount;
    }

    @Override
    public Collection<String> getSubscriptions() {
        return Powered ? Mirrored : List.of();
    }

    @Override
    public void receiveMessage(WireMessage Message) {
        if (level instanceof ServerLevel World) {
            NetHubs.relay(World, this, Message);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel World) {
            NetHubs.loaded(World, this);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level instanceof ServerLevel World) {
            NetHubs.unloaded(World, this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel World) {
            NetHubs.unloaded(World, this);
        } else if (level != null) {
            NetHubClientHooks.removed(this);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Data, HolderLookup.Provider Lookup) {
        super.saveAdditional(Data, Lookup);
        Data.putInt("Port", Port);
        Data.putBoolean("Powered", Powered);
    }

    @Override
    protected void loadAdditional(CompoundTag Data, HolderLookup.Provider Lookup) {
        super.loadAdditional(Data, Lookup);
        Port = WireChannels.clampPort(Data.getInt("Port"));
        if (Data.contains("Powered")) {
            Powered = Data.getBoolean("Powered");
            Restored = true;
        }
        if (Data.contains("Bridging")) {
            Bridging = Data.getBoolean("Bridging");
            NetworkName = Data.getString("Name");
            DeviceCount = Data.getInt("Devices");
            Relayed = Data.getLong("Relayed");
            Delivered = Data.getLong("Delivered");
            DimensionName = Data.getString("Dimension");
            List<FarView> Read = new ArrayList<>();
            ListTag Listed = Data.getList("Views", Tag.TAG_COMPOUND);
            for (int Index = 0; Index < Listed.size() && Read.size() < MaxViews; Index++) {
                Read.add(FarView.read(Listed.getCompound(Index)));
            }
            Views = List.copyOf(Read);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Lookup) {
        CompoundTag Data = new CompoundTag();
        Data.putInt("Port", Port);
        Data.putBoolean("Powered", Powered);
        Data.putBoolean("Bridging", Bridging);
        Data.putString("Name", NetworkName);
        Data.putInt("Devices", DeviceCount);
        Data.putLong("Relayed", Relayed);
        Data.putLong("Delivered", Delivered);
        Data.putString("Dimension", DimensionName);
        ListTag Listed = new ListTag();
        for (FarView View : Views) {
            Listed.add(View.write());
        }
        Data.put("Views", Listed);
        return Data;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public record FarView(String Dimension, BlockPos Pos, String Name, int Devices, int Kind, int Missed) {
        public CompoundTag write() {
            CompoundTag Data = new CompoundTag();
            Data.putString("Dimension", Dimension);
            Data.putLong("Pos", Pos.asLong());
            Data.putString("Name", Name);
            Data.putInt("Devices", Devices);
            Data.putInt("Kind", Kind);
            Data.putInt("Missed", Missed);
            return Data;
        }

        public static FarView read(CompoundTag Data) {
            return new FarView(Data.getString("Dimension"), BlockPos.of(Data.getLong("Pos")), Data.getString("Name"), Data.getInt("Devices"), Data.getInt("Kind"),
                    Data.getInt("Missed"));
        }
    }
}
