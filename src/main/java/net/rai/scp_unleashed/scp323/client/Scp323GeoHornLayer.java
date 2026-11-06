package net.rai.scp_unleashed.scp323.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.dclass.DClassLike;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class Scp323GeoHornLayer<T extends DClassLike> extends GeoRenderLayer<T> {
    public static final String HeadBone = "head";
    public static final float ModelTop = 1.5f;
    private final ModelPart Horns;

    public Scp323GeoHornLayer(GeoRenderer<T> Renderer, EntityModelSet Models) {
        super(Renderer);
        Horns = Models.bakeLayer(Scp323HornShape.LayerLocation);
    }

    @Override
    public void renderForBone(PoseStack Pose, T Owner, GeoBone Bone, RenderType Type, MultiBufferSource Buffers, VertexConsumer Buffer, float Partial, int Light, int Overlay) {
        if (!HeadBone.equals(Bone.getName()) || Owner.isInvisible() || Owner.isSpecimen()) {
            return;
        }
        Entity Subject = Owner.isPuppet() && Owner.puppetOwner() != null ? Owner.puppetOwner() : Owner;
        if (!Scp323ClientBonds.horned(Subject)) {
            return;
        }
        Pose.pushPose();
        Pose.scale(-1.0f, -1.0f, 1.0f);
        Pose.translate(0.0f, -ModelTop, 0.0f);
        Horns.render(Pose, Buffers.getBuffer(RenderType.entityCutoutNoCull(Scp323HornShape.Texture)), Light, OverlayTexture.NO_OVERLAY);
        Pose.popPose();
    }
}
