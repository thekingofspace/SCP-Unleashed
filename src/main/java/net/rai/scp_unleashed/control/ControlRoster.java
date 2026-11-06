package net.rai.scp_unleashed.control;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ControlRoster extends SavedData {
    public static final String DataName = "scp_unleashed_control_roster";
    private static final SavedData.Factory<ControlRoster> DataFactory = new SavedData.Factory<>(ControlRoster::new, ControlRoster::load);

    public enum BodyStatus {
        Unknown,
        Free,
        Controlled,
        Deceased
    }

    public record BodyRecord(int Incarnation, BodyStatus Status, @Nullable UUID Controller, String ControllerName) {
    }

    private final Map<UUID, BodyRecord> Bodies = new HashMap<>();
    private final Map<UUID, UUID> Vacants = new HashMap<>();

    public static ControlRoster get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    @Nullable
    public BodyRecord record(UUID Body) {
        return Bodies.get(Body);
    }

    public BodyStatus status(UUID Body) {
        BodyRecord Found = Bodies.get(Body);
        return Found == null ? BodyStatus.Unknown : Found.Status();
    }

    public int incarnation(UUID Body) {
        BodyRecord Found = Bodies.get(Body);
        return Found == null ? 0 : Found.Incarnation();
    }

    public boolean accepts(UUID Body, int Incarnation) {
        BodyRecord Found = Bodies.get(Body);
        if (Found == null) {
            return true;
        }
        return Found.Status() == BodyStatus.Free && Incarnation >= Found.Incarnation();
    }

    public void know(UUID Body, int Incarnation) {
        BodyRecord Found = Bodies.get(Body);
        if (Found == null || Found.Status() == BodyStatus.Free && Incarnation > Found.Incarnation()) {
            Bodies.put(Body, new BodyRecord(Incarnation, BodyStatus.Free, null, ""));
            setDirty();
        }
    }

    public void possess(UUID Body, UUID Controller, String ControllerName) {
        Bodies.put(Body, new BodyRecord(incarnation(Body), BodyStatus.Controlled, Controller, ControllerName));
        setDirty();
    }

    public int free(UUID Body) {
        int Next = incarnation(Body) + 1;
        Bodies.put(Body, new BodyRecord(Next, BodyStatus.Free, null, ""));
        setDirty();
        return Next;
    }

    public void dead(UUID Body) {
        Bodies.put(Body, new BodyRecord(incarnation(Body), BodyStatus.Deceased, null, ""));
        setDirty();
    }

    @Nullable
    public UUID vacantOf(UUID Owner) {
        return Vacants.get(Owner);
    }

    public void setVacant(UUID Owner, UUID Vacant) {
        Vacants.put(Owner, Vacant);
        setDirty();
    }

    public void clearVacant(UUID Owner) {
        if (Vacants.remove(Owner) != null) {
            setDirty();
        }
    }

    public static ControlRoster load(CompoundTag Data, HolderLookup.Provider Registries) {
        ControlRoster Roster = new ControlRoster();
        ListTag Saved = Data.getList("Bodies", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag Entry = Saved.getCompound(Index);
            if (!Entry.hasUUID("Id")) {
                continue;
            }
            int StatusIndex = Math.max(0, Math.min(BodyStatus.values().length - 1, Entry.getByte("Status")));
            UUID Controller = Entry.hasUUID("Controller") ? Entry.getUUID("Controller") : null;
            Roster.Bodies.put(Entry.getUUID("Id"), new BodyRecord(Entry.getInt("Incarnation"), BodyStatus.values()[StatusIndex], Controller, Entry.getString("ControllerName")));
        }
        ListTag Seats = Data.getList("Vacants", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Seats.size(); Index++) {
            CompoundTag Entry = Seats.getCompound(Index);
            if (Entry.hasUUID("Owner") && Entry.hasUUID("Vacant")) {
                Roster.Vacants.put(Entry.getUUID("Owner"), Entry.getUUID("Vacant"));
            }
        }
        return Roster;
    }

    @Override
    public CompoundTag save(CompoundTag Data, HolderLookup.Provider Registries) {
        ListTag Saved = new ListTag();
        for (Map.Entry<UUID, BodyRecord> Entry : Bodies.entrySet()) {
            CompoundTag Item = new CompoundTag();
            Item.putUUID("Id", Entry.getKey());
            Item.putInt("Incarnation", Entry.getValue().Incarnation());
            Item.putByte("Status", (byte) Entry.getValue().Status().ordinal());
            if (Entry.getValue().Controller() != null) {
                Item.putUUID("Controller", Entry.getValue().Controller());
                Item.putString("ControllerName", Entry.getValue().ControllerName());
            }
            Saved.add(Item);
        }
        Data.put("Bodies", Saved);
        ListTag Seats = new ListTag();
        for (Map.Entry<UUID, UUID> Entry : Vacants.entrySet()) {
            CompoundTag Item = new CompoundTag();
            Item.putUUID("Owner", Entry.getKey());
            Item.putUUID("Vacant", Entry.getValue());
            Seats.add(Item);
        }
        Data.put("Vacants", Seats);
        return Data;
    }
}
