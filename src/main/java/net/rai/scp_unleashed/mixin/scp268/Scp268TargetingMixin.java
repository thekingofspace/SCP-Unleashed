package net.rai.scp_unleashed.mixin.scp268;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.rai.scp_unleashed.scp268.Scp268;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetingConditions.class)
public abstract class Scp268TargetingMixin {
    @Shadow
    @Final
    private boolean isCombat;

    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void scp268Overlook(LivingEntity Looker, LivingEntity Target, CallbackInfoReturnable<Boolean> Info) {
        if (Scp268.isNeglected(Target) && (isCombat || Scp268.overlooks(Looker))) {
            Info.setReturnValue(false);
        }
    }
}
