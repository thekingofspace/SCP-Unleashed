package net.rai.scp_unleashed.scp217.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.dclass.DClassLike;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Map;

public class Scp217GeoLayer<T extends DClassLike> extends GeoRenderLayer<T> {
    public static final float ModelTop = 1.5f;
    public static final Map<String, Vector3f> Pivots = Map.of(
            "head", new Vector3f(0.0f, 0.0f, 0.0f),
            "body", new Vector3f(0.0f, 0.0f, 0.0f),
            "right_arm", new Vector3f(-5.0f, 2.0f, 0.0f),
            "left_arm", new Vector3f(5.0f, 2.0f, 0.0f),
            "right_leg", new Vector3f(-1.9f, 12.0f, 0.0f),
            "left_leg", new Vector3f(1.9f, 12.0f, 0.0f));

    public Scp217GeoLayer(GeoRenderer<T> Renderer) {
        super(Renderer);
    }

    @Override
    public void renderForBone(PoseStack Pose, T Owner, GeoBone Bone, RenderType Type, MultiBufferSource Buffers, VertexConsumer Buffer, float Partial, int Light, int Overlay) {
        Vector3f Pivot = Pivots.get(Bone.getName());
        if (Pivot == null || Owner.isSpecimen() || Owner.isInvisible()) {
            return;
        }
        LivingEntity Source = Owner.isPuppet() && Owner.puppetOwner() != null ? Owner.puppetOwner() : Owner;
        Scp217ClientState.Entry Found = Scp217ClientState.get(Source.getId());
        if (Found == null || Found.stage() < Scp217ClockworkRenderer.FirstVisibleStage) {
            return;
        }
        Pose.pushPose();
        Pose.scale(-1.0f, -1.0f, 1.0f);
        Pose.translate(0.0f, -ModelTop, 0.0f);
        Pose.translate(Pivot.x() * Scp217ClockworkRenderer.Pixel, Pivot.y() * Scp217ClockworkRenderer.Pixel, Pivot.z() * Scp217ClockworkRenderer.Pixel);
        Scp217ClockworkRenderer.draw(Pose, Buffers, Light, Source, Bone.getName(), Found.stage(), Found.steps(Partial), false, false, Found.perfect());
        Pose.popPose();
    }
}
