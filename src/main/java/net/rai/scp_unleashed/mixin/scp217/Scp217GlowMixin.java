package net.rai.scp_unleashed.mixin.scp217;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.scp217.client.Scp217PulseClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class Scp217GlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void scp217Pulse(Entity Target, CallbackInfoReturnable<Boolean> Info) {
        if (Scp217PulseClient.reveals(Target)) {
            Info.setReturnValue(true);
        }
    }
}
