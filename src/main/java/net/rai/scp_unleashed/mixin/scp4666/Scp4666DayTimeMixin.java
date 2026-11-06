package net.rai.scp_unleashed.mixin.scp4666;

import net.minecraft.world.level.storage.DerivedLevelData;
import net.rai.scp_unleashed.scp4666.Scp4666Clock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DerivedLevelData.class)
public abstract class Scp4666DayTimeMixin {
    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void scpWinterClock(CallbackInfoReturnable<Long> Info) {
        if (Scp4666Clock.owns(this)) {
            Info.setReturnValue(Scp4666Clock.serverTime());
        }
    }
}
