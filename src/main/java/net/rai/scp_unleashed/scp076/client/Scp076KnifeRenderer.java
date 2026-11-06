package net.rai.scp_unleashed.scp076.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp076.Scp076Knife;

public class Scp076KnifeRenderer extends EntityRenderer<Scp076Knife> {
    public static final float Size = 0.85f;
    public static final float Balance = 7.0f / 16.0f;

    public Scp076KnifeRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(Scp076Knife Knife, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Pose.pushPose();
        Pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(Partial, Knife.yRotO, Knife.getYRot())));
        Pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(Partial, Knife.xRotO, Knife.getXRot())));
        if (!Knife.isStuck()) {
            Pose.mulPose(Axis.XP.rotationDegrees((Knife.tickCount + Partial) * Scp076Knife.SpinPerTick));
        } else {
            Pose.translate(0.0f, 0.0f, -0.18f);
        }
        Pose.mulPose(Axis.XP.rotationDegrees(90.0f));
        Pose.scale(Size, Size, Size);
        Pose.translate(-0.5f, -Balance, -0.5f);
        Scp076Models.render(Pose.last(), Buffers.getBuffer(Sheets.cutoutBlockSheet()), Scp076Models.get(Scp076Models.Knife), Light, OverlayTexture.NO_OVERLAY, 1.0f, 0.0f,
                Knife.level(), Direction.SOUTH);
        Pose.popPose();
        super.render(Knife, Yaw, Partial, Pose, Buffers, Light);
    }

    @Override
    public ResourceLocation getTextureLocation(Scp076Knife Knife) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
