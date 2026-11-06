package net.rai.scp_unleashed.mixin.scp268;

import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;
import net.rai.scp_unleashed.scp268.Scp268;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class Scp268TrackingMixin {
    @Inject(method = "broadcastToPlayer", at = @At("HEAD"), cancellable = true)
    private void scp268HideWearer(ServerPlayer Viewer, CallbackInfoReturnable<Boolean> Info) {
        Object Self = this;
        if (Viewer != Self && Scp268.isNeglected((ServerPlayer) Self) && !AntiMemetic.isProtected(Viewer)) {
            Info.setReturnValue(false);
        }
    }
}
