package net.rai.scp_unleashed.mixin.scp5001;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.scp5001.mekhane.entity.Mekhane;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class MekhaneTrackingMixin {
    @Shadow
    @Final
    Entity entity;

    @ModifyExpressionValue(method = "updatePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;getPlayerViewDistance(Lnet/minecraft/server/level/ServerPlayer;)I"))
    private int mekhaneSeenPastViewDistance(int Original) {
        return entity instanceof Mekhane ? Math.max(Original, Mekhane.TrackChunks) : Original;
    }

    @ModifyExpressionValue(method = "updatePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;isChunkTracked(Lnet/minecraft/server/level/ServerPlayer;II)Z"))
    private boolean mekhaneSeenOverUnsentChunks(boolean Original) {
        return Original || entity instanceof Mekhane;
    }
}
