package net.rai.scp_unleashed.mixin.scp268;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.rai.scp_unleashed.scp268.Scp268;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ShulkerBullet.class)
public abstract class Scp268ShulkerBulletMixin {
    @Shadow
    private Entity finalTarget;
    @Shadow
    private UUID targetId;

    @Inject(method = "tick", at = @At("HEAD"))
    private void scp268LoseTrack(CallbackInfo Info) {
        if (finalTarget != null && Scp268.isNeglected(finalTarget)) {
            finalTarget = null;
            targetId = null;
        }
    }
}
