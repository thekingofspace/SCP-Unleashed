package net.rai.scp_unleashed.scp354.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp354.Scp354Beast;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class Scp354BeastRenderer<T extends Scp354Beast> extends GeoEntityRenderer<T> {
    public static final float AlphaSteps = 8.0f;
    private final float Shadow;

    public Scp354BeastRenderer(EntityRendererProvider.Context Context, Scp354GeoModel<T> Model, float Shadow) {
        super(Context, Model);
        this.Shadow = Shadow;
        shadowRadius = Shadow;
    }

    @Override
    protected float getShadowRadius(T Creature) {
        if (Creature.isEmerging()) {
            return Shadow * (float) Math.min(1.0, Creature.actionElapsed() / Creature.emergeTicks());
        }
        return Shadow * Creature.fade(0.0f);
    }

    @Override
    public void render(T Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        Creature.applyLockedYaw();
        if (Creature.fade(PartialTick) <= 0.0f) {
            return;
        }
        super.render(Creature, Creature.locksYaw() ? Creature.getYRot() : Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    public RenderType getRenderType(T Creature, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        return Creature.fade(PartialTick) < 0.999f ? RenderType.entityTranslucent(Texture) : RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    public Color getRenderColor(T Creature, float PartialTick, int PackedLight) {
        float Stepped = Math.round(Creature.fade(PartialTick) * AlphaSteps) / AlphaSteps;
        return Color.ofRGBA(1.0f, 1.0f, 1.0f, Math.max(0.0f, Stepped));
    }

    @Override
    public int getPackedOverlay(T Creature, float U, float PartialTick) {
        return OverlayTexture.pack(OverlayTexture.u(U), OverlayTexture.v(Creature.hurtTime > 0 && !Creature.isDeadOrDying()));
    }

    @Override
    protected float getDeathMaxRotation(T Creature) {
        return 0.0f;
    }

    @Override
    protected void applyRotations(T Creature, PoseStack Pose, float AgeInTicks, float RotationYaw, float PartialTick, float NativeScale) {
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - RotationYaw));
    }
}
