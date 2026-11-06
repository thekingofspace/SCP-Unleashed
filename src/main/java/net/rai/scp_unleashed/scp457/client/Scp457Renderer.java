package net.rai.scp_unleashed.scp457.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.rai.scp_unleashed.scp457.Scp457;
import net.rai.scp_unleashed.scp457.Scp457Effects;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Optional;

public class Scp457Renderer extends GeoEntityRenderer<Scp457> {
    public static final float ShadowSize = 0.6f;
    public static final int SelfLight = 9;

    public Scp457Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp457GeoModel());
        addRenderLayer(new Scp457FlameLayer(this));
        shadowRadius = ShadowSize;
    }

    @Override
    public void render(Scp457 Owner, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Owner.setGaitPartial(PartialTick);
        Owner.setRenderPartial(PartialTick);
        super.render(Owner, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    protected float getDeathMaxRotation(Scp457 Owner) {
        return 0.0f;
    }

    @Override
    protected int getBlockLightLevel(Scp457 Owner, BlockPos Pos) {
        return Math.max(SelfLight, super.getBlockLightLevel(Owner, Pos));
    }

    @Override
    public void renderFinal(PoseStack Pose, Scp457 Owner, BakedGeoModel Model, MultiBufferSource Buffers, VertexConsumer Buffer, float PartialTick, int PackedLight,
                            int PackedOverlay, int Colour) {
        super.renderFinal(Pose, Owner, Model, Buffers, Buffer, PartialTick, PackedLight, PackedOverlay, Colour);
        Scp457BodyFire.learn(Model);
        Matrix4f[] Poses = new Matrix4f[Scp457Effects.TrackedBones.length];
        for (int Index = 0; Index < Poses.length; Index++) {
            Optional<GeoBone> Found = Model.getBone(Scp457Effects.TrackedBones[Index]);
            if (Found.isEmpty() || !Found.get().isTrackingMatrices()) {
                return;
            }
            Poses[Index] = new Matrix4f(Found.get().getLocalSpaceMatrix());
        }
        Owner.setBonePoses(Poses);
    }

    @Override
    public int getPackedOverlay(Scp457 Owner, float U, float PartialTick) {
        return OverlayTexture.pack(OverlayTexture.u(U), OverlayTexture.v(Owner.hurtTime > 0 && Owner.deathTime <= 0));
    }
}
