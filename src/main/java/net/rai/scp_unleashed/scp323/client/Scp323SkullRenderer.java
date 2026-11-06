package net.rai.scp_unleashed.scp323.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp323.Scp323Skull;
import net.rai.scp_unleashed.scp323.Scp323SkullItem;

public class Scp323SkullRenderer extends EntityRenderer<Scp323Skull> {
    public static final float RestScale = 0.62f;
    public static final float ModelBottom = 2.0f / 16.0f;
    public static final float Clearance = 0.002f;
    public static final float Tilt = 8.0f;

    private final ItemRenderer Items;
    private final ItemStack Shown;

    public Scp323SkullRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        this.Items = Context.getItemRenderer();
        this.Shown = Scp323SkullItem.fresh();
        this.shadowRadius = 0.3f;
        this.shadowStrength = 0.6f;
    }

    @Override
    public ResourceLocation getTextureLocation(Scp323Skull Skull) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public void render(Scp323Skull Skull, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Pose.pushPose();
        Pose.translate(0.0f, (0.5f - ModelBottom) * RestScale + Clearance, 0.0f);
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Skull.getYRot()));
        Pose.mulPose(Axis.XP.rotationDegrees(Tilt));
        Pose.scale(RestScale, RestScale, RestScale);
        Items.renderStatic(Shown, ItemDisplayContext.NONE, Light, OverlayTexture.NO_OVERLAY, Pose, Buffers, Skull.level(), Skull.getId());
        Pose.popPose();
        super.render(Skull, Yaw, PartialTick, Pose, Buffers, Light);
    }
}
