package net.rai.scp_unleashed.mixin.z0003;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.z0003.Z0003Frozen;
import net.rai.scp_unleashed.z0003.client.Z0003StatueRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderDispatcher.class)
public abstract class Z0003StatueRenderMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void z0003Statue(EntityRenderer<?> Renderer, Entity Shown, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light,
                             Operation<Void> Original) {
        if (Z0003StatueRendering.frozen(Shown)) {
            Z0003Frozen Live = Z0003StatueRendering.hold(Shown);
            try {
                Original.call(Renderer, Shown, Yaw, Z0003StatueRendering.FrozenPartial, Pose, Z0003StatueRendering.wrap(Buffers, Shown), Light);
            } finally {
                Z0003StatueRendering.release(Shown, Live);
            }
            return;
        }
        Original.call(Renderer, Shown, Yaw, Partial, Pose, Buffers, Light);
    }
}
