package net.rai.scp_unleashed.scp465;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Scp465Parties extends SavedData {
    public static final String DataName = "scp_unleashed_scp465";
    private final Map<Integer, Scp465Party> Parties = new LinkedHashMap<>();
    private int NextId = 1;

    public static SavedData.Factory<Scp465Parties> factory() {
        return new SavedData.Factory<>(Scp465Parties::new, Scp465Parties::load);
    }

    public static Scp465Parties get(ServerLevel World) {
        return World.getDataStorage().computeIfAbsent(factory(), DataName);
    }

    @Nullable
    public static Scp465Parties peek(ServerLevel World) {
        return World.getDataStorage().get(factory(), DataName);
    }

    @Nullable
    public static Scp465Party find(ServerLevel World, int Id) {
        Scp465Parties Data = peek(World);
        return Data == null ? null : Data.party(Id);
    }

    @Nullable
    public static Scp465Party containing(ServerLevel World, BlockPos Pos) {
        Scp465Parties Data = peek(World);
        return Data == null ? null : Data.at(Pos);
    }

    public int nextId() {
        int Given = NextId++;
        setDirty();
        return Given;
    }

    public void add(Scp465Party Party) {
        Parties.put(Party.id(), Party);
        setDirty();
    }

    @Nullable
    public Scp465Party remove(int Id) {
        Scp465Party Removed = Parties.remove(Id);
        if (Removed != null) {
            setDirty();
        }
        return Removed;
    }

    @Nullable
    public Scp465Party party(int Id) {
        return Parties.get(Id);
    }

    @Nullable
    public Scp465Party at(BlockPos Pos) {
        for (Scp465Party Party : Parties.values()) {
            if (Party.contains(Pos)) {
                return Party;
            }
        }
        return null;
    }

    @Nullable
    public Scp465Party around(Player User) {
        for (Scp465Party Party : Parties.values()) {
            if (Party.inArea(User)) {
                return Party;
            }
        }
        return null;
    }

    public Collection<Scp465Party> all() {
        return Collections.unmodifiableCollection(Parties.values());
    }

    public List<Scp465Party> snapshotList() {
        return new ArrayList<>(Parties.values());
    }

    public boolean isEmpty() {
        return Parties.isEmpty();
    }

    @Override
    public CompoundTag save(CompoundTag Root, HolderLookup.Provider Access) {
        Root.putInt("NextId", NextId);
        ListTag Saved = new ListTag();
        for (Scp465Party Party : Parties.values()) {
            Saved.add(Party.save());
        }
        Root.put("Parties", Saved);
        return Root;
    }

    public static Scp465Parties load(CompoundTag Root, HolderLookup.Provider Access) {
        Scp465Parties Data = new Scp465Parties();
        Data.NextId = Math.max(1, Root.getInt("NextId"));
        ListTag Saved = Root.getList("Parties", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            Scp465Party Party = Scp465Party.load(Saved.getCompound(Index), Access.lookupOrThrow(Registries.BLOCK));
            if (Party != null) {
                Data.Parties.put(Party.id(), Party);
                Data.NextId = Math.max(Data.NextId, Party.id() + 1);
            }
        }
        return Data;
    }
}
