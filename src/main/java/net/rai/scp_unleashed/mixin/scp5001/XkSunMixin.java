package net.rai.scp_unleashed.mixin.scp5001;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp5001.xk.client.XkSky;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class XkSunMixin {
    @ModifyArg(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V", ordinal = 0),
            index = 1, require = 0)
    private ResourceLocation scpUnleashedRedSun(ResourceLocation Original) {
        return XkSky.sunTexture(Original);
    }

    @Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V", ordinal = 0),
            require = 0)
    private void scpUnleashedTintSun(CallbackInfo Info) {
        XkSky.tintSun();
    }

    @Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V", ordinal = 1),
            require = 0)
    private void scpUnleashedTintMoon(CallbackInfo Info) {
        XkSky.tintMoon();
    }
}
