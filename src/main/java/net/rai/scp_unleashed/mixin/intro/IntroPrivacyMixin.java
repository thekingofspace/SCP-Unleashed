package net.rai.scp_unleashed.mixin.intro;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.intro.IntroPrivacy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class IntroPrivacyMixin {
    @Inject(method = "broadcastToPlayer", at = @At("HEAD"), cancellable = true)
    private void introPrivate(ServerPlayer Viewer, CallbackInfoReturnable<Boolean> Info) {
        if (IntroPrivacy.hidden((Entity) (Object) this, Viewer)) {
            Info.setReturnValue(false);
        }
    }
}
