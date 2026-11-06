package net.rai.scp_unleashed.raid.hostile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.rai.scp_unleashed.faction.FactionState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class HostileLedger {
    public static final String LedgerKey = "Ledger";
    public static final String NextKey = "LedgerNext";
    public static final long DayTicks = 24000L;

    public record Entry(int Id, HostileTrophy Trophy, long Time, long Day, int Level) {
        public CompoundTag save() {
            CompoundTag Saved = new CompoundTag();
            Saved.putInt("Id", Id);
            Saved.put("Trophy", Trophy.save());
            Saved.putLong("Time", Time);
            Saved.putLong("Day", Day);
            Saved.putInt("Level", Level);
            return Saved;
        }

        @Nullable
        public static Entry load(CompoundTag Saved) {
            HostileTrophy Trophy = HostileTrophy.load(Saved.getCompound("Trophy"));
            if (Trophy == null) {
                return null;
            }
            return new Entry(Saved.getInt("Id"), Trophy, Saved.getLong("Time"), Saved.getLong("Day"), Saved.getInt("Level"));
        }
    }

    private HostileLedger() {
    }

    private static ListTag raw(FactionState State) {
        return State.extra().getList(LedgerKey, Tag.TAG_COMPOUND);
    }

    public static List<Entry> entries(FactionState State) {
        List<Entry> Found = new ArrayList<>();
        ListTag Saved = raw(State);
        for (int Index = 0; Index < Saved.size(); Index++) {
            Entry One = Entry.load(Saved.getCompound(Index));
            if (One != null) {
                Found.add(One);
            }
        }
        return Found;
    }

    public static List<Entry> entries(FactionState State, Predicate<Entry> Test) {
        List<Entry> Found = new ArrayList<>();
        for (Entry One : entries(State)) {
            if (Test.test(One)) {
                Found.add(One);
            }
        }
        return Found;
    }

    public static int size(FactionState State) {
        return raw(State).size();
    }

    public static Entry add(FactionState State, HostileTrophy Trophy, long Time, long Day, int Level) {
        CompoundTag Extra = State.extra();
        int Id = Extra.getInt(NextKey) + 1;
        Extra.putInt(NextKey, Id);
        Entry Made = new Entry(Id, Trophy.copy(), Time, Day, Level);
        ListTag Saved = raw(State);
        Saved.add(Made.save());
        Extra.put(LedgerKey, Saved);
        State.changed();
        return Made;
    }

    @Nullable
    public static Entry find(FactionState State, int Id) {
        for (Entry One : entries(State)) {
            if (One.Id() == Id) {
                return One;
            }
        }
        return null;
    }

    public static boolean remove(FactionState State, int Id) {
        ListTag Saved = raw(State);
        for (int Index = 0; Index < Saved.size(); Index++) {
            if (Saved.getCompound(Index).getInt("Id") == Id) {
                Saved.remove(Index);
                State.extra().put(LedgerKey, Saved);
                State.changed();
                return true;
            }
        }
        return false;
    }

    public static void clear(FactionState State) {
        State.extra().remove(LedgerKey);
        State.changed();
    }

    public static long dayOf(long DayTime) {
        return Math.max(0L, DayTime) / DayTicks + 1L;
    }
}
