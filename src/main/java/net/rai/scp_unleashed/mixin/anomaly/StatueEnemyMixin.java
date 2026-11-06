package net.rai.scp_unleashed.mixin.anomaly;

import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.anomaly.Targeting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class StatueEnemyMixin {
    @Inject(method = "canBeSeenAsEnemy", at = @At("HEAD"), cancellable = true)
    private void statueNoEnemy(CallbackInfoReturnable<Boolean> Info) {
        if (Targeting.isStatue((LivingEntity) (Object) this)) {
            Info.setReturnValue(false);
        }
    }
}
