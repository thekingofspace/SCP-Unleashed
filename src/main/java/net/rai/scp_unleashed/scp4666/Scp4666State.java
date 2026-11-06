package net.rai.scp_unleashed.scp4666;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Scp4666State extends SavedData {
    public static final String DataName = "scp_unleashed_scp4666";
    public static final int FinalNight = 14;
    public static final int FoodStops = 10;
    public static final int LightsDie = 12;
    public static final int PhaseDay = 0;
    public static final int PhaseMidnight = 1;
    public static final int PhaseWoken = 2;
    public static final int PhaseEnding = 3;
    public static final int PhaseOver = 4;
    public static final int TreeFull = 4;
    public static final int StartFire = 6000;
    private static final SavedData.Factory<Scp4666State> DataFactory = new SavedData.Factory<>(Scp4666State::new, Scp4666State::load);

    public static final class Visitor {
        public final UUID Id;
        public String Name = "";
        public int Joined = 1;
        public int Mornings;
        public int Cocoas;
        public int TreeNights;
        public int Nights;
        public int Stares;
        public int Sightings;
        public int OutsideNights;
        public int LoudNights;
        public int ColdNights;
        public int Fate;

        public Visitor(UUID Id) {
            this.Id = Id;
        }

        public CompoundTag save() {
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Id", Id);
            Row.putString("Name", Name);
            Row.putInt("Joined", Joined);
            Row.putInt("Mornings", Mornings);
            Row.putInt("Cocoas", Cocoas);
            Row.putInt("TreeNights", TreeNights);
            Row.putInt("Nights", Nights);
            Row.putInt("Stares", Stares);
            Row.putInt("Sightings", Sightings);
            Row.putInt("OutsideNights", OutsideNights);
            Row.putInt("LoudNights", LoudNights);
            Row.putInt("ColdNights", ColdNights);
            Row.putInt("Fate", Fate);
            return Row;
        }

        public static Visitor load(CompoundTag Row) {
            Visitor Made = new Visitor(Row.getUUID("Id"));
            Made.Name = Row.getString("Name");
            Made.Joined = Row.getInt("Joined");
            Made.Mornings = Row.getInt("Mornings");
            Made.Cocoas = Row.getInt("Cocoas");
            Made.TreeNights = Row.getInt("TreeNights");
            Made.Nights = Row.getInt("Nights");
            Made.Stares = Row.getInt("Stares");
            Made.Sightings = Row.getInt("Sightings");
            Made.OutsideNights = Row.getInt("OutsideNights");
            Made.LoudNights = Row.getInt("LoudNights");
            Made.ColdNights = Row.getInt("ColdNights");
            Made.Fate = Row.getInt("Fate");
            return Made;
        }
    }

    private int Built = -1;
    private int Epoch;
    private int Night = 1;
    private long Clock = Scp4666Clock.Arrival;
    private int Phase = PhaseDay;
    private boolean LightsOn = true;
    private int Tree = TreeFull;
    private boolean WokeTonight;
    private boolean Started;
    private float Activity;
    private int Fire = StartFire;
    private int Coal;
    private boolean Offering;
    private final Set<Integer> Cracked = new HashSet<>();
    private final Set<Integer> Boarded = new HashSet<>();
    private final Set<String> Daily = new HashSet<>();
    private final Map<UUID, Visitor> Visitors = new LinkedHashMap<>();
    private final Set<String> Fired = new HashSet<>();
    private final Map<String, Integer> Counts = new HashMap<>();
    private final List<BlockPos> Prints = new ArrayList<>();
    private final List<UUID> Doomed = new ArrayList<>();
    private final Set<UUID> Cold = new HashSet<>();

    public static Scp4666State get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public int built() {
        return Built;
    }

    public void markBuilt(int Signature) {
        Built = Signature;
        setDirty();
    }

    public void unbuild() {
        Built = -1;
        setDirty();
    }

    public int epoch() {
        return Epoch;
    }

    public int night() {
        return Night;
    }

    public void setNight(int Value) {
        Night = Math.max(1, Math.min(FinalNight, Value));
        setDirty();
    }

    public long clock() {
        return Clock;
    }

    public void setClock(long Value) {
        Clock = Math.floorMod(Value, Scp4666Clock.Day);
        Scp4666Clock.set(Clock);
        setDirty();
    }

    public int phase() {
        return Phase;
    }

    public void setPhase(int Value) {
        Phase = Value;
        setDirty();
    }

    public boolean started() {
        return Started;
    }

    public void start() {
        if (!Started) {
            Started = true;
            setDirty();
        }
    }

    public boolean lightsOn() {
        return LightsOn && Night < LightsDie;
    }

    public boolean switchOn() {
        return LightsOn;
    }

    public void setLights(boolean On) {
        LightsOn = On;
        setDirty();
    }

    public boolean lightsDead() {
        return Night >= LightsDie;
    }

    public int tree() {
        return Tree;
    }

    public void setTree(int Value) {
        Tree = Math.max(0, Math.min(TreeFull, Value));
        setDirty();
    }

    public boolean wokeTonight() {
        return WokeTonight;
    }

    public void setWoke(boolean Value) {
        WokeTonight = Value;
        setDirty();
    }

    public float activity() {
        return Activity;
    }

    public void setActivity(float Value) {
        float Next = Math.max(0.0f, Math.min(100.0f, Value));
        if (Next != Activity) {
            Activity = Next;
            setDirty();
        }
    }

    public int fire() {
        return Fire;
    }

    public void setFire(int Value) {
        int Next = Math.max(0, Value);
        if (Next != Fire) {
            Fire = Next;
            setDirty();
        }
    }

    public int coal() {
        return Coal;
    }

    public void setCoal(int Value) {
        int Next = Math.max(0, Value);
        if (Next != Coal) {
            Coal = Next;
            setDirty();
        }
    }

    public boolean offering() {
        return Offering;
    }

    public void setOffering(boolean Value) {
        if (Value != Offering) {
            Offering = Value;
            setDirty();
        }
    }

    public Set<Integer> cracked() {
        return Cracked;
    }

    public Set<Integer> boarded() {
        return Boarded;
    }

    public void setWindow(int Index, boolean Crack, boolean Board) {
        boolean Changed = Crack ? Cracked.add(Index) : Cracked.remove(Index);
        Changed |= Board ? Boarded.add(Index) : Boarded.remove(Index);
        if (Changed) {
            setDirty();
        }
    }

    public boolean daily(String Key) {
        return Daily.contains(Key);
    }

    public void markDaily(String Key) {
        if (Daily.add(Key)) {
            setDirty();
        }
    }

    public void clearDaily() {
        if (!Daily.isEmpty()) {
            Daily.clear();
            setDirty();
        }
    }

    public Visitor visitor(UUID Id) {
        Visitor Found = Visitors.get(Id);
        if (Found == null) {
            Found = new Visitor(Id);
            Found.Joined = Night;
            Visitors.put(Id, Found);
            setDirty();
        }
        return Found;
    }

    @Nullable
    public Visitor known(UUID Id) {
        return Visitors.get(Id);
    }

    public List<Visitor> visitors() {
        return new ArrayList<>(Visitors.values());
    }

    public void touch() {
        setDirty();
    }

    public boolean fired(String Key) {
        return Fired.contains(Key);
    }

    public void fire(String Key) {
        Fired.add(Key);
        Counts.merge(Key, 1, Integer::sum);
        setDirty();
    }

    public int count(String Key) {
        return Counts.getOrDefault(Key, 0);
    }

    public void newNight() {
        Fired.removeIf(Key -> !Key.startsWith("once:"));
        WokeTonight = false;
        setDirty();
    }

    public List<BlockPos> prints() {
        return Prints;
    }

    public void addPrint(BlockPos Pos) {
        Prints.add(Pos.immutable());
        setDirty();
    }

    public void clearPrints() {
        Prints.clear();
        setDirty();
    }

    public List<UUID> doomed() {
        return Doomed;
    }

    public Set<UUID> cold() {
        return Cold;
    }

    public boolean isCold(UUID Id) {
        return Cold.contains(Id);
    }

    public void setCold(UUID Id, boolean Value) {
        if (Value ? Cold.add(Id) : Cold.remove(Id)) {
            setDirty();
        }
    }

    public void clearCold() {
        if (!Cold.isEmpty()) {
            Cold.clear();
            setDirty();
        }
    }

    public void reset() {
        Epoch++;
        Night = 1;
        Clock = Scp4666Clock.Arrival;
        Scp4666Clock.set(Clock);
        Phase = PhaseDay;
        LightsOn = true;
        Tree = TreeFull;
        WokeTonight = false;
        Started = false;
        Activity = 0.0f;
        Fire = StartFire;
        Coal = 0;
        Offering = false;
        Cracked.clear();
        Boarded.clear();
        Daily.clear();
        Visitors.clear();
        Fired.clear();
        Counts.clear();
        Prints.clear();
        Doomed.clear();
        Cold.clear();
        Built = -1;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Saved.putInt("Built", Built);
        Saved.putInt("Epoch", Epoch);
        Saved.putInt("Night", Night);
        Saved.putLong("Clock", Clock);
        Saved.putInt("Phase", Phase);
        Saved.putBoolean("LightsOn", LightsOn);
        Saved.putInt("Tree", Tree);
        Saved.putBoolean("Woke", WokeTonight);
        Saved.putBoolean("Started", Started);
        Saved.putFloat("Activity", Activity);
        Saved.putInt("Fire", Fire);
        Saved.putInt("Coal", Coal);
        Saved.putBoolean("Offering", Offering);
        Saved.putIntArray("Cracked", Cracked.stream().mapToInt(Integer::intValue).toArray());
        Saved.putIntArray("Boarded", Boarded.stream().mapToInt(Integer::intValue).toArray());
        ListTag Chores = new ListTag();
        for (String Key : Daily) {
            Chores.add(StringTag.valueOf(Key));
        }
        Saved.put("Daily", Chores);
        ListTag Rows = new ListTag();
        for (Visitor Entry : Visitors.values()) {
            Rows.add(Entry.save());
        }
        Saved.put("Visitors", Rows);
        ListTag Keys = new ListTag();
        for (String Key : Fired) {
            Keys.add(StringTag.valueOf(Key));
        }
        Saved.put("Fired", Keys);
        CompoundTag Tally = new CompoundTag();
        for (Map.Entry<String, Integer> Entry : Counts.entrySet()) {
            Tally.putInt(Entry.getKey(), Entry.getValue());
        }
        Saved.put("Counts", Tally);
        ListTag Marks = new ListTag();
        for (BlockPos Pos : Prints) {
            Marks.add(NbtUtils.writeBlockPos(Pos));
        }
        Saved.put("Prints", Marks);
        ListTag Fates = new ListTag();
        for (UUID Id : Doomed) {
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Id", Id);
            Fates.add(Row);
        }
        Saved.put("Doomed", Fates);
        ListTag Frozen = new ListTag();
        for (UUID Id : Cold) {
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Id", Id);
            Frozen.add(Row);
        }
        Saved.put("Cold", Frozen);
        return Saved;
    }

    public static Scp4666State load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Scp4666State Data = new Scp4666State();
        Data.Built = Saved.contains("Built") ? Saved.getInt("Built") : -1;
        Data.Epoch = Saved.getInt("Epoch");
        Data.Night = Math.max(1, Saved.getInt("Night"));
        Data.Clock = Saved.contains("Clock") ? Saved.getLong("Clock") : Scp4666Clock.Arrival;
        Scp4666Clock.set(Data.Clock);
        Data.Phase = Saved.getInt("Phase");
        Data.LightsOn = !Saved.contains("LightsOn") || Saved.getBoolean("LightsOn");
        Data.Tree = Saved.contains("Tree") ? Saved.getInt("Tree") : TreeFull;
        Data.WokeTonight = Saved.getBoolean("Woke");
        Data.Started = Saved.getBoolean("Started");
        Data.Activity = Saved.getFloat("Activity");
        Data.Fire = Saved.contains("Fire") ? Saved.getInt("Fire") : StartFire;
        Data.Coal = Saved.getInt("Coal");
        Data.Offering = Saved.getBoolean("Offering");
        for (int Index : Saved.getIntArray("Cracked")) {
            Data.Cracked.add(Index);
        }
        for (int Index : Saved.getIntArray("Boarded")) {
            Data.Boarded.add(Index);
        }
        for (Tag Raw : Saved.getList("Daily", Tag.TAG_STRING)) {
            Data.Daily.add(Raw.getAsString());
        }
        for (Tag Raw : Saved.getList("Visitors", Tag.TAG_COMPOUND)) {
            CompoundTag Row = (CompoundTag) Raw;
            if (Row.hasUUID("Id")) {
                Visitor Entry = Visitor.load(Row);
                Data.Visitors.put(Entry.Id, Entry);
            }
        }
        for (Tag Raw : Saved.getList("Fired", Tag.TAG_STRING)) {
            Data.Fired.add(Raw.getAsString());
        }
        CompoundTag Tally = Saved.getCompound("Counts");
        for (String Key : Tally.getAllKeys()) {
            Data.Counts.put(Key, Tally.getInt(Key));
        }
        for (Tag Raw : Saved.getList("Prints", Tag.TAG_INT_ARRAY)) {
            int[] Values = ((IntArrayTag) Raw).getAsIntArray();
            if (Values.length == 3) {
                Data.Prints.add(new BlockPos(Values[0], Values[1], Values[2]));
            }
        }
        for (Tag Raw : Saved.getList("Doomed", Tag.TAG_COMPOUND)) {
            CompoundTag Row = (CompoundTag) Raw;
            if (Row.hasUUID("Id")) {
                Data.Doomed.add(Row.getUUID("Id"));
            }
        }
        for (Tag Raw : Saved.getList("Cold", Tag.TAG_COMPOUND)) {
            CompoundTag Row = (CompoundTag) Raw;
            if (Row.hasUUID("Id")) {
                Data.Cold.add(Row.getUUID("Id"));
            }
        }
        return Data;
    }
}
