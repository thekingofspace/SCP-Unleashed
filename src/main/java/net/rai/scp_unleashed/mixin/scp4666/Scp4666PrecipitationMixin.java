package net.rai.scp_unleashed.mixin.scp4666;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.scp4666.Scp4666World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class Scp4666PrecipitationMixin {
    @Inject(method = "tickPrecipitation", at = @At("HEAD"), cancellable = true)
    private void scpWinterStill(BlockPos Pos, CallbackInfo Info) {
        if (Scp4666World.still((ServerLevel) (Object) this, Pos)) {
            Info.cancel();
        }
    }
}
