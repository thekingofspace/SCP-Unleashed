package net.rai.scp_unleashed.nethub;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.rai.scp_unleashed.wire.WireChannels;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NetHubLedger extends SavedData {
    public static final String DataName = "scp_unleashed_network_hubs";
    public static final int MaxSubscriptions = 128;
    public static final int MaxName = 16;
    private static final SavedData.Factory<NetHubLedger> DataFactory = new SavedData.Factory<>(NetHubLedger::new, NetHubLedger::load);

    private final Map<NetHubs.HubKey, Entry> Entries = new LinkedHashMap<>();

    public static NetHubLedger get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    @Nullable
    public Entry find(NetHubs.HubKey Key) {
        return Entries.get(Key);
    }

    public boolean put(NetHubs.HubKey Key, Entry Value) {
        Entry Previous = Entries.put(Key, Value);
        if (Objects.equals(Previous, Value)) {
            return false;
        }
        setDirty();
        return true;
    }

    public boolean remove(NetHubs.HubKey Key) {
        if (Entries.remove(Key) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    public List<Map.Entry<NetHubs.HubKey, Entry>> onPort(int Port) {
        List<Map.Entry<NetHubs.HubKey, Entry>> Found = new ArrayList<>();
        for (Map.Entry<NetHubs.HubKey, Entry> Each : Entries.entrySet()) {
            if (Each.getValue().Port() == Port) {
                Found.add(Map.entry(Each.getKey(), Each.getValue()));
            }
        }
        return Found;
    }

    public int size() {
        return Entries.size();
    }

    @Override
    public CompoundTag save(CompoundTag Data, HolderLookup.Provider Lookup) {
        ListTag Saved = new ListTag();
        for (Map.Entry<NetHubs.HubKey, Entry> Each : Entries.entrySet()) {
            CompoundTag Item = new CompoundTag();
            Item.putString("Dimension", Each.getKey().Dimension().location().toString());
            Item.putLong("Pos", Each.getKey().Pos().asLong());
            Entry Value = Each.getValue();
            Item.putInt("Port", Value.Port());
            Item.putBoolean("Powered", Value.Powered());
            Item.putBoolean("Archive", Value.Archive());
            Item.putString("Name", Value.Name());
            Item.putInt("Devices", Value.Devices());
            ListTag Subscriptions = new ListTag();
            for (String Subscription : Value.Subscriptions()) {
                Subscriptions.add(StringTag.valueOf(Subscription));
            }
            Item.put("Subscriptions", Subscriptions);
            Saved.add(Item);
        }
        Data.put("Hubs", Saved);
        return Data;
    }

    public static NetHubLedger load(CompoundTag Data, HolderLookup.Provider Lookup) {
        NetHubLedger Ledger = new NetHubLedger();
        ListTag Saved = Data.getList("Hubs", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag Item = Saved.getCompound(Index);
            ResourceLocation Dimension = ResourceLocation.tryParse(Item.getString("Dimension"));
            if (Dimension == null) {
                continue;
            }
            List<String> Subscriptions = new ArrayList<>();
            ListTag Listed = Item.getList("Subscriptions", Tag.TAG_STRING);
            for (int Each = 0; Each < Listed.size() && Subscriptions.size() < MaxSubscriptions; Each++) {
                String Subscription = Listed.getString(Each);
                if (WireChannels.isValidSubscription(Subscription)) {
                    Subscriptions.add(Subscription);
                }
            }
            String Name = Item.getString("Name");
            Ledger.Entries.put(new NetHubs.HubKey(ResourceKey.create(Registries.DIMENSION, Dimension), BlockPos.of(Item.getLong("Pos"))),
                    new Entry(WireChannels.clampPort(Item.getInt("Port")), Item.getBoolean("Powered"), Item.getBoolean("Archive"),
                            Name.length() > MaxName ? Name.substring(0, MaxName) : Name, Math.max(0, Item.getInt("Devices")), List.copyOf(Subscriptions)));
        }
        return Ledger;
    }

    public record Entry(int Port, boolean Powered, boolean Archive, String Name, int Devices, List<String> Subscriptions) {
    }
}
