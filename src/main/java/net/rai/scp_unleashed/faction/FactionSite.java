package net.rai.scp_unleashed.faction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FactionSite {
    private final int Id;
    private final String Kind;
    private final ResourceKey<Level> Dimension;
    private BlockPos Anchor;
    private Direction Front = Direction.SOUTH;
    private final long Seed;
    private boolean Built;
    private int Tries;
    private long NextTry;
    private long LastReinforce;
    private long LastSeen;
    private double Reserve;
    private final Map<String, List<BlockPos>> Marks = new LinkedHashMap<>();
    private CompoundTag Extra = new CompoundTag();

    public FactionSite(int Id, String Kind, ResourceKey<Level> Dimension, BlockPos Anchor, long Seed) {
        this.Id = Id;
        this.Kind = Kind;
        this.Dimension = Dimension;
        this.Anchor = Anchor.immutable();
        this.Seed = Seed;
    }

    public int id() {
        return Id;
    }

    public String kind() {
        return Kind;
    }

    public boolean is(String Name) {
        return Kind.equals(Name);
    }

    public ResourceKey<Level> dimension() {
        return Dimension;
    }

    public BlockPos anchor() {
        return Anchor;
    }

    public Direction front() {
        return Front;
    }

    public long seed() {
        return Seed;
    }

    public boolean built() {
        return Built;
    }

    public int tries() {
        return Tries;
    }

    public void failedTry(long Now, long Delay) {
        Tries++;
        NextTry = Now + Delay;
    }

    public boolean readyToTry(long Now) {
        return Now >= NextTry;
    }

    public void markBuilt(BlockPos Origin, Direction Facing, Map<String, List<BlockPos>> Found) {
        Anchor = Origin.immutable();
        Front = Facing;
        Built = true;
        Marks.clear();
        for (Map.Entry<String, List<BlockPos>> Entry : Found.entrySet()) {
            Marks.put(Entry.getKey(), new ArrayList<>(Entry.getValue()));
        }
    }

    public void moveAnchor(BlockPos Target) {
        if (!Built) {
            Anchor = Target.immutable();
        }
    }

    public List<BlockPos> marks(String Name) {
        return Marks.getOrDefault(Name, List.of());
    }

    @Nullable
    public BlockPos mark(String Name) {
        List<BlockPos> Found = marks(Name);
        return Found.isEmpty() ? null : Found.get(0);
    }

    public long lastReinforce() {
        return LastReinforce;
    }

    public void setLastReinforce(long Time) {
        LastReinforce = Time;
    }

    public long lastSeen() {
        return LastSeen;
    }

    public void setLastSeen(long Time) {
        LastSeen = Time;
    }

    public double reserve() {
        return Reserve;
    }

    public void setReserve(double Value) {
        Reserve = Math.max(0.0, Value);
    }

    public CompoundTag extra() {
        return Extra;
    }

    public CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        Saved.putInt("Id", Id);
        Saved.putString("Kind", Kind);
        Saved.putString("Dimension", Dimension.location().toString());
        Saved.putLong("Anchor", Anchor.asLong());
        Saved.putString("Front", Front.getSerializedName());
        Saved.putLong("Seed", Seed);
        Saved.putBoolean("Built", Built);
        Saved.putInt("Tries", Tries);
        Saved.putLong("NextTry", NextTry);
        Saved.putLong("LastReinforce", LastReinforce);
        Saved.putLong("LastSeen", LastSeen);
        Saved.putDouble("Reserve", Reserve);
        CompoundTag MarkTag = new CompoundTag();
        for (Map.Entry<String, List<BlockPos>> Entry : Marks.entrySet()) {
            ListTag Points = new ListTag();
            for (BlockPos Point : Entry.getValue()) {
                Points.add(LongTag.valueOf(Point.asLong()));
            }
            MarkTag.put(Entry.getKey(), Points);
        }
        Saved.put("Marks", MarkTag);
        Saved.put("Extra", Extra);
        return Saved;
    }

    @Nullable
    public static FactionSite load(CompoundTag Saved) {
        ResourceLocation Place = ResourceLocation.tryParse(Saved.getString("Dimension"));
        if (Place == null) {
            return null;
        }
        FactionSite Site = new FactionSite(Saved.getInt("Id"), Saved.getString("Kind"), ResourceKey.create(Registries.DIMENSION, Place), BlockPos.of(Saved.getLong("Anchor")),
                Saved.getLong("Seed"));
        Direction Facing = Direction.byName(Saved.getString("Front"));
        Site.Front = Facing != null && Facing.getAxis().isHorizontal() ? Facing : Direction.SOUTH;
        Site.Built = Saved.getBoolean("Built");
        Site.Tries = Saved.getInt("Tries");
        Site.NextTry = Saved.getLong("NextTry");
        Site.LastReinforce = Saved.getLong("LastReinforce");
        Site.LastSeen = Saved.getLong("LastSeen");
        Site.Reserve = Saved.getDouble("Reserve");
        CompoundTag MarkTag = Saved.getCompound("Marks");
        for (String Key : MarkTag.getAllKeys()) {
            ListTag Points = MarkTag.getList(Key, Tag.TAG_LONG);
            List<BlockPos> Found = new ArrayList<>();
            for (Tag Point : Points) {
                Found.add(BlockPos.of(((LongTag) Point).getAsLong()));
            }
            Site.Marks.put(Key, Found);
        }
        Site.Extra = Saved.getCompound("Extra");
        return Site;
    }
}
