package net.rai.scp_unleashed.scp428.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp428.Scp428Core;
import net.rai.scp_unleashed.scp428.Scp428Size;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp428CoreRenderer extends GeoEntityRenderer<Scp428Core> {
    public static final float ModelWidth = 23.0f / 16.0f;
    public static final float ModelHeight = 18.625f / 16.0f;
    public static final float ShadowShare = 0.5f;

    public Scp428CoreRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp428CoreGeoModel());
        shadowRadius = Scp428Size.SingleDiameter * ShadowShare;
    }

    @Override
    public void render(Scp428Core Core, float Yaw, float PartialTick, PoseStack Transforms, MultiBufferSource Buffers, int Light) {
        shadowRadius = Scp428Size.width(Core.girth(PartialTick), Core.squeeze(PartialTick)) * ShadowShare;
        super.render(Core, Yaw, PartialTick, Transforms, Buffers, Light);
        Scp428Tendrils.render(Core, PartialTick, Transforms, Buffers, Light);
    }

    @Override
    public void scaleModelForRender(float WidthScale, float HeightScale, PoseStack Transforms, Scp428Core Core, BakedGeoModel Model, boolean IsReRender, float PartialTick,
                                    int PackedLight, int PackedOverlay) {
        float Girth = Core.girth(PartialTick);
        float Squeeze = Core.squeeze(PartialTick);
        float Wide = Scp428Size.width(Girth, Squeeze) / ModelWidth;
        float Tall = Scp428Size.tall(Girth, Squeeze) / ModelHeight;
        super.scaleModelForRender(WidthScale * Wide, HeightScale * Tall, Transforms, Core, Model, IsReRender, PartialTick, PackedLight, PackedOverlay);
    }

    @Nullable
    @Override
    public RenderType getRenderType(Scp428Core Core, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        return RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    public int getPackedOverlay(Scp428Core Core, float U, float PartialTick) {
        if (Core.isDeadOrDying()) {
            return OverlayTexture.NO_OVERLAY;
        }
        return super.getPackedOverlay(Core, U, PartialTick);
    }

    @Override
    protected float getDeathMaxRotation(Scp428Core Core) {
        return 0.0f;
    }
}
