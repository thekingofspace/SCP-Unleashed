package net.rai.scp_unleashed.scp093.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.rai.scp_unleashed.scp093.Scp093Registry;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.joml.Matrix4f;

public final class Scp093SunGlare {
    public static final ResourceLocation Texture = Scp093Registry.location("textures/environment/scp093_glare.png");
    public static final float Size = 72.0f;
    public static final float Distance = 99.0f;
    public static final float Red = 1.0f;
    public static final float Green = 0.72f;
    public static final float Blue = 0.38f;
    public static final float Alpha = 0.55f;

    private Scp093SunGlare() {
    }

    public static void render(RenderLevelStageEvent Event) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || !Scp093ClientState.inWastes(Client) || ShaderCompat.shaderPackInUse()) {
            return;
        }
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack Pose = new PoseStack();
        Pose.mulPose(Event.getModelViewMatrix());
        Pose.mulPose(Axis.YP.rotationDegrees(-90.0f));
        Pose.mulPose(Axis.XP.rotationDegrees(Client.level.getTimeOfDay(Partial) * 360.0f));
        Matrix4f Matrix = Pose.last().pose();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, Texture);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        BufferBuilder Builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Builder.addVertex(Matrix, -Size, Distance, -Size).setUv(0.0f, 0.0f).setColor(Red, Green, Blue, Alpha);
        Builder.addVertex(Matrix, Size, Distance, -Size).setUv(1.0f, 0.0f).setColor(Red, Green, Blue, Alpha);
        Builder.addVertex(Matrix, Size, Distance, Size).setUv(1.0f, 1.0f).setColor(Red, Green, Blue, Alpha);
        Builder.addVertex(Matrix, -Size, Distance, Size).setUv(0.0f, 1.0f).setColor(Red, Green, Blue, Alpha);
        BufferUploader.drawWithShader(Builder.buildOrThrow());
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
