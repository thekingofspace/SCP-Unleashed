package net.rai.scp_unleashed.instability.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.instability.InstabilityReactorBlockEntity;
import net.rai.scp_unleashed.rift.client.RiftRenderTypes;
import net.rai.scp_unleashed.shadercompat.client.RiftFallback;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.joml.Vector3f;

import java.util.List;

public class InstabilityRenderer implements BlockEntityRenderer<InstabilityReactorBlockEntity> {
    public static final ResourceLocation OuterRing = part("ring_outer");
    public static final ResourceLocation OuterRingLit = part("ring_outer_lit");
    public static final ResourceLocation InnerRing = part("ring_inner");
    public static final ResourceLocation InnerRingLit = part("ring_inner_lit");
    public static final List<ResourceLocation> Models = List.of(OuterRing, OuterRingLit, InnerRing, InnerRingLit);
    private static final ResourceLocation GlowTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/block/instability/fx_glow.png");
    private static final float CoreHalf = 0.31f;
    private static final float CoreExtentX = 0.75f;
    private static final float CoreExtentY = 1.3f;
    private static final float GlowExtentX = 1.6f;
    private static final float GlowExtentY = 2.0f;
    private static final float RingTilt = 27.0f;
    private static final float EmitterReach = 5.25f / 16.0f;
    private static final float NodeReach = 11.5f / 16.0f;
    private static final int RingLight = 12;
    private static final float LitOpen = 0.1f;
    private static final double Precess = 0.35;
    private static final double FirstInner = 1.6;
    private static final double SecondInner = 1.25;
    private static final int CoreViolence = 3;

    public InstabilityRenderer(BlockEntityRendererProvider.Context Context) {
    }

    private static ResourceLocation part(String Name) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/instability/" + Name);
    }

    @Override
    public void render(InstabilityReactorBlockEntity Reactor, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Reactor.getLevel();
        if (World == null) {
            return;
        }
        BlockPos Base = Reactor.getBlockPos();
        Vec3 Centre = Reactor.getCoreCentre();
        float LocalX = (float) (Centre.x - Base.getX());
        float LocalY = (float) (Centre.y - Base.getY());
        float LocalZ = (float) (Centre.z - Base.getZ());
        float Open = Reactor.getOpen(PartialTick);
        float Collapse = Reactor.getCollapse(PartialTick);
        float Flicker = Reactor.getFlicker();
        double Time = Reactor.getTime(PartialTick);
        int Rgb = Reactor.getTint();
        int Light = LevelRenderer.getLightColor(World, BlockPos.containing(Centre));
        if (Open > 0.05f) {
            Light = LightTexture.pack(Math.max(LightTexture.block(Light), RingLight), LightTexture.sky(Light));
        }
        Pose.pushPose();
        Pose.translate(LocalX, LocalY, LocalZ);
        rings(Reactor, PartialTick, Pose, Buffers, Light, Open > LitOpen);
        float Shine = Mth.clamp(Open * (1.0f - Collapse * 0.6f) + Flicker * 0.5f, 0.0f, 1.4f);
        boolean ShadowPass = ShaderCompat.renderingShadowPass();
        if (Shine > 0.01f && !ShadowPass) {
            glows(Pose, Buffers, Rgb, Shine, Time, Flicker);
        }
        Pose.popPose();
        if ((Open > 0.002f || Collapse > 0.0f) && RiftRenderTypes.available() && !ShadowPass) {
            core(Reactor, Pose, Buffers, LocalX, LocalY, LocalZ, Centre, Open, Collapse, Flicker, Time, Rgb);
        }
    }

    private void rings(InstabilityReactorBlockEntity Reactor, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light, boolean Lit) {
        Minecraft Game = Minecraft.getInstance();
        ModelBlockRenderer Renderer = Game.getBlockRenderer().getModelRenderer();
        BakedModel Outer = Game.getModelManager().getModel(ModelResourceLocation.standalone(Lit ? OuterRingLit : OuterRing));
        BakedModel Inner = Game.getModelManager().getModel(ModelResourceLocation.standalone(Lit ? InnerRingLit : InnerRing));
        VertexConsumer Consumer = Buffers.getBuffer(Sheets.cutoutBlockSheet());
        double Turn = Reactor.getRingPhase(PartialTick) * InstabilityReactorBlockEntity.RingSpeed;
        float Precession = wrap(Turn * Precess);
        Pose.pushPose();
        Pose.mulPose(Axis.YP.rotationDegrees(wrap(Turn)));
        ring(Pose, Consumer, Renderer, Outer, Light);
        Pose.popPose();
        Pose.pushPose();
        Pose.mulPose(Axis.YP.rotationDegrees(Precession));
        Pose.mulPose(Axis.XP.rotationDegrees(RingTilt));
        Pose.mulPose(Axis.YP.rotationDegrees(wrap(-Turn * FirstInner)));
        ring(Pose, Consumer, Renderer, Inner, Light);
        Pose.popPose();
        Pose.pushPose();
        Pose.mulPose(Axis.YP.rotationDegrees(Precession + 90.0f));
        Pose.mulPose(Axis.XP.rotationDegrees(-RingTilt));
        Pose.mulPose(Axis.YP.rotationDegrees(wrap(Turn * SecondInner)));
        ring(Pose, Consumer, Renderer, Inner, Light);
        Pose.popPose();
    }

    private static float wrap(double Degrees) {
        return (float) (Degrees % 360.0);
    }

    private static void ring(PoseStack Pose, VertexConsumer Consumer, ModelBlockRenderer Renderer, BakedModel Model, int Light) {
        Pose.pushPose();
        Pose.translate(-0.5f, -0.5f, -0.5f);
        Renderer.renderModel(Pose.last(), Consumer, null, Model, 1.0f, 1.0f, 1.0f, Light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        Pose.popPose();
    }

    private static void glows(PoseStack Pose, MultiBufferSource Buffers, int Rgb, float Shine, double Time, float Flicker) {
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.eyes(GlowTexture));
        float Red = ((Rgb >> 16) & 255) / 255.0f;
        float Green = ((Rgb >> 8) & 255) / 255.0f;
        float Blue = (Rgb & 255) / 255.0f;
        float Throb = 0.8f + 0.2f * (float) Math.sin(Time * 0.21) + Flicker * 0.4f;
        float Strength = Mth.clamp(Shine * Throb, 0.0f, 1.5f);
        sprite(Pose, Consumer, 0.0f, EmitterReach, 0.0f, (1.6f + 0.4f * Flicker) / 16.0f, Red, Green, Blue, Strength);
        sprite(Pose, Consumer, 0.0f, -EmitterReach, 0.0f, (1.6f + 0.4f * Flicker) / 16.0f, Red, Green, Blue, Strength);
        for (int Corner = 0; Corner < 4; Corner++) {
            float Wobble = 0.7f + 0.3f * (float) Math.sin(Time * 0.37 + Corner * 1.7);
            float X = (Corner % 2 == 0 ? -1.0f : 1.0f) * NodeReach;
            float Z = (Corner / 2 == 0 ? -1.0f : 1.0f) * NodeReach;
            sprite(Pose, Consumer, X, 0.0f, Z, 1.1f / 16.0f, Red, Green, Blue, Strength * Wobble * 0.8f);
        }
    }

    private static void sprite(PoseStack Pose, VertexConsumer Consumer, float X, float Y, float Z, float Radius, float Red, float Green, float Blue, float Strength) {
        Pose.pushPose();
        Pose.translate(X, Y, Z);
        Pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        PoseStack.Pose Last = Pose.last();
        float Brightness = Math.min(1.0f, Strength);
        float R = Mth.lerp(0.35f, Red, 1.0f) * Brightness;
        float G = Mth.lerp(0.35f, Green, 1.0f) * Brightness;
        float B = Mth.lerp(0.35f, Blue, 1.0f) * Brightness;
        vertex(Consumer, Last, -Radius, -Radius, 0.0f, 1.0f, R, G, B);
        vertex(Consumer, Last, Radius, -Radius, 1.0f, 1.0f, R, G, B);
        vertex(Consumer, Last, Radius, Radius, 1.0f, 0.0f, R, G, B);
        vertex(Consumer, Last, -Radius, Radius, 0.0f, 0.0f, R, G, B);
        Pose.popPose();
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Y, float U, float V, float Red, float Green, float Blue) {
        Consumer.addVertex(Last, X, Y, 0.0f)
                .setColor(Red, Green, Blue, 1.0f)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(Last, 0.0f, 0.0f, 1.0f);
    }

    private static void core(InstabilityReactorBlockEntity Reactor, PoseStack Pose, MultiBufferSource Buffers, float LocalX, float LocalY, float LocalZ, Vec3 Centre,
                             float Open, float Collapse, float Flicker, double Time, int Rgb) {
        Camera View = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 Eye = View.getPosition();
        Vector3f Depth = facing(Centre.x - Eye.x, Centre.z - Eye.z, View);
        float AcrossX = -Depth.z;
        float AcrossZ = Depth.x;
        float Unrest = 1.0f - Reactor.getEasedCalm();
        float Pull = Mth.clamp(0.06f + 0.05f * Reactor.getEasedGrade() + Unrest * 0.6f + Flicker * 0.55f, 0.0f, 1.0f);
        float Strain = Pull * Pull + 0.6f * Collapse;
        double Seed = (Reactor.getBlockPos().asLong() & 1023L) * 0.37;
        float Sideways = (float) (Math.sin(Time * 2.9 + Seed) * 0.6 + Math.sin(Time * 7.3 + Seed * 1.7) * 0.4) * 0.035f * Strain * CoreHalf;
        float Upward = (float) (Math.sin(Time * 3.7 + Seed * 0.3) * 0.6 + Math.sin(Time * 8.9 + Seed * 2.1) * 0.4) * 0.025f * Strain * CoreHalf;
        float CentreX = LocalX + AcrossX * Sideways;
        float CentreY = LocalY + Upward;
        float CentreZ = LocalZ + AcrossZ * Sideways;
        int Colour = (Mth.clamp(Math.round(Open * 255.0f), 0, 255) << 24) | (Rgb & 0xFFFFFF);
        int Extraction = Mth.clamp(Math.round(Pull * 1000.0f), 0, 1000);
        int Implosion = Mth.clamp(Math.round(Collapse * 1000.0f), 0, 1000);
        int Variant = (int) (Reactor.getBlockPos().asLong() & 4095L);
        int Profile = CoreViolence + 16 * Mth.clamp(Math.round(CoreHalf * 100.0f), 5, 2000);
        PoseStack.Pose Last = Pose.last();
        if (RiftRenderTypes.fallback()) {
            RiftFallback.draw(Last, Buffers, CentreX, CentreY, CentreZ, AcrossX, AcrossZ, CoreHalf, Colour, Extraction, Implosion, Variant, Profile);
            return;
        }
        CoreQuad Quad = new CoreQuad(Last, CentreX, CentreY, CentreZ, AcrossX, AcrossZ, CoreHalf, Colour, Extraction, Implosion, Variant, Profile, Depth.x, Depth.z);
        Quad.draw(Buffers.getBuffer(RiftRenderTypes.Core), CoreExtentX, CoreExtentY);
        Quad.draw(Buffers.getBuffer(RiftRenderTypes.Glow), GlowExtentX, GlowExtentY);
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
    public boolean shouldRenderOffScreen(InstabilityReactorBlockEntity Reactor) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(InstabilityReactorBlockEntity Reactor) {
        return Reactor.getRenderBounds();
    }

    private record CoreQuad(PoseStack.Pose Last, float CentreX, float CentreY, float CentreZ, float AcrossX, float AcrossZ, float Half, int Colour,
                            int Extraction, int Implosion, int Variant, int Profile, float DepthX, float DepthZ) {
        void draw(VertexConsumer Consumer, float ExtentX, float ExtentY) {
            corner(Consumer, -ExtentX, -ExtentY);
            corner(Consumer, ExtentX, -ExtentY);
            corner(Consumer, ExtentX, ExtentY);
            corner(Consumer, -ExtentX, ExtentY);
        }

        private void corner(VertexConsumer Consumer, float U, float V) {
            Consumer.addVertex(Last, CentreX + AcrossX * U * Half, CentreY + V * Half, CentreZ + AcrossZ * U * Half)
                    .setColor(Colour)
                    .setUv(U, V)
                    .setUv1(Extraction, Implosion)
                    .setUv2(Variant, Profile)
                    .setNormal(DepthX, 0.0f, DepthZ);
        }
    }
}
