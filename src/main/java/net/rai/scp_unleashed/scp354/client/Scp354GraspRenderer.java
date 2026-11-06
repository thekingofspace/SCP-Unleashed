package net.rai.scp_unleashed.scp354.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp354.Scp354Grasp;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp354GraspRenderer extends GeoEntityRenderer<Scp354Grasp> {
    public static final float BaseGirth = 0.6f;
    public static final float MaxScale = 3.0f;

    public Scp354GraspRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp354GeoModel<>("scp354_grasp", "grasp", "rise", Grasp -> Grasp.stage() == Scp354Grasp.StageRise));
        shadowRadius = 0.0f;
    }

    @Override
    public void render(Scp354Grasp Grasp, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Grasp.setRenderPartial(PartialTick);
        float Scale = Mth.clamp(Grasp.girth() / BaseGirth, 1.0f, MaxScale);
        scaleWidth = Scale;
        scaleHeight = Scale;
        Pose.pushPose();
        Entity Victim = Grasp.victim();
        if (Victim != null && Grasp.stage() != Scp354Grasp.StageRelease) {
            Vec3 Held = Victim.getPosition(PartialTick);
            Vec3 Own = Grasp.getPosition(PartialTick);
            Vec3 Shift = Held.subtract(Own);
            if (Shift.lengthSqr() < 4.0) {
                Pose.translate(Shift.x, Shift.y, Shift.z);
            }
        }
        super.render(Grasp, Grasp.getYRot(), PartialTick, Pose, Buffers, Light);
        Pose.popPose();
    }

    @Override
    public RenderType getRenderType(Scp354Grasp Grasp, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        return RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    protected void applyRotations(Scp354Grasp Grasp, PoseStack Pose, float AgeInTicks, float RotationYaw, float PartialTick, float NativeScale) {
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - RotationYaw));
    }
}
