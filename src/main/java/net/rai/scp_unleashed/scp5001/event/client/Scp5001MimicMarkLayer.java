package net.rai.scp_unleashed.scp5001.event.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Mimic;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public class Scp5001MimicMarkLayer extends RenderLayer<Scp5001Mimic, PlayerModel<Scp5001Mimic>> {
    public static final ResourceLocation Marks = Scp5001EventRegistry.location("textures/entity/scp5001_event/mimic_marks.png");
    public static final ResourceLocation Eyes = Scp5001EventRegistry.location("textures/entity/scp5001_event/mimic_eyes.png");

    public Scp5001MimicMarkLayer(RenderLayerParent<Scp5001Mimic, PlayerModel<Scp5001Mimic>> Parent) {
        super(Parent);
    }

    @Override
    public void render(PoseStack Pose, MultiBufferSource Buffers, int Light, Scp5001Mimic Mimic, float Swing, float Amount, float Partial, float Age, float HeadYaw,
                       float HeadPitch) {
        if (Mimic.isInvisible()) {
            return;
        }
        getParentModel().renderToBuffer(Pose, Buffers.getBuffer(RenderType.entityTranslucent(Marks)), Light, LivingEntityRenderer.getOverlayCoords(Mimic, 0.0f), 0xE6FFFFFF);
        if (ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Blink = Mth.sin((Age + Mimic.getId() * 7) * 0.09f) > 0.93f ? 0.2f : 1.0f;
        int Channel = Math.round(255 * Blink);
        int Colour = 0xFF000000 | Channel << 16 | Channel << 8 | Channel;
        getParentModel().renderToBuffer(Pose, Buffers.getBuffer(RenderType.eyes(Eyes)), LightTexture.FULL_BRIGHT, LivingEntityRenderer.getOverlayCoords(Mimic, 0.0f), Colour);
    }
}
