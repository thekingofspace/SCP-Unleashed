package net.rai.scp_unleashed.scp001scarlet.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

public final class ScarletBeam {
    public static final float Width = 1.6f;
    public static final float Top = 260.0f;
    public static final float NearShare = 0.6f;

    private ScarletBeam() {
    }

    public static void render(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !ScarletClient.beamVisible()) {
            return;
        }
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World == null || World.dimension() != Level.OVERWORLD) {
            return;
        }
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 Eye = Event.getCamera().getPosition();
        BlockPos Throne = ScarletClient.throne();
        double Dx = Throne.getX() + 0.5 - Eye.x;
        double Dz = Throne.getZ() + 0.5 - Eye.z;
        double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
        double Reach = Client.gameRenderer.getRenderDistance() * NearShare;
        double Pull = Flat > Reach ? Reach / Flat : 1.0;
        float X = (float) (Dx * Pull);
        float Z = (float) (Dz * Pull);
        float Bottom = (float) ((Throne.getY() + 2.0 - Eye.y) * Pull);
        float Height = (float) (Top * Math.max(Pull, 0.25));
        float Half = (float) (Width * Math.max(Pull, 0.08) * (Flat > Reach ? 1.6 : 1.0));
        float Throb = 0.8f + 0.2f * Mth.sin((ScarletClient.clock() + Partial) * 0.05f);
        int Alpha = (int) (110 * Throb);
        Matrix4f Pose = new Matrix4f(Event.getModelViewMatrix());
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder Builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int Side = 0; Side < 2; Side++) {
            float Ax = Side == 0 ? Half : 0.0f;
            float Az = Side == 0 ? 0.0f : Half;
            Builder.addVertex(Pose, X - Ax, Bottom, Z - Az).setColor(220, 30, 30, Alpha);
            Builder.addVertex(Pose, X + Ax, Bottom, Z + Az).setColor(220, 30, 30, Alpha);
            Builder.addVertex(Pose, X + Ax, Bottom + Height, Z + Az).setColor(160, 10, 14, 0);
            Builder.addVertex(Pose, X - Ax, Bottom + Height, Z - Az).setColor(160, 10, 14, 0);
            Builder.addVertex(Pose, X - Ax * 0.35f, Bottom, Z - Az * 0.35f).setColor(255, 120, 100, Alpha);
            Builder.addVertex(Pose, X + Ax * 0.35f, Bottom, Z + Az * 0.35f).setColor(255, 120, 100, Alpha);
            Builder.addVertex(Pose, X + Ax * 0.35f, Bottom + Height * 0.7f, Z + Az * 0.35f).setColor(220, 40, 40, 0);
            Builder.addVertex(Pose, X - Ax * 0.35f, Bottom + Height * 0.7f, Z - Az * 0.35f).setColor(220, 40, 40, 0);
        }
        BufferUploader.drawWithShader(Builder.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
