package net.rai.scp_unleashed.scp140;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.rai.scp_unleashed.scp140.api.Scp140PageEvent;
import net.rai.scp_unleashed.scp140.api.Scp140Pages;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Scp140Chronicle extends SavedData {
    public static final String DataName = "scp_unleashed_scp140_chronicle";
    public static final long TicksPerDay = 24000L;
    private static final Logger Log = LogUtils.getLogger();
    private static final SavedData.Factory<Scp140Chronicle> DataFactory = new SavedData.Factory<>(Scp140Chronicle::new, Scp140Chronicle::load);

    private final Scp140Page[] Pages = new Scp140Page[Scp140Pages.Total];
    private final Map<UUID, Long> Seen = new HashMap<>();
    private int Written;
    private double Ink;
    private double TotalInk;
    private long LastWritten = -1L;
    private boolean Writing;
    private double Pending;

    public static Scp140Chronicle get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public int written() {
        return Written;
    }

    public double ink() {
        return Ink;
    }

    public double totalInk() {
        return TotalInk;
    }

    public long lastWritten() {
        return LastWritten;
    }

    public boolean isComplete() {
        return Written >= Scp140Pages.Total;
    }

    public double currentCost() {
        return isComplete() ? 0.0 : Scp140Ink.cost(Written + 1);
    }

    public double remaining() {
        return isComplete() ? 0.0 : Math.max(0.0, currentCost() - Ink);
    }

    public double progress() {
        return isComplete() ? 1.0 : Math.min(1.0, Ink / currentCost());
    }

    @Nullable
    public Scp140Page page(int Page) {
        return Page >= 1 && Page <= Scp140Pages.Total ? Pages[Page - 1] : null;
    }

    public Scp140Mood mood(long Now) {
        if (isComplete()) {
            return Scp140Mood.Complete;
        }
        if (Written > 0 && LastWritten >= 0L && Now - LastWritten < Scp140Ink.FreshTicks) {
            return Scp140Mood.Fresh;
        }
        return progress() >= Scp140Ink.GlistenFraction ? Scp140Mood.Glistens : Scp140Mood.Dry;
    }

    public int addInk(ServerLevel World, BlockPos Origin, double Amount) {
        if (Amount <= 0.0 || isComplete()) {
            return 0;
        }
        if (Writing) {
            Pending += Amount;
            return 0;
        }
        Scp140Mood Before = mood(World.getGameTime());
        Ink += Amount;
        TotalInk += Amount;
        setDirty();
        int Count = 0;
        Writing = true;
        try {
            while (true) {
                if (Pending > 0.0) {
                    Ink += Pending;
                    TotalInk += Pending;
                    Pending = 0.0;
                }
                if (isComplete() || Ink < currentCost()) {
                    break;
                }
                writePage(World, Origin);
                Count++;
            }
        } finally {
            Writing = false;
        }
        if (isComplete()) {
            Ink = 0.0;
            Pending = 0.0;
        }
        if (Count > 0 || Before != mood(World.getGameTime())) {
            changed(World.getServer());
        } else {
            Scp140Reading.progress(World.getServer());
        }
        return Count;
    }

    private void writePage(ServerLevel World, BlockPos Origin) {
        int Page = Written + 1;
        Ink -= Scp140Ink.cost(Page);
        BlockPos Site = null;
        ResourceKey<Level> SiteDimension = null;
        try {
            Scp140PageEvent Event = Scp140PageEvent.fire(World, Page, Origin);
            Site = Event.getSite();
            SiteDimension = Event.getSiteDimension();
        } catch (RuntimeException Problem) {
            Log.error("SCP-140 listeners failed while page {} was written", Page, Problem);
        }
        long Now = World.getGameTime();
        Pages[Page - 1] = new Scp140Page(Page, Site, SiteDimension, Origin.immutable(), World.dimension(), Now, day(World.getServer()));
        Written = Page;
        LastWritten = Now;
        setDirty();
        Scp140Effects.pageWritten(World, Origin, Page);
    }

    public int fill(ServerLevel World, BlockPos Origin, int Count) {
        int Done = 0;
        for (int Step = 0; Step < Count && !isComplete(); Step++) {
            Done += addInk(World, Origin, remaining() + 1.0E-6);
        }
        return Done;
    }

    public void reset(MinecraftServer Server) {
        Arrays.fill(Pages, null);
        Seen.clear();
        Written = 0;
        Ink = 0.0;
        TotalInk = 0.0;
        LastWritten = -1L;
        Pending = 0.0;
        setDirty();
        changed(Server);
    }

    public void changed(MinecraftServer Server) {
        Scp140Witnesses.refreshPlaced(Server);
        Scp140Reading.broadcast(Server);
    }

    public long seenMask(UUID Reader) {
        return Seen.getOrDefault(Reader, 0L);
    }

    public void markSeen(UUID Reader, int Page) {
        if (Page < 1 || Page > Written) {
            return;
        }
        long Bit = 1L << (Page - 1);
        long Before = seenMask(Reader);
        if ((Before & Bit) == 0L) {
            Seen.put(Reader, Before | Bit);
            setDirty();
        }
    }

    public static long day(MinecraftServer Server) {
        return Server.overworld().getDayTime() / TicksPerDay + 1L;
    }

    @Override
    public CompoundTag save(CompoundTag Root, HolderLookup.Provider Registries) {
        Root.putInt("Written", Written);
        Root.putDouble("Ink", Ink);
        Root.putDouble("TotalInk", TotalInk);
        Root.putLong("LastWritten", LastWritten);
        ListTag Entries = new ListTag();
        for (Scp140Page Entry : Pages) {
            if (Entry != null) {
                Entries.add(Entry.save());
            }
        }
        Root.put("Pages", Entries);
        ListTag Readers = new ListTag();
        for (Map.Entry<UUID, Long> Reader : Seen.entrySet()) {
            CompoundTag Saved = new CompoundTag();
            Saved.putUUID("Reader", Reader.getKey());
            Saved.putLong("Mask", Reader.getValue());
            Readers.add(Saved);
        }
        Root.put("Seen", Readers);
        return Root;
    }

    public static Scp140Chronicle load(CompoundTag Root, HolderLookup.Provider Registries) {
        Scp140Chronicle Data = new Scp140Chronicle();
        Data.Written = Math.max(0, Math.min(Scp140Pages.Total, Root.getInt("Written")));
        Data.Ink = Math.max(0.0, Root.getDouble("Ink"));
        Data.TotalInk = Math.max(0.0, Root.getDouble("TotalInk"));
        Data.LastWritten = Root.contains("LastWritten") ? Root.getLong("LastWritten") : -1L;
        ListTag Entries = Root.getList("Pages", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Entries.size(); Index++) {
            Scp140Page Entry = Scp140Page.load(Entries.getCompound(Index));
            if (Entry != null && Entry.Page() >= 1 && Entry.Page() <= Scp140Pages.Total) {
                Data.Pages[Entry.Page() - 1] = Entry;
            }
        }
        ListTag Readers = Root.getList("Seen", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Readers.size(); Index++) {
            CompoundTag Saved = Readers.getCompound(Index);
            if (Saved.hasUUID("Reader")) {
                Data.Seen.put(Saved.getUUID("Reader"), Saved.getLong("Mask"));
            }
        }
        return Data;
    }
}
