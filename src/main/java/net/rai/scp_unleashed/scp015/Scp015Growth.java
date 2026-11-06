package net.rai.scp_unleashed.scp015;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Scp015Growth extends SavedData {
    public static final String DataName = "scp_unleashed_scp015_growth";
    public static final int MaxColonies = 32;
    public static final int GrowthPerTick = 6;
    public static final int ChecksPerTick = 48;
    private static final SavedData.Factory<Scp015Growth> DataFactory = new SavedData.Factory<>(Scp015Growth::new, Scp015Growth::load);
    private static boolean Working;

    private final List<Scp015Colony> Colonies = new ArrayList<>();
    private int Segments;
    private int NextId;
    private int Cursor;

    public static Scp015Growth get(ServerLevel World) {
        return World.getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public static boolean isWorking() {
        return Working;
    }

    public static boolean isFree(BlockState State) {
        return !Scp015PipeBlock.isPipe(State) && (State.isAir() || State.canBeReplaced());
    }

    public int getSegments() {
        return Segments;
    }

    public int getColonyCount() {
        return Colonies.size();
    }

    public int getFrontCount() {
        int Count = 0;
        for (Scp015Colony Colony : Colonies) {
            Count += Colony.frontCount();
        }
        return Count;
    }

    public void segmentAdded() {
        Segments++;
        setDirty();
    }

    public void segmentRemoved() {
        Segments = Math.max(0, Segments - 1);
        setDirty();
    }

    public void seed(ServerLevel World, BlockPos Pos) {
        long StartAt = World.getGameTime() + Scp015Colony.StartDelay + World.random.nextInt(Scp015Colony.StartJitter);
        colonyFor(Pos, true).seed(Pos.immutable(), StartAt, World.random);
        setDirty();
    }

    public void wake(ServerLevel World, BlockPos Pos) {
        Scp015Colony Colony = colonyFor(Pos, false);
        Colony.wake(Pos.immutable(), World.getGameTime() + Scp015Colony.RegrowDelay);
        setDirty();
    }

    private Scp015Colony colonyFor(BlockPos Pos, boolean Fresh) {
        if (!Fresh) {
            for (Scp015Colony Colony : Colonies) {
                if (Colony.contains(Pos)) {
                    return Colony;
                }
            }
        }
        if (Colonies.size() < MaxColonies) {
            Scp015Colony Created = new Scp015Colony(NextId++, Pos);
            Colonies.add(Created);
            return Created;
        }
        Scp015Colony Nearest = nearest(Pos);
        return Nearest == null ? Colonies.get(0) : Nearest;
    }

    @Nullable
    private Scp015Colony nearest(BlockPos Pos) {
        Scp015Colony Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (Scp015Colony Colony : Colonies) {
            double Distance = Colony.distanceTo(Pos);
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Colony;
            }
        }
        return Best;
    }

    public void tick(ServerLevel World) {
        if (Colonies.isEmpty() || !World.tickRateManager().runsNormally()) {
            return;
        }
        int Limit = Scp015Registry.segmentLimit(World.getGameRules());
        Budget Spend = new Budget(GrowthPerTick, ChecksPerTick);
        int Count = Colonies.size();
        Cursor = Math.floorMod(Cursor, Count);
        boolean Changed = false;
        Working = true;
        try {
            for (int Step = 0; Step < Count; Step++) {
                Changed |= Colonies.get((Cursor + Step) % Count).tick(World, this, Limit, Spend);
            }
        } finally {
            Working = false;
        }
        Cursor++;
        Changed |= Colonies.removeIf(Scp015Colony::isIdle);
        if (Changed) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag Root, HolderLookup.Provider Registries) {
        Root.putInt("Segments", Segments);
        Root.putInt("NextId", NextId);
        ListTag Saved = new ListTag();
        for (Scp015Colony Colony : Colonies) {
            Saved.add(Colony.save());
        }
        Root.put("Colonies", Saved);
        return Root;
    }

    public static Scp015Growth load(CompoundTag Root, HolderLookup.Provider Registries) {
        Scp015Growth Data = new Scp015Growth();
        Data.Segments = Math.max(0, Root.getInt("Segments"));
        Data.NextId = Root.getInt("NextId");
        ListTag Saved = Root.getList("Colonies", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size() && Data.Colonies.size() < MaxColonies; Index++) {
            Data.Colonies.add(Scp015Colony.load(Saved.getCompound(Index)));
        }
        return Data;
    }

    static final class Budget {
        int Growth;
        int Checks;

        Budget(int Growth, int Checks) {
            this.Growth = Growth;
            this.Checks = Checks;
        }
    }
}
