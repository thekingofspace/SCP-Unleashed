package net.rai.scp_unleashed.mixin.cameras;

import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.rai.scp_unleashed.cameras.CameraTrackingView;
import net.rai.scp_unleashed.cameras.CameraViews;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ChunkMap.class)
public abstract class CameraChunkViewMixin {
    @Shadow
    @Final
    ServerLevel level;

    @Shadow
    abstract int getPlayerViewDistance(ServerPlayer Viewer);

    @Shadow
    private void markChunkPendingToSend(ServerPlayer Viewer, ChunkPos Pos) {
        throw new AssertionError();
    }

    @Shadow
    private static void dropChunk(ServerPlayer Viewer, ChunkPos Pos) {
        throw new AssertionError();
    }

    @Inject(method = "updateChunkTracking", at = @At("HEAD"), cancellable = true)
    private void camerasHoldView(ServerPlayer Viewer, CallbackInfo Info) {
        ChunkTrackingView Current = Viewer.getChunkTrackingView();
        ChunkTrackingView.Positioned Lens = CameraViews.lens(Viewer);
        if (Lens == null && !(Current instanceof CameraTrackingView)) {
            return;
        }
        Info.cancel();
        if (Viewer.level() != level) {
            return;
        }
        ChunkTrackingView.Positioned Body = new ChunkTrackingView.Positioned(Viewer.chunkPosition(), getPlayerViewDistance(Viewer));
        ChunkTrackingView Wanted = Lens == null ? Body : new CameraTrackingView(Body, Lens);
        if (Wanted.equals(Current)) {
            return;
        }
        ChunkPos From = CameraTrackingView.centre(Current);
        ChunkPos To = CameraTrackingView.centre(Wanted);
        if (To != null && !To.equals(From)) {
            Viewer.connection.send(new ClientboundSetChunkCacheCenterPacket(To.x, To.z));
        }
        boolean Alive = Viewer.isAlive();
        for (ChunkPos Pos : CameraTrackingView.leaving(Current, Wanted)) {
            boolean Pending = Viewer.connection.chunkSender.isPending(Pos.toLong());
            dropChunk(Viewer, Pos);
            if (!Alive && !Pending) {
                Viewer.connection.send(new ClientboundForgetLevelChunkPacket(Pos));
            }
        }
        ChunkPos OldLens = CameraTrackingView.lensCentre(Current);
        ChunkPos NewLens = CameraTrackingView.lensCentre(Wanted);
        if (!Objects.equals(OldLens, NewLens)) {
            CameraViews.sendWindow(Viewer, NewLens);
        }
        for (ChunkPos Pos : CameraTrackingView.leaving(Wanted, Current)) {
            markChunkPendingToSend(Viewer, Pos);
        }
        Viewer.setChunkTrackingView(Wanted);
    }
}
