package net.rai.scp_unleashed.z0002;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Z0002World extends SavedData {
    public static final String DataName = "scp_unleashed_z0002";
    private static final SavedData.Factory<Z0002World> DataFactory = new SavedData.Factory<>(Z0002World::new, Z0002World::load);
    private final List<Z0002Domain> Domains = new ArrayList<>();
    private final Map<UUID, Long> Dormancy = new HashMap<>();
    private final Map<UUID, Z0002Spot> PendingReturns = new HashMap<>();

    public static Z0002World get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public static void writeNow(MinecraftServer Server) {
        Server.overworld().getDataStorage().save();
    }

    public static Z0002World reload(MinecraftServer Server) {
        HolderLookup.Provider Lookup = Server.registryAccess();
        CompoundTag Saved = get(Server).save(new CompoundTag(), Lookup);
        Z0002World Fresh = load(Saved, Lookup);
        Server.overworld().getDataStorage().set(DataName, Fresh);
        return Fresh;
    }

    public List<Z0002Domain> domains() {
        return Domains;
    }

    public List<Z0002Domain> active() {
        List<Z0002Domain> Found = new ArrayList<>();
        for (Z0002Domain Each : Domains) {
            if (Each.active()) {
                Found.add(Each);
            }
        }
        return Found;
    }

    @Nullable
    public Z0002Domain byId(UUID Id) {
        for (Z0002Domain Each : Domains) {
            if (Each.Id.equals(Id)) {
                return Each;
            }
        }
        return null;
    }

    public void add(Z0002Domain Domain) {
        Domains.add(Domain);
        setDirty();
    }

    public void remove(Z0002Domain Domain) {
        if (Domains.remove(Domain)) {
            setDirty();
        }
    }

    public long dormantUntil(UUID Holder) {
        return Dormancy.getOrDefault(Holder, 0L);
    }

    public void setDormant(UUID Holder, long Until) {
        if (Until <= 0L) {
            if (Dormancy.remove(Holder) != null) {
                setDirty();
            }
            return;
        }
        Dormancy.put(Holder, Until);
        setDirty();
    }

    public void pruneDormancy(long Now) {
        if (Dormancy.values().removeIf(Until -> Until <= Now)) {
            setDirty();
        }
    }

    @Nullable
    public Z0002Spot pendingReturn(UUID Id) {
        return PendingReturns.get(Id);
    }

    public void setPendingReturn(UUID Id, Z0002Spot Spot) {
        PendingReturns.put(Id, Spot);
        setDirty();
    }

    public void clearPendingReturn(UUID Id) {
        if (PendingReturns.remove(Id) != null) {
            setDirty();
        }
    }

    public void touch() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        ListTag DomainRows = new ListTag();
        for (Z0002Domain Each : Domains) {
            DomainRows.add(Each.save());
        }
        Saved.put("Domains", DomainRows);
        ListTag DormantRows = new ListTag();
        for (Map.Entry<UUID, Long> Entry : Dormancy.entrySet()) {
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Id", Entry.getKey());
            Row.putLong("Until", Entry.getValue());
            DormantRows.add(Row);
        }
        Saved.put("Dormancy", DormantRows);
        ListTag ReturnRows = new ListTag();
        for (Map.Entry<UUID, Z0002Spot> Entry : PendingReturns.entrySet()) {
            CompoundTag Row = Entry.getValue().save();
            Row.putUUID("Id", Entry.getKey());
            ReturnRows.add(Row);
        }
        Saved.put("Returns", ReturnRows);
        return Saved;
    }

    public static Z0002World load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Z0002World Data = new Z0002World();
        ListTag DomainRows = Saved.getList("Domains", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < DomainRows.size(); Index++) {
            Z0002Domain Loaded = Z0002Domain.load(DomainRows.getCompound(Index), Lookup);
            if (Loaded != null) {
                Data.Domains.add(Loaded);
            }
        }
        ListTag DormantRows = Saved.getList("Dormancy", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < DormantRows.size(); Index++) {
            CompoundTag Row = DormantRows.getCompound(Index);
            if (Row.hasUUID("Id")) {
                Data.Dormancy.put(Row.getUUID("Id"), Row.getLong("Until"));
            }
        }
        ListTag ReturnRows = Saved.getList("Returns", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < ReturnRows.size(); Index++) {
            CompoundTag Row = ReturnRows.getCompound(Index);
            Z0002Spot Spot = Z0002Spot.load(Row);
            if (Spot != null && Row.hasUUID("Id")) {
                Data.PendingReturns.put(Row.getUUID("Id"), Spot);
            }
        }
        return Data;
    }
}
