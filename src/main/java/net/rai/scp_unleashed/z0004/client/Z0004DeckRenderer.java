package net.rai.scp_unleashed.z0004.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.rai.scp_unleashed.client.geo.SharedBones;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

public class Z0004DeckRenderer extends GeoObjectRenderer<Z0004DeckHand> {
    public Z0004DeckRenderer() {
        super(new Z0004DeckGeoModel());
    }

    @Override
    public void preRender(PoseStack Pose, Z0004DeckHand Hand, BakedGeoModel Model, MultiBufferSource Buffers, VertexConsumer Buffer, boolean ReRender, float Partial,
                          int Light, int Overlay, int Colour) {
        objectRenderTranslations = new Matrix4f(Pose.last().pose());
        scaleModelForRender(scaleWidth, scaleHeight, Pose, Hand, Model, ReRender, Partial, Light, Overlay);
    }

    public void draw(PoseStack Pose, Z0004DeckHand Hand, MultiBufferSource Buffers, int Light, float Partial) {
        try {
            Hand.update(Partial);
            render(Pose, Hand, Buffers, null, null, Light, Partial);
        } finally {
            SharedBones.release(getGeoModel().getAnimationProcessor().getRegisteredBones());
        }
    }
}
