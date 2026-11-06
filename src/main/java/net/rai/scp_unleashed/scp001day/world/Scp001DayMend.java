package net.rai.scp_unleashed.scp001day.world;

import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.slf4j.Logger;

public final class Scp001DayMend {
    public static final int GroundSection = 0;
    public static final String Bedrock = "minecraft:bedrock";
    public static final String StatusKey = "Status";
    public static final String SectionsKey = "sections";
    public static final String LightKey = "isLightOn";
    public static final String HeightmapsKey = "Heightmaps";
    public static final String SaveKey = "shouldSave";
    public static final String SectionYKey = "Y";
    public static final String StatesKey = "block_states";
    public static final String PaletteKey = "palette";
    public static final String NameKey = "Name";
    public static final Set<String> LightLayers = Set.of("BlockLight", "SkyLight");
    private static final Logger Log = LogUtils.getLogger();
    private static final AtomicInteger Mended = new AtomicInteger();

    private Scp001DayMend() {
    }

    public static boolean city(ServerLevel World) {
        return World.getChunkSource().getGenerator() instanceof Scp001DayChunkGenerator;
    }

    public static int mended() {
        return Mended.get();
    }

    public static CompoundTag mend(ServerLevel World, CompoundTag Data) {
        if (!city(World) || !hollow(Data)) {
            return Data;
        }
        CompoundTag Fresh = Data.copy();
        Fresh.putString(StatusKey, BuiltInRegistries.CHUNK_STATUS.getKey(ChunkStatus.EMPTY).toString());
        Fresh.putBoolean(LightKey, false);
        Fresh.putBoolean(SaveKey, true);
        Fresh.remove(HeightmapsKey);
        ListTag Sections = Fresh.getList(SectionsKey, Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Sections.size(); Index++) {
            CompoundTag Section = Sections.getCompound(Index);
            for (String Layer : LightLayers) {
                Section.remove(Layer);
            }
        }
        int Count = Mended.incrementAndGet();
        if (Count == 1) {
            Log.info("SCP-001 city: rebuilding a chunk in {} that was saved without its ground", World.dimension().location());
        } else {
            Log.debug("SCP-001 city: rebuilding chunk {} {} in {} ({} so far)", Data.getInt("xPos"), Data.getInt("zPos"), World.dimension().location(), Count);
        }
        return Fresh;
    }

    public static boolean hollow(CompoundTag Data) {
        if (!Data.contains(StatusKey, Tag.TAG_STRING)) {
            return false;
        }
        ChunkStatus Status = ChunkStatus.byName(Data.getString(StatusKey));
        if (Status == null || Status.isBefore(ChunkStatus.NOISE)) {
            return false;
        }
        ListTag Sections = Data.getList(SectionsKey, Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Sections.size(); Index++) {
            CompoundTag Section = Sections.getCompound(Index);
            if (Section.getByte(SectionYKey) == GroundSection && bedded(Section)) {
                return false;
            }
        }
        return true;
    }

    private static boolean bedded(CompoundTag Section) {
        if (!Section.contains(StatesKey, Tag.TAG_COMPOUND)) {
            return false;
        }
        ListTag Palette = Section.getCompound(StatesKey).getList(PaletteKey, Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Palette.size(); Index++) {
            if (Bedrock.equals(Palette.getCompound(Index).getString(NameKey))) {
                return true;
            }
        }
        return false;
    }
}
