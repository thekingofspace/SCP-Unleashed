package net.rai.scp_unleashed.mixin.worldend;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftServer.class)
public interface WorldEndStorageAccessor {
    @Accessor("storageSource")
    LevelStorageSource.LevelStorageAccess scpUnleashedStorage();
}
