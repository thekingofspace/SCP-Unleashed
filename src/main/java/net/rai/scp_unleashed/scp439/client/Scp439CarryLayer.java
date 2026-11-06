package net.rai.scp_unleashed.scp439.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp439.Scp439Worker;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class Scp439CarryLayer extends BlockAndItemGeoLayer<Scp439Worker> {
    public static final String CarryBone = "carry";
    public static final float ItemScale = 0.42f;

    public Scp439CarryLayer(GeoRenderer<Scp439Worker> Renderer) {
        super(Renderer);
    }

    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone Bone, Scp439Worker Worker) {
        if (!CarryBone.equals(Bone.getName()) || !Worker.isCarrying() || Worker.deathTime > 0) {
            return null;
        }
        return Worker.carried();
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone Bone, ItemStack Stack, Scp439Worker Worker) {
        return ItemDisplayContext.FIXED;
    }

    @Override
    protected void renderStackForBone(PoseStack Pose, GeoBone Bone, ItemStack Stack, Scp439Worker Worker, MultiBufferSource Buffers, float Partial, int Light, int Overlay) {
        Pose.pushPose();
        Pose.mulPose(Axis.XP.rotationDegrees(90.0f));
        Pose.scale(ItemScale, ItemScale, ItemScale);
        super.renderStackForBone(Pose, Bone, Stack, Worker, Buffers, Partial, Light, Overlay);
        Pose.popPose();
    }
}
