package net.rai.scp_unleashed.scp439.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp439.Scp439Creature;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class Scp439CreatureRenderer<T extends Scp439Creature> extends GeoEntityRenderer<T> {
    public static final float AlphaSteps = 16.0f;
    private final float Shadow;

    public Scp439CreatureRenderer(EntityRendererProvider.Context Context, String Name, float Scale, float Shadow) {
        super(Context, new Scp439CreatureModel<>(Name));
        withScale(Scale);
        this.Shadow = Shadow;
        shadowRadius = Shadow;
    }

    public static float alpha(Scp439Creature Creature, float PartialTick) {
        return Math.round(Creature.fade(PartialTick) * AlphaSteps) / AlphaSteps;
    }

    @Override
    public void render(T Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        shadowRadius = Shadow * Creature.fade(PartialTick);
        if (alpha(Creature, PartialTick) <= 0.0f) {
            return;
        }
        super.render(Creature, Creature.locksMovement() && Creature.isActing() ? Creature.lockedYaw() : Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Nullable
    @Override
    public RenderType getRenderType(T Creature, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        float Alpha = alpha(Creature, PartialTick);
        if (Alpha < 0.5f && ShaderCompat.renderingShadowPass()) {
            return null;
        }
        return Alpha < 0.999f ? RenderType.entityTranslucent(Texture) : RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    public Color getRenderColor(T Creature, float PartialTick, int PackedLight) {
        return Color.ofRGBA(1.0f, 1.0f, 1.0f, alpha(Creature, PartialTick));
    }

    @Override
    public int getPackedOverlay(T Creature, float U, float PartialTick) {
        return Creature.deathTime > 0 ? OverlayTexture.NO_OVERLAY : super.getPackedOverlay(Creature, U, PartialTick);
    }

    @Override
    protected float getDeathMaxRotation(T Creature) {
        return 0.0f;
    }
}
