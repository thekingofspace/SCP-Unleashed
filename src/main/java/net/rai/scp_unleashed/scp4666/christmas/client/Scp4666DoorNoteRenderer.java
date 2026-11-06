package net.rai.scp_unleashed.scp4666.christmas.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.christmas.Scp4666DoorNote;
import org.joml.Matrix4f;

public class Scp4666DoorNoteRenderer extends EntityRenderer<Scp4666DoorNote> {
    public static final ResourceLocation Texture = Scp4666Registry.location("textures/entity/scp4666/scp4666_door_note.png");
    public static final float Left = 4.0f / 16.0f;
    public static final float Right = 12.0f / 16.0f;
    public static final float Top = 3.0f / 16.0f;
    public static final float Bottom = 13.0f / 16.0f;
    public static final float HalfWidth = 4.0f / 16.0f;
    public static final float Height = 10.0f / 16.0f;
    public static final float Rise = 0.0f;

    public Scp4666DoorNoteRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(Scp4666DoorNote Note, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Scp4666DoorNote.Placement Spot = Note.placement();
        Vec3 Origin = Note.getPosition(PartialTick);
        Vec3 Target = Spot != null ? Spot.Spot() : Origin;
        float Facing = Spot != null ? Spot.Yaw() : Note.getYRot();
        Pose.pushPose();
        Pose.translate(Target.x - Origin.x, Target.y - Origin.y + Rise, Target.z - Origin.z);
        Pose.mulPose(Axis.YP.rotationDegrees(-Facing));
        PoseStack.Pose Last = Pose.last();
        Matrix4f Matrix = Last.pose();
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.entityCutoutNoCull(Texture));
        vertex(Consumer, Matrix, Last, -HalfWidth, 0.0f, Right, Bottom, Light);
        vertex(Consumer, Matrix, Last, HalfWidth, 0.0f, Left, Bottom, Light);
        vertex(Consumer, Matrix, Last, HalfWidth, Height, Left, Top, Light);
        vertex(Consumer, Matrix, Last, -HalfWidth, Height, Right, Top, Light);
        Pose.popPose();
        super.render(Note, Yaw, PartialTick, Pose, Buffers, Light);
    }

    private static void vertex(VertexConsumer Consumer, Matrix4f Matrix, PoseStack.Pose Last, float X, float Y, float U, float V, int Light) {
        Consumer.addVertex(Matrix, X, Y, 0.0f).setColor(255, 255, 255, 255).setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Light).setNormal(Last, 0.0f, 0.0f, 1.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(Scp4666DoorNote Note) {
        return Texture;
    }
}
