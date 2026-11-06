package net.rai.scp_unleashed.mixin.scp217;

import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.scp217.client.Scp217PulseClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class Scp217OutlineMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void scp217PulseColour(CallbackInfoReturnable<Integer> Info) {
        Entity Self = (Entity) (Object) this;
        if (Self.level().isClientSide && Self.getTeam() == null && !Self.isCurrentlyGlowing() && Scp217PulseClient.reveals(Self)) {
            Info.setReturnValue(Scp217PulseClient.Colour);
        }
    }
}
