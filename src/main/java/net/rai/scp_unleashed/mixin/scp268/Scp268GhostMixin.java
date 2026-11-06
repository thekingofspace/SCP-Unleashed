package net.rai.scp_unleashed.mixin.scp268;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp268.client.Scp268Ghost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class Scp268GhostMixin {
    @Unique
    private boolean Scp268Faded;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void scp268FadeType(LivingEntity Shown, boolean BodyVisible, boolean Translucent, boolean Glowing, CallbackInfoReturnable<RenderType> Info) {
        Scp268Faded = BodyVisible && !Translucent && Scp268Ghost.isFaded(Shown);
        if (Scp268Faded) {
            Info.setReturnValue(RenderType.itemEntityTranslucentCull(((EntityRenderer) (Object) this).getTextureLocation(Shown)));
        }
    }

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
            index = 4)
    private int scp268FadeColour(int Colour) {
        if (!Scp268Faded) {
            return Colour;
        }
        Scp268Faded = false;
        return Scp268Ghost.fade(Colour);
    }
}
