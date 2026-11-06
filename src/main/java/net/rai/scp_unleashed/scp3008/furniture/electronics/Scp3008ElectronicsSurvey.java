package net.rai.scp_unleashed.scp3008.furniture.electronics;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.world.Scp3008ChunkGenerator;
import net.rai.scp_unleashed.scp3008.world.Scp3008Showrooms;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp3008ElectronicsSurvey extends SavedData {
    public static final String DataName = "scp_unleashed_scp3008_departments";
    public static final int Version = 1;
    private static final Logger Log = LogUtils.getLogger();
    private static final SavedData.Factory<Scp3008ElectronicsSurvey> DataFactory = new SavedData.Factory<>(() -> new Scp3008ElectronicsSurvey(List.of(), 0),
            Scp3008ElectronicsSurvey::load);

    private final List<Long> Sites;
    private final int Chunks;

    public Scp3008ElectronicsSurvey(Collection<Long> Sites, int Chunks) {
        this.Sites = List.copyOf(Sites);
        this.Chunks = Chunks;
    }

    public List<Long> sites() {
        return Sites;
    }

    public int chunks() {
        return Chunks;
    }

    public static Scp3008ElectronicsSurvey load(CompoundTag Saved, HolderLookup.Provider Registries) {
        List<Long> Found = new ArrayList<>();
        for (long Site : Saved.getLongArray("Sites")) {
            Found.add(Site);
        }
        return new Scp3008ElectronicsSurvey(Found, Saved.getInt("Chunks"));
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Registries) {
        Saved.putInt("Version", Version);
        Saved.putInt("Chunks", Chunks);
        Saved.putLongArray("Sites", Sites.stream().mapToLong(Long::longValue).toArray());
        return Saved;
    }

    public static Scp3008ElectronicsSurvey survey(Path Region) throws IOException {
        List<Long> Generated = Scp3008ElectronicsLedger.folderChunks(Region);
        Set<Long> Frozen = Scp3008ElectronicsLedger.sitesTouching(Generated);
        return new Scp3008ElectronicsSurvey(Frozen, Generated.size());
    }

    public static Scp3008ElectronicsSurvey of(ServerLevel World) {
        DimensionDataStorage Storage = World.getDataStorage();
        Scp3008ElectronicsSurvey Found = Storage.get(DataFactory, DataName);
        if (Found != null) {
            return Found;
        }
        Path Region = DimensionType.getStorageFolder(World.dimension(), World.getServer().getWorldPath(LevelResource.ROOT)).resolve("region");
        try {
            Found = survey(Region);
        } catch (IOException Problem) {
            Log.error("SCP-3008 could not read the explored store at {}, so every department stays as it was", Region, Problem);
            return null;
        }
        Storage.set(DataName, Found);
        Found.setDirty();
        Storage.save();
        Log.info("SCP-3008 departments surveyed: {} explored chunks keep {} department sites as they were", Found.Chunks, Found.Sites.size());
        return Found;
    }

    @SubscribeEvent
    public static void loaded(LevelEvent.Load Event) {
        if (!(Event.getLevel() instanceof ServerLevel World) || !(World.getChunkSource().getGenerator() instanceof Scp3008ChunkGenerator)) {
            return;
        }
        Scp3008ElectronicsSurvey Found = of(World);
        if (Found == null) {
            Scp3008ElectronicsLedger.freezeAll();
        } else {
            Scp3008ElectronicsLedger.install(Found.Sites);
        }
        Scp3008Showrooms.forget();
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp3008ElectronicsLedger.reset();
        Scp3008Showrooms.forget();
    }
}
