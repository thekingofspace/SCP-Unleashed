package net.rai.scp_unleashed.mixin.scp5001;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp5001.xk.client.XkSky;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class XkSkyMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void scpUnleashedRedSky(Vec3 Position, float Partial, CallbackInfoReturnable<Vec3> Info) {
        Info.setReturnValue(XkSky.sky(Info.getReturnValue(), (ClientLevel) (Object) this, Partial));
    }

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void scpUnleashedRedClouds(float Partial, CallbackInfoReturnable<Vec3> Info) {
        Info.setReturnValue(XkSky.cloud(Info.getReturnValue(), (ClientLevel) (Object) this, Partial));
    }

    @Inject(method = "getSkyDarken", at = @At("RETURN"), cancellable = true)
    private void scpUnleashedDarkSky(float Partial, CallbackInfoReturnable<Float> Info) {
        Info.setReturnValue(XkSky.darken(Info.getReturnValue(), (ClientLevel) (Object) this, Partial));
    }
}
