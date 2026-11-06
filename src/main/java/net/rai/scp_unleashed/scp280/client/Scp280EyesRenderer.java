package net.rai.scp_unleashed.scp280.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.rai.scp_unleashed.scp280.Scp280Eyes;
import net.rai.scp_unleashed.scp280.Scp280Registry;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public class Scp280EyesRenderer extends EntityRenderer<Scp280Eyes> {
    public static final ResourceLocation Texture = Scp280Registry.location("textures/entity/scp280/eyes.png");
    public static final RenderType Glow = RenderType.eyes(Texture);
    public static final int AtlasWidth = 96;
    public static final int AtlasHeight = 32;
    public static final int CellWidth = 16;
    public static final int CellHeight = 8;
    public static final float QuadWidth = 0.5f;
    public static final float QuadHeight = 0.25f;

    public Scp280EyesRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
        shadowStrength = 0.0f;
    }

    @Override
    public boolean shouldRender(Scp280Eyes Eyes, Frustum View, double CameraX, double CameraY, double CameraZ) {
        return Eyes.visible() && super.shouldRender(Eyes, View, CameraX, CameraY, CameraZ);
    }

    @Override
    protected int getBlockLightLevel(Scp280Eyes Eyes, BlockPos Pos) {
        return 15;
    }

    @Override
    public void render(Scp280Eyes Eyes, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        if (!Eyes.visible() || ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Glowing = Eyes.glow(PartialTick);
        if (Glowing <= 0.01f) {
            return;
        }
        int Frame = Eyes.frame(PartialTick);
        float U0 = Frame * CellWidth / (float) AtlasWidth;
        float U1 = (Frame + 1) * CellWidth / (float) AtlasWidth;
        float V0 = Eyes.variant() * CellHeight / (float) AtlasHeight;
        float V1 = (Eyes.variant() + 1) * CellHeight / (float) AtlasHeight;
        Pose.pushPose();
        Pose.translate(0.0f, Scp280Eyes.Height * 0.5f, 0.0f);
        Pose.mulPose(entityRenderDispatcher.cameraOrientation());
        PoseStack.Pose Last = Pose.last();
        VertexConsumer Builder = Buffers.getBuffer(Glow);
        int Shade = Math.round(Glowing * 255.0f);
        int Colour = FastColor.ARGB32.color(255, Shade, Shade, Shade);
        float HalfWidth = QuadWidth * 0.5f;
        float HalfHeight = QuadHeight * 0.5f;
        vertex(Builder, Last, Colour, HalfWidth, -HalfHeight, U1, V1);
        vertex(Builder, Last, Colour, HalfWidth, HalfHeight, U1, V0);
        vertex(Builder, Last, Colour, -HalfWidth, HalfHeight, U0, V0);
        vertex(Builder, Last, Colour, -HalfWidth, -HalfHeight, U0, V1);
        Pose.popPose();
    }

    private static void vertex(VertexConsumer Builder, PoseStack.Pose Last, int Colour, float X, float Y, float U, float V) {
        Builder.addVertex(Last, X, Y, 0.0f).setColor(Colour).setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(Last, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(Scp280Eyes Eyes) {
        return Texture;
    }
}
