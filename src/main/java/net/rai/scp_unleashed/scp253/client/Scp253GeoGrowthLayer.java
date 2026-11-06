package net.rai.scp_unleashed.scp253.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.scp253.Scp253Display;
import net.rai.scp_unleashed.scp253.Scp253Growths;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class Scp253GeoGrowthLayer<T extends DClassLike> extends GeoRenderLayer<T> {
    public static final float ModelTop = 1.5f;
    public static final float[][] Rest = {{0.0f, 0.0f}, {0.0f, 0.0f}, {-5.0f, 2.0f}, {5.0f, 2.0f}, {-1.9f, 12.0f}, {1.9f, 12.0f}};

    public Scp253GeoGrowthLayer(GeoRenderer<T> Renderer) {
        super(Renderer);
    }

    public static int partOf(String Bone) {
        return switch (Bone) {
            case "head" -> Scp253Growths.Head;
            case "body" -> Scp253Growths.Body;
            case "right_arm" -> Scp253Growths.RightArm;
            case "left_arm" -> Scp253Growths.LeftArm;
            case "right_leg" -> Scp253Growths.RightLeg;
            case "left_leg" -> Scp253Growths.LeftLeg;
            default -> -1;
        };
    }

    @Override
    public void renderForBone(PoseStack Pose, T Owner, GeoBone Bone, RenderType Type, MultiBufferSource Buffers, VertexConsumer Buffer, float Partial, int Light, int Overlay) {
        int Part = partOf(Bone.getName());
        if (Part < 0 || Owner.isInvisible() || Bone.isHidden()) {
            return;
        }
        LivingEntity Driver = Owner.isPuppet() ? Owner.puppetOwner() : null;
        LivingEntity Source = Driver != null ? Driver : Owner;
        Scp253Display Shown = Scp253GrowthPainter.display(Source);
        if (Shown == null) {
            return;
        }
        float Progress = Scp253GrowthPainter.progress(Source, Shown, Partial);
        if (!Scp253GrowthPainter.visible(Progress)) {
            return;
        }
        float[] Lifts = Scp253GrowthPainter.lifts(Owner, Scp253GrowthPainter.SkinLift, Scp253GrowthPainter.HatLift);
        Pose.pushPose();
        Pose.scale(-1.0f, -1.0f, 1.0f);
        Pose.translate(0.0f, -ModelTop, 0.0f);
        Pose.translate(Rest[Part][0] / 16.0f, Rest[Part][1] / 16.0f, 0.0f);
        Scp253GrowthPainter.paint(Pose, Buffers, Light, Overlay, Scp253GrowthPainter.layout(Shown.Seed()), Progress, Part, Scp253Growths.Wide[Part], Lifts[Part]);
        Pose.popPose();
    }
}
