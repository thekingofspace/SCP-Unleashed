package net.rai.scp_unleashed.scp465;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Scp465Party {
    public static final int CheerTicks = 120;
    public static final int Servings = 4;
    public static final double GuardRadius = 4.0;
    public static final double WatchRadius = 7.5;

    private final int Id;
    private final BlockPos BoxPos;
    private final BlockPos Low;
    private final int[] Cells;
    private final int[] Kept;
    private final int FloorY;
    private final Scp465Snapshot Snapshot;
    private final Set<UUID> Celebrants;
    private final List<UUID> Guests = new ArrayList<>();
    @Nullable
    private UUID Dj;
    private final BlockPos Booth;
    private final Direction BoothFacing;
    private final BlockPos DjStand;
    private final List<BlockPos> Bar;
    private final List<BlockPos> BarFront;
    private final List<BlockPos> Dance;
    private final List<BlockPos> Floor;
    private final Map<UUID, Integer> Served = new HashMap<>();
    private final List<String> Names = new ArrayList<>();
    private final long OpenedAt;
    private final long MusicStart;
    private final LongOpenHashSet Area = new LongOpenHashSet();
    private final int[] Restore;
    private final BlockPos High;
    private final Map<UUID, UUID> Bouncers = new HashMap<>();

    public Scp465Party(int Id, BlockPos BoxPos, BlockPos Low, int[] Cells, int[] Kept, int FloorY, Scp465Snapshot Snapshot, Set<UUID> Celebrants,
                       BlockPos Booth, Direction BoothFacing, BlockPos DjStand, List<BlockPos> Bar, List<BlockPos> BarFront, List<BlockPos> Dance, List<BlockPos> Floor,
                       long OpenedAt, long MusicStart) {
        this.Id = Id;
        this.BoxPos = BoxPos.immutable();
        this.Low = Low.immutable();
        this.Cells = Cells.clone();
        this.Kept = Kept.clone();
        this.FloorY = FloorY;
        this.Snapshot = Snapshot;
        this.Celebrants = new LinkedHashSet<>(Celebrants);
        this.Booth = Booth.immutable();
        this.BoothFacing = BoothFacing;
        this.DjStand = DjStand.immutable();
        this.Bar = List.copyOf(Bar);
        this.BarFront = List.copyOf(BarFront);
        this.Dance = List.copyOf(Dance);
        this.Floor = List.copyOf(Floor);
        this.OpenedAt = OpenedAt;
        this.MusicStart = MusicStart;
        int TopX = Low.getX();
        int TopY = Low.getY();
        int TopZ = Low.getZ();
        for (int Packed : this.Cells) {
            BlockPos Pos = Scp465Snapshot.unpack(Low, Packed);
            Area.add(Pos.asLong());
            TopX = Math.max(TopX, Pos.getX());
            TopY = Math.max(TopY, Pos.getY());
            TopZ = Math.max(TopZ, Pos.getZ());
        }
        this.High = new BlockPos(TopX, TopY, TopZ);
        Set<Integer> Skipped = new HashSet<>();
        for (int Packed : this.Kept) {
            Skipped.add(Packed);
        }
        this.Restore = Arrays.stream(this.Cells).filter(Packed -> !Skipped.contains(Packed)).toArray();
    }

    public int id() {
        return Id;
    }

    public BlockPos boxPos() {
        return BoxPos;
    }

    public BlockPos low() {
        return Low;
    }

    public BlockPos high() {
        return High;
    }

    public int[] restoreCells() {
        return Restore;
    }

    public int[] cells() {
        return Cells;
    }

    public int floorY() {
        return FloorY;
    }

    public Scp465Snapshot snapshot() {
        return Snapshot;
    }

    public Set<UUID> celebrants() {
        return Collections.unmodifiableSet(Celebrants);
    }

    public boolean isCelebrant(Entity Target) {
        return Celebrants.contains(Target.getUUID());
    }

    public List<UUID> guests() {
        return Collections.unmodifiableList(Guests);
    }

    public void addGuest(UUID Guest, boolean IsDj) {
        Guests.add(Guest);
        if (IsDj) {
            Dj = Guest;
        }
    }

    @Nullable
    public UUID dj() {
        return Dj;
    }

    public BlockPos booth() {
        return Booth;
    }

    public Vec3 boothCentre() {
        return Vec3.atBottomCenterOf(Booth);
    }

    public Direction boothFacing() {
        return BoothFacing;
    }

    public BlockPos djStand() {
        return DjStand;
    }

    public List<BlockPos> bar() {
        return Bar;
    }

    public List<BlockPos> barFront() {
        return BarFront;
    }

    public List<BlockPos> dance() {
        return Dance;
    }

    public List<BlockPos> floor() {
        return Floor;
    }

    public long openedAt() {
        return OpenedAt;
    }

    public long musicStart() {
        return MusicStart;
    }

    public List<String> names() {
        return Names;
    }

    public Map<UUID, UUID> bouncers() {
        return Bouncers;
    }

    public int served(UUID Who) {
        return Served.getOrDefault(Who, 0);
    }

    public void serve(UUID Who) {
        Served.merge(Who, 1, Integer::sum);
    }

    public boolean contains(BlockPos Pos) {
        return Area.contains(Pos.asLong());
    }

    public boolean contains(long Packed) {
        return Area.contains(Packed);
    }

    public AABB bounds() {
        return new AABB(Low.getX(), Low.getY(), Low.getZ(), High.getX() + 1.0, High.getY() + 1.0, High.getZ() + 1.0);
    }

    public boolean inArea(Entity Target) {
        BlockPos Feet = BlockPos.containing(Target.getX(), Target.getY() + 0.2, Target.getZ());
        if (contains(Feet)) {
            return true;
        }
        return contains(BlockPos.containing(Target.getEyePosition()));
    }

    public boolean overlaps(Scp465Party Other) {
        if (!bounds().intersects(Other.bounds())) {
            return false;
        }
        for (long Packed : Area) {
            if (Other.Area.contains(Packed)) {
                return true;
            }
        }
        return false;
    }

    public boolean overlaps(LongOpenHashSet Others) {
        for (long Packed : Others) {
            if (Area.contains(Packed)) {
                return true;
            }
        }
        return false;
    }

    public CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        Saved.putInt("Id", Id);
        Saved.put("Box", NbtUtils.writeBlockPos(BoxPos));
        Saved.put("Low", NbtUtils.writeBlockPos(Low));
        Saved.putIntArray("Cells", Cells.clone());
        Saved.putIntArray("Kept", Kept.clone());
        Saved.putInt("FloorY", FloorY);
        Saved.put("Snapshot", Snapshot.save());
        ListTag Who = new ListTag();
        for (UUID Each : Celebrants) {
            Who.add(NbtUtils.createUUID(Each));
        }
        Saved.put("Celebrants", Who);
        ListTag Party = new ListTag();
        for (UUID Each : Guests) {
            Party.add(NbtUtils.createUUID(Each));
        }
        Saved.put("Guests", Party);
        if (Dj != null) {
            Saved.putUUID("Dj", Dj);
        }
        Saved.put("Booth", NbtUtils.writeBlockPos(Booth));
        Saved.putInt("BoothFacing", BoothFacing.get3DDataValue());
        Saved.put("DjStand", NbtUtils.writeBlockPos(DjStand));
        Saved.put("Bar", positions(Bar));
        Saved.put("BarFront", positions(BarFront));
        Saved.put("Dance", positions(Dance));
        Saved.put("Floor", positions(Floor));
        ListTag Plates = new ListTag();
        for (Map.Entry<UUID, Integer> Entry : Served.entrySet()) {
            CompoundTag Record = new CompoundTag();
            Record.putUUID("Who", Entry.getKey());
            Record.putInt("Count", Entry.getValue());
            Plates.add(Record);
        }
        Saved.put("Served", Plates);
        ListTag Called = new ListTag();
        for (String Name : Names) {
            Called.add(StringTag.valueOf(Name));
        }
        Saved.put("Names", Called);
        Saved.putLong("OpenedAt", OpenedAt);
        Saved.putLong("MusicStart", MusicStart);
        return Saved;
    }

    private IntArrayTag positions(List<BlockPos> Spots) {
        int[] Packed = new int[Spots.size()];
        for (int Index = 0; Index < Spots.size(); Index++) {
            Packed[Index] = Scp465Snapshot.packOf(Low, Spots.get(Index));
        }
        return new IntArrayTag(Packed);
    }

    private static List<BlockPos> positions(BlockPos Low, int[] Packed) {
        List<BlockPos> Found = new ArrayList<>();
        for (int Value : Packed) {
            Found.add(Scp465Snapshot.unpack(Low, Value));
        }
        return Found;
    }

    @Nullable
    public static Scp465Party load(CompoundTag Saved, HolderGetter<Block> BlockLookup) {
        BlockPos Low = NbtUtils.readBlockPos(Saved, "Low").orElse(null);
        BlockPos Box = NbtUtils.readBlockPos(Saved, "Box").orElse(null);
        BlockPos BoothSpot = NbtUtils.readBlockPos(Saved, "Booth").orElse(null);
        BlockPos Stand = NbtUtils.readBlockPos(Saved, "DjStand").orElse(null);
        if (Low == null || Box == null || BoothSpot == null || Stand == null) {
            return null;
        }
        Set<UUID> Who = new LinkedHashSet<>();
        ListTag Listed = Saved.getList("Celebrants", Tag.TAG_INT_ARRAY);
        for (Tag Each : Listed) {
            Who.add(NbtUtils.loadUUID(Each));
        }
        Scp465Party Made = new Scp465Party(Saved.getInt("Id"), Box, Low, Saved.getIntArray("Cells"), Saved.getIntArray("Kept"), Saved.getInt("FloorY"),
                Scp465Snapshot.load(Saved.getCompound("Snapshot"), BlockLookup), Who, BoothSpot, Direction.from3DDataValue(Saved.getInt("BoothFacing")), Stand,
                positions(Low, Saved.getIntArray("Bar")), positions(Low, Saved.getIntArray("BarFront")), positions(Low, Saved.getIntArray("Dance")),
                positions(Low, Saved.getIntArray("Floor")), Saved.getLong("OpenedAt"), Saved.getLong("MusicStart"));
        UUID Dj = Saved.hasUUID("Dj") ? Saved.getUUID("Dj") : null;
        for (Tag Each : Saved.getList("Guests", Tag.TAG_INT_ARRAY)) {
            UUID Guest = NbtUtils.loadUUID(Each);
            Made.addGuest(Guest, Guest.equals(Dj));
        }
        for (Tag Each : Saved.getList("Served", Tag.TAG_COMPOUND)) {
            CompoundTag Record = (CompoundTag) Each;
            if (Record.hasUUID("Who")) {
                Made.Served.put(Record.getUUID("Who"), Record.getInt("Count"));
            }
        }
        for (Tag Each : Saved.getList("Names", Tag.TAG_STRING)) {
            Made.Names.add(Each.getAsString());
        }
        return Made;
    }
}
