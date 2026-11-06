package net.rai.scp_unleashed.z0002.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import net.rai.scp_unleashed.z0002.Z0002Registry;
import net.rai.scp_unleashed.z0002.Z0002Slash;

public class Z0002SlashRenderer extends EntityRenderer<Z0002Slash> {
    public static final ResourceLocation Texture = Z0002Registry.location("textures/entity/z0002/slash.png");
    public static final float FrontPad = 0.35f;
    public static final float BackPad = 0.4f;
    public static final float SidePad = 0.4f;
    public static final float FadeTicks = 14.0f;
    public static final float CrossDepth = 0.6f;

    public Z0002SlashRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(Z0002Slash Slash, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Vec3 Ahead = Slash.forward();
        Vec3 Across = Slash.side();
        float Span = Z0002Slash.ArcSpan * Mth.DEG_TO_RAD;
        float Front = FrontPad;
        float Back = (float) (Z0002Slash.ArcRadius * (Math.cos(Span) - 1.0)) - BackPad;
        float Half = (float) (Z0002Slash.ArcRadius * Math.sin(Span)) + SidePad;
        float Age = Slash.tickCount + PartialTick;
        int Alpha = (int) (255 * Mth.clamp(1.0f - Age / FadeTicks, 0.3f, 1.0f));
        Vec3 Normal = Ahead.cross(Across).normalize();
        if (ShaderCompat.renderingShadowPass()) {
            return;
        }
        PoseStack.Pose Saved = Pose.last().copy();
        Z0002Deferred.queue(Own -> {
            VertexConsumer Consumer = Own.getBuffer(RenderType.entityTranslucentEmissive(Texture));
            quad(Consumer, Saved, Ahead, Across, Front, Back, Half, Alpha, Normal);
            quad(Consumer, Saved, Normal.scale(-1.0), Across, Front * CrossDepth, Back * CrossDepth, Half, Alpha, Ahead);
        });
    }

    private static void quad(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Ahead, Vec3 Across, float Front, float Back, float Half, int Alpha, Vec3 Normal) {
        corner(Consumer, Last, Ahead, Across, Back, -Half, 0.0f, 1.0f, Alpha, Normal);
        corner(Consumer, Last, Ahead, Across, Back, Half, 1.0f, 1.0f, Alpha, Normal);
        corner(Consumer, Last, Ahead, Across, Front, Half, 1.0f, 0.0f, Alpha, Normal);
        corner(Consumer, Last, Ahead, Across, Front, -Half, 0.0f, 0.0f, Alpha, Normal);
    }

    private static void corner(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Ahead, Vec3 Across, float Forward, float Side, float U, float V, int Alpha, Vec3 Normal) {
        Vec3 Point = Ahead.scale(Forward).add(Across.scale(Side));
        Consumer.addVertex(Last, (float) Point.x, (float) Point.y, (float) Point.z).setColor(255, 255, 255, Alpha).setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(Last, (float) Normal.x, (float) Normal.y, (float) Normal.z);
    }

    @Override
    public ResourceLocation getTextureLocation(Z0002Slash Slash) {
        return Texture;
    }
}
