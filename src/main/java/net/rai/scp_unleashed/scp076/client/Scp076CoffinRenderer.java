package net.rai.scp_unleashed.scp076.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.scp076.Scp076CoffinBlockEntity;

public class Scp076CoffinRenderer implements BlockEntityRenderer<Scp076CoffinBlockEntity> {
    public static final float SlideShare = 0.55f;
    public static final float ClosedX = 1.5f;
    public static final float ClosedY = 12.0f;
    public static final float SlideX = 11.0f;
    public static final float SlideLift = 0.5f;
    public static final float RestX = 22.2f;
    public static final float RestY = 1.3f;
    public static final float RestAngle = 115.0f;

    public Scp076CoffinRenderer(BlockEntityRendererProvider.Context Context) {
    }

    private static float smooth(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    @Override
    public void render(Scp076CoffinBlockEntity Coffin, float Partial, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Coffin.getLevel();
        if (World == null) {
            return;
        }
        Direction Axis0 = Coffin.axis();
        float Share = Coffin.lid(Partial);
        float Slide = smooth(Share / SlideShare);
        float Tip = smooth((Share - SlideShare) / (1.0f - SlideShare));
        float SlidX = ClosedX + SlideX * Slide;
        float SlidY = ClosedY + SlideLift * Slide;
        float X = Mth.lerp(Tip, SlidX, RestX);
        float Y = Mth.lerp(Tip, SlidY, RestY);
        int Light = LevelRenderer.getLightColor(World, Coffin.getBlockPos().above());
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(-Axis0.toYRot()));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        Pose.translate(X / 16.0f, Y / 16.0f, 0.0f);
        Pose.mulPose(Axis.ZP.rotationDegrees(RestAngle * Tip));
        Scp076Models.render(Pose.last(), Buffers.getBuffer(RenderType.solid()), Scp076Models.get(Scp076Models.CoffinLid), Light, PackedOverlay, 1.0f, 1.0f, World, Axis0);
        Pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(Scp076CoffinBlockEntity Coffin) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(Scp076CoffinBlockEntity Coffin) {
        return Coffin.renderBounds();
    }
}
