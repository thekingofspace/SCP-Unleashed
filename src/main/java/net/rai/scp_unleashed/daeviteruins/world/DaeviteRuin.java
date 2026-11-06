package net.rai.scp_unleashed.daeviteruins.world;

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
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public final class DaeviteRuin {
    public static final int Planned = 0;
    public static final int Built = 2;
    public static final int Dart = 0;
    public static final int Collapse = 1;

    public static final class Guardian {
        public final BlockPos Pos;
        public final int Kind;
        public final int Facing;

        public Guardian(BlockPos Pos, int Kind, int Facing) {
            this.Pos = Pos.immutable();
            this.Kind = Kind;
            this.Facing = Facing;
        }
    }

    public static final class Trap {
        public final int Group;
        public final int Kind;
        public final List<BlockPos> Triggers = new ArrayList<>();
        public final List<BlockPos> Effects = new ArrayList<>();
        public boolean Sprung;
        public long Ready;
        public long Pending;

        public Trap(int Group, int Kind) {
            this.Group = Group;
            this.Kind = Kind;
        }
    }

    public int Id;
    public int Page;
    public ResourceKey<Level> Dimension = Level.OVERWORLD;
    public BlockPos Origin = BlockPos.ZERO;
    public BlockPos Target = BlockPos.ZERO;
    public int State = Planned;
    public int Tries;
    public long Created;
    public boolean Command;
    public BlockPos Site = BlockPos.ZERO;
    public Direction Front = Direction.SOUTH;
    public BlockPos Centre = BlockPos.ZERO;
    public BlockPos Entrance = BlockPos.ZERO;
    public BlockPos Low = BlockPos.ZERO;
    public BlockPos High = BlockPos.ZERO;
    public boolean Woken;
    public final List<Guardian> Guardians = new ArrayList<>();
    public final List<Trap> Traps = new ArrayList<>();

    public boolean built() {
        return State == Built;
    }

    public BlockPos where() {
        return built() ? Centre : Target;
    }

    public AABB bounds() {
        return new AABB(Low.getX(), Low.getY(), Low.getZ(), High.getX() + 1, High.getY() + 1, High.getZ() + 1);
    }

    public CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        Saved.putInt("Id", Id);
        Saved.putInt("Page", Page);
        Saved.putString("Dimension", Dimension.location().toString());
        Saved.putLong("Origin", Origin.asLong());
        Saved.putLong("Target", Target.asLong());
        Saved.putInt("State", State);
        Saved.putInt("Tries", Tries);
        Saved.putLong("Created", Created);
        Saved.putBoolean("Command", Command);
        Saved.putLong("Site", Site.asLong());
        Saved.putString("Front", Front.getSerializedName());
        Saved.putLong("Centre", Centre.asLong());
        Saved.putLong("Entrance", Entrance.asLong());
        Saved.putLong("Low", Low.asLong());
        Saved.putLong("High", High.asLong());
        Saved.putBoolean("Woken", Woken);
        ListTag Keepers = new ListTag();
        for (Guardian Entry : Guardians) {
            CompoundTag Row = new CompoundTag();
            Row.putLong("Pos", Entry.Pos.asLong());
            Row.putInt("Kind", Entry.Kind);
            Row.putInt("Facing", Entry.Facing);
            Keepers.add(Row);
        }
        Saved.put("Guardians", Keepers);
        ListTag Snares = new ListTag();
        for (Trap Entry : Traps) {
            CompoundTag Row = new CompoundTag();
            Row.putInt("Group", Entry.Group);
            Row.putInt("Kind", Entry.Kind);
            Row.putBoolean("Sprung", Entry.Sprung);
            Row.putLong("Ready", Entry.Ready);
            Row.putLong("Pending", Entry.Pending);
            ListTag Triggers = new ListTag();
            for (BlockPos Pos : Entry.Triggers) {
                Triggers.add(LongTag.valueOf(Pos.asLong()));
            }
            Row.put("Triggers", Triggers);
            ListTag Effects = new ListTag();
            for (BlockPos Pos : Entry.Effects) {
                Effects.add(LongTag.valueOf(Pos.asLong()));
            }
            Row.put("Effects", Effects);
            Snares.add(Row);
        }
        Saved.put("Traps", Snares);
        return Saved;
    }

    public static DaeviteRuin load(CompoundTag Saved) {
        DaeviteRuin Ruin = new DaeviteRuin();
        Ruin.Id = Saved.getInt("Id");
        Ruin.Page = Saved.getInt("Page");
        ResourceLocation Dim = ResourceLocation.tryParse(Saved.getString("Dimension"));
        Ruin.Dimension = Dim == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, Dim);
        Ruin.Origin = BlockPos.of(Saved.getLong("Origin"));
        Ruin.Target = BlockPos.of(Saved.getLong("Target"));
        Ruin.State = Saved.getInt("State");
        Ruin.Tries = Saved.getInt("Tries");
        Ruin.Created = Saved.getLong("Created");
        Ruin.Command = Saved.getBoolean("Command");
        Ruin.Site = BlockPos.of(Saved.getLong("Site"));
        Direction Facing = Direction.byName(Saved.getString("Front"));
        Ruin.Front = Facing != null && Facing.getAxis().isHorizontal() ? Facing : Direction.SOUTH;
        Ruin.Centre = BlockPos.of(Saved.getLong("Centre"));
        Ruin.Entrance = BlockPos.of(Saved.getLong("Entrance"));
        Ruin.Low = BlockPos.of(Saved.getLong("Low"));
        Ruin.High = BlockPos.of(Saved.getLong("High"));
        Ruin.Woken = Saved.getBoolean("Woken");
        ListTag Keepers = Saved.getList("Guardians", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Keepers.size(); Index++) {
            CompoundTag Row = Keepers.getCompound(Index);
            Ruin.Guardians.add(new Guardian(BlockPos.of(Row.getLong("Pos")), Row.getInt("Kind"), Row.getInt("Facing")));
        }
        ListTag Snares = Saved.getList("Traps", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Snares.size(); Index++) {
            CompoundTag Row = Snares.getCompound(Index);
            Trap Entry = new Trap(Row.getInt("Group"), Row.getInt("Kind"));
            Entry.Sprung = Row.getBoolean("Sprung");
            Entry.Ready = Row.getLong("Ready");
            Entry.Pending = Row.getLong("Pending");
            ListTag Triggers = Row.getList("Triggers", Tag.TAG_LONG);
            for (int Step = 0; Step < Triggers.size(); Step++) {
                Entry.Triggers.add(BlockPos.of(((LongTag) Triggers.get(Step)).getAsLong()));
            }
            ListTag Effects = Row.getList("Effects", Tag.TAG_LONG);
            for (int Step = 0; Step < Effects.size(); Step++) {
                Entry.Effects.add(BlockPos.of(((LongTag) Effects.get(Step)).getAsLong()));
            }
            Ruin.Traps.add(Entry);
        }
        return Ruin;
    }
}
