package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.rai.scp_unleashed.faction.FactionSite;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Scp610Hive {
    public static final String Kind = "hive";
    public static final String SpewerKey = "Spewer";
    public static final String FleshKey = "Flesh";
    public static final String PlanKey = "Plan";
    public static final String CarvedKey = "Carved";
    public static final String RoomsKey = "Rooms";
    public static final String TailKey = "Tail";
    public static final String BiomassKey = "Biomass";
    public static final String ScarsKey = "Scars";
    public static final String MergeTurnKey = "MergeTurn";
    public static final String JunctionsKey = "Junctions";
    public static final int ScarCap = 64;
    public static final int JunctionCap = 32;
    public final FactionSite Site;
    public final Scp610Owned Owned;
    public final Scp610Owned Frontier = new Scp610Owned();
    public final List<Scp610Chambers.Chamber> Plan = new ArrayList<>();
    public final List<Long> Scars = new ArrayList<>();
    public int Weavers;
    public int Burrowers;
    public double SurfaceCredit;
    public double ChamberCredit;
    public long NextMerge;
    public boolean Dirty;
    public boolean Migrated;
    public long NextRefill;
    public int ReachSq;
    public int Lowest = Integer.MAX_VALUE;
    @Nullable
    private List<Scp610Chambers.Op> Ops;
    @Nullable
    private Scp610Chambers.Chamber OpsFor;

    private Scp610Hive(FactionSite Site, Scp610Owned Owned) {
        this.Site = Site;
        this.Owned = Owned;
    }

    public static Scp610Hive load(FactionSite Site) {
        return load(Site, new Scp610Owned());
    }

    public static Scp610Hive load(FactionSite Site, Scp610Owned Owned) {
        Scp610Hive Made = new Scp610Hive(Site, Owned);
        CompoundTag Extra = Site.extra();
        if (Extra.contains(FleshKey)) {
            for (long Pos : Extra.getLongArray(FleshKey)) {
                Made.Owned.add(Pos);
            }
            Made.Migrated = true;
            Made.Dirty = true;
        }
        ListTag Rooms = Extra.getList(PlanKey, Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Rooms.size(); Index++) {
            Made.Plan.add(Scp610Chambers.Chamber.load(Rooms.getCompound(Index)));
        }
        for (long Pos : Extra.getLongArray(ScarsKey)) {
            Made.Scars.add(Pos);
        }
        return Made;
    }

    public void flush() {
        CompoundTag Extra = Site.extra();
        Extra.remove(FleshKey);
        ListTag Rooms = new ListTag();
        for (Scp610Chambers.Chamber Room : Plan) {
            Rooms.add(Room.save());
        }
        Extra.put(PlanKey, Rooms);
        long[] Marks = new long[Scars.size()];
        for (int Index = 0; Index < Marks.length; Index++) {
            Marks[Index] = Scars.get(Index);
        }
        Extra.putLongArray(ScarsKey, Marks);
        Dirty = false;
    }

    public int id() {
        return Site.id();
    }

    public BlockPos anchor() {
        return Site.anchor();
    }

    public int flesh() {
        return Owned.size();
    }

    @Nullable
    public UUID spewer() {
        CompoundTag Extra = Site.extra();
        return Extra.hasUUID(SpewerKey) ? Extra.getUUID(SpewerKey) : null;
    }

    public void setSpewer(UUID Id) {
        Site.extra().putUUID(SpewerKey, Id);
    }

    public double biomass() {
        return Site.extra().getDouble(BiomassKey);
    }

    public void setBiomass(double Value) {
        Site.extra().putDouble(BiomassKey, Math.max(0.0, Math.min(Scp610Biomass.capOf(this), Value)));
        Dirty = true;
    }

    public void scar(long Pos) {
        if (Scars.contains(Pos)) {
            return;
        }
        if (Scars.size() >= ScarCap) {
            Scars.remove(0);
        }
        Scars.add(Pos);
        Dirty = true;
    }

    public boolean healScar(long Pos) {
        if (Scars.remove(Long.valueOf(Pos))) {
            Dirty = true;
            return true;
        }
        return false;
    }

    public int mergeTurn() {
        return Site.extra().getInt(MergeTurnKey);
    }

    public void nextMergeTurn() {
        Site.extra().putInt(MergeTurnKey, mergeTurn() + 1);
        Dirty = true;
    }

    public int carved() {
        return Site.extra().getInt(CarvedKey);
    }

    public void addCarved() {
        Site.extra().putInt(CarvedKey, carved() + 1);
    }

    public int rooms() {
        return Site.extra().getInt(RoomsKey);
    }

    public void addRoom() {
        Site.extra().putInt(RoomsKey, rooms() + 1);
    }

    public BlockPos tail() {
        CompoundTag Extra = Site.extra();
        return Extra.contains(TailKey) ? BlockPos.of(Extra.getLong(TailKey)) : anchor().below(3);
    }

    public void setTail(BlockPos Pos) {
        Site.extra().putLong(TailKey, Pos.asLong());
    }

    public boolean surfaceLevel(long Pos) {
        int Y = BlockPos.getY(Pos);
        int Base = anchor().getY();
        return Y >= Base - Scp610Growth.SurfaceDepthCap - 2 && Y <= Base + Scp610Growth.SurfaceRiseCap;
    }

    public void touched(long Pos) {
        BlockPos Anchor = anchor();
        int Dx = BlockPos.getX(Pos) - Anchor.getX();
        int Dz = BlockPos.getZ(Pos) - Anchor.getZ();
        int Gap = Dx * Dx + Dz * Dz;
        if (Gap > ReachSq) {
            ReachSq = Gap;
        }
        int Y = BlockPos.getY(Pos);
        if (Y < Lowest) {
            Lowest = Y;
        }
        if (surfaceLevel(Pos)) {
            Frontier.add(Pos);
        }
    }

    public void survey() {
        Frontier.clear();
        ReachSq = 0;
        Lowest = Integer.MAX_VALUE;
        for (int At = 0; At < Owned.size(); At++) {
            touched(Owned.get(At));
        }
    }

    public int reach() {
        return (int) Math.ceil(Math.sqrt(ReachSq));
    }

    public List<Long> junctions() {
        List<Long> Found = new ArrayList<>();
        for (long Pos : Site.extra().getLongArray(JunctionsKey)) {
            Found.add(Pos);
        }
        return Found;
    }

    public void addJunction(BlockPos Pos) {
        List<Long> Found = junctions();
        Found.add(Pos.asLong());
        while (Found.size() > JunctionCap) {
            Found.remove(0);
        }
        long[] Marks = new long[Found.size()];
        for (int Index = 0; Index < Marks.length; Index++) {
            Marks[Index] = Found.get(Index);
        }
        Site.extra().putLongArray(JunctionsKey, Marks);
    }

    public boolean planningLord() {
        for (Scp610Chambers.Chamber Room : Plan) {
            if (Room.deep()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public Scp610Chambers.Chamber current() {
        return Plan.isEmpty() ? null : Plan.get(0);
    }

    public List<Scp610Chambers.Op> ops(Scp610Chambers.Chamber Room) {
        if (Ops == null || OpsFor != Room) {
            Ops = Scp610Chambers.ops(Room);
            OpsFor = Room;
        }
        return Ops;
    }

    public void finishRoom() {
        if (!Plan.isEmpty()) {
            Plan.remove(0);
        }
        Ops = null;
        OpsFor = null;
        Dirty = true;
    }

    public void forgetOps() {
        Ops = null;
        OpsFor = null;
    }
}
