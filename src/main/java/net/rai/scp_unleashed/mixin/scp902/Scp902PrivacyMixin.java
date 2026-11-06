package net.rai.scp_unleashed.mixin.scp902;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.scp902.Scp902Privacy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class Scp902PrivacyMixin {
    @Inject(method = "broadcastToPlayer", at = @At("HEAD"), cancellable = true)
    private void scp902KeepTripsApart(ServerPlayer Viewer, CallbackInfoReturnable<Boolean> Info) {
        if (Scp902Privacy.hidden((Entity) (Object) this, Viewer)) {
            Info.setReturnValue(false);
        }
    }
}
