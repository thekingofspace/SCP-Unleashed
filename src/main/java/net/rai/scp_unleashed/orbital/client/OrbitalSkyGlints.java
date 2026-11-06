package net.rai.scp_unleashed.orbital.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.rai.scp_unleashed.orbital.OrbitalPart;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class OrbitalSkyGlints {
    public static final float Distance = 60.0F;
    public static final float Size = 0.32F;
    public static final float MinimumBrightness = 0.05F;
    public static final float[][] Offsets = {{0.0F, 0.0F}, {0.035F, 0.012F}, {-0.028F, 0.03F}, {0.018F, -0.033F}, {-0.04F, -0.014F}, {0.05F, 0.04F}};
    public static final float[] Glint = {1.0F, 0.72F, 0.62F};

    private OrbitalSkyGlints() {
    }

    public static void render(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || ShaderCompat.renderingShadowPass()) {
            return;
        }
        int Mask = OrbitalClientState.mask();
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        if (Mask < 0 || (Mask & OrbitalPart.FullMask) == 0 || World == null || World.dimension() != Level.OVERWORLD) {
            return;
        }
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float Brightness = World.getStarBrightness(Partial) * (1.0F - World.getRainLevel(Partial));
        if (Brightness <= MinimumBrightness) {
            return;
        }
        float Clock = World.getGameTime() + Partial;
        Quaternionf Sky = new Quaternionf().rotateY(-Mth.HALF_PI).rotateX(World.getTimeOfDay(Partial) * Mth.TWO_PI);
        PoseStack Pose = Event.getPoseStack();
        MultiBufferSource.BufferSource Buffers = Game.renderBuffers().bufferSource();
        RenderType Glow = OrbitalRenderTypes.glow(OrbitalRenderTypes.FlareTexture);
        VertexConsumer Consumer = Buffers.getBuffer(Glow);
        PoseStack.Pose Last = Pose.last();
        Quaternionf Facing = Game.gameRenderer.getMainCamera().rotation();
        for (OrbitalPart Part : OrbitalPart.values()) {
            if ((Mask & Part.bit()) == 0) {
                continue;
            }
            float[] Offset = Offsets[Part.ordinal() % Offsets.length];
            Vector3f Direction = new Vector3f(0.35F + Offset[0], -0.85F, 0.4F + Offset[1]).normalize();
            Sky.transform(Direction);
            Direction.mul(Distance);
            float Twinkle = 0.65F + 0.35F * Mth.sin(Clock * 0.21F + Part.ordinal() * 1.7F);
            float Strength = Brightness * Twinkle;
            Vector3f Right = Facing.transform(new Vector3f(Size, 0.0F, 0.0F));
            Vector3f Up = Facing.transform(new Vector3f(0.0F, Size, 0.0F));
            corner(Consumer, Last, Direction, Right, Up, -1.0F, -1.0F, 0.0F, 1.0F, Strength);
            corner(Consumer, Last, Direction, Right, Up, 1.0F, -1.0F, 1.0F, 1.0F, Strength);
            corner(Consumer, Last, Direction, Right, Up, 1.0F, 1.0F, 1.0F, 0.0F, Strength);
            corner(Consumer, Last, Direction, Right, Up, -1.0F, 1.0F, 0.0F, 0.0F, Strength);
        }
        Buffers.endBatch(Glow);
    }

    private static void corner(VertexConsumer Consumer, PoseStack.Pose Last, Vector3f Centre, Vector3f Right, Vector3f Up, float Across, float Over, float U, float V, float Strength) {
        float X = Centre.x + Right.x * Across + Up.x * Over;
        float Y = Centre.y + Right.y * Across + Up.y * Over;
        float Z = Centre.z + Right.z * Across + Up.z * Over;
        Consumer.addVertex(Last, X, Y, Z).setColor(Mth.clamp(Glint[0] * Strength, 0.0F, 1.0F), Mth.clamp(Glint[1] * Strength, 0.0F, 1.0F), Mth.clamp(Glint[2] * Strength, 0.0F, 1.0F), 1.0F)
                .setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(Last, 0.0F, 1.0F, 0.0F);
    }
}
