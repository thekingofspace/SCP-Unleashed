package net.rai.scp_unleashed.scp939.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp939.Scp939;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class Scp939HazeLayer extends GeoRenderLayer<Scp939> {
    public static final int Echoes = 2;
    public static final float Drift = 0.09f;
    public static final float EchoAlpha = 0.28f;

    public Scp939HazeLayer(GeoRenderer<Scp939> Renderer) {
        super(Renderer);
    }

    @Override
    public void render(PoseStack Pose, Scp939 Owner, BakedGeoModel Model, RenderType Type, MultiBufferSource Buffers, VertexConsumer Buffer, float PartialTick, int Light,
                       int Overlay) {
        float Haze = Scp939Renderer.hazeShown(Owner);
        if (Haze <= 0.0f) {
            return;
        }
        RenderType Ghost = RenderType.entityTranslucent(getRenderer().getTextureLocation(Owner));
        float Time = Owner.tickCount + PartialTick + Owner.getId() * 7.0f;
        for (int Index = 0; Index < Echoes; Index++) {
            float Phase = Time * (0.21f + Index * 0.07f) + Index * 2.1f;
            float Dx = Mth.sin(Phase) * Drift * Haze * (Index + 1);
            float Dz = Mth.cos(Phase * 0.8f) * Drift * Haze * (Index + 1);
            float Alpha = EchoAlpha * Haze * (0.6f + 0.4f * Mth.sin(Phase * 1.7f)) / (Index + 1);
            int Colour = FastColor.ARGB32.colorFromFloat(Mth.clamp(Alpha, 0.0f, 1.0f), 0.8f, 0.55f, 0.55f);
            Pose.pushPose();
            Pose.translate(Dx, 0.0f, Dz);
            getRenderer().reRender(Model, Pose, Buffers, Owner, Ghost, Buffers.getBuffer(Ghost), PartialTick, Light, OverlayTexture.NO_OVERLAY, Colour);
            Pose.popPose();
        }
    }
}
