package net.rai.scp_unleashed.scp427.integration;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp427.Scp427Registry;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public final class Scp427CuriosRenderer implements ICurioRenderer {
    public static final float Drop = 0.3f;
    public static final float Ahead = -0.16f;
    public static final float Size = 0.28f;

    public static void register() {
        CuriosRendererRegistry.register(Scp427Registry.Locket.get(), Scp427CuriosRenderer::new);
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack Stack, SlotContext Slot, PoseStack Pose, RenderLayerParent<T, M> Parent, MultiBufferSource Buffers,
                                                                          int Light, float LimbSwing, float LimbSwingAmount, float PartialTicks, float AgeInTicks, float NetHeadYaw, float HeadPitch) {
        if (!(Parent.getModel() instanceof HumanoidModel<?> Model)) {
            return;
        }
        LivingEntity Wearer = Slot.entity();
        Pose.pushPose();
        ICurioRenderer.translateIfSneaking(Pose, Wearer);
        ICurioRenderer.rotateIfSneaking(Pose, Wearer);
        Model.body.translateAndRotate(Pose);
        Pose.translate(0.0f, Drop, Ahead);
        Pose.mulPose(Axis.XP.rotationDegrees(180.0f));
        Pose.scale(Size, Size, Size);
        Minecraft.getInstance().getItemRenderer().renderStatic(Stack, ItemDisplayContext.FIXED, Light, OverlayTexture.NO_OVERLAY, Pose, Buffers, Wearer.level(), 0);
        Pose.popPose();
    }
}
