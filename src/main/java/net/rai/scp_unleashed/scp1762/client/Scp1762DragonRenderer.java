package net.rai.scp_unleashed.scp1762.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp1762.Scp1762Dragon;
import net.rai.scp_unleashed.scp1762.Scp1762Kind;
import software.bernie.geckolib.util.Color;

public class Scp1762DragonRenderer extends SharedGeoRenderer<Scp1762Dragon> {
    public static final float Shadow = 0.12f;

    public Scp1762DragonRenderer(EntityRendererProvider.Context Context, Scp1762Kind Kind) {
        super(Context, new Scp1762DragonGeoModel(Kind));
        withScale(Kind.Scale);
        shadowRadius = Shadow;
    }

    @Override
    public void render(Scp1762Dragon Dragon, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Dragon.setRenderPartial(Partial);
        super.render(Dragon, Yaw, Partial, Pose, Buffers, Light);
    }

    @Override
    public Color getRenderColor(Scp1762Dragon Dragon, float Partial, int Light) {
        return Color.ofOpaque(Dragon.tint());
    }

    @Override
    protected void applyRotations(Scp1762Dragon Dragon, PoseStack Pose, float Age, float Yaw, float Partial, float NativeScale) {
        super.applyRotations(Dragon, Pose, Age, Yaw, Partial, NativeScale);
        float Pitch = Mth.lerp(Partial, Dragon.xRotO, Dragon.getXRot());
        float Roll = Dragon.roll(Partial);
        if (Math.abs(Pitch) < 0.01f && Math.abs(Roll) < 0.01f) {
            return;
        }
        float Middle = Dragon.getBbHeight() * 0.5f;
        Pose.translate(0.0f, Middle, 0.0f);
        Pose.mulPose(Axis.XP.rotationDegrees(-Pitch));
        Pose.mulPose(Axis.ZP.rotationDegrees(Roll));
        Pose.translate(0.0f, -Middle, 0.0f);
    }
}
