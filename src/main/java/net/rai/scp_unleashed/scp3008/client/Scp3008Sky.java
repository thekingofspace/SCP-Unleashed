package net.rai.scp_unleashed.scp3008.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Plan;
import org.joml.Matrix4f;

public final class Scp3008Sky {
    public static final ResourceLocation CeilingTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/block/scp3008/ceiling.png");
    public static final float Cell = 8.0f;
    public static final int Reach = 40;
    public static final float DayAlpha = 0.26f;
    public static final float NightAlpha = 0.05f;
    public static final float FadeHeight = 520.0f;

    private Scp3008Sky() {
    }

    public static void ceiling(Camera View, Matrix4f ModelView, float Partial) {
        Vec3 Eye = View.getPosition();
        float Height = (float) (Scp3008Plan.CeilingY - Eye.y);
        if (Height <= 0.05f) {
            return;
        }
        float Lit = Scp3008ClientState.brightness(Partial);
        float Strength = Mth.lerp(Lit, NightAlpha, DayAlpha) * Mth.clamp(1.0f - Height / FadeHeight, 0.15f, 1.0f);
        float[] Fog = RenderSystem.getShaderFogColor();
        float Red = Fog[0] * 0.8f;
        float Green = Fog[1] * 0.8f;
        float Blue = Fog[2] * 0.82f;
        Matrix4f Pose = new Matrix4f(ModelView);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, CeilingTexture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        BufferBuilder Buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float OffsetX = (float) (Math.floor(Eye.x / Cell) * Cell - Eye.x);
        float OffsetZ = (float) (Math.floor(Eye.z / Cell) * Cell - Eye.z);
        float Span = Reach * Cell;
        for (int Column = -Reach; Column < Reach; Column++) {
            for (int Row = -Reach; Row < Reach; Row++) {
                float X0 = OffsetX + Column * Cell;
                float Z0 = OffsetZ + Row * Cell;
                float Middle = Mth.sqrt((X0 + Cell / 2) * (X0 + Cell / 2) + (Z0 + Cell / 2) * (Z0 + Cell / 2));
                float Alpha = Strength * Mth.clamp(1.0f - Middle / Span, 0.0f, 1.0f);
                if (Alpha <= 0.003f) {
                    continue;
                }
                Buffer.addVertex(Pose, X0, Height, Z0).setUv(0.0f, 0.0f).setColor(Red, Green, Blue, Alpha);
                Buffer.addVertex(Pose, X0 + Cell, Height, Z0).setUv(1.0f, 0.0f).setColor(Red, Green, Blue, Alpha);
                Buffer.addVertex(Pose, X0 + Cell, Height, Z0 + Cell).setUv(1.0f, 1.0f).setColor(Red, Green, Blue, Alpha);
                Buffer.addVertex(Pose, X0, Height, Z0 + Cell).setUv(0.0f, 1.0f).setColor(Red, Green, Blue, Alpha);
            }
        }
        MeshData Mesh = Buffer.build();
        if (Mesh != null) {
            BufferUploader.drawWithShader(Mesh);
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }
}
