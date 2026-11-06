package net.rai.scp_unleashed.mixin.scp001scarlet;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001scarlet.client.ScarletSky;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ScarletSkyMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void scpUnleashedScarletSky(Vec3 Position, float Partial, CallbackInfoReturnable<Vec3> Info) {
        Info.setReturnValue(ScarletSky.sky(Info.getReturnValue(), (ClientLevel) (Object) this, Partial));
    }

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void scpUnleashedScarletClouds(float Partial, CallbackInfoReturnable<Vec3> Info) {
        Info.setReturnValue(ScarletSky.cloud(Info.getReturnValue(), (ClientLevel) (Object) this, Partial));
    }

    @Inject(method = "getSkyDarken", at = @At("RETURN"), cancellable = true)
    private void scpUnleashedScarletDarken(float Partial, CallbackInfoReturnable<Float> Info) {
        Info.setReturnValue(ScarletSky.darken(Info.getReturnValue(), (ClientLevel) (Object) this, Partial));
    }
}
