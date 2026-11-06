package net.rai.scp_unleashed.scp024.client;

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
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp024.Scp024Spotlight;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.joml.Vector3f;

public class Scp024SpotlightRenderer extends EntityRenderer<Scp024Spotlight> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp024/spotlight.png");
    public static final int Segments = 7;
    public static final float RootRadius = 0.14f;
    public static final float Step = 1.0f / 16.0f;
    public static final float BeamAlpha = 0.30f;
    public static final float PoolAlpha = 0.55f;
    public static final float AdditiveScale = 0.42f;
    public static final float LensSize = 0.22f;

    public Scp024SpotlightRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
        shadowStrength = 0.0f;
    }

    @Override
    public void render(Scp024Spotlight Beam, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        if (ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Power = Beam.power();
        if (Power <= 0.01f) {
            return;
        }
        boolean Additive = ShaderCompat.useFallback();
        RenderType Type = Additive ? RenderType.eyes(Texture) : RenderType.beaconBeam(Texture, true);
        VertexConsumer Consumer = Buffers.getBuffer(Type);
        Vec3 Origin = Beam.getPosition(PartialTick);
        Vec3 Spot = Beam.target(PartialTick).subtract(Origin);
        double Length = Spot.length();
        if (Length < 0.5) {
            return;
        }
        Vector3f Along = new Vector3f((float) (Spot.x / Length), (float) (Spot.y / Length), (float) (Spot.z / Length));
        Vector3f Helper = Math.abs(Along.y) > 0.95f ? new Vector3f(1.0f, 0.0f, 0.0f) : new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f Across = new Vector3f(Along).cross(Helper).normalize();
        Vector3f Second = new Vector3f(Along).cross(Across).normalize();
        int Rgb = Beam.colour();
        float Red = (Rgb >> 16 & 255) / 255.0f;
        float Green = (Rgb >> 8 & 255) / 255.0f;
        float Blue = (Rgb & 255) / 255.0f;
        float Scale = Additive ? AdditiveScale * Power : Math.min(1.0f, Power);
        float Width = Beam.width();
        PoseStack.Pose Last = Pose.last();
        for (int Segment = 0; Segment < Segments; Segment++) {
            float Start = Segment / (float) Segments;
            float End = (Segment + 1) / (float) Segments;
            float Radius = RootRadius + (Width - RootRadius) * End;
            Radius = Math.max(Step, Mth.floor(Radius / Step) * Step);
            float Fade = 1.0f - 0.45f * Start;
            float Alpha = Additive ? 1.0f : BeamAlpha * Fade * Math.min(1.0f, Power);
            float Shade = Additive ? Scale * Fade : 1.0f;
            Vector3f Near = new Vector3f(Along).mul((float) (Length * Start));
            Vector3f Far = new Vector3f(Along).mul((float) (Length * End));
            for (int Side = 0; Side < 4; Side++) {
                Vector3f Normal = Side == 0 ? Across : Side == 1 ? Second : Side == 2 ? new Vector3f(Across).negate() : new Vector3f(Second).negate();
                Vector3f Tangent = Side == 0 ? Second : Side == 1 ? new Vector3f(Across).negate() : Side == 2 ? new Vector3f(Second).negate() : Across;
                Vector3f Offset = new Vector3f(Normal).mul(Radius);
                Vector3f Wide = new Vector3f(Tangent).mul(Radius);
                Vector3f A = new Vector3f(Near).add(Offset).sub(Wide);
                Vector3f B = new Vector3f(Near).add(Offset).add(Wide);
                Vector3f C = new Vector3f(Far).add(Offset).add(Wide);
                Vector3f D = new Vector3f(Far).add(Offset).sub(Wide);
                quad(Consumer, Last, A, B, C, D, 0.0f, 0.5f, Start, End, Red * Shade, Green * Shade, Blue * Shade, Alpha);
            }
        }
        Vector3f Ground = new Vector3f((float) Spot.x, (float) Spot.y + 0.03f, (float) Spot.z);
        float Pool = Mth.floor(Width * 1.15f / Step) * Step;
        float PoolShade = Additive ? Scale * 0.8f : 1.0f;
        float PoolOpacity = Additive ? 1.0f : PoolAlpha * Math.min(1.0f, Power);
        quad(Consumer, Last, new Vector3f(Ground).add(-Pool, 0.0f, -Pool), new Vector3f(Ground).add(Pool, 0.0f, -Pool), new Vector3f(Ground).add(Pool, 0.0f, Pool),
                new Vector3f(Ground).add(-Pool, 0.0f, Pool), 0.5f, 1.0f, 0.0f, 1.0f, Red * PoolShade, Green * PoolShade, Blue * PoolShade, PoolOpacity);
        float Lens = LensSize;
        for (int Side = 0; Side < 3; Side++) {
            Vector3f Normal = Side == 0 ? new Vector3f(1.0f, 0.0f, 0.0f) : Side == 1 ? new Vector3f(0.0f, 1.0f, 0.0f) : new Vector3f(0.0f, 0.0f, 1.0f);
            Vector3f First = Side == 0 ? new Vector3f(0.0f, 1.0f, 0.0f) : new Vector3f(1.0f, 0.0f, 0.0f);
            Vector3f Other = Side == 2 ? new Vector3f(0.0f, 1.0f, 0.0f) : new Vector3f(0.0f, 0.0f, 1.0f);
            First.mul(Lens);
            Other.mul(Lens);
            quad(Consumer, Last, new Vector3f(First).negate().sub(Other), new Vector3f(First).sub(Other), new Vector3f(First).add(Other), new Vector3f(Other).sub(First),
                    0.25f, 0.3f, 0.0f, 0.05f, Red * (Additive ? Scale : 1.0f), Green * (Additive ? Scale : 1.0f), Blue * (Additive ? Scale : 1.0f), Additive ? 1.0f : 0.8f);
        }
        super.render(Beam, Yaw, PartialTick, Pose, Buffers, PackedLight);
    }

    private static void quad(VertexConsumer Consumer, PoseStack.Pose Last, Vector3f A, Vector3f B, Vector3f C, Vector3f D, float U0, float U1, float V0, float V1,
                             float Red, float Green, float Blue, float Alpha) {
        vertex(Consumer, Last, A, U0, V0, Red, Green, Blue, Alpha);
        vertex(Consumer, Last, B, U1, V0, Red, Green, Blue, Alpha);
        vertex(Consumer, Last, C, U1, V1, Red, Green, Blue, Alpha);
        vertex(Consumer, Last, D, U0, V1, Red, Green, Blue, Alpha);
        vertex(Consumer, Last, D, U0, V1, Red, Green, Blue, Alpha);
        vertex(Consumer, Last, C, U1, V1, Red, Green, Blue, Alpha);
        vertex(Consumer, Last, B, U1, V0, Red, Green, Blue, Alpha);
        vertex(Consumer, Last, A, U0, V0, Red, Green, Blue, Alpha);
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, Vector3f Point, float U, float V, float Red, float Green, float Blue, float Alpha) {
        Consumer.addVertex(Last, Point.x, Point.y, Point.z)
                .setColor(Red, Green, Blue, Alpha)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(Last, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(Scp024Spotlight Beam) {
        return Texture;
    }
}
