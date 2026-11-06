package net.rai.scp_unleashed.scp140.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.storage.client.StorageQuads;

import java.util.List;
import java.util.function.Consumer;

public final class Scp140BookModels {
    public static final ModelResourceLocation Base = standalone("book_base");
    public static final ModelResourceLocation BaseInked = standalone("book_base_inked");
    public static final ModelResourceLocation Lid = standalone("book_lid");
    public static final ModelResourceLocation LidInked = standalone("book_lid_inked");
    public static final ModelResourceLocation Spine = standalone("book_spine");
    public static final ModelResourceLocation Leaf = standalone("book_leaf");
    public static final ModelResourceLocation LeafInked = standalone("book_leaf_inked");
    public static final float HingeX = 4.5f;
    public static final float HingeY = 2.0f;
    public static final float OpenShift = 3.5f;
    public static final float LidLimit = 180.0f;
    public static final float LeafHide = 4.0f;

    private Scp140BookModels() {
    }

    private static ModelResourceLocation standalone(String Name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp140/" + Name));
    }

    public static List<ModelResourceLocation> all() {
        return List.of(Base, BaseInked, Lid, LidInked, Spine, Leaf, LeafInked);
    }

    public static float ease(float Value) {
        float Clamped = Math.max(0.0f, Math.min(1.0f, Value));
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    public static void render(PoseStack Pose, MultiBufferSource Buffers, Level World, float Open, float Turning, boolean Inked, int Light, int Overlay) {
        ModelManager Models = Minecraft.getInstance().getModelManager();
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.cutout());
        render(Pose, Location -> StorageQuads.render(Pose.last(), Consumer, Models.getModel(Location), null, World, 1.0f, 1.0f, 1.0f, 1.0f, Light, Overlay, true),
                Open, Turning, Inked);
    }

    public static void render(PoseStack Pose, Consumer<ModelResourceLocation> Draw, float Open, float Turning, boolean Inked) {
        float Amount = ease(Open);
        float LidAngle = LidLimit * Amount;
        Pose.pushPose();
        Pose.translate(OpenShift * Amount / 16.0f, 0.0f, 0.0f);
        Draw.accept(Inked ? BaseInked : Base);
        hinged(Pose, Draw, Inked ? LidInked : Lid, LidAngle);
        hinged(Pose, Draw, Spine, LidAngle * 0.5f);
        if (Turning > LeafHide && Turning < LidLimit - LeafHide) {
            hinged(Pose, Draw, Inked ? LeafInked : Leaf, Turning);
        }
        Pose.popPose();
    }

    private static void hinged(PoseStack Pose, Consumer<ModelResourceLocation> Draw, ModelResourceLocation Location, float Degrees) {
        Pose.pushPose();
        Pose.translate(HingeX / 16.0f, HingeY / 16.0f, 0.0f);
        Pose.mulPose(Axis.ZP.rotationDegrees(Degrees));
        Pose.translate(-HingeX / 16.0f, -HingeY / 16.0f, 0.0f);
        Draw.accept(Location);
        Pose.popPose();
    }
}
