package net.rai.scp_unleashed.scp1730.puzzle;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Puzzles;

public class Scp1730PuzzleState extends SavedData {
    public static final String Name = "scp_unleashed_scp1730_puzzles";
    public static final int Counters = 4;

    private int Run = -1;
    private final Zone[] Zones = new Zone[Scp1730Puzzles.Zones];

    public final class Zone {
        public int Progress;
        public boolean Solved;
        public final int[] Counts = new int[Counters];
        public long Timer;
        public long Lockout;
        public int Taken;
        public long TakenAt;
        public int Hold;
        public boolean Waved;

        public void changed() {
            setDirty();
        }

        public boolean has(int Bit) {
            return (Progress & 1 << Bit) != 0;
        }

        CompoundTag save() {
            CompoundTag Tag = new CompoundTag();
            Tag.putInt("Progress", Progress);
            Tag.putBoolean("Solved", Solved);
            Tag.putIntArray("Counts", Counts.clone());
            Tag.putLong("Timer", Timer);
            Tag.putLong("Lockout", Lockout);
            Tag.putInt("Taken", Taken);
            Tag.putLong("TakenAt", TakenAt);
            Tag.putInt("Hold", Hold);
            Tag.putBoolean("Waved", Waved);
            return Tag;
        }

        void load(CompoundTag Tag) {
            Progress = Tag.getInt("Progress");
            Solved = Tag.getBoolean("Solved");
            int[] Saved = Tag.getIntArray("Counts");
            for (int Index = 0; Index < Counts.length; Index++) {
                Counts[Index] = Index < Saved.length ? Saved[Index] : 0;
            }
            Timer = Tag.getLong("Timer");
            Lockout = Tag.getLong("Lockout");
            Taken = Tag.getInt("Taken");
            TakenAt = Tag.getLong("TakenAt");
            Hold = Tag.getInt("Hold");
            Waved = Tag.getBoolean("Waved");
        }
    }

    public Scp1730PuzzleState() {
        for (int Index = 0; Index < Zones.length; Index++) {
            Zones[Index] = new Zone();
        }
    }

    public static Scp1730PuzzleState get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Scp1730PuzzleState::new, Scp1730PuzzleState::load, null), Name);
    }

    public static Scp1730PuzzleState load(CompoundTag Tag, HolderLookup.Provider Lookup) {
        Scp1730PuzzleState State = new Scp1730PuzzleState();
        State.Run = Tag.contains("Run") ? Tag.getInt("Run") : -1;
        for (int Index = 0; Index < State.Zones.length; Index++) {
            State.Zones[Index].load(Tag.getCompound("Zone" + Index));
        }
        return State;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Lookup) {
        Tag.putInt("Run", Run);
        for (int Index = 0; Index < Zones.length; Index++) {
            Tag.put("Zone" + Index, Zones[Index].save());
        }
        return Tag;
    }

    public int run() {
        return Run;
    }

    public Zone zone(int ForRun, int Index) {
        if (ForRun != Run) {
            Run = ForRun;
            for (int Each = 0; Each < Zones.length; Each++) {
                Zones[Each] = new Zone();
            }
            setDirty();
        }
        return Zones[Math.max(0, Math.min(Zones.length - 1, Index))];
    }
}
