package net.rai.scp_unleashed.scp096.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp096.Scp096ShyGuy;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtil;

public class Scp096Renderer extends GeoEntityRenderer<Scp096ShyGuy> {
    public static final float ShadowSize = 0.6f;

    public Scp096Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp096GeoModel());
        shadowRadius = ShadowSize;
    }

    @Override
    public void render(Scp096ShyGuy Guy, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Guy.setGaitPartial(PartialTick);
        super.render(Guy, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    public void renderRecursively(PoseStack Pose, Scp096ShyGuy Guy, GeoBone Bone, RenderType Type, MultiBufferSource Buffers, VertexConsumer Buffer, boolean IsReRender,
                                  float PartialTick, int Light, int Overlay, int Colour) {
        String Name = Bone.getName();
        boolean IsFace = Scp096GeoModel.FaceBone.equals(Name);
        if (!IsReRender && Guy != null && (IsFace || Scp096GeoModel.ChinBone.equals(Name))) {
            Pose.pushPose();
            RenderUtil.translateMatrixToBone(Pose, Bone);
            RenderUtil.translateToPivotPoint(Pose, Bone);
            RenderUtil.rotateMatrixAroundBone(Pose, Bone);
            RenderUtil.scaleMatrixForBone(Pose, Bone);
            Matrix4f Local = new Matrix4f(entityRenderTranslations).invert().mul(Pose.last().pose());
            Pose.popPose();
            Scp096Faces.capture(Guy, IsFace, Local, PartialTick);
        }
        super.renderRecursively(Pose, Guy, Bone, Type, Buffers, Buffer, IsReRender, PartialTick, Light, Overlay, Colour);
    }
}
