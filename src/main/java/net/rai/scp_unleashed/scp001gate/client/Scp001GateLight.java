package net.rai.scp_unleashed.scp001gate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001gate.Scp001GateRegistry;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public final class Scp001GateLight {
    public static final ResourceLocation Trail = Scp001GateRegistry.location("textures/entity/scp001_gate/flame_trail.png");
    public static final ResourceLocation BladeFire = Scp001GateRegistry.location("textures/entity/scp001_gate/blade_fire.png");
    public static final ResourceLocation Flash = Scp001GateRegistry.location("textures/entity/scp001_gate/flash.png");
    public static final ResourceLocation Shockwave = Scp001GateRegistry.location("textures/entity/scp001_gate/shockwave.png");
    public static final ResourceLocation Scorch = Scp001GateRegistry.location("textures/entity/scp001_gate/scorch.png");
    public static final ResourceLocation Garden = Scp001GateRegistry.location("textures/entity/scp001_gate/garden_view.png");
    public static final ResourceLocation Plain = Scp001GateRegistry.location("textures/entity/scp001_gate/plain_view.png");

    private Scp001GateLight() {
    }

    public static RenderType glow(ResourceLocation Texture) {
        return ShaderCompat.useFallback() ? RenderType.eyes(Texture) : RenderType.entityTranslucentEmissive(Texture);
    }

    public static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 At, float U, float V, float R, float G, float B, float Alpha, int Light, Vec3 Normal) {
        boolean Additive = ShaderCompat.useFallback();
        float Scale = Additive ? Alpha : 1.0f;
        Consumer.addVertex(Last, (float) At.x, (float) At.y, (float) At.z).setColor(R * Scale, G * Scale, B * Scale, Additive ? 1.0f : Alpha).setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(Light).setNormal(Last, (float) Normal.x, (float) Normal.y, (float) Normal.z);
    }

    public static void quad(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 A, Vec3 B, Vec3 C, Vec3 D, float U0, float V0, float U1, float V1, float R, float G, float Bl,
                            float Alpha, int Light) {
        Vec3 Normal = B.subtract(A).cross(D.subtract(A)).normalize();
        Vec3 Back = Normal.scale(-1.0);
        vertex(Consumer, Last, A, U0, V0, R, G, Bl, Alpha, Light, Normal);
        vertex(Consumer, Last, B, U0, V1, R, G, Bl, Alpha, Light, Normal);
        vertex(Consumer, Last, C, U1, V1, R, G, Bl, Alpha, Light, Normal);
        vertex(Consumer, Last, D, U1, V0, R, G, Bl, Alpha, Light, Normal);
        vertex(Consumer, Last, D, U1, V0, R, G, Bl, Alpha, Light, Back);
        vertex(Consumer, Last, C, U1, V1, R, G, Bl, Alpha, Light, Back);
        vertex(Consumer, Last, B, U0, V1, R, G, Bl, Alpha, Light, Back);
        vertex(Consumer, Last, A, U0, V0, R, G, Bl, Alpha, Light, Back);
    }

    public static void ribbon(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Eye, Vec3[] Points, float Width, float Scroll, float Alpha) {
        for (int Index = 0; Index + 1 < Points.length; Index++) {
            Vec3 From = Points[Index];
            Vec3 To = Points[Index + 1];
            Vec3 Along = To.subtract(From);
            if (Along.lengthSqr() < 1.0e-6) {
                continue;
            }
            Vec3 Side = Along.cross(From.subtract(Eye)).normalize().scale(Width * (1.0f - 0.4f * Index / Points.length));
            float V0 = Scroll + Index * 0.25f;
            float V1 = V0 + 0.25f;
            float Fade = Alpha * (0.55f + 0.45f * (1.0f - (float) Index / Points.length));
            quad(Consumer, Last, From.subtract(Side), To.subtract(Side), To.add(Side), From.add(Side), 0.0f, V0, 1.0f, V1, 1.0f, 1.0f, 1.0f, Fade, LightTexture.FULL_BRIGHT);
        }
    }

    public static void flat(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Centre, Vec3 Normal, float Radius, float Spin, float R, float G, float B, float Alpha, int Light) {
        Vec3 Facing = Normal.normalize();
        Vec3 Helper = Math.abs(Facing.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 First = Facing.cross(Helper).normalize();
        Vec3 Second = Facing.cross(First).normalize();
        double Cos = Math.cos(Spin);
        double Sin = Math.sin(Spin);
        Vec3 U = First.scale(Cos).add(Second.scale(Sin)).scale(Radius);
        Vec3 V = Second.scale(Cos).subtract(First.scale(Sin)).scale(Radius);
        quad(Consumer, Last, Centre.subtract(U).subtract(V), Centre.subtract(U).add(V), Centre.add(U).add(V), Centre.add(U).subtract(V), 0.0f, 0.0f, 1.0f, 1.0f, R, G, B, Alpha,
                Light);
    }
}
