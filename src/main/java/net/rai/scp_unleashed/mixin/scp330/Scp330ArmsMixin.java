package net.rai.scp_unleashed.mixin.scp330;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp330.client.Scp330Stumps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class Scp330ArmsMixin {
    @Unique
    private boolean Scp330Shortened;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void scp330Stumps(LivingEntity Shown, float LimbSwing, float LimbSwingAmount, float AgeInTicks, float NetHeadYaw, float HeadPitch, CallbackInfo Info) {
        PlayerModel<?> Self = (PlayerModel<?>) (Object) this;
        if (Scp330Stumps.isGreedy(Shown)) {
            Scp330Stumps.shorten(Self.rightArm, Self.rightSleeve);
            Scp330Stumps.shorten(Self.leftArm, Self.leftSleeve);
            Scp330Shortened = true;
        } else if (Scp330Shortened) {
            Scp330Stumps.restore(Self.rightArm, Self.rightSleeve);
            Scp330Stumps.restore(Self.leftArm, Self.leftSleeve);
            Scp330Shortened = false;
        }
    }
}
