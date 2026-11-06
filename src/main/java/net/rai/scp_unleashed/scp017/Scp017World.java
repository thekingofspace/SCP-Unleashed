package net.rai.scp_unleashed.scp017;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Scp017World extends SavedData {
    public static final String DataName = "scp_unleashed_scp017";
    private static final SavedData.Factory<Scp017World> DataFactory = new SavedData.Factory<>(Scp017World::new, Scp017World::load);

    @Nullable
    private UUID Loose;

    public static Scp017World get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    @Nullable
    public UUID loose() {
        return Loose;
    }

    public boolean blocks(@Nullable UUID Candidate) {
        return Loose != null && !Loose.equals(Candidate);
    }

    public boolean claim(UUID Candidate) {
        if (blocks(Candidate)) {
            return false;
        }
        if (!Candidate.equals(Loose)) {
            Loose = Candidate;
            setDirty();
        }
        return true;
    }

    public void free(UUID Candidate) {
        if (Candidate.equals(Loose)) {
            Loose = null;
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        if (Loose != null) {
            Tag.putUUID("Loose", Loose);
        }
        return Tag;
    }

    public static Scp017World load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp017World World = new Scp017World();
        if (Tag.hasUUID("Loose")) {
            World.Loose = Tag.getUUID("Loose");
        }
        return World;
    }
}
