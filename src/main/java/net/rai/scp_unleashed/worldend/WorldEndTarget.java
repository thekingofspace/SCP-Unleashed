package net.rai.scp_unleashed.worldend;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.rai.scp_unleashed.mixin.worldend.WorldEndStorageAccessor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

public record WorldEndTarget(LevelStorageSource Parent, String LevelId, Path Folder, String Name) {
    private static final Logger Log = LogUtils.getLogger();

    @Nullable
    public static WorldEndTarget resolve(MinecraftServer Server) {
        LevelStorageSource.LevelStorageAccess Access = ((WorldEndStorageAccessor) Server).scpUnleashedStorage();
        if (Access == null) {
            Log.error("World ending: the server has no level storage access, nothing will be deleted");
            return null;
        }
        String Id = Access.getLevelId();
        LevelStorageSource Parent = Access.parent();
        Path Folder = Access.getLevelDirectory().path().toAbsolutePath().normalize();
        Path Base = Parent.getBaseDir().toAbsolutePath().normalize();
        Path Reported = Server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        String Name = Server.getWorldData().getLevelName();
        if (Id == null || Id.isBlank() || Id.contains("..") || Id.contains("/") || Id.contains("\\")) {
            Log.error("World ending: refusing an unusual level id '{}'", Id);
            return null;
        }
        if (!Folder.equals(Base.resolve(Id).normalize()) || Folder.equals(Base) || Folder.getParent() == null || !Folder.getParent().equals(Base)) {
            Log.error("World ending: refusing {}, it is not the level '{}' directly inside {}", Folder, Id, Base);
            return null;
        }
        if (!Folder.equals(Reported)) {
            Log.error("World ending: refusing {}, the server reports its world at {}", Folder, Reported);
            return null;
        }
        if (!Files.isRegularFile(Folder.resolve(LevelResource.LEVEL_DATA_FILE.getId()))) {
            Log.error("World ending: refusing {}, it holds no level.dat", Folder);
            return null;
        }
        return new WorldEndTarget(Parent, Id, Folder, Name);
    }

    public boolean delete() {
        Log.warn("World ending: deleting world '{}' (level id '{}') at {}", Name, LevelId, Folder);
        try (LevelStorageSource.LevelStorageAccess Fresh = Parent.validateAndCreateAccess(LevelId)) {
            Path Check = Fresh.getLevelDirectory().path().toAbsolutePath().normalize();
            if (!Check.equals(Folder)) {
                Log.error("World ending: the fresh storage access points at {} instead of {}, nothing deleted", Check, Folder);
                return false;
            }
            Fresh.deleteLevel();
            Log.warn("World ending: world '{}' deleted", Name);
            return true;
        } catch (Exception Problem) {
            Log.error("World ending: could not delete world '{}' at {}", Name, Folder, Problem);
            return false;
        }
    }
}
