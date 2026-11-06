package net.rai.scp_unleashed.scp020.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class Scp020GuiseRenderer extends LivingEntityRenderer<AbstractClientPlayer, Scp020GuiseModel> {
    public static final ResourceLocation ZombieTexture = ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");
    public static final float PlayerScale = 0.9375f;
    public static final float ShadowRadius = 0.5f;

    public Scp020GuiseRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp020GuiseModel(Context.bakeLayer(ModelLayers.ZOMBIE)), ShadowRadius);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractClientPlayer Subject) {
        return ZombieTexture;
    }

    @Override
    public void render(AbstractClientPlayer Subject, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        model.crouching = Subject.isCrouching();
        super.render(Subject, Yaw, PartialTick, Pose, Buffers, PackedLight);
    }

    @Override
    protected void scale(AbstractClientPlayer Subject, PoseStack Pose, float PartialTick) {
        Pose.scale(PlayerScale, PlayerScale, PlayerScale);
    }

    @Override
    protected boolean shouldShowName(AbstractClientPlayer Subject) {
        return false;
    }

    @Override
    protected void setupRotations(AbstractClientPlayer Subject, PoseStack Pose, float Bob, float BodyYaw, float PartialTick, float Scale) {
        super.setupRotations(Subject, Pose, Bob, BodyYaw, PartialTick, Scale);
        float Swim = Subject.getSwimAmount(PartialTick);
        if (Subject.isFallFlying()) {
            float Flight = Subject.getFallFlyingTicks() + PartialTick;
            float Lean = Mth.clamp(Flight * Flight / 100.0f, 0.0f, 1.0f);
            if (!Subject.isAutoSpinAttack()) {
                Pose.mulPose(Axis.XP.rotationDegrees(Lean * (-90.0f - Subject.getViewXRot(PartialTick))));
            }
        } else if (Swim > 0.0f) {
            float Tilt = Subject.isInWater() ? -90.0f - Subject.getXRot() : -90.0f;
            Pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(Swim, 0.0f, Tilt)));
            if (Subject.isVisuallySwimming()) {
                Pose.translate(0.0f, -1.0f, 0.3f);
            }
        }
    }
}
