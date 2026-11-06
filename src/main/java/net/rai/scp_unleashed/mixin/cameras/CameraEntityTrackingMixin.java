package net.rai.scp_unleashed.mixin.cameras;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.cameras.CameraViews;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class CameraEntityTrackingMixin {
    @Shadow
    @Final
    Entity entity;

    @ModifyExpressionValue(method = "updatePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 camerasTrackFromLens(Vec3 Original, @Local(argsOnly = true) ServerPlayer Viewer) {
        return CameraViews.trackingOrigin(Viewer, Original, entity.position());
    }
}
