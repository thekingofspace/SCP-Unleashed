package net.rai.scp_unleashed.mixin.upgrade;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.rai.scp_unleashed.upgrade.client.MufflerClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMufflerMixin {
    @ModifyExpressionValue(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"))
    private float mufflerStart(float Volume, @Local(argsOnly = true) SoundInstance Sound) {
        return Volume * MufflerClient.factor(Sound);
    }

    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
    private void mufflerTick(SoundInstance Sound, CallbackInfoReturnable<Float> Info) {
        float Factor = MufflerClient.factor(Sound);
        if (Factor < 1.0f) {
            Info.setReturnValue(Info.getReturnValueF() * Factor);
        }
    }
}
