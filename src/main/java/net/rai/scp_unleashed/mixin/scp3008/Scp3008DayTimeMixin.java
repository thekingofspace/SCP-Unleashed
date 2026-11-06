package net.rai.scp_unleashed.mixin.scp3008;

import net.minecraft.world.level.storage.DerivedLevelData;
import net.rai.scp_unleashed.scp3008.Scp3008Clock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DerivedLevelData.class)
public abstract class Scp3008DayTimeMixin {
    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void scpStoreClock(CallbackInfoReturnable<Long> Info) {
        if (Scp3008Clock.owns(this)) {
            Info.setReturnValue(Scp3008Clock.time());
        }
    }
}
