package net.rai.scp_unleashed.scp3008.gear.client;

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
import net.rai.scp_unleashed.scp3008.gear.Scp3008GearRegistry;
import net.rai.scp_unleashed.scp3008.gear.entity.Scp3008ThrownSpear;

public class Scp3008ThrownSpearRenderer extends EntityRenderer<Scp3008ThrownSpear> {
    public static final float Drop = -0.35f;
    private final ItemRenderer Items;

    public Scp3008ThrownSpearRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        Items = Context.getItemRenderer();
        shadowRadius = 0.0f;
    }

    @Override
    public void render(Scp3008ThrownSpear Spear, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        ItemStack Stack = Spear.displayStack();
        if (Stack.isEmpty()) {
            Stack = new ItemStack(Scp3008GearRegistry.Spear.get());
        }
        Pose.pushPose();
        Pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(Partial, Spear.yRotO, Spear.getYRot()) - 90.0f));
        Pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(Partial, Spear.xRotO, Spear.getXRot()) - 90.0f));
        Pose.translate(0.0f, Drop, 0.0f);
        Items.renderStatic(Stack, ItemDisplayContext.NONE, Light, OverlayTexture.NO_OVERLAY, Pose, Buffers, Spear.level(), Spear.getId());
        Pose.popPose();
        super.render(Spear, Yaw, Partial, Pose, Buffers, Light);
    }

    @Override
    public ResourceLocation getTextureLocation(Scp3008ThrownSpear Spear) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
