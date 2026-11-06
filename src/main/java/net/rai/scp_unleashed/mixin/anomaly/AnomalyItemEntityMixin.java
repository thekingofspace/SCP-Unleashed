package net.rai.scp_unleashed.mixin.anomaly;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.rai.scp_unleashed.anomaly.AnomalyDrops;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class AnomalyItemEntityMixin {
    @Inject(method = "fireImmune", at = @At("HEAD"), cancellable = true)
    private void anomalyFireproof(CallbackInfoReturnable<Boolean> Info) {
        if (AnomalyDrops.isAnomalous(((ItemEntity) (Object) this).getItem())) {
            Info.setReturnValue(true);
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void anomalyUnharmed(DamageSource Source, float Amount, CallbackInfoReturnable<Boolean> Info) {
        if (AnomalyDrops.shrugsOff(((ItemEntity) (Object) this).getItem(), Source)) {
            Info.setReturnValue(false);
        }
    }
}
