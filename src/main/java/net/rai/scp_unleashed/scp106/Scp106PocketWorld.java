package net.rai.scp_unleashed.scp106;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Scp106PocketWorld extends SavedData {
    public static final String DataName = "scp_unleashed_scp106";
    private static final SavedData.Factory<Scp106PocketWorld> DataFactory = new SavedData.Factory<>(Scp106PocketWorld::new, Scp106PocketWorld::load);

    public record Spot(ResourceKey<Level> Dimension, Vec3 Pos, float Yaw) {
        public CompoundTag save() {
            CompoundTag Row = new CompoundTag();
            Row.putString("Dimension", Dimension.location().toString());
            Row.putDouble("X", Pos.x);
            Row.putDouble("Y", Pos.y);
            Row.putDouble("Z", Pos.z);
            Row.putFloat("Yaw", Yaw);
            return Row;
        }

        @Nullable
        public static Spot load(CompoundTag Row) {
            ResourceLocation Where = ResourceLocation.tryParse(Row.getString("Dimension"));
            if (Where == null) {
                return null;
            }
            return new Spot(ResourceKey.create(Registries.DIMENSION, Where), new Vec3(Row.getDouble("X"), Row.getDouble("Y"), Row.getDouble("Z")), Row.getFloat("Yaw"));
        }
    }

    public static final class Visit {
        public final Spot Return;
        public int Doors;
        public int Room;

        public Visit(Spot Return, int Doors, int Room) {
            this.Return = Return;
            this.Doors = Doors;
            this.Room = Room;
        }
    }

    public static final class Captive {
        public final UUID Id;
        public final Spot Return;
        public final long Due;
        public final boolean Escapes;
        public int Misses;

        public Captive(UUID Id, Spot Return, long Due, boolean Escapes) {
            this.Id = Id;
            this.Return = Return;
            this.Due = Due;
            this.Escapes = Escapes;
        }
    }

    private int BuiltSignature = -1;
    private final Map<UUID, Visit> Visits = new HashMap<>();
    private final Map<UUID, Captive> Captives = new HashMap<>();

    public static Scp106PocketWorld get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public int builtSignature() {
        return BuiltSignature;
    }

    public void markBuilt(int Signature) {
        BuiltSignature = Signature;
        setDirty();
    }

    @Nullable
    public Visit visitOf(UUID Visitor) {
        return Visits.get(Visitor);
    }

    public void startVisit(UUID Visitor, Visit Entry) {
        Visits.put(Visitor, Entry);
        setDirty();
    }

    public void touchVisit() {
        setDirty();
    }

    public void endVisit(UUID Visitor) {
        if (Visits.remove(Visitor) != null) {
            setDirty();
        }
    }

    public Collection<Captive> captives() {
        return Captives.values();
    }

    public boolean isCaptive(UUID Id) {
        return Captives.containsKey(Id);
    }

    public void addCaptive(Captive Entry) {
        Captives.put(Entry.Id, Entry);
        setDirty();
    }

    public void removeCaptive(UUID Id) {
        if (Captives.remove(Id) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Saved.putInt("Built", BuiltSignature);
        ListTag VisitRows = new ListTag();
        for (Map.Entry<UUID, Visit> Entry : Visits.entrySet()) {
            CompoundTag Row = Entry.getValue().Return.save();
            Row.putUUID("Id", Entry.getKey());
            Row.putInt("Doors", Entry.getValue().Doors);
            Row.putInt("Room", Entry.getValue().Room);
            VisitRows.add(Row);
        }
        Saved.put("Visits", VisitRows);
        ListTag CaptiveRows = new ListTag();
        for (Captive Entry : Captives.values()) {
            CompoundTag Row = Entry.Return.save();
            Row.putUUID("Id", Entry.Id);
            Row.putLong("Due", Entry.Due);
            Row.putBoolean("Escapes", Entry.Escapes);
            Row.putInt("Misses", Entry.Misses);
            CaptiveRows.add(Row);
        }
        Saved.put("Captives", CaptiveRows);
        return Saved;
    }

    public static Scp106PocketWorld load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Scp106PocketWorld Data = new Scp106PocketWorld();
        Data.BuiltSignature = Saved.contains("Built") ? Saved.getInt("Built") : -1;
        ListTag VisitRows = Saved.getList("Visits", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < VisitRows.size(); Index++) {
            CompoundTag Row = VisitRows.getCompound(Index);
            Spot Return = Spot.load(Row);
            if (Row.hasUUID("Id") && Return != null) {
                Data.Visits.put(Row.getUUID("Id"), new Visit(Return, Row.getInt("Doors"), Row.getInt("Room")));
            }
        }
        ListTag CaptiveRows = Saved.getList("Captives", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < CaptiveRows.size(); Index++) {
            CompoundTag Row = CaptiveRows.getCompound(Index);
            Spot Return = Spot.load(Row);
            if (Row.hasUUID("Id") && Return != null) {
                Captive Entry = new Captive(Row.getUUID("Id"), Return, Row.getLong("Due"), Row.getBoolean("Escapes"));
                Entry.Misses = Row.getInt("Misses");
                Data.Captives.put(Entry.Id, Entry);
            }
        }
        return Data;
    }
}
