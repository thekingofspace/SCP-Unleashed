package net.rai.scp_unleashed.z0002;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class Z0002Domain {
    public final UUID Id;
    public final ResourceKey<Level> Dimension;
    public final Z0002Region Region;
    public final UUID Owner;
    public final String OwnerName;
    public final long Start;
    public long End;
    public final float Facing;
    public final Vec3 Anchor;
    public final Map<UUID, Z0002Spot> Members = new LinkedHashMap<>();
    public final Map<UUID, Vec3> Arrivals = new LinkedHashMap<>();
    public final Map<UUID, Vec3> MobReturns = new LinkedHashMap<>();
    public Z0002Snapshot Snapshot = new Z0002Snapshot();
    @Nullable
    public UUID ShrineId;
    public boolean Cracked;
    public boolean Closed;
    public long ClosedAt;
    public Z0002Phase Phase = Z0002Phase.Active;
    public long BuildEnd;
    public long CollapseStart;

    public Z0002Domain(UUID Id, ResourceKey<Level> Dimension, Z0002Region Region, UUID Owner, String OwnerName, long Start, long End, float Facing, Vec3 Anchor) {
        this.Id = Id;
        this.Dimension = Dimension;
        this.Region = Region;
        this.Owner = Owner;
        this.OwnerName = OwnerName;
        this.Start = Start;
        this.End = End;
        this.Facing = Facing;
        this.Anchor = Anchor;
    }

    public boolean active() {
        return !Closed;
    }

    public long left(long Now) {
        return Math.max(0L, End - Now);
    }

    public CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        Saved.putUUID("Id", Id);
        Saved.putString("Dimension", Dimension.location().toString());
        Saved.put("Centre", NbtUtils.writeBlockPos(Region.Centre()));
        Saved.putInt("Radius", Region.Radius());
        Saved.putUUID("Owner", Owner);
        Saved.putString("OwnerName", OwnerName);
        Saved.putLong("Start", Start);
        Saved.putLong("End", End);
        Saved.putFloat("Facing", Facing);
        Saved.putDouble("AnchorX", Anchor.x);
        Saved.putDouble("AnchorY", Anchor.y);
        Saved.putDouble("AnchorZ", Anchor.z);
        ListTag MemberRows = new ListTag();
        for (Map.Entry<UUID, Z0002Spot> Entry : Members.entrySet()) {
            CompoundTag Row = Entry.getValue().save();
            Row.putUUID("Id", Entry.getKey());
            Vec3 Arrival = Arrivals.get(Entry.getKey());
            if (Arrival != null) {
                Row.putDouble("ArriveX", Arrival.x);
                Row.putDouble("ArriveY", Arrival.y);
                Row.putDouble("ArriveZ", Arrival.z);
            }
            MemberRows.add(Row);
        }
        Saved.put("Members", MemberRows);
        ListTag MobRows = new ListTag();
        for (Map.Entry<UUID, Vec3> Entry : MobReturns.entrySet()) {
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Id", Entry.getKey());
            Row.putDouble("X", Entry.getValue().x);
            Row.putDouble("Y", Entry.getValue().y);
            Row.putDouble("Z", Entry.getValue().z);
            MobRows.add(Row);
        }
        Saved.put("Mobs", MobRows);
        Saved.put("Snapshot", Snapshot.save());
        if (ShrineId != null) {
            Saved.putUUID("Shrine", ShrineId);
        }
        Saved.putBoolean("Cracked", Cracked);
        Saved.putBoolean("Closed", Closed);
        Saved.putLong("ClosedAt", ClosedAt);
        Saved.putString("Phase", Phase.key());
        Saved.putLong("BuildEnd", BuildEnd);
        Saved.putLong("CollapseStart", CollapseStart);
        return Saved;
    }

    @Nullable
    public static Z0002Domain load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        ResourceLocation Where = ResourceLocation.tryParse(Saved.getString("Dimension"));
        if (Where == null || !Saved.hasUUID("Id") || !Saved.hasUUID("Owner")) {
            return null;
        }
        BlockPos Centre = NbtUtils.readBlockPos(Saved, "Centre").orElse(null);
        if (Centre == null) {
            return null;
        }
        Z0002Domain Loaded = new Z0002Domain(Saved.getUUID("Id"), ResourceKey.create(Registries.DIMENSION, Where), new Z0002Region(Centre, Saved.getInt("Radius")),
                Saved.getUUID("Owner"), Saved.getString("OwnerName"), Saved.getLong("Start"), Saved.getLong("End"), Saved.getFloat("Facing"),
                new Vec3(Saved.getDouble("AnchorX"), Saved.getDouble("AnchorY"), Saved.getDouble("AnchorZ")));
        ListTag MemberRows = Saved.getList("Members", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < MemberRows.size(); Index++) {
            CompoundTag Row = MemberRows.getCompound(Index);
            Z0002Spot Spot = Z0002Spot.load(Row);
            if (Spot == null || !Row.hasUUID("Id")) {
                continue;
            }
            Loaded.Members.put(Row.getUUID("Id"), Spot);
            if (Row.contains("ArriveX")) {
                Loaded.Arrivals.put(Row.getUUID("Id"), new Vec3(Row.getDouble("ArriveX"), Row.getDouble("ArriveY"), Row.getDouble("ArriveZ")));
            }
        }
        ListTag MobRows = Saved.getList("Mobs", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < MobRows.size(); Index++) {
            CompoundTag Row = MobRows.getCompound(Index);
            if (Row.hasUUID("Id")) {
                Loaded.MobReturns.put(Row.getUUID("Id"), new Vec3(Row.getDouble("X"), Row.getDouble("Y"), Row.getDouble("Z")));
            }
        }
        Loaded.Snapshot = Z0002Snapshot.load(Saved.getCompound("Snapshot"), Lookup);
        Loaded.ShrineId = Saved.hasUUID("Shrine") ? Saved.getUUID("Shrine") : null;
        Loaded.Cracked = Saved.getBoolean("Cracked");
        Loaded.Closed = Saved.getBoolean("Closed");
        Loaded.ClosedAt = Saved.getLong("ClosedAt");
        Loaded.Phase = Saved.contains("Phase") ? Z0002Phase.byKey(Saved.getString("Phase")) : Z0002Phase.Active;
        Loaded.BuildEnd = Saved.getLong("BuildEnd");
        Loaded.CollapseStart = Saved.getLong("CollapseStart");
        return Loaded;
    }
}
