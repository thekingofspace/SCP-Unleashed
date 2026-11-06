package net.rai.scp_unleashed.scp001scarlet.throne;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ScarletThroneSite extends SavedData {
    public static final String DataName = "scp_unleashed_scp001_scarlet_throne";
    public static final long Salt = 0x5CA41E7L;
    private static final SavedData.Factory<ScarletThroneSite> DataFactory = new SavedData.Factory<>(ScarletThroneSite::new, ScarletThroneSite::load, null);

    private boolean Planned;
    private int CentreX;
    private int CentreZ;
    private int Floor;
    private long Seed;
    private final LongSet Done = new LongOpenHashSet();
    private boolean Rising;
    private int Stage;
    @Nullable
    private UUID Blob;
    @Nullable
    private UUID Leader;
    private long XkTick;

    public boolean rising() {
        return Rising;
    }

    public int stage() {
        return Stage;
    }

    @Nullable
    public UUID blob() {
        return Blob;
    }

    @Nullable
    public UUID leader() {
        return Leader;
    }

    public long xkTick() {
        return XkTick;
    }

    public void beginRising(@Nullable UUID Offering, @Nullable UUID Speaker, long PreviousTick) {
        Rising = true;
        Stage = 0;
        Blob = Offering;
        Leader = Speaker;
        XkTick = PreviousTick;
        setDirty();
    }

    public void stage(int Next) {
        Stage = Next;
        setDirty();
    }

    public void endRising() {
        Rising = false;
        Stage = 0;
        for (ChunkPos Each : chunks()) {
            Done.add(Each.toLong());
        }
        setDirty();
    }

    public static ScarletThroneSite get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean planned() {
        return Planned;
    }

    public BlockPos centre() {
        return new BlockPos(CentreX, Floor, CentreZ);
    }

    public long seed() {
        return Seed;
    }

    public boolean done(long Chunk) {
        return Done.contains(Chunk);
    }

    public void finish(long Chunk) {
        if (Done.add(Chunk)) {
            setDirty();
        }
    }

    public int finished() {
        return Done.size();
    }

    public List<ChunkPos> chunks() {
        List<ChunkPos> Found = new ArrayList<>();
        int Reach = ScarletThrone.RimRadius + 1;
        for (int Cx = (CentreX - Reach) >> 4; Cx <= (CentreX + Reach) >> 4; Cx++) {
            for (int Cz = (CentreZ - Reach) >> 4; Cz <= (CentreZ + Reach) >> 4; Cz++) {
                Found.add(new ChunkPos(Cx, Cz));
            }
        }
        return Found;
    }

    public boolean covers(ChunkPos Pos) {
        if (!Planned) {
            return false;
        }
        int Reach = ScarletThrone.RimRadius + 1;
        return Pos.getMaxBlockX() >= CentreX - Reach && Pos.getMinBlockX() <= CentreX + Reach && Pos.getMaxBlockZ() >= CentreZ - Reach && Pos.getMinBlockZ() <= CentreZ + Reach;
    }

    public boolean complete() {
        return Planned && Done.size() >= chunks().size();
    }

    public void plan(int X, int Z, int Y, long PlanSeed) {
        Planned = true;
        CentreX = X;
        CentreZ = Z;
        Floor = Y;
        Seed = PlanSeed;
        Done.clear();
        setDirty();
    }

    public void forget() {
        Planned = false;
        Rising = false;
        Stage = 0;
        Blob = null;
        Leader = null;
        Done.clear();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.putBoolean("Planned", Planned);
        Tag.putInt("X", CentreX);
        Tag.putInt("Z", CentreZ);
        Tag.putInt("Floor", Floor);
        Tag.putLong("Seed", Seed);
        Tag.put("Done", new LongArrayTag(Done.toLongArray()));
        Tag.putBoolean("Rising", Rising);
        Tag.putInt("Stage", Stage);
        if (Blob != null) {
            Tag.putUUID("Blob", Blob);
        }
        if (Leader != null) {
            Tag.putUUID("Leader", Leader);
        }
        Tag.putLong("XkTick", XkTick);
        return Tag;
    }

    public static ScarletThroneSite load(CompoundTag Tag, HolderLookup.Provider Registries) {
        ScarletThroneSite Data = new ScarletThroneSite();
        Data.Planned = Tag.getBoolean("Planned");
        Data.CentreX = Tag.getInt("X");
        Data.CentreZ = Tag.getInt("Z");
        Data.Floor = Tag.getInt("Floor");
        Data.Seed = Tag.getLong("Seed");
        for (long Each : Tag.getLongArray("Done")) {
            Data.Done.add(Each);
        }
        Data.Rising = Tag.getBoolean("Rising");
        Data.Stage = Tag.getInt("Stage");
        Data.Blob = Tag.hasUUID("Blob") ? Tag.getUUID("Blob") : null;
        Data.Leader = Tag.hasUUID("Leader") ? Tag.getUUID("Leader") : null;
        Data.XkTick = Tag.getLong("XkTick");
        return Data;
    }

    public static void placeAt(MinecraftServer Server, BlockPos Centre) {
        ScarletThroneSite Data = get(Server);
        Data.plan(Centre.getX(), Centre.getZ(), Centre.getY(), Server.overworld().getSeed() ^ Salt ^ Centre.asLong());
        ScarletThroneEmitter.clear();
        ScarletThroneEmitter.planned(Server.overworld(), Data);
    }

    public static void claim(MinecraftServer Server, BlockPos Centre) {
        ScarletThroneSite Data = get(Server);
        Data.plan(Centre.getX(), Centre.getZ(), Centre.getY(), Server.overworld().getSeed() ^ Salt ^ Centre.asLong());
        ScarletThroneEmitter.clear();
    }
}
