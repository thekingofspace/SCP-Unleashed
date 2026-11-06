package net.rai.scp_unleashed.rift.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.rift.RiftEntity;
import net.rai.scp_unleashed.shadercompat.client.RiftFallback;
import org.joml.Vector3f;

public class RiftRenderer extends EntityRenderer<RiftEntity> {
    public static final float CoreExtentX = 0.75f;
    public static final float CoreExtentY = 1.3f;
    public static final float GlowExtentX = 1.6f;
    public static final float GlowExtentY = 2.0f;
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/particle/rift/mote_0.png");

    public RiftRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
        shadowStrength = 0.0f;
    }

    @Override
    public void render(RiftEntity Rift, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        if (!RiftRenderTypes.available() || RiftRenderTypes.shadowPass()) {
            return;
        }
        float Open = Rift.getOpenness(PartialTick);
        if (Open <= 0.002f) {
            return;
        }
        float Half = Rift.getRiftHeight() * 0.5f;
        float Extraction = Rift.getShownExtraction(PartialTick);
        float Collapse = Rift.getCollapse(PartialTick);
        Camera View = entityRenderDispatcher.camera;
        Vec3 Eye = View.getPosition();
        Vec3 Origin = Rift.getPosition(PartialTick);
        Vector3f Depth = facing(Origin.x - Eye.x, Origin.z - Eye.z, View);
        float AcrossX = -Depth.z;
        float AcrossZ = Depth.x;
        float Strain = Extraction * Extraction + 0.6f * Collapse;
        double Time = Rift.level().getGameTime() % 24000L + PartialTick;
        float Seed = Rift.getSeed();
        float Sideways = (float) (Math.sin(Time * 2.9 + Seed) * 0.6 + Math.sin(Time * 7.3 + Seed * 1.7) * 0.4) * 0.05f * Strain * Half;
        float Upward = (float) (Math.sin(Time * 3.7 + Seed * 0.3) * 0.6 + Math.sin(Time * 8.9 + Seed * 2.1) * 0.4) * 0.035f * Strain * Half;
        float CenterX = AcrossX * Sideways;
        float CenterY = Half + Upward;
        float CenterZ = AcrossZ * Sideways;
        int Colour = (Mth.clamp(Math.round(Open * 255.0f), 0, 255) << 24) | (Rift.getRiftColour() & 0xFFFFFF);
        int Pull = Mth.clamp(Math.round(Extraction * 1000.0f), 0, 1000);
        int Implosion = Mth.clamp(Math.round(Collapse * 1000.0f), 0, 1000);
        int Variant = Rift.getSeed() & 4095;
        int Profile = Rift.getRarity().getTier() + 16 * Mth.clamp(Math.round(Half * 100.0f), 5, 2000);
        PoseStack.Pose Last = Pose.last();
        if (RiftRenderTypes.fallback()) {
            RiftFallback.draw(Last, Buffers, CenterX, CenterY, CenterZ, AcrossX, AcrossZ, Half, Colour, Pull, Implosion, Variant, Profile);
            return;
        }
        Quad Core = new Quad(Last, CenterX, CenterY, CenterZ, AcrossX, AcrossZ, Half, Colour, Pull, Implosion, Variant, Profile, Depth.x, Depth.z);
        Core.draw(Buffers.getBuffer(RiftRenderTypes.Core), CoreExtentX, CoreExtentY);
        Core.draw(Buffers.getBuffer(RiftRenderTypes.Glow), GlowExtentX, GlowExtentY);
    }

    private static Vector3f facing(double OffsetX, double OffsetZ, Camera View) {
        double Length = Math.sqrt(OffsetX * OffsetX + OffsetZ * OffsetZ);
        if (Length > 0.001) {
            return new Vector3f((float) (OffsetX / Length), 0.0f, (float) (OffsetZ / Length));
        }
        Vector3f Look = new Vector3f(View.getLookVector().x(), 0.0f, View.getLookVector().z());
        if (Look.lengthSquared() < 0.0001f) {
            return new Vector3f(0.0f, 0.0f, 1.0f);
        }
        return Look.normalize();
    }

    @Override
    public ResourceLocation getTextureLocation(RiftEntity Rift) {
        return Texture;
    }

    private record Quad(PoseStack.Pose Last, float CenterX, float CenterY, float CenterZ, float AcrossX, float AcrossZ, float Half, int Colour,
                        int Pull, int Implosion, int Variant, int Profile, float DepthX, float DepthZ) {
        void draw(VertexConsumer Consumer, float ExtentX, float ExtentY) {
            corner(Consumer, -ExtentX, -ExtentY);
            corner(Consumer, ExtentX, -ExtentY);
            corner(Consumer, ExtentX, ExtentY);
            corner(Consumer, -ExtentX, ExtentY);
        }

        private void corner(VertexConsumer Consumer, float U, float V) {
            Consumer.addVertex(Last, CenterX + AcrossX * U * Half, CenterY + V * Half, CenterZ + AcrossZ * U * Half)
                    .setColor(Colour)
                    .setUv(U, V)
                    .setUv1(Pull, Implosion)
                    .setUv2(Variant, Profile)
                    .setNormal(DepthX, 0.0f, DepthZ);
        }
    }
}
