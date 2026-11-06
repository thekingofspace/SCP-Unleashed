package net.rai.scp_unleashed.scp035.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
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
import net.rai.scp_unleashed.scp035.Scp035Mask;
import net.rai.scp_unleashed.scp035.Scp035MaskItem;

public class Scp035MaskRenderer extends EntityRenderer<Scp035Mask> {
    public static final float BackDepth = 1.0f / 16.0f;
    public static final float Clearance = 0.002f;
    public static final float ShimmerPeriod = 90.0f;
    public static final int ShimmerLevels = 3;

    private final ItemRenderer Items;
    private final ItemStack Shown;

    public Scp035MaskRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        this.Items = Context.getItemRenderer();
        this.Shown = Scp035MaskItem.fresh();
        this.shadowRadius = 0.18f;
        this.shadowStrength = 0.6f;
    }

    @Override
    public ResourceLocation getTextureLocation(Scp035Mask Mask) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public void render(Scp035Mask Mask, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Pose.pushPose();
        Pose.translate(0.0f, BackDepth * Scp035Mask.RestScale + Clearance, 0.0f);
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Mask.getYRot()));
        Pose.mulPose(Axis.XP.rotationDegrees(-90.0f));
        Pose.scale(Scp035Mask.RestScale, Scp035Mask.RestScale, Scp035Mask.RestScale);
        Items.renderStatic(Shown, ItemDisplayContext.NONE, shimmer(Mask, PartialTick, Light), OverlayTexture.NO_OVERLAY, Pose, Buffers, Mask.level(), Mask.getId());
        Pose.popPose();
        super.render(Mask, Yaw, PartialTick, Pose, Buffers, Light);
    }

    private static int shimmer(Scp035Mask Mask, float PartialTick, int Light) {
        float Time = (Mask.tickCount + PartialTick + Mask.getId() * 17.0f) / ShimmerPeriod;
        float Wave = Mth.sin(Time * Mth.TWO_PI);
        int Lift = Math.round(Math.max(0.0f, Wave) * ShimmerLevels);
        int Block = Math.min(15, LightTexture.block(Light) + Lift);
        return LightTexture.pack(Block, LightTexture.sky(Light));
    }
}
