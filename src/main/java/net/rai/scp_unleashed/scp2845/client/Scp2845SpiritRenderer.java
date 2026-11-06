package net.rai.scp_unleashed.scp2845.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp2845.Scp2845Spirit;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import software.bernie.geckolib.util.Color;

public class Scp2845SpiritRenderer extends SharedGeoRenderer<Scp2845Spirit> {
    private float Partial;

    public Scp2845SpiritRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp2845SpiritModel());
        withScale(Scp2845Renderer.Scale * Scp2845Spirit.RenderScale);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(Scp2845Spirit Owner, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        if (ShaderCompat.renderingShadowPass()) {
            return;
        }
        Partial = PartialTick;
        Owner.setRenderPartial(PartialTick);
        super.render(Owner, Yaw, PartialTick, Pose, Buffers, LightTexture.FULL_BRIGHT);
    }

    @Override
    protected void applyRotations(Scp2845Spirit Owner, PoseStack Pose, float Age, float Yaw, float PartialTick, float NativeScale) {
        super.applyRotations(Owner, Pose, Age, Owner.headingYaw(), PartialTick, NativeScale);
    }

    @Override
    public RenderType getRenderType(Scp2845Spirit Owner, ResourceLocation Texture, MultiBufferSource Buffers, float PartialTick) {
        return RenderType.entityTranslucentEmissive(Texture);
    }

    @Override
    public Color getRenderColor(Scp2845Spirit Owner, float PartialTick, int Light) {
        return Color.ofRGBA(1.0f, 1.0f, 1.0f, Owner.alpha(Partial));
    }
}
