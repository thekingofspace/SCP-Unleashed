package net.rai.scp_unleashed.z0002.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import net.rai.scp_unleashed.z0002.Z0002Shrine;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.cache.texture.AutoGlowingTexture;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.HashMap;
import java.util.Map;

public class Z0002ShrineRenderer extends GeoEntityRenderer<Z0002Shrine> {
    public static final float Scale = 2.0f;
    public static final float LightReach = 0.45f;
    private final Map<Long, Integer> Lights = new HashMap<>();
    private boolean Emissive;
    private boolean Sampling;

    public Z0002ShrineRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Z0002ShrineModel());
        withScale(Scale);
        shadowRadius = 0.0f;
        addRenderLayer(new Glow(this));
    }

    @Override
    public void render(Z0002Shrine Shrine, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Lights.clear();
        Sampling = !ShaderCompat.renderingShadowPass();
        Pose.pushPose();
        Pose.translate(0.0, Shrine.offset(PartialTick), 0.0);
        super.render(Shrine, Yaw, PartialTick, Pose, Buffers, Light);
        Pose.popPose();
    }

    @Override
    public RenderType getRenderType(Z0002Shrine Shrine, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        return RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    protected void applyRotations(Z0002Shrine Shrine, PoseStack Pose, float AgeInTicks, float RotationYaw, float PartialTick, float NativeScale) {
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Shrine.getYRot()));
    }

    @Override
    public void renderRecursively(PoseStack Pose, Z0002Shrine Shrine, GeoBone Bone, RenderType Kind, MultiBufferSource Buffers, VertexConsumer Builder, boolean IsReRender,
                                  float PartialTick, int Light, int Overlay, int Colour) {
        Emissive = IsReRender;
        super.renderRecursively(Pose, Shrine, Bone, Kind, Buffers, Builder, IsReRender, PartialTick, Light, Overlay, Colour);
    }

    @Override
    public void renderCube(PoseStack Pose, GeoCube Cube, VertexConsumer Builder, int Light, int Overlay, int Colour) {
        ClientLevel World = Minecraft.getInstance().level;
        if (Emissive || !Sampling || World == null) {
            super.renderCube(Pose, Cube, Builder, Light, Overlay, Colour);
            return;
        }
        RenderUtil.translateToPivotPoint(Pose, Cube);
        RenderUtil.rotateMatrixAroundCube(Pose, Cube);
        RenderUtil.translateAwayFromPivotPoint(Pose, Cube);
        Matrix3f Normals = Pose.last().normal();
        Matrix4f Positions = new Matrix4f(Pose.last().pose());
        Vec3 Eye = entityRenderDispatcher.camera.getPosition();
        for (GeoQuad Quad : Cube.quads()) {
            if (Quad == null) {
                continue;
            }
            Vector3f Normal = Normals.transform(new Vector3f(Quad.normal()));
            RenderUtil.fixInvertedFlatCube(Cube, Normal);
            Vector3f Middle = new Vector3f();
            for (GeoVertex Corner : Quad.vertices()) {
                Middle.add(Corner.position());
            }
            Middle.div(Quad.vertices().length);
            Vector4f Placed = Positions.transform(new Vector4f(Middle, 1.0f));
            BlockPos Probe = BlockPos.containing(Placed.x() + Eye.x + Normal.x() * LightReach, Placed.y() + Eye.y + Normal.y() * LightReach,
                    Placed.z() + Eye.z + Normal.z() * LightReach);
            int Sampled = Lights.computeIfAbsent(Probe.asLong(), Key -> LevelRenderer.getLightColor(World, Probe));
            createVerticesOfQuad(Quad, Positions, Normal, Builder, Sampled, Overlay, Colour);
        }
    }

    public static class Glow extends AutoGlowingGeoLayer<Z0002Shrine> {
        public Glow(GeoRenderer<Z0002Shrine> Renderer) {
            super(Renderer);
        }

        @Nullable
        @Override
        protected RenderType getRenderType(Z0002Shrine Shrine, @Nullable MultiBufferSource Buffers) {
            if (!ShaderCompat.useFallback()) {
                return super.getRenderType(Shrine, Buffers);
            }
            return RenderType.entityTranslucentEmissive(AutoGlowingTexture.getEmissiveResource(getTextureResource(Shrine)));
        }

        @Override
        public void render(PoseStack Pose, Z0002Shrine Shrine, BakedGeoModel Model, @Nullable RenderType Kind, MultiBufferSource Buffers, @Nullable VertexConsumer Builder,
                           float PartialTick, int Light, int Overlay) {
            if (ShaderCompat.renderingShadowPass()) {
                return;
            }
            super.render(Pose, Shrine, Model, Kind, Buffers, Builder, PartialTick, Light, Overlay);
        }
    }
}
