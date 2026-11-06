package net.rai.scp_unleashed.mixin.anomaly;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.rai.scp_unleashed.anomaly.Targeting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetingConditions.class)
public abstract class StatueTargetingMixin {
    @Shadow
    @Final
    private boolean isCombat;

    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void statueIgnored(LivingEntity Looker, LivingEntity Target, CallbackInfoReturnable<Boolean> Info) {
        if (isCombat && Targeting.isStatue(Target)) {
            Info.setReturnValue(false);
        }
    }
}
