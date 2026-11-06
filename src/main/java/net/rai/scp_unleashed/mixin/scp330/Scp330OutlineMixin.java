package net.rai.scp_unleashed.mixin.scp330;

import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.scp330.client.Scp330Sight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class Scp330OutlineMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void scp330SightColour(CallbackInfoReturnable<Integer> Info) {
        Entity Self = (Entity) (Object) this;
        if (Self.level().isClientSide && Self.getTeam() == null && !Self.isCurrentlyGlowing() && Scp330Sight.reveals(Self)) {
            Info.setReturnValue(Scp330Sight.Colour);
        }
    }
}
