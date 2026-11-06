package net.rai.scp_unleashed.scp049.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Locale;

public class Scp049ShambleLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
    public static final float Lift = 1.4f;
    public static final float AngryLift = 1.75f;
    public static final float Bob = 0.06f;
    public static final float Inward = 0.08f;

    public Scp049ShambleLayer(GeoRenderer<T> Renderer) {
        super(Renderer);
    }

    public static int armSide(String Name) {
        String Plain = Name.toLowerCase(Locale.ROOT).replace("_", "");
        if (Plain.equals("rightarm") || Plain.equals("armright")) {
            return 1;
        }
        if (Plain.equals("leftarm") || Plain.equals("armleft")) {
            return -1;
        }
        return 0;
    }

    @Override
    public void renderForBone(PoseStack Pose, T Animatable, GeoBone Bone, RenderType Type, MultiBufferSource Buffers, VertexConsumer Buffer, float Partial, int Light,
                              int Overlay) {
        if (!(Animatable instanceof Entity Body) || !Scp049Mimicry.isShambling(Body)) {
            return;
        }
        float Age = Body.tickCount + Partial;
        float Raise = Scp049Mimicry.angry() ? AngryLift : Lift;
        for (GeoBone Child : Bone.getChildBones()) {
            int Side = armSide(Child.getName());
            if (Side == 0) {
                continue;
            }
            Child.setRotX(Raise + Mth.sin(Age * 0.067f + Side) * Bob);
            Child.setRotY(Side * Inward);
            Child.setRotZ(Side * Mth.cos(Age * 0.09f) * 0.04f);
        }
    }
}
