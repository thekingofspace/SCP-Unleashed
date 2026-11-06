package net.rai.scp_unleashed.seasonal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.rai.scp_unleashed.scp173.Scp173Statue;
import net.rai.scp_unleashed.seasonal.SeasonalLooks;
import net.rai.scp_unleashed.seasonal.Seasons;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import net.rai.scp_unleashed.z0003.client.Z0003StatueRendering;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

public class Scp173PumpkinLayer extends GeoRenderLayer<Scp173Statue> {
    public static final RenderType Shell = RenderType.entityCutoutNoCull(SeasonalLooks.PumpkinTexture);
    public static final RenderType Ember = RenderType.eyes(SeasonalLooks.PumpkinGlow);
    public static final int White = 0xFFFFFFFF;

    public Scp173PumpkinLayer(GeoRenderer<Scp173Statue> Renderer) {
        super(Renderer);
    }

    @Override
    public void renderForBone(PoseStack Pose, Scp173Statue Statue, GeoBone Bone, RenderType Type, MultiBufferSource Buffers, VertexConsumer Buffer, float Partial,
                              int Light, int Overlay) {
        if (!SeasonalLooks.HeadBone.equals(Bone.getName()) || !SeasonalLooks.scp173Pumpkin(Seasons.clientMask(), Statue.isInvisible())) {
            return;
        }
        BakedGeoModel Pumpkin = GeckoLibCache.getBakedModels().get(SeasonalLooks.PumpkinModel);
        if (Pumpkin == null) {
            return;
        }
        draw(Pose, Pumpkin, Buffers.getBuffer(Shell), Light, Overlay, White);
        float Glow = SeasonalLooks.emberGlow(LightTexture.block(Light), LightTexture.sky(Light), Statue.level().getSkyDarken());
        if (!SeasonalLooks.emberShown(Glow, ShaderCompat.renderingShadowPass(), Z0003StatueRendering.frozen(Statue))) {
            return;
        }
        int Shade = Math.round(Glow * 255.0f);
        draw(Pose, Pumpkin, Buffers.getBuffer(Ember), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFF000000 | Shade << 16 | Shade << 8 | Shade);
    }

    private void draw(PoseStack Pose, BakedGeoModel Pumpkin, VertexConsumer Target, int Light, int Overlay, int Colour) {
        for (GeoBone Part : Pumpkin.topLevelBones()) {
            drawBone(Pose, Part, Target, Light, Overlay, Colour);
        }
    }

    private void drawBone(PoseStack Pose, GeoBone Part, VertexConsumer Target, int Light, int Overlay, int Colour) {
        Pose.pushPose();
        RenderUtil.prepMatrixForBone(Pose, Part);
        getRenderer().renderCubesOfBone(Pose, Part, Target, Light, Overlay, Colour);
        for (GeoBone Child : Part.getChildBones()) {
            drawBone(Pose, Child, Target, Light, Overlay, Colour);
        }
        Pose.popPose();
    }
}
