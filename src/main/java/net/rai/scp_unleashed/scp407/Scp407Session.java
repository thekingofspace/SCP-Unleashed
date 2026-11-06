package net.rai.scp_unleashed.scp407;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Scp407Session {
    public static final byte ActPending = 0;
    public static final byte ActGenesis = 1;
    public static final byte ActWanderer = 2;
    public static final byte Playing = 0;
    public static final byte Withering = 1;

    public final int Id;
    public long StartTick;
    public BlockPos Origin;
    public final Map<BlockPos, Track> Tracks = new LinkedHashMap<>();
    public byte Act = ActPending;
    public long ActTick = -1L;
    public byte State = Playing;
    public long WitherStart = -1L;
    public boolean Struck;
    public final LongOpenHashSet Region = new LongOpenHashSet();
    public final LongLinkedOpenHashSet Grown = new LongLinkedOpenHashSet();
    public final Long2ObjectOpenHashMap<BlockState> Replaced = new Long2ObjectOpenHashMap<>();
    public final LongArrayList Tips = new LongArrayList();
    public final LongArrayList TipGoals = new LongArrayList();
    public int Spread;
    public float Debt;
    public final Map<UUID, Listener> Listeners = new LinkedHashMap<>();
    public final Set<UUID> Creatures = new LinkedHashSet<>();
    @Nullable
    public UUID Titan;
    public boolean TitanSpawned;
    public int Sprouts;
    public long NextSprout = -1L;
    public int Browned;
    public int Greyed;
    public int Crumbled;
    public final LongArrayList Leftover = new LongArrayList();
    public final LongArrayList Fading = new LongArrayList();
    public final LongArrayList Surface = new LongArrayList();
    public final LongOpenHashSet SurfaceSet = new LongOpenHashSet();
    public final LongOpenHashSet Frontier = new LongOpenHashSet();
    public final LongArrayList FrontierList = new LongArrayList();
    public boolean Verified;
    public long LastSound;
    public int Cap = Scp407Growth.MaxSpread;
    @Nullable
    private RandomSource Dice;

    public Scp407Session(int Id, long StartTick, BlockPos Origin) {
        this.Id = Id;
        this.StartTick = StartTick;
        this.Origin = Origin.immutable();
    }

    public RandomSource random() {
        if (Dice == null) {
            Dice = RandomSource.create(Id * 7919L + StartTick * 31L);
        }
        return Dice;
    }

    public void reseed(long Seed) {
        random().setSeed(Seed);
    }

    public long position(long Now) {
        return Math.max(0L, Now - StartTick);
    }

    public boolean isPlaying() {
        return State == Playing;
    }

    public boolean isGenesis() {
        return Act == ActGenesis;
    }

    public boolean anyLoop() {
        for (Track Entry : Tracks.values()) {
            if (Entry.Loop) {
                return true;
            }
        }
        return false;
    }

    public boolean finished(long Now) {
        return Tracks.isEmpty() || !anyLoop() && position(Now) >= Scp407Song.Length;
    }

    @Nullable
    public Listener listener(UUID Key) {
        return Listeners.get(Key);
    }

    public int fusedCount() {
        int Count = 0;
        for (Listener Entry : Listeners.values()) {
            if (Entry.Fused) {
                Count++;
            }
        }
        return Count;
    }

    public CompoundTag save() {
        CompoundTag Data = new CompoundTag();
        Data.putInt("Id", Id);
        Data.putLong("Start", StartTick);
        Data.putLong("Origin", Origin.asLong());
        ListTag TrackList = new ListTag();
        for (Map.Entry<BlockPos, Track> Entry : Tracks.entrySet()) {
            CompoundTag Saved = new CompoundTag();
            Saved.putLong("Pos", Entry.getKey().asLong());
            Saved.putLong("Since", Entry.getValue().Since);
            Saved.putBoolean("Loop", Entry.getValue().Loop);
            Saved.putFloat("Volume", Entry.getValue().Volume);
            TrackList.add(Saved);
        }
        Data.put("Tracks", TrackList);
        Data.putByte("Act", Act);
        Data.putLong("ActTick", ActTick);
        Data.putByte("State", State);
        Data.putLong("WitherStart", WitherStart);
        Data.putBoolean("Struck", Struck);
        Data.put("Region", new LongArrayTag(Region.toLongArray()));
        Data.put("Grown", new LongArrayTag(Grown.toLongArray()));
        ListTag ReplacedList = new ListTag();
        for (Long2ObjectOpenHashMap.Entry<BlockState> Entry : Replaced.long2ObjectEntrySet()) {
            CompoundTag Saved = new CompoundTag();
            Saved.putLong("Pos", Entry.getLongKey());
            Saved.put("State", NbtUtils.writeBlockState(Entry.getValue()));
            ReplacedList.add(Saved);
        }
        Data.put("Replaced", ReplacedList);
        Data.put("Tips", new LongArrayTag(Tips.toLongArray()));
        Data.put("TipGoals", new LongArrayTag(TipGoals.toLongArray()));
        Data.putInt("Spread", Spread);
        Data.putFloat("Debt", Debt);
        ListTag ListenerList = new ListTag();
        for (Listener Entry : Listeners.values()) {
            ListenerList.add(Entry.save());
        }
        Data.put("Listeners", ListenerList);
        ListTag CreatureList = new ListTag();
        for (UUID Creature : Creatures) {
            CompoundTag Saved = new CompoundTag();
            Saved.putUUID("Id", Creature);
            CreatureList.add(Saved);
        }
        Data.put("Creatures", CreatureList);
        if (Titan != null) {
            Data.putUUID("Titan", Titan);
        }
        Data.putBoolean("TitanSpawned", TitanSpawned);
        Data.putInt("Sprouts", Sprouts);
        Data.putLong("NextSprout", NextSprout);
        Data.putInt("Browned", Browned);
        Data.putInt("Greyed", Greyed);
        Data.putInt("Crumbled", Crumbled);
        Data.put("Leftover", new LongArrayTag(Leftover.toLongArray()));
        return Data;
    }

    public static Scp407Session load(CompoundTag Data, HolderGetter<Block> Blocks) {
        Scp407Session Loaded = new Scp407Session(Data.getInt("Id"), Data.getLong("Start"), BlockPos.of(Data.getLong("Origin")));
        ListTag TrackList = Data.getList("Tracks", CompoundTag.TAG_COMPOUND);
        for (int Index = 0; Index < TrackList.size(); Index++) {
            CompoundTag Saved = TrackList.getCompound(Index);
            Loaded.Tracks.put(BlockPos.of(Saved.getLong("Pos")), new Track(Saved.getLong("Since"), Saved.getBoolean("Loop"), Saved.getFloat("Volume")));
        }
        Loaded.Act = Data.getByte("Act");
        Loaded.ActTick = Data.getLong("ActTick");
        Loaded.State = Data.getByte("State");
        Loaded.WitherStart = Data.getLong("WitherStart");
        Loaded.Struck = Data.getBoolean("Struck");
        for (long Value : Data.getLongArray("Region")) {
            Loaded.Region.add(Value);
        }
        for (long Value : Data.getLongArray("Grown")) {
            Loaded.Grown.add(Value);
        }
        ListTag ReplacedList = Data.getList("Replaced", CompoundTag.TAG_COMPOUND);
        for (int Index = 0; Index < ReplacedList.size(); Index++) {
            CompoundTag Saved = ReplacedList.getCompound(Index);
            Loaded.Replaced.put(Saved.getLong("Pos"), NbtUtils.readBlockState(Blocks, Saved.getCompound("State")));
        }
        long[] TipValues = Data.getLongArray("Tips");
        long[] GoalValues = Data.getLongArray("TipGoals");
        for (int Index = 0; Index < Math.min(TipValues.length, GoalValues.length); Index++) {
            Loaded.Tips.add(TipValues[Index]);
            Loaded.TipGoals.add(GoalValues[Index]);
        }
        Loaded.Spread = Data.getInt("Spread");
        Loaded.Debt = Data.getFloat("Debt");
        ListTag ListenerList = Data.getList("Listeners", CompoundTag.TAG_COMPOUND);
        for (int Index = 0; Index < ListenerList.size(); Index++) {
            Listener Entry = Listener.load(ListenerList.getCompound(Index));
            if (Entry != null) {
                Loaded.Listeners.put(Entry.Key, Entry);
            }
        }
        ListTag CreatureList = Data.getList("Creatures", CompoundTag.TAG_COMPOUND);
        for (int Index = 0; Index < CreatureList.size(); Index++) {
            CompoundTag Saved = CreatureList.getCompound(Index);
            if (Saved.hasUUID("Id")) {
                Loaded.Creatures.add(Saved.getUUID("Id"));
            }
        }
        Loaded.Titan = Data.hasUUID("Titan") ? Data.getUUID("Titan") : null;
        Loaded.TitanSpawned = Data.getBoolean("TitanSpawned");
        Loaded.Sprouts = Data.getInt("Sprouts");
        Loaded.NextSprout = Data.getLong("NextSprout");
        Loaded.Browned = Data.getInt("Browned");
        Loaded.Greyed = Data.getInt("Greyed");
        Loaded.Crumbled = Data.getInt("Crumbled");
        for (long Value : Data.getLongArray("Leftover")) {
            Loaded.Leftover.add(Value);
        }
        return Loaded;
    }

    public static final class Track {
        public final long Since;
        public boolean Loop;
        public float Volume;

        public Track(long Since, boolean Loop, float Volume) {
            this.Since = Since;
            this.Loop = Loop;
            this.Volume = Volume;
        }
    }

    public static final class Listener {
        public final UUID Key;
        public String Name;
        public boolean Person;
        public long FirstHeard;
        public long LastHeard;
        public boolean Fused;
        @Nullable
        public BlockPos Anchor;
        public double HoldX;
        public double HoldY;
        public double HoldZ;
        public int PodStage;
        public long PodTick;
        public boolean Struck;

        public Listener(UUID Key, String Name, boolean Person, long FirstHeard) {
            this.Key = Key;
            this.Name = Name;
            this.Person = Person;
            this.FirstHeard = FirstHeard;
        }

        public CompoundTag save() {
            CompoundTag Data = new CompoundTag();
            Data.putUUID("Key", Key);
            Data.putString("Name", Name);
            Data.putBoolean("Person", Person);
            Data.putLong("FirstHeard", FirstHeard);
            Data.putLong("LastHeard", LastHeard);
            Data.putBoolean("Fused", Fused);
            if (Anchor != null) {
                Data.putLong("Anchor", Anchor.asLong());
            }
            Data.putDouble("HoldX", HoldX);
            Data.putDouble("HoldY", HoldY);
            Data.putDouble("HoldZ", HoldZ);
            Data.putInt("PodStage", PodStage);
            Data.putLong("PodTick", PodTick);
            Data.putBoolean("Struck", Struck);
            return Data;
        }

        @Nullable
        public static Listener load(CompoundTag Data) {
            if (!Data.hasUUID("Key")) {
                return null;
            }
            Listener Entry = new Listener(Data.getUUID("Key"), Data.getString("Name"), Data.getBoolean("Person"), Data.getLong("FirstHeard"));
            Entry.LastHeard = Data.getLong("LastHeard");
            Entry.Fused = Data.getBoolean("Fused");
            Entry.Anchor = Data.contains("Anchor", CompoundTag.TAG_LONG) ? BlockPos.of(Data.getLong("Anchor")) : null;
            Entry.HoldX = Data.getDouble("HoldX");
            Entry.HoldY = Data.getDouble("HoldY");
            Entry.HoldZ = Data.getDouble("HoldZ");
            Entry.PodStage = Data.getInt("PodStage");
            Entry.PodTick = Data.getLong("PodTick");
            Entry.Struck = Data.getBoolean("Struck");
            return Entry;
        }
    }
}
