package net.rai.scp_unleashed.scp439;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Scp439Colonies extends SavedData {
    public static final String DataName = "scp_unleashed_scp439";
    public static final int Cap = 6;
    public static final long MemberStale = 1200L;
    public static final long WanderStale = 24000L;
    public static final byte Workers = 0;
    public static final byte Brutes = 1;
    private final Map<UUID, BlockPos> Hives = new HashMap<>();
    private final Map<UUID, Member> Members = new HashMap<>();
    private final Map<UUID, Long> Hosts = new HashMap<>();
    private final Map<UUID, Long> Queens = new HashMap<>();

    public record Member(UUID Hive, byte Kind, long Seen) {
    }

    public static Scp439Colonies get(ServerLevel World) {
        return World.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Scp439Colonies::new, Scp439Colonies::load), DataName);
    }

    public static boolean hiveAlive(MinecraftServer Server, UUID Hive) {
        for (ServerLevel World : Server.getAllLevels()) {
            if (get(World).hasHive(Hive)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasHive(UUID Hive) {
        return Hives.containsKey(Hive);
    }

    public Optional<BlockPos> hivePos(UUID Hive) {
        return Optional.ofNullable(Hives.get(Hive));
    }

    public void addHive(UUID Hive, BlockPos Pos) {
        BlockPos Before = Hives.put(Hive, Pos.immutable());
        if (!Pos.equals(Before)) {
            setDirty();
        }
    }

    public List<UUID> removeHive(UUID Hive) {
        List<UUID> Bound = new ArrayList<>();
        if (Hives.remove(Hive) != null) {
            setDirty();
        }
        Members.entrySet().removeIf(Entry -> {
            if (Entry.getValue().Hive().equals(Hive)) {
                Bound.add(Entry.getKey());
                return true;
            }
            return false;
        });
        if (!Bound.isEmpty()) {
            setDirty();
        }
        return Bound;
    }

    public void seeMember(UUID Id, UUID Hive, byte Kind, long Now) {
        Member Before = Members.put(Id, new Member(Hive, Kind, Now));
        if (Before == null || !Before.Hive().equals(Hive)) {
            setDirty();
        }
    }

    public void removeMember(UUID Id) {
        if (Members.remove(Id) != null) {
            setDirty();
        }
    }

    public int members(UUID Hive, byte Kind, long Now) {
        int Count = 0;
        for (Member Found : Members.values()) {
            if (Found.Hive().equals(Hive) && Found.Kind() == Kind && Now - Found.Seen() <= MemberStale) {
                Count++;
            }
        }
        return Count;
    }

    public List<UUID> membersOf(UUID Hive) {
        List<UUID> Found = new ArrayList<>();
        for (Map.Entry<UUID, Member> Entry : Members.entrySet()) {
            if (Entry.getValue().Hive().equals(Hive)) {
                Found.add(Entry.getKey());
            }
        }
        return Found;
    }

    public void seeHost(UUID Host, long Now) {
        if (Hosts.put(Host, Now) == null) {
            setDirty();
        }
    }

    public boolean tracksHost(UUID Host) {
        return Hosts.containsKey(Host);
    }

    public void removeHost(UUID Host) {
        if (Hosts.remove(Host) != null) {
            setDirty();
        }
    }

    public void seeQueen(UUID Queen, long Now) {
        if (Queens.put(Queen, Now) == null) {
            setDirty();
        }
    }

    public void removeQueen(UUID Queen) {
        if (Queens.remove(Queen) != null) {
            setDirty();
        }
    }

    public int hives() {
        return Hives.size();
    }

    public int hosts(long Now) {
        return (int) Hosts.values().stream().filter(Seen -> Now - Seen <= WanderStale).count();
    }

    public int queens(long Now) {
        return (int) Queens.values().stream().filter(Seen -> Now - Seen <= WanderStale).count();
    }

    public int colonies(long Now) {
        return hives() + hosts(Now) + queens(Now);
    }

    public boolean roomForQueen(long Now) {
        return colonies(Now) < Cap;
    }

    public void forget() {
        Hives.clear();
        Members.clear();
        Hosts.clear();
        Queens.clear();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag Data, HolderLookup.Provider Lookup) {
        ListTag HiveList = new ListTag();
        for (Map.Entry<UUID, BlockPos> Entry : Hives.entrySet()) {
            CompoundTag Saved = new CompoundTag();
            Saved.putUUID("Id", Entry.getKey());
            Saved.put("Pos", NbtUtils.writeBlockPos(Entry.getValue()));
            HiveList.add(Saved);
        }
        Data.put("Hives", HiveList);
        ListTag MemberList = new ListTag();
        for (Map.Entry<UUID, Member> Entry : Members.entrySet()) {
            CompoundTag Saved = new CompoundTag();
            Saved.putUUID("Id", Entry.getKey());
            Saved.putUUID("Hive", Entry.getValue().Hive());
            Saved.putByte("Kind", Entry.getValue().Kind());
            Saved.putLong("Seen", Entry.getValue().Seen());
            MemberList.add(Saved);
        }
        Data.put("Members", MemberList);
        Data.put("Hosts", seen(Hosts));
        Data.put("Queens", seen(Queens));
        return Data;
    }

    private static ListTag seen(Map<UUID, Long> Source) {
        ListTag Result = new ListTag();
        for (Map.Entry<UUID, Long> Entry : Source.entrySet()) {
            CompoundTag Saved = new CompoundTag();
            Saved.putUUID("Id", Entry.getKey());
            Saved.putLong("Seen", Entry.getValue());
            Result.add(Saved);
        }
        return Result;
    }

    private static void readSeen(ListTag Source, Map<UUID, Long> Target) {
        for (int Index = 0; Index < Source.size(); Index++) {
            CompoundTag Saved = Source.getCompound(Index);
            if (Saved.hasUUID("Id")) {
                Target.put(Saved.getUUID("Id"), Saved.getLong("Seen"));
            }
        }
    }

    public static Scp439Colonies load(CompoundTag Data, HolderLookup.Provider Lookup) {
        Scp439Colonies Loaded = new Scp439Colonies();
        ListTag HiveList = Data.getList("Hives", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < HiveList.size(); Index++) {
            CompoundTag Saved = HiveList.getCompound(Index);
            if (Saved.hasUUID("Id")) {
                NbtUtils.readBlockPos(Saved, "Pos").ifPresent(Pos -> Loaded.Hives.put(Saved.getUUID("Id"), Pos));
            }
        }
        ListTag MemberList = Data.getList("Members", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < MemberList.size(); Index++) {
            CompoundTag Saved = MemberList.getCompound(Index);
            if (Saved.hasUUID("Id") && Saved.hasUUID("Hive")) {
                Loaded.Members.put(Saved.getUUID("Id"), new Member(Saved.getUUID("Hive"), Saved.getByte("Kind"), Saved.getLong("Seen")));
            }
        }
        readSeen(Data.getList("Hosts", Tag.TAG_COMPOUND), Loaded.Hosts);
        readSeen(Data.getList("Queens", Tag.TAG_COMPOUND), Loaded.Queens);
        return Loaded;
    }
}
