package net.rai.scp_unleashed.mixin.cameras;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.rai.scp_unleashed.cameras.client.CameraClientWindow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientChunkCache$Storage")
public abstract class CameraChunkStorageMixin {
    @Shadow
    @Final
    int chunkRadius;

    @Shadow
    @Final
    private int viewRange;

    @Shadow
    int viewCenterX;

    @Shadow
    int viewCenterZ;

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/atomic/AtomicReferenceArray;<init>(I)V"))
    private int camerasRoomForLens(int Size) {
        return Size * 2;
    }

    @ModifyReturnValue(method = "inRange", at = @At("RETURN"))
    private boolean camerasLensInRange(boolean Original, @Local(ordinal = 0, argsOnly = true) int X, @Local(ordinal = 1, argsOnly = true) int Z) {
        return Original || CameraClientWindow.covers(X, Z, chunkRadius);
    }

    @ModifyReturnValue(method = "getIndex", at = @At("RETURN"))
    private int camerasLensIndex(int Original, @Local(ordinal = 0, argsOnly = true) int X, @Local(ordinal = 1, argsOnly = true) int Z) {
        if (!CameraClientWindow.isOpen() || Math.abs(X - viewCenterX) <= chunkRadius && Math.abs(Z - viewCenterZ) <= chunkRadius || !CameraClientWindow.covers(X, Z, chunkRadius)) {
            return Original;
        }
        return Original + viewRange * viewRange;
    }
}
