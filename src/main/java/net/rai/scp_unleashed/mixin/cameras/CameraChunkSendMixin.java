package net.rai.scp_unleashed.mixin.cameras;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.world.level.ChunkPos;
import net.rai.scp_unleashed.cameras.CameraViews;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerChunkSender.class)
public abstract class CameraChunkSendMixin {
    @ModifyExpressionValue(method = "sendNextChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;chunkPosition()Lnet/minecraft/world/level/ChunkPos;"))
    private ChunkPos camerasSendNearLens(ChunkPos Original, @Local(argsOnly = true) ServerPlayer Viewer) {
        return CameraViews.sendCentre(Viewer, Original);
    }
}
