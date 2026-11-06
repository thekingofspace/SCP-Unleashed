package net.rai.scp_unleashed.intro;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IntroPlots extends SavedData {
    public static final String DataName = "scp_unleashed_intro_plots";
    private static final SavedData.Factory<IntroPlots> DataFactory = new SavedData.Factory<>(IntroPlots::new, IntroPlots::load);
    private final Map<UUID, Integer> Plots = new HashMap<>();
    private final Map<Integer, Integer> Built = new HashMap<>();
    private int Next;

    public static IntroPlots get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public int plotOf(UUID Player) {
        Integer Found = Plots.get(Player);
        if (Found != null) {
            return Found;
        }
        int Made = Next++;
        Plots.put(Player, Made);
        setDirty();
        return Made;
    }

    public boolean hasPlot(UUID Player) {
        return Plots.containsKey(Player);
    }

    public int count() {
        return Plots.size();
    }

    public int builtSignature(int Plot) {
        return Built.getOrDefault(Plot, 0);
    }

    public void markBuilt(int Plot, int Signature) {
        Built.put(Plot, Signature);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Saved.putInt("next", Next);
        ListTag Rows = new ListTag();
        for (Map.Entry<UUID, Integer> Entry : Plots.entrySet()) {
            CompoundTag Row = new CompoundTag();
            Row.putUUID("player", Entry.getKey());
            Row.putInt("plot", Entry.getValue());
            Rows.add(Row);
        }
        Saved.put("plots", Rows);
        ListTag Marks = new ListTag();
        for (Map.Entry<Integer, Integer> Entry : Built.entrySet()) {
            CompoundTag Row = new CompoundTag();
            Row.putInt("plot", Entry.getKey());
            Row.putInt("signature", Entry.getValue());
            Marks.add(Row);
        }
        Saved.put("built", Marks);
        return Saved;
    }

    public static IntroPlots load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        IntroPlots Loaded = new IntroPlots();
        Loaded.Next = Saved.getInt("next");
        for (Tag Each : Saved.getList("plots", Tag.TAG_COMPOUND)) {
            CompoundTag Row = (CompoundTag) Each;
            Loaded.Plots.put(Row.getUUID("player"), Row.getInt("plot"));
        }
        for (Tag Each : Saved.getList("built", Tag.TAG_COMPOUND)) {
            CompoundTag Row = (CompoundTag) Each;
            Loaded.Built.put(Row.getInt("plot"), Row.getInt("signature"));
        }
        for (int Plot : Loaded.Plots.values()) {
            Loaded.Next = Math.max(Loaded.Next, Plot + 1);
        }
        return Loaded;
    }
}
