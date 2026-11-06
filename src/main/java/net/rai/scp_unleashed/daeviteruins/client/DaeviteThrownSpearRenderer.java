package net.rai.scp_unleashed.daeviteruins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.daeviteruins.DaeviteRuinsRegistry;
import net.rai.scp_unleashed.daeviteruins.entity.DaeviteThrownSpear;

public class DaeviteThrownSpearRenderer extends EntityRenderer<DaeviteThrownSpear> {
    private final ItemRenderer Items;

    public DaeviteThrownSpearRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        Items = Context.getItemRenderer();
        shadowRadius = 0.0f;
    }

    @Override
    public void render(DaeviteThrownSpear Spear, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        ItemStack Stack = Spear.displayStack();
        if (Stack.isEmpty()) {
            Stack = new ItemStack(DaeviteRuinsRegistry.Spear.get());
        }
        Pose.pushPose();
        Pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(Partial, Spear.yRotO, Spear.getYRot()) - 90.0f));
        Pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(Partial, Spear.xRotO, Spear.getXRot()) - 90.0f));
        Pose.translate(0.0f, -0.35f, 0.0f);
        Items.renderStatic(Stack, ItemDisplayContext.NONE, Light, OverlayTexture.NO_OVERLAY, Pose, Buffers, Spear.level(), Spear.getId());
        Pose.popPose();
        super.render(Spear, Yaw, Partial, Pose, Buffers, Light);
    }

    @Override
    public ResourceLocation getTextureLocation(DaeviteThrownSpear Spear) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
