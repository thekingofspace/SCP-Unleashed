package net.rai.scp_unleashed.scp024;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.IntPredicate;

public class Scp024World extends SavedData {
    public static final String DataName = "scp_unleashed_scp024";
    public static final int MaxFields = 4096;
    private static final SavedData.Factory<Scp024World> DataFactory = new SavedData.Factory<>(Scp024World::new, Scp024World::load);

    @Nullable
    private BlockPos Door;
    private Direction Front = Direction.SOUTH;
    private final Map<UUID, Scp024Run> Runs = new LinkedHashMap<>();
    private final TreeMap<Integer, Claim> Fields = new TreeMap<>();

    private static final class Claim {
        private final UUID Owner;
        private boolean Active;
        private boolean Stranded;
        private long FreeAt;

        private Claim(UUID Owner, boolean Active, boolean Stranded, long FreeAt) {
            this.Owner = Owner;
            this.Active = Active;
            this.Stranded = Stranded;
            this.FreeAt = FreeAt;
        }
    }

    public static Scp024World get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean hasWarehouse() {
        return Door != null;
    }

    @Nullable
    public BlockPos door() {
        return Door;
    }

    public Direction front() {
        return Front;
    }

    public void recordWarehouse(BlockPos DoorOrigin, Direction Facing) {
        Door = DoorOrigin.immutable();
        Front = Facing;
        setDirty();
    }

    @Nullable
    public Scp024Run run(UUID Contestant) {
        return Runs.get(Contestant);
    }

    public Collection<Scp024Run> runs() {
        return Runs.values();
    }

    public void addRun(Scp024Run Run) {
        Runs.put(Run.playerId(), Run);
    }

    public void removeRun(UUID Contestant) {
        Runs.remove(Contestant);
    }

    public void removeRun(Scp024Run Run) {
        Runs.remove(Run.playerId(), Run);
    }

    public void clearRuns() {
        Runs.clear();
    }

    public boolean isFree(int Field, long Now) {
        Claim Held = Fields.get(Field);
        return Held == null || !Held.Active && !Held.Stranded && Held.FreeAt <= Now;
    }

    public int claimField(UUID Owner, long Now, IntPredicate Usable) {
        int Fallback = -1;
        for (int Field = 0; Field < MaxFields; Field++) {
            if (!isFree(Field, Now)) {
                continue;
            }
            if (Usable.test(Field)) {
                return claim(Field, Owner);
            }
            if (Fallback < 0) {
                Fallback = Field;
            }
        }
        if (Fallback < 0) {
            Fallback = Fields.isEmpty() ? 0 : Fields.lastKey() + 1;
        }
        return claim(Fallback, Owner);
    }

    private int claim(int Field, UUID Owner) {
        Fields.put(Field, new Claim(Owner, true, false, 0L));
        setDirty();
        return Field;
    }

    public void releaseField(int Field, UUID Owner, long FreeAt, boolean Stranded) {
        Claim Held = Fields.get(Field);
        if (Held == null || !Held.Owner.equals(Owner)) {
            return;
        }
        Held.Active = false;
        Held.Stranded = Stranded;
        Held.FreeAt = FreeAt;
        setDirty();
    }

    public void ownerLeft(UUID Owner, int Field) {
        Claim Held = Fields.get(Field);
        if (Held != null && !Held.Active && Held.Stranded && Held.Owner.equals(Owner)) {
            Held.Stranded = false;
            setDirty();
        }
    }

    public Map<Integer, UUID> strandedFields() {
        Map<Integer, UUID> Found = new LinkedHashMap<>();
        for (Map.Entry<Integer, Claim> Entry : Fields.entrySet()) {
            if (!Entry.getValue().Active && Entry.getValue().Stranded) {
                Found.put(Entry.getKey(), Entry.getValue().Owner);
            }
        }
        return Found;
    }

    public void forgetFree(long Now) {
        if (Fields.entrySet().removeIf(Entry -> !Entry.getValue().Active && !Entry.getValue().Stranded && Entry.getValue().FreeAt <= Now)) {
            setDirty();
        }
    }

    public void strandActive(long FreeAt) {
        for (Claim Held : Fields.values()) {
            if (Held.Active) {
                Held.Active = false;
                Held.Stranded = true;
                Held.FreeAt = FreeAt;
                setDirty();
            }
        }
    }

    public int claimedFields() {
        return Fields.size();
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        if (Door != null) {
            Tag.put("Door", NbtUtils.writeBlockPos(Door));
            Tag.putString("Front", Front.getSerializedName());
        }
        ListTag Claims = new ListTag();
        for (Map.Entry<Integer, Claim> Entry : Fields.entrySet()) {
            CompoundTag Each = new CompoundTag();
            Each.putInt("Field", Entry.getKey());
            Each.putUUID("Owner", Entry.getValue().Owner);
            Each.putBoolean("Active", Entry.getValue().Active);
            Each.putBoolean("Stranded", Entry.getValue().Stranded);
            Each.putLong("FreeAt", Entry.getValue().FreeAt);
            Claims.add(Each);
        }
        Tag.put("Fields", Claims);
        return Tag;
    }

    public static Scp024World load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp024World World = new Scp024World();
        NbtUtils.readBlockPos(Tag, "Door").ifPresent(Pos -> World.Door = Pos);
        Direction Facing = Direction.byName(Tag.getString("Front"));
        World.Front = Facing != null && Facing.getAxis().isHorizontal() ? Facing : Direction.SOUTH;
        ListTag Claims = Tag.getList("Fields", CompoundTag.TAG_COMPOUND);
        for (int Index = 0; Index < Claims.size(); Index++) {
            CompoundTag Each = Claims.getCompound(Index);
            if (Each.hasUUID("Owner")) {
                World.Fields.put(Each.getInt("Field"), new Claim(Each.getUUID("Owner"), Each.getBoolean("Active"), Each.getBoolean("Stranded"), Each.getLong("FreeAt")));
            }
        }
        return World;
    }
}
