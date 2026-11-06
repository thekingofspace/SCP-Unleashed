package net.rai.scp_unleashed.mixin.scp268;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.rai.scp_unleashed.scp268.Scp268;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Warden.class)
public abstract class Scp268WardenMixin {
    @Inject(method = "canTargetEntity", at = @At("HEAD"), cancellable = true)
    private void scp268Unheard(Entity Candidate, CallbackInfoReturnable<Boolean> Info) {
        if (Scp268.isNeglected(Candidate)) {
            Info.setReturnValue(false);
        }
    }
}
