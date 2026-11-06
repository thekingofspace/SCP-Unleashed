package net.rai.scp_unleashed.scp5001.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Scp5001EventData extends SavedData {
    public enum Kind { Rod, Laser, Generator, Core }

    public static final String Name = "scp_unleashed_scp5001_event";
    private final Map<Kind, Set<BlockPos>> Devices = new EnumMap<>(Kind.class);
    private final Set<BlockPos> Overdriven = new LinkedHashSet<>();
    private final Set<UUID> Swarm = new LinkedHashSet<>();
    private Scp5001State.Phase LastSeen = Scp5001State.Phase.Dormant;
    private boolean RodsSpent;
    private long CooldownEnd;
    private long RiseStart;
    private long NextWave;
    private long NextSwarm;
    private int SwarmWave;
    private int Abandoned;
    private long ResetStart;
    private int WaveSerial;

    public Scp5001EventData() {
        for (Kind Each : Kind.values()) {
            Devices.put(Each, new LinkedHashSet<>());
        }
    }

    public static Scp5001EventData get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Scp5001EventData::new, Scp5001EventData::load, null), Name);
    }

    public static Scp5001EventData load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp5001EventData Data = new Scp5001EventData();
        for (Kind Each : Kind.values()) {
            for (Tag Entry : Tag.getList("Devices" + Each.name(), Tag.TAG_LONG)) {
                Data.Devices.get(Each).add(BlockPos.of(((LongTag) Entry).getAsLong()));
            }
        }
        for (Tag Entry : Tag.getList("Overdriven", Tag.TAG_LONG)) {
            Data.Overdriven.add(BlockPos.of(((LongTag) Entry).getAsLong()));
        }
        for (Tag Entry : Tag.getList("Swarm", Tag.TAG_INT_ARRAY)) {
            Data.Swarm.add(NbtUtils.loadUUID(Entry));
        }
        String Seen = Tag.getString("LastSeen");
        for (Scp5001State.Phase Each : Scp5001State.Phase.values()) {
            if (Each.name().equals(Seen)) {
                Data.LastSeen = Each;
            }
        }
        Data.RodsSpent = Tag.getBoolean("RodsSpent");
        Data.CooldownEnd = Tag.getLong("CooldownEnd");
        Data.RiseStart = Tag.getLong("RiseStart");
        Data.NextWave = Tag.getLong("NextWave");
        Data.NextSwarm = Tag.getLong("NextSwarm");
        Data.SwarmWave = Tag.getInt("SwarmWave");
        Data.Abandoned = Tag.getInt("Abandoned");
        Data.ResetStart = Tag.getLong("ResetStart");
        Data.WaveSerial = Tag.getInt("WaveSerial");
        return Data;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        for (Kind Each : Kind.values()) {
            ListTag Entries = new ListTag();
            for (BlockPos Pos : Devices.get(Each)) {
                Entries.add(LongTag.valueOf(Pos.asLong()));
            }
            Tag.put("Devices" + Each.name(), Entries);
        }
        ListTag Hot = new ListTag();
        for (BlockPos Pos : Overdriven) {
            Hot.add(LongTag.valueOf(Pos.asLong()));
        }
        Tag.put("Overdriven", Hot);
        ListTag Spawned = new ListTag();
        for (UUID Id : Swarm) {
            Spawned.add(NbtUtils.createUUID(Id));
        }
        Tag.put("Swarm", Spawned);
        Tag.putString("LastSeen", LastSeen.name());
        Tag.putBoolean("RodsSpent", RodsSpent);
        Tag.putLong("CooldownEnd", CooldownEnd);
        Tag.putLong("RiseStart", RiseStart);
        Tag.putLong("NextWave", NextWave);
        Tag.putLong("NextSwarm", NextSwarm);
        Tag.putInt("SwarmWave", SwarmWave);
        Tag.putInt("Abandoned", Abandoned);
        Tag.putLong("ResetStart", ResetStart);
        Tag.putInt("WaveSerial", WaveSerial);
        return Tag;
    }

    public void register(Kind Type, BlockPos Pos) {
        if (Devices.get(Type).add(Pos.immutable())) {
            setDirty();
        }
    }

    public void forget(Kind Type, BlockPos Pos) {
        if (Devices.get(Type).remove(Pos)) {
            Overdriven.remove(Pos);
            setDirty();
        }
    }

    public List<BlockPos> devices(Kind Type, BlockPos Core, int Radius) {
        List<BlockPos> Found = new ArrayList<>();
        for (BlockPos Pos : Devices.get(Type)) {
            if (Math.abs(Pos.getX() - Core.getX()) <= Radius && Math.abs(Pos.getZ() - Core.getZ()) <= Radius && Math.abs(Pos.getY() - Core.getY()) <= Radius) {
                Found.add(Pos);
            }
        }
        Found.sort((A, B) -> Long.compare(A.asLong(), B.asLong()));
        return Found;
    }

    public List<BlockPos> allDevices() {
        List<BlockPos> Found = new ArrayList<>();
        for (Set<BlockPos> Each : Devices.values()) {
            Found.addAll(Each);
        }
        return Found;
    }

    public void clearDevices() {
        for (Set<BlockPos> Each : Devices.values()) {
            Each.clear();
        }
        Overdriven.clear();
        setDirty();
    }

    public Set<BlockPos> overdriven() {
        return Overdriven;
    }

    public boolean overdrive(BlockPos Pos) {
        boolean Added = Overdriven.add(Pos.immutable());
        if (Added) {
            setDirty();
        }
        return Added;
    }

    public void clearOverdrive() {
        if (!Overdriven.isEmpty()) {
            Overdriven.clear();
            setDirty();
        }
    }

    public Set<UUID> swarm() {
        return Swarm;
    }

    public void track(UUID Id) {
        Swarm.add(Id);
        setDirty();
    }

    public boolean untrack(UUID Id) {
        boolean Removed = Swarm.remove(Id);
        if (Removed) {
            setDirty();
        }
        return Removed;
    }

    public void clearSwarm() {
        Swarm.clear();
        SwarmWave = 0;
        NextSwarm = 0;
        setDirty();
    }

    public Scp5001State.Phase lastSeen() {
        return LastSeen;
    }

    public void lastSeen(Scp5001State.Phase Value) {
        if (LastSeen != Value) {
            LastSeen = Value;
            setDirty();
        }
    }

    public boolean rodsSpent() {
        return RodsSpent;
    }

    public void rodsSpent(boolean Value) {
        RodsSpent = Value;
        setDirty();
    }

    public long cooldownEnd() {
        return CooldownEnd;
    }

    public void cooldownEnd(long Value) {
        CooldownEnd = Value;
        setDirty();
    }

    public boolean cooling(long Now) {
        return CooldownEnd > 0 && Now < CooldownEnd;
    }

    public long riseStart() {
        return RiseStart;
    }

    public void riseStart(long Value) {
        RiseStart = Value;
        setDirty();
    }

    public long nextWave() {
        return NextWave;
    }

    public void nextWave(long Value) {
        NextWave = Value;
        setDirty();
    }

    public long nextSwarm() {
        return NextSwarm;
    }

    public void nextSwarm(long Value) {
        NextSwarm = Value;
        setDirty();
    }

    public int swarmWave() {
        return SwarmWave;
    }

    public void swarmWave(int Value) {
        SwarmWave = Value;
        setDirty();
    }

    public int abandoned() {
        return Abandoned;
    }

    public void abandoned(int Value) {
        if (Abandoned != Value) {
            Abandoned = Value;
            setDirty();
        }
    }

    public long resetStart() {
        return ResetStart;
    }

    public void resetStart(long Value) {
        ResetStart = Value;
        setDirty();
    }

    public int waveSerial() {
        return WaveSerial;
    }

    public int nextWaveSerial() {
        WaveSerial++;
        setDirty();
        return WaveSerial;
    }
}
