package net.rai.scp_unleashed.scp007.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp007.Scp007Entity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp007Renderer extends GeoEntityRenderer<Scp007Entity> {
    public static final float Shadow = 0.45f;
    public static final float NightSide = 0.25f;

    private final Vector3f Sun = new Vector3f(0.0f, 1.0f, 0.0f);
    private boolean GlowPass;

    public Scp007Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp007GeoModel());
        addRenderLayer(new Scp007LightsLayer(this));
        shadowRadius = Shadow;
    }

    @Override
    public void render(Scp007Entity Owner, float Yaw, float PartialTick, PoseStack Matrices, MultiBufferSource Buffers, int Light) {
        Owner.setGaitPartial(PartialTick);
        super.render(Owner, Yaw, PartialTick, Matrices, Buffers, Light);
    }

    public void beginGlow(Vector3f Direction) {
        Sun.set(Direction);
        GlowPass = true;
    }

    public void endGlow() {
        GlowPass = false;
    }

    @Override
    public void renderCubesOfBone(PoseStack Matrices, GeoBone Bone, VertexConsumer Buffer, int Light, int Overlay, int Colour) {
        if (GlowPass && !Scp007GeoModel.PlanetBone.equals(Bone.getName())) {
            return;
        }
        super.renderCubesOfBone(Matrices, Bone, Buffer, Light, Overlay, Colour);
    }

    @Override
    public void createVerticesOfQuad(GeoQuad Quad, Matrix4f Pose, Vector3f Normal, VertexConsumer Buffer, int Light, int Overlay, int Colour) {
        if (GlowPass && Normal.dot(Sun) > NightSide) {
            return;
        }
        super.createVerticesOfQuad(Quad, Pose, Normal, Buffer, Light, Overlay, Colour);
    }
}
