package net.rai.scp_unleashed.mixin.scp330;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.scp330.client.Scp330Sight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class Scp330GlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void scp330Sight(Entity Target, CallbackInfoReturnable<Boolean> Info) {
        if (Scp330Sight.reveals(Target)) {
            Info.setReturnValue(true);
        }
    }
}
