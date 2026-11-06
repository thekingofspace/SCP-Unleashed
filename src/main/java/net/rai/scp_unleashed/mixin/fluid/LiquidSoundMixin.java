package net.rai.scp_unleashed.mixin.fluid;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.liquidsound.LiquidSoundSet;
import net.rai.scp_unleashed.liquidsound.LiquidSounds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class LiquidSoundMixin {
    @Shadow
    protected boolean firstTick;

    @Unique
    private LiquidSoundSet LiquidSplashed;

    @ModifyExpressionValue(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isInWater()Z"))
    private boolean swimsInLiquid(boolean InWater) {
        return InWater || LiquidSounds.setFor((Entity) (Object) this) != null;
    }

    @WrapOperation(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;waterSwimSound()V"))
    private void liquidSwimSound(Entity Self, Operation<Void> Original) {
        LiquidSoundSet Sounds = Self.isInWater() ? null : LiquidSounds.setFor(Self);
        if (Sounds == null) {
            Original.call(Self);
        } else {
            LiquidSounds.swim(Self, Sounds);
        }
    }

    @Inject(method = "updateInWaterStateAndDoFluidPushing", at = @At("RETURN"))
    private void liquidSplash(CallbackInfoReturnable<Boolean> Info) {
        Entity Self = (Entity) (Object) this;
        LiquidSoundSet Sounds = LiquidSounds.setFor(Self);
        if (Sounds != null && Sounds != LiquidSplashed && !firstTick && !Self.isInWater()) {
            LiquidSounds.splash(Self, Sounds);
        }
        LiquidSplashed = Sounds;
    }
}
