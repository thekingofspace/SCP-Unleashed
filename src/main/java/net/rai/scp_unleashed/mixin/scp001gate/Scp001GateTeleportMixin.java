package net.rai.scp_unleashed.mixin.scp001gate;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001gate.Scp001GateAvoid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class Scp001GateTeleportMixin {
    @Inject(method = "teleportTo(DDD)V", at = @At("HEAD"), cancellable = true)
    private void scp001gateKeepAvoidersOut(double X, double Y, double Z, CallbackInfo Callback) {
        Entity Self = (Entity) (Object) this;
        if (!Self.level().isClientSide() && Scp001GateAvoid.avoids(Self) && Scp001GateAvoid.blocked(Self.level(), new Vec3(X, Y, Z))) {
            Callback.cancel();
        }
    }
}
